# Deployment Guide

## Docker Compose 部署（推荐）

### 前置条件

- Docker 20+
- Docker Compose 2+

### 一键部署

```bash
# 克隆项目
git clone <repo-url> && cd inkwell

# 方式一：仅启动中间件（推荐，应用在本地 IDE 开发运行）
docker compose up -d mysql redis minio

# 方式二：完全部署（先单独构建镜像，避免 Docker Compose build bug）
docker compose build blog-server blog-web blog-admin
docker compose up -d
```

> **注意**：Docker Compose v2 某些版本在构建 Java/Maven 镜像时存在 panic bug（`build_bake.go:312`），直接 `docker compose up -d` 可能失败。方式一不受影响，方式二先 build 再 up 也可绕过。
>
> **数据库初始化**：MySQL 容器首次启动时自动执行 `sql/init.sql`，无需手动导入。如果要重置数据库，删除数据卷重新创建即可：`docker compose down -v && docker compose up -d mysql`。

# 查看启动状态
docker compose ps

# 查看日志
docker compose logs -f
```

### 访问地址

| 服务 | 地址 | 说明 |
|------|------|------|
| Blog (C端) | http://localhost:3000 | Nuxt SSR 前台 |
| Admin (B端) | http://localhost:3001 | Vue SPA 管理后台 |
| API | http://localhost:8080 | Spring Boot API |
| Nginx | http://localhost:80 | 反向代理入口 |

### 管理命令

```bash
# 查看日志
docker compose logs -f blog-server
docker compose logs -f blog-web
docker compose logs -f blog-admin

# 重启单个服务
docker compose restart blog-server

# 重新构建并启动
docker compose build blog-web && docker compose up -d blog-web

# 停止所有服务
docker compose down

# 停止并删除数据卷（清空数据库）
docker compose down -v

# 扩展服务
docker compose up -d --scale blog-server=2
```

### 环境变量

通过 `.env` 文件配置：

```env
MYSQL_ROOT_PASSWORD=your_secure_password
```

### 生产部署

修改 `nginx/nginx.conf` 中的 `server_name` 为实际域名，然后：

```bash
docker compose build  # 先构建所有镜像，避免 Compose build bug
docker compose up -d
```

## 传统部署（非 Docker）

### 1. 基础设施

```bash
# MySQL 8.0
mysql -u root -p -e "CREATE DATABASE blog DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
mysql -u root -p blog < sql/init.sql

# Redis 7
redis-server
```

### 2. 后端 (blog-server)

```bash
cd blog-server

# 开发模式
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# 生产打包
./mvnw clean package -DskipTests

# 运行
java -jar target/blog-server.jar --spring.profiles.active=prod
```

### 3. C端前端 (blog-web)

```bash
cd blog-web

# 安装依赖
npm install

# 开发
npm run dev

# 构建
npm run build

# 生产运行
node .output/server/index.mjs
```

### 4. B端管理后台 (blog-admin)

```bash
cd blog-admin

# 安装依赖
npm install

# 开发
npm run dev

# 构建
npm run build

# 用任意静态服务器部署 dist/ 目录
```

### 5. Nginx 配置

复制 `nginx/nginx.conf` 到 Nginx 配置目录，修改 `server_name` 后重启：

```bash
cp nginx/nginx.conf /etc/nginx/conf.d/blog.conf
nginx -s reload
```

## Nginx 配置说明

```
/api/     → Spring Boot :8080    (API 代理)
/admin/   → blog-admin :80       (管理后台)
/         → Nuxt :3000           (前台页面)
```

## 镜像构建

### 单模块构建

```bash
# 后端
cd blog-server
docker build -t blog-server .

# C端
cd blog-web
docker build -t blog-web .

# B端
cd blog-admin
docker build -t blog-admin .
```

### 数据持久化

- MySQL 数据 → `mysql_data` volume
- Redis 数据 → `redis_data` volume
- 用户上传 → `uploads/` 目录（建议生产环境挂载外部卷）

## 常见问题

### 数据库连接失败

确保 MySQL 已启动且 `init.sql` 已执行：

```bash
docker compose logs mysql
mysql -h 127.0.0.1 -u root -p -e "SELECT 1"
```

### 端口冲突

检查端口占用：

```bash
netstat -tlnp | grep -E '3000|3001|8080|3306|6379'
```

### 跨域问题

开发环境通过 dev proxy 解决（`nuxt.config.ts` 和 `vite.config.ts`），生产环境通过 Nginx 同源代理。

### 图片访问

- **开发环境**：MinIO 图片通过后端代理 `/api/web/files/` 访问（适合局域网）
- **生产环境**：`application-prod.yml` 中 `blog.storage.minio.use-proxy: false`，图片走 MinIO 直连地址
- 生产环境建议将 MinIO 置于 CDN 或反向代理后，通过域名访问

### MinIO 配置

```yaml
blog:
  storage:
    type: minio
    minio:
      endpoint: http://minio:9000       # 生产环境改为 CDN 域名
      access-key: ${MINIO_ACCESS_KEY}
      secret-key: ${MINIO_SECRET_KEY}
      bucket: blog-images
      use-proxy: false                  # false=直接 MinIO, true=走后端代理
```
