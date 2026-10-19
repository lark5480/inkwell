# 04 — 部署到腾讯云轻量 2C2G（生产）

> 面向 2 核 2G 轻量应用服务器（腾讯云）的单机生产部署。
> 与 [03-deployment.md](03-deployment.md) 的关系：03 讲的是通用 Docker 部署，本文是**针对 2G 小内存做过裁剪、并修掉了原编排 3 个启动阻塞问题**的版本。
>
> ⚠️ **本文档不写真实服务器地址、密钥、证书**：命令里统一用 `SERVER_IP` 变量占位，密钥只在服务器上的 `.env.prod` 里（该文件已被 `.gitignore` 忽略）。往仓库里提交文档/配置时，请勿填回真实 IP 或任何密钥。

## 0. 结论

可以部署。但有三个硬性前提：

1. **必须开 2G swap**（2G 内存同时跑 MySQL + JVM + Node SSR 会紧）
2. **不要启用 MinIO**（省 200~300MB 内存），改用本地磁盘存储 + `./uploads` 持久化
3. **用 `docker-compose.prod.yml`**，不要用根目录的 `docker-compose.yml`（后者在当前代码下 `docker compose up -d` 起不来，原因见文末「本次改动清单」）

## 1. 目标架构

```
浏览器
  │ 80
  ▼
blog-nginx ──┬─ /api/     → blog-server:8080  (Spring Boot 3.2)
             ├─ /uploads/ → blog-server:8080  (本地磁盘图片)
             ├─ /admin/   → blog-admin:80     (Vue3 SPA，构建时 base=/admin/)
             └─ /        → blog-web:3000      (Nuxt 3 SSR)
                              │
        blog-server ──────────┼── blog-mysql:3306 (仅内网，不映射端口)
                              └── blog-redis:6379 (仅内网，不映射端口)
```

关键点：**对外只暴露 80**（以后加 443）。原代码里浏览器端把 API 地址硬编码成 `http://<hostname>:8080`，本次已改为同源 `/api`，所以 8080 不需要对外开放。

## 2. 服务器准备

```bash
# 下面用 SERVER_IP 代指你的服务器公网 IP（别把真实 IP 写进仓库）
export SERVER_IP=<你的服务器公网IP>
ssh root@$SERVER_IP

# 2.1 swap（必做，2G 机器没 swap 构建和运行都可能被杀）
sudo fallocate -l 2G /swapfile
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab
free -h          # 确认 Swap 有 2.0Gi

# 2.2 Docker + Compose（国内用镜像脚本，快很多）
curl -fsSL https://get.docker.com | sudo sh -s -- --mirror Aliyun
sudo systemctl enable --now docker
docker compose version    # 需要 v2.20+，低了就 apt 升级 docker-compose-plugin

# 2.3 时区（后端日志/JDBC 都按东八区）
sudo timedatectl set-timezone Asia/Shanghai
```

**2.4 腾讯云控制台 → 安全组**：只放通

| 端口 | 用途 | 是否必须 |
|------|------|----------|
| 22 | SSH | 必须（已开） |
| 80 | 网站入口 | 必须 |
| 443 | HTTPS | 有域名后再开 |
| 3306 / 6379 / 8080 / 9000 | —— | **一律不要开** |

> 新买的机器默认通常只放通 22，其余全被安全组挡住，所以要手动加规则。**改完安全组前后都可以用 `nmap`/在线端口检测确认一下，别顺手把 3306/6379 也开了。**

## 3. 把代码放到服务器

方式 A：走 GitHub（推荐，仓库已是 `https://github.com/lark5480/inkwell.git`）

```bash
# 先在 Windows 本地提交并推送本次改动（见「本次改动清单」）
git push origin dev

# 服务器上
sudo mkdir -p /opt && cd /opt
sudo git clone -b dev https://github.com/lark5480/inkwell.git inkwell
cd inkwell
```

方式 B：本地打包上传（GitHub 拉不动时用）

```powershell
# Windows
cd F:\other\code\ai\inkwell
tar --exclude=node_modules --exclude=target --exclude=.git --exclude=uploads -czf ..\inkwell-src.tgz .
scp ..\inkwell-src.tgz root@$SERVER_IP:/opt/
```

```bash
# 服务器
mkdir -p /opt/inkwell && tar xzf /opt/inkwell-src.tgz -C /opt/inkwell
```

> `uploads/`（历史图片，约 100MB）不在 git 里，需要就单独 `scp -r`；不需要就跳过，站点会自动以空目录启动。

## 4. 生产配置

```bash
cd /opt/inkwell
cp .env.prod.example .env.prod
chmod 600 .env.prod

# 生成两个密钥，填进 .env.prod
openssl rand -base64 24    # → MYSQL_ROOT_PASSWORD
openssl rand -base64 48    # → JWT_SECRET（必须 Base64，解码后 ≥32 字节）
vim .env.prod
```

`.env.prod` 三个必填项：

