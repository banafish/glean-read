## 1. 入口与 Activity 搭建

- [x] 1.1 在现有的 `glean-read-android/app/src/main/java/com/gleanread/android/` 目录中，创建 `FastCaptureActivity.kt`，继承自 `ComponentActivity`，并在 onCreate 中配置好 Compose 环境。
- [x] 1.2 在 `AndroidManifest.xml` 中将原本针对 `Intent.ACTION_SEND` 的拦截（针对 text/plain）从 `MainActivity` 挪到新的 `FastCaptureActivity`。
- [x] 1.3 为 `FastCaptureActivity` 设置透明的主题 (例如 `@style/Theme.AppCompat.Translucent.NoTitleBar` 或 `android:theme="@android:style/Theme.Translucent.NoTitleBar"`），保证它不会打开全屏黑色背景。

## 2. 视图与半屏适配

- [x] 2.1 修改原本的分享表单 Compose 界面（如果存在类似 `CaptureFormContent` 的 Composable），使其支持依附在底部并且有带圆角的白色背景，而不是撑满全栈。
- [x] 2.2 在 `FastCaptureActivity` 的 `setContent` 中，用一个外层黑色半透明的可点击遮罩 (`Box` + `clickable`) 包裹表单视图，实现点击表单外部即可消失的功能。
- [x] 2.3 确保键盘弹起时表单能自适应推流（`Modifier.imePadding()`）且不被遮挡。

## 3. 功能迁移与验证

- [x] 3.1 确保从 Intent 成功提取到解析后的分享文本/分享 URL 并填充表单。
- [x] 3.2 确保在点击“确认保存”（触发发送 API 调用），或者点击遮罩等取消操作时，调用 `Activity.finish()` 结束生命周期，完美平滑退回发起分享的目标 App 应用。
