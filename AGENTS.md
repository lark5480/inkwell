# AGENTS.md

Instructions for AI agents (Claude Code, Codex, etc.) when working on this project.

> 本文件是 AI 协作指令的**唯一事实源**；`CLAUDE.md` 仅作为指向本文件的存根，请勿在两边重复维护。

## Commands

### blog-server (Java 17, Maven)

```bash
cd blog-server
./mvnw spring-boot:run          # 启动开发服务器 (dev profile)
./mvnw clean package -DskipTests # 打包
./mvnw test                      # 运行测试
```

依赖：MySQL (localhost:3306, db:blog) + Redis (localhost:6379) + MinIO (localhost:9000，文件存储)。

### blog-web (Nuxt 3)

```bash
cd blog-web
npm run dev       # 开发服务器 (端口 3000, 代理 /api → 8080)
npm run build     # 构建
npm run preview   # 预览构建产物
```

### blog-admin (Vue 3 + Vite, SPA)

```bash
cd blog-admin
npm run dev       # 开发服务器 (端口 3001, 代理 /api → 8080)
npm run build     # 构建输出到 dist/
npm run preview   # 预览构建产物
```

### Docker

```bash
docker compose up -d   # 启动全部服务
docker compose down    # 停止
docker compose logs -f # 查看日志
```

### Database

```bash
mysql -u root -p blog < sql/init.sql  # 初始化数据库（建表 + 种子数据）
```

## Docs

- `docs/01-getting-started.md` — 环境要求、快速启动
- `docs/02-architecture.md` — 系统架构、分层设计
- `docs/03-deployment.md` — Docker 部署、Nginx 配置

## Git Workflow

```bash
# 分支
main → develop → feature/* / fix/*

# commit
feat:|fix:|docs:|refactor:|chore:
```
