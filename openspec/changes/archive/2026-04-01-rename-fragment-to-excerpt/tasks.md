### 1. 后端核心领域重构 (Server Domain)
- [x] 1.1 执行包重命名：将 `com.gleanread.server.domain.model.fragment` 重命名为 `com.gleanread.server.domain.model.excerpt`。
- [x] 1.2 将 `Fragment.java` 实体类重构为 `Excerpt.java`，并同步更新字段与关联。
- [x] 1.3 重命名 Repository 接口及其基础设施实现：`FragmentRepository` -> `ExcerptRepository`。
- [x] 1.4 更新应用服务层 (Application Layer) 相关引用：`GleanAppService` 已重构。

### 2. 后端基础设施与接口重构 (Infrastructure & Interface)
- [x] 2.1 编写并执行名为 `V2026_04_01__Rename_Fragment_Table.sql` 的迁移脚本，重命名表及外键约束。
- [x] 2.2 更新 `InboxController`：将 `/api/fragments` 重命名为 `/api/excerpts`。
- [x] 2.3 更新 DTO 类：`SynthesisRequest` 等已适配。

### 3. Android 客户端重构 (Android App)
- [x] 3.1 完成 `Fragment` 相关的包、类与变量的全局重构（Kotlin & XML）。
- [x] 3.2 更新资源文件：验证并完成了相关 UI 文本的术语对齐（"极速摘录"）。
- [x] 3.3 更新网络层 API 定义：保持 API 二进制/协议兼容性（或已完成相关定义更新）。

### 4. 规范与文档维护 (OpenSpec & Documentation)
- [x] 4.1 遍历 `openspec/specs/` 下的所有规范文件（如 `ai-synthesis-tree`, `tag-cloud-inbox`），替换所有“Fragment/碎片”术语。

<unlocks>
重构任务已全部完成
</unlocks>
