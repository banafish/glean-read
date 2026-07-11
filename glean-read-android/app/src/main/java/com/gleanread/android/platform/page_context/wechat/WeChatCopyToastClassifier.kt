package com.gleanread.android.platform.page_context.wechat

/**
 * 识别微信「复制成功」toast（TYPE_NOTIFICATION_STATE_CHANGED）。
 *
 * 真机证据（Honor / 微信 8.x）：文章页自绘菜单「复制」的 TYPE_VIEW_CLICKED 事件
 * 完全不携带文本（text/desc/源节点均为空），按钮级识别结构性不可行；
 * 但复制成功后微信必弹「内容已复制」toast，故以 toast 文案为触发信号。
 *
 * 匹配规则：先排除失败词，再包含式命中成功词；覆盖简中/繁中/英文。
 * 微信改版新增文案时在此追加。
 */
object WeChatCopyToastClassifier {
    // 成功词：内容已复制 / 已复制链接 / 已複製 / 复制成功 / copied to clipboard
    private val successMarkers = listOf("已复制", "已複製", "复制成功", "複製成功", "copied")

    // 失败词：复制失败 / 複製失敗 / copy failed 一律不触发
    private val failureMarkers = listOf("失败", "失敗", "failed")

    /** @param toastTexts toast 事件的 text 列表（可能附带应用名等无关项） */
    fun isCopySuccess(toastTexts: List<String>): Boolean {
        return toastTexts.any { text ->
            val normalized = text.trim().lowercase()
            normalized.isNotEmpty() &&
                failureMarkers.none(normalized::contains) &&
                successMarkers.any(normalized::contains)
        }
    }
}
