-- ============================================================================
-- Personal Blog - MySQL 8.0 Initialization Script
-- Safe for re-execution (idempotent DROP followed by CREATE)
-- Character set: utf8mb4, Collation: utf8mb4_unicode_ci
-- Engine: InnoDB for all tables
-- ============================================================================

-- ---------------------------------------------------------------------------
-- Drop tables in reverse dependency order (respect FK constraints)
-- ---------------------------------------------------------------------------
DROP TABLE IF EXISTS `article_likes`;
DROP TABLE IF EXISTS `article_view_history`;
DROP TABLE IF EXISTS `article_tags`;
DROP TABLE IF EXISTS `comment_votes`;
DROP TABLE IF EXISTS `comment_reports`;
DROP TABLE IF EXISTS `user_blocks`;
DROP TABLE IF EXISTS `user_follows`;
DROP TABLE IF EXISTS `notifications`;
DROP TABLE IF EXISTS `messages`;
DROP TABLE IF EXISTS `comments`;
DROP TABLE IF EXISTS `articles`;
DROP TABLE IF EXISTS `categories`;
DROP TABLE IF EXISTS `tags`;
DROP TABLE IF EXISTS `users`;
DROP TABLE IF EXISTS `links`;
DROP TABLE IF EXISTS `settings`;
DROP TABLE IF EXISTS `visit_logs`;

-- ---------------------------------------------------------------------------
-- 1. users - User table
-- ---------------------------------------------------------------------------
CREATE TABLE `users` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT  COMMENT '用户ID',
    `username`    VARCHAR(50)  NOT NULL                 COMMENT '用户名',
    `password`    VARCHAR(255) NOT NULL                 COMMENT 'BCrypt hash',
    `email`       VARCHAR(100) DEFAULT NULL             COMMENT '邮箱',
    `nickname`    VARCHAR(50)  DEFAULT NULL             COMMENT '昵称',
    `avatar`      VARCHAR(500) DEFAULT NULL             COMMENT '头像URL',
    `bio`         TEXT         DEFAULT NULL             COMMENT '个人简介',
    `role`        VARCHAR(20)  DEFAULT 'AUTHOR'         COMMENT '角色: ADMIN / AUTHOR',
    `status`      TINYINT      DEFAULT 1                COMMENT '状态: 0=禁用, 1=正常',
    `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`  TINYINT      DEFAULT 0                COMMENT '逻辑删除: 0=正常, 1=已删',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_users_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------------
-- 2. categories - Category table
-- ---------------------------------------------------------------------------
CREATE TABLE `categories` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '分类ID',
    `name`        VARCHAR(50)  NOT NULL                 COMMENT '分类名称',
    `slug`        VARCHAR(50)  NOT NULL                 COMMENT 'URL别名',
    `description` VARCHAR(200) DEFAULT NULL             COMMENT '分类描述',
    `sort`        INT          DEFAULT 0                COMMENT '排序号',
    `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`  TINYINT      DEFAULT 0                COMMENT '逻辑删除: 0=正常, 1=已删',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_categories_slug` (`slug`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------------
-- 3. tags - Tag table
-- ---------------------------------------------------------------------------
CREATE TABLE `tags` (
    `id`          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '标签ID',
    `name`        VARCHAR(50) NOT NULL                 COMMENT '标签名称',
    `slug`        VARCHAR(50) NOT NULL                 COMMENT 'URL别名',
    `create_time` DATETIME    DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`  TINYINT     DEFAULT 0                COMMENT '逻辑删除: 0=正常, 1=已删',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tags_slug` (`slug`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------------
-- 4. articles - Article table
-- ---------------------------------------------------------------------------
CREATE TABLE `articles` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '文章ID',
    `title`         VARCHAR(200) NOT NULL                 COMMENT '标题',
    `slug`          VARCHAR(200) NOT NULL                 COMMENT 'URL别名（nanoid短ID）',
    `content`       LONGTEXT     DEFAULT NULL             COMMENT 'Markdown原文',
    `summary`       VARCHAR(500) DEFAULT NULL             COMMENT '摘要',
    `cover_image`   VARCHAR(500) DEFAULT NULL             COMMENT '封面图URL',
    `status`        VARCHAR(20)  DEFAULT 'DRAFT'          COMMENT '状态: DRAFT/PUBLISHED/HIDDEN',
    `view_count`    BIGINT       DEFAULT 0                COMMENT '浏览数',
    `like_count`    INT          DEFAULT 0                COMMENT '点赞数',
    `comment_count` INT          DEFAULT 0                COMMENT '评论数',
    `is_top`        TINYINT      DEFAULT 0                COMMENT '是否置顶: 0=否, 1=是',
    `is_featured`   TINYINT      DEFAULT 0                COMMENT '是否精选: 0=否, 1=是',
    `user_id`       BIGINT       NOT NULL                 COMMENT '作者ID',
    `category_id`   BIGINT       DEFAULT NULL             COMMENT '分类ID',
    `published_at`  DATETIME     DEFAULT NULL             COMMENT '发布时间',
    `create_time`   DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`    TINYINT      DEFAULT 0                COMMENT '逻辑删除: 0=正常, 1=已删',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_articles_slug` (`slug`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------------
-- 5. article_tags - Article-Tag junction table
-- ---------------------------------------------------------------------------
CREATE TABLE `article_tags` (
    `article_id` BIGINT NOT NULL,
    `tag_id`     BIGINT NOT NULL,
    PRIMARY KEY (`article_id`, `tag_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------------
