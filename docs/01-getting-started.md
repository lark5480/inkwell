# Getting Started

## 环境要求

| 工具 | 版本 |
|------|------|
| Java | 17+ |
| Maven | 3.8+ |
| Node.js | 18+ |
| MySQL | 8.0 |
| Redis | 7+ |
| Docker (可选) | 20+ |

## Docker Compose 部署（推荐）

快速体验全部服务：

```bash
# 启动所有服务（MySQL/Redis/后端/C端/B端/Nginx）
docker compose up -d

# 查看状态
docker compose ps

# 访问
# C端前台: http://localhost:3000
# B端后台: http://localhost:3001 (admin/admin123)
# API: http://localhost:8080
```

## 传统部署

### 1. 启动基础设施

```bash
# MySQL (端口 3306)
# 数据库: blog, 用户: root, 密码: root123

# Redis (端口 6379)
```

或一键启动：

```bash
docker compose up -d mysql redis
```

### 2. 初始化数据库

```bash
# 执行建表脚本
mysql -u root -p blog < sql/init.sql
```

### 3. 启动后端

```bash
cd blog-server
./mvnw spring-boot:run
# 启动在 http://localhost:8080
```

### 4. 启动 C 端 (Nuxt 3)

```bash
cd blog-web
npm install
npm run dev
# 启动在 http://localhost:3000
```

### 5. 启动 B 端 (Vue 3 管理后台)

```bash
cd blog-admin
npm install
npm run dev
# 启动在 http://localhost:3001
```

### 6. 访问

| 端 | 地址 |
|---|------|
| C端 (前台) | http://localhost:3000 |
| B端 (管理后台) | http://localhost:3001 |
| API | http://localhost:8080/api |

**管理员账号**：`admin` / `admin123`

## 项目结构

```
personal-blog/
├── blog-server/       # Spring Boot 3 后端 (端口 8080)
│   ├── src/main/java/com/blog/
│   │   ├── config/        # Security, CORS, Redis 配置
│   │   ├── controller/
│   │   │   ├── admin/     # B端 API (/api/admin/**)
│   │   │   └── web/       # C端 API (/api/web/**)
│   │   ├── service/       # 业务逻辑 (含通知、拉黑、举报过滤)
│   │   ├── repository/    # JPA 数据访问
│   │   ├── entity/        # 数据模型 (含 ArticleLike, ArticleViewHistory)
│   │   ├── dto/           # 请求/响应 DTO (Java records)
│   │   ├── security/      # JWT 认证
│   │   ├── storage/       # 文件存储 (MinIO / 本地)
│   │   ├── schedule/      # 定时任务 (Redis 回写)
│   │   └── exception/     # 全局异常处理
│   └── src/main/resources/
│       ├── application.yml      # 主配置 (含 MinIO 存储配置)
│       ├── application-dev.yml  # 开发环境配置
│       └── application-docker.yml # Docker 环境配置
│
├── blog-web/          # Nuxt 3 C端 SSR (端口 3000)
│   ├── pages/         # 页面路由 (20 个页面)
│   ├── components/    # 公共组件 (TocBlock, MarkdownRenderer)
│   ├── composables/   # useAuth, useBlogApi, useTheme
│   ├── middleware/     # auth.ts (认证守卫)
│   ├── layouts/       # default.vue 布局
│   └── server/        # RSS 等服务端路由
│
├── blog-admin/        # Vue 3 + Element Plus B端 SPA (端口 3001)
│   ├── src/
│   │   ├── views/     # 页面
│   │   ├── stores/    # Pinia 状态管理
│   │   ├── api/       # Axios 封装
│   │   └── router/    # 路由守卫
│   └── vite.config.ts
│
├── sql/               # 数据库初始化脚本
├── docs/              # 文档
├── nginx/             # Nginx 反向代理配置
├── .env.example       # 环境变量模板
└── docker-compose.yml # 全栈编排
```

## 常用命令

### 后端

```bash
cd blog-server
./mvnw spring-boot:run                  # 启动
./mvnw clean package -DskipTests        # 打包
./mvnw test                             # 测试
```

### C端

```bash
cd blog-web
npm run dev       # 开发模式 (端口 3000)
npm run build     # 构建 (SSR)
npm run preview   # 预览构建产物
```