| 变量 | 说明 |
|------|------|
| `MYSQL_ROOT_PASSWORD` | 首次启动时写入 MySQL。**之后改这里不会改库里已有密码**，两边必须一致 |
| `JWT_SECRET` | 后端 `application-docker.yml` 里是无默认值的 `${JWT_SECRET}`，缺了直接启动失败 |
| `SITE_URL` | 注入给 blog-server（`blog.site-url`），决定 RSS / Sitemap 里的绝对链接，填 `http://$SERVER_IP`（以后有域名就换成域名）；缺省回落到 `http://localhost`，链接会是错的 |

## 5. 构建与启动

```bash
cd /opt/inkwell
DC="docker compose --env-file .env.prod -f docker-compose.prod.yml"

# 5.1 必须关掉 buildx bake：Compose v2 的 bake 路径有 panic bug（build_bake.go:312），
#     实测 Compose v2.40.3 + Docker 28.3.3 上 100% 复现，报
#     "panic: runtime error: slice bounds out of range [1:0]"。
#     注意：网上流传的"先 build 再 up 就绕过"是错的——`docker compose build` 本身就会 panic。
export COMPOSE_BAKE=false

$DC build          # 2C2G + swap：约 10~25 分钟（Maven 拉依赖 + Nuxt/ Vite 构建）
$DC up -d
$DC ps             # 六个容器，mysql / redis / blog-server 应为 healthy
$DC logs -f blog-server   # 首次看 Spring Boot 启动日志，出现 Started BlogApplication 即可 Ctrl+C
```

> 万一 `COMPOSE_BAKE=false` 仍然失败，就退化成逐个镜像构建（最稳，不受 compose 版本影响）：
>
> ```bash
> docker build -t inkwell-blog-server ./blog-server
> docker build -t inkwell-blog-web    ./blog-web
> docker build -t inkwell-blog-admin  --build-arg VITE_BASE=/admin/ ./blog-admin
> $DC up -d --no-build
> ```

> **以后往前端加依赖时注意**：`blog-web/package-lock.json` 必须用**与容器一致的 npm** 生成，否则 `npm ci` 会报
> `Missing: @rolldown/binding-... from lock file`（本机 npm 12 与容器 npm 10 的 lockfile 记录方式不同）。正确做法：
>
> ```bash
> cd /opt/inkwell
> docker run --rm -v "$PWD/blog-web:/w" -w /w node:22-alpine npm install <包名> --package-lock-only
> ```
>
> 前端基础镜像已统一为 `node:22-alpine`（Node 20 已 EOL，且部分依赖要求 Node ≥ 22）。

首次启动顺序：MySQL 初始化（执行 `sql/init.sql`，建表 + 14 篇演示文章 + `admin` 账号）→ Redis → 后端（healthcheck start_period 90s）→ 前端 → nginx。

> 这三个镜像的构建**已在本地用当前仓库验证通过**（`npm run build` / `mvn package` 等价流程）。
> 注意：`blog-web/package.json` 里的 `@unocss/nuxt` 必须是 `^66`——原作者锁的 `^0.58` 与 Nuxt 3.21 自带的 Vite 7 不兼容，
> `npm run build` 会直接报 `[unocss:global:build:scan] cssPlugins.get(...).transform.call is not a function`，
> 而 `npm run dev` 不会报，属于「本地能跑、一构建就炸」的坑（详见附录 B）。

## 6. 验收清单

在服务器上：

```bash
curl -sI  http://127.0.0.1/                          # 200 + Nuxt SSR
curl -s   "http://127.0.0.1/api/web/site-info"       # {"code":200,...}
curl -sI  http://127.0.0.1/admin/                    # 200 + text/html
curl -s   "http://127.0.0.1/api/web/articles?page=1&pageSize=1" | head -c 200
curl -s   http://127.0.0.1/rss.xml | head -5
```

在你本机：

```bash
curl -s "http://$SERVER_IP/api/web/site-info"   # 安全组放通后才通
```

浏览器：

- `http://$SERVER_IP/` — 首页能出文章列表（**列表空白基本就是 SSR 连不上后端**，看 `$DC logs blog-web`）
- `http://$SERVER_IP/admin/` — 用 `admin` / `admin123` 登录
- 随便进一篇文章，F12 Network 里不应出现 `:8080` 的请求

## 7. 上线后必做

### 7.1 改掉默认管理员密码

`admin/admin123` 是初始化脚本里的弱口令。登录 C 端 → 个人中心 `/settings` → 修改密码（后台账号同时是 C 端账号）。注册接口是开放的（多作者平台），如果暂时不想要公开注册，先把首页注册入口下线或在前端路由里拦住。

