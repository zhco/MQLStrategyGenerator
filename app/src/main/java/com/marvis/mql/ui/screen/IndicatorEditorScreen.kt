
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marvis.mql.generator.IndicatorCompiler
import com.marvis.mql.ui.theme.*

/**
 * 系统内置指标模板 — 导入后可直接编译生成 MQL 代码
 */
object IndicatorTemplateLibrary {
    data class Template(val id: String, val name: String, val desc: String, val category: String, val code: String)

    val allTemplates = listOf(
        Template("tpl_ma_cross", "双均线交叉", "短期均线上穿长期均线买入", "趋势跟踪",
            """MA5 := MA(CLOSE, 5);
MA20 := MA(CLOSE, 20);
金叉信号 := CROSS(MA5, MA20);
死叉信号 := CROSS(MA20, MA5);"""),
        Template("tpl_macd_signal", "MACD 金叉死叉", "经典 MACD 交易信号", "趋势跟踪",
            """DIFF := EMA(CLOSE, 12) - EMA(CLOSE, 26);
DEA := EMA(DIFF, 9);
MACD := 2 * (DIFF - DEA);
金叉信号 := CROSS(DIFF, DEA);
死叉信号 := CROSS(DEA, DIFF);"""),
        Template("tpl_rsi_over", "RSI 超买超卖", "RSI 逆向交易信号", "震荡指标",
            """RSI14 := RSI(CLOSE, 14);
超卖信号 := RSI14 < 30;
超买信号 := RSI14 > 70;"""),
        Template("tpl_kdj", "KDJ 金叉信号", "随机指标交叉交易", "震荡指标",
            """RSV := (CLOSE - LLV(LOW, 9)) / (HHV(HIGH, 9) - LLV(LOW, 9)) * 100;
K := SMA(RSV, 3, 1);
D := SMA(K, 3, 1);
J := 3 * K - 2 * D;
金叉信号 := CROSS(K, D) AND J < 30;"""),
        Template("tpl_boll_break", "布林带突破", "价格突破布林带上下轨", "波动率",
            """MID := MA(CLOSE, 20);
UPPER := MID + 2 * STD(CLOSE, 20);
LOWER := MID - 2 * STD(CLOSE, 20);
上轨突破 := CROSS(CLOSE, UPPER);
下轨突破 := CROSS(LOWER, CLOSE);
中轨支撑 := CLOSE > MID AND REF(CLOSE, 1) < REF(MID, 1);"""),
        Template("tpl_ema_ribbon", "EMA 均线带", "5/10/20/60 均线多头排列", "趋势跟踪",
            """EMA5 := EMA(CLOSE, 5);
EMA10 := EMA(CLOSE, 10);
EMA20 := EMA(CLOSE, 20);
EMA60 := EMA(CLOSE, 60);
多头排列 := EMA5 > EMA10 AND EMA10 > EMA20 AND EMA20 > EMA60 AND CLOSE > EMA5;
空头排列 := EMA5 < EMA10 AND EMA10 < EMA20 AND EMA20 < EMA60 AND CLOSE < EMA5;"""),
        Template("tpl_break_high", "唐奇安通道突破", "N日最高/最低价突破", "突破策略",
            """N := 20;
上轨 := HHV(HIGH, N);
下轨 := LLV(LOW, N);
中轨 := (上轨 + 下轨) / 2;
上突破 := CROSS(CLOSE, REF(上轨, 1));
下突破 := CROSS(REF(下轨, 1), CLOSE);"""),
        Template("tpl_atr_stop", "ATR 动态止损", "基于 ATR 波动率的止损", "风控",
            """ATR14 := MA(TR, 14);
多头止损价 := CLOSE - 2 * ATR14;
空头止损价 := CLOSE + 2 * ATR14;"""),
        Template("tpl_volume_break", "量价突破", "放量突破前高信号", "量价分析",
            """VMA20 := MA(VOL, 20);
前高 := HHV(HIGH, 20);
放量突破 := CLOSE > REF(前高, 1) AND VOL > 1.5 * VMA20;"""),
        Template("tpl_multi_tf", "多周期共振", "日线+小时线方向一致", "多周期",
            """日线趋势 := CLOSE > MA(CLOSE, 20);
小时趋势 := CLOSE#60MIN > MA(CLOSE#60MIN, 20);
共振做多 := 日线趋势 AND 小时趋势;"""),
        Template("tpl_sar_trend", "SAR 抛物线", "SAR 转向交易系统", "趋势跟踪",
            """AF := 0.02;
EP := 0.0;
SAR信号 := SAR(AF, EP);
趋势转多 := CROSS(CLOSE, SAR信号);
趋势转空 := CROSS(SAR信号, CLOSE);"""),
        Template("tpl_adx_trend", "ADX 趋势强度", "DMI 趋势判定系统", "趋势跟踪",
            """PDI14 := PDI(14);
MDI14 := MDI(14);
ADX14 := ADX(14);
强势多头 := PDI14 > MDI14 AND ADX14 > 25;
强势空头 := MDI14 > PDI14 AND ADX14 > 25;"""),
        Template("tpl_pullback", "均线回调策略", "价格回调至均线买入", "趋势跟踪",
            """MA60 := MA(CLOSE, 60);
趋势向上 := MA60 > REF(MA60, 5);
回调买入 := 趋势向上 AND LLV(CLOSE, 3) <= MA60 * 1.005 AND CLOSE > MA60;"""),
        Template("tpl_open_range", "开盘区间突破", "突破开盘 N 分钟高低点", "日内策略",
            """开盘高 := HHV(HIGH, 10);
开盘低 := LLV(LOW, 10);
区间突破上 := CROSS(CLOSE, 开盘高);
区间突破下 := CROSS(开盘低, CLOSE);"""),
        Template("tpl_custom_ref", "跨周期引用模板", "引用日线数据到小时线", "多周期",
            """INPUT: FAST(12,1,100,1), SLOW(26,1,100,1);
日线DIFF := (EMA(CLOSE#DAY, FAST) - EMA(CLOSE#DAY, SLOW));
日线DEA := EMA(日线DIFF#DAY, 9);
日线金叉 := CROSS(日线DIFF, 日线DEA) AND 日线DIFF > 0;"""),
        Template("tpl_turtle", "海龟交易系统", "经典海龟趋势跟踪", "趋势跟踪",
            """N := MA(TR, 20);
入场上轨 := HHV(HIGH, 20);
入场下轨 := LLV(LOW, 20);
止损价格 := CLOSE - 2 * N;
做多信号 := CROSS(CLOSE, REF(入场上轨, 1)) AND N > 0;
做空信号 := CROSS(REF(入场下轨, 1), CLOSE) AND N > 0;"""),
        Template("tpl_wr_oversold", "威廉超卖反弹", "威廉指标极端区域反弹", "震荡指标",
            """WR14 := (HHV(HIGH, 14) - CLOSE) / (HHV(HIGH, 14) - LLV(LOW, 14)) * 100;
超卖信号 := WR14 > 80 AND REF(WR14, 1) > 80;
超买信号 := WR14 < 20 AND REF(WR14, 1) < 20;"""),
        Template("tpl_ichi", "一目均衡表", "一目云层突破系统", "趋势跟踪",
            """转向线 := (HHV(HIGH, 9) + LLV(LOW, 9)) / 2;
基准线 := (HHV(HIGH, 26) + LLV(LOW, 26)) / 2;
先行A := (转向线 + 基准线) / 2;
先行B := (HHV(HIGH, 52) + LLV(LOW, 52)) / 2;
金叉信号 := CROSS(转向线, 基准线) AND CLOSE > 先行A;"""),
        Template("tpl_vwap", "VWAP 成交量", "量加权均价偏离策略", "量价分析",
            """VWAP值 := SUM(CLOSE * VOL, 20) / SUM(VOL, 20);
多头信号 := CLOSE > VWAP值 AND VOL > MA(VOL, 20);
空头信号 := CLOSE < VWAP值 AND VOL > MA(VOL, 20);"""),
        Template("tpl_dual_thrust", "双推力突破", "经典隔夜突破策略", "突破策略",
            """N := 5; K1 := 0.7; K2 := 0.7;
HH := HHV(HIGH, N);
LL := LLV(LOW, N);
Range := MAX(HH - LL, CLOSE - LL);
上轨 := CLOSE + K1 * Range;
下轨 := CLOSE - K2 * Range;
做多信号 := CROSS(CLOSE, REF(上轨, 1));
做空信号 := CROSS(REF(下轨, 1), CLOSE);""")
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IndicatorEditorScreen(
    onCompile: (String, String) -> Unit
) {
    var sourceCode by remember { mutableStateOf("MA5 := MA(CLOSE, 5);\nMA10 := MA(CLOSE, 10);\n金叉 := CROSS(MA5, MA10);") }
    var errors by remember { mutableStateOf<List<String>>(emptyList()) }
    var showTemplateDialog by remember { mutableStateOf(false) }
    var showFormatDialog by remember { mutableStateOf(false) }
    var compileResult by remember { mutableStateOf<String?>(null) }

    // ===== 模板对话框 =====
    if (showTemplateDialog) {
        AlertDialog(
            onDismissRequest = { showTemplateDialog = false },
            title = { Text("导入系统模板", fontWeight = FontWeight.Bold) },
            text = {
                LazyColumn(modifier = Modifier.height(420.dp)) {
                    // 分类展示
                    val categories = IndicatorTemplateLibrary.allTemplates.map { it.category }.distinct()
                    categories.forEach { cat ->
                        item {
                            Text(cat, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Primary, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
                        }
                        val catTemplates = IndicatorTemplateLibrary.allTemplates.filter { it.category == cat }
                        items(catTemplates) { tpl ->
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp).clickable {
                                    sourceCode = tpl.code
                                    errors = emptyList()
                                    compileResult = null
                                    showTemplateDialog = false
                                },
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(tpl.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Primary)
                                        Text(tpl.category, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurface)
                                    }
                                    Text(tpl.desc, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                                    Text(tpl.code.lines().take(3).joinToString("\n"), fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = Color.Gray, maxLines = 3)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showTemplateDialog = false }) { Text("关闭") } }
        )
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // 插入常用函数
                var showInsertMenu by remember { mutableStateOf(false) }
                Box {
                    OutlinedButton(onClick = { showInsertMenu = true }, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)) {
                        Text("插入", fontSize = 12.sp)
                    }
                    DropdownMenu(expanded = showInsertMenu, onDismissRequest = { showInsertMenu = false }) {
                        listOf(
                            "MA(CLOSE,5)" to "MA",
                            "EMA(CLOSE,12)" to "EMA",
                            "CROSS(A,B)" to "CROSS",
                            "REF(X,N)" to "REF",
                            "HHV(HIGH,N)" to "HHV",
                            "LLV(LOW,N)" to "LLV",
                            "STD(CLOSE,N)" to "STD",
                            "IF(COND,A,B)" to "IF",
                            "BARSLAST(X)" to "BARSLAST",
                            "MACD(CLOSE,12,26,9)" to "MACD",
                            ":= " to "赋值",
                            "CLOSE#DAY" to "跨周期"
                        ).forEach { (code, name) ->
                            DropdownMenuItem(text = { Text(name, fontSize = 12.sp) }, onClick = {
                                sourceCode = if (sourceCode.endsWith("\n") || sourceCode.isEmpty()) sourceCode + code else sourceCode + "\n" + code
                                showInsertMenu = false
                            })
                        }
                    }
                }
                // 导入模板
                OutlinedButton(onClick = { showTemplateDialog = true }, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)) {
                    Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(2.dp))
                    Text("模板", fontSize = 12.sp)
                }
                Spacer(Modifier.weight(1f))
                // 语法检查
                OutlinedButton(onClick = {
                    errors = IndicatorCompiler.quickCheck(sourceCode)
                    compileResult = null
                }, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)) {
                    Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(2.dp))
                    Text("检查", fontSize = 12.sp)
                }
                // 编译生成
                Button(onClick = {
                    val result = IndicatorCompiler.compile(sourceCode)
                    if (result.errors.isEmpty()) {
                        onCompile(result.name, result.code)
                        compileResult = result.code
                    } else {
                        errors = result.errors
                        compileResult = null
                    }
                }, contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)) {
                    Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(2.dp))
                    Text("生成", fontSize = 12.sp)
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(horizontal = 8.dp)) {
            // 说明
            Text("编写麦语言指标公式，可引用 CLOSE/OPEN/HIGH/LOW/VOL 及 MA/EMA/MACD/RSI/KDJ 等函数。", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(bottom = 4.dp))

            // 代码编辑区
            OutlinedTextField(
                value = sourceCode, onValueChange = {
                    sourceCode = it
                    // 实时语法检查（超过5行且停止输入1秒后触发）
                    if (it.trim().lines().size >= 2) {
                        errors = IndicatorCompiler.quickCheck(it)
                        compileResult = null
                    }
                },
                modifier = Modifier.fillMaxWidth().weight(1f),
                textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = 13.sp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )

            // 错误列表
            if (errors.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = Error.copy(alpha = 0.08f)),
                    border = BorderStroke(1.dp, Error.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text("语法错误 (${errors.size})", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Error)
                        errors.take(8).forEach { err ->
                            Text("• $err", fontSize = 11.sp, color = Error.copy(alpha = 0.85f))
                        }
                        if (errors.size > 8) {
                            Text("... 还有 ${errors.size - 8} 个错误", fontSize = 11.sp, color = Color.Gray)
                        }
                    }
                }
            }

            // 编译成功预览
            if (compileResult != null) {
                Spacer(Modifier.height(4.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = Secondary.copy(alpha = 0.08f)),
                    border = BorderStroke(1.dp, Secondary.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, null, tint = Secondary, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("编译成功!", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Secondary)
                        }
                        Text(compileResult!!.lines().take(15).joinToString("\n"), fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurface, maxLines = 15)
                    }
                }
            }
        }
    }
}
