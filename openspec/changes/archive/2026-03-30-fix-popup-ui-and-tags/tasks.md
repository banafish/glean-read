# 实施任务：修复弹出层 UI 与标签展示

## 1. 弹出层容器重构 (Shadow Fix)

- [x] 1.1 在 `FastCaptureActivity.kt` 中重构 `TagMenuPopup`：将 `graphicsLayer` 的阴影逻辑改为使用 `Surface` 或标准 `Modifier.shadow` + 显式 `shape`。
- [x] 1.2 在 `FastCaptureActivity.kt` 中重构 `LinkMenuPopup`：应用同样的阴影修复，确保视觉统一。
- [x] 1.3 验证深色模式下的颜色显示，确保弹出层不因默认 Material 反色策略变色（强制 `color = Color.White`）。

## 2. 标签云增强 (Tag Cloud scrolling)

- [x] 2.1 在 `TagMenuPopup` 中将 `FlowRow` 包装在带有 `Modifier.heightIn(max = 130.dp).verticalScroll(rememberScrollState())` 的 `Column` 中。
- [x] 2.2 测试标签较多（超过 10 个）时，确保滚动条可正常工作且高度不会撑满屏幕。
- [x] 2.3 测试标签较少（如 2-3 个）时，容器应自适应收缩。

## 3. 视觉验证

- [x] 3.1 运行测试（手动）确认弹出动画期间无阴影畸变。
- [x] 3.2 确认弹出层宽度（`260.dp` / `280.dp`）在标准手机屏幕上排版美观。