> **不想在浏览器里操作 / 想用随机强口令**（口令不经手任何人）：
>
> ```bash
> # 在服务器上生成并改密（口令只存在于服务器）
> NEWPW=$(openssl rand -base64 48 | tr -dc 'A-Za-z0-9' | head -c 20)
> TOKEN=$(curl -s -X POST http://127.0.0.1/api/web/auth/login -H 'Content-Type: application/json' \
>   -d '{"username":"admin","password":"admin123"}' | sed -n 's/.*"token":"\([^"]*\)".*/\1/p')
> curl -s -X PUT http://127.0.0.1/api/web/user/password -H "Authorization: Bearer $TOKEN" \
>   -H 'Content-Type: application/json' -d "{\"oldPassword\":\"admin123\",\"newPassword\":\"$NEWPW\"}"
> printf '新口令: %s\n' "$NEWPW" | sudo tee /root/admin-password.txt >/dev/null && sudo chmod 600 /root/admin-password.txt
> ```
>
> ⚠️ **验证时必须看 body 里的 `code`**：本项目业务错误同样返回 HTTP 200（`{"code":401,...}`），
> 用 `curl -w '%{http_code}'` 会看到"旧口令也能登录"的假象。正确判据：
> 新口令 → `code:200` 且拿到 token；旧口令 → `code:401` 且无 token。
>
> 另外：JWT 无状态，**改密码不会让已签发的 token 失效**（有效期 2h）。怀疑 token 泄露时，除了改密码还要换 `JWT_SECRET` 并重启后端。

### 7.2 删掉演示数据（可选）

初始化脚本塞了 14 篇演示文章和 6 个分类。推荐在后台文章列表里直接删（会带上关联清理）；要一次性清库就先备份再执行：

```bash
# 注意：docker compose exec 后面接的是**服务名**（本编排里是 mysql），不是容器名 blog-mysql；
# 写容器名会得到很难懂的报错：service "blog-mysql" is not running
$DC exec -T mysql sh -c 'mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" --single-transaction blog' > /tmp/blog-before-clean.sql

$DC exec -T mysql sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" blog' <<'SQL'
DELETE FROM comment_reports WHERE comment_id IN (SELECT id FROM comments WHERE article_id BETWEEN 1 AND 14);
DELETE FROM comment_votes   WHERE comment_id IN (SELECT id FROM comments WHERE article_id BETWEEN 1 AND 14);
DELETE FROM comments        WHERE article_id BETWEEN 1 AND 14;
DELETE FROM article_tags    WHERE article_id BETWEEN 1 AND 14;
DELETE FROM article_likes   WHERE article_id BETWEEN 1 AND 14;
DELETE FROM article_view_history WHERE article_id BETWEEN 1 AND 14;
DELETE FROM articles WHERE id BETWEEN 1 AND 14;
SQL
```

> `sql/init.sql` 开头是 `DROP TABLE`，**只会在数据卷为空时自动执行一次**；千万不要对生产库手工重跑它。

### 7.3 备份（强烈建议）

```bash
sudo mkdir -p /opt/backup/mysql
sudo tee /opt/backup/dump.sh >/dev/null <<'EOF'
#!/bin/bash
set -eo pipefail
# ↑ pipefail 是关键：没有它，mysqldump 失败会被管道末端 gzip 的成功退出码掩盖，
#   结果是"每天备份都成功"，实际全是 20 字节的空文件
# 这里用 docker exec + 容器名（blog-mysql）而不是 docker compose exec：
#   - compose exec 要接服务名（mysql），且依赖 compose 文件/cwd/项目解析
#   - docker exec 用容器名，在 cron 的最小环境里更稳
out=/opt/backup/mysql/blog-$(date +%F).sql.gz
docker exec -i blog-mysql sh -c 'exec mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" --single-transaction --routines blog' | gzip > "$out"
sz=$(stat -c%s "$out")
[ "$sz" -gt 1024 ] || { echo "备份文件异常小($sz 字节)，视为失败"; exit 1; }
gzip -t "$out" || { echo "备份文件损坏"; exit 1; }
tar czf /opt/backup/uploads-$(date +%F).tgz -C /opt/inkwell uploads
find /opt/backup -type f -mtime +14 -delete
echo "OK $(date '+%F %T') mysql_dump=$(stat -c%s "$out") bytes"
EOF
sudo chmod 755 /opt/backup/dump.sh
# 去重安装（重复执行不会叠加）
( sudo crontab -l 2>/dev/null | grep -v '/opt/backup/dump.sh' ; echo "30 3 * * * /opt/backup/dump.sh >> /var/log/inkwell-backup.log 2>&1" ) | sudo crontab -
sudo crontab -l
```

> 配好后**务必当场验证**，而且要**连 cron 的最小环境一起验证**（cron 里没有登录时的 PATH）：
>
> ```bash
> sudo env -i PATH=/usr/bin:/bin /opt/backup/dump.sh     # 应输出 OK ... mysql_dump=一万多 bytes
> ls -la /opt/backup/mysql/                              # 确认是几十 KB，不是 20 字节空 gzip
> ```

`.env.prod` 和 `uploads/` 也要一起备份（前者丢了 JWT 密钥就换，后者丢了图片就没了）。

### 7.4 日常观察

```bash
$DC ps                     # 状态
docker stats --no-stream   # 各容器内存占用
free -h; df -h             # 内存 / 磁盘
$DC logs --tail=100 blog-server
docker system prune -f     # 偶尔清一下悬空镜像（构建多次后占盘）
```

### 7.5 别把服务器信息提交进仓库（重要）

服务器公网 IP、密钥、证书、`.env.prod` **都不应该进 git**——这个仓库是要推到 GitHub 的：

