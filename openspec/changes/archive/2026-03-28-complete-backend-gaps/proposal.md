# 提案：补全后端核心功能缺口 (complete-backend-gaps)

## 动机 (Why)

在对 `2026-03-25-glean-read` 归档变更与 `glean-read-server` 现有实现进行对比后，发现两处关键功能存在实现空白，导致整个知识摘录→AI合成的数据链路无法闭环：

1. **Fragment ↔ Tag 关联丢失**：`GleanService.captureFragment()` 虽然保存了碎片并累加了标签热度，但始终没有写入碎片与标签之间的多对多关联表。这导致无法从某个标签反向查询其关联的碎片列表——而这正是触发 AI 合成的前序步骤。

2. **LLM 接入仍为 Mock**：`AiSynthesisService` 中的 `mockInvokeLlm()` 是一段硬编码字符串，真实的大语言模型（如 DeepSeek）从未被接入。整个 AI 大纲合成功能目前形同虚设。

## 变更内容 (What Changes)

### 新增功能
- `fragment-tag-relation`：新增 `fragment_tag` 中间表及完整的建表 SQL，补全碎片与标签的多对多关联写入逻辑，并提供按标签筛选碎片的查询接口。
- `llm-integration`：引入 `LlmPort` 策略接口，实现 `DeepSeekLlmAdapter`（基于 Spring AI）和 `MockLlmAdapter`（本地测试用），通过配置文件切换供应商，替换现有的 mock 硬编码。

### 修改功能
- `GleanService.captureFragment()`：在保存碎片后，额外写入 `fragment_tag` 关联记录。
- `InboxController`：新增按 tag 筛选碎片的查询端点，以支持 AI 合成选碎片的前序流程。
- `AiSynthesisService`：将 `mockInvokeLlm()` 调用替换为注入的 `LlmPort` 接口调用。

## 影响 (Impact)

- **数据库**：需新增 `fragment_tag` 关联表，并建立相应索引。
- **依赖**：`pom.xml` 引入 `spring-ai-openai-spring-boot-starter`；`application.yml` 增加 AI 供应商配置段及 API Key 占位符。
- **现有接口**：`/api/v1/glean/capture` 行为不变，向下兼容；新增 `/api/v1/inbox/fragments/by-tag/{tagId}` 端点。
- **配置**：新增 `gleanread.ai.provider`（`deepseek` / `mock`）配置项用于切换 LLM 适配器。
