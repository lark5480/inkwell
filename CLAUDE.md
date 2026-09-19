# CLAUDE.md

本项目的 AI 协作指令（命令、依赖、Git 工作流等）统一维护在 **[AGENTS.md](./AGENTS.md)**，以避免双份文件重复维护、彼此漂移。

Claude Code 支持 `@` 引用导入，以下会自动加载 AGENTS.md 的完整内容：

@AGENTS.md

> 若你的工具版本不支持 `@` 导入，请直接阅读 [AGENTS.md](./AGENTS.md)。修改指令时只改 AGENTS.md，不要在本文件里复制内容。
