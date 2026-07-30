-- 迁移脚本：为已有 articles 表添加 ngram 全文索引
-- 适用于已部署的环境升级，新部署请直接使用 init.sql
-- 执行前请确认 MySQL 版本 >= 5.7 且支持 ngram 全文解析器
-- 注意：大表执行可能需要较长时间

ALTER TABLE `articles` ADD FULLTEXT INDEX `ft_idx_article_search` (`title`, `summary`, `content`) WITH PARSER ngram;
