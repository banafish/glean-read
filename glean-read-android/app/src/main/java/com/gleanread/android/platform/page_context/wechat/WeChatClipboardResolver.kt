package com.gleanread.android.platform.page_context.wechat

import com.gleanread.android.platform.page_context.CaptureSeed
import kotlin.math.abs

/**
 * 微信摘录入口的剪贴板解析：
 * - 新鲜度校验：剪贴板内容必须对应无障碍服务记录的最近一次复制点击，
 *   防止把陈旧剪贴板误填进摘录；
 * - 判别：公众号文章链接填 URL（正文不动），普通文本填正文。
 * 纯逻辑，剪贴板的实际读取由获焦的 Activity 完成。
 */
object WeChatClipboardResolver {
    /** 剪贴板输入：文本 + 系统时间戳（部分 ROM 恒为 0） */
    data class ClipInput(
        val text: String,
        val timestampMillis: Long,
    )

    private val weChatArticleUrlRegex = Regex("""https?://mp\.weixin\.qq\.com/\S+""")

    /**
     * 把剪贴板内容套用到当前 seed。
     * @return 更新后的 seed；剪贴板不可用/不新鲜/无变化时返回 null
     */
    fun applyToSeed(
        seed: CaptureSeed,
        clip: ClipInput?,
        signals: WeChatCaptureSignals,
        now: Long,
    ): CaptureSeed? {
        val safeClip = clip ?: return null
        val text = safeClip.text.trim()
        if (text.isEmpty()) return null
        if (!isFresh(safeClip, signals, now)) return null

        val articleUrl = weChatArticleUrlRegex.find(text)?.value
        return if (articleUrl != null) {
            // 「复制链接」路径：剪贴板承载的是链接，正文保持等待用户后续选择
            seed.copy(url = articleUrl, usedCachedUrl = false)
                .takeIf { it.url != seed.url || seed.usedCachedUrl }
        } else {
            seed.copy(content = text).takeIf { it.content != seed.content }
        }
    }

    private fun isFresh(
        clip: ClipInput,
        signals: WeChatCaptureSignals,
        now: Long,
    ): Boolean {
        val latestSignal = signals.latestCopyAt
        // 没有复制信号说明剪贴板内容并非来自微信内的复制动作
        if (latestSignal <= 0L) return false

        return if (clip.timestampMillis > 0L) {
            abs(clip.timestampMillis - latestSignal) <= WeChatCaptureContract.ClipTimestampToleranceMillis
        } else {
            // ROM 未提供剪贴板时间戳时，以复制信号的兜底时间窗判定
            now - latestSignal <= WeChatCaptureContract.ClipFallbackWindowMillis
        }
    }
}
