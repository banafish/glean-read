package com.gleanread.android.platform.page_context

import android.content.Context

class PageContextStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )

    fun save(snapshot: PageContextSnapshot) {
        preferences.edit()
            .putString(KEY_SOURCE_PACKAGE, snapshot.sourcePackage)
            .putString(KEY_SOURCE_TITLE, snapshot.sourceTitle)
            .putString(KEY_SOURCE_URL, snapshot.sourceUrl)
            .putLong(KEY_CAPTURED_AT, snapshot.capturedAt)
            .putString(KEY_CAPTURE_SOURCE, snapshot.captureSource)
            .putFloat(KEY_CONFIDENCE, snapshot.confidence)
            .apply()
    }

    fun clear() {
        preferences.edit().clear().apply()
    }

    fun readRecentSnapshot(
        expectedSourcePackage: String?,
        now: Long = System.currentTimeMillis(),
    ): PageContextSnapshot? {
        val capturedAt = preferences.getLong(KEY_CAPTURED_AT, 0L)
        if (capturedAt <= 0L) {
            return null
        }

        val sourcePackage = preferences.getString(KEY_SOURCE_PACKAGE, null).orEmpty()
        if (sourcePackage.isBlank() || !matchesExpectedPackage(sourcePackage, expectedSourcePackage)) {
            return null
        }

        // 有效期按已存快照的来源宿主区分（微信更长），而不是统一 60 秒
        if (now - capturedAt > PageContextSupport.cacheTtlMillisFor(sourcePackage)) {
            return null
        }

        return PageContextSnapshot(
            sourcePackage = sourcePackage,
            sourceTitle = preferences.getString(KEY_SOURCE_TITLE, null).orEmpty(),
            sourceUrl = preferences.getString(KEY_SOURCE_URL, null).orEmpty(),
            capturedAt = capturedAt,
            captureSource = preferences.getString(KEY_CAPTURE_SOURCE, null)
                ?: PageContextSupport.AccessibilityCaptureSource,
            confidence = preferences.getFloat(KEY_CONFIDENCE, 0f),
        )
    }

    /**
     * 把快摘弹窗从剪贴板识别出的公众号文章链接回写到快照：
     * 已有微信快照时仅覆盖 URL 并刷新采集时间（保留标题），否则新建仅含 URL 的微信快照。
     */
    fun mergeWeChatUrl(
        url: String,
        now: Long = System.currentTimeMillis(),
    ) {
        val normalizedUrl = url.trim()
        if (normalizedUrl.isBlank()) return

        val existing = readRecentSnapshot(
            expectedSourcePackage = PageContextSupport.WeChatPackage,
            now = now,
        )
        val snapshot = if (existing != null) {
            existing.copy(
                sourceUrl = normalizedUrl,
                capturedAt = now,
                confidence = if (existing.sourceTitle.isNotBlank()) 0.95f else 0.7f,
            )
        } else {
            PageContextSnapshot(
                sourcePackage = PageContextSupport.WeChatPackage,
                sourceTitle = "",
                sourceUrl = normalizedUrl,
                capturedAt = now,
                captureSource = PageContextSupport.AccessibilityCaptureSource,
                confidence = 0.7f,
            )
        }
        save(snapshot)
    }

    private fun matchesExpectedPackage(
        snapshotPackage: String,
        expectedSourcePackage: String?,
    ): Boolean {
        val normalizedExpected = expectedSourcePackage?.trim().orEmpty()
        return if (normalizedExpected.isNotEmpty()) {
            snapshotPackage == normalizedExpected
        } else {
            PageContextSupport.isSupportedPackage(snapshotPackage)
        }
    }

    private companion object {
        const val PREFERENCES_NAME = "page_context_cache"
        const val KEY_SOURCE_PACKAGE = "source_package"
        const val KEY_SOURCE_TITLE = "source_title"
        const val KEY_SOURCE_URL = "source_url"
        const val KEY_CAPTURED_AT = "captured_at"
        const val KEY_CAPTURE_SOURCE = "capture_source"
        const val KEY_CONFIDENCE = "confidence"
    }
}
