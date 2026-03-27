package com.gleanread.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import android.content.Intent
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gleanread.android.ui.CaptureUI
import com.gleanread.android.ui.GlassyBottomSheet
import com.gleanread.android.network.ApiConstants
import com.gleanread.android.ui.RichExcerptCard
import com.gleanread.android.ui.TagPill
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * 接收来自其他应用的文本分享意图，采用沉浸式毛玻璃弹窗 (V2)
 */
class FastCaptureActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        var sharedText = ""
        var sharedUrl = ""
        
        if (intent?.action == Intent.ACTION_SEND) {
            when (intent.type) {
                "text/plain" -> {
                    sharedText = intent.getStringExtra(Intent.EXTRA_TEXT) ?: ""
                    if (sharedText.startsWith("http://") || sharedText.startsWith("https://")) {
                        sharedUrl = sharedText
                    }
                }
            }
        }

        setContent {
            // Android 12+ 启用系统级背景模糊 (Backdrop Blur)
            // 在 setContent 中调用，确保 DecorView 已创建，或者使用 SideEffect
            SideEffect {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                    window.setBackgroundBlurRadius(50)
                }
            }

            MaterialTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.45f)) // 轻度变暗，突出玻璃感
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaptureDialogV2(initialSharedContent: String, initialUrl: String, onDismiss: () -> Unit) {
    var thought by remember { mutableStateOf("") }
    val availableTags = listOf("研究", "想法", "待读", "灵感", "摘录", "教程")
    var selectedTags by remember { mutableStateOf(setOf<String>()) }
    var isSaving by remember { mutableStateOf(false) }

    // 按钮呼吸感动效
    val transition = rememberInfiniteTransition(label = "save_breathe")
    val scaleBreathe by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    GlassyBottomSheet(onDismiss = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { /* Consume clicks */ },
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 1. 标题头
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "✨",
                    fontSize = 20.sp,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = "极速摘录",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = CaptureUI.DeepSpaceBlue
                    )
                )
            }

            // 2. 书摘卡片预览 (Task 2.1)
            RichExcerptCard(content = initialSharedContent)

            // 3. 极简想法输入区域
            Column {
                Text(
                    text = "附加思考",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                OutlinedTextField(
                    value = thought,
                    onValueChange = { newVal -> thought = newVal },
                    placeholder = { Text("此刻你的心境是...", color = Color.LightGray) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CaptureUI.AuroraPurple,
                        unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor = Color.White.copy(alpha = 0.5f),
                        unfocusedContainerColor = Color.White.copy(alpha = 0.5f)
                    ),
                    maxLines = 3
                )
            }

            // 4. 胶囊标签选择 (Task 2.3)
            Column {
                Text(
                    text = "分类标记",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    contentPadding = PaddingValues(bottom = 4.dp)
                ) {
                    items(availableTags) { tag ->
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

            // 5. 交互式保存按钮 (Task 3.3)
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = { 
                    isSaving = true
                    // 调用保存逻辑
                    CoroutineScope(Dispatchers.IO).launch {
                        saveToGleanRead(initialSharedContent, initialUrl, thought, selectedTags)
                        onDismiss()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .scale(if (isSaving) 0.95f else scaleBreathe),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent
                ),
                contentPadding = PaddingValues()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(CaptureUI.PrimaryGradient),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Done, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("保存并继续心流", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

/**
 * 核心保存逻辑迁移
 */
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
