package com.gleanread.android.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FormatQuote
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.net.URL

@Composable
fun RichExcerptCard(
    content: String,
    url: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(CaptureUI.Indigo50, RoundedCornerShape(20.dp))
            .border(1.dp, Color.Transparent, RoundedCornerShape(20.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = Icons.Outlined.FormatQuote,
            contentDescription = null,
            tint = CaptureUI.Slate400.copy(alpha = 0.8f),
            modifier = Modifier
                .size(24.dp)
                .padding(top = 2.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = content,
                fontSize = 15.sp,
                color = Color(0xFF2D3748),
                lineHeight = 22.sp,
                letterSpacing = 0.5.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 3, // 多行截断
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(8.dp))
            SourceBadge(url = url)
        }
    }
}

@Composable
fun TagPill(
    label: String,
    isSelected: Boolean = false,
    onClick: () -> Unit
) {
    // 修复：舍弃 Surface，改用原生的 Box，防止不同机型/主题下文本颜色被覆盖失效
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(if (isSelected) CaptureUI.Indigo600 else CaptureUI.Slate50)
            .border(
                width = 1.dp,
                color = if (isSelected) Color.Transparent else CaptureUI.Slate200,
                shape = CircleShape
            )
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = if (isSelected) Color.White else CaptureUI.Slate600
        )
    }
}

@Composable
fun GlassyBottomSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .imePadding(),
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        color = Color.White,
        shadowElevation = 24.dp
    ) {
        Column(
            modifier = Modifier.padding(top = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .width(48.dp)
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(CaptureUI.Slate200)
            )
            Spacer(modifier = Modifier.height(16.dp))
            content()
        }
    }
}

@Composable
fun SourceBadge(url: String) {
    val host = remember(url) {
        try { URL(url).host.ifEmpty { url } } catch (e: Exception) { url }
    }
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Outlined.Link,
            contentDescription = null,
            tint = CaptureUI.Slate400,
            modifier = Modifier.size(12.dp).rotate(-45f)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = host.ifEmpty { "暂无来源链接" },
            fontSize = 12.sp,
            color = CaptureUI.Slate400,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}