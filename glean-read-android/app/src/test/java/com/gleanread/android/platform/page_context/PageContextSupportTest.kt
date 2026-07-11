package com.gleanread.android.platform.page_context

import org.junit.Assert.assertEquals
import org.junit.Test

class PageContextSupportTest {
    @Test
    fun `wechat snapshots use extended ttl`() {
        assertEquals(
            600_000L,
            PageContextSupport.cacheTtlMillisFor(PageContextSupport.WeChatPackage),
        )
    }

    @Test
    fun `browser snapshots keep default ttl`() {
        assertEquals(
            PageContextSupport.CacheTtlMillis,
            PageContextSupport.cacheTtlMillisFor(PageContextSupport.ChromePackage),
        )
        assertEquals(
            PageContextSupport.CacheTtlMillis,
            PageContextSupport.cacheTtlMillisFor("unknown.package"),
        )
    }
}
