package com.gleanread.android.platform.page_context.wechat

/** 微信复制类按钮点击的分类结果 */
enum class WeChatCopyClickKind {
    COPY_TEXT,
    COPY_LINK,
    NONE,
}

/**
 * 识别微信自绘菜单中的「复制」「复制链接」按钮点击。
 * 词表采用归一化后的精确匹配（非 contains），避免误命中
 * 「复制链接失败」「已复制」等 toast / 提示文案。
 */
object WeChatClickClassifier {
    // 支持简中 / 繁中 / 英文界面；微信改版新增文案时在此追加
    private val copyTextLabels = setOf("复制", "複製", "copy")
    private val copyLinkLabels = setOf("复制链接", "複製鏈接", "複製連結", "copy link")

    /**
     * @param candidateTexts 事件与源节点携带的候选文案
     *（event.text + source.text + source.contentDescription）
     */
    fun classify(candidateTexts: List<String>): WeChatCopyClickKind {
        val normalized = candidateTexts
            .map { it.trim().lowercase() }
            .filter { it.isNotEmpty() }
        if (normalized.isEmpty()) return WeChatCopyClickKind.NONE

        // 「复制链接」更长更特异，必须先于「复制」判定
        if (normalized.any { it in copyLinkLabels }) return WeChatCopyClickKind.COPY_LINK
        if (normalized.any { it in copyTextLabels }) return WeChatCopyClickKind.COPY_TEXT
        return WeChatCopyClickKind.NONE
    }
}
