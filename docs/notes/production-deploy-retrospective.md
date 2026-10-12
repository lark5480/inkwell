# Inkwell 生产上线复盘 — 腾讯云轻量 2C2G

> ⚠️ **本文是 2026-10-12 那次上线的过程复盘（快照），不是权威文档。** 文中的提交号、镜像/容器版本、内存与耗时都是当时实测值，会随时间失效。
> **权威的部署步骤、硬性约束与排障请以 [`docs/04`](../04-deploy-tencent-2c2g.md)、[`AGENTS.md`](../../AGENTS.md) 和源码为准。**

## 一、结果

| 项目 | 结果 |
|------|------|
| 站点 | `http://<服务器公网IP>/` — 首页、`/admin/`、`/api/web/*`、`/rss.xml` 全部 200 |
| 部署版本 | `dev@ba0811c`（服务器 `/opt/inkwell/DEPLOYED`） |
| 发布方式 | 本地构建镜像 → `docker save`（221MB）→ `scp`（47s @4.7MB/s）→ 服务器 `docker load`（7s） |
| 后端启动 | 15s 就绪（含 MySQL 首次执行 `sql/init.sql`） |
| 容器内存 | server 306MB / mysql 176MB / web 59MB / redis 7.5MB / nginx 3.6MB / admin 3.4MB ≈ **555MB** |
| 系统内存 | used 1230MB / available 736MB / swap 用 125MB（1.9G swap 是系统自带） |
| 对外端口 | 只有 22 / 80；8080、3306、6379、9000 从公网不可达 |

## 二、机器实况（和"我刚买的机器"不一样的地方）

| 项 | 实际值 |
|----|--------|
| 系统 | Ubuntu 24.04.4 LTS，2 vCPU，1967MB 内存，系统自带 1.9G `/swap.img` |
| 磁盘 | 50G（部署后用了 15G，镜像约 2GB） |
| **默认登录用户** | **`ubuntu`，不是 root** —— 公钥要追加到 `/home/ubuntu/.ssh/authorized_keys`；用 `root` 登录会一直 `Permission denied`，很容易误判成"公钥没生效" |
| 原有 Caddy | 占着 80，但 `/etc/caddy/Caddyfile` 是**出厂配置**（`root * /usr/share/caddy` + `file_server`），只服务一个 "Welcome to Caddy" 静态页。已 stop + disable |
| 原有 Hermes Agent | 用户级 systemd `hermes-gateway.service`（python 263MB）+ 其 node MCP 插件（127MB）≈ 390MB。已 stop + disable（`~/.hermes` 数据未动） |
| 其它常驻 | YunJing `YDService` 62MB、`barad_agent` 26MB、`multipathd` 26MB（腾讯云监控/安全组件，未动） |
| 网络 | **Docker Hub 不可达**（`registry-1.docker.io` 超时）；`mirror.ccs.tencentyun.com`（腾讯内网镜像）200 ✓；Maven Central 200 ✓；npmmirror 200 ✓ |

停掉 Caddy + Hermes 后：used 从 789MB 降到 504MB，available 1463MB —— 2G 机器上这 390MB 值得省。

## 三、为什么这么决策

| 决策 | 理由 |
|------|------|
| 不部署 MinIO，改用本地磁盘存储 | 省 200~300MB；图片走 `/uploads/**`（nginx 转发到后端）。前提是先修掉 `FileController` 强制依赖 `MinioClient` 的 bug |
| 不在服务器上构建，改为本地构建 + 搬运 | 2C2G 上跑 Maven + Nuxt 构建约 10~25 分钟且有 OOM 风险；实测本地上行 5.2MB/s，传 221MB 只要 47 秒 |
| 停 Caddy | 出厂欢迎页没有业务价值，却占着标准 80 端口 |
| 停 Hermes Agent | 2G 机器上近 400MB，用户确认不用 |
| 前端彻底同源化（去掉硬编码 `:8080`） | 否则必须公网暴露 8080、且以后挂 HTTPS 会被混合内容拦截 |

## 四、四个真正卡住人的问题（按发现顺序）

