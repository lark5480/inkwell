# AGENTS.md

Instructions for AI agents (Claude Code, Codex, etc.) when working on this project.

> 本文件是 AI 协作指令的**唯一事实源**；`CLAUDE.md` 仅作为指向本文件的存根，请勿在两边重复维护。
> 部署步骤、排障细节见 `docs/03` / `docs/04`；本文件只放**命令**和**改代码前必须知道的硬性约束**。

## Commands

### blog-server (Java 17, Maven)

> ⚠️ 仓库**没有提交 Maven Wrapper**，`./mvnw` 会直接报 no such file。本地用系统 `mvn`；
> 想用 wrapper 先执行一次 `mvn -N wrapper:wrapper`（生成后可 `./mvnw`，但注意别把 wrapper 的 jar 混进构建上下文）。

```bash
cd blog-server
mvn spring-boot:run               # 启动开发服务器 (dev profile, 端口 8080)
mvn clean package -DskipTests     # 打包
mvn test                          # 运行测试
```

依赖：MySQL (localhost:3306, db:`blog`) + Redis (localhost:6379) + 文件存储。
`dev` profile 默认 `blog.storage.type: minio`（需要 localhost:9000）；不想装 MinIO 就把该属性改成 `local`（图片改走 `/uploads/**`，已支持）。

### blog-web (Nuxt 3)

```bash
cd blog-web
npm run dev       # 开发服务器 3000，devProxy 把 /api/** 转发到 8080
npm run build     # 生产构建
npm run preview   # 预览构建产物
```

### blog-admin (Vue 3 + Vite, SPA)

```bash
cd blog-admin
npm run dev       # 开发服务器 3001，vite proxy /api → 8080
npm run build     # 输出 dist/（`npm run build` 里带 vue-tsc 类型检查）
npm run preview   # 预览构建产物
```

### Docker

```bash
# 本地开发：只起中间件（后端/前端在 IDE 里跑）
docker compose up -d mysql redis minio

# 全量编排（含 MinIO）
docker compose up -d
docker compose logs -f
docker compose down
```

> ⚠️ **构建前必须关掉 buildx bake**：Compose v2 的 bake 路径在构建 Java 镜像时会 panic
> （`build_bake.go:312`，`panic: runtime error: slice bounds out of range [1:0]`）。
> `export COMPOSE_BAKE=false`（Windows PowerShell：`$env:COMPOSE_BAKE='false'`）。
> **"先 build 再 up" 不能绕过**——panic 就发生在 `docker compose build` 里。

### Database

```bash
mysql -u root -p blog < sql/init.sql   # 建表 + 种子数据（仅空库）
```

> ⚠️ `sql/init.sql` 开头是 `DROP TABLE`，**只对空库/新数据卷有意义**；对已有数据的库执行会清库。

**已有库的变更走迁移**（详见 `sql/migrations/README.md`）：

```powershell
pwsh scripts/migrate.ps1 -Status     # 本地迁移 vs 服务器已应用
pwsh scripts/migrate.ps1 -Apply      # 备份 → 按序应用 → 登记到 schema_migrations 表
```

新增表/字段要同时改两处：`sql/init.sql`（新库一步到位）+ 一条 `sql/migrations/V<YYYYMMDDHHMMSS>__<desc>.sql`（老库跟上）。

## 硬性约束（改动前必读，破坏这些会直接搞挂本地/线上）

| 约束 | 为什么（都是实测踩出来的） |
|------|---------------------------|
| 前端访问后端一律用**同源相对路径**（`/api`、`/uploads`），禁止硬编码 `http://<host>:8080` | 硬编码会让 8080 必须对外暴露、HTTPS 下被混合内容拦截，而且 SSR 与浏览器端产出的 URL 不一致会导致 hydration 失败 |
| 前端加/升级依赖后，**lockfile 必须用与容器一致的 npm 生成** | 本机 npm 12 生成的 lockfile 会让容器内 npm 10 的 `npm ci` 直接拒绝：`Missing: @rolldown/binding-linux-x64-musl from lock file`。正确做法：<br>`docker run --rm -v "$PWD/blog-web:/w" -w /w node:22-alpine npm install <包> --package-lock-only` |
| 前端基础镜像保持 `node:22-alpine`；`@unocss/nuxt` 不得降回 `^0.58` | 0.58 与 Nuxt 3.21 自带的 Vite 7 不兼容，`npm run build` 必然失败：`[unocss:global:build:scan] cssPlugins.get(...).transform.call is not a function`（`npm run dev` 却不报，属于"本地能跑、一构建就炸"） |
| `nginx/nginx.conf` 的 upstream 必须保留 `resolver 127.0.0.11 valid=10s` + `server <svc>:<port> resolve` | 否则 nginx 只在启动时解析一次容器 IP；容器重建后 IP 会变（实测 blog-web/blog-admin 直接**对调**），表现为「API 正常、首页/后台 502」 |
| `blog.storage.type=local` 必须能启动：`FileController` 保持用 `ObjectProvider<MinioClient>`，不要改回构造函数强制注入 | `local` 模式下 `StorageConfig` 不创建 `MinioClient` bean，强制注入会让应用启动失败（`UnsatisfiedDependencyException`）。`application-prod.yml` 默认值就是 `local` |
| 正文插图（`upload-image-content`）永远写容器内 `./uploads`，compose 必须挂载 `./uploads:/app/uploads` | 这条与是否启用 MinIO **无关**（该接口直接 `new LocalFileStorageService()`）；不挂载则容器一重建图片全丢 |
| 改实体/表结构必须配套迁移文件（`sql/migrations/V<时间戳>__<描述>.sql`），并先 `pwsh scripts/migrate.ps1 -Apply` 再发布 | 三个 profile 都是 `ddl-auto: validate`，结构不一致时新后端启动即失败并反复重启（站点 502）。`scripts/deploy.ps1` 会主动拦截：有待应用迁移、或动了实体却没带迁移文件 → 拒绝发布 |
| `.env.prod`、TLS 证书、真实服务器地址**不入库**（`.gitignore` 已覆盖）；发布脚本用 `$env:INKWELL_SERVER` 传地址 | 仓库是公开的；服务器 IP 上线前泄露等于给扫描器指路 |
| 后台按 `/admin/` 子路径构建（`VITE_BASE=/admin/` + router 用 `import.meta.env.BASE_URL`） | nginx 用 `location /admin/` 反代并剥掉前缀，按根路径构建会白屏 + 刷新 404 |
| 调 API 做验证时，**必须看 body 里的 `code`，不能只看 HTTP 状态码** | 本项目统一用 `Result<T> = {code,message,data}` 信封：登录失败等业务错误**也是 HTTP 200**，只有 body 里的 `code=401`。用 `curl -w '%{http_code}'` 判断会得出完全相反的结论（实测踩到：改完密码后用旧密码登录仍显示 HTTP 200，差点误判为"改密没生效"） |
| 判断分支关系/能否快进前，先 `git fetch --prune`；发布只从 `master` 的提交发，别从工作区发 | 本仓库 master 是通过 PR 合并的（已有 #1~#13），本地 master 引用很容易陈旧；`deploy.ps1` 会把脏工作区标成 `+dirty(N)`，用提交号才能和 `DEPLOYED` 对账 |