```bash
git check-ignore -v .env.prod      # 必须命中 .gitignore，否则先补规则再提交
git diff --cached | grep -niE "password|secret|token|([0-9]{1,3}\.){3}[0-9]{1,3}"   # 提交前扫一眼
```

- 真实 IP / 域名只写在**服务器上的 `.env.prod`**（`chmod 600`），文档和示例统一用 `SERVER_IP` 占位
- `nginx/certs/` 已加入 `.gitignore`，证书文件不要入库
- 上传的图片 `uploads/` 也在 `.gitignore` 里，不会进 git
- SSH 顺手加固：改用密钥登录、`PasswordAuthentication no`、有必要就改端口 + 装 fail2ban

> 说明：站点一旦上线，公网 IP 本身会通过 DNS、HTTP 等多处暴露，它不是凭据；但**上线前把 IP 写进公开仓库**等于主动告诉扫描器「这台机器值得试」，
> 而且 `root@` + 22 端口的组合信息也一并给了出去。所以能不放仓库就不放。

## 8. 内存配额（可调旋钮）

| 容器 | mem_limit | 实际峰值（参考） | 想省内存就调 |
|------|-----------|------------------|--------------|
| blog-mysql | 512m | 350~450MB | `--innodb-buffer-pool-size`（128M→64M） |
| blog-redis | 160m | 10~20MB | `--maxmemory`（128mb→64mb） |
| blog-server | 768m | 550~700MB | `JAVA_TOOL_OPTIONS` 里的 `-Xmx448m` → `-Xmx320m` |
| blog-web | 384m | 150~250MB | `NODE_OPTIONS=--max-old-space-size` 256 → 192 |
| blog-admin | 64m | ~10MB | —— |
| blog-nginx | 64m | ~10MB | —— |

调完 `$DC up -d`（重建受影响的容器即可）。如果 `docker inspect <容器> --format '{{.State.OOMKilled}}'` 出现 `true`，就是配额给小了。

## 9. 常见故障对照

| 现象 | 原因 | 处理 |
|------|------|------|
| blog-server 起不来，日志 `Could not resolve placeholder 'JWT_SECRET'` | 没配 `.env.prod` 或没加 `--env-file` | 补 `JWT_SECRET` 后 `$DC up -d blog-server` |
| 日志 `Access denied for user 'root'` | `.env.prod` 的密码和数据卷里的实际密码不一致（卷是上次初始化的） | 用回旧密码；或 `$DC down -v` 重来（**会清库**） |
| 首页能开但文章列表空 / SSR 报 500 | `NUXT_API_BASE` 没指到 `http://blog-server:8080` | 检查 compose 里 blog-web 的 `NUXT_API_BASE`（注意不是 `NUXT_PUBLIC_API_BASE`） |
| 图片 404 | `BLOG_STORAGE_TYPE` 不是 `local`，或 `./uploads:/app/uploads` 没挂上 | `$DC logs blog-server \| grep 存储`，确认输出「使用本地文件系统存储」 |
| `/admin/` 白屏 | 构建时 `VITE_BASE` 不是 `/admin/` | `$DC build blog-admin` 重来；检查镜像里 `dist/index.html` 的资源前缀 |
| 后端启动失败：`No qualifying bean of type 'io.minio.MinioClient'` | 用的是修复前的 jar（`storage.type=local` + `FileController` 强制注入 MinioClient） | 用本仓库当前代码重新 build（已修）；或先改回 `type: minio` 并启用 MinIO 服务 |
| 构建被 Killed / `JavaScript heap out of memory` | 2G 内存 + 无 swap | 开 swap；或调大 `NODE_BUILD_MEMORY`、改用下面附录的本地构建 |
| `docker compose build` 直接 panic：`slice bounds out of range [1:0]`（`build_bake.go:312`） | Compose v2 的 buildx bake 路径有 bug（v2.40.3 + Docker 28.3.3 实测必现） | `export COMPOSE_BAKE=false` 后重试；仍失败就逐个 `docker build`（见 §5） |
| 构建报 `[unocss:global:build:scan] cssPlugins.get(...).transform.call is not a function` | `@unocss/nuxt` 被降回 0.58（与 Vite 7 不兼容） | 改回 `^66`（本次已修，勿回退）后重新 build |
| 构建报 `npm error `npm ci` can only install packages when your package.json and package-lock.json ... are in sync` + `Missing: @rolldown/binding-linux-x64-musl from lock file` | lockfile 是用**比容器更新的 npm**（如本机 npm 12）生成的，缺少多平台可选依赖条目，容器里的 npm 10 不认 | 用与容器一致的 npm 重新生成 lockfile（见下） |
| 浏览器打不开 80 | 安全组没放通 | 控制台加规则 |
| `docker compose exec` 报 `service "blog-mysql" is not running`（但 `ps` 显示容器在跑） | `docker compose exec` 接的是**服务名**（本编排里是 `mysql`），不是容器名 `blog-mysql`；写容器名会得到这个误导性报错 | 用 `$DC exec -T mysql`（服务名），或 `docker exec -i blog-mysql`（容器名，cron 里更稳） |
| **部署/回滚后「API 正常但首页和后台 502」** | nginx 启动时缓存了 upstream 容器 IP，容器重建后 IP 变了（实测会对调），nginx 仍往旧 IP 转发 | 已在 `nginx/nginx.conf` 用 `resolver 127.0.0.11 valid=10s` + `server ... resolve` 修好；若用旧配置，临时 `docker exec blog-nginx nginx -s reload` 即可恢复 |
| MySQL 反复重启 | 被 OOM 杀 | `docker inspect blog-mysql --format '{{.State.OOMKilled}}'`，调大 mem_limit 或降 buffer pool |

