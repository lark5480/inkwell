<#
.SYNOPSIS
  把本地构建好的 Inkwell 镜像 + 配置发布到生产服务器并重启（含回滚）。

.DESCRIPTION
  为什么是"本地构建 + 搬运"而不是"服务器上构建"：
  生产机是 2C2G，容器内跑 Maven + Nuxt 构建极易 OOM，且国内拉 Docker Hub 不通。
  所以在开发机构建（可复用层缓存，秒级），再把镜像 save/scp/load 过去。

  脚本会同步：镜像、docker-compose.prod.yml、nginx/nginx.conf、sql/。
  nginx 配置变化时用 `nginx -t && nginx -s reload` 热加载（零停机），不重启容器。

.EXAMPLE
  # 服务器地址不写进仓库：用环境变量或显式传参
  $env:INKWELL_SERVER = '<你的服务器公网IP>'
  pwsh scripts/deploy.ps1                              # 发布全部三个服务
  pwsh scripts/deploy.ps1 -Services blog-web           # 只改了前端？秒级
  pwsh scripts/deploy.ps1 -Server 1.2.3.4 -SshKey ~/.ssh/id_ed25519

.EXAMPLE
  # 回滚到上一次发布（服务器上保留的 :prev 标签）
  pwsh scripts/deploy.ps1 -Rollback

.NOTES
  数据库迁移是手工的：本项目所有 profile 都是 ddl-auto=validate，
  改了实体/表结构必须先执行迁移 SQL，否则新后端启动即失败。见 docs/04 §11.5。
#>
[CmdletBinding()]
param(
  # 刻意不给默认值：服务器地址不入库，用环境变量 INKWELL_SERVER 或 -Server 传
  [string]$Server = $env:INKWELL_SERVER,
  [string]$SshUser = 'ubuntu',
  [string]$SshKey = $(if ($env:INKWELL_SSH_KEY) { $env:INKWELL_SSH_KEY } else { Join-Path $env:USERPROFILE '.ssh\inkwell_deploy' }),
  [string]$RemoteDir = '/opt/inkwell',
  [ValidateSet('blog-server', 'blog-web', 'blog-admin')]
  [string[]]$Services = @('blog-server', 'blog-web', 'blog-admin'),
  [switch]$Rollback,
  [switch]$SkipVerify,
  # 明知本次动了实体/表结构却没有配套迁移文件时，显式跳过守卫（危险，自担风险）
  [switch]$AllowSchemaChange
)

$ErrorActionPreference = 'Stop'
if (Get-Variable -Name PSNativeCommandUseErrorActionPreference -ErrorAction SilentlyContinue) {
  $PSNativeCommandUseErrorActionPreference = $false   # 原生命令写 stderr 不要当异常抛出
}
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root
$env:COMPOSE_BAKE = 'false'   # Compose v2 的 buildx bake 路径有 panic bug

if (-not $Server) {
  throw "缺少服务器地址。请设置环境变量 `$env:INKWELL_SERVER='<IP或域名>'，或用 -Server 传入（不要把真实 IP 提交进仓库）。"
}
if (-not (Test-Path $SshKey)) { throw "SSH 私钥不存在: $SshKey（用 -SshKey 指定）" }

$target = "$SshUser@$Server"
$sshArgs = @('-i', $SshKey, '-o', 'BatchMode=yes', '-o', 'StrictHostKeyChecking=no', '-o', 'ConnectTimeout=15')
# 本地构建只需要变量非空即可，缺 .env.prod 时退化为示例文件（里面是 CHANGE_ME 占位）
$envFile = if (Test-Path "$root\.env.prod") { "$root\.env.prod" } else { "$root\.env.prod.example" }
$compose = @('compose', '--env-file', $envFile, '-f', "$root\docker-compose.prod.yml")
$remoteCompose = "docker compose --env-file .env.prod -f docker-compose.prod.yml"

function Invoke-Remote([string]$Command) {
  $out = & ssh @sshArgs $target $Command 2>&1
  if ($LASTEXITCODE -ne 0) { throw "远端命令失败（exit $LASTEXITCODE）: $Command`n$out" }
  return $out
}

