# 任务：auto-capture-url-on-share

## 1. 新建 UrlExtractor 工具类

- [x] 1.1 在 `app/src/main/java/com/gleanread/android/` 新建 `UrlExtractor.kt`，实现 `object UrlExtractor` 包含 `extract(intent: Intent): String?` 函数
- [x] 1.2 实现策略1：`EXTRA_TEXT` 整体以 `http://` 或 `https://` 开头，直接返回
- [x] 1.3 实现策略2：`EXTRA_STREAM` Uri 转字符串后以 `http://` 或 `https://` 开头，返回该字符串
- [x] 1.4 实现策略3：用正则 `https?://[^\s\u4e00-\u9fff"'<>]+` 从 `EXTRA_TEXT` 末尾提取 URL（通过 `endsWith` 验证是否真为末尾）
- [x] 1.5 实现策略4：用正则从完整 `EXTRA_TEXT` 任意位置提取第一个合法 URL
- [x] 1.6 策略5（兜底）：以上均无结果时返回 `null`
- [x] 1.7 提取私有扩展函数 `String.isValidUrl()`，用于合法性校验

## 2. 修改 FastCaptureActivity.kt — Intent 解析层

- [x] 2.1 在 `onCreate` 中删除 L49-61 的原有 URL 提取逻辑（前缀匹配）
- [x] 2.2 改为调用 `UrlExtractor.extract(intent) ?: ""` 获取 `sharedUrl`
- [x] 2.3 将 `sharedUrl` 作为 `initialUrl` 参数传入 `CaptureDialogV2`（参数签名已存在，确认传参正确）

## 3. 新增 UI 组件 — SourceBadge

- [x] 3.1 在 `CaptureComponents.kt` 新增 `@Composable fun SourceBadge(url: String)` 组件
- [x] 3.2 `SourceBadge` 解析 `url` 的 host 部分（通过 `java.net.URL(url).host`）并展示
- [x] 3.3 `SourceBadge` 样式：胶囊形状、半透明白色背景、🔗 前缀图标、字号 ≤ 12sp，与 `TagPill` 视觉协调
- [x] 3.4 `SourceBadge` 仅展示，不可点击编辑

## 4. 新增 UI 组件 — CollapsibleUrlInput

- [x] 4.1 在 `CaptureComponents.kt` 新增 `@Composable fun CollapsibleUrlInput(url: String, onUrlChange: (String) -> Unit)` 组件
- [x] 4.2 实现折叠状态：显示「+ 添加来源链接」文字按钮
- [x] 4.3 实现展开状态：显示 `OutlinedTextField`，样式与「附加思考」输入框一致
- [x] 4.4 点击「+ 添加来源链接」切换为展开状态，再次点击不折叠（展开后保持）
- [x] 4.5 展开状态下 URL 值通过 `onUrlChange` 回调向上传递

## 5. 修改 CaptureDialogV2 — URL UI 集成

- [x] 5.1 在 `CaptureDialogV2` 中新增 `var currentUrl by remember { mutableStateOf(initialUrl) }` 状态
- [x] 5.2 在书摘卡片（`RichExcerptCard`）**下方**插入条件 UI：
  - `initialUrl.isNotEmpty()` → 渲染 `SourceBadge(url = initialUrl)`
  - `initialUrl.isEmpty()` → 渲染 `CollapsibleUrlInput(url = currentUrl, onUrlChange = { currentUrl = it })`
- [x] 5.3 修改保存调用：将 `url` 参数由 `initialUrl` 改为 `currentUrl`，确保用户手动输入的 URL 也能提交

## 6. 验证

- [ ] 6.1 手动测试：使用 Chrome 选中网页文字，通过系统分享打开 GleanRead，确认 URL 能从文末提取（若 Chrome 追加了 URL）
- [ ] 6.2 手动测试：使用 Chrome 分享整页到 GleanRead，确认 `SourceBadge` 正确显示域名
- [ ] 6.3 手动测试：使用微信公众号选中文字分享，确认 URL 提取或 `CollapsibleUrlInput` 降级展示
- [ ] 6.4 手动测试：不填写 URL 直接点击保存，确认摘录正常提交
- [ ] 6.5 确认现有的「附加思考」和「分类标记」功能不受影响

