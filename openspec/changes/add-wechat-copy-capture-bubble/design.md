# 设计：微信公众号「复制即摘」气泡

## 背景约束

- 微信长按菜单自绘，不接系统分享/文本处理；公众号 URL 不渲染在界面上。
- Android 10+ 只有获得窗口焦点的应用才能读剪贴板，无障碍服务进程读不到；Android 12+ 首次读取会出现一次系统"已粘贴"提示（与"点气泡=摘录刚复制内容"的用户心智一致，可接受）。
- 剪贴板只有一格：若「复制链接」只记标记不弹气泡，用户随后复制正文会覆盖链接，URL 永远读不到。因此「复制链接」也弹同一个气泡，点一下即完成 URL 采集与回写。

## 架构

```
无障碍服务（主线程）
  PageContextAccessibilityService
    ├─ TYPE_NOTIFICATION_STATE_CHANGED → 仅微信 Toast → WeChatCaptureCoordinator.onCopyToastEvent
    ├─ 窗口事件(微信包)   → WeChatCaptureCoordinator.onWindowEvent（标题提取 + 窗口类名记录）
    ├─ 窗口事件(浏览器)   → 原有管线（不变）
    └─ serviceScope 收集「微信摘录气泡」开关 Flow
  platform/page_context/wechat/
    ├─ WeChatCaptureCoordinator    编排：去抖→toast 分类→文章页判定→信号→气泡
    ├─ WeChatCopyToastClassifier   纯逻辑：复制成功 toast 识别（简中/繁中/英文，含失败词排除）
    ├─ WeChatArticlePageDetector   BFS 找 WebView/XWeb 容器 + windowId 缓存 30s
    ├─ WeChatArticleTitleExtractor 纯逻辑：标题候选打分（网页容器内 +120、相对屏高）
    ├─ WeChatBubbleController      WindowManager + TYPE_ACCESSIBILITY_OVERLAY
    ├─ BubbleOverlayHost           ComposeView + 手工 LifecycleOwner（约 50 行）
    ├─ WeChatClipboardResolver     纯逻辑：剪贴板新鲜度校验 + URL/正文判别
    ├─ WeChatCaptureSignalStore    SharedPreferences：复制成功时间戳
    └─ WeChatCaptureContract       action 与阈值常量
```

## 关键决策

1. **触发器 = 微信「复制成功」toast**（`TYPE_NOTIFICATION_STATE_CHANGED` + toast 文案包含匹配）。真机验证推翻了最初的按钮点击方案：自绘菜单「复制」的 `TYPE_VIEW_CLICKED` 事件 text/desc/源节点全为空，按钮级识别结构性不可行；toast「内容已复制」则稳定出现。toast 无法区分复制正文/复制链接，也无需区分——下游只用时间戳校验新鲜度，正文/链接由剪贴板内容判别。不监听 XWeb 选中事件（暴露不稳定、跨段落文本取不全）。
1b. **文章页判定双信号**：主判定为活动窗口根节点 BFS 找网页容器（toast 无窗口归属，用 `rootInActiveWindow`）；兜底为最近一次微信窗口切换的类名含 WebView（真机证据 `TmplWebViewMMUI`），覆盖部分内核不暴露网页节点的机型。
2. **正文/链接统一由获焦的 `FastCaptureActivity` 读剪贴板**（两阶段 seed：onCreate 缓存补齐 + 首次 `onWindowFocusChanged` 剪贴板填充），服务进程不做任何剪贴板 hack。
3. **气泡用 ComposeView + 手工 LifecycleOwner**：项目无 View 版 Material 依赖，纯 View 方案会脱离 M3 动态色/深色模式；每次 show 新建、hide 即销毁，规避服务进程长生命周期 Compose 悬挂，风险收敛在单一 Host 文件。
4. **TTL 按宿主区分**（浏览器 60s / 微信 600s），合并窗口同步区分（15s / 600s）；仍维持单条快照，微信 URL 回写通过 `mergeWeChatUrl` 保留已有标题。
5. **剪贴板新鲜度**：`ClipDescription.getTimestamp()`（minSdk 26 可用）与服务记录的复制成功信号比对（容差 15s）；部分 ROM 时间戳恒 0 时退化为 180s 时间窗。
6. **微信标题提取独立成打分器**：网页容器内节点强加权（公众号标题只会渲染在网页里）、相对屏高替代绝对像素、压低原生顶栏公众号名；通用提取器删除微信分支后只服务浏览器。

## 降级矩阵

| 失效点 | 行为 |
|---|---|
| 微信改版 toast 文案 | 气泡不出现；词表集中在 Classifier 一处可快速追加 |
| XWeb 不暴露网页节点 | 窗口类名兜底判定接管；两者都失败则不弹气泡（宁缺勿滥），标题留空可手填 |
| MIUI 禁后台弹界面 | 点气泡无反应；设置文案引导授权 |
| 剪贴板空/陈旧 | 弹窗保持缓存标题/URL + 空正文，保存不阻塞 |
| 快照被浏览器覆盖 | 微信上下文丢失，正文仍可摘、URL 手填（与单条快照 spec 一致）|