function Send-File([string]$Local, [string]$Remote) {
  & scp @sshArgs $Local "$target`:$Remote"
  if ($LASTEXITCODE -ne 0) { throw "scp 失败: $Local -> $Remote" }
}

function Get-LocalMd5([string]$Path) { (Get-FileHash $Path -Algorithm MD5).Hash.ToLower() }

# ---------------------------------------------------------------- 版本信息
$branch = (& git rev-parse --abbrev-ref HEAD 2>$null)
$rev = (& git rev-parse --short HEAD 2>$null)
$dirty = @(& git status --porcelain 2>$null)
$dirtyFlag = if ($dirty.Count -gt 0) { "+dirty($($dirty.Count))" } else { '' }
$stamp = "$(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')"

Write-Host "=== Inkwell 发布 ===" -ForegroundColor Cyan
Write-Host "  目标    : $target`:$RemoteDir"
Write-Host "  代码版本: $branch@$rev$dirtyFlag"
if ($dirty.Count -gt 0) {
  Write-Host "  ⚠️  工作区有 $($dirty.Count) 个未提交改动，发布的是当前文件内容而非某个提交" -ForegroundColor Yellow
  Write-Host "     想按提交发布：先 git commit（建议再 merge 到 master），再跑本脚本" -ForegroundColor Yellow
}

# ---------------------------------------------------------------- 回滚分支
if ($Rollback) {
  Write-Host "=== 回滚到上一版（:prev 标签）===" -ForegroundColor Cyan
  $tags = ($Services | ForEach-Object { "inkwell-$_" }) -join ' '
  $cmd = 'for i in ' + $tags + '; do if docker image inspect "$i:prev" >/dev/null 2>&1; then docker tag "$i:prev" "$i:latest"; echo "  rolled back $i"; else echo "  skip $i (no :prev tag)"; fi; done'
  Invoke-Remote $cmd
  Invoke-Remote "cd $RemoteDir && $remoteCompose up -d --no-build 2>&1 | sed 's/^/  /'"
  Invoke-Remote "docker exec blog-nginx nginx -s reload >/dev/null 2>&1 && echo '  nginx 已 reload（重新解析 upstream IP）' || true"
  Write-Host "  回滚完成" -ForegroundColor Green
  exit 0
}

