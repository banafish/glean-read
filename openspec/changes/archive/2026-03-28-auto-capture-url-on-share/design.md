# 设计：auto-capture-url-on-share

## 背景

`FastCaptureActivity` 当前通过 `Intent.EXTRA_TEXT` 前缀匹配来提取 URL（仅处理 "http" 开头的整段文本）。
这在「选中文字后分享」（主要场景）中完全失效，因为此时 `EXTRA_TEXT` 只含摘录文字，URL 不在标准字段中，
或被追加在文字末尾，需要正则提取。

## 目标 / 非目标

**目标：**
- 在 Android 端实现纯本地的 URL 提取策略链，覆盖浏览器和微信公众号的常见分享格式
- 提取失败时提供优雅的降级 UI（可折叠的 URL 编辑框），不打断主流程
- 提取成功时在弹窗中展示只读域名徽章，提升信任感

**非目标：**
- 不引入任何网络请求（如抓取页面元数据）
- 不支持除浏览器和微信公众号以外的其他 App（本期范围）
- 不修改后端 API

## 技术方案

### 核心组件：`UrlExtractor`（新建工具类）

新建 `app/src/main/java/com/gleanread/android/UrlExtractor.kt`，
实现一个纯函数式的策略链，接受 `Intent` 返回 `String?`（提取到的 URL 或 null）。

```kotlin
object UrlExtractor {
    private val URL_REGEX = Regex("""https?://[^\s\u4e00-\u9fff"'<>]+""")

    fun extract(intent: Intent): String? {
        val text = intent.getStringExtra(Intent.EXTRA_TEXT) ?: ""
        val stream = intent.getParcelableExtra<android.net.Uri>(Intent.EXTRA_STREAM)

        // 策略1：EXTRA_TEXT 整体即为合法 URL
        if (text.isValidUrl()) return text.trim()

        // 策略2：EXTRA_STREAM 转字符串后为合法 URL
        stream?.toString()?.takeIf { it.isValidUrl() }?.let { return it }

        // 策略3：正则从 EXTRA_TEXT 末尾提取 URL（Chrome/微信末尾追加链接）
        URL_REGEX.findAll(text).lastOrNull()
            ?.value?.takeIf { it.isValidUrl() }?.let { url ->
                // 必须出现在文本末尾（末尾50字符内），排除正文中偶发的 URL
                if (text.trimEnd().endsWith(url.trimEnd('/'))) return url
            }

        // 策略4：正则从完整 EXTRA_TEXT 任意位置提取第一个合法 URL
        URL_REGEX.find(text)?.value?.takeIf { it.isValidUrl() }?.let { return it }

        // 策略5：所有策略均失败
        return null
    }

    private fun String.isValidUrl(): Boolean {
        return startsWith("http://") || startsWith("https://")
    }
}
```

**策略链设计说明：**

| 优先级 | 策略 | 覆盖场景 |
|---|---|---|
| 1 | `EXTRA_TEXT` 整体即 URL | Chrome 整页分享（标准路径） |
| 2 | `EXTRA_STREAM` Uri | 部分 App 通过 Uri 携带 URL |
| 3 | 正则提取末尾 URL | Chrome 选中文字分享（URL 追加末尾）；微信公众号常见格式 |
| 4 | 正则提取任意位置第一个 URL | Firefox 等其他浏览器的混排格式 |
| 5 | 返回 null → 降级 UI | 微信直接复制文字分享等无 URL 情况 |

**策略3的"末尾判断"逻辑：**
对于 "这段文字很精彩 https://mp.weixin.qq.com/s/xxx" 这类微信格式，
URL 必然在文本末尾。策略3通过 `endsWith` 验证，避免误把正文中的 URL 当作来源链接。

### `FastCaptureActivity.kt` 修改

**Intent 解析层（`onCreate`）：**
```kotlin
val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT) ?: ""
val sharedUrl = UrlExtractor.extract(intent) ?: ""
```
替换当前 L49-61 的手动逻辑，统一委托给 `UrlExtractor`。

**UI 层（`CaptureDialogV2`）：**
新增 `initialUrl: String` 参数的展示逻辑（已存在，只需补充 UI）：
- `initialUrl.isNotEmpty()` → 展示只读域名徽章（`SourceBadge` 组件）
- `initialUrl.isEmpty()` → 展示可折叠的 URL 输入框（`CollapsibleUrlInput` 组件）

### 新 UI 组件

#### `SourceBadge`（URL 提取成功）
```kotlin
// 在书摘卡片下方展示来源域名
// 样式：小型胶囊、半透明背景、🔗 图标 + 域名文字
// 仅展示 host 部分（如 "mp.weixin.qq.com"），不显示完整 URL
@Composable
fun SourceBadge(url: String) { ... }
```

#### `CollapsibleUrlInput`（URL 提取失败）
```kotlin
// 默认折叠，点击「添加来源链接 +」展开
// 展开后显示 OutlinedTextField，用户可粘贴 URL
// 折叠状态下不影响主流程（保存按钮正常可用）
@Composable
fun CollapsibleUrlInput(url: String, onUrlChange: (String) -> Unit) { ... }
```

### 文件变更清单

| 文件 | 操作 | 说明 |
|---|---|---|
| `UrlExtractor.kt` | **新建** | URL 提取策略链工具类 |
| `FastCaptureActivity.kt` | **修改** | Intent 解析逻辑 + UI 调用 |
| `CaptureComponents.kt` | **修改** | 新增 `SourceBadge`、`CollapsibleUrlInput` 组件 |

## 关键设计决策

**1. 为何单独提取 `UrlExtractor` 而不是内联在 Activity 中？**
便于单元测试——策略链逻辑是纯函数，不依赖 Android Framework，可以用 JUnit 直接测试各策略的命中情况。

**2. 为何策略3要做"末尾判断"而不是直接取最后一个 URL？**
避免正文内容含有 URL 却被误认为来源链接。例如："文章引用了 https://example.com 的数据" 中，
`example.com` 是被引用的资源，不是文章来源。末尾判断（`endsWith`）过滤掉这类情况。

**3. `CollapsibleUrlInput` 为何默认折叠？**
URL 字段在业务上是可选的，强制显示输入框会打断「极速摘录」的心流。
用户若不关心来源链接，可以直接保存；若需要，自行展开填写。

## 风险 / 权衡

| 风险 | 概率 | 应对 |
|---|---|---|
| 微信公众号 Intent 格式随版本变化 | 中 | 策略3/4 使用通用正则，对格式变化有一定容错 |
| 正则误提取正文内 URL 为来源 URL | 低-中 | 策略3 的末尾验证减少误判；策略4 作为最后手段 |
| 不同 Android 版本 `EXTRA_STREAM` 类型差异 | 低 | 使用 `try-catch` 包装 `getParcelableExtra` 调用 |
