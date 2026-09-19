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

## Architecture

Nginx 统一反向代理入口 → `blog-web` (Nuxt SSR :3000) / `blog-admin` (Vue SPA :3001) / `blog-server` (Spring Boot :8080) 三服务并行，后端依赖 MySQL + Redis + MinIO。完整架构图、后端分层设计与认证流程见 [02-系统架构](docs/02-architecture.md)。

## Key Features

- **多作者平台**：注册即可成为作者，自主管理文章、个人主页、浏览/点赞记录；每用户仅能管理自己的内容（ownership 校验）
- **社区互动**：评论（扁平回复 + 赞/踩 + 敏感词审核）、点赞 Toggle、关注、站内私信、黑名单与举报审核
- **通知中心**：被回复/被点赞/新粉丝/私信 → 铃铛未读提醒，按类型分类查看
- **全文搜索**：MySQL FULLTEXT + ngram 中文分词，按相关性排序，Redis 缓存
- **高并发计数**：浏览量/点赞量先累加 Redis、定时原子回写 MySQL；Markdown 服务端渲染（Flexmark）+ Redis 缓存
- **认证与集成**：JWT 双角色（ADMIN / AUTHOR）无状态认证，MinIO 对象存储策略化，RSS / Sitemap、暗色模式、中英文国际化

> 逐条功能清单与实现细节（含设计取舍）见 [02-系统架构](docs/02-architecture.md)。

## Key Conventions

- **统一响应**: `Result<T>` = `{ code, message, data }`
- **分页响应**: `PageDTO<T>` = `{ records, total, page, pageSize }`
- **逻辑删除**: `is_deleted` 字段 + `@SQLRestriction`
- **JWT 认证**: B端必须 Bearer Token，C端可选
- **DTO 隔离**: Entity 不暴露到 Controller，用 Java record
- **缓存一致性**: 写操作通过 `@CacheEvict` 清除相关缓存

## 文档

完整文档索引见 [docs/README](docs/README.md)。

| 文档 | 说明 |
|------|------|
| [01-快速入门](docs/01-getting-started.md) | 环境要求、本地启动（Docker / 传统）、项目结构、开发规范、API 端点 |
| [02-系统架构](docs/02-architecture.md) | 整体架构图、后端分层设计、认证流程、前后端关键组件与路由 |
| [03-部署指南](docs/03-deployment.md) | Docker Compose 与 Nginx 生产部署、环境变量、常见问题排查 |

附：[设计知识笔记 / 面试 Q&A](docs/notes/interview-qa.md) — 基于某版本代码整理的实现快照，**非权威文档**，仅供参考，准确行为以架构文档和源码为准。

## Deployment

全量 Docker Compose 部署详见 [03-部署指南](docs/03-deployment.md)。

```bash
docker compose up -d   # 启动全部服务
docker compose logs -f # 查看日志
docker compose down    # 停止
```

## License

MIT
