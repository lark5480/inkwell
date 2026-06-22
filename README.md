# Inkwell — Multi-author Blog Platform

基于 Spring Boot 3 + Nuxt 3 + Vue 3 的多作者博客平台。

## Tech Stack

| 模块 | 技术 | 端口 |
|------|------|------|
| **blog-server** | Spring Boot 3.2 + JPA + Security + Redis | 8080 |
| **blog-web** (C端) | Nuxt 3 SSR + UnoCSS + Pinia | 3000 |
| **blog-admin** (B端) | Vue 3 + Element Plus + Pinia + Axios | 3001 |
| **Database** | MySQL 8.0 | 3306 |
| **Cache** | Redis 7 | 6379 |
| **Storage** | MinIO (S3 兼容) | 9000/9001 |

## Quick Start

### Prerequisites

- Java 17+, Maven 3.8+
- Node.js 18+
- MySQL 8.0, Redis 7
- Docker 20+ (optional)

### 1. Start Infrastructure

```bash
# 本地开发：需要本地运行 MySQL (localhost:3306, db: blog) 和 Redis (localhost:6379)
# 或用 Docker 启动：
docker run -d --name blog-mysql -e MYSQL_ROOT_PASSWORD=root -e MYSQL_DATABASE=blog -p 3306:3306 mysql:8.0
docker run -d --name blog-redis -p 6379:6379 redis:7-alpine
```

### 2. Initialize Database

```bash
mysql -u root -p blog < sql/init.sql
```

### 3. Start Backend

```bash
cd blog-server
./mvnw spring-boot:run
# → http://localhost:8080
```

### 4. Start C端 Frontend

```bash
cd blog-web
npm install
npm run dev
# → http://localhost:3000
```

### 5. Start B端 Admin

```bash
cd blog-admin
npm install
npm run dev
# → http://localhost:5173 (dev) / http://localhost:3001 (docker)
```

### Docker Compose (一站式)

```bash
docker compose up -d
# → blog-web: http://localhost:3000
# → blog-admin: http://localhost:3001
# → API: http://localhost:8080
```

Default admin: `admin` / `admin123`

## Project Structure

```
├── blog-admin/          # B端管理后台 (Vue 3 SPA)
│   ├── src/
│   │   ├── api/         # Axios API modules
│   │   ├── views/       # Page components
│   │   ├── router/      # Vue Router
│   │   ├── stores/      # Pinia stores
│   │   └── layouts/     # Layout components
│   └── Dockerfile
├── blog-server/         # 后端 API (Spring Boot)
│   ├── src/main/java/com/blog/
│   │   ├── controller/  # admin/ + web/
│   │   ├── service/     # Business logic
│   │   ├── repository/  # JPA repositories
│   │   ├── entity/      # JPA entities
│   │   ├── dto/         # Java records
│   │   ├── config/      # Security, CORS, Redis
│   │   └── security/    # JWT auth
│   └── Dockerfile
├── blog-web/            # C端前台 (Nuxt 3 SSR)
│   ├── pages/           # Route pages
│   │   ├── index.vue           # 首页
│   │   ├── article/[slug].vue  # 文章详情
│   │   ├── article/edit/[id].vue # 编辑文章
│   │   ├── category/[slug].vue # 分类页
│   │   ├── tag/[slug].vue      # 标签页
│   │   ├── user/[id].vue       # 用户公开主页
│   │   ├── messages/           # 私信/通知中心
│   │   ├── write.vue           # 写文章
│   │   ├── my-articles.vue     # 我的文章管理
│   │   ├── notifications.vue   # 通知 (重定向到 /messages)
│   │   ├── search.vue          # 搜索
│   │   ├── archive.vue         # 归档
│   │   └── login/register/about/links/tags.vue
│   ├── composables/     # useAuth, useBlogApi, useTheme, useI18n
│   ├── components/      # TocBlock, CommentActionBar, VoteButton, MoreMenu 等
│   ├── middleware/       # auth.ts (SSR 守卫)
│   └── layouts/         # default.vue 布局
├── sql/
│   └── init.sql         # Database schema + seed data
├── nginx/
│   └── nginx.conf       # Nginx reverse proxy config
├── docs/                # Documentation
└── docker-compose.yml   # Full-stack orchestration
```

