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
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape // 加上这一行导入
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
import com.gleanread.android.ui.CaptureBottomSheet
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
            val isDark = isSystemInDarkTheme()
            SideEffect {
                // 对于 OLED 模式，背景模糊是不必要的，但保留不会影响功能
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                    window.setBackgroundBlurRadius(if(isDark) 1 else 50)
                }
            }
            MaterialTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        // OLED 模式下采用深黑色遮罩 (Black 60%)
                        .background(Color.Black.copy(alpha = if (isDark) 0.6f else 0.5f))
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
    val isDark = isSystemInDarkTheme()
    var thought by remember { mutableStateOf("") }
    val availableTags = listOf("研究", "想法", "待读", "灵感", "摘录", "教程", "稍后阅读", "研究2", "想法2", "待读2", "灵感2", "摘录2", "教程2")
    var selectedTags by remember { mutableStateOf(setOf<String>()) }
    var currentUrl by remember { mutableStateOf(initialUrl) }

    // UI 交互状态
    var isSaving by remember { mutableStateOf(false) }
    var isInputFocused by remember { mutableStateOf(false) }
    var showTagMenu by remember { mutableStateOf(false) }
    var showLinkMenu by remember { mutableStateOf(false) }
    var tempLink by remember { mutableStateOf("") }

    // 主题适配动态色值
    val targetBgColor = if (isDark) CaptureUI.OledCard else {
        if (isInputFocused || showTagMenu || showLinkMenu) Color.White else CaptureUI.Slate100
    }
    val targetBorderColor = if (isDark) CaptureUI.OledBorder else {
        if (isInputFocused || showTagMenu || showLinkMenu) CaptureUI.Indigo100 else Color.Transparent
    }

    val containerBgColor by animateColorAsState(targetValue = targetBgColor, label = "bg_color")
    val containerBorderColor by animateColorAsState(targetValue = targetBorderColor, label = "border_color")

    CaptureBottomSheet(
        onDismiss = onDismiss,
        modifier = Modifier.fillMaxHeight(0.54f)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Transparent)
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
                        tint = if (isDark) CaptureUI.OledIconTint else CaptureUI.Indigo600,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "极速摘录",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) CaptureUI.OledTextHeader else CaptureUI.Slate800,
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
                                elevation = if ((isInputFocused || showTagMenu || showLinkMenu) && !isDark) 12.dp else 0.dp,
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
                                color = if (isDark) CaptureUI.OledTextPrimary else CaptureUI.Slate700,
                                lineHeight = 24.sp
                            ),
                            cursorBrush = SolidColor(if (isDark) CaptureUI.OledIconTint else CaptureUI.Indigo500),
                            decorationBox = { innerTextField ->
                                Box {
                                    if (thought.isEmpty()) {
                                        Text(
                                            text = "此刻你的想法是...",
                                            color = if (isDark) CaptureUI.OledTextPlaceholder else CaptureUI.Slate400.copy(alpha = 0.8f),
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
                                val tagMenuBtnActive = selectedTags.isNotEmpty() || showTagMenu
                                val tagMenuBtnBg = Color.Transparent // 2. 去掉选中时的背景色
                                val tagMenuBtnTint = if (isDark) {
                                    if (tagMenuBtnActive) CaptureUI.OledIconTint else CaptureUI.OledTextSecondary
                                } else {
                                    if (tagMenuBtnActive) CaptureUI.Indigo600 else CaptureUI.Slate400
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = tagMenuBtnBg,
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
                                            tint = tagMenuBtnTint,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        if (selectedTags.isNotEmpty()) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            val tagText = if (selectedTags.size == 1) selectedTags.first() else "${selectedTags.first()}等"
                                            Text(
                                                text = tagText,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = tagMenuBtnTint,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.widthIn(max = 50.dp)
                                            )
                                        }
                                    }
                                }

                                // 链接按钮
                                val linkMenuBtnBg = Color.Transparent // 同步去掉链接按钮选中时的背景色保持一致
                                val linkMenuBtnTint = if (isDark) {
                                    if (showLinkMenu) CaptureUI.OledIconTint else CaptureUI.OledTextSecondary
                                } else {
                                    if (showLinkMenu) CaptureUI.Indigo600 else CaptureUI.Slate400
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = linkMenuBtnBg,
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
                                        tint = linkMenuBtnTint,
                                        modifier = Modifier.padding(8.dp).size(18.dp)
                                    )
                                }
                            }

                            // 右侧：保存发送键
                            val thoughtNotEmpty = thought.isNotEmpty()
                            val saveBtnBg = if (isDark) {
                                // 2. 深色模式下不可点时提亮背景色（OledHandle 替代 OledCard），使其从深色背景中显现出来
                                if (thoughtNotEmpty) CaptureUI.OledIconTint else CaptureUI.OledHandle
                            } else {
                                if (thoughtNotEmpty) CaptureUI.Indigo600 else CaptureUI.Slate800
                            }

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
                                shape = CircleShape, // 1. 修改为椭圆形 (即 Compose 里的胶囊形状 CircleShape)
                                color = saveBtnBg,
                                border = if (isDark && !thoughtNotEmpty) BorderStroke(1.dp, CaptureUI.OledBorder) else null,
                                shadowElevation = if (isDark) 0.dp else (if (thoughtNotEmpty) 6.dp else 2.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp), // 稍微拉大水平宽度让胶囊感更好
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (isSaving) {
                                        CircularProgressIndicator(
                                            color = Color.White, // 统一使用白色以确保在彩色按钮上清晰可见
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp
                                        )
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

            // 交互层覆盖
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

            // 弹出层组件
            TagMenuPopup(
                isDark = isDark,
                showTagMenu = showTagMenu,
                availableTags = availableTags,
                selectedTags = selectedTags,
                onTagSelected = { tag ->
                    selectedTags = if (selectedTags.contains(tag)) selectedTags - tag else selectedTags + tag
                },
                onClearTags = { selectedTags = emptySet() }
            )

            LinkMenuPopup(
                isDark = isDark,
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

@Composable
private fun PopupTagPill(
    label: String,
    isSelected: Boolean,
    isDark: Boolean,
    onClick: () -> Unit
) {
    val bgColor = if (isDark) {
        if (isSelected) CaptureUI.OledIconTint else CaptureUI.OledCard
    } else {
        if (isSelected) CaptureUI.Indigo600 else Color(0xFFF1F5F9)
    }

    val borderColor = if (isDark) {
        if (isSelected) Color.Transparent else CaptureUI.OledBorder
    } else {
        if (isSelected) Color.Transparent else Color(0xFFE2E8F0)
    }

    val textColor = if (isDark) {
        if (isSelected) Color.White else CaptureUI.OledTextSecondary
    } else {
        if (isSelected) Color.White else Color(0xFF334155)
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(bgColor)
            .clickable { onClick() }
            .border(
                width = 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(50)
            )
            .padding(horizontal = 14.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = textColor
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BoxScope.TagMenuPopup(
    isDark: Boolean,
    showTagMenu: Boolean,
    availableTags: List<String>,
    selectedTags: Set<String>,
    onTagSelected: (String) -> Unit,
    onClearTags: () -> Unit
) {
    AnimatedVisibility(
        visible = showTagMenu,
        enter = fadeIn(tween(200)) + scaleIn(initialScale = 0.95f, animationSpec = tween(200), transformOrigin = TransformOrigin(0f, 1f)),
        exit = fadeOut(tween(150)) + scaleOut(targetScale = 0.95f, animationSpec = tween(150), transformOrigin = TransformOrigin(0f, 1f)),
        modifier = Modifier
            .align(Alignment.BottomStart)
            .padding(bottom = 52.dp)
            .offset(x = (-12).dp)
            .graphicsLayer { clip = false }
    ) {
        Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 16.dp)) {
        Surface(
                modifier = Modifier.width(280.dp).graphicsLayer { clip = false }, // 1. 加宽到 280dp，确保刚好能放下一行4个两字标签，消除右侧过多留白
                shadowElevation = if (isDark) 0.dp else 12.dp,
                shape = RoundedCornerShape(16.dp),
                color = if (isDark) CaptureUI.OledSheet else Color.White,
                border = if (isDark) BorderStroke(1.dp, CaptureUI.OledBorder) else BorderStroke(1.dp, CaptureUI.Slate100)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // 1. 标签云区域
                    Column(
                        modifier = Modifier
                            .heightIn(max = 160.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            availableTags.forEach { tag ->
                                PopupTagPill(
                                    label = tag,
                                    isSelected = selectedTags.contains(tag),
                                    isDark = isDark
                                ) {
                                    onTagSelected(tag)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp)) // 去掉 HorizontalDivider 分割线，仅保留留白间距

                    // 2. 标题与操作行 (重新排版)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "选择分类标签",
                            fontSize = 12.sp, // 稍微放大以匹配常规字号层级
                            fontWeight = FontWeight.Medium,
                            color = if (isDark) CaptureUI.OledTextSecondary else CaptureUI.Slate400
                        )
                        if (selectedTags.isNotEmpty()) {
                            Text(
                                text = "清空",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isDark) CaptureUI.OledIconTint else CaptureUI.Indigo500,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .clickable { onClearTags() }
                                    .padding(horizontal = 4.dp, vertical = 2.dp) // 扩大点击热区
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
    isDark: Boolean,
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
            .padding(bottom = 52.dp)
            .offset(x = (-12).dp)
            .graphicsLayer { clip = false }
    ) {
        Box(modifier = Modifier.padding(16.dp)) {
            Surface(
                modifier = Modifier.width(280.dp).graphicsLayer { clip = false },
                shadowElevation = if (isDark) 0.dp else 12.dp,
                shape = RoundedCornerShape(16.dp),
                color = if (isDark) CaptureUI.OledSheet else Color.White,
                border = if (isDark) BorderStroke(1.dp, CaptureUI.OledBorder) else BorderStroke(1.dp, CaptureUI.Slate100)
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    val inputBg = if (isDark) CaptureUI.OledCard else CaptureUI.Slate50
                    val inputBorder = if (isDark) CaptureUI.OledBorder else CaptureUI.Slate200

                    BasicTextField(
                        value = tempLink,
                        onValueChange = onTempLinkChange,
                        modifier = Modifier
                            .weight(1f)
                            .background(inputBg, RoundedCornerShape(8.dp))
                            .border(1.dp, inputBorder, RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        textStyle = TextStyle(
                            fontSize = 13.sp,
                            color = if (isDark) CaptureUI.OledTextPrimary else CaptureUI.Slate700
                        ),
                        singleLine = true,
                        cursorBrush = SolidColor(if (isDark) CaptureUI.OledIconTint else CaptureUI.Indigo500),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { onSaveLink() }),
                        decorationBox = { inner ->
                            if(tempLink.isEmpty()) {
                                Text(
                                    text = "http://...",
                                    fontSize = 13.sp,
                                    color = if (isDark) CaptureUI.OledTextPlaceholder else CaptureUI.Slate400
                                )
                            }
                            inner()
                        }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        onClick = { onSaveLink() },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isDark) CaptureUI.OledButtonBg else CaptureUI.Slate800,
                        border = if (isDark) BorderStroke(1.dp, CaptureUI.OledBorder) else null
                    ) {
                        Text(
                            text = "确定",
                            fontSize = 13.sp,
                            color = if (isDark) CaptureUI.OledTextPrimary else Color.White,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
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