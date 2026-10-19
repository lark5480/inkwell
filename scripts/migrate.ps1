<#
.SYNOPSIS
  数据库迁移：查看待应用迁移 / 备份并应用迁移（带登记表）。

.DESCRIPTION
  本项目三个 profile 都是 ddl-auto: validate，Hibernate 只校验不改表；
  sql/init.sql 是"空库全量脚本"，对已有数据的库不会（也不该）重跑。
  因此改了实体/表结构必须走 sql/migrations/ 下的迁移，并由本脚本登记到
  schema_migrations 表——这样"哪些迁移应用过"是可查询的事实，而不是靠人记住。

.EXAMPLE
  $env:INKWELL_SERVER = '<服务器IP>'
  pwsh scripts/migrate.ps1            # 查看状态（默认）
  pwsh scripts/migrate.ps1 -Apply     # 备份 + 按序应用 + 登记
  pwsh scripts/migrate.ps1 -Apply -SkipBackup
#>
[CmdletBinding()]
param(
  [string]$Server = $env:INKWELL_SERVER,
  [string]$SshUser = 'ubuntu',
  [string]$SshKey = $(if ($env:INKWELL_SSH_KEY) { $env:INKWELL_SSH_KEY } else { Join-Path $env:USERPROFILE '.ssh\inkwell_deploy' }),
  [string]$RemoteDir = '/opt/inkwell',
  [string]$Database = 'blog',
  [switch]$Apply,
  [switch]$SkipBackup,
  # 给 deploy.ps1 用：只判断有没有待应用迁移（有=exit 2，无=exit 0），不做任何写操作
  [switch]$CheckPending
)

$ErrorActionPreference = 'Stop'
if (Get-Variable -Name PSNativeCommandUseErrorActionPreference -ErrorAction SilentlyContinue) {
  $PSNativeCommandUseErrorActionPreference = $false
}
$root = Split-Path -Parent $PSScriptRoot
if (-not $Server) { throw "缺少服务器地址：设置 `$env:INKWELL_SERVER 或用 -Server 传入" }
if (-not (Test-Path $SshKey)) { throw "SSH 私钥不存在: $SshKey" }
$target = "$SshUser@$Server"
$sshArgs = @('-i', $SshKey, '-o', 'BatchMode=yes', '-o', 'StrictHostKeyChecking=no', '-o', 'ConnectTimeout=15')

function Get-NormalizedSha256([string]$Path) {
  # 仓库以 LF 存储迁移文件，Windows 检出可能是 CRLF；
  # 直接哈希原始字节会把行尾差异误报成内容漂移，所以先归一化为 LF。
  $bytes = [System.IO.File]::ReadAllBytes($Path)
  $normalized = [System.Collections.Generic.List[byte]]::new($bytes.Length)
  for ($i = 0; $i -lt $bytes.Length; $i++) {
    if ($bytes[$i] -eq 13 -and $i + 1 -lt $bytes.Length -and $bytes[$i + 1] -eq 10) { continue }
    $normalized.Add($bytes[$i])
  }
  $sha = [System.Security.Cryptography.SHA256]::Create()
  try {
    return ([System.BitConverter]::ToString($sha.ComputeHash($normalized.ToArray())) -replace '-', '').ToLower()
  } finally {
    $sha.Dispose()
  }
}

function Invoke-RemoteScript([string]$Script) {
  # 注意：PowerShell 把字符串喂给原生命令的 stdin 时会写 CRLF，
  # 会让远端 heredoc 的终止符（xxx\r）匹配不上，所以远端先用 tr 去掉 \r。
  $out = $Script | & ssh @sshArgs $target "tr -d '\r' | bash -s" 2>&1
  return [pscustomobject]@{ Exit = $LASTEXITCODE; Out = (($out | Out-String).Trim()) }
}

function New-SqlScript([string]$Sql) {
  @"
docker exec -i blog-mysql sh -c 'mysql -uroot -p`$MYSQL_ROOT_PASSWORD -N -B $Database' <<'INKWELL_SQL_EOF'
$Sql
INKWELL_SQL_EOF
"@
}

# ---------------------------------------------------------------- 本地迁移文件
$files = @(Get-ChildItem "$root\sql\migrations" -Filter '*.sql' -File -ErrorAction SilentlyContinue | Sort-Object Name)
if ($files.Count -eq 0) { Write-Host "sql/migrations 下没有迁移文件" -ForegroundColor Yellow; exit 0 }
$local = foreach ($f in $files) {
  $ver = $f.Name -replace '\.sql$', ''
  if ($ver -notmatch '^V\d{14}__') { throw "迁移文件名不符合约定 V<YYYYMMDDHHMMSS>__<desc>.sql: $($f.Name)" }
  [pscustomobject]@{
    Version  = $ver
    Name     = $f.Name
    Path     = $f.FullName
    Checksum = Get-NormalizedSha256 $f.FullName
  }
}

# ---------------------------------------------------------------- 服务器已应用
Write-Host "=== 连接 $target ===" -ForegroundColor Cyan
$exists = Invoke-RemoteScript (New-SqlScript "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='$Database' AND table_name='schema_migrations';")
if ($exists.Exit -ne 0) { throw "查询失败（MySQL 容器是否在跑？）：`n$($exists.Out)" }
$tableExists = ($exists.Out -split "`n" | Where-Object { $_ -match '^\d+$' } | Select-Object -Last 1) -eq '1'

