package com.gleanread.android.platform.page_context

import android.content.Intent
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.gleanread.android.platform.page_context.wechat.WeChatCaptureContract
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CaptureSeedResolverTest {
    private lateinit var pageContextStore: PageContextStore
    private lateinit var resolver: CaptureSeedResolver

    @Before
    fun setUp() {
        pageContextStore = PageContextStore(ApplicationProvider.getApplicationContext())
        pageContextStore.clear()
        resolver = CaptureSeedResolver(pageContextStore)
    }

    @After
    fun tearDown() {
        pageContextStore.clear()
    }

    @Test
    fun `resolver uses explicit send extras before cached context`() {
        pageContextStore.save(
            PageContextSnapshot(
                sourcePackage = PageContextSupport.ChromePackage,
                sourceTitle = "缓存标题",
                sourceUrl = "https://cached.example.com",
                capturedAt = 10L,
                captureSource = PageContextSupport.AccessibilityCaptureSource,
                confidence = 0.9f,
            ),
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "显式正文")
            putExtra(Intent.EXTRA_SUBJECT, "显式标题")
        }

        val seed = resolver.resolve(
            intent = intent,
            referrer = Uri.parse("android-app://${PageContextSupport.ChromePackage}"),
            now = 11L,
        )

        assertEquals("显式正文", seed.content)
        assertEquals("显式标题", seed.sourceTitle)
        assertEquals("https://cached.example.com", seed.url)
        assertFalse(seed.usedCachedTitle)
        assertTrue(seed.usedCachedUrl)
    }

    @Test
    fun `resolver backfills missing title and url from recent snapshot`() {
        pageContextStore.save(
            PageContextSnapshot(
                sourcePackage = PageContextSupport.WeChatPackage,
                sourceTitle = "公众号文章标题",
                sourceUrl = "https://mp.weixin.qq.com/s/test",
                capturedAt = 100L,
                captureSource = PageContextSupport.AccessibilityCaptureSource,
                confidence = 0.95f,
            ),
        )

        val intent = Intent(Intent.ACTION_PROCESS_TEXT).apply {
            putExtra(Intent.EXTRA_PROCESS_TEXT, "分享出来的选中文本")
        }

        val seed = resolver.resolve(
            intent = intent,
            referrer = Uri.parse("android-app://${PageContextSupport.WeChatPackage}"),
            now = 150L,
        )

        assertEquals("分享出来的选中文本", seed.content)
        assertEquals("公众号文章标题", seed.sourceTitle)
        assertEquals("https://mp.weixin.qq.com/s/test", seed.url)
        assertTrue(seed.usedCachedTitle)
        assertTrue(seed.usedCachedUrl)
    }

    @Test
    fun `resolver ignores expired snapshot`() {
        pageContextStore.save(
            PageContextSnapshot(
                sourcePackage = PageContextSupport.ChromePackage,
                sourceTitle = "过期标题",
                sourceUrl = "https://expired.example.com",
                capturedAt = 0L,
                captureSource = PageContextSupport.AccessibilityCaptureSource,
                confidence = 0.8f,
            ),
        )

        val intent = Intent(Intent.ACTION_PROCESS_TEXT).apply {
            putExtra(Intent.EXTRA_PROCESS_TEXT, "只剩正文")
        }

        val seed = resolver.resolve(
            intent = intent,
            referrer = Uri.parse("android-app://${PageContextSupport.ChromePackage}"),
            now = PageContextSupport.CacheTtlMillis + 1L,
        )

        assertEquals("只剩正文", seed.content)
        assertEquals("", seed.sourceTitle)
        assertEquals("", seed.url)
        assertFalse(seed.usedCachedTitle)
        assertFalse(seed.usedCachedUrl)
    }

    @Test
    fun `resolver uses referrer extras when activity referrer is missing`() {
        pageContextStore.save(
            PageContextSnapshot(
                sourcePackage = PageContextSupport.ChromePackage,
                sourceTitle = "缓存标题",
                sourceUrl = "https://cached.example.com/page",
                capturedAt = 200L,
                captureSource = PageContextSupport.AccessibilityCaptureSource,
                confidence = 0.9f,
            ),
        )

        val intent = Intent(Intent.ACTION_PROCESS_TEXT).apply {
            putExtra(Intent.EXTRA_PROCESS_TEXT, "选中的文本")
            putExtra(
                Intent.EXTRA_REFERRER_NAME,
                "android-app://${PageContextSupport.ChromePackage}",
            )
        }

        val seed = resolver.resolve(
            intent = intent,
            referrer = null,
            now = 220L,
        )

        assertEquals(PageContextSupport.ChromePackage, seed.sourcePackage)
        assertEquals("缓存标题", seed.sourceTitle)
        assertEquals("https://cached.example.com/page", seed.url)
        assertTrue(seed.usedCachedTitle)
        assertTrue(seed.usedCachedUrl)
    }

    @Test
    fun `wechat capture action seeds from cache with empty content`() {
        pageContextStore.save(
            PageContextSnapshot(
                sourcePackage = PageContextSupport.WeChatPackage,
                sourceTitle = "公众号文章标题",
                sourceUrl = "https://mp.weixin.qq.com/s/test",
                capturedAt = 100L,
                captureSource = PageContextSupport.AccessibilityCaptureSource,
                confidence = 0.95f,
            ),
        )

        val intent = Intent(WeChatCaptureContract.ActionWeChatCapture)

        val seed = resolver.resolve(
            intent = intent,
            referrer = null,
            now = 150L,
        )

        assertEquals("", seed.content)
        assertEquals("公众号文章标题", seed.sourceTitle)
        assertEquals("https://mp.weixin.qq.com/s/test", seed.url)
        assertEquals(PageContextSupport.WeChatPackage, seed.sourcePackage)
        assertTrue(seed.usedCachedTitle)
        assertTrue(seed.usedCachedUrl)
    }

    @Test
    fun `wechat capture action without cache keeps everything empty`() {
        val intent = Intent(WeChatCaptureContract.ActionWeChatCapture)

        val seed = resolver.resolve(
            intent = intent,
            referrer = null,
            now = 150L,
        )

        assertEquals("", seed.content)
        assertEquals("", seed.sourceTitle)
        assertEquals("", seed.url)
        assertEquals(PageContextSupport.WeChatPackage, seed.sourcePackage)
        assertFalse(seed.usedCachedTitle)
        assertFalse(seed.usedCachedUrl)
    }

    @Test
    fun `wechat capture action ignores browser snapshot`() {
        pageContextStore.save(
            PageContextSnapshot(
                sourcePackage = PageContextSupport.ChromePackage,
                sourceTitle = "浏览器标题",
                sourceUrl = "https://example.com",
                capturedAt = 100L,
                captureSource = PageContextSupport.AccessibilityCaptureSource,
                confidence = 0.95f,
            ),
        )

        val intent = Intent(WeChatCaptureContract.ActionWeChatCapture)

        val seed = resolver.resolve(
            intent = intent,
            referrer = null,
            now = 150L,
        )

        assertEquals("", seed.sourceTitle)
        assertEquals("", seed.url)
    }

    @Test
    fun `resolver ignores generic share subject placeholders`() {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "分享出来的正文")
            putExtra(Intent.EXTRA_SUBJECT, "Including link:")
        }

        val seed = resolver.resolve(
            intent = intent,
            referrer = null,
            now = 1L,
        )

        assertEquals("分享出来的正文", seed.content)
        assertEquals("", seed.sourceTitle)
    }

    @Test
    fun `resolver ignores browser sharing text placeholder title`() {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "分享出来的正文")
            putExtra(Intent.EXTRA_SUBJECT, "Sharing text")
        }

        val seed = resolver.resolve(
            intent = intent,
            referrer = null,
            now = 1L,
        )

        assertEquals("分享出来的正文", seed.content)
        assertEquals("", seed.sourceTitle)
    }

    @Test
    fun `web capture deep link seeds all fields from query and ignores cache`() {
        pageContextStore.save(
            PageContextSnapshot(
                sourcePackage = PageContextSupport.ChromePackage,
                sourceTitle = "缓存标题",
                sourceUrl = "https://cached.example.com",
                capturedAt = 100L,
                captureSource = PageContextSupport.AccessibilityCaptureSource,
                confidence = 0.9f,
            ),
        )

        val rawUri = "gleanread://capture" +
            "?url=" + Uri.encode("https://article.example.com/post/1") +
            "&title=" + Uri.encode("文章标题") +
            "&text=" + Uri.encode("选中的正文")
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(rawUri))

        val seed = resolver.resolve(
            intent = intent,
            referrer = Uri.parse("android-app://com.mmbox.xbrowser"),
            now = 150L,
        )

        assertEquals("选中的正文", seed.content)
        assertEquals("https://article.example.com/post/1", seed.url)
        assertEquals("文章标题", seed.sourceTitle)
        assertEquals("com.mmbox.xbrowser", seed.sourcePackage)
        assertFalse(seed.usedCachedUrl)
        assertFalse(seed.usedCachedTitle)
    }

    @Test
    fun `web capture deep link tolerates missing title and text`() {
        val rawUri = "gleanread://capture?url=" + Uri.encode("https://article.example.com/post/2")
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(rawUri))

        val seed = resolver.resolve(
            intent = intent,
            referrer = null,
            now = 1L,
        )

        assertEquals("", seed.content)
        assertEquals("https://article.example.com/post/2", seed.url)
        assertEquals("", seed.sourceTitle)
        assertEquals("", seed.sourcePackage)
        assertFalse(seed.usedCachedUrl)
        assertFalse(seed.usedCachedTitle)
    }

    @Test
    fun `web capture deep link without url falls back to generic resolution`() {
        pageContextStore.save(
            PageContextSnapshot(
                sourcePackage = PageContextSupport.ChromePackage,
                sourceTitle = "缓存标题",
                sourceUrl = "https://cached.example.com/page",
                capturedAt = 100L,
                captureSource = PageContextSupport.AccessibilityCaptureSource,
                confidence = 0.9f,
            ),
        )

        // url 参数缺失：web 分支放弃接管，回落通用逻辑（快照补齐标题/URL，query 里的 text 不生效）
        val rawUri = "gleanread://capture" +
            "?title=" + Uri.encode("只有标题") +
            "&text=" + Uri.encode("只有正文")
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(rawUri))

        val seed = resolver.resolve(
            intent = intent,
            referrer = Uri.parse("android-app://${PageContextSupport.ChromePackage}"),
            now = 150L,
        )

        assertEquals("缓存标题", seed.content)
        assertEquals("https://cached.example.com/page", seed.url)
        assertEquals("缓存标题", seed.sourceTitle)
        assertTrue(seed.usedCachedUrl)
        assertTrue(seed.usedCachedTitle)
    }

    @Test
    fun `web capture deep link decodes special characters`() {
        val sourceUrl = "https://example.com/路径?a=1&b=中文#片段"
        val title = "深度阅读 & 摘录 100%"
        val text = "第一段\n第二段 <引用>"
        // Uri.encode 与 userscript 侧 encodeURIComponent 的保留字符集一致，可模拟脚本组装产物
        val rawUri = "gleanread://capture" +
            "?url=" + Uri.encode(sourceUrl) +
            "&title=" + Uri.encode(title) +
            "&text=" + Uri.encode(text)
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(rawUri))

        val seed = resolver.resolve(
            intent = intent,
            referrer = null,
            now = 1L,
        )

        assertEquals(text, seed.content)
        assertEquals(sourceUrl, seed.url)
        assertEquals(title, seed.sourceTitle)
    }

    @Test
    fun `wechat capture action wins over web capture data`() {
        val rawUri = "gleanread://capture?url=" + Uri.encode("https://article.example.com")
        val intent = Intent(WeChatCaptureContract.ActionWeChatCapture, Uri.parse(rawUri))

        val seed = resolver.resolve(
            intent = intent,
            referrer = null,
            now = 150L,
        )

        // 微信分支按 action 前置命中：content 恒空，URL 不读 deep link query
        assertEquals("", seed.content)
        assertEquals("", seed.url)
        assertEquals(PageContextSupport.WeChatPackage, seed.sourcePackage)
    }

    @Test
    fun `auth callback deep link is not treated as web capture`() {
        val intent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("gleanread://auth/callback?code=abc"),
        )

        val seed = resolver.resolve(
            intent = intent,
            referrer = null,
            now = 1L,
        )

        // host 不是 capture：走通用逻辑，且 gleanread:// dataString 不会被误提取为 URL
        assertEquals("", seed.content)
        assertEquals("", seed.url)
        assertEquals("", seed.sourceTitle)
        assertFalse(seed.usedCachedUrl)
        assertFalse(seed.usedCachedTitle)
    }
}