## API Overview

### C端 `/api/web/**` (公开，部分功能需登录)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/web/articles` | 文章列表 (分页，支持 categoryId/tagSlug 筛选) |
| GET | `/api/web/articles/{slug}` | 文章详情 |
| GET | `/api/web/articles/{slug}/prev-next` | 上下篇导航 |
| POST | `/api/web/articles/{slug}/like` | 点赞/取消点赞 (登录用户 toggle) |
| GET | `/api/web/categories` | 分类列表 |
| GET | `/api/web/tags` | 标签列表 |
| GET | `/api/web/links` | 友链列表 |
| GET | `/api/web/comments?articleId=` | 评论列表 (含赞踩统计) |
| POST | `/api/web/comments` | 提交评论 |
| POST | `/api/web/comments/{id}/vote` | 赞/踩投票 |
| POST | `/api/web/comments/{id}/report` | 举报评论 |
| DELETE | `/api/web/comments/{id}` | 删除自己的评论 |
| POST | `/api/web/users/{id}/block` | 拉黑用户 |
| DELETE | `/api/web/users/{id}/block` | 取消拉黑 |
| GET | `/api/web/search?q=` | 搜索文章 (FULLTEXT + ngram) |
| GET | `/api/web/archives` | 文章归档 |
| GET | `/api/web/site-info` | 站点信息 |
| GET | `/api/web/rss.xml` | RSS 订阅 |
| GET | `/api/web/sitemap.xml` | Sitemap |
| POST | `/api/web/auth/login` | 登录 |
| POST | `/api/web/auth/register` | 注册 |
| GET | `/api/web/auth/me` | 当前用户信息 |

### C端 `/api/web/user/**` (需登录)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/web/user/articles` | 创建文章 |
| GET | `/api/web/user/articles` | 我的文章列表 (分页，支持 status/keyword) |
| GET | `/api/web/user/articles/{id}` | 我的文章详情 |
| PUT | `/api/web/user/articles/{id}` | 更新文章 |
| DELETE | `/api/web/user/articles/{id}` | 删除文章 |
| POST | `/api/web/user/articles/upload-image` | 上传文章图片 |
| GET | `/api/web/user/history` | 浏览记录 (分页) |
| DELETE | `/api/web/user/history/{id}` | 删除浏览记录 |
| GET | `/api/web/user/likes` | 点赞记录 (分页) |
| GET | `/api/web/user/profile` | 我的资料 |
| PUT | `/api/web/user/profile` | 更新资料 |

### C端 `/api/web/users/**` (公开)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/web/users/{id}` | 用户公开主页 |
| GET | `/api/web/users/{id}/articles` | 用户公开文章列表 |
| POST | `/api/web/users/{id}/follow` | 关注/取消关注 |
| GET | `/api/web/users/{id}/followers` | 关注者列表 |
| GET | `/api/web/users/{id}/following` | 正在关注列表 |
| GET | `/api/web/users/{id}/follow-status` | 关注状态查询 |

### C端通知/私信 (需登录)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/web/user/notifications` | 通知列表 (支持 ?type=COMMENT/LIKE/FOLLOW 筛选) |
| GET | `/api/web/user/notifications/unread-count` | 未读通知数 |
| PUT | `/api/web/user/notifications/read-all` | 全部标记已读 |
| PUT | `/api/web/user/notifications/{id}/read` | 标记单个已读 |
| GET | `/api/web/messages/conversations` | 会话列表 |
| GET | `/api/web/messages?userId=` | 与某人消息历史 |
| POST | `/api/web/messages` | 发送消息 |
| PUT | `/api/web/messages/{id}/read` | 标记消息已读 |
| PUT | `/api/web/messages/read-all?userId=` | 标记与某人的消息全部已读 |

