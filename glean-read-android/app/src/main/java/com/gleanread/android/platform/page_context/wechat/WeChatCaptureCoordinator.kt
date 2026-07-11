package com.gleanread.android.platform.page_context.wechat

import android.graphics.Rect
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.gleanread.android.platform.page_context.PageContextAccessibilityPolicy
import com.gleanread.android.platform.page_context.PageContextSnapshot
import com.gleanread.android.platform.page_context.PageContextStore
import com.gleanread.android.platform.page_context.PageContextSupport

/**
 * 微信「复制即摘」协调器：
 * - 点击链路：去抖 → 按钮分类 → 公众号文章页判定 → 记录复制信号 → 弹出气泡；
 * - 标题链路：微信窗口事件 → 收集节点（标记是否在网页容器内）→ 打分提取标题 → 写入快照。
 * Android 依赖集中在这里，规则判断全部下沉到可单测的纯逻辑类。
 */
class WeChatCaptureCoordinator(
    private val pageContextStore: PageContextStore,
    private val signalStore: WeChatCaptureSignalStore,
    private val onLaunchCapture: () -> Unit,
    private val clock: () -> Long = System::currentTimeMillis,
    bubbleFactory: (onTapped: () -> Unit) -> WeChatCaptureBubble,
) {
    private val bubble: WeChatCaptureBubble = bubbleFactory { onBubbleTapped() }
    private val detector = WeChatArticlePageDetector(clock)
    private var isBubbleEnabled = true
    private var lastHandledCopyAt = 0L

    // 最近一次微信窗口切换的窗口类名；公众号文章页类名含 WebView（真机证据：TmplWebViewMMUI）
    private var lastWindowClassName = ""

    /** 服务转发的微信「复制成功」toast 事件入口 */
    fun onCopyToastEvent(
        event: AccessibilityEvent,
        rootProvider: () -> AccessibilityNodeInfo?,
    ) {
        val toastTexts = buildList {
            event.text.orEmpty().forEach { text ->
                text?.toString()?.let(::add)
            }
        }
        handleCopyToast(toastTexts) {
            // toast 事件不属于任何窗口（windowId=-1），改用当前活动窗口根节点做文章页判定
            val root = rootProvider()
            root != null && detector.isArticlePage(root.windowId) { root }
        }
    }

    /** 服务转发的微信窗口事件入口：维护文章页缓存 + 提取标题写快照 */
    fun onWindowEvent(
        event: AccessibilityEvent,
        roots: List<AccessibilityNodeInfo>,
        screenHeight: Int,
    ) {
        val isWindowStateChange = event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
        if (isWindowStateChange) {
            noteWindowStateChanged(event.className?.toString().orEmpty())
        }

        val eventTexts = buildList {
            event.contentDescription?.toString()?.let(::add)
            event.text.orEmpty().forEach { text ->
                text?.toString()?.let(::add)
            }
        }
        handleWindowTitle(
            eventTexts = eventTexts,
            candidates = collectTitleCandidates(roots),
            isWindowStateChange = isWindowStateChange,
            screenHeight = screenHeight,
        )
    }

    fun onBubbleEnabledChanged(enabled: Boolean) {
        isBubbleEnabled = enabled
        if (!enabled) {
            bubble.hide()
        }
    }

    fun destroy() {
        bubble.destroy()
    }

    /**
     * 窗口切换：失效文章页判定缓存，并记录窗口类名供 toast 触发时兜底判定。
     * 仅 Activity 级窗口（微信 Activity 均以 UI 结尾，如 LauncherUI/TmplWebViewMMUI）才覆盖记录：
     * 真机证据表明文章页内弹出分享菜单会派发 dialog 窗口事件（如 ui.widget.dialog.a4），
     * 而菜单关闭不派发任何事件，若被其覆盖，兜底判定将永久失效直至下次 Activity 切换。
     */
    internal fun noteWindowStateChanged(className: String) {
        detector.invalidate()
        if (isActivityWindowClassName(className)) {
            lastWindowClassName = className
        }
    }

    /** 复制成功 toast 处理主干；事件字段提取与其解耦，便于纯逻辑单测 */
    internal fun handleCopyToast(
        toastTexts: List<String>,
        articleScan: () -> Boolean,
    ) {
        val now = clock()
        // 微信一次复制可能派发多条 toast，只处理第一条
        if (now - lastHandledCopyAt < WeChatCaptureContract.CopyToastDebounceMillis) {
            return
        }

        if (!WeChatCopyToastClassifier.isCopySuccess(toastTexts)) return

        // 仅公众号文章页触发（聊天等原生页面复制不打扰）：
        // 主判定为节点扫描找网页容器，兜底为活动窗口类名含 WebView（部分内核不暴露网页节点）
        val isArticle = articleScan() ||
            WeChatArticlePageDetector.isWebContainerClassName(lastWindowClassName)
        if (!isArticle) return

        lastHandledCopyAt = now
        signalStore.markCopyObserved(now)

        // 开关关闭时仍记录信号（供弹窗剪贴板校验），但不打扰用户
        if (isBubbleEnabled) {
            bubble.show()
        }
    }

    /**
     * 标题写入策略：
     * - 无快照：新建仅含标题的微信快照；
     * - 窗口切换且标题变化：视为打开了新文章，重置 URL 防止上一篇的链接串页；
     * - 其余情况：同页更新，保留 URL，标题择优（更长者胜，顶栏截断标题不会覆盖完整 h1）。
     */
    internal fun handleWindowTitle(
        eventTexts: List<String>,
        candidates: List<WeChatArticleTitleExtractor.TitleCandidate>,
        isWindowStateChange: Boolean,
        screenHeight: Int,
    ) {
        val title = WeChatArticleTitleExtractor.extract(
            eventTexts = eventTexts,
            candidates = candidates,
            screenHeight = screenHeight,
        )
        if (title.isBlank()) return

        val now = clock()
        val previous = pageContextStore.readRecentSnapshot(
            expectedSourcePackage = PageContextSupport.WeChatPackage,
            now = now,
        )
        val next = when {
            previous == null -> titleOnlySnapshot(title, now)
            isWindowStateChange && title != previous.sourceTitle -> titleOnlySnapshot(title, now)
            else -> {
                val mergedTitle = if (title.length > previous.sourceTitle.length) title else previous.sourceTitle
                previous.copy(
                    sourceTitle = mergedTitle,
                    capturedAt = now,
                    confidence = if (previous.sourceUrl.isNotBlank()) 0.95f else 0.7f,
                )
            }
        }
        if (PageContextAccessibilityPolicy.shouldSkipStoreWrite(previous, next)) return
        pageContextStore.save(next)
    }

    private fun titleOnlySnapshot(title: String, now: Long): PageContextSnapshot {
        return PageContextSnapshot(
            sourcePackage = PageContextSupport.WeChatPackage,
            sourceTitle = title,
            sourceUrl = "",
            capturedAt = now,
            captureSource = PageContextSupport.AccessibilityCaptureSource,
            confidence = 0.7f,
        )
    }

    private fun onBubbleTapped() {
        bubble.hide()
        onLaunchCapture()
    }

    /** BFS 收集标题候选节点，并沿途标记节点是否位于网页容器（WebView/XWeb）子树内 */
    private fun collectTitleCandidates(
        roots: List<AccessibilityNodeInfo>,
    ): List<WeChatArticleTitleExtractor.TitleCandidate> {
        val candidates = mutableListOf<WeChatArticleTitleExtractor.TitleCandidate>()
        roots.forEach { root -> collectTitleCandidatesFrom(root, candidates) }
        return candidates
    }

    private fun collectTitleCandidatesFrom(
        root: AccessibilityNodeInfo,
        destination: MutableList<WeChatArticleTitleExtractor.TitleCandidate>,
    ) {
        val pending = ArrayDeque<TitleNodeVisit>()
        pending.addLast(TitleNodeVisit(node = root, depth = 0, inWebContainer = false))
        var visitedCount = 0

        while (
            pending.isNotEmpty() &&
            visitedCount < MAX_VISITED_NODES &&
            destination.size < MAX_COLLECTED_NODES
        ) {
            val current = pending.removeFirst()
            visitedCount += 1

            val className = current.node.className?.toString().orEmpty()
            val inWebContainer = current.inWebContainer ||
                WeChatArticlePageDetector.isWebContainerClassName(className)

            val text = current.node.text?.toString().orEmpty().trim()
            if (text.isNotBlank()) {
                val bounds = Rect()
                current.node.getBoundsInScreen(bounds)
                destination += WeChatArticleTitleExtractor.TitleCandidate(
                    text = text,
                    viewId = current.node.viewIdResourceName.orEmpty(),
                    className = className,
                    boundsTop = bounds.top,
                    inWebContainer = inWebContainer,
                )
            }

            if (current.depth >= MAX_SCAN_DEPTH) continue

            for (index in 0 until current.node.childCount) {
                current.node.getChild(index)?.let { child ->
                    pending.addLast(
                        TitleNodeVisit(
                            node = child,
                            depth = current.depth + 1,
                            inWebContainer = inWebContainer,
                        ),
                    )
                }
            }
        }
    }

    private data class TitleNodeVisit(
        val node: AccessibilityNodeInfo,
        val depth: Int,
        val inWebContainer: Boolean,
    )

    private companion object {
        const val MAX_SCAN_DEPTH = 24
        const val MAX_VISITED_NODES = 400
        const val MAX_COLLECTED_NODES = 180

        /** 微信 Activity 命名惯例：类名以 UI 结尾；dialog/popup 等浮层为混淆短名，不满足该惯例 */
        fun isActivityWindowClassName(className: String): Boolean {
            return className.endsWith("UI")
        }
    }
}
