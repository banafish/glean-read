## 上下文

当前系统架构中，标签（Tag）和知识树节点（KnowledgeTreeNode）的创建逻辑高度耦合在“挂载（Mount）”流程中。用户无法独立创建这些实体，限制了在没有具体摘录内容时预建知识分类体系的能力。

## 目标 / 非目标

**目标：**
- 提供独立的 API 接口用于创建 Tag。
- 提供独立的 API 接口用于在知识树中创建 Node。
- 遵循 DDD 架构，实现相应的 Application Service 和 Repository 调用。

**非目标：**
- 修改现有的挂载逻辑（保持兼容）。
- 实现复杂的树形结构重构（如挪动节点）。

## 关键设计决策

### 1. 接口定义
- **TagController** (`POST /api/v1/tags`):
    - 请求体: `{ "tagName": "string" }`
    - 返回值: 创建成功的 Tag 对象。
- **KnowledgeController** (`POST /api/v1/knowledge/tree/nodes`):
    - 请求体: `{ "nodeTitle": "string", "parentId": long, "outlineMarkdown": "string" }`
    - 返回值: 创建成功的 KnowledgeTreeNode 对象。

### 2. 逻辑分层
- **Interfaces**: 新增 `TagController`；在 `KnowledgeController` 中增加节点创建接口。
- **Application**: 增加 `KnowledgeManagementAppService` 处理非合成（纯增删改查）的管理逻辑。
- **Domain**: 复用 `Tag.createNew()` 和 `KnowledgeTreeNode.create()` 工厂方法。

## 风险 / 权衡

- **重名风险**: 目前领域模型未强制要求标签名唯一，但在独立创建时建议进行基本校验。
- **闭图风波**: 知识树节点创建时需确保 `parentId` 有效（除非为根节点）。