# ---------------------------------------------------------------- 数据库结构守卫
# 三个 profile 都是 ddl-auto: validate：数据库没跟上代码时，新后端启动即失败。
# 这里做两道拦截，把"忘了迁移"变成发布失败而不是线上 502。
if (-not $Rollback) {
  Write-Host "`n=== 0/6 数据库迁移检查 ===" -ForegroundColor Cyan

  # a) 服务器上还有未应用的迁移 → 拒绝发布
  & pwsh -NoProfile -File "$root\scripts\migrate.ps1" -Server $Server -SshUser $SshUser -SshKey $SshKey -RemoteDir $RemoteDir -CheckPending
  switch ($LASTEXITCODE) {
    0 { Write-Host "  迁移状态：无待应用" -ForegroundColor Green }
    2 {
      throw "存在待应用的数据库迁移，已中止发布。请先执行：`n  pwsh scripts/migrate.ps1 -Apply`n（或先 -Status 看清单）"
    }
    default { throw "迁移状态检查失败（exit $LASTEXITCODE）" }
  }

  # b) 本次改动动了实体/表结构，却没有配套迁移文件 → 拒绝发布
  $deployedRaw = (Invoke-Remote "cat $RemoteDir/DEPLOYED 2>/dev/null || echo ''").Trim()
  $deployedRev = if ($deployedRaw -match '@([0-9a-f]{7,40})') { $Matches[1] } else { $null }
  if ($deployedRev) {
    & git rev-parse --verify --quiet "$deployedRev^{commit}" > $null 2>&1
    if ($LASTEXITCODE -eq 0) {
      $changed = @(& git diff --name-only $deployedRev 2>$null) + @(& git ls-files --others --exclude-standard 2>$null)
      $schemaTouched = @($changed | Where-Object { $_ -match '^blog-server/src/main/java/com/blog/entity/' -or $_ -eq 'sql/init.sql' } | Select-Object -Unique)
      $migrationTouched = @($changed | Where-Object { $_ -match '^sql/migrations/.*\.sql$' } | Select-Object -Unique)
      if ($schemaTouched.Count -gt 0 -and $migrationTouched.Count -eq 0) {
        if ($AllowSchemaChange) {
          Write-Host "  ⚠️ 动了实体但没有迁移文件（已用 -AllowSchemaChange 跳过）：" -ForegroundColor Yellow
          $schemaTouched | ForEach-Object { Write-Host "      $_" -ForegroundColor Yellow }
        } else {
          Write-Host "  ✗ 自 $deployedRev 以来动了实体/表结构，但 sql/migrations/ 下没有新迁移：" -ForegroundColor Red
          $schemaTouched | ForEach-Object { Write-Host "      $_" -ForegroundColor Red }
          throw @"
这会导致新后端启动时 Hibernate 校验失败（线上 502 + 容器重启）。
请先写一条迁移（命名 V<YYYYMMDDHHMMSS>__<desc>.sql，见 sql/migrations/README.md），
再 pwsh scripts/migrate.ps1 -Apply，然后重新发布。
确实不需要迁移（例如只改了 getter/注释）时，可加 -AllowSchemaChange 跳过本检查。
"@
        }
      } elseif ($migrationTouched.Count -gt 0) {
        Write-Host "  本次变更包含迁移文件（已确认应用）：$($migrationTouched -join ', ')" -ForegroundColor Green
      }
    } else {
      Write-Host "  跳过实体变更守卫：本地没有服务器上部署的提交 $deployedRev" -ForegroundColor DarkGray
    }
  } else {
    Write-Host "  跳过实体变更守卫：读不到 $RemoteDir/DEPLOYED" -ForegroundColor DarkGray
  }
}

# ---------------------------------------------------------------- 1) 构建
Write-Host "`n=== 1/6 本地构建 $($Services -join ', ') ===" -ForegroundColor Cyan
& docker @compose build @Services
if ($LASTEXITCODE -ne 0) { throw "构建失败" }

# ---------------------------------------------------------------- 2) 导出
$images = $Services | ForEach-Object { "inkwell-$_" }
$tar = Join-Path $env:TEMP 'inkwell-release.tar'
Remove-Item $tar -ErrorAction SilentlyContinue
Write-Host "`n=== 2/6 导出镜像 ===" -ForegroundColor Cyan
& docker save -o $tar @images
Write-Host "  $($images -join ', ') -> $([math]::Round((Get-Item $tar).Length / 1MB, 1)) MB"

# ---------------------------------------------------------------- 3) 同步配置（compose / nginx / sql）
Write-Host "`n=== 3/6 同步配置 ===" -ForegroundColor Cyan
$localNginxMd5 = Get-LocalMd5 "$root\nginx\nginx.conf"
$remoteNginxMd5 = (Invoke-Remote "md5sum $RemoteDir/nginx/nginx.conf 2>/dev/null | cut -d' ' -f1").Trim()
$nginxChanged = ($localNginxMd5 -ne $remoteNginxMd5)
Send-File "$root\docker-compose.prod.yml" "$RemoteDir/docker-compose.prod.yml"
Send-File "$root\nginx\nginx.conf" "$RemoteDir/nginx/nginx.conf"
Invoke-Remote "mkdir -p $RemoteDir/sql"
Send-File "$root\sql\init.sql" "$RemoteDir/sql/init.sql"
Write-Host "  docker-compose.prod.yml / nginx/nginx.conf / sql/init.sql 已同步"
if ($nginxChanged) { Write-Host "  nginx 配置有变化，将在重启后热加载" -ForegroundColor Yellow } else { Write-Host "  nginx 配置无变化" }

