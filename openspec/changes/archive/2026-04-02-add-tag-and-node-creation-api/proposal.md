# 提案：增加 Tag 和知识树 Node 独立创建接口

## 摘要

本变更旨在为系统增加独立创建标签（Tag）和知识树节点（KnowledgeTreeNode）的 REST API 接口。目前这些实体的创建通常与知识摘录（Excerpt）的挂载逻辑耦合，增加独立接口可以提升系统的灵活性，允许用户预先构建知识架构。

## 动机

- **解耦**: 现有逻辑下，标签和节点的创建往往是“挂载（Mount）”过程的副产品。用户无法在没有摘录的情况下预先定义分类体系。
- **灵活性**: 允许管理后台或第三方集成独立维护标签库和知识树结构。
- **用户体验**: 提升用户在整理知识架构时的效率，无需先有内容再有结构。

## 需求变更

- 无：本项目目前的 spec 可能尚未显式定义独立创建的流程，本次变更为“向后兼容”的增量功能。

## 影响

- **接口层**: 
    - 新增 `TagController` 处理 `POST /api/v1/tags`。
    - 在 `KnowledgeController` 中增加 `POST /api/v1/knowledge/tree/nodes`（支持传入初始 `outlineMarkdown`）。
- **应用层**: 
    - 增加 `TagAppService` 和 `KnowledgeTreeAppService`。
- **领域层**: 
    - 调用 `Tag` 和 `KnowledgeTreeNode` 的构造逻辑及仓储实现。

## 成功标准

- 用户可以通过 `POST /api/v1/tags` 成功创建一个新标签，并能通过 ID 查询到。
- 用户可以通过 `POST /api/v1/knowledge/tree/nodes` 在指定父节点下（或根节点）创建一个新节点。
- 创建过程中执行必要的校验（如：标签名称不能为空，父节点必须存在等）。