-- 6. comments - Comment table
-- ---------------------------------------------------------------------------
CREATE TABLE `comments` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '评论ID',
    `content`      TEXT         NOT NULL                 COMMENT '评论内容',
    `article_id`   BIGINT       NOT NULL                 COMMENT '文章ID',
    `user_id`      BIGINT       DEFAULT NULL             COMMENT '用户ID（登录用户）',
    `parent_id`    BIGINT       DEFAULT NULL             COMMENT '回复目标ID，最多一层嵌套',
    `author_name`  VARCHAR(50)  DEFAULT NULL             COMMENT '匿名评论者昵称',
    `author_email` VARCHAR(100) DEFAULT NULL             COMMENT '匿名评论者邮箱',
    `status`       VARCHAR(20)  DEFAULT 'PENDING'        COMMENT '状态: PENDING/APPROVED/SPAM',
    `ip`           VARCHAR(45)  DEFAULT NULL             COMMENT '评论者IP',
    `user_agent`   VARCHAR(500) DEFAULT NULL             COMMENT '浏览器UA',
    `create_time`  DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`  DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`   TINYINT      DEFAULT 0                COMMENT '逻辑删除: 0=正常, 1=已删',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------------
-- 7. links - Friend links table
-- ---------------------------------------------------------------------------
CREATE TABLE `links` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '友链ID',
    `name`        VARCHAR(100) NOT NULL                 COMMENT '站点名称',
    `url`         VARCHAR(500) NOT NULL                 COMMENT '站点URL',
    `avatar`      VARCHAR(500) DEFAULT NULL             COMMENT '头像URL',
    `description` VARCHAR(200) DEFAULT NULL             COMMENT '站点描述',
    `sort`        INT          DEFAULT 0                COMMENT '排序号',
    `status`      TINYINT      DEFAULT 1                COMMENT '状态: 0=禁用, 1=正常',
    `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`  TINYINT      DEFAULT 0                COMMENT '逻辑删除: 0=正常, 1=已删',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------------
-- 8. settings - Site settings KV table (hard deletes only, no is_deleted)
-- ---------------------------------------------------------------------------
CREATE TABLE `settings` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '配置ID',
    `setting_key`   VARCHAR(100) NOT NULL                 COMMENT '配置键',
    `setting_value` TEXT         DEFAULT NULL             COMMENT '配置值',
    `description`   VARCHAR(200) DEFAULT NULL             COMMENT '配置说明',
    `create_time`   DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_settings_key` (`setting_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------------
