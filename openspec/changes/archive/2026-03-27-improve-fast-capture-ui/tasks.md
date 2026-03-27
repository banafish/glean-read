# 任务：重构极速摘录 UI (Capture V2)

## 1. 样式与主题准备
- [x] 1.1 在 `ui/theme/Color.kt` 或 Activity 中定义毛玻璃专属透明色与渐变主色（基于深空蓝与极光紫）。
- [x] 1.2 为 Compose 环境创建通用的 `Modifier.glassyBackground()` 扩展，支持多级模糊与透明度渐变。

## 2. UI 重构与组件开发
- [x] 2.1 构建 `RichExcerptCard` 容器，不仅展示文本，还要提供书摘般的阴影与排版质感。
- [x] 2.2 开发 `GlassyBottomSheet` 总容器，负责处理顶部拉环（Grabber）和至少 `28.dp` 的圆角裁剪。
- [x] 2.3 实现可复用的 `TagPill` 胶囊组件，并支持横向滚动选择已有标签。

## 3. 集成与打磨
- [x] 3.1 将 `FastCaptureActivity.kt` 的 `setContent` 替换为新的 `CaptureDialogV2` 结构。
- [x] 3.2 确保在 Android 12+ 设备上启用真正的 `RenderEffect` 模糊，同时为旧版本提供优雅的半透明回退。
- [x] 3.3 为「保存」按钮添加微缩放与按下时的流光反馈交互。
