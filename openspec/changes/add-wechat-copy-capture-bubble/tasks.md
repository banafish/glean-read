## 1. 数据与管线基础

- [x] 1.1 新增 `CapturePreferencesRepository`（DataStore，`wechat_capture_bubble_enabled` 默认开）并注册到 `AppContainer`
- [x] 1.2 服务配置 XML 追加 `typeViewClicked`；`PageContextAccessibilityPolicy` 新增微信点击路由判定；服务入口按事件类型+包名前置分流，浏览器快照管线不变
- [x] 1.3 `PageContextSupport` 增加按包名 TTL 与合并窗口；`PageContextStore.readRecentSnapshot` 按已存快照包名取 TTL，新增 `mergeWeChatUrl`；新增 `WeChatCaptureContract` 与 `WeChatCaptureSignalStore`

## 2. 微信识别与气泡

- [x] 2.1 新增 `WeChatClickClassifier`（简中/繁中/英文词表精确匹配，复制链接优先）、`WeChatArticlePageDetector`（网页容器 BFS + windowId 缓存）、`WeChatArticleTitleExtractor`（打分制标题提取），全部纯逻辑可 JVM 单测
- [x] 2.2 新增 `BubbleOverlayHost`（ComposeView + 手工 LifecycleOwner）、`WeChatBubbleController`（TYPE_ACCESSIBILITY_OVERLAY、8s 自动消失、防重复叠加）、`WeChatCaptureCoordinator`（去抖→分类→文章页判定→信号→气泡→拉起弹窗），服务装配并收集开关 Flow

## 3. 弹窗两阶段种子

- [x] 3.1 新增 `WeChatClipboardResolver`（新鲜度校验 + URL/正文判别）；`CaptureSeedResolver` 新增 `ACTION_WECHAT_CAPTURE` 分支（content 恒空、不做标题回退）
- [x] 3.2 `FastCaptureActivity` 改为两阶段 seed（mutableState + 首次获焦读剪贴板 + URL 回写缓存）；`FastCaptureScreen.currentUrl` 以 seed.url 为重置键

## 4. 设置与清理

- [x] 4.1 设置页无障碍卡片新增「微信摘录气泡」M3 Switch（UiState/ViewModel/Route/Screen/Section 贯通）
- [x] 4.2 微信窗口事件切换到新标题管线；删除 `PageContextNodeExtractor` 微信分支，并给出浏览器行为等价性论证

## 5. 验证

- [x] 5.1 单测：Classifier/Detector/TitleExtractor/ClipboardResolver/Store TTL/SeedResolver 微信分支/Policy 路由/偏好仓库；既有浏览器用例零修改通过（testDebugUnitTest 165 例 0 失败；assembleDebug 与 assembleRelease/R8 通过）
- [ ] 5.2 真机验收：公众号复制出气泡、聊天页不出、点气泡弹窗正文正确、复制链接回写 URL、10 分钟 TTL、开关即时生效、Chrome 三入口回归（建议原生系 + MIUI 各一台）
