# 提案: 重构为 DDD 架构与充血模型

## 背景与目标
当前 `glean-read-server` 的业务逻辑（如捕捉碎片、计算标签热度、请求 AI 合成大纲、碎片节点关联）高度集中在 `GleanService` 和 `AiSynthesisService` 这类“胖服务（Fat Service）”中，而对应的实体（如 `Fragment`、`KnowledgeTreeNode`、`Tag`）则是典型的贫血模型（Anemic Domain Model），只包含数据而没有行为。

这种强耦合的事务脚本模式在早期能够快速迭代，但随着业务复杂度增加，核心业务逻辑散落，导致领域概念模糊，难以维护。

因此，我们的目标是：将基于 MVC + 事务脚本的系统重构为标准的 **DDD (Domain-Driven Design) 四层架构**，并将核心业务逻辑下沉到实体中，实现**充血模型**（Rich Domain Model）。

## 本次重构目标
1. **领域模型充血化**：
   - 将对实体的操作封装在实体自身中（如 `Tag` 的 `incrementHeat` 方法，`Fragment` 的 `mountToNode` 方法）。
2. **架构分层**：
   - 展现层（Interfaces/Controller）：接收 API 请求，参数校验。
   - 应用服务层（Application）：用例编排（Orchestration），获取并协调领域对象，不包含核心业务规则。
   - 领域层（Domain）：纯业务逻辑，不依赖基础设施。包含 Entities, Value Objects, Domain Services 以及 Repository 接口。
   - 基础设施层（Infrastructure）：数据库访问（Mybatis Plus Mapper）、第三方 API 调用（DeepSeek 等大模型客户端实现 `LlmPort`）及其他技术细节。
3. **保持向后兼容**：仅重构内部包结构和代码逻辑，保持对外的 REST API 不受任何影响。

## 影响范围
- **代码库**：`glean-read-server`。
- **重构目录**：几乎所有现有类的位置都会被移动以匹配全新的依赖倒置架构；`GleanService` 和 `AiSynthesisService` 里的代码将被拆分并迁移到 Domain 实体和 Application 服务。
- **不影响**：数据库表结构不变（实体仍会打 MyBatis Plus 注解以简化开发），API 签名不变，移动端调用逻辑不变。
