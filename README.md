# Inkwell — 多作者博客平台

基于 Spring Boot 3 + Nuxt 3 + Vue 3 的多作者博客系统，支持文章管理、社区互动（评论/点赞/关注/私信）、用户通知、举报审核、数据统计等完整功能。

## Tech Stack

| 模块 | 技术 | 端口 |
|------|------|------|
| **blog-server** | Spring Boot 3.2 + JPA + Security + Redis | 8080 |
| **blog-web** (C端) | Nuxt 3 SSR + UnoCSS (+ Pinia 注册但未使用) | 3000 |
| **blog-admin** (B端) | Vue 3 + Element Plus + Pinia + Axios | 3001 |
| **Database** | MySQL 8.0 | 3306 | 
| **Cache** | Redis 7 | 6379 |
| **Storage** | MinIO (S3 兼容) | 9000/9001 |

## Quick Start

```bash
# 推荐：一键启动所有服务
docker compose up -d
# → blog-web: http://localhost:3000
# → blog-admin: http://localhost:3001
# → API: http://localhost:8080
```

默认管理员：`admin` / `admin123`

传统本地开发（Java + Node.js 手动启动）见 [01-快速入门](docs/01-getting-started.md)。

## Project Structure

```
├── blog-admin/          # B端管理后台 (Vue 3 SPA)
├── blog-server/         # 后端 API (Spring Boot)
├── blog-web/            # C端前台 (Nuxt 3 SSR)
├── sql/                 # 数据库初始化脚本
├── nginx/               # Nginx 反向代理配置
├── docs/                # 文档
└── docker-compose.yml   # 全栈编排
```

详细目录结构见 [02-系统架构](docs/02-architecture.md)。

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

- **多作者平台**: 注册即可成为作者，自主管理文章、个人主页、浏览/点赞记录
- **JPA Criteria 动态查询**: 后台文章管理支持关键字/分类/状态多条件组合检索，C 端支持分类+标签双维度筛选
- **Redis 计数缓冲**: 浏览量/点赞量先累加至 Redis Hash，定时回写 MySQL，失败条目保留重试，避免高并发写库
- **点赞 Toggle**: 登录用户每人每篇可赞/取消，评论同样支持赞/踩投票
- **评论互动**: 扁平回复 + 赞/踩 + 举报 + 拉黑，敏感词自动拦截，管理员审核
- **通知中心**: 被回复/被点赞/新粉丝/私信 → 铃铛未读提醒，按类型分类查看
- **站内私信**: 会话列表、实时未读标记
- **关注系统**: 关注/取关、粉丝列表、关注者列表，发现有趣作者
- **浏览记录**: 自动记录登录用户的文章浏览历史
- **精选推荐**: 手动精选 + 热度补全（viewCount/likeCount/commentCount 加权）
- **黑名单**: 拉黑用户后自动屏蔽其评论和通知
- **举报审核**: 评论举报 → 管理员处理（"已解决"自动软删除评论）
- **全文搜索**: MySQL FULLTEXT + ngram 中文分词
- **Markdown 渲染**: 服务端 Flexmark 渲染（表格/任务列表/自动链接），结果缓存至 Redis
- **RSS / Sitemap**: 自动生成 RSS Feed 和 XML Sitemap，支持 SEO
- **MinIO 对象存储**: 策略模式抽象，开发环境后端代理访问（局域网友好），生产环境直连或 CDN
- **JWT 无状态认证**: 双角色（ADMIN / AUTHOR），OncePerRequestFilter 鉴权
- **暗色模式**: CSS 变量主题切换
- **国际化和布局**: 中英文切换，响应式布局

## Key Conventions

- **统一响应**: `Result<T>` = `{ code, message, data }`
- **分页响应**: `PageDTO<T>` = `{ records, total, page, pageSize }`
- **逻辑删除**: `is_deleted` 字段 + `@SQLRestriction`
- **JWT 认证**: B端必须 Bearer Token，C端可选
- **DTO 隔离**: Entity 不暴露到 Controller，用 Java record
- **Markdown 渲染**: flexmark 服务端渲染，Redis 缓存 HTML
- **缓存一致性**: 写操作通过 `@CacheEvict` 清除相关缓存

## 文档

| 文档 | 说明 |
|------|------|
| [01-快速入门](docs/01-getting-started.md) | 环境要求、本地启动步骤（Docker 和传统两种方式）、项目结构概览、开发规范 |
| [02-系统架构](docs/02-architecture.md) | 整体架构图、后端分层设计、认证流程、前后端关键组件与路由 |
| [03-部署指南](docs/03-deployment.md) | Docker Compose 与 Nginx 生产部署、环境变量、常见问题排查 |

## Deployment

全量 Docker Compose 部署详见 [03-部署指南](docs/03-deployment.md)。

```bash
# 启动全部服务
docker compose up -d

# 查看日志
docker compose logs -f

# 停止
docker compose down
```

## License

MIT
