# 规范：DDD 架构与充血模型重构

## 新增需求

### 需求: 提取实体的核心业务防腐
- **Fragment (知识碎片)**：内部管理 `treeNodeId` 的赋值逻辑。需要新增 `mountToNode(Long topicNodeId)` 实例方法代替原来的外部 Setter 操作。
- **Tag (标签)**：新增 `incrementHeat()` 方法代替原来在 Service 层写的自增逻辑加 Setter 操作。新增构建方法 `createNew(String name)` 负责初始化。
- **KnowledgeTreeNode (树节点)**：使用工厂方法创建实例进行统一初始化。

### 需求: 提取持久层防腐接口 (Repository Pattern)
定义 `FragmentRepository`, `TagRepository`, `KnowledgeTreeNodeRepository`，使得 Application 不再直接依赖 `*Mapper`，而是调用 Repository 接口；然后在 Infrastructure 层用现有的 `*Mapper` 去实现这些 Repository。

### 需求: 定义纯应用服务编排 (AppService)
新建 `GleanAppService` 与 `SynthesisAppService`。它们只负责协调：从 Repo 中查出对象 -> 调用对象的行为（充血模型） -> 将查得或变动的对象通过 Repo 保存回去。不允许直接包含任何字符串组装、特定条件判定等业务逻辑（例如组装 AI 的 prompt 这种逻辑应该封装到 Domain Service 或专门的策略构建类中）。

## 修改需求

### 需求: 修改 GleanService 逻辑层分离
现有的 `GleanService` 被拆分为：
1. `GleanAppService` (在 `application` 目录)。
2. `Tag` 和 `Fragment` 的充血行为 (在 `domain/model` 目录)。
3. MyBatis Mapper 的调用 (在 `infrastructure/persistence` 目录中的 Repo 实现)。

### 需求: 修改 AiSynthesisService 逻辑层分离
现有的 `AiSynthesisService` 被拆分为：
1. `SynthesisAppService` (在 `application` 目录)。
2. 内部逻辑使用领域行为：获取一组 `Fragment` -> `SynthesisAppService` 将它传给 `domain/port` 或者领域服务。Prompt 的组装和模型的调用被放入 `domain` 处理或者基础设施层按接口完成。
3. `LlmPort` 从 `service.llm` 移至 `domain.port` 内，具体的 `DeepSeekLlmAdapter` 和 `MockLlmAdapter` 应该在 `infrastructure/llm` 目录下。

## 移除需求
- 移除：直接暴露在表现层的 `GleanService` 和 `AiSynthesisService`，以及它们对于 `*Mapper` 的直接自动装配。
