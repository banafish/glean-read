## 1. 数据与管线基础

- [x] 1.1 新增 `CapturePreferencesRepository`（DataStore，`wechat_capture_bubble_enabled` 默认开）并注册到 `AppContainer`
- [x] 1.2 服务配置 XML 追加 `typeNotificationStateChanged`；`PageContextAccessibilityPolicy` 新增微信复制成功 toast 路由判定（限定 `android.widget.Toast` 类名）；服务入口按事件类型+包名前置分流，浏览器快照管线不变
- [x] 1.3 `PageContextSupport` 增加按包名 TTL 与合并窗口；`PageContextStore.readRecentSnapshot` 按已存快照包名取 TTL，新增 `mergeWeChatUrl`；新增 `WeChatCaptureContract` 与 `WeChatCaptureSignalStore`

## 2. 微信识别与气泡

- [x] 2.1 新增 `WeChatCopyToastClassifier`（复制成功 toast 识别：简中/繁中/英文包含匹配 + 失败词排除；真机验证按钮点击事件无文本后由 `WeChatClickClassifier` 改造而来）、`WeChatArticlePageDetector`（网页容器 BFS + windowId 缓存，另有窗口类名含 WebView 的兜底判定）、`WeChatArticleTitleExtractor`（打分制标题提取），全部纯逻辑可 JVM 单测
- [x] 2.2 新增 `BubbleOverlayHost`（ComposeView + 手工 LifecycleOwner）、`WeChatBubbleController`（TYPE_ACCESSIBILITY_OVERLAY、8s 自动消失、防重复叠加）、`WeChatCaptureCoordinator`（去抖→分类→文章页判定→信号→气泡→拉起弹窗），服务装配并收集开关 Flow

## 3. 弹窗两阶段种子

- [x] 3.1 新增 `WeChatClipboardResolver`（新鲜度校验 + URL/正文判别）；`CaptureSeedResolver` 新增 `ACTION_WECHAT_CAPTURE` 分支（content 恒空、不做标题回退）
- [x] 3.2 `FastCaptureActivity` 改为两阶段 seed（mutableState + 首次获焦读剪贴板 + URL 回写缓存）；`FastCaptureScreen.currentUrl` 以 seed.url 为重置键

## 4. 设置与清理

- [x] 4.1 设置页无障碍卡片新增「微信摘录气泡」M3 Switch（UiState/ViewModel/Route/Screen/Section 贯通）
- [x] 4.2 微信窗口事件切换到新标题管线；删除 `PageContextNodeExtractor` 微信分支，并给出浏览器行为等价性论证

## 5. 验证

- [x] 5.1 单测：Classifier/Detector/TitleExtractor/ClipboardResolver/Store TTL/SeedResolver 微信分支/Policy 路由/偏好仓库；既有浏览器用例零修改通过（testDebugUnitTest 165 例 0 失败；assembleDebug 与 assembleRelease/R8 通过）
- [x] 5.2 真机第一轮（Honor）：定位「气泡不出现」根因——自绘菜单点击事件不带文本；触发方式重写为「复制成功 toast」（testDebugUnitTest 164 例 0 失败，debug/release 构建通过）
- [x] 5.3 真机复验（Honor）：定位并修复三处真机专属缺陷——① dialog 分享菜单窗口事件污染文章页兜底类名（仅类名以 UI 结尾的 Activity 窗口才更新兜底记录）；② ModalBottomSheet 独立 dialog 窗口致 Activity 主窗口在部分 ROM 永不获焦，剪贴板读取改为「sheet 窗口获焦 + Activity 主窗口获焦」双信号；③ 复制正文/复制链接均可出气泡、点气泡正文回填正确。已移除全部 WeChatCaptureDx 诊断日志（testDebugUnitTest 166 例 0 失败，debug/release/R8 构建通过）