-- 9. visit_logs - Visit log table (no logical delete, cleaned every 90 days)
-- ---------------------------------------------------------------------------
CREATE TABLE `visit_logs` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '日志ID',
    `article_id`  BIGINT       DEFAULT NULL             COMMENT '文章ID',
    `ip`          VARCHAR(45)  DEFAULT NULL             COMMENT '访客IP',
    `user_agent`  VARCHAR(500) DEFAULT NULL             COMMENT '浏览器UA',
    `referer`     VARCHAR(500) DEFAULT NULL             COMMENT '来源URL',
    `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '访问时间',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- 1. 用户浏览记录表（登录用户，无软删除）
CREATE TABLE IF NOT EXISTS `article_view_history` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '记录ID',
    `user_id`       BIGINT       NOT NULL                 COMMENT '用户ID',
    `article_id`    BIGINT       NOT NULL                 COMMENT '文章ID',
    `article_title` VARCHAR(200) DEFAULT NULL             COMMENT '文章标题（冗余）',
    `article_slug`  VARCHAR(200) DEFAULT NULL             COMMENT '文章别名（冗余）',
    `create_time`   DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '浏览时间',
    PRIMARY KEY (`id`),
    INDEX `idx_user_create` (`user_id`, `create_time`),
    INDEX `idx_user_article` (`user_id`, `article_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. 用户点赞记录表（无软删除）
CREATE TABLE IF NOT EXISTS `article_likes` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '记录ID',
    `user_id`       BIGINT       NOT NULL                 COMMENT '用户ID',
    `article_id`    BIGINT       NOT NULL                 COMMENT '文章ID',
    `article_title` VARCHAR(200) DEFAULT NULL             COMMENT '文章标题（冗余）',
    `article_slug`  VARCHAR(200) DEFAULT NULL             COMMENT '文章别名（冗余）',
    `create_time`   DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_article` (`user_id`, `article_id`),
    INDEX `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- 3. 评论赞/踩投票表
