package com.gleanread.android.platform.page_context.wechat

/**
 * 微信「复制即摘」链路的契约常量。
 * 集中维护 action 与各时间阈值，方便微信改版后统一调整。
 */
object WeChatCaptureContract {
    /** 气泡点击后拉起摘录弹窗的专用 action */
    const val ActionWeChatCapture = "com.gleanread.android.action.WECHAT_CAPTURE"

    /** 微信一次点击可能派发多条无障碍事件，800ms 内视为同一次点击 */
    const val ClickDebounceMillis = 800L

    /** 气泡无交互时的自动消失时长 */
    const val BubbleAutoDismissMillis = 8_000L

    /** 公众号文章页判定结果按窗口缓存的时长 */
    const val ArticleDetectCacheMillis = 30_000L

    /** 剪贴板时间戳与复制点击信号的允许偏差 */
    const val ClipTimestampToleranceMillis = 15_000L

    /** 部分 ROM 剪贴板时间戳恒为 0 时，退化使用的复制信号兜底时间窗 */
    const val ClipFallbackWindowMillis = 180_000L
}
