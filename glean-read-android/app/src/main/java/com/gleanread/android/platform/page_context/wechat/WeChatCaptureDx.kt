package com.gleanread.android.platform.page_context.wechat

/**
 * 微信摘录链路的诊断日志开关（排查真机差异用）。
 *
 * 设计约束：
 * - 纯逻辑类（协调器/分类器）会在 JVM 单测中执行，不能直接依赖 android.util.Log，
 *   故用可注入 sink：服务在 debug 构建下注入 Log 实现，单测与 release 下为 null 空操作；
 * - message 用 lambda 延迟构造，sink 为 null 时零字符串拼接开销。
 *
 * 采集方式：adb logcat -s WeChatCaptureDx
 */
object WeChatCaptureDx {
    @Volatile
    var sink: ((String) -> Unit)? = null

    inline fun log(message: () -> String) {
        sink?.invoke(message())
    }
}
