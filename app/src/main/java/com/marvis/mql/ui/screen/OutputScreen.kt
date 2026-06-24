package com.marvis.mql.ui.screen

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marvis.mql.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OutputScreen(
    title: String,
    code: String,
    isStrategy: Boolean,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var showShareDialog by remember { mutableStateOf(false) }

    if (showShareDialog) {
        AlertDialog(
            onDismissRequest = { showShareDialog = false },
            title = { Text("分享/导出") },
            text = {
                Column {
                    Text("已复制到剪贴板，你可以粘贴到任何地方。")
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "提示: 将此代码导入文化财经交易软件的策略编辑器即可使用。",
                        fontSize = 12.sp,
                        color = OnSurface
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showShareDialog = false }) { Text("知道了") }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CodeBg)
    ) {
        // 顶部信息栏
        Surface(
            color = PrimaryDark,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "返回", tint = OnPrimary)
                    }
                    Column {
                        Text(title, color = OnPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(
                            if (isStrategy) "麦语言自动交易脚本" else "麦语言自定义指标",
                            color = OnPrimary.copy(alpha = 0.7f),
                            fontSize = 11.sp
                        )
                    }
                }
                Row {
                    IconButton(onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("MQL Code", code))
                        Toast.makeText(context, "已复制到剪贴板", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(Icons.Default.ContentCopy, "复制", tint = OnPrimary)
                    }
                    IconButton(onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("MQL Code", code))
                        showShareDialog = true
                    }) {
                        Icon(Icons.Default.Share, "分享", tint = OnPrimary)
                    }
                }
            }
        }

        // 统计信息
        Surface(
            color = Surface.copy(alpha = 0.05f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                StatChip("行数", "${code.lines().size}")
                StatChip("字符", "${code.length}")
                StatChip("类型", if (isStrategy) "策略" else "指标")
            }
        }

        // 代码显示
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                SelectionContainer {
                    Text(
                        text = highlightMqlSyntax(code),
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            color = CodeText
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun StatChip(label: String, value: String) {
    Column {
        Text(label, fontSize = 9.sp, color = OnSurface.copy(alpha = 0.5f))
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = OnSurface)
    }
}

/** 简单的麦语言语法着色（用 ANSI 风格 AnnotatedString，此处简化） */
@Composable
fun highlightMqlSyntax(code: String): String {
    // 实际项目中可用 AnnotatedString + SpanStyle 实现真正的语法高亮
    // 此处返回原文本，保留后续扩展空间
    return code
}
