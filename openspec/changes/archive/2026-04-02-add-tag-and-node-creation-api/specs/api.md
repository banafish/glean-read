# 增量需求: 独立元数据创建

## 新需求

### 需求: 独立标签创建接口
用户应能够独立于任何知识摘录或挂载操作，创建一个标签。

**验收标准:**
- 支持 `POST /api/v1/tags`。
- 由专门的 `TagController` 处理。
- 请求体必须包含 `tagName`。
- 响应应返回创建后的 `Tag` 完整对象。

### 需求: 独立知识树节点创建接口
用户应能够在知识树中手动创建一个新节点，并支持直接传入初始大纲。

**验收标准:**
- 支持 `POST /api/v1/knowledge/tree/nodes`。
- 请求体包含 `nodeTitle`、可选的 `parentId` 以及可选的 `outlineMarkdown`。
- 响应应包含 `KnowledgeTreeNode` 数据实体。
