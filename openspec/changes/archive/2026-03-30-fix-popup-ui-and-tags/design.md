# 设计：修复弹出层 UI 与标签展示

## 上下文
当前 `FastCaptureActivity` 的弹出层由 `Box` + `graphicsLayer` 实现阴影。由于 Compose `graphicsLayer` 的阴影渲染与 `AnimatedVisibility` 的动画组合时，可能在某些 Android 版本的硬件加速路径上产生形状延迟应用，导致“先方后圆”。

## 目标
- 阴影在动画全程与圆角矩形对齐。
- 标签层在标签较多时可滚动，且高度受控。

## 详细设计

### 1. 弹出层阴影修复
- **方案**：弃用 `graphicsLayer` 手动绘制阴影，改用 `Surface` 组件。
- **原因**：`Surface` 是 Material Design 的标准容器，它内部处理了阴影渲染逻辑，并会自动将阴影与 `shape` 参数对齐。
- **调整点**：
    - 在 `TagMenuPopup` 和 `LinkMenuPopup` 中，将最外层的 `Box` 替换为（或包裹在）`Surface` 中。
    - 统一 `elevation` 为 `12.dp`（根据原代码 `20.dp` 进行微调，使之更柔和）。
    - 确保 `Surface` 的 `color` 设置为 `Color.White`，以保持其不透明度对阴影渲染的影响。

### 2. 标签层滚动与限高
- **方案**：为 `FlowRow` 引入滚动容器。
- **实现方案**：
    ```kotlin
    Column(
        modifier = Modifier
            .heightIn(max = 130.dp) // 容纳约 2.5 行标签的高度
            .verticalScroll(rememberScrollState())
            .padding(vertical = 4.dp)
    ) {
        FlowRow(...) { ... }
    }
    ```
- **关键参数**：
    - `heightIn(max = 130.dp)`：确保在标签少时自适应高度，在标签多时固定最大高度，避免挤占输入区。
    - `verticalScroll`：确保超出最大高度时内容可滚动。

## 决策记录
- **使用 Surface 而非 Box**：虽然原代码注释提到为了规避深色模式反色而弃用 `Surface`，但实际上可以通过设置 `Surface(color = Color.White)` 来通过底层 Material 主题强制显色。如果仍然存在问题，我们将回退到 `Modifier.shadow(elevation, shape)`。

## 风险 / 权衡
- `heightIn` 的固定值可能在不同字号/密度下略有偏差，建议使用 `130.dp` 作为一个保守的、能容纳 2-3 行的安全值。