## 10. 之后要上域名 + HTTPS

1. 域名 A 记录指向服务器公网 IP，安全组放通 443
2. 申请证书（`nginx/nginx.conf` 末尾注释块里有完整命令）
3. 证书放到 `nginx/certs/`（已加入 `.gitignore`）
4. `docker-compose.prod.yml` 里放开 `443:443` 并挂载证书目录，取消注释 `nginx/nginx.conf` 的 443 server 块
5. `SITE_URL` 改成 `https://你的域名`，重启 blog-web 与 nginx

因为前端已经改成同源 `/api`，挂 HTTPS 不会出现混合内容拦截——这也是本次把 `http://<hostname>:8080` 去掉的主要原因。

## 11. 更新服务器代码（发布 / 回滚）

### 11.1 先搞清楚"服务器上跑的是什么"

服务器上**没有 `.git`**，代码是用打包方式传上去的；**真正在跑的是镜像**。所以：

- 跑的不是某个分支/提交，而是**构建那一刻开发机工作区的内容**
- 版本记录写在服务器 `/opt/inkwell/DEPLOYED`（形如 `master@a1b2c3d  deployed=2026-10-13 10:20:00`）
- `docker compose up -d` 会重建**镜像变化**的服务，MySQL/Redis/nginx 不受影响

> 因此建议把 master 作为唯一部署源：在 dev 上开发 → 合并到 master → 发布前 `git checkout master && git pull`，不要在未提交的工作区状态下发布。

### 11.2 推荐流程（一条命令）

仓库里已经有脚本 [scripts/deploy.ps1](../scripts/deploy.ps1)：

```powershell
# 服务器地址刻意不写进仓库，用环境变量传（PowerShell）
$env:INKWELL_SERVER = '<你的服务器IP或域名>'

cd F:\other\code\ai\inkwell
git checkout master; git pull            # 要发布 master 上的代码就先切过来
pwsh scripts\deploy.ps1                  # 全部三个服务
pwsh scripts\deploy.ps1 -Services blog-web   # 只改了前端？只发布 C 端，秒级
```

脚本会依次做：① 本地构建（`COMPOSE_BAKE=false`，命中缓存时几秒）→ ② `docker save`（三个镜像约 220MB）→
③ **同步配置**（`docker-compose.prod.yml`、`nginx/nginx.conf`、`sql/init.sql`）→ ④ 在服务器上把当前版本打 `:prev` 标签（**回滚用**）→
⑤ `scp` + `docker load` + 写 `/opt/inkwell/DEPLOYED` → ⑥ `up -d --no-build` 只重建变化的容器，nginx 配置有变化时用
`nginx -t && nginx -s reload` **热加载（零停机）** → 最后验收首页/后台/API/RSS 并提示数据库校验风险。
工作区有未提交改动时它会黄色警告（发布的是文件内容而非提交）。

> **为什么要动态解析 upstream**：`nginx/nginx.conf` 里配了 `resolver 127.0.0.11 valid=10s` + upstream 的 `server blog-web:3000 resolve`。
> 这是实测踩出来的坑——容器重建后 Docker 会把 IP 重新分配（实测 blog-web/blog-admin 的 IP 直接**对调**），
> 而 nginx 默认只在**启动时解析一次** upstream，于是它一直往旧 IP 转发，表现为「API 正常、首页/后台全 502」。
> 加上动态解析后，部署/回滚都不再需要重启 nginx（需要 nginx ≥ 1.27.3，`nginx:alpine` 当前是 1.31.x）。

### 11.3 回滚

```powershell
pwsh scripts\deploy.ps1 -Rollback        # 把 :prev 标签还原成 :latest 并重启
```

上一版镜像一直留在服务器上（`:prev` 标签，只是多一个引用，不额外占空间），所以回滚 = 秒级。

### 11.4 手工等价命令（不想用脚本时）

```bash
# 开发机
$env:COMPOSE_BAKE='false'                      # bash: export COMPOSE_BAKE=false
docker compose --env-file .env.prod -f docker-compose.prod.yml build blog-server
docker save -o images.tar inkwell-blog-server
scp images.tar ubuntu@$SERVER_IP:/tmp/
# 服务器
cd /opt/inkwell
docker tag inkwell-blog-server:latest inkwell-blog-server:prev   # 先留后路
docker load -i /tmp/images.tar && rm -f /tmp/images.tar
docker compose --env-file .env.prod -f docker-compose.prod.yml up -d --no-build
curl -s -o /dev/null -w '%{http_code}\n' http://127.0.0.1/api/web/site-info
```

