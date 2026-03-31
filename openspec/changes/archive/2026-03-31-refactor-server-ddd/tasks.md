# 任务列表：重构 DDD 架构与充血模型

### 1. 结构与领域层充血
- [x] 1.1 创建 `com.gleanread.server.domain.model.fragment` 包及 `FragmentRepository` 接口。
- [x] 1.2 创建 `com.gleanread.server.domain.model.tag` 包及 `TagRepository` 接口，包含 `FragmentTag` 及相关结构。
- [x] 1.3 创建 `com.gleanread.server.domain.model.tree` 包及 `KnowledgeTreeNodeRepository` 接口。
- [x] 1.4 将现有的 `Fragment`, `Tag`, `FragmentTag`, `KnowledgeTreeNode` 实体分别移入到上述对应包中。
- [x] 1.5 为 `Fragment` 增加充血方法 `mountToNode(Long treeNodeId)` 和创建工厂方法。
- [x] 1.6 为 `Tag` 增加充血方法 `incrementHeatWeight()` 和实例化工厂方法。
- [x] 1.7 为 `KnowledgeTreeNode` 增加工厂创建方法。
- [x] 1.8 创建 `com.gleanread.server.domain.port` 并将 `LlmPort` 移入。

### 2. 基础设施层适配
- [x] 2.1 创建 `com.gleanread.server.infrastructure.persistence.mapper` 包，将旧的 Mapper 文件（`FragmentMapper`, `TagMapper`, `FragmentTagMapper`, `KnowledgeTreeNodeMapper`）移入。
- [x] 2.2 创建 `com.gleanread.server.infrastructure.persistence.repository` 包，编写各个 `RepositoryImpl` 类实现领域层的 Repository 接口（内部调用 Mapper）。
- [x] 2.3 创建 `com.gleanread.server.infrastructure.llm` 包，并将 `DeepSeekLlmAdapter`, `MockLlmAdapter` 移入，保证它们实现 `domain.port.LlmPort`。

### 3. 应用服务层(AppService)抽离与重构
- [x] 3.1 创建 `com.gleanread.server.application.service` 和 `com.gleanread.server.application.dto`。
- [x] 3.2 将原有的 `CaptureRequest` 和 `SynthesisRequest` 迁移到 `dto` 包。
- [x] 3.3 新建 `GleanAppService` 替换原 `GleanService`，通过编排 Domain 层的 Repository 接口和实体的充血行为来完成知识碎片的采集和打标签逻辑。
- [x] 3.4 新建 `SynthesisAppService` 替换原 `AiSynthesisService`，逻辑变为：查询碎片列表 -> 组装 Prompt (或者调用领域服务/构建器) -> 调用 `LlmPort` -> 构建新树节点 -> 更新碎片 -> 调用 Repository 保存修改。

### 4. 展现层(Interfaces)更新
- [x] 4.1 创建 `com.gleanread.server.interfaces.rest` 包，将原 `controller` 类移入。
- [x] 4.2 更新 `GleanController`，注入 `GleanAppService` 而不再注入原本服务。
- [x] 4.3 更新 `KnowledgeController`，注入 `SynthesisAppService` 等相关内容。
- [x] 4.4 更新 `InboxController`及其他受影响的控制器，使其依赖 `XXXAppService` 或相关应用的查询服务/Repository。

### 5. 清理与验证
- [x] 5.1 移除废弃的 `com.gleanread.server.service`、`domain.entity` 等老包。
- [x] 5.2 确保 `GleanReadServerApplication` 及整个工程能成功编译启动（测试依赖是否找对与包扫描）。
