## 为什么
目前在移动端极速捕获组件中，接收到分享意图时会直接跳转到 App 主界面。这违反了功能需求中“不打开完整的 App 主界面，而是以半屏弹窗形式弹出轻量级表单”的规定。这会打断用户当前的应用使用流程，影响体验。因此，我们需要修复此问题，使其按照规范以半屏透明弹窗（Transparent Activity / Dialog）的形式完成极速捕获。

## 变更内容
- 修改处理 `Intent.ACTION_SEND` 的入口 Activity，使其不再是全屏或不需要调起主界面。
- 引入或重构一个纯透明/半屏的 Activity（如 `FastCaptureActivity`），用于作为极速捕获的轻量表单承载者。
- 如果之前是路由到了主界面的特定 Fragment，现在需抽离视图以独立呈现弹窗。

## 功能 (Capabilities)

### 新增功能
无

### 修改功能
- `fast-capture-widget`: 修复分享意图的目标 Activity，使其呈现为半屏弹窗表单，避免打开完整 Main Activity 的行为。

## 影响
- Android 端：拦截 `Intent.ACTION_SEND` 的入口配置 (AndroidManifest.xml)。
- 捕获表单视图可能需要适配半屏显示。
- 全局路由如果跟该入口耦合，需要断开。
