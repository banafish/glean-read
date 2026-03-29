package com.gleanread.android

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gleanread.android.network.ApiConstants
import com.gleanread.android.ui.CaptureUI
import com.gleanread.android.ui.GlassyBottomSheet
import com.gleanread.android.ui.RichExcerptCard
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class FastCaptureActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        var sharedText = ""
        var sharedUrl = ""

        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            sharedText = intent.getStringExtra(Intent.EXTRA_TEXT) ?: ""
            val subject = intent.getStringExtra(Intent.EXTRA_SUBJECT) ?: ""
            sharedUrl = UrlExtractor.extract(intent) ?: ""
            if (sharedText == sharedUrl) sharedText = subject
        }

        setContent {
            SideEffect {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                    window.setBackgroundBlurRadius(50)
                }
            }
            MaterialTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { finish() },
                    contentAlignment = Alignment.BottomCenter
                ) {
                    CaptureDialogV2(
                        initialSharedContent = sharedText,
                        initialUrl = sharedUrl,
                        onDismiss = { finish() }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CaptureDialogV2(initialSharedContent: String, initialUrl: String, onDismiss: () -> Unit) {
    var thought by remember { mutableStateOf("") }
    val availableTags = listOf("研究", "想法", "待读", "灵感", "摘录", "教程", "稍后阅读")
    var selectedTags by remember { mutableStateOf(setOf<String>()) }
    var currentUrl by remember { mutableStateOf(initialUrl) }

    // UI 交互状态
    var isSaving by remember { mutableStateOf(false) }
    var isInputFocused by remember { mutableStateOf(false) }
    var showTagMenu by remember { mutableStateOf(false) }
    var showLinkMenu by remember { mutableStateOf(false) }
    var tempLink by remember { mutableStateOf("") }

    val containerBgColor by animateColorAsState(
        targetValue = if (isInputFocused || showTagMenu || showLinkMenu) Color.White else CaptureUI.Slate100,
        label = "bg_color"
    )
    val containerBorderColor by animateColorAsState(
        targetValue = if (isInputFocused || showTagMenu || showLinkMenu) CaptureUI.Indigo100 else Color.Transparent,
        label = "border_color"
    )

    // 严苛的高度控制：底部弹出只占屏幕的 54%，实现全屏无滚动
    GlassyBottomSheet(
        onDismiss = onDismiss,
        modifier = Modifier.fillMaxHeight(0.54f)
    ) {
        // 使用 Box 包装整个内容，允许弹出层 (Tag/Link Menu) 突破输入区的 weight 限制向上生长
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { /* 拦截点击，防止穿透关闭 */ },
            ) {
                // 1. 标题与头部 (左对齐布局)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AutoAwesome,
                        contentDescription = null,
                        tint = CaptureUI.Indigo600,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "极速摘录",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = CaptureUI.Slate800,
                            letterSpacing = 0.5.sp,
                            fontSize = 17.sp
                        )
                    )
                }

                // 2. 摘录源卡片
                Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                    RichExcerptCard(content = initialSharedContent, url = currentUrl)
                }

                // 3. 创新型无界思考输入区
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 10.dp)
                ) {
                    // 主体容器
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .shadow(
                                elevation = if (isInputFocused || showTagMenu || showLinkMenu) 12.dp else 0.dp,
                                shape = RoundedCornerShape(24.dp),
                                spotColor = CaptureUI.Indigo500.copy(alpha = 0.1f)
                            )
                            .background(containerBgColor, RoundedCornerShape(24.dp))
                            .border(1.dp, containerBorderColor, RoundedCornerShape(24.dp))
                    ) {
                        // 输入框部分
                        BasicTextField(
                            value = thought,
                            onValueChange = { thought = it },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 8.dp)
                                .onFocusChanged {
                                    isInputFocused = it.isFocused
                                    if(it.isFocused) { showTagMenu = false; showLinkMenu = false }
                                },
                            textStyle = TextStyle(
                                fontSize = 15.sp,
                                color = CaptureUI.Slate700,
                                lineHeight = 24.sp
                            ),
                            cursorBrush = SolidColor(CaptureUI.Indigo500),
                            decorationBox = { innerTextField ->
                                Box {
                                    if (thought.isEmpty()) {
                                        Text(
                                            text = "此刻你的心境是...",
                                            color = CaptureUI.Slate400.copy(alpha = 0.8f),
                                            fontSize = 15.sp
                                        )
                                    }
                                    innerTextField()
                                }
                            }
                        )

                        // 底部内嵌快捷操作台
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 左侧：标签与链接图标
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                // 标签按钮
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (selectedTags.isNotEmpty() || showTagMenu) CaptureUI.Indigo50 else Color.Transparent,
                                    modifier = Modifier.clickable(
                                        interactionSource = remember { MutableInteractionSource() }, indication = null
                                    ) { showTagMenu = !showTagMenu; showLinkMenu = false }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.LocalOffer,
                                            contentDescription = "Tags",
                                            tint = if (selectedTags.isNotEmpty() || showTagMenu) CaptureUI.Indigo600 else CaptureUI.Slate400,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        if (selectedTags.isNotEmpty()) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            val tagText = if (selectedTags.size == 1) selectedTags.first() else "${selectedTags.first()}等"
                                            Text(
                                                text = tagText,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = CaptureUI.Indigo600,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.widthIn(max = 50.dp)
                                            )
                                        }
                                    }
                                }

                                // 链接按钮
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (showLinkMenu) CaptureUI.Indigo50 else Color.Transparent,
                                    modifier = Modifier.clickable(
                                        interactionSource = remember { MutableInteractionSource() }, indication = null
                                    ) {
                                        showLinkMenu = !showLinkMenu; showTagMenu = false
                                        if(showLinkMenu) tempLink = currentUrl
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Link,
                                        contentDescription = "Link",
                                        tint = if (showLinkMenu) CaptureUI.Indigo600 else CaptureUI.Slate400,
                                        modifier = Modifier.padding(8.dp).size(18.dp)
                                    )
                                }
                            }

                            // 右侧：保存发送键
                            Surface(
                                onClick = {
                                    if (!isSaving) {
                                        isSaving = true
                                        CoroutineScope(Dispatchers.IO).launch {
                                            saveToGleanRead(initialSharedContent, currentUrl, thought, selectedTags)
                                            onDismiss()
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                color = if (thought.isNotEmpty()) CaptureUI.Indigo600 else CaptureUI.Slate800,
                                shadowElevation = if (thought.isNotEmpty()) 6.dp else 2.dp
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (isSaving) {
                                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                    } else {
                                        Text(
                                            text = "保存并继续",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.White
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 交互层覆盖：用于点击空白处关闭气泡
            if (showTagMenu || showLinkMenu) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { showTagMenu = false; showLinkMenu = false }
                )
            }

            // 标签弹出层 (移动到根容器 Box 中，彻底解除高度受输入框 bounds 限制的问题)
            TagMenuPopup(
                showTagMenu = showTagMenu,
                availableTags = availableTags,
                selectedTags = selectedTags,
                onTagSelected = { tag ->
                    selectedTags = if (selectedTags.contains(tag)) selectedTags - tag else selectedTags + tag
                },
                onClearTags = { selectedTags = emptySet() }
            )

            // 链接弹出层
            LinkMenuPopup(
                showLinkMenu = showLinkMenu,
                tempLink = tempLink,
                onTempLinkChange = { tempLink = it },
                onSaveLink = {
                    if(tempLink.trim().isNotEmpty()) currentUrl = tempLink
                    showLinkMenu = false
                }
            )
        }
    }
}

// ========================
// 扩展气泡组件区
// ========================

/**
 * 修复定制系统深色模式强制反色的问题。
 * 完全不使用 Surface，纯用最底层的 Box 渲染保证极高的色彩安全性。
 */
@Composable
private fun PopupTagPill(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (isSelected) CaptureUI.Indigo600 else Color(0xFFF1F5F9)) // 强制底色 (Slate100)
            .clickable { onClick() }
            .border(
                width = 1.dp,
                color = if (isSelected) Color.Transparent else Color(0xFFE2E8F0), // 强制边框色 (Slate200)
                shape = RoundedCornerShape(50)
            )
            .padding(horizontal = 14.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = if (isSelected) Color.White else Color(0xFF334155) // 强制文字色 (Slate700) 防止变白
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BoxScope.TagMenuPopup(
    showTagMenu: Boolean,
    availableTags: List<String>,
    selectedTags: Set<String>,
    onTagSelected: (String) -> Unit,
    onClearTags: () -> Unit
) {
    // 修复：改用 ScaleIn 缩放动画取代 SlideIn 位移，彻底解决动画期间由于裁剪导致的“矩形直角阴影”问题
    AnimatedVisibility(
        visible = showTagMenu,
        enter = fadeIn(tween(200)) + scaleIn(initialScale = 0.95f, animationSpec = tween(200), transformOrigin = TransformOrigin(0f, 1f)),
        exit = fadeOut(tween(150)) + scaleOut(targetScale = 0.95f, animationSpec = tween(150), transformOrigin = TransformOrigin(0f, 1f)),
        modifier = Modifier
            .align(Alignment.BottomStart)
            // 补偿内部 16dp 的留白 padding，确保视觉位置维持在 68dp/4dp
            // 关键修复：Compose 不允许负 padding，改用 offset 实现负向偏移
            .padding(bottom = 52.dp) 
            .offset(x = (-12).dp)
            .graphicsLayer { clip = false }
    ) {
        // 关键修复：在 AnimatedVisibility 内部增加一层透明 padding (16dp)
        // 强制扩大动画容器的边界，使得阴影绘制在容器内部，从而彻底避免被裁剪变方
        Box(modifier = Modifier.padding(16.dp)) {
            Surface(
                modifier = Modifier.width(260.dp).graphicsLayer { clip = false },
                shadowElevation = 12.dp,
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                border = BorderStroke(1.dp, CaptureUI.Slate100)
            ) {
                // “以底部为基准向上增长”：将内容顺序调整为 [内容] -> [标题/功能行]
                Column(modifier = Modifier.padding(12.dp)) {
                    // 1. 标签云区域 (上方)
                    Column(
                        modifier = Modifier
                            .heightIn(max = 160.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                        ) {
                            availableTags.forEach { tag ->
                                PopupTagPill(
                                    label = tag,
                                    isSelected = selectedTags.contains(tag)
                                ) {
                                    onTagSelected(tag)
                                }
                            }
                        }
                    }

                    // 2. 标题与操作行 (下方)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, bottom = 4.dp, start = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("选择分类标签", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = CaptureUI.Slate400)
                        if (selectedTags.isNotEmpty()) {
                            Text(
                                text = "清空",
                                fontSize = 11.sp,
                                color = CaptureUI.Indigo500,
                                modifier = Modifier.clickable { onClearTags() }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BoxScope.LinkMenuPopup(
    showLinkMenu: Boolean,
    tempLink: String,
    onTempLinkChange: (String) -> Unit,
    onSaveLink: () -> Unit
) {
    AnimatedVisibility(
        visible = showLinkMenu,
        enter = fadeIn(tween(200)) + scaleIn(initialScale = 0.95f, animationSpec = tween(200), transformOrigin = TransformOrigin(0f, 1f)),
        exit = fadeOut(tween(150)) + scaleOut(targetScale = 0.95f, animationSpec = tween(150), transformOrigin = TransformOrigin(0f, 1f)),
        modifier = Modifier
            .align(Alignment.BottomStart)
            .padding(bottom = 52.dp) // 同步补偿 16dp 留白
            .offset(x = (-12).dp)
            .graphicsLayer { clip = false }
    ) {
        Box(modifier = Modifier.padding(16.dp)) {
            Surface(
                modifier = Modifier.width(280.dp).graphicsLayer { clip = false },
                shadowElevation = 12.dp,
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                border = BorderStroke(1.dp, CaptureUI.Slate100)
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    BasicTextField(
                        value = tempLink,
                        onValueChange = onTempLinkChange,
                        modifier = Modifier
                            .weight(1f)
                            .background(CaptureUI.Slate50, RoundedCornerShape(8.dp))
                            .border(1.dp, CaptureUI.Slate200, RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        textStyle = TextStyle(fontSize = 13.sp, color = CaptureUI.Slate700),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { onSaveLink() }),
                        decorationBox = { inner ->
                            if(tempLink.isEmpty()) Text("http://...", fontSize=13.sp, color=CaptureUI.Slate400)
                            inner()
                        }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        onClick = { onSaveLink() },
                        shape = RoundedCornerShape(8.dp),
                        color = CaptureUI.Slate800
                    ) {
                        Text("确定", fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Medium, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
                    }
                }
            }
        }
    }
}

private fun saveToGleanRead(content: String, url: String, thought: String, tags: Set<String>) {
    try {
        val apiUrl = URL(ApiConstants.CAPTURE_ENDPOINT)
        val conn = apiUrl.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Content-Type", "application/json")
        conn.doOutput = true

        val json = JSONObject()
        json.put("content", content)
        json.put("url", url)
        json.put("userThought", thought)
        val tagArray = JSONArray()
        tags.forEach { tagArray.put(it) }
        json.put("tags", tagArray)

        OutputStreamWriter(conn.outputStream).use { it.write(json.toString()) }
        val responseCode = conn.responseCode
    } catch (e: Exception) {
        e.printStackTrace()
    }
}