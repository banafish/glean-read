package com.gleanread.android.platform.page_context.wechat

import android.content.Context

/** 微信复制类点击信号（不含剪贴板内容本身），供剪贴板新鲜度校验使用 */
data class WeChatCaptureSignals(
    val lastCopyTextAt: Long,
    val lastCopyLinkAt: Long,
) {
    val latestCopyAt: Long
        get() = maxOf(lastCopyTextAt, lastCopyLinkAt)
}

/**
 * 记录无障碍服务观察到的微信「复制 / 复制链接」点击时间戳。
 * 服务进程写入，摘录弹窗进程内读取，用 SharedPreferences 直读保证跨组件可见。
 */
class WeChatCaptureSignalStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )

    fun markCopyTextClicked(at: Long) {
        preferences.edit().putLong(KEY_LAST_COPY_TEXT_AT, at).apply()
    }

    fun markCopyLinkClicked(at: Long) {
        preferences.edit().putLong(KEY_LAST_COPY_LINK_AT, at).apply()
    }

    fun read(): WeChatCaptureSignals {
        return WeChatCaptureSignals(
            lastCopyTextAt = preferences.getLong(KEY_LAST_COPY_TEXT_AT, 0L),
            lastCopyLinkAt = preferences.getLong(KEY_LAST_COPY_LINK_AT, 0L),
        )
    }

    fun clear() {
        preferences.edit().clear().apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "wechat_capture_signals"
        const val KEY_LAST_COPY_TEXT_AT = "last_copy_text_at"
        const val KEY_LAST_COPY_LINK_AT = "last_copy_link_at"
    }
}
