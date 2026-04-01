-- ============================================================
-- GleanRead 数据库建表脚本
-- 手动在 PostgreSQL 数据库中执行此文件完成初始化
-- ============================================================

-- 1. 知识树节点表
CREATE TABLE IF NOT EXISTS knowledge_tree_node (
    id                BIGSERIAL       PRIMARY KEY,
    parent_id         BIGINT,
    node_title        VARCHAR(255)    NOT NULL,
    outline_markdown  TEXT,
    create_time       TIMESTAMP       NOT NULL DEFAULT NOW(),
    update_time       TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_ktn_parent_id ON knowledge_tree_node(parent_id);

-- 2. 标签表
CREATE TABLE IF NOT EXISTS tags (
    id           BIGSERIAL       PRIMARY KEY,
    tag_name     VARCHAR(64)     NOT NULL UNIQUE,
    heat_weight  INT             NOT NULL DEFAULT 0,
    create_time  TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_tags_heat_weight ON tags(heat_weight DESC);

-- 3. 知识摘录表
CREATE TABLE IF NOT EXISTS excerpts (
    id            BIGSERIAL       PRIMARY KEY,
    content       TEXT            NOT NULL,
    url           VARCHAR(2048),
    user_thought  TEXT,
    tree_node_id  BIGINT,
    create_time   TIMESTAMP       NOT NULL DEFAULT NOW(),
    update_time   TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_excerpt_tree_node_id ON excerpts(tree_node_id);
CREATE INDEX IF NOT EXISTS idx_excerpt_create_time  ON excerpts(create_time DESC);

-- 4. 摘录-标签关联中间表
CREATE TABLE IF NOT EXISTS excerpt_tags (
    excerpt_id  BIGINT  NOT NULL,
    tag_id      BIGINT  NOT NULL,
    PRIMARY KEY (excerpt_id, tag_id)
);

CREATE INDEX IF NOT EXISTS idx_excerpt_tag_tag_id      ON excerpt_tags(tag_id);
CREATE INDEX IF NOT EXISTS idx_excerpt_tag_excerpt_id  ON excerpt_tags(excerpt_id);
