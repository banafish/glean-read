# 技术设计：GleanRead Capture V2 UI 重构

## 方案概述 (Overview)
本设计旨在将 `FastCaptureActivity` 从基础的 Material 3 表单重构为一个沉浸式的、受毛玻璃风格启发（Glassmorphism）的极速摘录界面。重点在于利用 Jetpack Compose 的渲染能力实现通透感，并优化视觉层级。

## 目标 (Goals)
- 实现全系统的**通透背景**与**模糊过渡**。
- 采用**圆角底部容器 (Bottom-Sheet Style)**，而非通用的 Dialog。
- 引入**富文本内容预览卡片**，统一摘录风格。

## 详细设计 (Detailed Design)

### 1. 毛玻璃材质 (Glassy Material)
由于 Android 原生的 `RenderEffect` 仅在 API 31+ 表现最佳，我们将采用「兼容方案 + 渐进增强」：
- **兼容层**：使用 `Color.White.copy(alpha = 0.5f)` 或 `Color.Black.copy(alpha = 0.5f)` 作为基底。
- **增强层**：在 API 31+ 设备上应用 `Modifier.graphicsLayer { renderEffect = BlurEffect(30f, 30f) }`。

### 2. 界面结构 (UI Structure)

```kotlin
@Composable
fun CaptureDialogV2(...) {
    // 1. 全局毛玻璃背景 (Surface)
    // 2. 顶部优雅拉环 (Grabber)
    // 3. 内容展示区 (ExcerptCard): 柔光、内阴影
    // 4. 输入交互区 (ThoughtForm): 无边框输入，动态计数器
    // 5. 快速操作区 (ActionStrip): 胶囊标签横向滚动、呼吸感保存按钮
}
```

### 3. 主题系统 (Theme System)
在 `Theme.kt` 中或直接在 Activity 内定义专门的 `CaptureUIColors`：
- `SurfaceColor`: 半透明的主色调（如 Slate Blue）。
- `AccentGradient`: 从蓝到紫的线性渐变，用于主操作按钮。

## 风险与权衡 (Risks & Trade-offs)
- **性能开销**：实时模糊在低端设备上可能存在掉帧。我们可以通过配置参数动态开启或降级到纯透明。
- **系统适配**：Android 底层透明 Activity 的状态栏/导航栏显示需要仔细调试 (`fitsSystemWindows`)。
