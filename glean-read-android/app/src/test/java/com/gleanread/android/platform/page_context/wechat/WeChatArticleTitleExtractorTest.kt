package com.gleanread.android.platform.page_context.wechat

import com.gleanread.android.platform.page_context.wechat.WeChatArticleTitleExtractor.TitleCandidate
import org.junit.Assert.assertEquals
import org.junit.Test

class WeChatArticleTitleExtractorTest {
    private val screenHeight = 2400

    @Test
    fun `web content title beats native top bar account name`() {
        val title = WeChatArticleTitleExtractor.extract(
            eventTexts = emptyList(),
            candidates = listOf(
                TitleCandidate(
                    text = "科技早知道",
                    viewId = "",
                    className = "android.widget.TextView",
                    boundsTop = 120,
                    inWebContainer = false,
                ),
                TitleCandidate(
                    text = "深度解读：大模型时代的阅读工作流",
                    viewId = "",
                    className = "android.view.View",
                    boundsTop = 480,
                    inWebContainer = true,
                ),
            ),
            screenHeight = screenHeight,
        )

        assertEquals("深度解读：大模型时代的阅读工作流", title)
    }

    @Test
    fun `title below legacy 520px cap is still selectable on tall screens`() {
        val title = WeChatArticleTitleExtractor.extract(
            eventTexts = emptyList(),
            candidates = listOf(
                TitleCandidate(
                    text = "这是一篇标题在屏幕较低位置的公众号文章",
                    viewId = "",
                    className = "android.view.View",
                    boundsTop = 800,
                    inWebContainer = true,
                ),
            ),
            screenHeight = screenHeight,
        )

        assertEquals("这是一篇标题在屏幕较低位置的公众号文章", title)
    }

    @Test
    fun `generic and navigation labels are never picked`() {
        val title = WeChatArticleTitleExtractor.extract(
            eventTexts = emptyList(),
            candidates = listOf(
                TitleCandidate(
                    text = "复制",
                    viewId = "",
                    className = "android.widget.TextView",
                    boundsTop = 300,
                    inWebContainer = true,
                ),
                TitleCandidate(
                    text = "返回",
                    viewId = "",
                    className = "android.widget.TextView",
                    boundsTop = 100,
                    inWebContainer = false,
                ),
            ),
            screenHeight = screenHeight,
        )

        assertEquals("", title)
    }

    @Test
    fun `url-like text is never picked as title`() {
        val title = WeChatArticleTitleExtractor.extract(
            eventTexts = emptyList(),
            candidates = listOf(
                TitleCandidate(
                    text = "https://mp.weixin.qq.com/s/abc",
                    viewId = "",
                    className = "android.view.View",
                    boundsTop = 400,
                    inWebContainer = true,
                ),
            ),
            screenHeight = screenHeight,
        )

        assertEquals("", title)
    }

    @Test
    fun `event text is used as fallback when no candidate scores`() {
        val title = WeChatArticleTitleExtractor.extract(
            eventTexts = listOf("窗口事件里携带的文章标题"),
            candidates = emptyList(),
            screenHeight = screenHeight,
        )

        assertEquals("窗口事件里携带的文章标题", title)
    }

    @Test
    fun `short action-like text is not treated as title`() {
        val title = WeChatArticleTitleExtractor.extract(
            eventTexts = emptyList(),
            candidates = listOf(
                TitleCandidate(
                    text = "在看",
                    viewId = "",
                    className = "android.widget.TextView",
                    boundsTop = 2200,
                    inWebContainer = true,
                ),
            ),
            screenHeight = screenHeight,
        )

        assertEquals("", title)
    }

    @Test
    fun `empty inputs return empty title`() {
        val title = WeChatArticleTitleExtractor.extract(
            eventTexts = emptyList(),
            candidates = emptyList(),
            screenHeight = screenHeight,
        )

        assertEquals("", title)
    }
}
