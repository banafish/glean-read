package com.gleanread.android.platform.page_context

import android.content.Intent

/**
 * 浏览器划词摘录 deep link 的契约常量。
 * userscript 在页面里组装 `gleanread://capture?url=<enc>&title=<enc>&text=<enc>`
 * （或 intent URI 包装形式）直达 FastCaptureActivity，App 侧只解析 data URI 的
 * query 部分，一套逻辑通吃两种触达形式。
 */
object WebCaptureContract {
    /** deep link 自定义 scheme，与 MainActivity 的 auth 回调共用 */
    const val Scheme = "gleanread"

    /** 划词摘录专用 host，与 `gleanread://auth/callback` 以 host 区分 */
    const val HostCapture = "capture"

    /** query 参数：来源页面 URL（缺失时回落到通用 intent 解析逻辑） */
    const val ParamUrl = "url"

    /** query 参数：来源页面标题，可空 */
    const val ParamTitle = "title"

    /** query 参数：选中文本，可空（脚本侧已截断至 6000 字符） */
    const val ParamText = "text"

    /** 判定是否为浏览器划词摘录 deep link（scheme + host 双匹配） */
    fun isWebCaptureIntent(intent: Intent?): Boolean {
        val data = intent?.data ?: return false
        return data.scheme == Scheme && data.host == HostCapture
    }
}
