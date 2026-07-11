package com.gleanread.android.platform.page_context.wechat

import org.junit.Assert.assertEquals
import org.junit.Test

class WeChatClickClassifierTest {
    @Test
    fun `simplified chinese copy is classified as copy text`() {
        assertEquals(
            WeChatCopyClickKind.COPY_TEXT,
            WeChatClickClassifier.classify(listOf("复制")),
        )
    }

    @Test
    fun `traditional chinese and english copy are classified as copy text`() {
        assertEquals(
            WeChatCopyClickKind.COPY_TEXT,
            WeChatClickClassifier.classify(listOf("複製")),
        )
        assertEquals(
            WeChatCopyClickKind.COPY_TEXT,
            WeChatClickClassifier.classify(listOf("Copy")),
        )
    }

    @Test
    fun `copy link variants are classified as copy link`() {
        assertEquals(
            WeChatCopyClickKind.COPY_LINK,
            WeChatClickClassifier.classify(listOf("复制链接")),
        )
        assertEquals(
            WeChatCopyClickKind.COPY_LINK,
            WeChatClickClassifier.classify(listOf("複製連結")),
        )
        assertEquals(
            WeChatCopyClickKind.COPY_LINK,
            WeChatClickClassifier.classify(listOf("Copy Link")),
        )
    }

    @Test
    fun `copy link wins when both labels are present`() {
        assertEquals(
            WeChatCopyClickKind.COPY_LINK,
            WeChatClickClassifier.classify(listOf("复制", "复制链接")),
        )
    }

    @Test
    fun `labels are matched exactly not by contains`() {
        assertEquals(
            WeChatCopyClickKind.NONE,
            WeChatClickClassifier.classify(listOf("复制链接失败")),
        )
        assertEquals(
            WeChatCopyClickKind.NONE,
            WeChatClickClassifier.classify(listOf("已复制")),
        )
    }

    @Test
    fun `whitespace around labels is tolerated`() {
        assertEquals(
            WeChatCopyClickKind.COPY_TEXT,
            WeChatClickClassifier.classify(listOf(" 复制 ")),
        )
    }

    @Test
    fun `unrelated labels and empty input return none`() {
        assertEquals(
            WeChatCopyClickKind.NONE,
            WeChatClickClassifier.classify(listOf("全选", "搜一搜")),
        )
        assertEquals(
            WeChatCopyClickKind.NONE,
            WeChatClickClassifier.classify(emptyList()),
        )
    }
}
