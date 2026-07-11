package com.gleanread.android.platform.page_context

import android.view.accessibility.AccessibilityEvent

internal object PageContextAccessibilityPolicy {
    const val NotificationTimeoutMillis = 350L

    private const val InterestingContentChangeTypes =
        AccessibilityEvent.CONTENT_CHANGE_TYPE_TEXT or
            AccessibilityEvent.CONTENT_CHANGE_TYPE_SUBTREE or
            AccessibilityEvent.CONTENT_CHANGE_TYPE_PANE_TITLE

    private const val DuplicateSnapshotRefreshWindowMillis = 5_000L

    fun shouldProcessEvent(
        eventType: Int,
        contentChangeTypes: Int,
    ): Boolean {
        return when (eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> true
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                contentChangeTypes == 0 ||
                    contentChangeTypes and InterestingContentChangeTypes != 0
            }

            else -> false
        }
    }

    /**
     * 微信「复制成功」toast 才路由给摘录处理器（点击事件不带文本，无法识别复制动作）。
     * 服务入口据此对 TYPE_NOTIFICATION_STATE_CHANGED 做 O(1) 分流。
     */
    fun isWeChatCopyToastEvent(
        eventType: Int,
        packageName: String,
        className: String,
    ): Boolean {
        return eventType == AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED &&
            packageName == PageContextSupport.WeChatPackage &&
            className.contains("Toast", ignoreCase = true)
    }

    fun shouldSkipStoreWrite(
        previous: PageContextSnapshot?,
        current: PageContextSnapshot,
    ): Boolean {
        if (previous == null) return false

        val isSamePayload = previous.sourcePackage == current.sourcePackage &&
            previous.sourceTitle == current.sourceTitle &&
            previous.sourceUrl == current.sourceUrl
        if (!isSamePayload) return false

        return current.capturedAt - previous.capturedAt < DuplicateSnapshotRefreshWindowMillis
    }
}