| # | 症状 | 根因 | 为什么之前没发现 |
|---|------|------|------------------|
| 1 | 后端启动即失败 | `application-docker.yml` 的 `${JWT_SECRET}` 无默认值，而 compose 从未注入 | 静态读配置就能发现（属于审查能覆盖的） |
| 2 | `npm run build` 必然失败 | `@unocss/nuxt@^0.58` 与 Nuxt 3.21 自带的 Vite 7 不兼容（`cssPlugins.get(...).transform.call is not a function`） | `npm run dev` 走的是另一条代码路径，能正常跑 → **"本地能跑、一构建就炸"** |
| 3 | 后端启动失败：`No qualifying bean of type 'io.minio.MinioClient'` | `StorageConfig` 在 `storage.type=local` 时不创建该 bean，而 `FileController` 构造函数强制注入它 | **只有真把服务起起来才会暴露**；`docker compose config`、`npm run build` 都发现不了。而 `application-prod.yml` 的默认值恰好就是 `local` → 项目 prod profile 开箱即崩 |
| 4 | 部署/回滚后「API 正常、首页和后台全 502」 | nginx 只在**启动时**解析一次 upstream 容器 IP；容器重建后 Docker 把 blog-web/blog-admin 的 IP **对调**了，nginx 仍往旧 IP 转发 | 第一次部署恰好 nginx 在应用容器之后启动，所以没暴露；**测回滚时才撞出来** |

第 3、4 条是这次最值得记的：**它们不在配置里、不在构建里，只在"真的跑一遍 + 真的回滚一次"里。**
修法：第 3 条改 `ObjectProvider<MinioClient>`；第 4 条给 upstream 加 `resolver 127.0.0.11 valid=10s` + `server ... resolve`（nginx ≥ 1.27.3）。

## 五、操作层面的坑（包括我自己踩的）

| 坑 | 现象 | 教训 |
|----|------|------|
| SSH 密钥被加密 | `ssh-keygen -N '""'` 在 PowerShell 里把 `""` 当成了**两个字符的口令**，私钥被 `aes256-ctr`+`bcrypt` 加密 → BatchMode 下 `Permission denied`，看着像公钥没装 | 要在非交互环境用，就别让口令参数经过 shell 解析；可用 .NET `ProcessStartInfo.ArgumentList` 精确传参（本次就是这么去掉口令的） |
| 无超时的 curl | `curl` 装 Docker 源那步挂住，整条命令静默卡死（apt 没在跑、文件也没写） | 脚本里所有网络调用都要 `--max-time/--connect-timeout` |
| `pkill -f` 自杀 | `pkill -f "<模式>"` 的模式出现在**自己这条命令的 cmdline 里**，把自己杀了（exit 1、后续输出全丢） | 用 `pgrep -af "hermes[-]agent"` 这类防自匹配写法，或先取 PID 再 kill |
| MySQL 密码"不生效" | 本地跑冒烟时报 `Access denied for user 'root'`，一度以为配置错了 | MySQL **只在数据卷为空时**读 `MYSQL_ROOT_PASSWORD`；那次数据卷是几个月前用老密码 `root123` 初始化的。生产上是新卷所以正常 —— 排障要先问"这个卷是哪次初始化的" |
| PowerShell 管道 heredoc | 用 `$script \| ssh host 'bash -s'` 时，PowerShell 会把换行写成 CRLF，远端 heredoc 终止符变成 `EOF\r` 匹配不上 | 远端先 `tr -d '\r'`，或者把脚本作为**参数**传（参数不做换行转换） |
| `docker exec -T` | 报 `unknown shorthand flag: 'T'` | `-T` 是 `docker compose exec` 的参数，`docker exec` 用 `-i` |
| **空备份假装成功** | 备份脚本"成功"，文件只有 **20 字节**（空 gzip）；`set -e` 完全没拦住 | `cmd \| gzip > f` 的退出码是 gzip 的，mysqldump 失败被吞掉。必须 `set -o pipefail` + 校验文件大小 + `gzip -t`。**没验证过的备份等于没有备份** |
| `/opt` 不可写 | `mkdir -p /opt/backup` 报 Permission denied | `/opt` 属 root；用 `sudo mkdir` + `sudo chown "$(id -un):$(id -gn)"` |

## 六、下次会不一样的地方

1. **用 CI 构建镜像推到腾讯云 TCR**，服务器只 `docker compose pull && up -d`（TCR 有内网域名，正好绕开 Docker Hub 不可达）
2. **固定基础镜像 digest**，别用 `node:22-alpine` 这种浮动 tag（今天 1.31.x 明天可能就变）
3. 上线前先把 `dev` 合到 `master`，**从提交发布而不是从工作区**（现在 `DEPLOYED` 会记录提交号，脏工作区会打 `+dirty(N)` 警告）
4. 把"验收"和"回滚"当作发布流程的固定部分（脚本已内置；`-Rollback` 也是因此才测出 502 那个 bug）
5. 数据库变更走 `sql/migrations/` + `scripts/migrate.ps1`（本次已补上：登记表 `schema_migrations`、发布前拦截、迁移前备份带完整性校验）

## 七、当时遗留的待办

- 改掉 `admin/admin123`（站点已公网可达）
- 决定是否清理 `sql/init.sql` 里的 14 篇演示文章 / 6 个演示分类
- 配置备份 cron（`docs/04` §7.3，注意其中的 pipefail + 大小校验）
