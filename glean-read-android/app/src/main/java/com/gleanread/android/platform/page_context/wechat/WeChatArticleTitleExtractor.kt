package com.gleanread.android.platform.page_context.wechat

import com.gleanread.android.platform.page_context.PageContextTextRules

/**
 * 公众号文章标题提取器（打分制）。
 *
 * 与旧的通用提取器不同：
 * - 公众号标题只会渲染在网页内容里，网页容器内节点强加权；
 * - 用相对屏高替代绝对像素上限（旧 0..520px 在高分屏上会漏掉标题）；
 * - 压低原生顶栏节点（文章未滚动时顶栏是公众号名而非文章标题）。
 */
object WeChatArticleTitleExtractor {
    data class TitleCandidate(
        val text: String,
        val viewId: String,
        val className: String,
        val boundsTop: Int,
        val inWebContainer: Boolean,
    )

    private const val MIN_TITLE_LENGTH = 4
    private const val MAX_TITLE_LENGTH = 100

    /** 低于该分的候选视为噪音，回退到事件文本 */
    private const val MIN_ACCEPT_SCORE = 60

    private val navigationLabels = setOf(
        "返回",
        "关闭",
        "更多",
        "首页",
        "菜单",
        "发现",
        "通讯录",
        "微信",
        "朋友圈",
        "在看",
        "赞",
        "分享",
        "back",
        "close",
        "more",
        "menu",
        "wechat",
    )

    fun extract(
        eventTexts: List<String>,
        candidates: List<TitleCandidate>,
        screenHeight: Int,
    ): String {
        val bestCandidate = candidates
            .asSequence()
            .filter { isTitleShapedText(it.text) }
            .map { it to score(it, screenHeight) }
            .filter { (_, score) -> score >= MIN_ACCEPT_SCORE }
            .maxByOrNull { (_, score) -> score }
            ?.first
        if (bestCandidate != null) {
            return bestCandidate.text.trim()
        }

        return eventTexts
            .map(String::trim)
            .firstOrNull(::isTitleShapedText)
            .orEmpty()
    }

    private fun score(
        candidate: TitleCandidate,
        screenHeight: Int,
    ): Int {
        val safeScreenHeight = screenHeight.coerceAtLeast(1)
        val relativeTop = candidate.boundsTop.toFloat() / safeScreenHeight
        val text = candidate.text.trim()
        val normalizedId = candidate.viewId.lowercase()

        var score = 0
        // 公众号标题（网页 h1）一定在网页容器内
        if (candidate.inWebContainer) score += 120
        // 相对位置：标题通常位于屏幕上半部
        if (relativeTop in 0f..0.45f) {
            score += 40
        } else if (relativeTop in 0f..0.7f) {
            score += 20
        }
        // 原生顶栏（非网页容器且贴近屏幕顶部）通常是公众号名/导航，压低
        if (!candidate.inWebContainer && relativeTop < 0.08f) score -= 40
        // 文本形态
        when (text.length) {
            in 8..64 -> score += 30
            in 5..96 -> score += 15
        }
        if (text.any(::isCjkChar)) score += 10
        if (containsTitleSeparator(text)) score += 8
        if (normalizedId.contains("url") || normalizedId.contains("address")) score -= 80
        return score
    }

    private fun isTitleShapedText(text: String): Boolean {
        val trimmed = text.trim()
        if (trimmed.length !in MIN_TITLE_LENGTH..MAX_TITLE_LENGTH) return false
        if (!PageContextTextRules.isMeaningfulTitle(trimmed)) return false
        if (trimmed.lowercase() in navigationLabels) return false
        if (looksLikeUrl(trimmed)) return false
        return true
    }

    private fun looksLikeUrl(text: String): Boolean {
        if (text.startsWith("http://") || text.startsWith("https://")) return true
        if (text.contains("://")) return true
        // 无空格且带路径分隔的 host 形态（如 mp.weixin.qq.com/s/xxx）
        return !text.contains(' ') && text.contains('.') && text.contains('/')
    }

    private fun containsTitleSeparator(text: String): Boolean {
        return text.any { it in "：:｜|丨（(－-," || it == ' ' }
    }

    private fun isCjkChar(char: Char): Boolean {
        return char.code in 0x4E00..0x9FFF
    }
}