## 发布 / 上线

```powershell
$env:INKWELL_SERVER = '<服务器IP或域名>'   # 刻意不写进仓库
pwsh scripts/deploy.ps1                     # 构建 → 传输 → 同步配置 → 重启 → 验收
pwsh scripts/deploy.ps1 -Services blog-web  # 只发前端
pwsh scripts/deploy.ps1 -Rollback           # 回滚到上一版（:prev 标签）
```

工作流：日常在 `dev` 开发 → 上线前合到 `master` → 在 `master` 上跑发布脚本。

## Docs

- `docs/01-getting-started.md` — 环境要求、快速启动
- `docs/02-architecture.md` — 系统架构、分层设计
- `docs/03-deployment.md` — Docker 部署、Nginx 配置
- `docs/04-deploy-tencent-2c2g.md` — 小内存（2C2G）生产上线、内存配额、更新与回滚、故障对照
- `docs/notes/` — **快照笔记，允许滞后，不作为权威来源**：
  - `interview-qa.md` — Redis 一致性、全文检索、JPA、JWT 等实现笔记
  - `production-deploy-retrospective.md` — 2C2G 上线复盘（时间线、事故分析、实测数据）

## Git Workflow

```bash
# 分支（实际使用的两个）
master            # 发布分支：**已启用分支保护，只能通过 PR 合并**（历史上有 PR #1~#14）
dev               # 唯一允许直接提交的分支；feature/* / fix/* 合到这里
```

> ⚠️ **禁止直接在 master 上改代码/提交**（文档也一样）。master 已开启 GitHub 分支保护：
> `required_pull_request_reviews`（0 个批准，自己就能合）+ `enforce_admins: true`（连仓库管理员也拦）
> + 禁止强推与删分支。实测直推会得到：
>
> ```
> remote: error: GH006: Protected branch update failed for refs/heads/master.
> remote: - Changes must be made through a pull request.
> ```
>
> 查看/临时关闭（**只在紧急情况用，用完立刻恢复**）：
>
> ```bash
> gh api repos/lark5480/inkwell/branches/master/protection --jq '{pr:(.required_pull_request_reviews!=null), admin:.enforce_admins.enabled}'
> gh api -X DELETE repos/lark5480/inkwell/branches/master/protection
> ```

发布流程（**不要**本地快进后直推 master——会被保护规则拒绝，且 master 上有 PR 合并提交、本地引用很容易陈旧）：

```bash
git fetch --prune                       # 先刷新，再谈能不能快进
git checkout dev && git status -sb      # ★ 提交前先确认自己在哪个分支
git add ... && git commit -m "..."      # 日常提交只提交到 dev
git push origin dev

# 上线：开 PR 并合并（单人仓库也别省这一步，合并记录就是发布记录）
gh pr create --base master --head dev --title "..." --body "..."
gh pr merge <编号> --merge

# 发布：从 master 的提交发，而不是从工作区
git checkout master && git pull --ff-only
pwsh scripts/deploy.ps1                 # DEPLOYED 会记录 master@<merge-commit>
git checkout dev                        # ★ 发完切回来，否则下一次提交会落在 master 上
```

> 实测踩过：从 master 发布后忘记切回 dev，下一个提交就落在了本地 master 上，
> 还导致 `gh pr create` 报 "No commits between master and dev"。**提交前 `git status -sb`，发布后切回 dev。**

```bash
# commit 规范
feat:|fix:|docs:|refactor:|chore:
```
