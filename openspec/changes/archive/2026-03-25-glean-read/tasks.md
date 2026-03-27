## 1. 基础架构搭建
- [x] 1.1 初始化 Spring Boot 3 后端服务工程，配置 PostgreSQL 数据库源及基本依赖。
- [x] 1.2 设计并建立基础的数据库表结构（如 `fragment`, `knowledge_tree_node`, `tags` 及关联表）。
- [x] 1.3 初始化 Android 主工程，配置 Jetpack Compose 和网络层基础组件。

## 2. 移动端极速捕获组件 (fast-capture-widget)
- [x] 2.1 修改 AndroidManifest，注册 `Intent.ACTION_SEND` 分享意图拦截。
- [x] 2.2 设计并实现一个半屏幕尺寸的透明 Compose Activity 浮窗。
- [x] 2.3 在浮窗内实现抽取剪贴板或 Intent 中的“原文”与“URL”。
- [x] 2.4 实现“输入个人附加想法”与“快速多选标签”的用户交互界面。
- [x] 2.5 联调请求发送，调用服务端 `/api/v1/glean/capture` 将新碎片静默保存并销毁浮窗。

## 3. 标签云与碎片收件箱 (tag-cloud-inbox)
- [x] 3.1 实现后端 `/api/v1/glean/capture` 接口，执行碎片的入库并置入 Inbox 状态。
- [x] 3.2 实现标签热度计数器逻辑（当存入附带新标签的摘录时触发递增）。
- [x] 3.3 提供 `/api/v1/inbox/fragments` 分页获取未整理原始碎片列表的拉取方法。
- [x] 3.4 提供 `/api/v1/inbox/tag-cloud` 获取降序标签权重列表，用于驱动标签云渲染。

## 4. AI 大纲合成与知识树建设 (ai-synthesis-tree)
- [x] 4.1 集成外部大语言模型服务 API（预留 Prompt 发送及回复解析层）。
- [x] 4.2 开发特定请求 Prompt，允许传入“M 条知识碎片”的聚合内容，要求 AI 返回带有细分溯源引用的结构化提纲。
- [x] 4.3 提供触发口 `/api/v1/knowledge/synthesis`，并在其成功返回大纲后创建 Tree 节点并双向挂载。
- [x] 4.4 针对前端（Web）提供一棵可读取、可展示完整组织结构的专属 Knowledge Tree 接口。
