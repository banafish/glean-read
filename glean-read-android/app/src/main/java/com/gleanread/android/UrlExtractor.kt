package com.gleanread.android

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log

/**
 * URL 提取策略链
 *
 * 按优先级依次尝试从系统分享 Intent 中提取原始文章 URL，
 * 覆盖浏览器「整页分享」与「选中文字分享」两种主要场景，
 * 以及微信公众号内置浏览器的常见分享格式。
 *
 * 所有提取逻辑均在本地完成，不发起任何网络请求。
 */
object UrlExtractor {

    private const val TAG = "UrlExtractor"

    /**
     * 用于从文本中提取 URL 的正则表达式。
     * - 匹配 http/https 开头的 URL
     * - 排除空白字符、中文字符（避免误匹配中文内容）、引号和尖括号
     */
    private val URL_REGEX = Regex("""https?://[^\s\u4e00-\u9fff"'<>]+""")

    /**
     * 从 Intent 中提取 URL。
     *
     * 策略链（按优先级）：
     * 1. EXTRA_TEXT 整体即为合法 URL（Chrome 整页分享标准路径）
     * 2. EXTRA_SUBJECT 整体即为合法 URL（少数浏览器用 SUBJECT 存 URL）
     * 3. EXTRA_STREAM Uri 转字符串后为合法 URL
     * 4. Intent.clipData 中提取 URL（Android 10+ Chrome 选中文字分享常用此方式）
     * 5. 正则从 EXTRA_TEXT 末尾提取 URL（文字后追加链接的格式）
     * 6. 正则从 EXTRA_SUBJECT 中提取 URL
     * 7. 正则从 EXTRA_TEXT 任意位置提取第一个合法 URL
     * 8. 全部失败 → 返回 null（调用方负责显示降级 UI）
     *
     * @return 提取到的合法 URL 字符串，或 null（无法提取）
     */
    fun extract(intent: Intent): String? {
        val text = intent.getStringExtra(Intent.EXTRA_TEXT)?.trim() ?: ""
        val subject = intent.getStringExtra(Intent.EXTRA_SUBJECT)?.trim() ?: ""
        val stream = getUriFromIntent(intent)

        // 调试日志，方便排查问题（发布时可移除）
        Log.d(TAG, "=== UrlExtractor.extract ===")
        Log.d(TAG, "EXTRA_TEXT: ${text.take(200)}")
        Log.d(TAG, "EXTRA_SUBJECT: $subject")
        Log.d(TAG, "EXTRA_STREAM: $stream")
        Log.d(TAG, "clipData itemCount: ${intent.clipData?.itemCount ?: 0}")
        intent.clipData?.let { cd ->
            for (i in 0 until cd.itemCount) {
                val item = cd.getItemAt(i)
                Log.d(TAG, "  clipData[$i] uri=${item.uri}, text=${item.text?.take(100)}")
            }
        }

        // 策略 1：EXTRA_TEXT 整体即为合法 URL
        if (text.isValidUrl()) {
            Log.d(TAG, "命中策略1: EXTRA_TEXT 整体为 URL")
            return text
        }

        // 策略 2：EXTRA_SUBJECT 整体即为合法 URL
        if (subject.isValidUrl()) {
            Log.d(TAG, "命中策略2: EXTRA_SUBJECT 整体为 URL")
            return subject
        }

        // 策略 3：EXTRA_STREAM Uri 转字符串后为合法 URL
        stream?.toString()?.trim()?.takeIf { it.isValidUrl() }?.let {
            Log.d(TAG, "命中策略3: EXTRA_STREAM 为 URL: $it")
            return it
        }

        // 策略 4：从 Intent.clipData 中提取 URL
        // Chrome 在 Android 10+ 上选中文字分享「with link」时，会将页面 URL
        // 作为 ClipData 的第一个条目 Uri 传递给目标 App
        extractFromClipData(intent)?.let {
            Log.d(TAG, "命中策略4: clipData 中提取到 URL: $it")
            return it
        }

        // 策略 5：正则从 EXTRA_TEXT 末尾提取 URL
        // 适用：Chrome/微信在所选文字末尾追加了原文链接
        if (text.isNotEmpty()) {
            URL_REGEX.findAll(text).lastOrNull()?.value?.let { candidateUrl ->
                val trimmedText = text.trimEnd()
                // 末尾判断：URL 必须出现在文本末尾（含末尾标点/斜杠容差）
                if (trimmedText.endsWith(candidateUrl) ||
                    trimmedText.endsWith(candidateUrl.trimEnd('/', '.'))
                ) {
                    Log.d(TAG, "命中策略5: EXTRA_TEXT 末尾提取到 URL: $candidateUrl")
                    return candidateUrl
                }
            }

            // 策略 7：正则从 EXTRA_TEXT 任意位置提取第一个合法 URL
            URL_REGEX.find(text)?.value?.let {
                Log.d(TAG, "命中策略7: EXTRA_TEXT 任意位置提取到 URL: $it")
                return it
            }
        }

        // 策略 6：正则从 EXTRA_SUBJECT 中提取 URL
        if (subject.isNotEmpty()) {
            URL_REGEX.find(subject)?.value?.let {
                Log.d(TAG, "命中策略6: EXTRA_SUBJECT 中提取到 URL: $it")
                return it
            }
        }

        Log.d(TAG, "所有策略均失败，返回 null")
        return null
    }

    /**
     * 从 Intent.clipData 中提取合法 URL。
     * Chrome Android 在选中文字「including link」分享时，
     * 会将页面 URL 作为 ClipData 条目的 uri 字段传递。
     */
    private fun extractFromClipData(intent: Intent): String? {
        val clipData = intent.clipData ?: return null
        for (i in 0 until clipData.itemCount) {
            val item = clipData.getItemAt(i)
            // 优先检查 ClipData 条目的 Uri（通常是目标页面 URL）
            item.uri?.toString()?.trim()?.takeIf { it.isValidUrl() }?.let { return it }
            // 其次检查 ClipData 条目的文字内容（可能是纯 URL 字符串）
            item.text?.toString()?.trim()?.takeIf { it.isValidUrl() }?.let { return it }
        }
        return null
    }

    /**
     * 兼容性获取 Intent 中的 EXTRA_STREAM Uri（Android 13+ API 变更）
     */
    @Suppress("DEPRECATION")
    private fun getUriFromIntent(intent: Intent): Uri? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
            } else {
                intent.getParcelableExtra(Intent.EXTRA_STREAM)
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 校验字符串是否为合法 URL（以 http:// 或 https:// 开头）
     */
    private fun String.isValidUrl(): Boolean {
        return startsWith("http://") || startsWith("https://")
    }
}
