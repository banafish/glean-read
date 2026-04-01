## 上下文
系统当前将核心知识结构称为“知识碎片 (Fragment)”。为了提升品牌一致性和语义准确性，决定将其统一重写为“知识摘录 (Excerpt)”。

## 目标 / 非目标
**目标：**
- 全局替换代码库中的 `Fragment` 关键字为 `Excerpt`。
- 重构数据库架构，将 `fragments` 相关表重命名。
- 更新 Android 应用中的所有 UI 文本，将“知识碎片”替换为“知识摘录”。
- 确保 REST API 层提供平滑过渡，支持新旧路径。

**非目标：**
- 本次变更不涉及业务逻辑的重大修改。
- 不涉及除“碎片/摘录”之外的其他领域对象重构。

## 详细设计

### 后端重构 (glean-read-server)
- **领域层**: 
    - `com.gleanread.server.domain.model.fragment` -> `com.gleanread.server.domain.model.excerpt`
    - `Fragment.java` -> `Excerpt.java`
    - `FragmentRepository.java` -> `ExcerptRepository.java`
- **基础设施层**:
    - 更新 MyBatis Plus/JPA 映射，将表 `fragments` 重命名为 `excerpts`。
    - 更新关联表（如 `fragment_tags` -> `excerpt_tags`）。
- **展现层**:
    - `InboxController`: 将 `/api/fragments` 重命名为 `/api/excerpts`。

### 移动端重构 (glean-read-android)
- **网络层**: 更新 Retrofit Service 定义中的路径及 DTO 类名。
- **UI 层**: 搜索整个项目中的 `strings.xml`，将所有“知识碎片”替换为“知识摘录”。
- **架构**: 遵循 IDE 的重构功能对 `Fragment` 相关的包和类进行重命名。

### 规范更新 (openspec)
- 执行全局搜索替换，确保 `specs/` 下的所有文档同步更新术语。

## 关键技术决策
- **数据库迁移**: 使用 SQL 脚本执行 `RENAME TABLE` 操作，确保数据完整性。
- **API 兼容性**: 采用多版本 API 支持或控制器层路由重定向，避免中断正在运行的客户端。

## 风险 / 权衡
- **重构范围大**: 涉及文件众多，需依赖 IDE 的重构工具并进行全案回归测试。
- **外部集成**: 需检查是否有外部系统（如有）依赖原有的 API 路径。

<unlocks>
完成此产出物将启用: tasks
</unlocks>
