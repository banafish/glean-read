package com.gleanread.android.platform.page_context

import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PageContextStoreTest {
    private lateinit var store: PageContextStore

    @Before
    fun setUp() {
        store = PageContextStore(ApplicationProvider.getApplicationContext())
        store.clear()
    }

    @After
    fun tearDown() {
        store.clear()
    }

    @Test
    fun `wechat snapshot survives five minutes`() {
        store.save(wechatSnapshot(capturedAt = 1L))

        val snapshot = store.readRecentSnapshot(
            expectedSourcePackage = PageContextSupport.WeChatPackage,
            now = 300_000L,
        )

        assertNotNull(snapshot)
    }

    @Test
    fun `wechat snapshot expires after ten minutes`() {
        store.save(wechatSnapshot(capturedAt = 1L))

        val snapshot = store.readRecentSnapshot(
            expectedSourcePackage = PageContextSupport.WeChatPackage,
            now = 600_002L,
        )

        assertNull(snapshot)
    }

    @Test
    fun `browser snapshot keeps sixty second ttl`() {
        store.save(chromeSnapshot(capturedAt = 1L))

        assertNotNull(
            store.readRecentSnapshot(
                expectedSourcePackage = PageContextSupport.ChromePackage,
                now = 60_001L,
            ),
        )
        assertNull(
            store.readRecentSnapshot(
                expectedSourcePackage = PageContextSupport.ChromePackage,
                now = 60_002L,
            ),
        )
    }

    @Test
    fun `mergeWeChatUrl keeps existing title and refreshes captured time`() {
        store.save(
            wechatSnapshot(
                capturedAt = 1_000L,
                title = "文章标题",
                url = "",
            ),
        )

        store.mergeWeChatUrl(url = "https://mp.weixin.qq.com/s/abc", now = 2_000L)

        val snapshot = store.readRecentSnapshot(
            expectedSourcePackage = PageContextSupport.WeChatPackage,
            now = 2_500L,
        )
        assertNotNull(snapshot)
        assertEquals("文章标题", snapshot?.sourceTitle)
        assertEquals("https://mp.weixin.qq.com/s/abc", snapshot?.sourceUrl)
        assertEquals(2_000L, snapshot?.capturedAt)
    }

    @Test
    fun `mergeWeChatUrl creates url-only snapshot when store is empty`() {
        store.mergeWeChatUrl(url = "https://mp.weixin.qq.com/s/xyz", now = 5_000L)

        val snapshot = store.readRecentSnapshot(
            expectedSourcePackage = PageContextSupport.WeChatPackage,
            now = 5_500L,
        )
        assertNotNull(snapshot)
        assertEquals(PageContextSupport.WeChatPackage, snapshot?.sourcePackage)
        assertEquals("", snapshot?.sourceTitle)
        assertEquals("https://mp.weixin.qq.com/s/xyz", snapshot?.sourceUrl)
    }

    @Test
    fun `mergeWeChatUrl replaces stale browser snapshot with wechat context`() {
        store.save(chromeSnapshot(capturedAt = 1_000L))

        store.mergeWeChatUrl(url = "https://mp.weixin.qq.com/s/new", now = 2_000L)

        val snapshot = store.readRecentSnapshot(
            expectedSourcePackage = PageContextSupport.WeChatPackage,
            now = 2_500L,
        )
        assertNotNull(snapshot)
        assertEquals(PageContextSupport.WeChatPackage, snapshot?.sourcePackage)
        assertEquals("https://mp.weixin.qq.com/s/new", snapshot?.sourceUrl)
    }

    @Test
    fun `mergeWeChatUrl ignores blank url`() {
        store.mergeWeChatUrl(url = "   ", now = 1_000L)

        assertNull(
            store.readRecentSnapshot(
                expectedSourcePackage = PageContextSupport.WeChatPackage,
                now = 1_500L,
            ),
        )
    }

    private fun wechatSnapshot(
        capturedAt: Long,
        title: String = "公众号文章",
        url: String = "https://mp.weixin.qq.com/s/test",
    ) = PageContextSnapshot(
        sourcePackage = PageContextSupport.WeChatPackage,
        sourceTitle = title,
        sourceUrl = url,
        capturedAt = capturedAt,
        captureSource = PageContextSupport.AccessibilityCaptureSource,
        confidence = 0.95f,
    )

    private fun chromeSnapshot(capturedAt: Long) = PageContextSnapshot(
        sourcePackage = PageContextSupport.ChromePackage,
        sourceTitle = "浏览器页面",
        sourceUrl = "https://example.com/page",
        capturedAt = capturedAt,
        captureSource = PageContextSupport.AccessibilityCaptureSource,
        confidence = 0.95f,
    )
}
