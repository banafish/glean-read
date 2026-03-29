package com.gleanread.android.ui

import androidx.compose.foundation.background
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Capture V2 UI 主题定义 (Slate & Indigo 现代风格)
 */
object CaptureUI {
    // 现代化石板灰调色板
    val Slate50 = Color(0xFFF8FAFC)
    val Slate100 = Color(0xFFF1F5F9)
    val Slate200 = Color(0xFFE2E8F0)
    val Slate400 = Color(0xFF94A3B8)
    val Slate500 = Color(0xFF64748B)
    val Slate600 = Color(0xFF475569)
    val Slate700 = Color(0xFF334155)
    val Slate800 = Color(0xFF1E293B)
    val Slate900 = Color(0xFF0F172A)

    // 靛青/紫色点缀调色板
    val Indigo50 = Color(0xFFEEF2FF)
    val Indigo100 = Color(0xFFE0E7FF)
    val Indigo200 = Color(0xFFC7D2FE)
    val Indigo500 = Color(0xFF6366F1)
    val Indigo600 = Color(0xFF4F46E5)
    val Indigo700 = Color(0xFF4338CA)
    val Purple500 = Color(0xFFA855F7)

    val PrimaryGradient = Brush.linearGradient(
        colors = listOf(Indigo500, Purple500)
    )

    fun Modifier.glassyBackground(
        alpha: Float = 1f
    ): Modifier = this.then(
        Modifier.background(Color.White)
    )
}