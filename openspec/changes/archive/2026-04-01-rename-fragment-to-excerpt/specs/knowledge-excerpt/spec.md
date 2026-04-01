# 规范：知识摘录 (Knowledge Excerpt)

## 核心定义
“知识摘录”是系统中最基本的知识单元，承载用户从网页、书籍或其他来源记录下来的片段信息。它是之前“知识碎片”的语义对齐版本。

## 需求变更

### 新增需求
- **术语对齐**: 系统内所有可见文本、API 路径和数据库标识符应统一使用“知识摘录”或“Excerpt”。

### 修改需求: [知识碎片核心模型]
- **重命名实体**: 所有的 `Fragment` 及其关联表（如 `fragment_heat_weight`）必须无损遷移并重命名为 `Excerpt` 相关名称。
- **UI 标签替换**: 移动端 UI（如弹窗标题、快速捕获界面）中出现的“知识碎片”全部更新为“知识摘录”。

### 移除需求
- **废弃原有术语**: 禁止在新的规范、文档和代码中使用“Fragment”一词来描述此领域对象。

## 验收准则
- 数据库表名变更为 `excerpts` 且原数据完整。
- Java 类 `com.gleanread.server.domain.model.excerpt.Excerpt` 已被定义并替代原有类。
- Android 客户端请求 `/api/excerpts` 成功。
- 所有 `openspec/specs` 下的存量文档已同步。

<unlocks>
完成此产出物将启用: tasks
</unlocks>
