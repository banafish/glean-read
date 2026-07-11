package com.gleanread.android.platform.page_context.wechat

import androidx.test.core.app.ApplicationProvider
import com.gleanread.android.platform.page_context.PageContextSnapshot
import com.gleanread.android.platform.page_context.PageContextStore
import com.gleanread.android.platform.page_context.PageContextSupport
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WeChatCaptureCoordinatorTest {
    private lateinit var pageContextStore: PageContextStore
    private lateinit var signalStore: WeChatCaptureSignalStore
    private lateinit var bubble: RecordingBubble
    private var launchCount = 0
    private var now = 10_000L

    @Before
    fun setUp() {
        pageContextStore = PageContextStore(ApplicationProvider.getApplicationContext())
        pageContextStore.clear()
        signalStore = WeChatCaptureSignalStore(ApplicationProvider.getApplicationContext())
        signalStore.clear()
        bubble = RecordingBubble()
        launchCount = 0
        now = 10_000L
    }

    private fun createCoordinator(): WeChatCaptureCoordinator {
        return WeChatCaptureCoordinator(
            pageContextStore = pageContextStore,
            signalStore = signalStore,
            onLaunchCapture = { launchCount += 1 },
            clock = { now },
            bubbleFactory = { onTapped ->
                bubble.onTapped = onTapped
                bubble
            },
        )
    }

    @Test
    fun `copy toast on article page marks signal and shows bubble`() {
        val coordinator = createCoordinator()

        coordinator.handleCopyToast(
            toastTexts = listOf("内容已复制", "微信"),
            articleScan = { true },
        )

        assertEquals(now, signalStore.read().lastCopyAt)
        assertEquals(1, bubble.showCount)
    }

    @Test
    fun `article window class fallback triggers when node scan fails`() {
        val coordinator = createCoordinator()

        // 真机证据：公众号文章页窗口类名 TmplWebViewMMUI（含 WebView），可兜底节点扫描失败
        coordinator.noteWindowStateChanged(
            "com.tencent.mm.plugin.brandservice.ui.timeline.preload.ui.TmplWebViewMMUI",
        )
        coordinator.handleCopyToast(
            toastTexts = listOf("内容已复制"),
            articleScan = { false },
        )

        assertEquals(1, bubble.showCount)
    }

    @Test
    fun `dialog window does not pollute article window fallback`() {
        val coordinator = createCoordinator()

        // 真机复现序列：文章页 → 点「⋯」弹出分享菜单（dialog 窗口事件）→ 关闭菜单（无事件）→ 复制正文
        coordinator.noteWindowStateChanged(
            "com.tencent.mm.plugin.brandservice.ui.timeline.preload.ui.TmplWebViewMMUI",
        )
        coordinator.noteWindowStateChanged("com.tencent.mm.ui.widget.dialog.a4")
        coordinator.handleCopyToast(
            toastTexts = listOf("内容已复制"),
            articleScan = { false },
        )

        assertEquals(1, bubble.showCount)
    }

    @Test
    fun `leaving article page via activity switch clears fallback`() {
        val coordinator = createCoordinator()

        // 文章页 → 返回微信主界面（Activity 级切换）→ 聊天里复制不应弹泡
        coordinator.noteWindowStateChanged(
            "com.tencent.mm.plugin.brandservice.ui.timeline.preload.ui.TmplWebViewMMUI",
        )
        coordinator.noteWindowStateChanged("com.tencent.mm.ui.LauncherUI")
        coordinator.handleCopyToast(
            toastTexts = listOf("内容已复制"),
            articleScan = { false },
        )

        assertEquals(0, bubble.showCount)
    }

    @Test
    fun `native window class does not act as article fallback`() {
        val coordinator = createCoordinator()

        coordinator.noteWindowStateChanged("com.tencent.mm.ui.LauncherUI")
        coordinator.handleCopyToast(
            toastTexts = listOf("内容已复制"),
            articleScan = { false },
        )

        assertEquals(0, bubble.showCount)
        assertEquals(0L, signalStore.read().lastCopyAt)
    }

    @Test
    fun `toasts inside debounce window are ignored`() {
        val coordinator = createCoordinator()

        coordinator.handleCopyToast(listOf("内容已复制"), articleScan = { true })
        now += WeChatCaptureContract.CopyToastDebounceMillis - 1L
        coordinator.handleCopyToast(listOf("内容已复制"), articleScan = { true })

        assertEquals(1, bubble.showCount)
    }

    @Test
    fun `toasts after debounce window are handled again`() {
        val coordinator = createCoordinator()

        coordinator.handleCopyToast(listOf("内容已复制"), articleScan = { true })
        now += WeChatCaptureContract.CopyToastDebounceMillis + 1L
        coordinator.handleCopyToast(listOf("内容已复制"), articleScan = { true })

        assertEquals(2, bubble.showCount)
    }

    @Test
    fun `non article page never shows bubble`() {
        val coordinator = createCoordinator()

        coordinator.handleCopyToast(listOf("内容已复制"), articleScan = { false })

        assertEquals(0, bubble.showCount)
        assertEquals(0L, signalStore.read().lastCopyAt)
    }

    @Test
    fun `unrelated toasts do nothing`() {
        val coordinator = createCoordinator()

        coordinator.handleCopyToast(listOf("发送成功"), articleScan = { true })

        assertEquals(0, bubble.showCount)
        assertEquals(0L, signalStore.read().lastCopyAt)
    }

    @Test
    fun `disabling bubble hides it and blocks new shows`() {
        val coordinator = createCoordinator()

        coordinator.onBubbleEnabledChanged(false)
        coordinator.handleCopyToast(listOf("内容已复制"), articleScan = { true })

        assertEquals(0, bubble.showCount)
        assertTrue(bubble.hideCount >= 1)

        coordinator.onBubbleEnabledChanged(true)
        now += WeChatCaptureContract.CopyToastDebounceMillis + 1L
        coordinator.handleCopyToast(listOf("内容已复制"), articleScan = { true })

        assertEquals(1, bubble.showCount)
    }

    @Test
    fun `disabled state still records copy signals`() {
        val coordinator = createCoordinator()

        coordinator.onBubbleEnabledChanged(false)
        coordinator.handleCopyToast(listOf("内容已复制"), articleScan = { true })

        assertEquals(now, signalStore.read().lastCopyAt)
    }

    @Test
    fun `bubble tap hides bubble and launches capture`() {
        createCoordinator()

        bubble.onTapped?.invoke()

        assertEquals(1, launchCount)
        assertTrue(bubble.hideCount >= 1)
    }

    @Test
    fun `destroy tears down bubble`() {
        val coordinator = createCoordinator()

        coordinator.destroy()

        assertEquals(1, bubble.destroyCount)
    }

    @Test
    fun `window title creates wechat snapshot with empty url`() {
        val coordinator = createCoordinator()

        coordinator.handleWindowTitle(
            eventTexts = emptyList(),
            candidates = listOf(webTitleCandidate("公众号文章的完整主标题")),
            isWindowStateChange = true,
            screenHeight = 2400,
        )

        val snapshot = readWeChatSnapshot()
        assertEquals("公众号文章的完整主标题", snapshot?.sourceTitle)
        assertEquals("", snapshot?.sourceUrl)
    }

    @Test
    fun `content change fills title into url-only snapshot and keeps url`() {
        pageContextStore.mergeWeChatUrl(url = "https://mp.weixin.qq.com/s/abc", now = now)
        val coordinator = createCoordinator()

        now += 6_000L
        coordinator.handleWindowTitle(
            eventTexts = emptyList(),
            candidates = listOf(webTitleCandidate("公众号文章的完整主标题")),
            isWindowStateChange = false,
            screenHeight = 2400,
        )

        val snapshot = readWeChatSnapshot()
        assertEquals("公众号文章的完整主标题", snapshot?.sourceTitle)
        assertEquals("https://mp.weixin.qq.com/s/abc", snapshot?.sourceUrl)
    }

    @Test
    fun `window state change with new title resets stale url`() {
        val coordinator = createCoordinator()
        coordinator.handleWindowTitle(
            eventTexts = emptyList(),
            candidates = listOf(webTitleCandidate("上一篇文章的标题内容")),
            isWindowStateChange = true,
            screenHeight = 2400,
        )
        pageContextStore.mergeWeChatUrl(url = "https://mp.weixin.qq.com/s/old", now = now)

        now += 6_000L
        coordinator.handleWindowTitle(
            eventTexts = emptyList(),
            candidates = listOf(webTitleCandidate("新打开文章的另一个标题")),
            isWindowStateChange = true,
            screenHeight = 2400,
        )

        val snapshot = readWeChatSnapshot()
        assertEquals("新打开文章的另一个标题", snapshot?.sourceTitle)
        assertEquals("", snapshot?.sourceUrl)
    }

    @Test
    fun `window state change with same title keeps url`() {
        val coordinator = createCoordinator()
        coordinator.handleWindowTitle(
            eventTexts = emptyList(),
            candidates = listOf(webTitleCandidate("同一篇文章的标题内容")),
            isWindowStateChange = true,
            screenHeight = 2400,
        )
        pageContextStore.mergeWeChatUrl(url = "https://mp.weixin.qq.com/s/same", now = now)

        now += 6_000L
        coordinator.handleWindowTitle(
            eventTexts = emptyList(),
            candidates = listOf(webTitleCandidate("同一篇文章的标题内容")),
            isWindowStateChange = true,
            screenHeight = 2400,
        )

        val snapshot = readWeChatSnapshot()
        assertEquals("https://mp.weixin.qq.com/s/same", snapshot?.sourceUrl)
    }

    @Test
    fun `shorter later title does not replace longer article title`() {
        val coordinator = createCoordinator()
        coordinator.handleWindowTitle(
            eventTexts = emptyList(),
            candidates = listOf(webTitleCandidate("深度解读：大模型时代的阅读工作流全景")),
            isWindowStateChange = true,
            screenHeight = 2400,
        )

        now += 6_000L
        coordinator.handleWindowTitle(
            eventTexts = emptyList(),
            candidates = listOf(webTitleCandidate("深度解读：大模型时代")),
            isWindowStateChange = false,
            screenHeight = 2400,
        )

        assertEquals("深度解读：大模型时代的阅读工作流全景", readWeChatSnapshot()?.sourceTitle)
    }

    @Test
    fun `blank title never writes snapshot`() {
        val coordinator = createCoordinator()

        coordinator.handleWindowTitle(
            eventTexts = emptyList(),
            candidates = emptyList(),
            isWindowStateChange = true,
            screenHeight = 2400,
        )

        assertNull(readWeChatSnapshot())
    }

    private fun readWeChatSnapshot(): PageContextSnapshot? {
        return pageContextStore.readRecentSnapshot(
            expectedSourcePackage = PageContextSupport.WeChatPackage,
            now = now,
        )
    }

    private fun webTitleCandidate(text: String) = WeChatArticleTitleExtractor.TitleCandidate(
        text = text,
        viewId = "",
        className = "android.view.View",
        boundsTop = 400,
        inWebContainer = true,
    )

    private class RecordingBubble : WeChatCaptureBubble {
        var onTapped: (() -> Unit)? = null
        var showCount = 0
        var hideCount = 0
        var destroyCount = 0

        override fun show() {
            showCount += 1
        }

        override fun hide() {
            hideCount += 1
        }

        override fun destroy() {
            destroyCount += 1
        }
    }
}
