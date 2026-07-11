package com.gleanread.android.platform.page_context.wechat

import android.content.Context

/** 微信复制成功信号（不含剪贴板内容本身），供剪贴板新鲜度校验使用 */
data class WeChatCaptureSignals(
    val lastCopyAt: Long,
)

/**
 * 记录无障碍服务观察到的微信「复制成功」toast 时间戳。
 * toast 无法区分「复制正文」与「复制链接」，也无需区分：
 * 下游只用时间戳做新鲜度校验，正文/链接由剪贴板内容本身判别。
 * 服务进程写入，摘录弹窗进程内读取，用 SharedPreferences 直读保证跨组件可见。
 */
class WeChatCaptureSignalStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )

    fun markCopyObserved(at: Long) {
        preferences.edit().putLong(KEY_LAST_COPY_AT, at).apply()
    }

    fun read(): WeChatCaptureSignals {
        return WeChatCaptureSignals(
            lastCopyAt = preferences.getLong(KEY_LAST_COPY_AT, 0L),
        )
    }

    fun clear() {
        preferences.edit().clear().apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "wechat_capture_signals"
        const val KEY_LAST_COPY_AT = "last_copy_at"
    }
}
