# 技术设计：重构摘录挂载与 AI 总结大纲功能

## 上下文
目前系统在处理摘录挂载到知识树的过程中，存在隐式的逻辑捆绑：挂载不仅完成关联操作，还在后台或者某个环节自动触发了针对该摘录（或关联）的 AI 总结 Outline 逻辑。这不仅让接口职责不够单一，也难以满足先预览和编辑 Outline、后再决定如何把摘录挂载到指定父节点的进阶需求。

## 目标 / 非目标

**目标：**
- 新增一个 `batch-ai-outline-generation` 的能力后端接口。
- 改造现有的挂载知识树接口，可以显式接收目标 `KnowledgeTreeNode` 的 ID。
- 在挂载操作中解除并移除原来绑定的大纲生成逻辑。

**非目标：**
- 重新设计底层的 KnowledgeTree 存储和模型本身。
- 改变当前 DeepSeek / Spring AI 的调用机制本身（只做组合和拆分，不更换 AI 底座或实现）。

## 架构

后端（Server）：
1. **AI 服务层**：
   暴露一个新的方法 `generateOutlineForExcerpts(List<Long> excerptIds)`，该方法使用 DeepSeek/SpringAI 能力基于传入的摘录内容生成纯文本或结构化的 outline 并返回给调用方。
2. **控制器暴露**：
   新增端点 `POST /api/ai/outline/batch`，Body: `{ excerptIds: [1, 2, 3] }`，返回大纲。
3. **知识树挂载**：
   修改 `mount` 或 `KnowledgeTreeController` 等端点，添加 `targetNodeId` 的入参。挂载时将摘录关联至特定的 Node，不再执行隐式的 AI summarization。

## 数据模型修改
现有的数据库模型本身（`KnowledgeTreeNode`, `Excerpt` 等表）不需要强制变动。只需确保摘录可以挂载到指定的树节点上即可。

## 决策
- **AI 接口形式**：暂定为同步长连接形式（或者客户端通过等待动画覆盖等待），以维持最简单的请求-响应闭环。

## 风险 / 权衡
- **现有挂载流程兼容性**：需要全面替换旧的挂载端点行为，并保证客户端配套升级，否则旧客户端可能因后端挂载不再自动触发总结而丢失业务流特性。
