package com.gleanread.android.platform.page_context.wechat

import com.gleanread.android.platform.page_context.CaptureSeed
import com.gleanread.android.platform.page_context.PageContextSupport
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class WeChatClipboardResolverTest {
    private val baseSeed = CaptureSeed(
        content = "",
        url = "",
        sourceTitle = "文章标题",
        sourcePackage = PageContextSupport.WeChatPackage,
        usedCachedUrl = false,
        usedCachedTitle = true,
    )

    @Test
    fun `plain text fills content`() {
        val updated = WeChatClipboardResolver.applyToSeed(
            seed = baseSeed,
            clip = WeChatClipboardResolver.ClipInput(
                text = "复制出来的一段正文",
                timestampMillis = 10_000L,
            ),
            signals = WeChatCaptureSignals(lastCopyAt = 10_000L),
            now = 11_000L,
        )

        assertEquals("复制出来的一段正文", updated?.content)
        assertEquals("", updated?.url)
    }

    @Test
    fun `wechat article url fills url instead of content`() {
        val updated = WeChatClipboardResolver.applyToSeed(
            seed = baseSeed,
            clip = WeChatClipboardResolver.ClipInput(
                text = "https://mp.weixin.qq.com/s/abcDEF123",
                timestampMillis = 10_000L,
            ),
            signals = WeChatCaptureSignals(lastCopyAt = 10_000L),
            now = 11_000L,
        )

        assertEquals("https://mp.weixin.qq.com/s/abcDEF123", updated?.url)
        assertEquals("", updated?.content)
        assertFalse(updated?.usedCachedUrl ?: true)
    }

    @Test
    fun `stale clip timestamp is rejected`() {
        val updated = WeChatClipboardResolver.applyToSeed(
            seed = baseSeed,
            clip = WeChatClipboardResolver.ClipInput(
                text = "一段很久之前复制的文本",
                timestampMillis = 10_000L,
            ),
            signals = WeChatCaptureSignals(
                lastCopyAt = 10_000L + WeChatCaptureContract.ClipTimestampToleranceMillis + 1L,
            ),
            now = 40_000L,
        )

        assertNull(updated)
    }

    @Test
    fun `zero timestamp falls back to signal window`() {
        val withinWindow = WeChatClipboardResolver.applyToSeed(
            seed = baseSeed,
            clip = WeChatClipboardResolver.ClipInput(text = "正文片段", timestampMillis = 0L),
            signals = WeChatCaptureSignals(lastCopyAt = 10_000L),
            now = 10_000L + WeChatCaptureContract.ClipFallbackWindowMillis,
        )
        val outsideWindow = WeChatClipboardResolver.applyToSeed(
            seed = baseSeed,
            clip = WeChatClipboardResolver.ClipInput(text = "正文片段", timestampMillis = 0L),
            signals = WeChatCaptureSignals(lastCopyAt = 10_000L),
            now = 10_000L + WeChatCaptureContract.ClipFallbackWindowMillis + 1L,
        )

        assertEquals("正文片段", withinWindow?.content)
        assertNull(outsideWindow)
    }

    @Test
    fun `missing copy signal rejects clipboard`() {
        val updated = WeChatClipboardResolver.applyToSeed(
            seed = baseSeed,
            clip = WeChatClipboardResolver.ClipInput(text = "正文", timestampMillis = 10_000L),
            signals = WeChatCaptureSignals(lastCopyAt = 0L),
            now = 11_000L,
        )

        assertNull(updated)
    }

    @Test
    fun `null or blank clip returns null`() {
        assertNull(
            WeChatClipboardResolver.applyToSeed(
                seed = baseSeed,
                clip = null,
                signals = WeChatCaptureSignals(lastCopyAt = 10_000L),
                now = 11_000L,
            ),
        )
        assertNull(
            WeChatClipboardResolver.applyToSeed(
                seed = baseSeed,
                clip = WeChatClipboardResolver.ClipInput(text = "   ", timestampMillis = 10_000L),
                signals = WeChatCaptureSignals(lastCopyAt = 10_000L),
                now = 11_000L,
            ),
        )
    }

    @Test
    fun `unchanged content returns null to avoid useless recomposition`() {
        val seedWithContent = baseSeed.copy(content = "已有正文")

        val updated = WeChatClipboardResolver.applyToSeed(
            seed = seedWithContent,
            clip = WeChatClipboardResolver.ClipInput(text = "已有正文", timestampMillis = 10_000L),
            signals = WeChatCaptureSignals(lastCopyAt = 10_000L),
            now = 11_000L,
        )

        assertNull(updated)
    }

    @Test
    fun `url embedded in share text is extracted`() {
        val updated = WeChatClipboardResolver.applyToSeed(
            seed = baseSeed,
            clip = WeChatClipboardResolver.ClipInput(
                text = "好文分享 https://mp.weixin.qq.com/s/xyz789 值得一读",
                timestampMillis = 10_000L,
            ),
            signals = WeChatCaptureSignals(lastCopyAt = 10_000L),
            now = 11_000L,
        )

        assertEquals("https://mp.weixin.qq.com/s/xyz789", updated?.url)
    }
}
