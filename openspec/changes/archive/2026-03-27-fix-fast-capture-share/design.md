## 上下文
当前极速捕获组件在接收到 `Intent.ACTION_SEND` 分享意图时，由于路由配置或 Intent 处理逻辑的原因，直接打开了全屏的主应用界面。这不符合“极速、不打扰”的产品定位。为了提供更好的跨应用碎片摘录体验，我们需要让这个流转过程保持在半屏/弹窗状态，以便用户在完成分享后能快速回到原应用。

## 目标 / 非目标
**目标：**
- 修改捕获页面的视图承载，使其在被外部应用分享唤起时，以半屏、背景透明的弹窗形式存在。
- 保证用户提交完碎片或者取消后，平滑返回之前的应用，不被拉起全屏的主 App 界面。

**非目标：**
- 不对组件内部的标签预测、内容解析逻辑做改动。
- 不修改原 App 内打开该组件的常规全屏展示模式（如果有的话）。

## 决策
- **决策一：引入独立的 `FastCaptureActivity`。**
  作为处理分享意图的专属入口，配置其 `theme` 为透明或 Dialog 样式 (`@style/Theme.AppCompat.Translucent` 或自定义半屏 Dialog Theme)。理由是 Activity 级别的透明和出入栈管理更为简单，不易与 `MainActivity` 的生命周期和返回栈发生纠缠。
- **决策二：重构 `CaptureFormContent` 的 Compose 视图。**
  使其能够自适应半屏尺寸，不再依赖于全屏的 Navigation Scaffold 背景，而是提供自己的底部卡片样式背景。

## 风险 / 权衡
- [风险] 透明 Activity 可能会在某些重度定制的 Android 系统中闪现黑屏。-> [缓解措施] 优化 `onCreate` 的加载速度，妥善设置 `windowBackground`。
- [风险] 键盘弹起时遮挡表单。-> [缓解措施] 使用 `adjustResize` 和 Compose 的 imePadding。
