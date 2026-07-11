package com.gleanread.android.platform.page_context.wechat

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WeChatCopyToastClassifierTest {
    @Test
    fun `recognizes the observed copy success toast`() {
        // 真机实测微信复制正文后弹出的 toast 文案
        assertTrue(WeChatCopyToastClassifier.isCopySuccess(listOf("内容已复制", "微信")))
    }

    @Test
    fun `recognizes simplified and traditional copy markers`() {
        assertTrue(WeChatCopyToastClassifier.isCopySuccess(listOf("已复制")))
        assertTrue(WeChatCopyToastClassifier.isCopySuccess(listOf("已复制链接")))
        assertTrue(WeChatCopyToastClassifier.isCopySuccess(listOf("已複製")))
        assertTrue(WeChatCopyToastClassifier.isCopySuccess(listOf("复制成功")))
    }

    @Test
    fun `recognizes english copy markers case insensitively`() {
        assertTrue(WeChatCopyToastClassifier.isCopySuccess(listOf("Copied")))
        assertTrue(WeChatCopyToastClassifier.isCopySuccess(listOf("Link copied to clipboard")))
    }

    @Test
    fun `rejects copy failure toasts`() {
        assertFalse(WeChatCopyToastClassifier.isCopySuccess(listOf("复制失败")))
        assertFalse(WeChatCopyToastClassifier.isCopySuccess(listOf("复制链接失败")))
    }

    @Test
    fun `rejects unrelated or empty toasts`() {
        assertFalse(WeChatCopyToastClassifier.isCopySuccess(listOf("发送成功")))
        assertFalse(WeChatCopyToastClassifier.isCopySuccess(listOf("   ")))
        assertFalse(WeChatCopyToastClassifier.isCopySuccess(emptyList()))
    }
}
