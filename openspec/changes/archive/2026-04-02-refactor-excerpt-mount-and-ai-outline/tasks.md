# 任务清单: refactor-excerpt-mount-and-ai-outline

## 后端任务 (Server)

1. [x] **AI 大纲生成支持**
   - 在 `AiSummaryService` (或类似负责 AI 的服务类) 中新增方法 `generateOutlineForExcerpts(List<Long> excerptIds)`。
   - 方法内组合 Spring AI / DeepSeek 完成基于多条摘录的主题提取和文本结构化构建，将结果返回为字符串形式的大纲内容。

2. [x] **AI 大纲生成接口暴露**
   - 在对应的 Controller (比如 `InboxController` 或者 `KnowledgeTreeController`) 增加一个 `POST /api/ai/outline/batch` 接口。
   - 接口接受 `excerptIds` 列表，返回生成的 Outline 字符串。

3. [x] **挂载接口重定义与修改**
   - 修改原有的摘录挂载端点（如 `POST /api/inbox/mount` 形式的），增加可空的入参 `targetNodeId`。
   - 更新挂载业务流：当指定 `targetNodeId` 时挂载到对应节点，否则挂载为根节点或者归属 Inbox。
   - **重要**：从挂载流中，剥离移除对 `AiSummaryService` 隐式的异步或同步大纲生成调用逻辑，确保单一职责化。
