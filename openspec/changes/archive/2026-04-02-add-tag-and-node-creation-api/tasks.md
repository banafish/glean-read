# 任务清单

## 1. 标签（Tag）独立创建功能

- [x] 1.1 在 `com.gleanread.server.application.service` 包下创建 `TagAppService` 接口及其实现类。
- [x] 1.2 创建 `com.gleanread.server.interfaces.rest.TagController`。
- [x] 1.3 在 `TagController` 中增加 `POST /api/v1/tags` 接口。
- [x] 1.4 实现标签重名校验逻辑（若已存在则返回现有标签）。

## 2. 知识树节点（Node）独立创建功能

- [x] 2.1 在 `com.gleanread.server.application.service` 包下创建 `KnowledgeTreeAppService` 接口及其实现类。
- [x] 2.2 在 `KnowledgeController` 中增加 `POST /api/v1/knowledge/tree/nodes` 接口。
- [x] 2.3 支持在创建节点时传入 `outlineMarkdown` 参数。
- [x] 2.4 实现父节点存在性校验逻辑。

## 3. 验证与清理

- [x] 3.1 编写简单的测试用例或手动测试 API 确保功能正常。
- [x] 3.2 确保不破坏现有的 Excerpt 挂载流程。
