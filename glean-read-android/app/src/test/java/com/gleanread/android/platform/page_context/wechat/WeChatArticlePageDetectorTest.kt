package com.gleanread.android.platform.page_context.wechat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WeChatArticlePageDetectorTest {
    @Test
    fun `web container class names are recognized`() {
        assertTrue(WeChatArticlePageDetector.isWebContainerClassName("android.webkit.WebView"))
        assertTrue(WeChatArticlePageDetector.isWebContainerClassName("com.tencent.xweb.XWebView"))
        assertTrue(WeChatArticlePageDetector.isWebContainerClassName("org.xwalk.core.XWalkView"))
        assertFalse(WeChatArticlePageDetector.isWebContainerClassName("android.widget.TextView"))
        assertFalse(WeChatArticlePageDetector.isWebContainerClassName(""))
    }

    @Test
    fun `same window result is cached within cache window`() {
        var now = 1_000L
        val detector = WeChatArticlePageDetector(clock = { now })
        var scanCount = 0

        val first = detector.isArticlePageCached(windowId = 7) {
            scanCount += 1
            true
        }
        now += 10_000L
        val second = detector.isArticlePageCached(windowId = 7) {
            scanCount += 1
            false
        }

        assertTrue(first)
        assertTrue(second)
        assertEquals(1, scanCount)
    }

    @Test
    fun `cache expires after cache window`() {
        var now = 1_000L
        val detector = WeChatArticlePageDetector(clock = { now })
        var scanCount = 0

        detector.isArticlePageCached(windowId = 7) {
            scanCount += 1
            true
        }
        now += WeChatCaptureContract.ArticleDetectCacheMillis + 1L
        val refreshed = detector.isArticlePageCached(windowId = 7) {
            scanCount += 1
            false
        }

        assertFalse(refreshed)
        assertEquals(2, scanCount)
    }

    @Test
    fun `different window id bypasses cache`() {
        val detector = WeChatArticlePageDetector(clock = { 1_000L })
        var scanCount = 0

        detector.isArticlePageCached(windowId = 7) {
            scanCount += 1
            true
        }
        val other = detector.isArticlePageCached(windowId = 8) {
            scanCount += 1
            false
        }

        assertFalse(other)
        assertEquals(2, scanCount)
    }

    @Test
    fun `invalidate forces a fresh scan`() {
        val detector = WeChatArticlePageDetector(clock = { 1_000L })
        var scanCount = 0

        detector.isArticlePageCached(windowId = 7) {
            scanCount += 1
            true
        }
        detector.invalidate()
        detector.isArticlePageCached(windowId = 7) {
            scanCount += 1
            true
        }

        assertEquals(2, scanCount)
    }
}
