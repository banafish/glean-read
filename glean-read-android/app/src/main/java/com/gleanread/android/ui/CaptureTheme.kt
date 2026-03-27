package com.gleanread.android.ui

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.BlurEffect

/**
 * Capture V2 UI 主题定义
 * 包含毛玻璃色彩库、渐变色资产以及通用的毛玻璃基础修饰符
 */
object CaptureUI {
    // 基础色调 - 沉浸式深空蓝
    val DeepSpaceBlue = Color(0xFF1A1A2E)
    val AuroraPurple = Color(0xFF700B97)
    
    // 玻璃质感基础色 - 极高通透度的白/黑
    val GlassyWhiteLong = Color(0x33FFFFFF) // 20% 透明度白
    val GlassyWhiteMedium = Color(0x66FFFFFF) // 40% 透明度白
    val GlassyBlackLow = Color(0x99000000) // 60% 透明度黑
    
    // 品牌渐变
    val PrimaryGradient = Brush.linearGradient(
        colors = listOf(Color(0xFF16213E), Color(0xFF0F3460), AuroraPurple)
    )

    /**
     * 通用的毛玻璃效果修饰符
     * @param blur 模糊半径 (默认为 30f)
     * @param alpha 背景白色叠加层的不透明度 (默认为 0.4f)
     */
    fun Modifier.glassyBackground(
        alpha: Float = 0.6f
    ): Modifier = this.then(
        // 移除 RenderEffect，因为它会模糊容器内的所有内容（包括文字）
        Modifier.background(Color.White.copy(alpha = alpha))
    )
}
