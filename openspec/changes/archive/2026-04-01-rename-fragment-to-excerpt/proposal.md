# 提案: 将“知识碎片”重构为“知识摘录”

## 背景与动机
为了更好地体现产品对内容的提炼属性，并与现有的“快捕卡片”概念进行区分，需要将系统内的核心概念“知识碎片 (Knowledge Fragment)”统一重构为“知识摘录 (Knowledge Excerpt)”。本次变更旨在统一全链条（从后台领域模型到前端 UI）的术语表述，并完成对应的代码库重构。

## 变更内容
本变更将涉及全栈的代码重构与文档更新：
- **领域模型**: 将 `Fragment` 及其关联对象（如 `FragmentRepository`, `FragmentDTO`）重构为 `Excerpt`。
- **持久化层**: 更新数据库表名（如果适用）或映射配置。
- **接口层**: 更新 REST API 路径或参数（视兼容性需求而定，初步计划保持兼容或平滑迁移）。
- **客户端**: 更新 Android App 内部类名、UI 文本及字符串资源。
- **规范文档**: 同步更新 `openspec` 下的所有现有规范。

## 影响面
- **glean-read-server**: 涉及 `com.gleanread.server.domain.model.fragment` 包及相关层级。
- **glean-read-android**: 涉及 UI 文本和网络请求模型。
- **openspec**: 全局规范文本替换。

## 功能 (Capabilities)
- `knowledge-excerpt`: 定义并实现重构后的“知识摘录”核心领域模型及其行为。

<success_criteria>
- 后端 `Fragment` 相关 Java 类全部重命名为 `Excerpt`。
- 数据库及 Repository 逻辑能正确处理重命名后的实体。
- Android UI 及 文档中的“知识碎片”字样被替换为“知识摘录”。
</success_criteria>

<unlocks>
完成此产出物将启用: design, specs
</unlocks>
