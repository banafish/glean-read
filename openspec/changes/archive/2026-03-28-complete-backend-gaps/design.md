# 技术设计：补全后端核心功能缺口

## 上下文 (Context)

`glean-read-server` 的 Inbox 和 AI 合成链路存在两处关键空白：
1. 碎片入库时没有写 `fragment_tag` 关联，标签云指向的碎片无法反向检索。
2. `AiSynthesisService` 的 LLM 调用是硬编码 Mock，未接入真实大语言模型。

## 目标 / 非目标

**目标：**
- 补全 `fragment_tag` 多对多中间表及其写入逻辑
- 提供按标签查询碎片的接口（AI 合成前序）
- 引入 `LlmPort` 策略接口，实现可切换的 DeepSeek 适配器
- 保留 `MockLlmAdapter` 供本地开发使用

**非目标：**
- 不引入 Redis 缓冲层（优先级较低，单用户场景暂不需要）
- 不实现 pgvector 向量扩展（未来迭代）
- 不修改现有已实现的接口签名

## 决策 (Decisions)

### 1. fragment_tag 用独立中间表，不用 JSON 字段

选择：标准关系型中间表 `fragment_tag(fragment_id, tag_id)`。

理由：AI 合成的核心流程需要"按 tag → 查碎片 → 喂给 LLM"的反向查询，JOIN 最为简洁；未来在关联表上增加字段（关联时间、是否手动标注）也更自然。JSON 字段方案在 MyBatis-Plus 下反向查询需要 `apply()` 拼原始 SQL，维护成本更高。

### 2. LLM 接入用 Spring AI + 策略模式

接入层：`spring-ai-openai-spring-boot-starter`，通过 `base-url` 指向 DeepSeek API（OpenAI 兼容协议）。

架构模式：定义 `LlmPort` 接口，`DeepSeekLlmAdapter` 和 `MockLlmAdapter` 各自实现，通过 `@ConditionalOnProperty` 依据 `gleanread.ai.provider` 值自动装配，`AiSynthesisService` 只依赖接口。

理由：换供应商只改 yml，零代码改动；本地无 API Key 时自动退回 Mock，开发体验不受影响。

### 3. 建表 SQL 作为项目文档单独维护

在 `glean-read-server/src/main/resources/db/schema.sql` 中集中维护完整 DDL，作为数据库初始化参考文档（不启用 Spring 自动建表）。

## 数据库变更

```sql
-- 新增：碎片-标签关联中间表
CREATE TABLE fragment_tag (
    fragment_id  BIGINT  NOT NULL REFERENCES fragment(id) ON DELETE CASCADE,
    tag_id       BIGINT  NOT NULL REFERENCES tags(id) ON DELETE CASCADE,
    PRIMARY KEY (fragment_id, tag_id)
);

CREATE INDEX idx_fragment_tag_tag_id      ON fragment_tag(tag_id);
CREATE INDEX idx_fragment_tag_fragment_id ON fragment_tag(fragment_id);
```

## 新增接口

```
GET /api/v1/inbox/fragments/by-tag/{tagId}?current=1&size=20
→ 返回该标签下所有 Inbox 状态（treeNodeId IS NULL）的碎片，分页
```

## 组件结构

```
service/
  llm/
    LlmPort.java                  ← 策略接口
    DeepSeekLlmAdapter.java       ← Spring AI 实现（生产）
    MockLlmAdapter.java           ← 硬编码返回（本地）
mapper/
  FragmentTagMapper.java          ← 新增中间表 Mapper
domain/entity/
  FragmentTag.java                ← 新增中间表实体
```

## 风险 / 权衡

- **Spring AI 版本迭代风险**：Spring AI 仍在 GA 前，API 有概率变化。缓解：只用最核心的 `ChatClient.prompt().call().content()` 调用，隔离在 `DeepSeekLlmAdapter` 内部，变更面最小。
- **DeepSeek API 可用性**：依赖外部服务。缓解：`MockLlmAdapter` 在无 key 时自动兜底，不影响本地开发和测试。
