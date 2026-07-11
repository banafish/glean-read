package com.gleanread.android.platform.page_context

data class PageContextSnapshot(
    val sourcePackage: String,
    val sourceTitle: String,
    val sourceUrl: String,
    val capturedAt: Long,
    val captureSource: String,
    val confidence: Float,
)

object PageContextSupport {
    const val AccessibilityCaptureSource = "accessibility"
    const val CacheTtlMillis = 60_000L

    /**
     * 微信公众号快照使用更长有效期：
     * 用户常在「复制链接」后继续阅读几分钟再复制正文，60 秒会让链接上下文过早失效。
     */
    const val WeChatCacheTtlMillis = 600_000L

    const val ChromePackage = "com.android.chrome"
    const val FirefoxPackage = "org.mozilla.firefox"
    const val EdgePackage = "com.microsoft.emmx"
    const val SamsungInternetPackage = "com.sec.android.app.sbrowser"
    const val WeChatPackage = "com.tencent.mm"

    private val supportedPackages = setOf(
        ChromePackage,
        FirefoxPackage,
        EdgePackage,
        SamsungInternetPackage,
        WeChatPackage,
    )

    fun isSupportedPackage(packageName: String): Boolean {
        return supportedPackages.contains(packageName)
    }

    /** 快照有效期按来源宿主区分：微信 10 分钟，其余 60 秒 */
    fun cacheTtlMillisFor(packageName: String): Long {
        return if (packageName == WeChatPackage) WeChatCacheTtlMillis else CacheTtlMillis
    }
}
