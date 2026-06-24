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
import com.marvis.mql.generator.StrategyGenerator
import com.marvis.mql.generator.TemplateLibrary
import com.marvis.mql.library.MqlLibrary
import com.marvis.mql.model.*
import com.marvis.mql.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConditionBuilderScreen(
    onGenerate: (String, String, Boolean) -> Unit
) {
    // 基础设置
    var strategyName by remember { mutableStateOf("MyStrategy") }
    var selectedPeriod by remember { mutableStateOf("1H") }
    var tradeDirection by remember { mutableStateOf(TradeDirection.LONG) }
    var capital by remember { mutableStateOf("100000") }

    // 模板选择
    var selectedTemplate by remember { mutableStateOf("") }
    var showTemplateDialog by remember { mutableStateOf(false) }

    // 多品种
    var multiEnabled by remember { mutableStateOf(false) }
    var multiInstruments by remember { mutableStateOf("RB,HC,RU") }

    // 绘图标注
    var drawEnabled by remember { mutableStateOf(false) }
    var drawEntryIcon by remember { mutableStateOf(true) }
    var drawExitIcon by remember { mutableStateOf(true) }
    var drawEntryIconType by remember { mutableStateOf("1") }
    var drawExitIconType by remember { mutableStateOf("2") }
    var drawEntryText by remember { mutableStateOf(false) }
    var drawEntryTextValue by remember { mutableStateOf("买入") }
    var drawExitText by remember { mutableStateOf(false) }
    var drawExitTextValue by remember { mutableStateOf("卖出") }
    var drawStopLine by remember { mutableStateOf(false) }

    // 金字塔分批建仓（终极级）
    var pyramidEnabled by remember { mutableStateOf(false) }
    var pyramidType by remember { mutableStateOf("PYRAMID") }
    var pyramidBaseLots by remember { mutableStateOf("1") }
    var pyramidLayers by remember { mutableStateOf("3") }
    var pyramidInterval by remember { mutableStateOf("ATR") }
    var pyramidIntervalValue by remember { mutableStateOf("1.0") }
    var pyramidDirection by remember { mutableStateOf("SAME") }
    var pyramidMultiplier by remember { mutableStateOf("1.0") }
    var pyramidAvgExit by remember { mutableStateOf(true) }

    // 日内风控熔断（终极级）
    var riskEnabled by remember { mutableStateOf(false) }
    var riskMaxDailyLoss by remember { mutableStateOf("5000") }
    var riskMaxDailyLossPct by remember { mutableStateOf("5.0") }
    var riskMaxConsecutive by remember { mutableStateOf("5") }
    var riskMaxTrades by remember { mutableStateOf("20") }
    var riskCooldown by remember { mutableStateOf("30") }
    var riskMaxPositions by remember { mutableStateOf("3") }
    var riskMaxTradeLoss by remember { mutableStateOf("1000") }

    // 参数优化（终极级）
    var optimEnabled by remember { mutableStateOf(false) }
    var optimObjective by remember { mutableStateOf("SHARPE_RATIO") }
    var optimGridSearch by remember { mutableStateOf(true) }
    var optimPopulation by remember { mutableStateOf("50") }
    var optimGenerations by remember { mutableStateOf("20") }

    // 策略组合（终极级）
    var groupEnabled by remember { mutableStateOf(false) }
    var groupMode by remember { mutableStateOf("AND") }
    var groupVoteThreshold by remember { mutableStateOf("0.5") }
    var groupSubIds by remember { mutableStateOf("") }
    var groupWeights by remember { mutableStateOf("") }
    var groupIgnoreExit by remember { mutableStateOf(false) }

    // 代码注解（生成选项）
    var annotateCode by remember { mutableStateOf(false) }

    // 指标选择
    var showIndicatorPicker by remember { mutableStateOf(false) }
    var pickingFor by remember { mutableStateOf("") }
    var currentGroupIndex by remember { mutableIntStateOf(0) }
    var currentCondIndex by remember { mutableIntStateOf(0) }

    // 条件组
    var entryGroups by remember { mutableStateOf(listOf(ConditionGroup())) }
    var exitGroups by remember { mutableStateOf(listOf(ConditionGroup())) }
    var shortEntryGroups by remember { mutableStateOf(listOf(ConditionGroup())) }
    var shortExitGroups by remember { mutableStateOf(listOf(ConditionGroup())) }

    // 止损止盈
    var stopType by remember { mutableStateOf("FIXED") }
    var stopValue by remember { mutableStateOf("2.0") }
    var stopATRPeriod by remember { mutableStateOf("14") }
    var stopATRMult by remember { mutableStateOf("2.0") }
    var stopEnabled by remember { mutableStateOf(true) }
    var tpType by remember { mutableStateOf("FIXED") }
    var tpValue by remember { mutableStateOf("5.0") }
    var tpATRMult by remember { mutableStateOf("3.0") }
    var tpRiskReward by remember { mutableStateOf("2.0") }
    var tpEnabled by remember { mutableStateOf(true) }

    // 移动止损
    var trailEnabled by remember { mutableStateOf(false) }
    var trailType by remember { mutableStateOf("ATR") }
    var trailATRPeriod by remember { mutableStateOf("14") }
    var trailATRMult by remember { mutableStateOf("3.0") }
    var trailActivation by remember { mutableStateOf("1.0") }

    // 资金管理
    var mmEnabled by remember { mutableStateOf(false) }
    var mmMode by remember { mutableStateOf("FIXED") }
    var mmFixedLots by remember { mutableStateOf("1") }
    var mmRiskPct by remember { mutableStateOf("2.0") }
    var mmMaxPos by remember { mutableStateOf("30") }
    var mmCompound by remember { mutableStateOf(false) }

    // 信号过滤
    var filterEnabled by remember { mutableStateOf(false) }
    var filterBars by remember { mutableStateOf("5") }
    var filterReverse by remember { mutableStateOf(false) }

    // 时间过滤
    var timeFilterEnabled by remember { mutableStateOf(false) }
    var timeStart by remember { mutableStateOf("09:00") }
    var timeEnd by remember { mutableStateOf("15:00") }
    var timeAvoidOvernight by remember { mutableStateOf(true) }

    // 跨周期引用
    var showCrossPeriod by remember { mutableStateOf(false) }
    var crossPeriodRefs by remember { mutableStateOf(listOf<CrossPeriodRef>()) }

    val operators = listOf("CROSS", "CROSS_UNDER", ">", "<", ">=", "<=", "==", "!=", "REF_UP", "REF_DOWN", "BREAK_HHV", "BREAK_LLV", "BARSLAST_GT", "MOD_EQ")
    val periods = listOf("1min", "5min", "15min", "30min", "1H", "2H", "4H", "1D", "1W", "1M")
    val indicatorList = MqlLibrary.allFunctions.filter {
        it.category !in listOf("交易指令", "跨周期引用")
    }
    val crossPeriods = listOf("MIN1", "MIN5", "MIN15", "MIN30", "HOUR1", "HOUR4", "DAY", "WEEK", "MONTH")
    val iconTypes = listOf("1" to "↑箭头", "2" to "↓箭头", "3" to "圆形", "4" to "笑脸", "5" to "三角")

    // ===== 模板选择对话框 =====
    if (showTemplateDialog) {
        AlertDialog(
            onDismissRequest = { showTemplateDialog = false },
            title = { Text("选择策略模板（大师级）") },
            text = {
                LazyColumn(modifier = Modifier.height(420.dp)) {
                    items(TemplateLibrary.allTemplates) { template ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable {
                                    val config = TemplateLibrary.buildConfig(template.id)
                                    selectedTemplate = template.id
                                    strategyName = config.name
                                    selectedPeriod = config.period
                                    tradeDirection = config.tradeDirection
                                    entryGroups = config.entryConditions
                                    exitGroups = config.exitConditions
                                    shortEntryGroups = config.shortEntryConditions
                                    shortExitGroups = config.shortExitConditions
                                    config.stopLoss?.let { sl -> stopType = sl.type; stopValue = sl.value.toString(); stopATRPeriod = sl.atrPeriod.toString(); stopATRMult = sl.atrMultiplier.toString(); stopEnabled = sl.enabled }
                                    config.takeProfit?.let { tp -> tpType = tp.type; tpValue = tp.value.toString(); tpATRMult = tp.atrMultiplier.toString(); tpRiskReward = tp.riskRewardRatio.toString(); tpEnabled = tp.enabled }
                                    config.trailingStop?.let { ts -> trailEnabled = ts.enabled; trailATRPeriod = ts.atrPeriod.toString(); trailATRMult = ts.atrMultiplier.toString(); trailActivation = ts.activationPercent.toString() }
                                    config.moneyManagement?.let { mm -> mmEnabled = mm.enabled; mmMode = mm.mode; mmFixedLots = mm.fixedLots.toString(); mmRiskPct = mm.riskPercent.toString(); mmCompound = mm.compoundProfit }
                                    config.signalFilter?.let { sf -> filterEnabled = sf.enabled; filterBars = sf.filterBars.toString(); filterReverse = sf.enableReverseSignal }
                                    showTemplateDialog = false
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = if (selectedTemplate == template.id) Primary.copy(alpha = 0.1f) else Surface
                            ),
                            border = if (selectedTemplate == template.id) BorderStroke(1.dp, Primary) else null
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(template.name, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Primary)
                                    Row {
                                        Text(template.category, fontSize = 10.sp, color = OnSurface)
                                        Spacer(Modifier.width(6.dp))
                                        Text(template.difficulty, fontSize = 10.sp, color = BuyColor)
                                    }
                                }
                                Spacer(Modifier.height(2.dp))
                                Text(template.description, fontSize = 12.sp, color = OnSurface)
                                if (template.params.isNotEmpty()) {
                                    Spacer(Modifier.height(2.dp))
                                    Text(template.params.joinToString(" | ") { "${it.label}: ${it.default}" }, fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = Color.Gray)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTemplateDialog = false }) { Text("关闭") }
            }
        )
    }

    // 指标选择对话框
    if (showIndicatorPicker) {
        AlertDialog(
            onDismissRequest = { showIndicatorPicker = false },
            title = { Text("选择指标/数据") },
            text = {
                LazyColumn(modifier = Modifier.height(400.dp)) {
                    items(indicatorList.chunked(2)) { row ->
                        Row(modifier = Modifier.fillMaxWidth()) {
                            row.forEach { func ->
                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(4.dp)
                                        .clickable {
                                            val mutable: MutableList<ConditionGroup>
                                            when {
                                                pickingFor.startsWith("entry") -> { mutable = entryGroups.toMutableList() }
                                                pickingFor.startsWith("shortEntry") -> { mutable = shortEntryGroups.toMutableList() }
                                                pickingFor.startsWith("shortExit") -> { mutable = shortExitGroups.toMutableList() }
                                                else -> { mutable = exitGroups.toMutableList() }
                                            }
                                            val group = mutable[currentGroupIndex].copy()
                                            val conds = group.conditions.toMutableList()
                                            if (currentCondIndex < conds.size) {
                                                val cond = conds[currentCondIndex].copy()
                                                if (pickingFor.contains("left")) {
                                                    conds[currentCondIndex] = cond.copy(leftIndicator = func.name)
                                                } else {
                                                    conds[currentCondIndex] = cond.copy(rightIndicator = func.name)
                                                }
                                            }
                                            mutable[currentGroupIndex] = group.copy(conditions = conds)
                                            when {
                                                pickingFor.startsWith("entry") -> { entryGroups = mutable }
                                                pickingFor.startsWith("shortEntry") -> { shortEntryGroups = mutable }
                                                pickingFor.startsWith("shortExit") -> { shortExitGroups = mutable }
                                                else -> { exitGroups = mutable }
                                            }
                                            showIndicatorPicker = false
                                        },
                                    colors = CardDefaults.cardColors(containerColor = Surface)
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Text(func.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Primary)
                                        Text(func.description, fontSize = 11.sp, color = OnSurface)
                                        Text(func.syntax, fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = Color.Gray)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showIndicatorPicker = false }) { Text("取消") }
            }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 20.dp)
    ) {
        // ========== 策略名称 ==========
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Surface)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Settings, null, tint = Primary, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("策略设置", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = strategyName, onValueChange = { strategyName = it },
                        label = { Text("策略名称") }, modifier = Modifier.fillMaxWidth(), singleLine = true
                    )
                    Spacer(Modifier.height(4.dp))
                    OutlinedTextField(
                        value = capital, onValueChange = { capital = it },
                        label = { Text("初始资金") }, modifier = Modifier.fillMaxWidth(), singleLine = true
                    )
                }
            }
        }

        // ========== 策略模板（大师级新增）==========
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Surface),
                border = BorderStroke(2.dp, Color(0xFFFF6F00))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Star, null, tint = Color(0xFFFF6F00), modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("大师级策略模板", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFFFF6F00))
                    }
                    Spacer(Modifier.height(4.dp))
                    Text("一键加载经典交易策略，自动填充所有参数", fontSize = 12.sp, color = OnSurface)
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        TemplateLibrary.allTemplates.take(4).forEach { t ->
                            FilterChip(
                                selected = selectedTemplate == t.id,
                                onClick = {
                                    if (selectedTemplate == t.id) { selectedTemplate = ""; return@FilterChip }
                                    val config = TemplateLibrary.buildConfig(t.id)
                                    selectedTemplate = t.id
                                    strategyName = config.name
                                    selectedPeriod = config.period
                                    tradeDirection = config.tradeDirection
                                    entryGroups = config.entryConditions
                                    exitGroups = config.exitConditions
                                    shortEntryGroups = config.shortEntryConditions
                                    shortExitGroups = config.shortExitConditions
                                    config.stopLoss?.let { sl -> stopType = sl.type; stopValue = sl.value.toString(); stopATRPeriod = sl.atrPeriod.toString(); stopATRMult = sl.atrMultiplier.toString(); stopEnabled = sl.enabled }
                                    config.takeProfit?.let { tp -> tpType = tp.type; tpValue = tp.value.toString(); tpATRMult = tp.atrMultiplier.toString(); tpRiskReward = tp.riskRewardRatio.toString(); tpEnabled = tp.enabled }
                                    config.trailingStop?.let { ts -> trailEnabled = ts.enabled; trailATRPeriod = ts.atrPeriod.toString(); trailATRMult = ts.atrMultiplier.toString(); trailActivation = ts.activationPercent.toString() }
                                    config.moneyManagement?.let { mm -> mmEnabled = mm.enabled; mmMode = mm.mode; mmFixedLots = mm.fixedLots.toString(); mmRiskPct = mm.riskPercent.toString(); mmCompound = mm.compoundProfit }
                                    config.signalFilter?.let { sf -> filterEnabled = sf.enabled; filterBars = sf.filterBars.toString(); filterReverse = sf.enableReverseSignal }
                                },
                                label = { Text(t.name, fontSize = 10.sp) }
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        TemplateLibrary.allTemplates.drop(4).forEach { t ->
                            FilterChip(
                                selected = selectedTemplate == t.id,
                                onClick = {
                                    if (selectedTemplate == t.id) { selectedTemplate = ""; return@FilterChip }
                                    val config = TemplateLibrary.buildConfig(t.id)
                                    selectedTemplate = t.id
                                    strategyName = config.name
                                    selectedPeriod = config.period
                                    tradeDirection = config.tradeDirection
                                    entryGroups = config.entryConditions
                                    exitGroups = config.exitConditions
                                    shortEntryGroups = config.shortEntryConditions
                                    shortExitGroups = config.shortExitConditions
                                    config.stopLoss?.let { sl -> stopType = sl.type; stopValue = sl.value.toString(); stopATRPeriod = sl.atrPeriod.toString(); stopATRMult = sl.atrMultiplier.toString(); stopEnabled = sl.enabled }
                                    config.takeProfit?.let { tp -> tpType = tp.type; tpValue = tp.value.toString(); tpATRMult = tp.atrMultiplier.toString(); tpRiskReward = tp.riskRewardRatio.toString(); tpEnabled = tp.enabled }
                                    config.trailingStop?.let { ts -> trailEnabled = ts.enabled; trailATRPeriod = ts.atrPeriod.toString(); trailATRMult = ts.atrMultiplier.toString(); trailActivation = ts.activationPercent.toString() }
                                    config.moneyManagement?.let { mm -> mmEnabled = mm.enabled; mmMode = mm.mode; mmFixedLots = mm.fixedLots.toString(); mmRiskPct = mm.riskPercent.toString(); mmCompound = mm.compoundProfit }
                                    config.signalFilter?.let { sf -> filterEnabled = sf.enabled; filterBars = sf.filterBars.toString(); filterReverse = sf.enableReverseSignal }
                                },
                                label = { Text(t.name, fontSize = 10.sp) }
                            )
                        }
                    }
                }
            }
        }

        // ========== 周期 + 方向 ==========
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Surface)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("运行周期", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        periods.forEach { p ->
                            FilterChip(selected = selectedPeriod == p, onClick = { selectedPeriod = p },
                                label = { Text(p, fontSize = 11.sp) })
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("交易方向", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TradeDirection.entries.forEach { d ->
                            FilterChip(
                                selected = tradeDirection == d,
                                onClick = { tradeDirection = d },
                                label = { Text(when(d) {
                                    TradeDirection.LONG -> "仅做多"
                                    TradeDirection.SHORT -> "仅做空"
                                    TradeDirection.BOTH -> "多空双向"
                                }, fontSize = 12.sp) }
                            )
                        }
                    }
                }
            }
        }

        // ========== 多头开仓 ==========
        item {
            SectionCard("多头开仓条件", BuyColor) {
                entryGroups.forEachIndexed { gi, group ->
                    ConditionGroupCard(gi, group, BuyColor, "开仓组${gi + 1}", operators,
                        onRemove = { if (entryGroups.size > 1) entryGroups = entryGroups.toMutableList().also { it.removeAt(gi) } },
                        onAddCondition = {
                            val gs = entryGroups.toMutableList()
                            gs[gi] = group.copy(conditions = group.conditions + SingleCondition())
                            entryGroups = gs
                        },
                        onRemoveCondition = { ci ->
                            val gs = entryGroups.toMutableList()
                            gs[gi] = group.copy(conditions = group.conditions.toMutableList().also { it.removeAt(ci) })
                            entryGroups = gs
                        },
                        onUpdateCondition = { ci, cond ->
                            val gs = entryGroups.toMutableList()
                            val cs = group.conditions.toMutableList(); cs[ci] = cond
                            gs[gi] = group.copy(conditions = cs); entryGroups = gs
                        },
                        onPickIndicator = { ci, side ->
                            currentGroupIndex = gi; currentCondIndex = ci
                            pickingFor = "entry_$side"; showIndicatorPicker = true
                        }
                    )
                }
                TextButton(onClick = { entryGroups = entryGroups + ConditionGroup() }) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp), tint = BuyColor)
                    Spacer(Modifier.width(4.dp)); Text("添加条件组", fontSize = 12.sp, color = BuyColor)
                }
            }
        }

        // ========== 多头平仓 ==========
        item {
            SectionCard("多头平仓条件", SellColor) {
                exitGroups.forEachIndexed { gi, group ->
                    ConditionGroupCard(gi, group, SellColor, "平仓组${gi + 1}", operators,
                        onRemove = { if (exitGroups.size > 1) exitGroups = exitGroups.toMutableList().also { it.removeAt(gi) } },
                        onAddCondition = {
                            val gs = exitGroups.toMutableList()
                            gs[gi] = group.copy(conditions = group.conditions + SingleCondition())
                            exitGroups = gs
                        },
                        onRemoveCondition = { ci ->
                            val gs = exitGroups.toMutableList()
                            gs[gi] = group.copy(conditions = group.conditions.toMutableList().also { it.removeAt(ci) })
                            exitGroups = gs
                        },
                        onUpdateCondition = { ci, cond ->
                            val gs = exitGroups.toMutableList()
                            val cs = group.conditions.toMutableList(); cs[ci] = cond
                            gs[gi] = group.copy(conditions = cs); exitGroups = gs
                        },
                        onPickIndicator = { ci, side ->
                            currentGroupIndex = gi; currentCondIndex = ci
                            pickingFor = "exit_$side"; showIndicatorPicker = true
                        }
                    )
                }
                TextButton(onClick = { exitGroups = exitGroups + ConditionGroup() }) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp), tint = SellColor)
                    Spacer(Modifier.width(4.dp)); Text("添加条件组", fontSize = 12.sp, color = SellColor)
                }
            }
        }

        // ========== 空头（根据交易方向显示）==========
        if (tradeDirection == TradeDirection.SHORT || tradeDirection == TradeDirection.BOTH) {
            item {
                SectionCard("空头开仓条件", Color(0xFFE65100)) {
                    shortEntryGroups.forEachIndexed { gi, group ->
                        ConditionGroupCard(gi, group, Color(0xFFE65100), "空开组${gi + 1}", operators,
                            onRemove = { if (shortEntryGroups.size > 1) shortEntryGroups = shortEntryGroups.toMutableList().also { it.removeAt(gi) } },
                            onAddCondition = {
                                val gs = shortEntryGroups.toMutableList()
                                gs[gi] = group.copy(conditions = group.conditions + SingleCondition())
                                shortEntryGroups = gs
                            },
                            onRemoveCondition = { ci ->
                                val gs = shortEntryGroups.toMutableList()
                                gs[gi] = group.copy(conditions = group.conditions.toMutableList().also { it.removeAt(ci) })
                                shortEntryGroups = gs
                            },
                            onUpdateCondition = { ci, cond ->
                                val gs = shortEntryGroups.toMutableList()
                                val cs = group.conditions.toMutableList(); cs[ci] = cond
                                gs[gi] = group.copy(conditions = cs); shortEntryGroups = gs
                            },
                            onPickIndicator = { ci, side ->
                                currentGroupIndex = gi; currentCondIndex = ci
                                pickingFor = "shortEntry_$side"; showIndicatorPicker = true
                            }
                        )
                    }
                    TextButton(onClick = { shortEntryGroups = shortEntryGroups + ConditionGroup() }) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp)); Text("添加条件组", fontSize = 12.sp)
                    }
                }
            }
            item {
                SectionCard("空头平仓条件", Color(0xFF1565C0)) {
                    shortExitGroups.forEachIndexed { gi, group ->
                        ConditionGroupCard(gi, group, Color(0xFF1565C0), "空平组${gi + 1}", operators,
                            onRemove = { if (shortExitGroups.size > 1) shortExitGroups = shortExitGroups.toMutableList().also { it.removeAt(gi) } },
                            onAddCondition = {
                                val gs = shortExitGroups.toMutableList()
                                gs[gi] = group.copy(conditions = group.conditions + SingleCondition())
                                shortExitGroups = gs
                            },
                            onRemoveCondition = { ci ->
                                val gs = shortExitGroups.toMutableList()
                                gs[gi] = group.copy(conditions = group.conditions.toMutableList().also { it.removeAt(ci) })
                                shortExitGroups = gs
                            },
                            onUpdateCondition = { ci, cond ->
                                val gs = shortExitGroups.toMutableList()
                                val cs = group.conditions.toMutableList(); cs[ci] = cond
                                gs[gi] = group.copy(conditions = cs); shortExitGroups = gs
                            },
                            onPickIndicator = { ci, side ->
                                currentGroupIndex = gi; currentCondIndex = ci
                                pickingFor = "shortExit_$side"; showIndicatorPicker = true
                            }
                        )
                    }
                    TextButton(onClick = { shortExitGroups = shortExitGroups + ConditionGroup() }) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp)); Text("添加条件组", fontSize = 12.sp)
                    }
                }
            }
        }

        // ========== 止损止盈 ==========
        item {
            var expanded by remember { mutableStateOf(true) }
            ExpandableCard("止损止盈配置", expanded, onToggle = { expanded = it }) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = stopEnabled, onCheckedChange = { stopEnabled = it })
                    Text("止损", fontWeight = FontWeight.Medium)
                    Spacer(Modifier.width(4.dp))
                    listOf("FIXED" to "固定", "ATR" to "ATR", "PERCENT" to "%", "MOVING_AVG" to "均线", "LOW_HIGH" to "高低点").forEach { (t, l) ->
                        FilterChip(selected = stopType == t, onClick = { stopType = t }, label = { Text(l, fontSize = 10.sp) })
                    }
                    Spacer(Modifier.width(4.dp))
                    OutlinedTextField(value = stopValue, onValueChange = { stopValue = it }, modifier = Modifier.width(60.dp), singleLine = true, textStyle = LocalTextStyle.current.copy(fontSize = 12.sp))
                }
                if (stopType == "ATR") {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("周期:", fontSize = 12.sp)
                        OutlinedTextField(value = stopATRPeriod, onValueChange = { stopATRPeriod = it }, modifier = Modifier.width(60.dp), singleLine = true, textStyle = LocalTextStyle.current.copy(fontSize = 11.sp))
                        Spacer(Modifier.width(12.dp))
                        Text("倍数:", fontSize = 12.sp)
                        OutlinedTextField(value = stopATRMult, onValueChange = { stopATRMult = it }, modifier = Modifier.width(60.dp), singleLine = true, textStyle = LocalTextStyle.current.copy(fontSize = 11.sp))
                    }
                }
                Spacer(Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = tpEnabled, onCheckedChange = { tpEnabled = it })
                    Text("止盈", fontWeight = FontWeight.Medium)
                    Spacer(Modifier.width(4.dp))
                    listOf("FIXED" to "固定", "PERCENT" to "%", "ATR" to "ATR", "RISK_REWARD" to "盈亏比", "FIBONACCI" to "斐波那契").forEach { (t, l) ->
                        FilterChip(selected = tpType == t, onClick = { tpType = t }, label = { Text(l, fontSize = 10.sp) })
                    }
                    Spacer(Modifier.width(4.dp))
                    OutlinedTextField(value = tpValue, onValueChange = { tpValue = it }, modifier = Modifier.width(60.dp), singleLine = true, textStyle = LocalTextStyle.current.copy(fontSize = 12.sp))
                }
                if (tpType == "ATR") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("倍数:", fontSize = 12.sp)
                        OutlinedTextField(value = tpATRMult, onValueChange = { tpATRMult = it }, modifier = Modifier.width(60.dp), singleLine = true, textStyle = LocalTextStyle.current.copy(fontSize = 11.sp))
                    }
                }
                if (tpType == "RISK_REWARD") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("盈亏比:", fontSize = 12.sp)
                        OutlinedTextField(value = tpRiskReward, onValueChange = { tpRiskReward = it }, modifier = Modifier.width(60.dp), singleLine = true, textStyle = LocalTextStyle.current.copy(fontSize = 11.sp))
                    }
                }
            }
        }

        // ========== 移动止损 ==========
        item {
            var expanded by remember { mutableStateOf(false) }
            ExpandableCard("移动止损（进阶）", expanded, onToggle = { expanded = it }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = trailEnabled, onCheckedChange = { trailEnabled = it })
                    Text("启用移动止损", fontWeight = FontWeight.Medium)
                }
                if (trailEnabled) {
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column {
                            Text("ATR周期", fontSize = 11.sp)
                            OutlinedTextField(value = trailATRPeriod, onValueChange = { trailATRPeriod = it }, modifier = Modifier.width(60.dp), singleLine = true, textStyle = LocalTextStyle.current.copy(fontSize = 11.sp))
                        }
                        Column {
                            Text("ATR倍数", fontSize = 11.sp)
                            OutlinedTextField(value = trailATRMult, onValueChange = { trailATRMult = it }, modifier = Modifier.width(60.dp), singleLine = true, textStyle = LocalTextStyle.current.copy(fontSize = 11.sp))
                        }
                        Column {
                            Text("激活阈值(%)", fontSize = 11.sp)
                            OutlinedTextField(value = trailActivation, onValueChange = { trailActivation = it }, modifier = Modifier.width(70.dp), singleLine = true, textStyle = LocalTextStyle.current.copy(fontSize = 11.sp))
                        }
                    }
                }
            }
        }

        // ========== 资金管理 ==========
        item {
            var expanded by remember { mutableStateOf(false) }
            ExpandableCard("资金管理（进阶）", expanded, onToggle = { expanded = it }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = mmEnabled, onCheckedChange = { mmEnabled = it })
                    Text("启用资金管理", fontWeight = FontWeight.Medium)
                }
                if (mmEnabled) {
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("FIXED" to "固定手数", "PERCENT_RISK" to "风险%", "KELLY" to "凯利", "FIXED_RATIO" to "固定比例", "MARTINGALE" to "马丁").forEach { (t, l) ->
                            FilterChip(selected = mmMode == t, onClick = { mmMode = t }, label = { Text(l, fontSize = 10.sp) })
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (mmMode == "FIXED" || mmMode == "MARTINGALE") {
                            Column { Text("手数", fontSize = 11.sp); OutlinedTextField(value = mmFixedLots, onValueChange = { mmFixedLots = it }, modifier = Modifier.width(60.dp), singleLine = true, textStyle = LocalTextStyle.current.copy(fontSize = 11.sp)) }
                        }
                        if (mmMode == "PERCENT_RISK") {
                            Column { Text("风险%", fontSize = 11.sp); OutlinedTextField(value = mmRiskPct, onValueChange = { mmRiskPct = it }, modifier = Modifier.width(60.dp), singleLine = true, textStyle = LocalTextStyle.current.copy(fontSize = 11.sp)) }
                        }
                        Column { Text("最大持仓%", fontSize = 11.sp); OutlinedTextField(value = mmMaxPos, onValueChange = { mmMaxPos = it }, modifier = Modifier.width(60.dp), singleLine = true, textStyle = LocalTextStyle.current.copy(fontSize = 11.sp)) }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = mmCompound, onCheckedChange = { mmCompound = it })
                        Text("复利模式", fontSize = 12.sp)
                    }
                }
            }
        }

        // ========== 信号过滤 ==========
        item {
            var expanded by remember { mutableStateOf(false) }
            ExpandableCard("信号过滤（进阶）", expanded, onToggle = { expanded = it }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = filterEnabled, onCheckedChange = { filterEnabled = it })
                    Text("启用信号过滤", fontWeight = FontWeight.Medium)
                }
                if (filterEnabled) {
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("过滤周期:", fontSize = 12.sp)
                        OutlinedTextField(value = filterBars, onValueChange = { filterBars = it }, modifier = Modifier.width(60.dp), singleLine = true, textStyle = LocalTextStyle.current.copy(fontSize = 11.sp))
                        Text("根K线内重复信号", fontSize = 11.sp, color = OnSurface)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = filterReverse, onCheckedChange = { filterReverse = it })
                        Text("反向信号自动平仓", fontSize = 12.sp)
                    }
                }
            }
        }

        // ========== 时间过滤 ==========
        item {
            var expanded by remember { mutableStateOf(false) }
            ExpandableCard("时间过滤（进阶）", expanded, onToggle = { expanded = it }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = timeFilterEnabled, onCheckedChange = { timeFilterEnabled = it })
                    Text("仅在指定时段交易", fontWeight = FontWeight.Medium)
                }
                if (timeFilterEnabled) {
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column { Text("开始", fontSize = 11.sp); OutlinedTextField(value = timeStart, onValueChange = { timeStart = it }, modifier = Modifier.width(70.dp), singleLine = true, textStyle = LocalTextStyle.current.copy(fontSize = 11.sp)) }
                        Column { Text("结束", fontSize = 11.sp); OutlinedTextField(value = timeEnd, onValueChange = { timeEnd = it }, modifier = Modifier.width(70.dp), singleLine = true, textStyle = LocalTextStyle.current.copy(fontSize = 11.sp)) }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = timeAvoidOvernight, onCheckedChange = { timeAvoidOvernight = it })
                        Text("收盘前5分钟强制平仓", fontSize = 12.sp)
                    }
                }
            }
        }

        // ========== 跨周期引用 ==========
        item {
            var expanded by remember { mutableStateOf(false) }
            ExpandableCard("跨周期引用（进阶）", expanded, onToggle = { expanded = it }) {
                crossPeriodRefs.forEachIndexed { idx, ref ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        var p by remember { mutableStateOf(ref.period) }
                        var indi by remember { mutableStateOf(ref.indicator) }
                        var al by remember { mutableStateOf(ref.alias) }
                        var par by remember { mutableStateOf(ref.params.joinToString(",")) }

                        FilterChip(selected = false, onClick = { }, label = { Text(p, fontSize = 10.sp) })
                        Spacer(Modifier.width(4.dp))
                        OutlinedTextField(value = indi, onValueChange = { indi = it
                            val m = crossPeriodRefs.toMutableList(); m[idx] = CrossPeriodRef(p, indi, al, par.split(",").map { it.trim() }); crossPeriodRefs = m
                        }, modifier = Modifier.weight(1f), singleLine = true, textStyle = LocalTextStyle.current.copy(fontSize = 11.sp), label = { Text("指标", fontSize = 10.sp) })
                        Spacer(Modifier.width(4.dp))
                        OutlinedTextField(value = al, onValueChange = { al = it
                            val m = crossPeriodRefs.toMutableList(); m[idx] = CrossPeriodRef(p, indi, al, par.split(",").map { it.trim() }); crossPeriodRefs = m
                        }, modifier = Modifier.weight(0.7f), singleLine = true, textStyle = LocalTextStyle.current.copy(fontSize = 11.sp), label = { Text("别名", fontSize = 10.sp) })
                        IconButton(onClick = {
                            crossPeriodRefs = crossPeriodRefs.toMutableList().also { it.removeAt(idx) }
                        }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Close, "删除", tint = Error, modifier = Modifier.size(16.dp))
                        }
                    }
                }
                TextButton(onClick = {
                    crossPeriodRefs = crossPeriodRefs + CrossPeriodRef("DAY", "MA", "REF${crossPeriodRefs.size + 1}", listOf("10"))
                }) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp)); Text("添加跨周期引用", fontSize = 12.sp)
                }
            }
        }

        // ========== 多品种并发（大师级新增）==========
        item {
            var expanded by remember { mutableStateOf(false) }
            ExpandableCard("多品种并发（大师级）", expanded, onToggle = { expanded = it }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = multiEnabled, onCheckedChange = { multiEnabled = it })
                    Text("启用多品种并发", fontWeight = FontWeight.Medium)
                }
                if (multiEnabled) {
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = multiInstruments,
                        onValueChange = { multiInstruments = it },
                        label = { Text("品种代码（逗号分隔）", fontSize = 11.sp) },
                        placeholder = { Text("例: RB,HC,RU,M", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(fontSize = 12.sp)
                    )
                    Spacer(Modifier.height(2.dp))
                    Text("策略将标注适用品种，可在量化平台批量加载到多个合约", fontSize = 10.sp, color = OnSurface)
                }
            }
        }

        // ========== 绘图标注（大师级新增）==========
        item {
            var expanded by remember { mutableStateOf(false) }
            ExpandableCard("绘图标注（大师级）", expanded, onToggle = { expanded = it }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = drawEnabled, onCheckedChange = { drawEnabled = it })
                    Text("启用K线图标注", fontWeight = FontWeight.Medium, color = Color(0xFFFF6F00))
                }
                if (drawEnabled) {
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = drawEntryIcon, onCheckedChange = { drawEntryIcon = it })
                        Text("开仓图标", fontSize = 12.sp)
                        Spacer(Modifier.width(8.dp))
                        iconTypes.forEach { (t, l) ->
                            FilterChip(selected = drawEntryIconType == t, onClick = { drawEntryIconType = t }, label = { Text(l, fontSize = 10.sp) })
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = drawExitIcon, onCheckedChange = { drawExitIcon = it })
                        Text("平仓图标", fontSize = 12.sp)
                        Spacer(Modifier.width(8.dp))
                        iconTypes.forEach { (t, l) ->
                            FilterChip(selected = drawExitIconType == t, onClick = { drawExitIconType = t }, label = { Text(l, fontSize = 10.sp) })
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = drawEntryText, onCheckedChange = { drawEntryText = it })
                        Text("开仓文字:", fontSize = 12.sp)
                        if (drawEntryText) {
                            OutlinedTextField(value = drawEntryTextValue, onValueChange = { drawEntryTextValue = it }, modifier = Modifier.width(80.dp), singleLine = true, textStyle = LocalTextStyle.current.copy(fontSize = 11.sp))
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = drawExitText, onCheckedChange = { drawExitText = it })
                        Text("平仓文字:", fontSize = 12.sp)
                        if (drawExitText) {
                            OutlinedTextField(value = drawExitTextValue, onValueChange = { drawExitTextValue = it }, modifier = Modifier.width(80.dp), singleLine = true, textStyle = LocalTextStyle.current.copy(fontSize = 11.sp))
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = drawStopLine, onCheckedChange = { drawStopLine = it })
                        Text("止损线标注", fontSize = 12.sp)
                    }
                }
            }
        }

        // ========== 金字塔分批建仓（终极级）==========
        item {
            var pyramidExpanded by remember { mutableStateOf(false) }
            ExpandableCard("金字塔分批建仓（终极级）", pyramidExpanded, onToggle = { pyramidExpanded = it }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = pyramidEnabled, onCheckedChange = { pyramidEnabled = it })
                    Text("启用金字塔分批建仓", fontWeight = FontWeight.Medium)
                }
                if (pyramidEnabled) {
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("模式:", fontSize = 12.sp, modifier = Modifier.width(60.dp))
                        listOf("PYRAMID" to "金字塔", "GRID" to "网格", "SCALED" to "比例", "MARTIN" to "马丁").forEach { (k, v) ->
                            FilterChip(selected = pyramidType == k, onClick = { pyramidType = k }, label = { Text(v, fontSize = 11.sp) }, modifier = Modifier.padding(end = 4.dp))
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Row {
                        OutlinedTextField(value = pyramidLayers, onValueChange = { pyramidLayers = it }, label = { Text("层数") }, modifier = Modifier.weight(1f), singleLine = true, textStyle = LocalTextStyle.current.copy(fontSize = 11.sp))
                        Spacer(Modifier.width(6.dp))
                        OutlinedTextField(value = pyramidBaseLots, onValueChange = { pyramidBaseLots = it }, label = { Text("基础手数") }, modifier = Modifier.weight(1f), singleLine = true, textStyle = LocalTextStyle.current.copy(fontSize = 11.sp))
                        Spacer(Modifier.width(6.dp))
                        OutlinedTextField(value = pyramidMultiplier, onValueChange = { pyramidMultiplier = it }, label = { Text("倍率×") }, modifier = Modifier.weight(1f), singleLine = true, textStyle = LocalTextStyle.current.copy(fontSize = 11.sp))
                    }
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("间隔:", fontSize = 12.sp, modifier = Modifier.width(60.dp))
                        listOf("ATR" to "ATR", "PERCENT" to "%", "FIXED" to "固定").forEach { (k, v) ->
                            FilterChip(selected = pyramidInterval == k, onClick = { pyramidInterval = k }, label = { Text(v, fontSize = 11.sp) }, modifier = Modifier.padding(end = 4.dp))
                        }
                        OutlinedTextField(value = pyramidIntervalValue, onValueChange = { pyramidIntervalValue = it }, modifier = Modifier.weight(1f), singleLine = true, textStyle = LocalTextStyle.current.copy(fontSize = 11.sp))
                    }
                    Spacer(Modifier.height(4.dp))
                    Row {
                        Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(checked = pyramidAvgExit, onCheckedChange = { pyramidAvgExit = it }); Text("均价平仓", fontSize = 11.sp) }
                    }
                }
            }
        }

        // ========== 日内风控熔断（终极级）==========
        item {
            var riskExpanded by remember { mutableStateOf(false) }
            ExpandableCard("日内风控熔断（终极级）", riskExpanded, onToggle = { riskExpanded = it }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = riskEnabled, onCheckedChange = { riskEnabled = it })
                    Text("启用日内风控熔断", fontWeight = FontWeight.Medium)
                }
                if (riskEnabled) {
                    Spacer(Modifier.height(6.dp))
                    Row {
                        OutlinedTextField(value = riskMaxDailyLoss, onValueChange = { riskMaxDailyLoss = it }, label = { Text("日内最大亏损") }, modifier = Modifier.weight(1f), singleLine = true, textStyle = LocalTextStyle.current.copy(fontSize = 11.sp))
                        Spacer(Modifier.width(6.dp))
                        OutlinedTextField(value = riskMaxDailyLossPct, onValueChange = { riskMaxDailyLossPct = it }, label = { Text("亏损%") }, modifier = Modifier.weight(1f), singleLine = true, textStyle = LocalTextStyle.current.copy(fontSize = 11.sp))
                    }
                    Spacer(Modifier.height(4.dp))
                    Row {
                        OutlinedTextField(value = riskMaxConsecutive, onValueChange = { riskMaxConsecutive = it }, label = { Text("连亏笔数") }, modifier = Modifier.weight(1f), singleLine = true, textStyle = LocalTextStyle.current.copy(fontSize = 11.sp))
                        Spacer(Modifier.width(6.dp))
                        OutlinedTextField(value = riskMaxTrades, onValueChange = { riskMaxTrades = it }, label = { Text("日内最大交易") }, modifier = Modifier.weight(1f), singleLine = true, textStyle = LocalTextStyle.current.copy(fontSize = 11.sp))
                    }
                    Spacer(Modifier.height(4.dp))
                    Row {
                        OutlinedTextField(value = riskMaxPositions, onValueChange = { riskMaxPositions = it }, label = { Text("最大持仓") }, modifier = Modifier.weight(1f), singleLine = true, textStyle = LocalTextStyle.current.copy(fontSize = 11.sp))
                        Spacer(Modifier.width(6.dp))
                        OutlinedTextField(value = riskMaxTradeLoss, onValueChange = { riskMaxTradeLoss = it }, label = { Text("单笔最大亏损") }, modifier = Modifier.weight(1f), singleLine = true, textStyle = LocalTextStyle.current.copy(fontSize = 11.sp))
                    }
                }
            }
        }

        // ========== 参数优化（终极级）==========
        item {
            var optimExpanded by remember { mutableStateOf(false) }
            ExpandableCard("参数优化配置（终极级）", optimExpanded, onToggle = { optimExpanded = it }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = optimEnabled, onCheckedChange = { optimEnabled = it })
                    Text("启用参数优化注解", fontWeight = FontWeight.Medium)
                }
                if (optimEnabled) {
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("目标:", fontSize = 12.sp, modifier = Modifier.width(60.dp))
                        listOf("SHARPE_RATIO" to "夏普", "PROFIT_FACTOR" to "盈利因子", "NET_PROFIT" to "净利润", "WIN_RATE" to "胜率").forEach { (k, v) ->
                            FilterChip(selected = optimObjective == k, onClick = { optimObjective = k }, label = { Text(v, fontSize = 11.sp) }, modifier = Modifier.padding(end = 4.dp))
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = optimGridSearch, onCheckedChange = { optimGridSearch = it })
                        Text("网格搜索（关闭则用遗传算法）", fontSize = 11.sp)
                    }
                    if (!optimGridSearch) {
                        Row {
                            OutlinedTextField(value = optimPopulation, onValueChange = { optimPopulation = it }, label = { Text("种群") }, modifier = Modifier.weight(1f), singleLine = true, textStyle = LocalTextStyle.current.copy(fontSize = 11.sp))
                            Spacer(Modifier.width(6.dp))
                            OutlinedTextField(value = optimGenerations, onValueChange = { optimGenerations = it }, label = { Text("代数") }, modifier = Modifier.weight(1f), singleLine = true, textStyle = LocalTextStyle.current.copy(fontSize = 11.sp))
                        }
                    }
                }
            }
        }

        // ========== 策略组合（终极级）==========
        item {
            var groupExpanded by remember { mutableStateOf(false) }
            ExpandableCard("策略组合引擎（终极级）", groupExpanded, onToggle = { groupExpanded = it }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = groupEnabled, onCheckedChange = { groupEnabled = it })
                    Text("启用策略组合", fontWeight = FontWeight.Medium)
                }
                if (groupEnabled) {
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("模式:", fontSize = 12.sp, modifier = Modifier.width(60.dp))
                        listOf("AND" to "与", "OR" to "或", "VOTE" to "投票", "WEIGHTED" to "加权").forEach { (k, v) ->
                            FilterChip(selected = groupMode == k, onClick = { groupMode = k }, label = { Text(v, fontSize = 11.sp) }, modifier = Modifier.padding(end = 4.dp))
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    OutlinedTextField(value = groupSubIds, onValueChange = { groupSubIds = it }, label = { Text("子策略ID（逗号分隔）") }, modifier = Modifier.fillMaxWidth(), singleLine = true, textStyle = LocalTextStyle.current.copy(fontSize = 11.sp))
                    if (groupMode in listOf("VOTE", "WEIGHTED")) {
                        OutlinedTextField(value = groupVoteThreshold, onValueChange = { groupVoteThreshold = it }, label = { Text("阈值(0~1)") }, modifier = Modifier.fillMaxWidth(), singleLine = true, textStyle = LocalTextStyle.current.copy(fontSize = 11.sp))
                    }
                    if (groupMode == "WEIGHTED") {
                        OutlinedTextField(value = groupWeights, onValueChange = { groupWeights = it }, label = { Text("权重（逗号分隔）") }, modifier = Modifier.fillMaxWidth(), singleLine = true, textStyle = LocalTextStyle.current.copy(fontSize = 11.sp))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = groupIgnoreExit, onCheckedChange = { groupIgnoreExit = it })
                        Text("忽略子策略平仓信号", fontSize = 11.sp)
                    }
                }
            }
        }

        // ========== 生成选项 ==========
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
                Checkbox(checked = annotateCode, onCheckedChange = { annotateCode = it })
                Text("自动添加逐行中文注释", fontSize = 13.sp, color = Color.Gray)
            }
        }

        // ========== 生成按钮 ==========
        item {
            Button(
                onClick = {
                    val config = StrategyConfig(
                        name = strategyName,
                        period = selectedPeriod,
                        capital = capital.toDoubleOrNull() ?: 100000.0,
                        tradeDirection = tradeDirection,
                        entryConditions = entryGroups,
                        exitConditions = exitGroups,
                        shortEntryConditions = if (tradeDirection == TradeDirection.SHORT || tradeDirection == TradeDirection.BOTH) shortEntryGroups else emptyList(),
                        shortExitConditions = if (tradeDirection == TradeDirection.SHORT || tradeDirection == TradeDirection.BOTH) shortExitGroups else emptyList(),
                        stopLoss = StopLossConfig(stopType, stopValue.toDoubleOrNull() ?: 2.0, stopATRPeriod.toIntOrNull() ?: 14, stopATRMult.toDoubleOrNull() ?: 2.0, stopEnabled),
                        takeProfit = TakeProfitConfig(tpType, tpValue.toDoubleOrNull() ?: 5.0, tpATRMult.toDoubleOrNull() ?: 3.0, tpRiskReward.toDoubleOrNull() ?: 2.0, tpEnabled),
                        trailingStop = TrailingStopConfig(trailType, trailATRMult.toDoubleOrNull() ?: 3.0, trailATRPeriod.toIntOrNull() ?: 14, trailATRMult.toDoubleOrNull() ?: 3.0, trailActivation.toDoubleOrNull() ?: 1.0, trailEnabled),
                        moneyManagement = MoneyManagementConfig(mmMode, mmFixedLots.toIntOrNull() ?: 1, mmRiskPct.toDoubleOrNull() ?: 2.0, mmMaxPos.toDoubleOrNull() ?: 30.0, capital.toDoubleOrNull() ?: 100000.0, mmCompound, mmEnabled),
                        signalFilter = SignalFilterConfig(true, filterBars.toIntOrNull() ?: 5, true, filterReverse, filterEnabled),
                        timeFilter = TimeFilterConfig(timeStart, timeEnd, timeAvoidOvernight, timeFilterEnabled),
                        crossPeriodRefs = crossPeriodRefs,
                        backtestConfig = BacktestConfig(initialCapital = capital.toDoubleOrNull() ?: 100000.0),
                        multiInstrument = MultiInstrumentConfig(multiInstruments.split(",").map { it.trim() }.filter { it.isNotEmpty() }, enabled = multiEnabled),
                        drawing = DrawingConfig(drawEntryIcon, drawExitIcon, drawEntryIconType.toIntOrNull() ?: 1, drawExitIconType.toIntOrNull() ?: 2, drawEntryText, drawEntryTextValue, drawExitText, drawExitTextValue, drawStopLine, drawEnabled),
                        templateId = selectedTemplate,
                        pyramid = PyramidConfig(pyramidType, pyramidBaseLots.toIntOrNull() ?: 1, pyramidLayers.toIntOrNull() ?: 3, pyramidInterval, pyramidIntervalValue.toDoubleOrNull() ?: 1.0, pyramidDirection, pyramidMultiplier.toDoubleOrNull() ?: 1.0, false, pyramidAvgExit, pyramidEnabled),
                        riskControl = RiskControlConfig(riskMaxDailyLoss.toDoubleOrNull() ?: 5000.0, riskMaxDailyLossPct.toDoubleOrNull() ?: 5.0, riskMaxConsecutive.toIntOrNull() ?: 5, riskMaxTrades.toIntOrNull() ?: 20, riskCooldown.toIntOrNull() ?: 30, riskMaxPositions.toIntOrNull() ?: 3, riskMaxTradeLoss.toDoubleOrNull() ?: 1000.0, riskEnabled),
                        paramOptimization = ParamOptimization(optimGridSearch, emptyList(), optimObjective, optimPopulation.toIntOrNull() ?: 50, optimGenerations.toIntOrNull() ?: 20, optimEnabled),
                        strategyGroup = StrategyGroupConfig(groupSubIds.split(",").map { it.trim() }.filter { it.isNotEmpty() }, groupMode, groupVoteThreshold.toDoubleOrNull() ?: 0.5, groupWeights.split(",").mapNotNull { it.trim().toDoubleOrNull() }, groupIgnoreExit, groupEnabled),
                        annotateCode = annotateCode
                    )
                    val result = StrategyGenerator().generate(config)
                    onGenerate("策略: ${result.strategyName}${if (selectedTemplate.isNotEmpty()) " [${selectedTemplate}]" else ""}", result.sourceCode, true)
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = when {
                        pyramidEnabled || riskEnabled || optimEnabled || groupEnabled -> Color(0xFFB71C1C)
                        selectedTemplate.isNotEmpty() -> Color(0xFFFF6F00)
                        else -> Primary
                    }
                )
            ) {
                Icon(Icons.Default.PlayArrow, null)
                Spacer(Modifier.width(8.dp))
                Text(
                    when {
                        pyramidEnabled || riskEnabled || optimEnabled || groupEnabled -> "生成终极级交易脚本"
                        selectedTemplate.isNotEmpty() -> "生成大师级交易脚本"
                        else -> "生成专业交易脚本"
                    },
                    fontSize = 16.sp, fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// ================================================================
// 辅助 Composables
// ================================================================

@Composable
private fun SectionCard(title: String, borderColor: Color, content: @Composable ColumnScope.() -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Surface),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = borderColor)
            Spacer(Modifier.height(6.dp))
            content()
        }
    }
}

