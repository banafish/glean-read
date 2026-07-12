package com.gleanread.android.platform.page_context.wechat

import android.content.Context
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

/**
 * 诊断日志的文件落盘 sink（仅 debug 构建注入，见 GleanReadApplication）。
 *
 * 背景：部分 ROM（如 Honor/Huawei 新版系统）对 logcat 内容整行加密（HKS/HKE 包裹），
 * tag 过滤失效，无法用 adb logcat 取证；故同时落盘到应用私有目录，
 * debug 包可通过 run-as 拉取：
 *   adb exec-out run-as com.gleanread.android cat files/wechat_capture_dx.log
 *
 * 时间戳在调用线程即刻生成（全部调用点都在主线程），落盘异步执行不阻塞无障碍回调；
 * 若进程曾被 ROM 冻结，积压事件解冻后集中落盘，表现为「写入时刻聚集 + queuedMs 巨大」，
 * 这正是冻结假设的直接证据形态。
 */
class WeChatCaptureDxFileSink(context: Context) : (String) -> Unit {
    private val logFile = File(context.applicationContext.filesDir, FILE_NAME)
    private val executor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "wechat-dx-log").apply { isDaemon = true }
    }

    // 仅主线程调用 invoke，SimpleDateFormat 无并发问题
    private val timeFormat = SimpleDateFormat("MM-dd HH:mm:ss.SSS", Locale.US)

    init {
        executor.execute {
            runCatching {
                // 防膨胀：超限直接重开（诊断场景无需保留历史）
                if (logFile.length() > MAX_BYTES) logFile.delete()
            }
        }
        // 进程启动标记：若日志中出现多次，说明进程被杀重启而非单纯冻结
        invoke("=== dx sink init (process start) ===")
    }

    override fun invoke(message: String) {
        Log.d(TAG, message)
        val line = "${timeFormat.format(Date())} $message\n"
        executor.execute {
            runCatching { logFile.appendText(line) }
        }
    }

    private companion object {
        const val TAG = "WeChatCaptureDx"
        const val FILE_NAME = "wechat_capture_dx.log"
        const val MAX_BYTES = 1_000_000L
    }
}