### 11.5 ⚠️ 改了实体/表结构必须先做数据库迁移

三个 profile 都是 `ddl-auto: validate`：**代码与表结构不一致时，新后端启动即校验失败并反复重启**（表现为站点 502 + 容器 restarting）。
`sql/init.sql` 只在数据卷为空时执行一次，**对已有库无效**。所以项目里有了一套正式的迁移机制：

| 位置 | 作用 |
|------|------|
| `sql/migrations/V<YYYYMMDDHHMMSS>__<desc>.sql` | 一条变更一个文件，按文件名排序执行 |
| `sql/migrations/README.md` | 命名、幂等、漂移等约定 |
| 服务器上的 `schema_migrations` 表 | 登记哪些迁移已应用 + 文件 sha256 |
| `scripts/migrate.ps1` | `-Status` 看清单；`-Apply` 备份 → 按序应用 → 登记 |

```powershell
# 1) 写迁移文件（同时记得改 sql/init.sql，让新库一步到位）
#    sql/migrations/V20261103101500__articles_add_cover_alt.sql
pwsh scripts/migrate.ps1 -Status     # 2) 看本地/已应用/待应用
pwsh scripts/migrate.ps1 -Apply      # 3) 备份 + 按序应用 + 登记（空备份会中止）
pwsh scripts/deploy.ps1              # 4) 再发布镜像
```

`deploy.ps1` 在第 0 步会**主动拦截**两种情况：① 服务器上有未应用的迁移；② 本次改动动了实体却没带迁移文件（除非显式 `-AllowSchemaChange`）。

急着手工做也行——但注意两个坑：

```bash
cd /opt/inkwell
DC="docker compose --env-file .env.prod -f docker-compose.prod.yml"
set -o pipefail                       # 坑②：不加 pipefail，mysqldump 失败会被 gzip 的退出码掩盖 → 你会得到一个"成功"的空备份
sudo mkdir -p /opt/backup && sudo chown "$(id -un):$(id -gn)" /opt/backup
# 注意：docker exec 没有 -T 参数（坑①），-T 是 docker compose exec 的
$DC exec -T mysql sh -c 'exec mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" --single-transaction --routines blog' \
  | gzip > /opt/backup/pre-migrate-$(date +%F-%H%M).sql.gz
sz=$(stat -c%s /opt/backup/pre-migrate-*.sql.gz | tail -1); [ "$sz" -gt 1024 ] || { echo "备份异常小，别继续"; exit 1; }
$DC exec -T mysql sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" blog' < sql/migrations/你的迁移.sql
```

判断是否踩到这个坑：`$DC logs blog-server | grep -iE "schema validation|SchemaManagementException|doesn't exist"`。

### 11.6 其它两条路（可选）

| 方式 | 做法 | 评价 |
|------|------|------|
| 服务器上直接构建 | 服务器 `git clone` 后 `export COMPOSE_BAKE=false; docker compose ... build` | 2C2G 上 Maven+Nuxt 构建约 10~25 分钟且**有 OOM 风险**（靠 1.9G swap 兜），不推荐日常用 |
| 镜像仓库 + CI | GitHub Actions 构建并推到**腾讯云 TCR**（TCR 有内网域名，服务器拉取快；Docker Hub 在服务器上不可达），服务器只 `docker compose pull && up -d` | 最正规，但要多维护一套 CI + 仓库；个人博客用 §11.2 的脚本足够 |

## 附录 A：本地构建镜像再传到服务器（服务器构建太慢/内存不够时）

```powershell
# Windows：先启动 Docker Desktop / Rancher Desktop
cd F:\other\code\ai\inkwell
copy .env.prod.example .env.prod   # 本地只为构建，值随便填
$env:COMPOSE_BAKE='false'          # 关掉会 panic 的 buildx bake
docker compose --env-file .env.prod -f docker-compose.prod.yml build
docker save inkwell-blog-server inkwell-blog-web inkwell-blog-admin -o inkwell-images.tar
scp inkwell-images.tar root@$SERVER_IP:/opt/
```

```bash
# 服务器
cd /opt/inkwell
docker load -i /opt/inkwell-images.tar
docker pull mysql:8.0; docker pull redis:7-alpine; docker pull nginx:alpine
$DC up -d --no-build
```

## 附录 B：本次改动清单（相对原仓库）

生产阻塞问题（不改就起不来/用不了）：