$applied = @{}
if ($tableExists) {
  $rows = Invoke-RemoteScript (New-SqlScript "SELECT CONCAT(version,'|',IFNULL(checksum,'')) FROM schema_migrations;")
  if ($rows.Exit -ne 0) { throw "读取 schema_migrations 失败：`n$($rows.Out)" }
  foreach ($line in ($rows.Out -split "`n")) {
    $t = $line.Trim()
    if ($t -match '^(\S+)\|([0-9a-f]*)$') { $applied[$Matches[1]] = $Matches[2] }
  }
}

# ---------------------------------------------------------------- 盘点
$pending = @($local | Where-Object { -not $applied.ContainsKey($_.Version) })
$drift = @($local | Where-Object { $applied.ContainsKey($_.Version) -and $applied[$_.Version] -and $applied[$_.Version] -ne $_.Checksum })
$orphan = @($applied.Keys | Where-Object { $_ -notin $local.Version })

Write-Host "`n=== 迁移状态 ===" -ForegroundColor Cyan
Write-Host "  登记表 schema_migrations: $(if ($tableExists) { '已存在' } else { '不存在（视为 0 条已应用）' })"
foreach ($m in $local) {
  $state = if ($applied.ContainsKey($m.Version)) { '已应用' } else { '待应用' }
  $color = if ($applied.ContainsKey($m.Version)) { 'DarkGray' } else { 'Yellow' }
  Write-Host ("  [{0}] {1}" -f $state, $m.Name) -ForegroundColor $color
}
foreach ($o in $orphan) { Write-Host "  [登记表里有、本地没有] $o" -ForegroundColor Red }
foreach ($d in $drift) { Write-Host "  [漂移] $($d.Name) 已应用但内容被改过（不要改已应用的迁移，请新增一条）" -ForegroundColor Red }
Write-Host "  合计：$($local.Count) 条本地 / $($applied.Count) 条已应用 / $($pending.Count) 条待应用"

if ($pending.Count -eq 0) { Write-Host "`n✅ 没有待应用的迁移" -ForegroundColor Green; exit 0 }
if ($CheckPending) {
  Write-Host "`n存在 $($pending.Count) 条待应用迁移（-CheckPending 模式，未做任何修改）" -ForegroundColor Red
  exit 2
}
if (-not $Apply) {
  Write-Host "`n这是只读检查。要应用请执行：pwsh scripts/migrate.ps1 -Apply" -ForegroundColor Yellow
  exit 0
}

# ---------------------------------------------------------------- 备份
$ts = Get-Date -Format 'yyyyMMdd-HHmmss'
if (-not $SkipBackup) {
  Write-Host "`n=== 备份（迁移前）===" -ForegroundColor Cyan
  $backup = @"
set -eo pipefail
sudo mkdir -p /opt/backup
sudo chown "`$(id -un):`$(id -gn)" /opt/backup
docker exec -i blog-mysql sh -c 'exec mysqldump -uroot -p`$MYSQL_ROOT_PASSWORD --single-transaction --routines $Database' | gzip > /opt/backup/pre-migrate-$ts.sql.gz
sz=`$(stat -c%s /opt/backup/pre-migrate-$ts.sql.gz)
if [ "`$sz" -lt 1024 ]; then echo "备份文件异常小（`$sz 字节），已中止迁移"; exit 1; fi
gzip -t /opt/backup/pre-migrate-$ts.sql.gz || { echo "备份文件损坏，已中止迁移"; exit 1; }
ls -la /opt/backup/pre-migrate-$ts.sql.gz
"@
  $r = Invoke-RemoteScript $backup
  if ($r.Exit -ne 0) { throw "备份失败，已中止迁移：`n$($r.Out)" }
  Write-Host "  $($r.Out -split "`n" | Select-Object -Last 1)"
} else {
  Write-Host "`n=== 跳过备份（-SkipBackup）===" -ForegroundColor Yellow
}

# ---------------------------------------------------------------- 应用
Write-Host "`n=== 应用迁移 ===" -ForegroundColor Cyan
foreach ($m in $pending) {
  Write-Host "  → $($m.Name)"
  $remoteFile = "/tmp/inkwell-mig-$($m.Version).sql"
  & scp @sshArgs $m.Path "$target`:$remoteFile" 2>&1 | Out-Null
  if ($LASTEXITCODE -ne 0) { throw "上传迁移文件失败: $($m.Name)" }

  $run = @"
set -e
docker exec -i blog-mysql sh -c 'mysql -uroot -p`$MYSQL_ROOT_PASSWORD $Database' < $remoteFile
docker exec -i blog-mysql sh -c 'mysql -uroot -p`$MYSQL_ROOT_PASSWORD -N -B $Database' <<'INKWELL_SQL_EOF'
INSERT INTO schema_migrations (version, filename, checksum)
VALUES ('$($m.Version)', '$($m.Name)', '$($m.Checksum)')
ON DUPLICATE KEY UPDATE filename = VALUES(filename), checksum = VALUES(checksum), applied_at = NOW();
INKWELL_SQL_EOF
rm -f $remoteFile
echo "  applied: $($m.Version)"
"@
  $r = Invoke-RemoteScript $run
  if ($r.Exit -ne 0) {
    Write-Host "  !! 应用失败：`n$($r.Out)" -ForegroundColor Red
    throw "迁移 $($m.Name) 执行失败，后续迁移已中止（数据库可能处于中间状态，请人工确认后再继续）"
  }
}

Write-Host "`n✅ 全部迁移已应用（$($pending.Count) 条）" -ForegroundColor Green
Write-Host "   接下来可以发布镜像：pwsh scripts/deploy.ps1" -ForegroundColor Green