### B端

```bash
cd blog-admin
npm run dev       # 开发模式 (端口 3001)
npm run build     # 构建 (输出到 dist/)
npm run preview   # 预览构建产物
```

### Docker

```bash
docker compose up -d      # 启动全部服务
docker compose down       # 停止
docker compose logs -f    # 查看日志
```

## 开发说明

### API 响应格式

```json
// 成功
{ "code": 200, "message": "success", "data": { ... } }

// 分页
{ "code": 200, "message": "success",
  "data": { "records": [...], "total": 100, "page": 1, "pageSize": 10 } }

// 错误
{ "code": 40001, "message": "错误信息", "data": null }
```

### C 端页面路由

| 路径 | 说明 |
|------|------|
| `/` | 首页 |
| `/article/:slug` | 文章详情 |
| `/article/edit/:id` | 编辑文章 |
| `/category/:slug` | 分类文章 |
| `/categories` | 分类总览 |
| `/tag/:slug` | 标签文章 |
| `/user/:id` | 用户公开主页 (文章/关注/粉丝/历史) |
| `/settings` | 个人设置 (头像/昵称/签名/修改密码) |
| `/messages` | 消息中心 (私信会话 + 通知分类，含各类型未读数) |
| `/messages/:userId` | 私信对话 |
| `/write` | 写文章 |
| `/my-articles` | 我的文章管理 |
| `/archive` | 归档 |
| `/search` | 搜索 |
| `/tags` | 标签云 |
| `/links` | 友链 |
| `/about` | 关于 |
| `/login` | 登录 |
| `/register` | 注册 |

### B 端页面路由

| 路径 | 说明 |
|------|------|
| `/login` | 登录 |
| `/dashboard` | 仪表盘 |
| `/articles` | 文章列表 |
| `/articles/edit/:id?` | 编辑/新建文章 |
| `/categories` | 分类管理 |
| `/tags` | 标签管理 |
| `/comments` | 评论管理 |
| `/reports` | 举报管理 |
| `/links` | 友链管理 |
| `/settings` | 站点设置 |

### API 端点

**C 端** `/api/web/**` — 公开访问，部分功能需登录

- `GET /api/web/articles` — 文章列表（分页/分类/标签筛选）
- `GET /api/web/articles/:slug` — 文章详情
- `GET /api/web/articles/:slug/prev-next` — 上下篇导航
- `POST /api/web/articles/:slug/like` — 点赞/取消点赞
- `GET /api/web/categories` — 全部分类
- `GET /api/web/tags` — 全部标签
- `GET /api/web/links` — 友链列表
- `GET /api/web/comments?articleId=` — 文章评论（登录时已过滤拉黑用户）
- `POST /api/web/comments` — 发表评论
- `GET /api/web/search?q=` — 搜索文章
- `GET /api/web/site-info` — 站点信息
- `GET /api/web/archives` — 文章归档
- `GET /api/web/rss.xml` — RSS 订阅
- `GET /api/web/sitemap.xml` — Sitemap
- `POST /api/web/auth/login` — 登录
- `POST /api/web/auth/register` — 注册
- `GET /api/web/auth/me` — 当前用户
- `POST /api/web/comments/{id}/vote` — 赞/踩投票
- `POST /api/web/comments/{id}/report` — 举报评论
- `DELETE /api/web/comments/{id}` — 删除自己的评论
- `POST /api/web/users/{id}/block` — 拉黑用户
- `DELETE /api/web/users/{id}/block` — 取消拉黑
- `POST /api/web/users/{id}/follow` — 关注/取消关注
- `GET /api/web/users/search?q=` — 搜索用户
- `GET /api/web/messages/conversations` — 会话列表
- `GET /api/web/messages?userId=&page=&pageSize=` — 聊天记录
- `POST /api/web/messages` — 发送私信
- `GET /api/web/messages/unread-count` — 私信未读数

**C 端（需登录）** `/api/web/user/**`

