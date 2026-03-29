package com.gleanread.android

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gleanread.android.ui.CaptureUI
import com.gleanread.android.ui.CollapsibleUrlInput
import com.gleanread.android.ui.GlassyBottomSheet
import com.gleanread.android.network.ApiConstants
import com.gleanread.android.ui.RichExcerptCard
import com.gleanread.android.ui.SourceBadge
import com.gleanread.android.ui.TagPill
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CaptureDialogV2(initialSharedContent: String, initialUrl: String, onDismiss: () -> Unit) {
    var thought by remember { mutableStateOf("") }
    val availableTags = listOf("研究", "想法", "待读", "灵感", "摘录", "教程", "稍后阅读", "工作资料", "好文")
    var selectedTags by remember { mutableStateOf(setOf<String>("研究")) }
    var isSaving by remember { mutableStateOf(false) }
    var currentUrl by remember { mutableStateOf(initialUrl) }

    val transition = rememberInfiniteTransition(label = "save_breathe")
    val scaleBreathe by transition.animateFloat(
        initialValue = 1f, targetValue = 0.98f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    GlassyBottomSheet(onDismiss = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {},
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // 1. 标题头与无底色的纯净关闭按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.AutoAwesome,
                        contentDescription = null,
                        tint = CaptureUI.Indigo600,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "极速摘录",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = CaptureUI.Slate800,
                            letterSpacing = 0.5.sp,
                            fontSize = 18.sp
                        )
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Close",
                        tint = CaptureUI.Slate400,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // 2. 带有极致光晕 (Aura) 效果的书摘卡片
            Box(modifier = Modifier.fillMaxWidth()) {
                // 底层发光模糊效果
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.horizontalGradient(listOf(CaptureUI.Indigo500, CaptureUI.Purple500)),
                            RoundedCornerShape(16.dp)
                        )
                        .blur(24.dp)
                        .alpha(0.25f)
                )

                // 上层实体卡片
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CaptureUI.Slate50, RoundedCornerShape(16.dp))
                        .border(1.dp, CaptureUI.Slate100, RoundedCornerShape(16.dp))
                        // 优化：四周间距微调，让整体更紧凑
                        .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 12.dp)
                ) {
                    RichExcerptCard(content = initialSharedContent)

                    Divider(
                        color = CaptureUI.Slate200.copy(alpha = 0.6f),
                        // 优化：缩小分割线上下的留白，节省空间
                        modifier = Modifier.padding(top = 10.dp, bottom = 10.dp)
                    )

                    if (initialUrl.isNotEmpty()) {
                        SourceBadge(url = initialUrl)
                    } else {
                        CollapsibleUrlInput(url = currentUrl, onUrlChange = { currentUrl = it })
                    }
                }
            }

            // 3. 附加思考
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.ChatBubbleOutline,
                        contentDescription = null,
                        tint = CaptureUI.Slate500,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "附加思考",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = CaptureUI.Slate600
                    )
                }
                OutlinedTextField(
                    value = thought,
                    onValueChange = { newVal -> thought = newVal },
                    placeholder = { Text("此刻你的心境是...", color = CaptureUI.Slate400, fontSize = 14.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    textStyle = LocalTextStyle.current.copy(fontSize = 14.sp, color = CaptureUI.Slate800),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CaptureUI.Indigo500,
                        unfocusedBorderColor = CaptureUI.Slate200,
                        focusedContainerColor = CaptureUI.Slate50,
                        unfocusedContainerColor = CaptureUI.Slate50
                    ),
                    maxLines = 3, minLines = 3
                )
            }

            // 4. 标签选择 (限制高度并支持内部滚动)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.LocalOffer, // 类似 Tag 图标
                        contentDescription = null,
                        tint = CaptureUI.Slate500,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "分类标记",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = CaptureUI.Slate600
                    )
                }

                // 优化：使用 Box 限制最大高度，并开启垂直滚动
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 85.dp) // 约限制在两行半的高度，多出可滑动
                        .verticalScroll(rememberScrollState())
                ) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        availableTags.forEach { tag ->
                            TagPill(
                                label = tag,
                                isSelected = selectedTags.contains(tag)
                            ) {
                                selectedTags = if (selectedTags.contains(tag)) {
                                    selectedTags - tag
                                } else {
                                    selectedTags + tag
                                }
                            }
                        }
                    }
                }
            }

            // 5. Action Puck 动作按钮
            Spacer(modifier = Modifier.height(4.dp))
            Surface(
                onClick = {
                    isSaving = true
                    CoroutineScope(Dispatchers.IO).launch {
                        saveToGleanRead(initialSharedContent, currentUrl, thought, selectedTags)
                        onDismiss()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(68.dp)
                    .scale(if (isSaving) 0.96f else scaleBreathe),
                shape = RoundedCornerShape(34.dp),
                color = Color.White,
                border = BorderStroke(1.dp, CaptureUI.Slate200.copy(alpha = 0.8f)),
                shadowElevation = 6.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "保存并继续心流",
                        modifier = Modifier.padding(start = 16.dp),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 5.sp, // 极宽的字间距对齐网页版效果
                        color = CaptureUI.Slate700
                    )

                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .background(CaptureUI.Slate900, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Outlined.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
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