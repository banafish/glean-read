## 1. 数据库与基础设施

- [x] 1.1 在 `src/main/resources/db/schema.sql` 中编写并维护完整建表 DDL（包含 `fragment`、`tags`、`fragment_tag`、`knowledge_tree_node` 四张表及索引）。
- [x] 1.2 在 `pom.xml` 中引入 `spring-ai-openai-spring-boot-starter` 依赖。
- [x] 1.3 在 `application.yml` 中增加 AI 供应商配置段：`gleanread.ai.provider`、`spring.ai.openai.base-url`、`spring.ai.openai.api-key`。

## 2. Fragment ↔ Tag 关联 (fragment-tag-relation)

- [x] 2.1 新建 `domain/entity/FragmentTag.java` 实体类，映射 `fragment_tag` 中间表（联合主键 `fragmentId` + `tagId`）。
- [x] 2.2 新建 `mapper/FragmentTagMapper.java`，继承 `BaseMapper<FragmentTag>`。
- [x] 2.3 修改 `GleanService.captureFragment()`：在标签热度递增逻辑后，额外调用 `fragmentTagMapper.insert()` 写入每条碎片-标签关联记录。
- [x] 2.4 在 `InboxController` 中新增 `GET /api/v1/inbox/fragments/by-tag/{tagId}` 端点：通过 `fragment_tag` 关联表，分页返回指定标签下 `treeNodeId IS NULL` 的碎片。

## 3. LLM 策略接口与适配器 (llm-integration)

- [x] 3.1 新建 `service/llm/LlmPort.java` 策略接口，声明 `String complete(String prompt)` 方法。
- [x] 3.2 新建 `service/llm/DeepSeekLlmAdapter.java`，注入 Spring AI 的 `ChatClient`，实现 `LlmPort`，用 `@ConditionalOnProperty(name="gleanread.ai.provider", havingValue="deepseek")` 条件装配。
- [x] 3.3 新建 `service/llm/MockLlmAdapter.java`，将 `AiSynthesisService` 中原有的 `mockInvokeLlm()` 逻辑迁移至此，用 `@ConditionalOnProperty(name="gleanread.ai.provider", havingValue="mock", matchIfMissing=true)` 作为默认回退。
- [x] 3.4 修改 `AiSynthesisService`：删除 `mockInvokeLlm()` 方法，注入 `LlmPort llm`，将 `String outlineMarkdown = mockInvokeLlm(prompt)` 替换为 `llm.complete(prompt)`。
