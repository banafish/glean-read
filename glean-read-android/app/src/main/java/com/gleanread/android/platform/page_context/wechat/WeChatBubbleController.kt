package com.gleanread.android.platform.page_context.wechat

import android.accessibilityservice.AccessibilityService
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.gleanread.android.R
import com.gleanread.android.core.ui.theme.GleanReadTheme

/** 气泡能力抽象，协调器通过它编排显示/隐藏，便于单测注入 */
interface WeChatCaptureBubble {
    fun show()
    fun hide()
    fun destroy()
}

/**
 * 微信摘录悬浮气泡控制器。
 * 使用 TYPE_ACCESSIBILITY_OVERLAY（无障碍服务专属层，无需悬浮窗权限）+
 * FLAG_NOT_FOCUSABLE（微信保持焦点，剪贴板读取留给获焦的摘录弹窗）。
 * 所有方法都假定在主线程调用（无障碍回调与开关 Flow 均在主线程）。
 */
class WeChatBubbleController(
    private val service: AccessibilityService,
    private val onBubbleTapped: () -> Unit,
) : WeChatCaptureBubble {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val autoDismissRunnable = Runnable { hide() }
    private var overlayView: ComposeView? = null
    private var overlayOwner: OverlayLifecycleOwner? = null

    override fun show() {
        // 已在显示中：只重置自动消失计时，避免重复 addView 崩溃或视图叠加
        if (overlayView != null) {
            WeChatCaptureDx.log { "bubble show: already visible, reset auto-dismiss" }
            scheduleAutoDismiss()
            return
        }

        val windowManager = service.getSystemService(WindowManager::class.java) ?: return
        val owner = OverlayLifecycleOwner()
        val view = ComposeView(service).apply {
            setContent {
                GleanReadTheme {
                    WeChatBubbleContent(onClick = onBubbleTapped)
                }
            }
        }
        owner.attachTo(view)

        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
        }

        // 个别 ROM 对 overlay 有额外限制，失败时静默放弃本次显示（分享路径不受影响）
        runCatching {
            windowManager.addView(view, layoutParams)
        }.onFailure { error ->
            WeChatCaptureDx.log { "bubble addView failed: ${error.javaClass.simpleName}: ${error.message}" }
            owner.destroy()
            return
        }

        WeChatCaptureDx.log { "bubble addView ok" }
        overlayView = view
        overlayOwner = owner
        owner.moveToResumed()
        scheduleAutoDismiss()
    }

    override fun hide() {
        mainHandler.removeCallbacks(autoDismissRunnable)
        val view = overlayView ?: return
        overlayView = null

        overlayOwner?.destroy()
        overlayOwner = null

        val windowManager = service.getSystemService(WindowManager::class.java)
        runCatching { windowManager?.removeView(view) }
    }

    override fun destroy() {
        hide()
    }

    private fun scheduleAutoDismiss() {
        mainHandler.removeCallbacks(autoDismissRunnable)
        mainHandler.postDelayed(autoDismissRunnable, WeChatCaptureContract.BubbleAutoDismissMillis)
    }
}

@Composable
private fun WeChatBubbleContent(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        // 贴右缘的半胶囊造型：左侧圆角、右侧平边
        shape = RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        tonalElevation = 3.dp,
        shadowElevation = 6.dp,
    ) {
        Row(
            modifier = Modifier.padding(start = 14.dp, end = 12.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.AutoAwesome,
                contentDescription = stringResource(R.string.wechat_capture_bubble_content_description),
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = stringResource(R.string.wechat_capture_bubble_label),
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}
