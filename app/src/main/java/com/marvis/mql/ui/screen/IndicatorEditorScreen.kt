package com.marvis.mql.ui.screen

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marvis.mql.generator.IndicatorCompiler
import com.marvis.mql.library.MqlLibrary
import com.marvis.mql.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IndicatorEditorScreen(
    onCompile: (String, String) -> Unit
) {
    var indicatorName by remember { mutableStateOf("MyIndicator") }
    var sourceCode by remember { mutableStateOf("") }
    var errors by remember { mutableStateOf<List<String>>(emptyList()) }
    var showHelp by remember { mutableStateOf(false) }
    var showTemplate by remember { mutableStateOf(false) }
    var quickCheckResult by remember { mutableStateOf<String?>(null) }

    val templates = listOf(
        "双均线交叉" to """
// 双均线交叉指标 - 金叉死叉信号
INPUT: SHORT(5,1,100,1), LONG(20,1,200,1);
MA_SHORT := MA(CLOSE, SHORT);
MA_LONG := MA(CLOSE, LONG);
金叉信号 := CROSS(MA_SHORT, MA_LONG);
死叉信号 := CROSS(MA_LONG, MA_SHORT);
差值 := MA_SHORT - MA_LONG;
        """.trimIndent(),
        "MACD指标" to """
// MACD指标
INPUT: SHORT(12,1,100,1), LONG(26,1,100,1), M(9,1,100,1);
DIFF := EMA(CLOSE, SHORT) - EMA(CLOSE, LONG);
DEA := EMA(DIFF, M);
MACD_VAL := 2 * (DIFF - DEA);
        """.trimIndent(),
        "KDJ指标" to """
// KDJ指标
INPUT: N(9,1,100,1), M1(3,1,50,1), M2(3,1,50,1);
RSV := (CLOSE - LLV(LOW, N)) / (HHV(HIGH, N) - LLV(LOW, N)) * 100;
K := SMA(RSV, M1, 1);
D := SMA(K, M2, 1);
J := 3 * K - 2 * D;
        """.trimIndent(),
        "RSI指标" to """
// RSI指标
INPUT: N(14,1,100,1);
LC := REF(CLOSE, 1);
UP := MAX(CLOSE - LC, 0);
DOWN := ABS(CLOSE - LC);
RSI_VAL := SMA(UP, N, 1) / SMA(DOWN, N, 1) * 100;
        """.trimIndent(),
        "布林带" to """
// 布林带指标
INPUT: N(20,1,200,1), P(2,1,10,1);
MID := MA(CLOSE, N);
STD_VAL := STD(CLOSE, N);
UPPER := MID + P * STD_VAL;
LOWER := MID - P * STD_VAL;
带宽 := (UPPER - LOWER) / MID * 100;
        """.trimIndent()
    )

    // 模板选择对话框
    if (showTemplate) {
        AlertDialog(
            onDismissRequest = { showTemplate = false },
            title = { Text("选择模板") },
            text = {
                LazyColumn(modifier = Modifier.height(350.dp)) {
                    items(templates) { (name, code) ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable {
                                    sourceCode = code
                                    indicatorName = name
                                    showTemplate = false
                                },
                            colors = CardDefaults.cardColors(containerColor = Surface)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(name, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Primary)
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    code.take(80) + "...",
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = OnSurface
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTemplate = false }) { Text("取消") }
            }
        )
    }

    // 帮助对话框
    if (showHelp) {
        AlertDialog(
            onDismissRequest = { showHelp = false },
            title = { Text("麦语言快速参考") },
            text = {
                LazyColumn(modifier = Modifier.height(400.dp)) {
                    items(MqlLibrary.allFunctions.sortedBy { it.category }) { func ->
                        Column(modifier = Modifier.padding(vertical = 2.dp)) {
                            Text(
                                "${func.name} - ${func.category}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Primary
                            )
                            Text(func.description, fontSize = 11.sp, color = OnSurface)
                            Text(
                                func.syntax,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = FunctionColor
                            )
                            Divider(modifier = Modifier.padding(vertical = 2.dp), color = DividerColor)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showHelp = false }) { Text("关闭") }
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 20.dp)
    ) {
        // 名称和模板
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = indicatorName,
                    onValueChange = { indicatorName = it },
                    label = { Text("指标名称") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                FilledTonalButton(onClick = { showTemplate = true }) {
                    Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("模板", fontSize = 12.sp)
                }
                FilledTonalButton(onClick = { showHelp = true }) {
                    Icon(Icons.Default.Help, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("帮助", fontSize = 12.sp)
                }
            }
        }

        // 代码编辑区
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CodeBg),
                modifier = Modifier.fillMaxWidth().heightIn(min = 280.dp)
            ) {
                OutlinedTextField(
                    value = sourceCode,
                    onValueChange = {
                        sourceCode = it
                        errors = emptyList()
                        quickCheckResult = null
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 280.dp),
                    textStyle = TextStyle(
                        color = CodeText,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        lineHeight = 20.sp
                    ),
                    placeholder = {
                        Text(
                            "在此输入麦语言指标代码...\n\n示例:\nMA5 := MA(CLOSE, 5);\nMA10 := MA(CLOSE, 10);\nCROSS(MA5, MA10);",
                            color = CommentColor,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Primary,
                        unfocusedBorderColor = CardBorder,
                        cursorColor = CodeText
                    )
                )
            }
        }

        // 快捷插入按钮
        item {
            Text("快捷插入:", fontWeight = FontWeight.Medium, fontSize = 13.sp)
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val snippets = listOf(
                    "MA" to "MA(CLOSE, ",
                    "EMA" to "EMA(CLOSE, ",
                    "MACD" to "MACD(CLOSE, 12, 26, 9)",
                    "CROSS" to "CROSS(",
                    "REF" to "REF(CLOSE, 1)",
                    "HHV" to "HHV(HIGH, 20)",
                    "LLV" to "LLV(LOW, 20)",
                    "RSI" to "RSI(CLOSE, 14)",
                    "IF" to "IF(COND, A, B)",
                    "INPUT" to "INPUT: N(20,1,100,1);",
                    "DRAWLINE" to "DRAWLINE(COND1, PRICE1, COND2, PRICE2, 0);",
                    "STICKLINE" to "STICKLINE(COND, HIGH, LOW, 1, 0);"
                )
                snippets.forEach { (label, snippet) ->
                    AssistChip(
                        onClick = { sourceCode += snippet },
                        label = { Text(label, fontSize = 11.sp, fontFamily = FontFamily.Monospace) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = Primary.copy(alpha = 0.1f)
                        )
                    )
                }
            }
        }

        // 错误列表
        if (errors.isNotEmpty()) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = Error.copy(alpha = 0.1f))) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("编译错误:", fontWeight = FontWeight.Bold, color = Error)
                        errors.forEach { err ->
                            Text("• $err", fontSize = 12.sp, color = Error)
                        }
                    }
                }
            }
        }

        // 快速检查结果
        quickCheckResult?.let { msg ->
            item {
                Card(colors = CardDefaults.cardColors(containerColor = Secondary.copy(alpha = 0.1f))) {
                    Text(msg, modifier = Modifier.padding(12.dp), color = Secondary, fontSize = 13.sp)
                }
            }
        }

        // 操作按钮
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        val compiler = IndicatorCompiler()
                        val checkErrors = compiler.quickCheck(sourceCode)
                        if (checkErrors.isEmpty()) {
                            quickCheckResult = "语法检查通过——代码结构正确，可以编译"
                        } else {
                            errors = checkErrors
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("语法检查", fontSize = 13.sp)
                }
                Button(
                    onClick = {
                        if (sourceCode.isBlank()) {
                            errors = listOf("请输入指标代码")
                            return@Button
                        }
                        val compiler = IndicatorCompiler()
                        val result = compiler.compile(sourceCode, indicatorName)
                        if (result.success) {
                            onCompile(
                                "指标: ${result.generatedIndicator?.name ?: indicatorName}",
                                result.sourceCode
                            )
                        } else {
                            errors = result.errors
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary)
                ) {
                    Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("编译指标", fontSize = 13.sp)
                }
            }
        }
    }
}
