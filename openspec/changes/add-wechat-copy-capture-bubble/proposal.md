## 为什么

现有"无障碍辅助快摘"在微信公众号文章页完全失效，且原因是结构性的，无法通过调参修复：

1. **触发通道不存在**：摘录弹窗只能被系统分享（`ACTION_SEND`）或系统文本处理（`ACTION_PROCESS_TEXT`）拉起，而微信公众号长按菜单是自绘的（复制/搜一搜等），不接系统通道，弹窗在微信里没有任何入口。
2. **URL 抓不到**：公众号文章链接从不渲染在界面上，无障碍节点树遍历永远拿不到精确的 `mp.weixin.qq.com/s/xxx`，现有 `isHighConfidenceWeChatUrlNode` 的地址栏节点假设不成立。
3. **标题脆弱**：标题在 XWeb WebView 网页内容里，现有 `boundsTop 0..520px` 与 viewId 启发式在微信里基本失效。

因此需要重写微信路径为「复制即摘」模式：以微信自带「复制/复制链接」按钮点击为触发信号，用无障碍悬浮气泡承接，剪贴板承载正文与链接。

## 变更内容

- 无障碍服务新增 `TYPE_VIEW_CLICKED` 事件监听（仅路由给微信处理器），识别公众号文章页内「复制」「复制链接」按钮点击。
- 新增微信摘录悬浮气泡（`TYPE_ACCESSIBILITY_OVERLAY`）：复制类点击后在屏幕边缘弹出，8 秒自动消失，点击后以自定义 action 拉起 `FastCaptureActivity`。
- 触发范围限定为公众号文章页（当前窗口含 WebView/XWeb 网页容器节点），聊天等原生页面复制不触发。
- `FastCaptureActivity` 新增两阶段种子：onCreate 用缓存补标题/URL，首次获得窗口焦点后读取剪贴板（Android 10+ 限制），非链接文本填正文、`mp.weixin.qq.com` 链接填 URL 并回写页面上下文缓存。
- 页面上下文缓存 TTL 按宿主区分：浏览器维持 60 秒，微信延长至 10 分钟（覆盖"复制链接后继续阅读几分钟再复制正文"的真实节奏）。
- 微信标题提取重写为独立打分器（网页容器内节点优先、相对屏高位置替代绝对像素），并删除通用提取器中的旧微信分支。
- 设置页无障碍卡片下新增「微信摘录气泡」开关（DataStore 持久化，默认开启），服务运行时响应开关变化。

## 功能 (Capabilities)

### 新增功能
- `android-wechat-capture-bubble`: 微信公众号文章页的复制触发摘录气泡能力，覆盖触发条件、触发范围、自动消失、开关控制与拉起快摘的完整契约。

### 修改功能
- `android-page-context-cache`: TTL 按来源宿主区分；新增微信文章 URL 回写与复制信号记录约束。
- `fast-capture-widget`: 新增微信气泡入口（`ACTION_WECHAT_CAPTURE`）与两阶段剪贴板种子填充；入口分类从三类扩展为四类。

## 影响

- 无障碍服务配置 XML（新增 `typeViewClicked`）、事件路由与微信协调器。
- `PageContextStore` / `PageContextSupport`（按包名 TTL、URL 回写）、`CaptureSeedResolver`（微信 action 分支）。
- `FastCaptureActivity`（两阶段 seed 与剪贴板读取）、`FastCaptureScreen`（currentUrl 重置键）。
- 设置页（新开关）、`AppContainer`（新偏好仓库）、strings.xml。
- 删除 `PageContextNodeExtractor` 中微信专属分支；浏览器路径行为保持不变。