# ---------------------------------------------------------------- 4) 备份当前镜像为 :prev（便于回滚）
Write-Host "`n=== 4/6 服务器上标记上一版为 :prev ===" -ForegroundColor Cyan
$tagCmd = ($images | ForEach-Object { "docker tag $_`:latest $_`:prev 2>/dev/null || true" }) -join '; '
Invoke-Remote $tagCmd
Write-Host "  已标记（只是多一个标签，不额外占空间）"

# ---------------------------------------------------------------- 5) 传输 + 载入
Write-Host "`n=== 5/6 传输并载入镜像 ===" -ForegroundColor Cyan
& scp @sshArgs $tar "$target`:/tmp/inkwell-release.tar"
if ($LASTEXITCODE -ne 0) { throw "scp 失败" }
Invoke-Remote "docker load -i /tmp/inkwell-release.tar | sed 's/^/  /' ; rm -f /tmp/inkwell-release.tar"
$revLine = "$branch@$rev$dirtyFlag  deployed=$stamp"
Invoke-Remote "printf '%s\n' '$revLine' | tee $RemoteDir/DEPLOYED > /dev/null; echo '  版本记录: $revLine'"
Remove-Item $tar -ErrorAction SilentlyContinue

# ---------------------------------------------------------------- 6) 重启 + 热加载 nginx
Write-Host "`n=== 6/6 重启容器（只重建镜像变化的服务）===" -ForegroundColor Cyan
Invoke-Remote "cd $RemoteDir && $remoteCompose up -d --no-build 2>&1 | sed 's/^/  /'"
if ($nginxChanged) {
  Write-Host "  热加载 nginx 配置（零停机）..." -ForegroundColor Cyan
  Invoke-Remote "docker exec blog-nginx nginx -t >/dev/null 2>&1 && docker exec blog-nginx nginx -s reload && echo '  nginx reloaded' || echo '  !! nginx -t 校验失败，保持旧配置运行'"
}

# ---------------------------------------------------------------- 验收
if (-not $SkipVerify) {
  Write-Host "`n=== 验收 ===" -ForegroundColor Cyan
  Invoke-Remote "cd $RemoteDir && $remoteCompose ps --format '  {{.Name}}  {{.Status}}'"
  $probe = 'for i in $(seq 1 30); do sleep 3; curl -sf http://127.0.0.1/api/web/site-info >/dev/null 2>&1 && break; done; ' +
  'printf "  首页 HTTP %s\n" "$(curl -s -o /dev/null -w "%{http_code}" http://127.0.0.1/)"; ' +
  'printf "  后台 HTTP %s\n" "$(curl -s -o /dev/null -w "%{http_code}" http://127.0.0.1/admin/)"; ' +
  'printf "  API  HTTP %s\n" "$(curl -s -o /dev/null -w "%{http_code}" http://127.0.0.1/api/web/site-info)"; ' +
  'printf "  RSS  HTTP %s\n" "$(curl -s -o /dev/null -w "%{http_code}" http://127.0.0.1/rss.xml)"'
  Invoke-Remote $probe
  # 后端没起来时，主动把"是不是数据库没跟上"这个原因查出来，而不是让你自己猜
  $diag = @'
if ! curl -sf http://127.0.0.1/api/web/site-info >/dev/null 2>&1; then
  echo ""
  echo "  !! 后端未就绪。blog-server 最近日志里的关键行："
  cd /opt/inkwell
  docker compose --env-file .env.prod -f docker-compose.prod.yml logs --tail=80 blog-server 2>&1 | grep -iE "schema|validation|missing column|doesn.t exist|ERROR|Exception" | tail -10 | sed "s/^/    /"
  echo ""
  echo "  若上面是 Hibernate schema validation 报错 → 数据库结构没跟上代码："
  echo "    1) pwsh scripts/migrate.ps1 -Status    # 看漏了哪条迁移"
  echo "    2) pwsh scripts/migrate.ps1 -Apply     # 备份 + 应用"
  echo "    3) pwsh scripts/deploy.ps1             # 再发布"
  echo "  急着先恢复服务：pwsh scripts/deploy.ps1 -Rollback"
fi
'@
  Invoke-Remote $diag
}

Write-Host "`n✅ 发布完成：http://$Server/" -ForegroundColor Green
