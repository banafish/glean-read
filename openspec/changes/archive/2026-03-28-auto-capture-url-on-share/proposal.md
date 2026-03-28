# 提案：auto-capture-url-on-share

## 问题陈述

用户通过手机浏览器（Chrome、Firefox 等）或微信公众号内置阅读器分享文章时，
GleanRead 的极速摘录弹窗（FastCaptureActivity）无法可靠地提取被分享文章的原始 URL。

### 核心场景：选中文字后分享（主要路径）

**绝大多数使用场景**是：用户阅读文章时，长按选中某一段精彩内容，通过系统「分享」菜单
发送到 GleanRead。此时系统 Intent 中：

- `EXTRA_TEXT`：**仅含被选中的摘录文字**，没有 URL
- 页面 URL **不出现在任何标准 Extra 字段中**（这是选中文字分享 vs 整页分享的根本区别）
- 部分浏览器（如 Chrome）会将页面 URL 追加在所选文本之后，但格式不统一
- 微信公众号：所选文本后可能追加文章 URL，也可能完全不追加

### 次要场景：整页分享（通过浏览器分享按钮）

用户点击浏览器工具栏的「分享」按钮分享整个页面。此时：

- Chrome 会同时填充 `EXTRA_SUBJECT`（标题）和 `EXTRA_TEXT`（URL），相对规范
- 微信公众号：`EXTRA_TEXT` 为「标题 + 消息」，真实 URL 藏于尾部或其他 Extra 字段

### 现有实现的缺陷

现有实现（`FastCaptureActivity.kt` L56-58）仅对 `EXTRA_TEXT` 做整体前缀匹配：
若共享文本整体以 `http://` 或 `https://` 开头则视为 URL，否则 URL 字段留空。

这导致：在「选中文字分享」这一**主要场景**中，URL 字段始终为空，用户必须手动粘贴链接，严重破坏「极速心流」体验。

## 变更内容

在 Android 端（`FastCaptureActivity.kt`）引入一个轻量的 **URL 提取策略链**
（Chain-of-Responsibility），按优先级依次尝试从 `Intent` 中提取 URL，
无需任何网络调用或后端参与。策略链同时覆盖「整页分享」与「选中文字分享」两种场景：

**标准字段提取（整页分享主路径）**
1. `EXTRA_TEXT` 整体即为合法 URL → 直接使用
2. `EXTRA_STREAM`（Uri 类型）转字符串后为合法 URL → 使用

**文本内嵌 URL 提取（选中文字分享 + 微信公众号）**
3. 用正则从 `EXTRA_TEXT` **末尾**提取 URL（Chrome 选中文字分享、微信公众号的常见格式：文字末尾追加原文链接）
4. 用正则从完整 `EXTRA_TEXT` 任意位置提取第一个合法 URL

**降级处理（URL 完全缺失时）**
5. 所有策略均失败 → URL 字段留空，弹窗内自动显示**可编辑的 URL 输入框**，允许用户手动粘贴原文链接后保存

> **关于「选中文字分享」URL 缺失问题的边界说明**：
> 若第三方 App 完全未在 Intent 中携带任何 URL（如微信直接复制文字分享），
> 则技术上无法在纯客户端侧自动恢复 URL，此时必然走降级路径（手动输入）。
> 这是 Android 系统分享 Intent 的固有限制，不在本变更解决范围内。

同时对 UI 层做配套调整：
- **URL 提取成功**：弹窗以只读域名徽章形式展示来源，给用户确认感
- **URL 提取失败**：展示折叠的可编辑 URL 输入框（默认折叠、可展开，不强制打断流程）

## 功能 (Capabilities)

### 新功能

- `url-auto-extraction`：Android 端 URL 提取策略链，纯本地逻辑，支持浏览器与微信公众号
- `url-display-in-capture-ui`：快速摘录弹窗新增"来源 URL"展示区域（成功时只读徽章 / 失败时可编辑输入框）

### 修改功能

- `fast-capture-widget`：`FastCaptureActivity.onCreate` 中的 Intent 解析逻辑升级，
  UI 层 `CaptureDialogV2` 增加 URL 展示/编辑区域

## 影响

- **代码变更范围**：`FastCaptureActivity.kt`（Intent 解析 + UI 组合逻辑）；
  可选新增 `UrlExtractor.kt` 工具类
- **依赖**：无新增第三方依赖，仅使用标准 Android SDK (`Intent`, `Uri`) 与 Kotlin 标准库正则
- **向后兼容**：完全后向兼容——现有的 URL 提取路径（EXTRA_TEXT 前缀匹配）被涵盖在策略链第一步中
- **后端**：无需变更

## 成功标准

**整页分享场景**
- 使用 Chrome「分享整页」功能分享到 GleanRead，弹窗能自动填充正确 URL，命中率 ≥ 95%
- 使用微信公众号内「分享文章链接」到 GleanRead，能自动填充正确 URL，命中率 ≥ 90%

**选中文字分享场景**
- 使用 Chrome「选中文字后分享」且 Chrome 在文本末尾追加了 URL 时，弹窗能正确提取 URL
- 微信公众号选中文字分享时，若 Intent 中含 URL 则能正确提取；若不含则优雅降级，显示输入框

**降级体验**
- URL 自动提取失败时，弹窗显示可编辑 URL 输入框（默认折叠，可展开），用户手动填写后提交正常保存
- URL 输入框为**可选项**，不填写 URL 也能正常提交摘录

**通用**
- 无新增第三方依赖
- 现有摘录（文本 + 标签 + 想法）功能不受影响
