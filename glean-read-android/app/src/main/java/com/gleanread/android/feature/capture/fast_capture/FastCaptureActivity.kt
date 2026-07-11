package com.gleanread.android.feature.capture.fast_capture

import android.content.ClipboardManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.SideEffect
import com.gleanread.android.platform.page_context.CaptureSeed
import com.gleanread.android.platform.page_context.CaptureSeedResolver
import com.gleanread.android.platform.page_context.PageContextStore
import com.gleanread.android.platform.page_context.wechat.WeChatCaptureContract
import com.gleanread.android.platform.page_context.wechat.WeChatCaptureSignalStore
import com.gleanread.android.platform.page_context.wechat.WeChatClipboardResolver
import com.gleanread.android.core.ui.theme.GleanReadTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.gleanread.android.app.appContainer
import com.gleanread.android.data.appearance.ThemeMode
import com.gleanread.android.data.appearance.ThemeColor

class FastCaptureActivity : ComponentActivity() {
    private lateinit var pageContextStore: PageContextStore

    /**
     * 两阶段 seed：onCreate 先用 Intent + 缓存组装初始值；
     * 微信气泡入口在首次获得窗口焦点后读剪贴板补正文/URL（Android 10+ 无焦点读不到剪贴板）。
     */
    private var captureSeedState by mutableStateOf<CaptureSeed?>(null)
    private var hasAppliedClipboardSeed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        pageContextStore = PageContextStore(applicationContext)
        captureSeedState = CaptureSeedResolver(
            pageContextStore = pageContextStore,
        ).resolve(
            intent = intent,
            referrer = referrer,
        )

        setContent {
            SideEffect {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    window.setBackgroundBlurRadius(50)
                }
            }
            val themeMode by appContainer.appearancePreferencesRepository.themeModeFlow.collectAsState(initial = ThemeMode.SYSTEM)
            val themeColor by appContainer.appearancePreferencesRepository.themeColorFlow.collectAsState(initial = ThemeColor.DYNAMIC)
            GleanReadTheme(themeMode = themeMode, themeColor = themeColor) {
                captureSeedState?.let { captureSeed ->
                    FastCaptureRoute(
                        captureSeed = captureSeed,
                        onDismiss = ::finish,
                    )
                }
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (!hasFocus || hasAppliedClipboardSeed) return
        // 仅微信气泡入口读剪贴板；分享 / 文本处理入口保持原有行为
        if (intent?.action != WeChatCaptureContract.ActionWeChatCapture) return
        hasAppliedClipboardSeed = true

        val currentSeed = captureSeedState ?: return
        val now = System.currentTimeMillis()
        val updatedSeed = WeChatClipboardResolver.applyToSeed(
            seed = currentSeed,
            clip = readPrimaryClip(),
            signals = WeChatCaptureSignalStore(applicationContext).read(),
            now = now,
        ) ?: return

        // 识别到公众号链接时回写快照，同一文章的后续摘录可复用
        if (updatedSeed.url.isNotBlank() && updatedSeed.url != currentSeed.url) {
            pageContextStore.mergeWeChatUrl(url = updatedSeed.url, now = now)
        }
        captureSeedState = updatedSeed
    }

    private fun readPrimaryClip(): WeChatClipboardResolver.ClipInput? {
        val clipboardManager = getSystemService(ClipboardManager::class.java) ?: return null
        val clip = clipboardManager.primaryClip ?: return null
        if (clip.itemCount <= 0) return null
        val text = clip.getItemAt(0)?.coerceToText(this)?.toString().orEmpty()
        return WeChatClipboardResolver.ClipInput(
            text = text,
            timestampMillis = clip.description?.timestamp ?: 0L,
        )
    }
}
