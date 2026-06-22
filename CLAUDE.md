# CLAUDE.md

Instructions for Claude Code when working on this project.

## Commands

### blog-server (Java 17, Maven)

```bash
cd blog-server
./mvnw spring-boot:run          # 启动开发服务器 (dev profile)
./mvnw clean package -DskipTests # 打包
./mvnw test                      # 运行测试
```

依赖：MySQL (localhost:3306, db:blog) + Redis (localhost:6379)。

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
npm run dev       # 开发服务器 (端口 5173, 代理 /api → 8080)
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

- `docs/getting-started.md` — 环境要求、快速启动
- `docs/architecture.md` — 系统架构、分层设计
- `docs/deployment.md` — Docker 部署、Nginx 配置

## Git Workflow

```bash
# 分支
main → develop → feature/* / fix/*

# commit
feat:|fix:|docs:|refactor:|chore:
```
