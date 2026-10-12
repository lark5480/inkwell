# Stage 1: Build
FROM node:22-alpine AS build
WORKDIR /app
# 2G 内存机器上限制 Node 堆，避免 vue-tsc/vite build OOM
ARG NODE_BUILD_MEMORY=1024
ENV NODE_OPTIONS="--max-old-space-size=${NODE_BUILD_MEMORY}"
# 国内服务器构建时用镜像加速：--build-arg NPM_REGISTRY=https://registry.npmmirror.com
ARG NPM_REGISTRY=https://registry.npmjs.org
RUN npm config set registry $NPM_REGISTRY
COPY package*.json ./
RUN npm ci
COPY . .
# 管理后台部署在 /admin/ 子路径下（与 nginx/nginx.conf 的 location /admin/ 对应）
ARG VITE_BASE=/admin/
ENV VITE_BASE=$VITE_BASE
RUN npm run build

# Stage 2: Serve with Nginx
FROM nginx:alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
