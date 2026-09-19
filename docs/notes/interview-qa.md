# Inkwell 博客平台 — 知识笔记与面试 Q&A

> 基于项目实际代码实现整理，涵盖 Redis 一致性、全文检索、JPA 动态查询、JWT 认证、对象存储、互动体系、社区治理、Markdown 缓存、数据库设计、性能统计等核心知识点。

> ⚠️ **本文是基于某版本代码整理的知识 / 面试笔记，可能滞后于实际实现，不作为项目权威文档。** 文中提到的类名、方法名、参数（如 TTL、分批大小）均为整理时的快照；准确的系统行为请以 [`docs/02-architecture.md`](../02-architecture.md) 和源码为准。

---
GitHub:[lark5480/inkwell: 多作者博客平台 | Spring Boot 3 + Nuxt 3 + Vue 3 | 文章/评论/点赞/关注/私信/通知](https://github.com/lark5480/inkwell)
## 一、Redis 计数缓冲与原子回写

### 知识概述

Inkwell 采用 **Redis Hash 缓冲 + 定时原子回写** 方案处理浏览量和点赞量。用户访问时通过 `HINCRBY` 原子累加至 Redis Hash（`blog:view-count` / `blog:like-count`），`ViewCountFlushTask` 每 5 分钟执行一次回写。回写链路：`hasKey 前置检查` → `RENAME NX 原子快照` → `JdbcTemplate 批量回写（500 条/批）` → `TransactionSynchronization.afterCommit 删 Redis` → `@PreDestroy 优雅关闭兜底`。

### 面试 Q&A

**Q1: 为什么用 RENAME NX 而不是 HGETALL + HDEL？**

A: HGETALL 和 HDEL 之间存在竞态窗口——HGETALL 读取快照后、HDEL 执行前，新的 HINCRBY 写入的增量会被 HDEL 误删，导致数据丢失。RENAME NX 是原子操作，将主 key 整体重命名为 processingKey，新写入的增量进入重建的空 Hash，不会丢失。

**Q2: afterCommit 回调解决什么问题？直接在事务内删 Redis 不行吗？**

A: 如果在 `@Transactional` 方法内删 Redis，事务回滚时 Redis 数据已被删除，无法恢复。`TransactionSynchronization.afterCommit()` 确保 MySQL 事务成功提交后才删除 processingKey。MySQL 失败时 processingKey 保留，下次定时任务可重试。

**Q3: 为什么 CountFlushService 要独立为一个 Service 类？**

A: Spring 的 `@Transactional` 基于 AOP 代理实现。如果在同一个类内部方法调用另一个带 `@Transactional` 的方法（self-invocation），代理不会拦截，事务不生效。将批量回写逻辑提取到独立的 `CountFlushService`，确保 `@Transactional` 通过代理正确生效。

**Q4: 应用突然崩溃（kill -9）数据会丢吗？**

A: 不会。processingKey 设置了 10 分钟 TTL 兜底。正常关闭时 `@PreDestroy` 触发 `mergeProcessingKeys()` 将残留 processingKey 合并回主 key 再执行最终回写。kill -9 时 processingKey 数据保留在 Redis，下次定时任务通过 RENAME NX 发现 processingKey 已存在会跳过，等 TTL 过期后自动恢复。

**Q5: 500 条分批是怎么实现的？**

A: `ViewCountFlushTask.flushHash()` 中将所有有效条目收集到 `List<long[]>`，然后按 500 条切分 `subBatch = batch.subList(i, min(i+500, size))`，逐批调用 `countFlushService.flushCounts(subBatch, isViewCount, processingKey, isLastBatch)`。只有最后一批（`isLastBatch=true`）才注册 afterCommit 回调删 Redis，避免中间批次提交后提前删除。

---

## 二、搜索优化 — MySQL ngram 全文检索

### 知识概述

搜索功能从 `LIKE '%keyword%'` 全表扫描升级为 **MySQL ngram FULLTEXT 索引 + MATCH AGAINST (BOOLEAN MODE)**。在 `articles` 表的 `(title, summary, content)` 三字段创建 ngram 全文索引（`ngram_token_size=2`），搜索时按相关性权重排序。搜索结果通过 Redis 缓存（MD5 key + 5min TTL），文章变更时 SCAN 惰性清理。

### 面试 Q&A

**Q6: 为什么选 ngram 而不是 Elasticsearch？**

A: 项目已有 Redis + MySQL 技术栈，ngram 是 MySQL 内置功能，零额外中间件成本。ngram 按固定粒度（默认 2 字符，项目使用 MySQL 默认 token_size=2，未在配置中显式设置）切分中文，配合 BOOLEAN MODE 支持 `+必须包含 -排除` 语法，满足博客搜索需求。如果搜索规模增长到百万级文章或需要更精细的分词，可以迁移到 Elasticsearch。

**Q7: BOOLEAN MODE 的特殊字符怎么处理？**

A: `escapeForBooleanMode()` 方法将 `+-><()~*"@!` 等 BOOLEAN MODE 保留字符替换为空格，防止用户输入破坏查询语法。例如搜索 `Java+C++` 会被转义为 `Java C `，避免 `+` 被解析为"必须包含"操作符。

**Q8: 搜索缓存的 key 怎么设计？怎么防止缓存穿透？**

A: key = `blog:search:` + MD5(keyword + ":" + page + ":" + pageSize)，MD5 保证 key 长度固定且无特殊字符。空结果也缓存，TTL 2 分钟（较短），防止恶意搜索不存在的关键词反复穿透到 DB。正常结果缓存 5 分钟。

**Q9: 文章更新后搜索缓存怎么清理？为什么用 SCAN 不用 KEYS？**

A: `clearSearchCache()` 使用 Redis SCAN（count=100）遍历 `blog:search:*` 模式匹配的所有 key 并批量删除。KEYS 命令在 key 数量大时会阻塞 Redis（O(N) 且单线程），SCAN 是增量迭代不阻塞。清理操作放在 `afterCommit` 回调中，确保事务提交后才清理，避免事务回滚时缓存已被误清。

**Q10: 搜索结果的排序逻辑是什么？**

A: 两字段排序——先按 `relevance DESC`（MySQL 自动计算的相关性权重，标题匹配天然权重更高），再按 `published_at DESC`（同权重下新文章优先）。相比原来仅按 `publishedAt DESC` 排序，搜索结果的相关性显著提升。

---

## 三、JPA Criteria 动态查询

### 知识概述

后台文章管理使用 **JPA Criteria API** 构建类型安全的动态查询。通过 `CriteriaBuilder` 根据关键字、分类、状态等条件动态组合 `Predicate`，支持分页和排序。C 端首页使用热度排序表达式 `viewCount + likeCount * 3 + commentCount * 5`。

### 面试 Q&A

**Q11: 为什么用 Criteria API 而不是 @Query 或 Specification？**

A: @Query 是静态 SQL，条件组合不灵活。Specification 底层也是 Criteria API 但封装过度，复杂查询（如 fetch join + 自定义 count）难以控制。直接使用 Criteria API 可以精确控制查询结构，特别是分离 countQuery（避免带 JOIN 的 count 性能差）和 fetch join（预加载关联避免 N+1）。

**Q12: 分页查询为什么要单独构建 countQuery？**

A: 数据查询使用 `root.fetch("tags", JoinType.LEFT)` 预加载标签关联（避免 N+1），但带 JOIN 的 count 查询会返回重复行导致计数偏大。单独构建 countQuery 不带 JOIN，避免重复计数。两种策略各有取舍——**Admin 查询**通过 `cq.distinct(true)` 在数据查询层去重，countQuery 使用 `cb.count(countRoot)`；**Web 查询**使用 `cb.countDistinct(countRoot)` 在 SQL 层去重。数据查询和 count 查询各自优化，互不干扰。

**Q13: 热度排序的表达式怎么通过 CriteriaBuilder 构建？**

A: 该热度公式用于**精选/推荐文章**排序（`getFeaturedArticles()`），`cb.sum(root.get("viewCount"), cb.sum(cb.prod(root.get("likeCount"), 3), cb.prod(root.get("commentCount"), 5)))`，生成 SQL `view_count + like_count * 3 + comment_count * 5`。使用 CriteriaBuilder 的算术 API 而非硬编码 SQL，保持类型安全。

---

## 四、JWT + Spring Security 认证

### 知识概述

采用 **JWT 无状态认证 + Spring Security**，双角色体系（ADMIN / AUTHOR）。`JwtAuthenticationFilter`（继承 `OncePerRequestFilter`）从 `Authorization: Bearer xxx` 提取 token，解析出 userId/username/role 设置到 `SecurityContextHolder`。会话策略 `STATELESS`，不创建 HttpSession。

### 面试 Q&A

**Q14: 为什么选 JWT 而不是 Session？**

A: 前后端分离架构，前端 Vue 3 / Nuxt 3 部署在不同域名，Session 需要 Cookie 跨域配置（CORS + SameSite）。JWT 无状态，Token 自包含用户信息，不需要服务端存储会话，天然支持多实例部署。

**Q15: 白名单策略怎么设计的？**

A: 分层放行：`/api/auth/**`（登录注册）全部放行；`/api/web/**`（C 端）大部分 permitAll，仅 `/api/web/user/**`（个人设置）需认证；`/api/admin/**`（后台管理）需认证，`/api/admin/auth/**`（后台登录）放行。未命中白名单的请求必须携带有效 JWT。

**Q16: Token 解析失败怎么处理？**

A: `JwtUtils.parseToken()` 捕获所有异常（签名无效、过期、格式错误）返回 null，不抛异常到过滤器链。Filter 中判断 token == null 或解析结果为 null 时直接放行（由后续 Security 拦截器决定是否拒绝），避免一个坏 Token 导致整个请求链路崩溃。

---

## 五、MinIO 对象存储 — 策略模式

### 知识概述

文件存储通过 **策略模式 + 条件装配** 实现可切换。`FileStorageService` 接口定义 `upload()` / `delete()` / `getUrl()` 方法，`MinioFileStorageService`（默认）和 `LocalFileStorageService` 两个实现通过 `@ConditionalOnProperty(name = "blog.storage.type")` 决定注入哪个。开发环境 MinIO 实现返回后端代理 URL，生产环境返回 MinIO 直连 URL。

### 面试 Q&A

**Q17: 为什么用策略模式而不是直接调 MinIO SDK？**

A: 解耦。开发环境可能没有 MinIO 服务，本地实现用文件系统替代。生产环境切 MinIO 只需改配置 `blog.storage.type=minio`，代码零修改。新增 S3/阿里云 OSS 等存储只需新增实现类。

**Q18: 开发代理模式是什么？为什么不直接返回 MinIO URL？**

A: 开发环境 MinIO 运行在 Docker 容器内，前端浏览器可能无法直连容器 IP。开发模式下 `useProxy=true`，返回 `/api/web/files/{bucket}/{object}` 由后端代理转发。生产模式返回 MinIO 直连 URL（可套 CDN），后端不做流量中转。

**Q19: Bucket 自动初始化怎么做的？**

A: `@PostConstruct` 中检查 bucket 是否存在（`minioClient.bucketExists()`），不存在则创建（`makeBucket()`）并设置公开读取的 S3 策略（`setBucketPolicy()`），允许匿名读取图片。

---

## 六、互动体系

### 知识概述

四维互动：**评论**（一级嵌套 + 敏感词审核）、**点赞**（登录用户 DB 记录 + 匿名用户 Redis 计数）、**关注**（Toggle + 双方计数同步）、**私信**（会话模型）。通知系统覆盖 COMMENT / LIKE / FOLLOW / MESSAGE 四类，所有通知发送前检查黑名单。

### 面试 Q&A

**Q20: 评论的敏感词过滤怎么实现的？**

A: `SensitiveWordFilter` 从 DB `settings` 表加载敏感词列表（逗号分隔），编译为 `Pattern`（正则 `word1|word2|...`）缓存。评论提交时匹配，命中则标记为 `PENDING`（待人工审核），否则自动 `APPROVED`。Pattern 缓存 5 分钟 TTL，管理员修改敏感词后调用 `refresh()` 即时刷新。

**Q21: 文章点赞为什么区分登录用户和匿名用户？**

A: 登录用户通过 `article_likes` 表记录（UNIQUE 约束保证一人一票），支持"取消点赞"Toggle。匿名用户无法写 DB，降级为 Redis Hash `HINCRBY` 计数，走定时回写流程合并到 MySQL。两种模式互补，登录用户精确控制，匿名用户保证计数不丢。

**Q22: 关注关系的 Toggle 怎么保证计数一致性？**

A: `toggleFollow()` 在同一事务中完成：关注时 INSERT `user_follows` + 双方 `follower_count/following_count` 各 +1；取消关注时 DELETE + 各 -1。UNIQUE 约束 `(follower_id, following_id)` 防止重复关注。

**Q23: 通知系统怎么避免给被拉黑的用户发通知？**

A: `NotificationServiceImpl` 发送通知前调用 `userBlockService.isBlocked(recipientId, senderId)` 检查，被拉黑则跳过。黑名单查询基于 `user_blocks` 表的 UNIQUE 约束 `(blocker_id, blocked_id)`，O(1) 查询。

---

## 七、社区治理

### 知识概述

三层治理：**敏感词过滤**（自动审核，Pattern 缓存 5min）、**评论举报**（用户举报 → 管理员审核，PENDING → RESOLVED/DISMISSED）、**用户黑名单**（拉黑后评论不可见 + 通知不发送）。

### 面试 Q&A

**Q24: 敏感词为什么用正则而不是 DFA 自动机？**

A: 当前敏感词数量少（管理员手动维护），正则 `word1|word2` 足够高效。如果敏感词库增长到数千条，可以切换为 DFA 自动机（O(n) 扫描，n 为文本长度），但当前规模下正则的编译缓存 + 5 分钟 TTL 已经满足性能需求。

**Q25: 举报的唯一约束有什么作用？**

A: `comment_reports` 表 UNIQUE `(comment_id, reporter_id)` 防止同一用户重复举报同一评论。重复提交时 DB 抛出唯一约束异常，Service 层捕获后返回"已举报"提示。

---

## 八、Markdown 渲染缓存

### 知识概述

使用 **Flexmark** 引擎渲染 Markdown（支持表格、任务列表、自动链接扩展），渲染结果通过 `@Cacheable` 按 articleId 缓存至 Redis。文章更新时 `@CacheEvict` 主动失效。

### 面试 Q&A

**Q26: 为什么按 articleId 缓存而不是按内容哈希？**

A: 按 articleId 缓存 key 简单固定，且文章更新时可以直接通过 `@CacheEvict(key = "#id")` 精确失效。按内容哈希需要每次计算 hash 且无法在更新时精确失效（不知道旧内容的 hash）。

**Q27: 浏览量会影响缓存的 HTML 吗？**

A: 不会。渲染缓存的 HTML 不包含浏览量数字，浏览量在响应层通过 `article.getViewCount() + Redis INCR 增量` 合并展示。这样浏览量变化不需要清除渲染缓存。

---

## 九、数据库设计

### 知识概述

16 张独立表 + 1 张多对多关联表（article_tags），覆盖用户、分类、标签、文章、评论、点赞、投票、举报、关注、通知、私信、友链、站点配置、访问日志等。核心实体表有 `is_deleted` 软删除和 `create_time/update_time` 自动时间戳。articles 表含 ngram FULLTEXT 索引。

### 面试 Q&A

**Q28: 为什么用软删除而不是物理删除？**

A: (1) 数据可恢复，误删后可还原；(2) 外键关联不断裂——文章软删除后评论仍可显示"已删除文章"；(3) 审计需求，所有操作留痕。核心实体表（users, articles, comments, categories, tags, links 等）有 `is_deleted` 软删除，查询时统一加 `WHERE is_deleted = 0` 条件。日志/记录型表（visit_logs, article_view_history, article_likes, comment_votes, notifications, messages）不设软删除，通过 TTL 或定期清理维护。

**Q29: 通知表为什么冗余了 from_user_name 和 from_user_avatar？**

A: 通知列表查询高频（每次打开通知页），如果 JOIN users 表获取发送者头像和昵称，查询成本翻倍。冗余存储后单表查询即可展示完整通知，以空间换时间。用户修改头像/昵称时通知中的旧信息可接受（历史通知不需要实时更新）。

**Q30: 评论表的 parent_id 自引用外键为什么限制最多一层？**

A: 业务设计上不允许无限嵌套——评论下只能直接回复，回复的回复不允许。代码层面在创建评论时校验 `parent.getParent() == null`（父评论必须是一级评论），违反则抛出 400。这样前端展示只需两层结构，不需要递归渲染。

---

## 十、性能统计

### 知识概述

`SearchPerfCollector` 使用内存统计收集搜索性能指标：`AtomicLong` 统计总搜索/缓存命中次数，`ConcurrentLinkedDeque` 保留最近 1000 条耗时记录，计算 P99 延迟、平均延迟、缓存命中率。通过 `GET /api/admin/stats/search-perf` 暴露。

### 面试 Q&A

**Q31: 为什么用 ConcurrentLinkedDeque 而不是直接算？**

A: 每次搜索记录一条（耗时 + 是否命中），保留最近 1000 条（超出淘汰最旧的）。`ConcurrentLinkedDeque` 是无锁并发队列，`addFirst()` + `size() > 1000` 时 `removeLast()`，多线程安全且无锁竞争。P99 计算：转为 List 排序后取 `size * 0.99` 索引位置的值。

**Q32: 这些统计数据重启会丢吗？**

A: 会丢。内存统计定位为运行时监控，不是持久化指标。如果需要持久化，可以接入 Micrometer + Prometheus（参考 Flash Sale 项目的做法）。当前方案足够展示"性能指标采集"能力，面试时可以对比讨论两种方案的取舍。
