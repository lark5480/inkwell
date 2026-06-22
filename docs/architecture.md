# Architecture

## 系统架构

```
                    ┌─────────────┐
                    │   Nginx     │ Port 80
                    │  反向代理    │
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

## 后端 (blog-server)

分层架构：`Controller → Service → Repository → Entity`

```
com.blog/
├── controller/
│   ├── admin/       # B端 API，需 JWT 认证
│   │   ├── ArticleController, CategoryController, TagController
│   │   ├── CommentController, ReportController, LinkController
│   │   ├── SettingController, StatsController
│   │   ├── AuthController, UserController
│   └── web/         # C端 API，可选认证
│       ├── ArticleController   # 文章列表/详情/点赞
│       ├── AuthController      # 登录/注册
│       ├── CategoryController, TagController, LinkController
│       ├── CommentController   # 评论 + 投票/举报/删除/拉黑
│       ├── WebController       # 搜索/归档/站点信息/RSS/Sitemap
│       ├── UserArticleController  # 用户文章 CRUD + 历史记录 + 点赞记录
│       ├── UserProfileController  # 用户资料 + 公开主页
│       ├── FollowController    # 关注/粉丝
│       ├── MessageController   # 私信
│       └── NotificationController # 站内通知
├── service/
│   ├── impl/        # 业务逻辑实现
│   │   ├── ArticleServiceImpl      # 文章 + 点赞 toggle + 浏览记录
│   │   ├── UserArticleServiceImpl  # 用户文章管理 (含 @CacheEvict)
│   │   └── ...
│   └── *.java       # 接口定义
├── repository/      # Spring Data JPA
├── entity/          # JPA 实体 (含 ArticleLike, ArticleViewHistory)
├── dto/             # Java records (请求/响应)
├── config/          # Security, CORS, Redis, WebMvc, StorageConfig
├── security/        # JWT 认证过滤器
├── common/          # 统一响应 Result<T>, CacheNames
├── exception/       # 全局异常处理
├── schedule/        # 定时任务 (Redis 浏览量/点赞回写 MySQL)
├── storage/         # 文件存储 (FileStorageService: MinIO / 本地 fallback)
└── util/            # 工具类
```

### 关键设计决策

- **DTO 用 Java record**，不引入 MapStruct
- **统一响应** `Result<T>`：`{ code: 200, message: "success", data: ... }`
- **分页响应** `PageDTO<T>`：`{ records, total, page, pageSize }`
- **Entity 不暴露到 Controller**，通过 DTO 传递
- **逻辑删除**：`@SQLRestriction("is_deleted = 0")`，删除走 UPDATE
- **访问量/点赞**用 Redis 原子计数，定时回写 MySQL（匿名用户）；登录用户点赞直接写 DB（per-user toggle）
- **浏览记录**：登录用户访问文章详情时自动记录到 `article_view_history` 表
- **Markdown** 用 flexmark 服务端渲染，HTML 结果缓存在 Redis
- **缓存一致性**：用户写操作通过 `@CacheEvict` 清除 ARTICLE_LIST/ARCHIVE/DETAIL/HTML/PREV_NEXT/SITE_INFO 缓存
- **多作者支持**：每个用户只能管理自己的文章，`UserArticleServiceImpl` 含 ownership 检查
- **文件存储**：`FileStorageService` 接口抽象，通过 `blog.storage.type` 配置切换 MinIO / 本地存储，`@ConditionalOnProperty` 自动选择实现

### 认证流程

```
请求 → JwtAuthenticationFilter → 解析 Token → 设置 SecurityContext
  ├─ /api/web/** → 可选认证（有 token 则识别用户）
  └─ /api/admin/** → 必须 Bearer Token
```

## 前端 C端 (blog-web)

Nuxt 3 SSR 渲染，关键目录：

```
pages/           # 路由页面 (自动路由)
  ├── index.vue           # 首页
  ├── article/[slug].vue  # 文章详情 + TOC 目录
  ├── article/edit/[id].vue # 编辑文章
  ├── category/[slug].vue # 分类页
  ├── tag/[slug].vue      # 标签页
  ├── user/[id].vue       # 用户公开主页 (文章/历史/点赞/关注/粉丝)
  ├── messages/index.vue  # 消息中心 (私信会话 + 通知分类查看)
  ├── messages/[userId].vue # 私信对话
  ├── write.vue           # 写文章 (Markdown 编辑器)
  ├── my-articles.vue     # 我的文章管理
  ├── notifications.vue   # 通知 (重定向到 /messages)
  ├── search.vue          # 搜索
  ├── archive.vue         # 归档
  ├── tags.vue            # 标签云
  └── login/register/about/links.vue
composables/
  ├── useAuth.ts      # 登录/注册/用户状态
  ├── useBlogApi.ts   # API 封装 (50+ 端点)
  ├── useTheme.ts     # 暗色模式
  └── useI18n.ts      # 国际化 (zh/en)
middleware/
  └── auth.ts         # 认证守卫 (SSR guard)
components/
  ├── TocBlock.vue        # 文章 TOC 目录 (IntersectionObserver)
  ├── MarkdownRenderer.vue # Markdown 渲染
  ├── CommentActionBar.vue  # 评论操作栏 (赞/踩/回复/分享/更多)
  ├── VoteButton.vue        # 赞/踩按钮
  ├── MoreMenu.vue          # 三点菜单
  ├── SharePopover.vue      # 分享面板
  └── NotificationList.vue  # 通知列表
server/routes/
  └── uploads/[...path].ts  # 图片文件服务
```

### 认证

- Token 存 localStorage (`blog_token`)
- 用户信息缓存在 localStorage，`onMounted` 恢复
- 受保护页面通过 `middleware/auth.ts` 守卫

## 前端 B端 (blog-admin)

Vue 3 SPA + Element Plus，关键目录：

```
views/           # 页面组件
  ├── DashboardPage.vue   # 统计概览
  ├── ArticleList.vue     # 文章管理
  ├── ArticleEdit.vue     # 文章编辑器
  ├── CategoryList.vue    # 分类管理
  ├── TagList.vue         # 标签管理
  └── ...
api/             # Axios API 封装
stores/          # Pinia 状态管理
router/          # Vue Router (路由守卫)
```

- Axios 自动带 Token
- 401 拦截自动跳转登录
- 路由守卫检查 Token
