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
    private var lastHandledClickAt = 0L

    /** 服务转发的微信 TYPE_VIEW_CLICKED 事件入口 */
    fun onClickEvent(
        event: AccessibilityEvent,
        rootProvider: () -> AccessibilityNodeInfo?,
    ) {
        handleCopyClick(
            candidateTexts = collectCandidateTexts(event),
            windowId = event.windowId,
            articleScan = { detector.isArticlePage(event.windowId, rootProvider) },
        )
    }

    /** 服务转发的微信窗口事件入口：维护文章页缓存 + 提取标题写快照 */
    fun onWindowEvent(
        event: AccessibilityEvent,
        roots: List<AccessibilityNodeInfo>,
        screenHeight: Int,
    ) {
        val isWindowStateChange = event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
        if (isWindowStateChange) {
            // 窗口切换时失效文章页判定缓存，避免跨页面误用
            detector.invalidate()
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

    /** 点击处理主干；事件字段提取与其解耦，便于纯逻辑单测 */
    internal fun handleCopyClick(
        candidateTexts: List<String>,
        windowId: Int,
        articleScan: () -> Boolean,
    ) {
        val now = clock()
        // 微信一次点击可能派发多条事件，只处理第一条
        if (now - lastHandledClickAt < WeChatCaptureContract.ClickDebounceMillis) return

        val kind = WeChatClickClassifier.classify(candidateTexts)
        if (kind == WeChatCopyClickKind.NONE) return

        // 仅公众号文章页触发（聊天等原生页面复制不打扰），扫描结果按窗口缓存
        if (!detector.isArticlePageCached(windowId, articleScan)) return

        lastHandledClickAt = now
        when (kind) {
            WeChatCopyClickKind.COPY_TEXT -> signalStore.markCopyTextClicked(now)
            WeChatCopyClickKind.COPY_LINK -> signalStore.markCopyLinkClicked(now)
            WeChatCopyClickKind.NONE -> Unit
        }

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

    private fun collectCandidateTexts(event: AccessibilityEvent): List<String> {
        return buildList {
            event.text.orEmpty().forEach { text ->
                text?.toString()?.let(::add)
            }
            event.source?.let { source ->
                source.text?.toString()?.let(::add)
                source.contentDescription?.toString()?.let(::add)
            }
        }
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
    }
}
