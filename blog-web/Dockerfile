# Stage 1: Build
FROM node:22-alpine AS build
WORKDIR /app
# 2G 内存机器上限制 Node 堆，避免 nuxt build OOM（不够时调大这个 ARG 并确保有 swap）
ARG NODE_BUILD_MEMORY=1024
ENV NODE_OPTIONS="--max-old-space-size=${NODE_BUILD_MEMORY}"
# 国内服务器构建时用镜像加速：--build-arg NPM_REGISTRY=https://registry.npmmirror.com
ARG NPM_REGISTRY=https://registry.npmjs.org
RUN npm config set registry $NPM_REGISTRY
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

# Stage 2: Runtime
FROM node:22-alpine
WORKDIR /app
COPY --from=build /app/.output ./.output
ENV NODE_ENV=production
EXPOSE 3000
CMD ["node", ".output/server/index.mjs"]
