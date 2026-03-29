package com.gleanread.android.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
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
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxWidth()) {
        // 装饰性双引号背景
        Icon(
            imageVector = Icons.Outlined.FormatQuote,
            contentDescription = null,
            tint = CaptureUI.Indigo500.copy(alpha = 0.12f),
            modifier = Modifier
                .size(56.dp)
                .offset(x = (-4).dp, y = (-8).dp)
                .rotate(-6f)
        )
        Text(
            text = content,
            style = MaterialTheme.typography.bodyMedium.copy(
                lineHeight = 24.sp,
                letterSpacing = 0.5.sp
            ),
            color = CaptureUI.Slate700,
            maxLines = 4,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 12.dp, start = 4.dp, end = 4.dp)
        )
    }
}

@Composable
fun TagPill(
    label: String,
    isSelected: Boolean = false,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.clickable { onClick() },
        shape = CircleShape,
        color = if (isSelected) CaptureUI.Indigo50 else Color.White,
        border = BorderStroke(
            width = 1.dp,
            color = if (isSelected) CaptureUI.Indigo200 else CaptureUI.Slate200
        )
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = if (isSelected) CaptureUI.Indigo700 else CaptureUI.Slate600
        )
    }
}

@Composable
fun GlassyBottomSheet(
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .imePadding(),
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        color = Color.White,
        shadowElevation = 24.dp
    ) {
        Column(
            modifier = Modifier.padding(top = 16.dp, start = 24.dp, end = 24.dp, bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .width(48.dp)
                    .height(5.dp)
                    .clip(CircleShape)
                    .background(CaptureUI.Slate200)
            )
            Spacer(modifier = Modifier.height(20.dp))
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
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .wrapContentWidth()
            // 优化：改为更柔和的半透明背景和全圆角（胶囊形状），去掉生硬的边框
            .background(CaptureUI.Slate200.copy(alpha = 0.4f), RoundedCornerShape(percent = 50))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Icon(
            imageVector = Icons.Outlined.Link,
            contentDescription = null,
            tint = CaptureUI.Slate500,
            modifier = Modifier.size(13.dp).rotate(-45f)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = host,
            fontSize = 12.sp,
            color = CaptureUI.Slate600,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun CollapsibleUrlInput(
    url: String,
    onUrlChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clickable { expanded = !expanded }
                .padding(vertical = 2.dp) // 优化：减小这里的垂直间距
        ) {
            Icon(
                imageVector = Icons.Outlined.Link,
                contentDescription = null,
                tint = CaptureUI.Indigo500,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = if (expanded) "来源链接" else "添加来源链接",
                fontSize = 13.sp,
                color = CaptureUI.Indigo600,
                fontWeight = FontWeight.Medium
            )
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            OutlinedTextField(
                value = url,
                onValueChange = onUrlChange,
                placeholder = { Text("粘贴文章原文链接...", color = CaptureUI.Slate400, fontSize = 13.sp) },
                // 优化：减小输入框出现时的顶部间距
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                shape = RoundedCornerShape(10.dp),
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(fontSize = 13.sp, color = CaptureUI.Slate700),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CaptureUI.Indigo500,
                    unfocusedBorderColor = CaptureUI.Slate200,
                    focusedContainerColor = CaptureUI.Slate50,
                    unfocusedContainerColor = CaptureUI.Slate50
                )
            )
        }
    }
}