| 文件 | 问题 | 修复 |
|------|------|------|
| `docker-compose.yml` / 新增 `docker-compose.prod.yml` | `application-docker.yml` 要求 `${JWT_SECRET}`，原 compose 没注入 → 后端启动即失败 | prod 编排注入 `JWT_SECRET` |
| `docker-compose.prod.yml` | 原 compose 注入的是 `NUXT_PUBLIC_API_BASE`，SSR 用的是运行时 `NUXT_API_BASE` → SSR 连不上后端 | 改注入 `NUXT_API_BASE` |
| `sql/init.sql` | 硬编码 `ALTER USER 'root'@'%' ... BY 'root123'`，会覆盖 `.env` 的密码并留下弱口令 | 删除该语句，密码统一由 `MYSQL_ROOT_PASSWORD` 控制 |
| `blog-admin/vite.config.ts`、`src/router/index.ts`、`blog-admin/nginx.conf`、`Dockerfile` | 后台按根路径构建，却被 nginx 挂在 `/admin/` → 白屏、刷新 404 | 构建 base 支持 `VITE_BASE`，router 用 `BASE_URL`，容器内加 SPA fallback |
| `blog-web/composables/*.ts`、`layouts/default.vue`、`components/UserAvatar.vue`、`server/routes/rss.xml.ts` | 浏览器端硬编码 `http://<hostname>:8080` → 必须公网暴露 8080、HTTPS 下混合内容报错 | 统一改同源相对路径，SSR 走 `NUXT_API_BASE` |
| `blog-web/package.json` | `@unocss/nuxt@^0.58` + Nuxt 3.21（Vite 7）→ **`npm run build` 必然失败**，前端根本无法产出生产构建 | 升到 `@unocss/nuxt@^66`（支持 Vite 7）。项目里 UnoCSS 工具类用得极少（样式主要在 SCSS 里），升级对视觉影响很小 |
| `blog-web/package-lock.json`、`blog-web/Dockerfile`、`blog-admin/Dockerfile` | ① lockfile 由本机 npm 12 生成，容器内 npm 10 的 `npm ci` **直接拒绝安装**；② `node:20-alpine` 已 EOL 且部分依赖声明 `node >=22` | 用 `node:22-alpine` 的 npm 10 重新生成 lockfile；两个前端镜像基础镜像升到 `node:22-alpine` |
| `blog-server/.../FileController.java` | 构造函数强制注入 `MinioClient`，而 `storage.type=local` 时该 bean 不创建 → **后端启动直接失败**（`No qualifying bean of type 'io.minio.MinioClient'`）。也就是说：项目的 `prod` profile 默认值是 `local`，**开箱即崩**；不做 MinIO 的小内存部署方案根本不成立 | 改为 `ObjectProvider<MinioClient>`，本地存储模式下该代理端点返回 404，应用正常启动 |
| `docker-compose.yml` | 没挂载 `./uploads`，而正文插图永远写本地磁盘 → 容器一重建图片全丢 | 增加 `./uploads:/app/uploads` |

| `nginx/nginx.conf` | 没有 `/uploads/` 转发，本地存储模式下图片 404 | 增加 `/uploads/` → blog-server |
| `.gitignore` | `.env.prod` 会被提交（含密钥） | 忽略 `.env.prod`、`nginx/certs/` |

> 上表前 6 项是「配置/依赖」问题，第 7 项（FileController）是**真正的代码缺陷**——只有把整套服务真跑起来才会暴露，`docker compose config`、`npm run build` 都发现不了。

小内存适配：三个 Dockerfile 支持构建期内存上限（`NODE_BUILD_MEMORY`、`MAVEN_BUILD_MEMORY`）、npm 镜像源 `NPM_REGISTRY`；prod 编排给每个容器 `mem_limit` + 日志轮转；MySQL 关 `performance_schema`、限 buffer pool；不带 MinIO。

## 附录 C：本地全量冒烟测试记录（部署前预检）

在 Windows + Docker（8C/15.6G）上把 prod 编排整套跑起来做的验证，使用**独立数据卷**（不动已有数据）：

| 检查项 | 结果 |
|--------|------|
| 三个镜像按 Dockerfile 构建（Maven 容器内打包、`npm ci`、Nuxt/Vite 生产构建） | ✅ 全部 Built |
| MySQL 首次启动执行 `sql/init.sql`（建表 + 种子数据 + admin 账号） | ✅ 后端 12s 就绪 |
| C 端首页 SSR 真渲染出库里的种子文章 | ✅ 3 篇标题都在 HTML 里 |
| `/api/web/site-info`、`/api/web/articles` | ✅ 200 |
| `/admin/` + `/admin/assets/*`（子路径构建与转发） | ✅ 200，JS 1.1MB / CSS 362KB |
| 管理员登录 `/api/admin/auth/login`（JWT_SECRET 全链路） | ✅ code=200，token 192 字符 |
| RSS `/rss.xml` | ✅ 200 |
| SSR HTML 不含硬编码 `:8080`、静态资源走 `/_nuxt/` 同源相对路径 | ✅ |
| 图片上传（正文插图 + 封面）→ `/uploads/...` → 经 nginx 回读 | ✅ 200 image/png，文件确实落到宿主机 `uploads/` |
| 容器内存占用（空载） | server 314MB / mysql 189MB / web 27MB / admin 7.6MB / nginx 9MB / redis 4.4MB（合计约 550MB，2G 机器余量充足） |

复现方式（本机 80 被占用时用 8080）：

```bash
export COMPOSE_BAKE=false
NGINX_HTTP_PORT=8080 docker compose --env-file .env.prod -f docker-compose.prod.yml up -d
```

