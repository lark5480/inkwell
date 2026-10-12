-- ============================================================================
-- V20261012000001__create_schema_migrations.sql
--
-- 迁移登记表：记录 sql/migrations/ 下哪些迁移已经在某个数据库上应用过。
-- 由 scripts/migrate.ps1 读写；这是**必须排在最前面的第一条迁移**，
-- 因为它自己就是登记表的来源。
--
-- 幂等：可重复执行。
-- ============================================================================

CREATE TABLE IF NOT EXISTS `schema_migrations` (
    `version`    VARCHAR(64)  NOT NULL COMMENT '迁移版本号（文件名 V 前缀部分）',
    `filename`   VARCHAR(255) NOT NULL COMMENT '迁移文件名',
    `checksum`   CHAR(64)     DEFAULT NULL COMMENT '应用时文件的 sha256（用于发现"改已应用的迁移"这种漂移）',
    `applied_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '应用时间',
    PRIMARY KEY (`version`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='数据库迁移登记表';
