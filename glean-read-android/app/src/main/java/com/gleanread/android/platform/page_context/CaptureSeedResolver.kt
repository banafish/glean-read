package com.gleanread.android.platform.page_context

import android.content.Intent
import android.net.Uri
import com.gleanread.android.platform.page_context.UrlExtractor
import com.gleanread.android.platform.page_context.wechat.WeChatCaptureContract

data class CaptureSeed(
    val content: String,
    val url: String,
    val sourceTitle: String,
    val sourcePackage: String,
    val usedCachedUrl: Boolean,
    val usedCachedTitle: Boolean,
)

class CaptureSeedResolver(
    private val pageContextStore: PageContextStore,
) {
    fun resolve(
        intent: Intent?,
        referrer: Uri?,
        now: Long = System.currentTimeMillis(),
    ): CaptureSeed {
        // 微信气泡入口：正文留待获焦后从剪贴板填充，标题/URL 只认微信快照
        if (intent?.action == WeChatCaptureContract.ActionWeChatCapture) {
            return resolveWeChatCaptureSeed(now)
        }

        // 浏览器划词 deep link 入口：URL/标题/选中文本随启动 intent 原子送达，
        // 不读快照、不走启发式；url 参数缺失时回落到下方通用逻辑当普通 intent 处理
        if (WebCaptureContract.isWebCaptureIntent(intent)) {
            resolveWebCaptureSeed(intent, referrer)?.let { return it }
        }

        val explicitContent = resolveExplicitContent(intent).trim()
        val explicitUrl = intent?.let(UrlExtractor::extract).orEmpty().trim()
        val explicitTitle = resolveExplicitTitle(intent, explicitContent, explicitUrl)
        val sourcePackage = resolveSourcePackage(intent, referrer)
        val snapshot = pageContextStore.readRecentSnapshot(
            expectedSourcePackage = sourcePackage,
            now = now,
        )

        val finalUrl = explicitUrl.ifBlank { snapshot?.sourceUrl.orEmpty() }
        val finalTitle = explicitTitle.ifBlank { snapshot?.sourceTitle.orEmpty() }
        val normalizedContent = when {
            explicitContent.isNotBlank() -> explicitContent
            finalTitle.isNotBlank() -> finalTitle
            else -> ""
        }

        return CaptureSeed(
            content = normalizedContent,
            url = finalUrl,
            sourceTitle = finalTitle,
            sourcePackage = sourcePackage.orEmpty(),
            usedCachedUrl = explicitUrl.isBlank() && finalUrl.isNotBlank(),
            usedCachedTitle = explicitTitle.isBlank() && finalTitle.isNotBlank(),
        )
    }

    /**
     * 微信气泡入口的初始 seed：content 恒空（禁止“标题回退为正文”），
     * 标题与 URL 仅从未过期的微信快照补齐。
     */
    private fun resolveWeChatCaptureSeed(now: Long): CaptureSeed {
        val snapshot = pageContextStore.readRecentSnapshot(
            expectedSourcePackage = PageContextSupport.WeChatPackage,
            now = now,
        )
        val cachedUrl = snapshot?.sourceUrl.orEmpty()
        val cachedTitle = snapshot?.sourceTitle.orEmpty()
        return CaptureSeed(
            content = "",
            url = cachedUrl,
            sourceTitle = cachedTitle,
            sourcePackage = PageContextSupport.WeChatPackage,
            usedCachedUrl = cachedUrl.isNotBlank(),
            usedCachedTitle = cachedTitle.isNotBlank(),
        )
    }

    /**
     * 浏览器划词 deep link 的 seed：三项数据全部取自 `gleanread://capture` 的
     * query 参数（`Uri.getQueryParameter` 已完成 URL 解码），保证与页面实际内容
     * 一致，因此不读快照、不标记 usedCached。url 参数缺失视为非法触达，
     * 返回 null 交由通用逻辑兜底。
     */
    private fun resolveWebCaptureSeed(intent: Intent?, referrer: Uri?): CaptureSeed? {
        val data = intent?.data ?: return null
        val url = data.getQueryParameter(WebCaptureContract.ParamUrl)?.trim().orEmpty()
        if (url.isBlank()) return null
        val title = data.getQueryParameter(WebCaptureContract.ParamTitle)?.trim().orEmpty()
        val text = data.getQueryParameter(WebCaptureContract.ParamText)?.trim().orEmpty()
        return CaptureSeed(
            content = text,
            url = url,
            sourceTitle = title,
            sourcePackage = resolveSourcePackage(intent, referrer).orEmpty(),
            usedCachedUrl = false,
            usedCachedTitle = false,
        )
    }

    private fun resolveExplicitContent(intent: Intent?): String {
        if (intent == null) return ""
        val action = intent.action.orEmpty()
        return when (action) {
            Intent.ACTION_PROCESS_TEXT -> {
                intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString().orEmpty()
            }

            Intent.ACTION_SEND -> {
                val text = intent.getStringExtra(Intent.EXTRA_TEXT).orEmpty()
                val subject = intent.getStringExtra(Intent.EXTRA_SUBJECT).orEmpty()
                val url = UrlExtractor.extract(intent).orEmpty()
                if (text.trim() == url.trim() && PageContextTextRules.isMeaningfulShareSubject(subject)) {
                    subject
                } else {
                    text
                }
            }

            else -> {
                intent.getStringExtra(Intent.EXTRA_TEXT).orEmpty()
            }
        }
    }

    private fun resolveExplicitTitle(
        intent: Intent?,
        explicitContent: String,
        explicitUrl: String,
    ): String {
        val subject = intent?.getStringExtra(Intent.EXTRA_SUBJECT)?.trim().orEmpty()
        return subject.takeUnless {
            it.isBlank() ||
                it == explicitContent ||
                it == explicitUrl ||
                !PageContextTextRules.isMeaningfulShareSubject(it)
        }.orEmpty()
    }

    private fun resolveSourcePackage(intent: Intent?, referrer: Uri?): String? {
        val referrerCandidates = listOfNotNull(
            referrer,
            intent?.getParcelableUriExtra(Intent.EXTRA_REFERRER),
            intent?.getStringExtra(Intent.EXTRA_REFERRER_NAME)?.let(Uri::parse),
        )

        val referrerPackage = referrerCandidates
            .asSequence()
            .mapNotNull(::extractAndroidAppPackage)
            .map(String::trim)
            .firstOrNull { it.isNotEmpty() }

        return referrerPackage ?: inferSourcePackageFromIntent(intent)
    }

    @Suppress("DEPRECATION")
    private fun Intent.getParcelableUriExtra(name: String): Uri? {
        return getParcelableExtra(name)
    }

    private fun extractAndroidAppPackage(uri: Uri?): String? {
        return uri
            ?.takeIf { it.scheme == "android-app" }
            ?.host
    }

    private fun inferSourcePackageFromIntent(intent: Intent?): String? {
        val safeIntent = intent ?: return null
        val extras = safeIntent.extras
        val keys = extras?.keySet().orEmpty()
        return when {
            keys.any { it.startsWith("org.chromium.chrome.") || it.startsWith("org.chromium.chrome.browser.") } -> {
                PageContextSupport.ChromePackage
            }

            safeIntent.`package`?.isNotBlank() == true -> safeIntent.`package`
            else -> null
        }
    }
}
