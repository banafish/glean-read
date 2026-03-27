package com.gleanread.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gleanread.android.ui.CaptureUI.glassyBackground

/**
 * 书摘风格的内容预览卡片
 */
@Composable
fun RichExcerptCard(
    content: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.7f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "“",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = CaptureUI.AuroraPurple.copy(alpha = 0.6f)
            )
            Text(
                text = content,
                style = MaterialTheme.typography.bodyLarge.copy(
                    lineHeight = 24.sp,
                    letterSpacing = 0.5.sp
                ),
                maxLines = 6,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            Text(
                text = "”",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = CaptureUI.AuroraPurple.copy(alpha = 0.6f),
                modifier = Modifier.align(Alignment.End)
            )
        }
    }
}

/**
 * 胶囊风格的标签组件
 */
@Composable
fun TagPill(
    label: String,
    isSelected: Boolean = false,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .padding(end = 8.dp)
            .clickable { onClick() },
        shape = CircleShape,
        color = if (isSelected) CaptureUI.AuroraPurple else Color.White.copy(alpha = 0.4f),
        border = if (isSelected) null else AssistChipDefaults.assistChipBorder(enabled = true)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            color = if (isSelected) Color.White else CaptureUI.DeepSpaceBlue
        )
    }
}

/**
 * 带有拉环和毛玻璃背景的底部总容器
 */
@Composable
fun GlassyBottomSheet(
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
            .glassyBackground(alpha = 0.6f)
            .imePadding(),
        color = Color.Transparent // 背景由 glassyBackground 决定
    ) {
        Column(
            modifier = Modifier.padding(top = 12.dp, start = 20.dp, end = 20.dp, bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 优雅拉环 (Grabber)
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Color.Gray.copy(alpha = 0.4f))
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            content()
        }
    }
}
