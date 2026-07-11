package com.gleanread.android.platform.page_context.wechat

import android.view.accessibility.AccessibilityNodeInfo

/**
 * 判定当前微信窗口是否为公众号文章页（节点树中存在网页容器）。
 * 结果按 windowId 缓存一段时间，避免每次复制点击都全树遍历；
 * 窗口切换（TYPE_WINDOW_STATE_CHANGED）时由调用方 invalidate。
 */
class WeChatArticlePageDetector(
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private var cachedWindowId = Int.MIN_VALUE
    private var cachedResult = false
    private var cachedAt = 0L

    fun isArticlePage(
        windowId: Int,
        rootProvider: () -> AccessibilityNodeInfo?,
    ): Boolean {
        return isArticlePageCached(windowId) { scanForWebContainer(rootProvider()) }
    }

    /** 缓存判定与扫描解耦，便于对缓存行为做纯 JVM 单测 */
    internal fun isArticlePageCached(
        windowId: Int,
        scan: () -> Boolean,
    ): Boolean {
        val now = clock()
        val cacheUsable = windowId == cachedWindowId &&
            now - cachedAt <= WeChatCaptureContract.ArticleDetectCacheMillis
        if (cacheUsable) {
            return cachedResult
        }

        val result = scan()
        cachedWindowId = windowId
        cachedResult = result
        cachedAt = now
        return result
    }

    fun invalidate() {
        cachedWindowId = Int.MIN_VALUE
        cachedResult = false
        cachedAt = 0L
    }

    private fun scanForWebContainer(root: AccessibilityNodeInfo?): Boolean {
        val safeRoot = root ?: return false
        val pending = ArrayDeque<NodeVisit>()
        pending.addLast(NodeVisit(node = safeRoot, depth = 0))
        var visitedCount = 0

        while (pending.isNotEmpty() && visitedCount < MAX_VISITED_NODES) {
            val current = pending.removeFirst()
            visitedCount += 1

            if (isWebContainerClassName(current.node.className?.toString().orEmpty())) {
                return true
            }
            if (current.depth >= MAX_SCAN_DEPTH) continue

            for (index in 0 until current.node.childCount) {
                current.node.getChild(index)?.let { child ->
                    pending.addLast(NodeVisit(node = child, depth = current.depth + 1))
                }
            }
        }
        return false
    }

    private data class NodeVisit(
        val node: AccessibilityNodeInfo,
        val depth: Int,
    )

    companion object {
        private const val MAX_SCAN_DEPTH = 12
        private const val MAX_VISITED_NODES = 120

        /** 覆盖系统 WebView、微信 XWeb 与旧版 XWalk 内核容器 */
        fun isWebContainerClassName(className: String): Boolean {
            val normalized = className.lowercase()
            return normalized.contains("webview") ||
                normalized.contains("xweb") ||
                normalized.contains("xwalk")
        }
    }
}
