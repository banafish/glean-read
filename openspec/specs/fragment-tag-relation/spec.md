## 新增需求

### 需求：碎片与标签的多对多关联持久化

系统必须在碎片入库时同时写入 `fragment_tag` 关联表，并提供按标签反向检索碎片的能力，以支撑 AI 合成的选料前序流程。

#### 场景：带标签摘录入库，关联记录正确写入

- **当** 移动端提交一条附带 `["架构", "后端"]` 标签的摘录
- **那么** 系统在 `fragment` 表新增碎片记录，在 `tags` 表累加 `#架构` 和 `#后端` 的 `heatWeight`，并在 `fragment_tag` 表写入两条关联记录 `(fragment_id, tag_id)`

#### 场景：按标签筛选 Inbox 碎片

- **当** 前端/用户在 AI 合成前，通过 `GET /api/v1/inbox/fragments/by-tag/{tagId}` 请求某标签下的待处理碎片
- **那么** 系统通过 `fragment_tag` 关联表，返回该标签下所有 `treeNodeId IS NULL`（Inbox 状态）的碎片分页列表
