package com.gleanread.android.app

import android.app.Application
import android.content.Context
import com.gleanread.android.BuildConfig
import com.gleanread.android.app.di.AppContainer
import com.gleanread.android.app.sync.WorkspaceSyncWorker
import com.gleanread.android.platform.page_context.wechat.WeChatCaptureDx
import com.gleanread.android.platform.page_context.wechat.WeChatCaptureDxFileSink
import kotlinx.coroutines.launch

class GleanReadApplication : Application() {
    val appContainer: AppContainer by lazy { AppContainer(this) }

    override fun onCreate() {
        super.onCreate()
        // 微信摘录链路诊断日志仅 debug 构建启用（服务与弹窗同进程，进程级注入一次即可）。
        // 文件落盘 + logcat 双写：部分 ROM 加密 logcat，文件是唯一可靠取证通道
        if (BuildConfig.DEBUG) {
            WeChatCaptureDx.sink = WeChatCaptureDxFileSink(this)
        }
        appContainer.restoreDatabaseFromSession()
        appContainer.applicationScope.launch {
            appContainer.databaseManager.deleteExpiredDatabases()
        }
        runCatching {
            WorkspaceSyncWorker.schedule(this)
        }
    }
}

val Context.appContainer: AppContainer
    get() = (applicationContext as GleanReadApplication).appContainer
