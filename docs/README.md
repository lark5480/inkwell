# Inkwell 文档索引

本目录是 Inkwell 博客平台的开发者文档。仓库总览与快速上手见根目录 [README](../README.md)。

## 权威文档（应与代码保持同步）

| 文档 | 面向 | 内容 |
|------|------|------|
| [01-getting-started.md](01-getting-started.md) | 初次搭建 | 环境要求、Docker / 传统两种方式启动、项目结构、常用命令、API 端点、开发规范、常见问题 |
| [02-architecture.md](02-architecture.md) | 深入开发 | 系统架构图、后端分层设计、关键设计决策、认证流程、前后端关键组件与路由 |
| [03-deployment.md](03-deployment.md) | 运维部署 | Docker Compose 与 Nginx 生产部署、环境变量、镜像构建、常见问题排查 |

## 附注（非权威，允许滞后）

| 文档 | 说明 |
|------|------|
| [notes/interview-qa.md](notes/interview-qa.md) | 基于某版本代码整理的知识 / 面试笔记，是实现快照，**不作为权威来源**；准确行为以上述文档和源码为准 |

## AI 协作指令

面向 AI 编码助手（Claude Code / Codex 等）的命令、依赖、Git 工作流统一维护在仓库根目录的 [AGENTS.md](../AGENTS.md)（唯一事实源，`CLAUDE.md` 仅指向它）。

## 约定

- `01~03` 为**权威文档**，改动代码中影响使用/部署的行为时须同步更新。
- `notes/` 下为**个人知识笔记**，可不定期滞后，但需在文件顶部标注其快照性质。
- 命令、端口、依赖等信息以本文档与 `AGENTS.md` 为准，避免在多处重复正文。