## 附录 D：本次实际部署实录（已上线）

**目标机器实况**：Ubuntu 24.04.4 LTS / 2 vCPU / 1967MB 内存 / 系统自带 1.9G swap / 50G 磁盘（用掉 15G）/ 默认登录用户是 **`ubuntu` 而不是 `root`**。
> 踩坑提醒：Lighthouse 的 Ubuntu 镜像默认用户是 `ubuntu`，把 SSH 公钥追加到 `~/.ssh/authorized_keys` 时，那个 `~` 是 `/home/ubuntu`，用 `root` 登录会一直 `Permission denied`。

**部署前遇到并处理的 3 个环境问题**：

| 问题 | 现象 | 处理 |
|------|------|------|
| 系统自带 **Caddy 占着 80** | `ss -ltnp` 显示 caddy 监听 `*:80`，但 `/etc/caddy/Caddyfile` 是出厂配置（`root * /usr/share/caddy` + `file_server`），只服务一个 "Welcome to Caddy" 静态页 | `systemctl stop/disable caddy`，把 80 让给 blog-nginx 容器 |
| **腾讯 Hermes Agent** 常驻 | 用户级 systemd `hermes-gateway.service`（python 263MB）+ 其 node MCP 插件（127MB），合计约 390MB | `systemctl --user stop/disable hermes-gateway`（`~/.hermes` 数据一律未动，可随时恢复） |
| **Docker Hub 不可达** | `registry-1.docker.io` 超时，`docker pull` 会失败 | ① 用 Ubuntu 自带源装 `docker.io`(29.1.3) + `docker-compose-v2`(2.40.3)，走腾讯内网 ubuntu 镜像；② `/etc/docker/daemon.json` 配 `"registry-mirrors": ["https://mirror.ccs.tencentyun.com"]`（内网，实测 200 且拉取正常） |

**镜像怎么上去的**：没有在服务器上构建（2G + 构建期 OOM 风险），而是**本地构建 + 搬运**：

```bash
# 本机（镜像已通过附录 C 的本地全量验证）
docker save -o inkwell-images.tar inkwell-blog-server inkwell-blog-web inkwell-blog-admin   # 221MB，8s
scp inkwell-images.tar ubuntu@$SERVER_IP:/tmp/                                              # 47s @4.7MB/s
# 服务器
cd /opt/inkwell && docker load -i /tmp/inkwell-images.tar                                   # 7s
docker compose --env-file .env.prod -f docker-compose.prod.yml up -d --no-build
```

**验收结果（全部通过）**：

| 检查项 | 结果 |
|--------|------|
| 6 个容器状态 | 全部 Up，MySQL/Redis/blog-server **healthy**，重启次数均为 0 |
| 后端就绪耗时 | 15s（含 MySQL 首次执行 init.sql） |
| 首页 / `/admin/` / `/api/web/site-info` / 文章列表 / `/rss.xml` / admin 静态资源 | 全部 HTTP 200 |
| SSR 渲染种子文章（3 篇标题）、文章详情页渲染 Markdown | ✅ |
| SSR HTML 无 `:8080` 残留；样式产物（5 个 CSS）全部 200，`.landing-page`/`.hero-section`/`.article-card`/`.article-content` 均在 | ✅ 说明 UnoCSS 升级未破坏外观 |
| 管理员登录 | ✅ code=200 |
| 图片上传 → `/opt/inkwell/uploads/` → 经 nginx 公网回读 | ✅ 200（测试文件已删） |
| 对外暴露端口 | 只有 **22 / 80**；8080、3306、6379、9000 从公网均不可达 ✅ |
| 发布脚本实测 | `scripts/deploy.ps1` 全流程跑通（构建命中缓存 → 传输 → 只重建变化的容器 → 验收 200）；`-Rollback` 也实测通过 |
| 发布踩坑（已修） | 回滚重建容器后 Docker 把 blog-web/blog-admin 的 IP **对调**，nginx 因只在启动时解析 upstream 而全部 502；改为 `resolver` + `resolve` 动态解析后，再次部署（容器同样被重建）全程 200 |
| 内存（运行时） | 容器合计约 555MB（server 306 / mysql 176 / web 59 / redis 7.5 / nginx 3.6 / admin 3.4）；系统 used 1230MB、available 736MB、swap 用了 125MB |
| 磁盘 | 50G 用 15G（含镜像约 2GB） |

**日常更新代码的流程**：见 [§11 更新服务器代码](#11-更新服务器代码发布--回滚)——用 `pwsh scripts/deploy.ps1` 一条命令完成构建、传输、重启与验收，回滚是 `-Rollback`。
> 注意：服务器上**没有 `.git`**，跑的是镜像；版本记录在 `/opt/inkwell/DEPLOYED`。目前这台机器上跑的是 `dev` 分支 + 当时未提交的部署修复，**不是 master**；把改动合并进 master 后再按 §11 发布即可。

**上线后待办**：① 改掉 `admin/admin123`；② 决定是否清理 14 篇演示文章；③ 配备份 cron（§7.3）；④ 有域名后按 §10 挂 HTTPS。

