# browser-scripts

浏览器 userscript 集合。当前包含 X浏览器划词摘录脚本。

## glean-web-capture.user.js — 划词摘录

选中网页文本后浮出「摘」圆钮，点击通过 deep link 直接拉起 GleanRead 快摘弹窗。
URL（`location.href`）、标题（`document.title`）、选中文本三项数据装在启动
intent 里原子送达，不依赖无障碍启发式抓取，也不怕 ROM 冻结后台进程。

### X浏览器导入步骤

1. 打开 X浏览器 → 菜单 → **工具箱 → 脚本**（不同版本入口可能叫「油猴脚本 / 拓展脚本」）。
2. 选择 **新建脚本 / 导入脚本**，把 `glean-web-capture.user.js` 全文粘贴进去（或直接导入文件）。
3. 保存并启用，确认脚本作用域为全部网站（脚本头 `@match *://*/*`）。
4. 打开任意文章页，长按选中一段文字 → 松手后按钮出现在选区下方 → 点击应直接弹出摘录弹窗。

### intent / scheme 两种触达形式

脚本顶部 `CONFIG.useIntentUri` 控制 deep link 形式，真机验证后定稿：

| 形式 | 值 | 说明 |
|---|---|---|
| intent URI（默认） | `useIntentUri: true` | Chromium 标准 `intent://...#Intent;scheme=gleanread;package=com.gleanread.android;end`，带包名防劫持，Chromium 系内核免确认框 |
| scheme 直跳 | `useIntentUri: false` | `location.href = "gleanread://capture?..."`，WebView 壳浏览器不识别 `intent:` 语法时的降级形式 |

验证顺序：先用默认 intent 形式；若点击按钮无反应或报「网址无效」，改为
`useIntentUri: false` 再试。两种形式在 X浏览器上都会弹「是否允许打开外部应用」
确认条，见下节说明。

### 「是否允许打开外部应用」确认条（已知限制）

X浏览器对**网页发起**的外部应用跳转统一弹底部确认条且**不记住选择**，`intent://`
与 `gleanread://`、主 frame 与隐藏 iframe、脚本特权 API `GM_openInTab` 全部命中
（真机已实测），官方文档也没有全局白名单开关——即无法从脚本侧绕过。

- **正常用法**：点「摘」后在底部确认条上点一下「允许」即可完成摘录，功能与数据完整性不受影响。
- 可顺手翻一下浏览器的站点设置（地址栏左侧 favicon 图标，或 工具箱 → 网站设置 →「更多…」），
  个别版本可能提供外部应用类开关；没有就接受确认条。
- **应急替代**：X浏览器的文字选择工具栏原生支持系统文本处理入口
  （长按选中 → 菜单里直接选 GleanRead），无确认条，但该入口拿不到页面
  URL/标题，仅正文可用，不作为主路径。

### 可调参数（脚本顶部 `CONFIG`）

- `buttonOffsetY`：按钮与选区底部的距离（默认 24px）。系统文本选择菜单遮挡按钮时调大。
- `maxTextLength`：选中文本截断长度（默认 6000 字符，URL 编码后约 18KB）。
- `debounceMillis`：选区变化防抖间隔（默认 250ms）。
- `debug`：控制台诊断日志开关（当前默认 `true`，真机验证定稿后改 `false`）。

### 按钮不出现时的排查步骤

先在**普通 https 网页**（如任意新闻文章页）验证，排除本地文件/内部页面因素，再按顺序检查：

1. **确认脚本被注入**：打开 X浏览器开发者控制台 → 刷新页面 → 找
   `[GleanRead] script v1.0.4 installed @ ...` 日志（版本号与导入的脚本一致）；
   或在控制台输入 `window.__gleanWebCaptureInstalled`，返回 `true` 即已注入。
   - 没有日志且返回 `undefined` → 脚本未注入：检查脚本管理器里是否启用、
     作用域是否为全部网站；旧版 X浏览器对 `@match` 支持不完整，脚本已同时
     声明 `@include *` 兜底，请确认导入的是最新版脚本。
   - 地址栏是 `file://` 本地文件或浏览器内部阅读页（阅读模式、小说模式）时，
     部分浏览器不注入 userscript，属预期限制，请用普通网页验证功能本身。
2. **确认选区事件通道**：长按选中一段正文，控制台应出现
   `[GleanRead] first selection-related event received: ...`，随后出现
   `[GleanRead] selection: N chars, show button below rect(...)`。
   - 有 installed 无任何 selection 日志 → 内核未派发选区相关事件
     （脚本已内置 `touchend`/`mouseup` 兜底，仍无日志请反馈内核信息）。
   - 有 `hide: selection rect is all-zero` → 内核不暴露选区几何信息，无法定位按钮。
   - 一直没有 selection 日志但页面确有选中 → 正文可能在 iframe 内（MVP 不支持），
     控制台输入 `document.querySelector('iframe')` 验证。
3. **确认跳转形式**：点击按钮后控制台出现 `[GleanRead] navigate: intent://...`
   但无反应或报「网址无效」→ 把 `CONFIG.useIntentUri` 改为 `false` 切 scheme 直跳。

### 已知限制

- 不处理 iframe 内选区（脚本仅在顶层 frame 运行）。
- 滚动页面时按钮隐藏，重新调整选区后再出现。
- 输入框（input / textarea / contenteditable）内选中文本不触发按钮。
