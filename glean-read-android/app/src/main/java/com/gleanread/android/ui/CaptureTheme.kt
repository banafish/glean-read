package com.gleanread.android.ui

import androidx.compose.foundation.background
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Capture V2 UI 主题定义
 * 支持浅色模式 (Slate & Indigo) 和 深色模式 (OLED True Black)
 */
object CaptureUI {
    // ==========================================
    // 浅色模式调色板 (现代化石板灰 & 靛青)
    // ==========================================
    val Slate50 = Color(0xFFF8FAFC)
    val Slate100 = Color(0xFFF1F5F9)
    val Slate200 = Color(0xFFE2E8F0)
    val Slate400 = Color(0xFF94A3B8)
    val Slate500 = Color(0xFF64748B)
    val Slate600 = Color(0xFF475569)
    val Slate700 = Color(0xFF334155)
    val Slate800 = Color(0xFF1E293B)
    val Slate900 = Color(0xFF0F172A)

    val Indigo50 = Color(0xFFEEF2FF)
    val Indigo100 = Color(0xFFE0E7FF)
    val Indigo200 = Color(0xFFC7D2FE)
    val Indigo400 = Color(0xFF818CF8)
    val Indigo500 = Color(0xFF6366F1)
    val Indigo600 = Color(0xFF4F46E5)
    val Indigo700 = Color(0xFF4338CA)
    val Purple500 = Color(0xFFA855F7)

    // ==========================================
    // 深色模式调色板 (OLED 纯黑版 Neutral 色系)
    // ==========================================
    val OledSheet = Color(0xFF0A0A0A)           // 极深灰，弹窗底色
    val OledCard = Color(0xFF141414)            // 卡片底色
    val OledHandle = Color(0xFF262626)          // Neutral 800，操作块/手柄底色
    val OledBorder = Color(0x80262626)          // Neutral 800 50%透明，卡片边框

    val OledTextHeader = Color(0xFFFAFAFA)      // Neutral 50，主标题
    val OledTextPrimary = Color(0xFFE5E5E5)     // Neutral 200，主文本
    val OledTextSecondary = Color(0xFF737373)   // Neutral 500，辅助文本/图标
    val OledTextPlaceholder = Color(0xFF525252) // Neutral 600，占位符

    val OledIconTint = Color(0xFFC084FC)        // Purple 400，主题强调色
    val OledButtonBg = Color(0xFF262626)        // Neutral 800，主按钮底色

    val PrimaryGradient = Brush.linearGradient(
        colors = listOf(Indigo500, Purple500)
    )

    fun Modifier.glassyBackground(
        alpha: Float = 1f
    ): Modifier = this.then(
        Modifier.background(Color.White)
    )
}