CREATE TABLE IF NOT EXISTS `comment_votes` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '记录ID',
    `comment_id`  BIGINT       NOT NULL                 COMMENT '评论ID',
    `user_id`     BIGINT       NOT NULL                 COMMENT '用户ID',
    `vote_type`   VARCHAR(10)  NOT NULL                 COMMENT 'LIKE / DISLIKE',
    `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_comment_user` (`comment_id`, `user_id`),
    KEY `idx_comment_votes_cid` (`comment_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. 举报记录表
CREATE TABLE IF NOT EXISTS `comment_reports` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '记录ID',
    `comment_id`   BIGINT       NOT NULL                 COMMENT '被举报评论ID',
    `reporter_id`  BIGINT       NOT NULL                 COMMENT '举报人ID',
    `reason`       VARCHAR(50)  DEFAULT NULL             COMMENT '举报原因: SPAM/ABUSE/OTHER',
    `status`       VARCHAR(20)  DEFAULT 'PENDING'        COMMENT '状态: PENDING/RESOLVED/DISMISSED',
    `create_time`  DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '举报时间',
    `resolve_time` DATETIME     DEFAULT NULL             COMMENT '处理时间',
    PRIMARY KEY (`id`),
    KEY `idx_report_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. 用户黑名单表
CREATE TABLE IF NOT EXISTS `user_blocks` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '记录ID',
    `blocker_id`  BIGINT       NOT NULL                 COMMENT '拉黑者ID',
    `blocked_id`  BIGINT       NOT NULL                 COMMENT '被拉黑用户ID',
    `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_block` (`blocker_id`, `blocked_id`),
    KEY `idx_block_blocker` (`blocker_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 6. 用户关注表
CREATE TABLE IF NOT EXISTS `user_follows` (
    `id`           BIGINT   NOT NULL AUTO_INCREMENT COMMENT '记录ID',
    `follower_id`  BIGINT   NOT NULL                 COMMENT '关注者ID',
    `following_id` BIGINT   NOT NULL                 COMMENT '被关注者ID',
    `create_time`  DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '关注时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_follower_following` (`follower_id`, `following_id`),
    KEY `idx_follower` (`follower_id`),
    KEY `idx_following` (`following_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 7. 通知表
CREATE TABLE IF NOT EXISTS `notifications` (
    `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '通知ID',
    `user_id`        BIGINT       NOT NULL                 COMMENT '通知接收者',
    `type`           VARCHAR(20)  NOT NULL                 COMMENT 'COMMENT/LIKE/FOLLOW/MESSAGE',
    `from_user_id`   BIGINT       DEFAULT NULL             COMMENT '触发者ID',
    `from_user_name` VARCHAR(50)  DEFAULT NULL             COMMENT '触发者昵称(冗余)',
    `from_user_avatar` VARCHAR(255) DEFAULT NULL           COMMENT '触发者头像(冗余)',
    `article_id`     BIGINT       DEFAULT NULL             COMMENT '关联文章ID',
    `article_title`  VARCHAR(200) DEFAULT NULL             COMMENT '关联文章标题(冗余)',
    `article_slug`   VARCHAR(200) DEFAULT NULL             COMMENT '关联文章slug(冗余)',
    `content`        TEXT         DEFAULT NULL             COMMENT '通知内容摘要',
    `is_read`        TINYINT      DEFAULT 0                COMMENT '已读: 0=未读, 1=已读',
    `create_time`    DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_notif_user_read` (`user_id`, `is_read`),
    KEY `idx_notif_user_time` (`user_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 8. 私信表
CREATE TABLE IF NOT EXISTS `messages` (
    `id`           BIGINT   NOT NULL AUTO_INCREMENT COMMENT '消息ID',
    `from_user_id` BIGINT   NOT NULL                 COMMENT '发件人ID',
    `to_user_id`   BIGINT   NOT NULL                 COMMENT '收件人ID',
    `content`      TEXT     NOT NULL                 COMMENT '消息内容',
    `is_read`      TINYINT  DEFAULT 0                COMMENT '已读: 0=未读, 1=已读',
    `create_time`  DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
    PRIMARY KEY (`id`),
    KEY `idx_msg_from` (`from_user_id`),
    KEY `idx_msg_to_read` (`to_user_id`, `is_read`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 9. User 表新增关注计数字段
ALTER TABLE `users` ADD COLUMN IF NOT EXISTS `follower_count` INT NOT NULL DEFAULT 0 COMMENT '粉丝数' AFTER `bio`;
ALTER TABLE `users` ADD COLUMN IF NOT EXISTS `following_count` INT NOT NULL DEFAULT 0 COMMENT '关注数' AFTER `follower_count`;


-- ---------------------------------------------------------------------------
-- Foreign Key Constraints
-- ---------------------------------------------------------------------------

-- articles -> users
ALTER TABLE `articles`
    ADD CONSTRAINT `fk_articles_user_id`
    FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE;

-- articles -> categories
ALTER TABLE `articles`
    ADD CONSTRAINT `fk_articles_category_id`
    FOREIGN KEY (`category_id`) REFERENCES `categories` (`id`) ON DELETE SET NULL;

-- article_tags -> articles
ALTER TABLE `article_tags`
    ADD CONSTRAINT `fk_article_tags_article_id`
    FOREIGN KEY (`article_id`) REFERENCES `articles` (`id`) ON DELETE CASCADE;

-- article_tags -> tags
ALTER TABLE `article_tags`
    ADD CONSTRAINT `fk_article_tags_tag_id`
    FOREIGN KEY (`tag_id`) REFERENCES `tags` (`id`) ON DELETE CASCADE;

-- comments -> articles
ALTER TABLE `comments`
    ADD CONSTRAINT `fk_comments_article_id`
    FOREIGN KEY (`article_id`) REFERENCES `articles` (`id`) ON DELETE CASCADE;

-- comments -> users (nullable)
ALTER TABLE `comments`
    ADD CONSTRAINT `fk_comments_user_id`
    FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE SET NULL;

-- comments -> comments (self-referencing, max 1 level nesting)
ALTER TABLE `comments`
    ADD CONSTRAINT `fk_comments_parent_id`
    FOREIGN KEY (`parent_id`) REFERENCES `comments` (`id`) ON DELETE CASCADE;

-- ---------------------------------------------------------------------------
-- Indexes
-- ---------------------------------------------------------------------------

-- articles
CREATE INDEX `idx_articles_status_published` ON `articles` (`status`, `published_at` DESC);
CREATE INDEX `idx_articles_category_id`      ON `articles` (`category_id`);
CREATE INDEX `idx_articles_user_id`          ON `articles` (`user_id`);

-- comments
CREATE INDEX `idx_comments_article_status`   ON `comments` (`article_id`, `status`);
CREATE INDEX `idx_comments_parent_id`        ON `comments` (`parent_id`);

-- visit_logs
CREATE INDEX `idx_visit_logs_create_time`    ON `visit_logs` (`create_time`);
CREATE INDEX `idx_visit_logs_article_id`     ON `visit_logs` (`article_id`);

-- ---------------------------------------------------------------------------
-- Seed Data
-- ---------------------------------------------------------------------------

-- Admin user (password: admin123, BCrypt cost 10)
INSERT IGNORE INTO `users` (`username`, `password`, `role`, `nickname`, `status`)
VALUES ('admin', '$2b$10$mMIb00Ixd7f9GRnIcPnoyOw7IVf5FV/1SXpmh22biwRW1k9KU70QG', 'ADMIN', 'Admin', 1);

-- Categories
INSERT IGNORE INTO `categories` (`id`, `name`, `slug`, `description`, `sort`) VALUES
(1, '技术', 'tech',    '技术相关文章', 1),
(2, '生活', 'life',    '生活随笔记录', 2),
(3, '读书', 'reading', '读书笔记与感悟', 3),
(4, '随笔', 'essay',   '随想随记', 4),
(5, '工具', 'tools',   '效率工具与软件推荐', 5),
(6, '笔记', 'notes',   '学习笔记与日常记录', 6);

-- Tags
INSERT IGNORE INTO `tags` (`name`, `slug`) VALUES
('Java',         'java'),
('Spring Boot',  'spring-boot'),
('Vue.js',       'vuejs'),
('Nuxt.js',      'nuxtjs'),
('数据库',        'database'),
('前端',          'frontend'),
('后端',          'backend'),
('Docker',       'docker'),
('架构设计',      'architecture'),
('开源',          'opensource'),
('其他',          'other');

-- Articles (one admin user at id=1, 6 categories 1~6)
INSERT IGNORE INTO `articles` (`id`, `title`, `slug`, `content`, `summary`, `status`, `view_count`, `like_count`, `comment_count`, `is_top`, `is_featured`, `user_id`, `category_id`, `published_at`, `create_time`) VALUES

-- ===== 技术 =====
(1, 'Spring Boot 3.2 新特性速览', 'spring-boot-3-2-overview',
'## 概述\n\nSpring Boot 3.2 带来了许多令人兴奋的新特性，包括虚拟线程支持、CRaC 等项目改进。\n\n## 虚拟线程\n\nJDK 21 正式发布了虚拟线程，Spring Boot 3.2 对其提供了原生支持：\n\n```yaml\nspring:\n  threads:\n    virtual:\n      enabled: true\n```\n\n## 结论\n\n升级到 Spring Boot 3.2 可以显著提升应用性能。',
'Spring Boot 3.2 的新特性概览，包括虚拟线程、CRaC 等。',
'PUBLISHED', 1280, 45, 12, 1, 1, 1, 1, '2026-01-15 10:00:00', '2026-01-15 09:00:00'),

(2, 'MySQL 索引优化实战', 'mysql-index-optimization',
'## 为什么需要索引\n\n索引是数据库性能优化的第一手段，合理的索引可以让查询效率提升几个数量级。\n\n## 最左前缀原则\n\n联合索引遵循最左前缀原则，在建索引时需要把最常查询的列放在最前面。\n\n## 慢查询分析\n\n```sql\nEXPLAIN SELECT * FROM articles WHERE status = ''PUBLISHED'' ORDER BY published_at DESC;\n```\n\n通过 `EXPLAIN` 可以查看 SQL 的执行计划，判断是否走了索引。',
'深入讲解 MySQL 索引优化技巧，包括联合索引、覆盖索引和慢查询分析。',
'PUBLISHED', 856, 32, 8, 0, 0, 1, 1, '2026-02-20 14:30:00', '2026-02-20 13:00:00'),

(3, 'Docker Compose 多环境部署指南', 'docker-compose-multi-env',
'## 问题\n\n开发、测试、生产环境配置不同，如何优雅管理？\n\n## 方案\n\n使用 `docker-compose.override.yml` 和 `.env` 文件组合：\n\n```yaml\n# docker-compose.override.yml（本地开发用）\nservices:\n  blog-server:\n    environment:\n      - SPRING_PROFILES_ACTIVE=dev\n```\n\n## 最佳实践\n\n- 通用配置放 `docker-compose.yml`\n- 环境差异用 `.env` + `profiles` 解决',
'用 Docker Compose 管理多环境部署的技巧和最佳实践。',
'PUBLISHED', 623, 28, 5, 0, 0, 1, 1, '2026-03-10 09:00:00', '2026-03-09 22:00:00'),

-- ===== 生活 =====
(4, '周末徒步日记', 'weekend-hiking',
'## 出发\n\n周六起了个大早，背上背包出发去郊外的森林公园。\n\n## 路上\n\n秋天的山林层林尽染，空气格外清新。走了大约两个小时到达山顶，视野豁然开朗。\n\n## 感悟\n\n偶尔离开屏幕，走进自然，是对身心最好的调节。',
'记录一次周末徒步的经历和感悟。',
'PUBLISHED', 432, 18, 3, 0, 0, 1, 2, '2026-04-05 18:30:00', '2026-04-05 16:00:00'),

(5, '阳台种花记', 'balcony-gardening',
'## 开始\n\n年初在阳台上搭了几个花盆，种下了月季和薄荷的种子。\n\n## 成长\n\n每天早晚浇水，看着嫩芽破土而出，是一件很有成就感的事。\n\n## 收获\n\n三个月后，月季开出了第一朵花。薄荷已经摘了好几轮泡茶喝。小小的阳台变成了家里的绿色角落。',
'从零开始在阳台种花的经验和乐趣。',
'PUBLISHED', 387, 22, 6, 0, 0, 1, 2, '2026-05-12 10:00:00', '2026-05-12 08:30:00'),

-- ===== 读书 =====
(6, '《重构》读书笔记：改善既有代码的设计', 'refactoring-reading-notes',
'## 为什么要重构\n\n软件在演进过程中，代码质量不可避免会下降。重构就是持续保持代码健康的必要手段。\n\n## 核心原则\n\n- 小步修改，频繁测试\n- 每次只改一个逻辑\n- 重构前后行为不变\n\n## 常用手法\n\n| 手法 | 场景 |\n|------|------|\n| Extract Method | 一段逻辑需要独立理解 |\n| Rename Variable | 命名不能表达意图 |\n| Replace Conditional with Polymorphism | 多个 if-else 判断类型 |',
'Martin Fowler 经典著作《重构》的核心读书笔记与实践心得。',
'PUBLISHED', 723, 35, 9, 0, 1, 1, 3, '2026-01-28 11:00:00', '2026-01-28 10:00:00'),

(7, '《深入理解 Java 虚拟机》精华总结', 'jvm-book-essentials',
'## 内存区域\n\nJava 运行时数据区包括：方法区、堆、虚拟机栈、本地方法栈、程序计数器。\n\n## 垃圾回收\n\nJVM 的垃圾回收算法从标记-清除到 G1，再到 ZGC，延迟越来越低。\n\n## 调优实战\n\n```bash\njava -Xms4g -Xmx4g -XX:+UseZGC -XX:+ZGenerational MyApp\n```\n\nZGC 的停顿时间控制在 1ms 以内，适合大堆内存场景。',
'周志明《深入理解 Java 虚拟机》关键知识点提炼。',
'PUBLISHED', 921, 41, 11, 0, 0, 1, 3, '2026-03-15 15:00:00', '2026-03-15 14:00:00'),

-- ===== 随笔 =====
(8, '关于编程语言的随想', 'thoughts-on-programming-languages',
'每种语言都有自己的哲学。\n\nJava 严谨，适合大型工程；Python 灵活，适合快速迭代；JavaScript 生态庞大，无所不能。\n\n但语言只是工具，真正的能力在于解决问题的能力。不要成为某种语言的"信徒"，而是成为问题的解决者。',
'对编程语言选择的一些个人思考。',
'PUBLISHED', 534, 27, 7, 0, 0, 1, 4, '2026-02-08 20:00:00', '2026-02-08 19:30:00'),

(9, '写博客的这三年', 'three-years-of-blogging',
'## 开始\n\n三年前搭了这个博客，初衷很简单——把学到的东西记录下来。\n\n## 变化\n\n从最初的技术笔记，到后来开始写读书感悟、生活记录，博客的内容越来越杂，但越来越真实。\n\n## 收获\n\n写博客最大的收获不是流量或关注，而是写作本身倒逼的思考。把知识写出来，才是真正的掌握。',
'回顾运营个人博客三年的心路历程与收获。',
'PUBLISHED', 689, 33, 14, 1, 1, 1, 4, '2026-04-20 12:00:00', '2026-04-20 10:00:00'),

-- ===== 工具 =====
(10, '效率工具推荐：我的日常开发工具箱', 'dev-toolbox',
'## IDE\n\nIntelliJ IDEA + VS Code 搭配使用，前者写 Java，后者写前端。\n\n## 终端\n\n- iTerm2 + Oh My Zsh\n- tmux 多窗口管理\n- ripgrep 取代 grep\n\n## 笔记\n\nObsidian 用于知识管理，双向链接让知识不再孤立。',
'分享日常开发中离不开的效率工具。',
'PUBLISHED', 756, 38, 10, 0, 0, 1, 5, '2026-05-01 09:30:00', '2026-05-01 08:00:00'),

(11, 'Git 进阶技巧：从入门到精通', 'git-advanced-tips',
'## 交互式 rebase\n\n```bash\ngit rebase -i HEAD~3\n```\n\n可以合并、修改 commit 信息或调整 commit 顺序。\n\n## bisect 定位 bug\n\n```bash\ngit bisect start\ngit bisect bad\ngit bisect good v1.0\n```\n\n二分查找，快速定位引入 bug 的 commit。\n\n## 最佳实践\n\n- 提交信息遵循 Conventional Commits\n- 分支命名用 `feature/` `fix/` `chore/` 前缀',
'Git 在日常开发中的进阶用法和最佳实践。',
'PUBLISHED', 598, 26, 6, 0, 0, 1, 5, '2026-06-01 16:00:00', '2026-06-01 14:30:00'),

-- ===== 笔记 =====
(12, 'Redis 核心数据结构与使用场景', 'redis-data-structures',
'## String\n\n缓存、计数器、分布式锁。\n\n## Hash\n\n存储对象，比 String + JSON 省内存。\n\n## List\n\n消息队列、最新动态列表。\n\n## Set / ZSet\n\n标签、排行榜、去重统计。\n\n每种数据结构都有其擅长的场景，选对数据结构事半功倍。',
'Redis 五种核心数据结构的特性与实际应用场景总结。',
'PUBLISHED', 834, 36, 9, 0, 0, 1, 6, '2026-02-25 13:00:00', '2026-02-25 12:00:00'),

(13, 'JWT 认证原理与 Spring Security 集成', 'jwt-spring-security',
'## JWT 结构\n\nJWT 由 Header、Payload、Signature 三部分组成，通过 Base64 编码并用密钥签名。\n\n## Spring Security 集成\n\n```java\n@Bean\npublic SecurityFilterChain filterChain(HttpSecurity http) {\n    return http\n        .authorizeHttpRequests(auth -> auth\n            .requestMatchers(\"/api/web/**\").permitAll()\n            .requestMatchers(\"/api/admin/**\").authenticated()\n        )\n        .sessionManagement(sm -> sm.sessionCreationPolicy(STATELESS))\n        .build();\n}\n```\n\n## 注意事项\n\n- Token 过期时间不宜过长\n- 敏感操作需额外验证\n- 服务端可以维护黑名单实现即时失效',
'JWT 认证原理详解及 Spring Security 无状态集成实践。',
'PUBLISHED', 1045, 48, 15, 0, 1, 1, 6, '2026-04-10 17:00:00', '2026-04-10 15:00:00'),

(14, 'RESTful API 设计规范总结', 'restful-api-design',
'## 资源命名\n\n- 使用名词复数：`/api/articles`\n- 层级关系：`/api/articles/{id}/comments`\n\n## HTTP 方法\n\n| 方法 | 操作 | 幂等 |\n|------|------|------|\n| GET | 查询 | ✅ |\n| POST | 创建 | ❌ |\n| PUT | 全量更新 | ✅ |\n| DELETE | 删除 | ✅ |\n\n## 响应格式\n\n统一使用 `{ code, message, data }` 结构。',
'RESTful API 设计的命名规范、状态码选择与最佳实践总结。',
'PUBLISHED', 712, 30, 8, 0, 0, 1, 6, '2026-05-20 08:00:00', '2026-05-19 22:00:00');

-- Article-Tag relationships
INSERT IGNORE INTO `article_tags` (`article_id`, `tag_id`) VALUES
-- 技术 -> Spring Boot, Java, 后端
(1, 2), (1, 1), (1, 7),
-- 技术 -> 数据库, 后端
(2, 5), (2, 7),
-- 技术 -> Docker, 后端, 架构设计
(3, 8), (3, 7), (3, 9),
-- 生活 -> 其他
(4, 11), (5, 11),
-- 读书 -> Java, 后端, 架构设计
(6, 1), (6, 7), (6, 9),
(7, 1), (7, 7),
-- 随笔 -> 其他
(8, 11), (9, 11),
-- 工具 -> Docker, 开源, 其他
(10, 8), (10, 10), (10, 11),
(11, 10), (11, 11),
-- 笔记 -> Java, 数据库, 后端
(12, 5), (12, 7),
(13, 1), (13, 7),
(14, 7), (14, 9);

-- Settings
INSERT IGNORE INTO `settings` (`setting_key`, `setting_value`, `description`) VALUES
('site_title',       'Inkwell',                        '站点标题'),
('site_description', 'Inkwell — 一个开发者社区与技术分享平台',    '站点描述'),
('site_keywords',    'blog,tech,编程,Java,Spring Boot,Vue.js,读书,个人博客', '站点关键词'),
('site_icon',        '/favicon.ico',                   '站点图标'),
('about_content',    '## 关于我\n\n一名全栈开发者，热爱技术、阅读与生活。\n\n这个博客主要分享：\n\n- 技术教程与踩坑记录\n- 读书笔记与感悟\n- 生活随笔与效率工具\n\n欢迎通过评论与我交流。', '关于页内容（Markdown）'),
('footer_info',      'Inkwell &mdash; Built with Spring Boot &amp; Nuxt 3', '页脚信息'),
('sensitive_words',  '赌博,色情,诈骗,毒品,暴力,恐怖,反动,代孕,裸聊,刷单,兼职,日结,高薪招聘,微信号,QQ群,私聊', '评论敏感词（逗号分隔，命中自动进入人工审核）');

-- Friend links (友链)
INSERT IGNORE INTO `links` (`name`, `url`, `avatar`, `description`, `sort`) VALUES
('Vue.js',       'https://vuejs.org',                          NULL, '渐进式 JavaScript 框架',                    1),
('Spring',       'https://spring.io',                          NULL, 'Java 企业级应用框架',                       2),
('Nuxt.js',      'https://nuxt.com',                           NULL, 'Vue 的元框架：SSR、SSG 一应俱全',           3),
('Vite',         'https://vitejs.dev',                         NULL, '下一代前端构建工具',                         4),
('MDN Web Docs', 'https://developer.mozilla.org/zh-CN/',       NULL, 'Web 技术权威文档',                          5),
('GitHub',       'https://github.com',                         NULL, '全球最大的代码托管平台',                     6),
('Stack Overflow', 'https://stackoverflow.com',                NULL, '程序员问答社区',                             7),
('Rust 语言',     'https://www.rust-lang.org/zh-CN',            NULL, '一门赋予每个人构建可靠高效软件能力的语言',    8);
