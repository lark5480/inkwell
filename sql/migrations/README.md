# sql/migrations — 数据库迁移

## 为什么需要这个目录

本项目的三个 profile 都是 `ddl-auto: validate`：**Hibernate 只校验、不改表**。
而 `sql/init.sql` 开头是 `DROP TABLE`，只在数据卷为空时由 MySQL 自动执行**一次**。

于是「改了实体/表结构」这件事，代码侧没有任何自动兜底：
新镜像起来后 Hibernate 校验不过，后端会**启动失败并反复重启**（2G 机器上表现为站点 502 + 容器 restarting）。
这个目录 + `scripts/migrate.ps1` 就是用来把这件事**从"靠人记住"变成"有登记、有拦截"**。

## 约定

1. 文件名：`V<YYYYMMDDHHMMSS>__<snake_case_描述>.sql`，例如
   `V20261012000001__create_schema_migrations.sql`、`V20261103101500__articles_add_cover_alt.sql`
   —— 版本号按**文件名排序**决定执行顺序，所以前缀一定要能排。
2. 一条迁移只做一件事，尽量**幂等**（`CREATE TABLE IF NOT EXISTS`、先查 `information_schema` 再决定是否 `ALTER`）。
3. **已经应用过的迁移文件不要再改**。`migrate.ps1 -Status` 会对比 checksum，改了会报"漂移"，正确做法是新增一条迁移。
4. 空库重建仍然用 `sql/init.sql`（它代表"当前完整结构"）；**已有数据的库一律走迁移**。
   因此新增字段时通常要改两处：`init.sql`（让新库一步到位）+ 一条 `migrations/`（让老库跟上）。

## 用法

```powershell
$env:INKWELL_SERVER = '<服务器IP>'
pwsh scripts/migrate.ps1 -Status   # 看本地有哪些迁移、服务器上已应用哪些（默认动作）
pwsh scripts/migrate.ps1 -Apply    # 先备份，再按序应用未应用的迁移，并登记
pwsh scripts/migrate.ps1 -Apply -SkipBackup   # 明确不需要备份时
```

`scripts/deploy.ps1` 会在发布前检查：**存在未应用的迁移就拒绝发布**，并提示你先跑 `-Apply`；
如果本次改动动了实体（`blog-server/.../entity/*.java`）却没带任何迁移文件，也会拒绝（除非 `-AllowSchemaChange`）。

## 登记表

```sql
SELECT * FROM schema_migrations ORDER BY version;
```

| 字段 | 说明 |
|------|------|
| `version` | 迁移版本号（文件名 `V` 前缀部分） |
| `filename` | 文件名 |
| `checksum` | 应用时文件的 sha256，用于发现"事后修改已应用迁移" |
| `applied_at` | 应用时间 |

## 历史说明

`sql/migration_add_ngram_fulltext.sql` 是引入本机制**之前**的一次性脚本，其内容（articles 的 ngram 全文索引）
已经包含在 `sql/init.sql` 里，因此不纳入 `migrations/` 登记。
