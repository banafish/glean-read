package com.gleanread.android.platform.page_context.wechat

import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WeChatCaptureSignalStoreTest {
    private lateinit var signalStore: WeChatCaptureSignalStore

    @Before
    fun setUp() {
        signalStore = WeChatCaptureSignalStore(ApplicationProvider.getApplicationContext())
        signalStore.clear()
    }

    @Test
    fun `initial signals are empty`() {
        val signals = signalStore.read()

        assertEquals(0L, signals.lastCopyTextAt)
        assertEquals(0L, signals.lastCopyLinkAt)
        assertEquals(0L, signals.latestCopyAt)
    }

    @Test
    fun `copy text and copy link signals are stored independently`() {
        signalStore.markCopyTextClicked(at = 1_000L)
        signalStore.markCopyLinkClicked(at = 2_000L)

        val signals = signalStore.read()

        assertEquals(1_000L, signals.lastCopyTextAt)
        assertEquals(2_000L, signals.lastCopyLinkAt)
        assertEquals(2_000L, signals.latestCopyAt)
    }

    @Test
    fun `clear resets all signals`() {
        signalStore.markCopyTextClicked(at = 1_000L)
        signalStore.markCopyLinkClicked(at = 2_000L)

        signalStore.clear()

        assertEquals(0L, signalStore.read().latestCopyAt)
    }
}