### B端 `/api/admin/**` (需 Bearer Token)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/admin/auth/login` | 管理员登录 |
| GET/POST | `/api/admin/articles` | 文章列表 / 创建 |
| PUT/DELETE | `/api/admin/articles/{id}` | 更新 / 删除文章 |
| GET/POST | `/api/admin/categories` | 分类列表 / 创建 |
| PUT/DELETE | `/api/admin/categories/{id}` | 更新 / 删除分类 |
| GET/POST | `/api/admin/tags` | 标签列表 / 创建 |
| PUT/DELETE | `/api/admin/tags/{id}` | 更新 / 删除标签 |
| GET | `/api/admin/comments` | 评论管理 |
| GET/PUT | `/api/admin/reports` | 举报管理 |
| GET/POST | `/api/admin/links` | 友链列表 / 创建 |
| PUT/DELETE | `/api/admin/links/{id}` | 更新 / 删除友链 |
| GET/PUT | `/api/admin/settings` | 站点配置 |
| GET | `/api/admin/users` | 用户管理 |
| GET | `/api/admin/stats/overview` | Dashboard 统计 |

## Architecture

```
                    ┌─────────────┐
                    │   Nginx     │ (80)
                    │ 反向代理     │
                    └──────┬──────┘
              ┌────────────┼────────────┐
              ▼            ▼            ▼
        ┌──────────┐ ┌──────────┐ ┌──────────┐
        │ blog-web │ │blog-admin│ │ blog-srv │
        │ Nuxt SSR │ │ Vue SPA  │ │ Spring   │
        │ :3000    │ │ :3001/80 │ │ :8080    │
        └──────────┘ └──────────┘ └─────┬────┘
                                        │
                    ┌───────────┬────────┴────────┐
                    ▼           ▼                 ▼
              ┌──────────┐ ┌──────────┐    ┌──────────┐
              │  MySQL   │ │  Redis   │    │  MinIO   │
              │ :3306    │ │ :6379    │    │:9000/9001│
              └──────────┘ └──────────┘    └──────────┘
```

## Key Features

- **多作者平台**: 用户注册后可撰写、编辑、管理自己的文章
- **用户主页**: 公开主页展示文章列表、浏览记录、点赞记录、关注/粉丝
- **评论互动**: 评论支持赞/踩二元投票、回复、举报、拉黑，Bilibili 风格扁平回复
- **通知中心**: 收到回复、收到点赞、新增粉丝、私信 → 铃铛未读提醒，分类查看
- **私信系统**: 站内私信，会话列表，未读标记
- **点赞 Toggle**: 登录用户每人每篇只能赞一次，再点取消；评论也可点赞
- **浏览记录**: 自动记录登录用户的文章浏览历史
- **Redis 高性能**: 浏览量/点赞增量写 Redis，定时回写 MySQL
- **全文搜索**: MySQL FULLTEXT + ngram，支持中文
- **黑名单**: 拉黑用户后可屏蔽其评论

## Key Conventions

- **统一响应**: `Result<T>` = `{ code, message, data }`
- **分页响应**: `PageDTO<T>` = `{ records, total, page, pageSize }`
- **逻辑删除**: `is_deleted` 字段 + `@SQLRestriction`
- **JWT 认证**: B端必须 Bearer Token，C端可选
- **DTO 隔离**: Entity 不暴露到 Controller，用 Java record
- **Markdown 渲染**: flexmark 服务端渲染，Redis 缓存 HTML
- **缓存一致性**: 写操作通过 `@CacheEvict` 清除相关缓存

## Deployment

### Docker Compose (推荐)

```bash
# 启动全部服务
docker compose up -d

# 查看日志
docker compose logs -f

# 重建特定服务
docker compose build blog-server && docker compose up -d blog-server

# 停止
docker compose down
```

### 传统部署

各模块独立部署，见各模块 README 或 `docs/deployment.md`。

## License

MIT