- `POST /api/web/user/articles` — 创建文章
- `GET /api/web/user/articles` — 我的文章列表
- `GET /api/web/user/articles/:id` — 我的文章详情
- `PUT /api/web/user/articles/:id` — 更新文章
- `DELETE /api/web/user/articles/:id` — 删除文章
- `POST /api/web/user/articles/upload-image` — 上传文章图片
- `GET /api/web/user/history` — 浏览记录
- `DELETE /api/web/user/history/:id` — 删除浏览记录
- `GET /api/web/user/likes` — 点赞记录
- `GET /api/web/user/profile` — 我的资料
- `PUT /api/web/user/profile` — 更新资料（昵称/签名）
- `POST /api/web/user/avatar` — 上传头像（前端裁剪后上传到 MinIO）
- `PUT /api/web/user/password` — 修改密码
- `GET /api/web/user/blocks` — 已拉黑的用户列表（支持取消拉黑）

**C 端（公开）** `/api/web/users/**`

- `GET /api/web/users/:id` — 用户公开主页（含文章数/关注数）
- `GET /api/web/users/:id/articles` — 用户公开文章列表
- `GET /api/web/users/:id/followers` — 粉丝列表
- `GET /api/web/users/:id/following` — 关注列表
- `GET /api/web/users/:id/follow-status` — 关注状态

**C 端（文件代理）** `/api/web/files/{bucket}/**`

- `GET /api/web/files/{bucket}/...` — 通过后端代理访问 MinIO 文件（开发环境）

**B 端** `/api/admin/**` — 需要 Bearer Token，ADMIN 角色

- `POST /api/admin/auth/login` — 登录
- `GET/POST /api/admin/articles` — 文章管理
- `PUT/DELETE /api/admin/articles/:id` — 更新/删除文章
- `GET/POST /api/admin/categories` — 分类管理
- `GET/POST /api/admin/tags` — 标签管理
- `GET /api/admin/comments` — 评论审核
- `GET/PUT /api/admin/reports` — 举报管理
- `GET/POST /api/admin/links` — 友链管理
- `GET/PUT /api/admin/settings` — 站点配置
- `GET /api/admin/users` — 用户管理
- `GET /api/admin/stats/overview` — 统计概览

### 后端配置

关键配置在 `application.yml`：

```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/blog
    username: root
    password: root123
  data:
    redis:
      host: localhost
      port: 6379
  jpa:
    hibernate:
      ddl-auto: validate    # 用 validate 确保表结构与 Entity 一致
    show-sql: false

jwt:
  secret: <base64-encoded-secret>
  expiration: 7200000       # 2h
```

### 后端规范

- `@RestControllerAdvice` 全局异常处理
- Entity 通过 DTO 传递，不暴露到 Controller
- DTO 用 Java record + 静态工厂方法
- 逻辑删除 `@SQLRestriction("is_deleted = 0")`
- N+1 用 `@EntityGraph` 或 `JOIN FETCH` 解决
- BaseEntity 基类：`create_time` / `update_time` / `is_deleted`

### 前端规范

- **C 端**：Nuxt 3 SSR，Composition API + `<script setup>`
- **B 端**：Vue 3 SPA，Element Plus 组件，Pinia 状态管理
- 样式：UnoCSS 原子化 + CSS 变量主题

### 常见问题

**Q: 提示数据库连接失败？**
确认 MySQL 已启动，`application-dev.yml` 中账号密码正确。

**Q: Redis 连接失败？**
确认 Redis 已启动，端口 6379。

**Q: 前端请求后端 404/跨域？**
后端 `CorsConfig` 已配置允许所有来源（开发环境），确认后端先启动。
Nuxt 3 和 Vite 开发模式下 `/api` 由 `proxy` 转发到 `localhost:8080`。

**Q: 局域网内同事访问不到开发服务器？**
前端启动时会绑定 `0.0.0.0`，同事用 `http://你的IP:3000` 访问即可。
浏览器端 API 会自动拼接当前主机名 + `:8080`，后端需要能通过该地址访问。

**Q: MinIO 的图片同事看不到？**
开发环境 `use-proxy=true`（默认），图片通过后端代理 `/api/web/files/` 访问，局域网友好。
生产环境 `use-proxy=false`，返回 MinIO 直连地址，需确保 MinIO 端点可公网访问。

**Q: 拉黑了用户还能看到他的评论？**
拉黑后评论列表会自动过滤被拉黑用户的评论（需要登录状态）。
可以在个人主页「黑名单」tab 管理已拉黑用户。被拉黑用户的通知也会被过滤。
