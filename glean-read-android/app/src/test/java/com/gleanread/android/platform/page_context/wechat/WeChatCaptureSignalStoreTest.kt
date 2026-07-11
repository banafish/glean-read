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
        assertEquals(0L, signalStore.read().lastCopyAt)
    }

    @Test
    fun `copy signal timestamp is stored and overwritten`() {
        signalStore.markCopyObserved(at = 1_000L)
        assertEquals(1_000L, signalStore.read().lastCopyAt)

        signalStore.markCopyObserved(at = 2_000L)
        assertEquals(2_000L, signalStore.read().lastCopyAt)
    }

    @Test
    fun `clear resets all signals`() {
        signalStore.markCopyObserved(at = 1_000L)

        signalStore.clear()

        assertEquals(0L, signalStore.read().lastCopyAt)
    }
}