@Composable
private fun ExpandableCard(title: String, expanded: Boolean, onToggle: (Boolean) -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Surface)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().clickable { onToggle(!expanded) },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    "展开/收起",
                    tint = OnSurface
                )
            }
            if (expanded) {
                Spacer(Modifier.height(6.dp))
                content()
            }
        }
    }
}

@Composable
fun ConditionGroupCard(
    groupIndex: Int, group: ConditionGroup, color: Color, label: String,
    operators: List<String>,
    onRemove: () -> Unit, onAddCondition: () -> Unit, onRemoveCondition: (Int) -> Unit,
    onUpdateCondition: (Int, SingleCondition) -> Unit, onPickIndicator: (Int, String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Background),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(label, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = color)
                Row {
                    TextButton(onClick = onAddCondition) { Text("+条件", fontSize = 12.sp) }
                    TextButton(onClick = onRemove) { Text("删除组", fontSize = 12.sp, color = Error) }
                }
            }
            group.conditions.forEachIndexed { ci, cond ->
                ConditionRow(ci, cond, operators,
                    onPick = { side -> onPickIndicator(ci, side) },
                    onUpdate = { onUpdateCondition(ci, it) },
                    onRemove = { onRemoveCondition(ci) }
                )
            }
        }
    }
}

@Composable
fun ConditionRow(
    ci: Int, cond: SingleCondition, operators: List<String>,
    onPick: (String) -> Unit, onUpdate: (SingleCondition) -> Unit, onRemove: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var leftParamStr by remember { mutableStateOf(cond.leftParams.joinToString(",")) }
    var rightParamStr by remember { mutableStateOf(cond.rightParams.joinToString(",")) }

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Surface)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("${ci + 1}.", fontSize = 12.sp, modifier = Modifier.width(20.dp))
            OutlinedButton(
                onClick = { onPick("left") },
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
            ) { Text(cond.leftIndicator.ifEmpty { "选择指标" }, fontSize = 11.sp, maxLines = 1) }
            Box(modifier = Modifier.width(60.dp)) {
                OutlinedButton(
                    onClick = { expanded = true },
                    contentPadding = PaddingValues(horizontal = 2.dp, vertical = 2.dp)
                ) { Text(cond.operator.ifEmpty { ">" }, fontSize = 10.sp) }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    operators.forEach { op ->
                        DropdownMenuItem(
                            text = { Text(op, fontSize = 13.sp) },
                            onClick = { onUpdate(cond.copy(operator = op)); expanded = false }
                        )
                    }
                }
            }
            OutlinedButton(
                onClick = { onPick("right") },
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
            ) { Text(cond.rightIndicator.ifEmpty { "选择指标" }, fontSize = 11.sp, maxLines = 1) }
            IconButton(onClick = onRemove, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.Close, "删除", tint = Error, modifier = Modifier.size(16.dp))
            }
        }
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp)) {
            OutlinedTextField(
                value = leftParamStr, onValueChange = { leftParamStr = it; onUpdate(cond.copy(leftParams = it.split(",").map { s -> s.trim() })) },
                label = { Text("左参数", fontSize = 10.sp) }, modifier = Modifier.weight(1f).height(50.dp), singleLine = true,
                textStyle = LocalTextStyle.current.copy(fontSize = 11.sp)
            )
            Spacer(Modifier.width(4.dp))
            OutlinedTextField(
                value = rightParamStr, onValueChange = { rightParamStr = it; onUpdate(cond.copy(rightParams = it.split(",").map { s -> s.trim() })) },
                label = { Text("右参数", fontSize = 10.sp) }, modifier = Modifier.weight(1f).height(50.dp), singleLine = true,
                textStyle = LocalTextStyle.current.copy(fontSize = 11.sp)
            )
        }
    }
}
