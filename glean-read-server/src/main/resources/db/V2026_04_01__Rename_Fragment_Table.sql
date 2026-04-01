-- 2026-04-01 领域术语重构：将 Fragment (碎片) 重命名为 Excerpt (摘录)
-- 修改核心表名
RENAME TABLE `fragment` TO `excerpts`;

-- 修改关联表名
RENAME TABLE `fragment_tag` TO `excerpt_tags`;

-- 修改关联表中的字段名 (如果适用)
-- 注意：MyBatis Plus 的 LambdaQueryWrapper 如果不映射字段名，
-- 这里保持字段名为 fragment_id 或者重命名为 excerpt_id 需视 ExcerptTag 实体映射而定。
-- 在 ExcerptTag.java 中我已经将其定义为 excerptId，所以这里需要重命名。
ALTER TABLE `excerpt_tags` CHANGE COLUMN `fragment_id` `excerpt_id` bigint(20) NOT NULL;
