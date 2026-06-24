package com.marvis.mql.generator

import com.marvis.mql.model.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * 麦语言大师级交易策略代码生成器（Level 7）
 * 新增：策略模板 | 多品种并发 | 绘图标注 | IF/VALUEWHEN/MOD | 8套经典模板
 */
class StrategyGenerator {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    fun generate(config: StrategyConfig): GenerationResult {
        val errors = mutableListOf<String>()
        val sb = StringBuilder()

        try {
            // ========== 头部注释 ==========
            sb.appendLine("// ╔══════════════════════════════════════════════════╗")
            sb.appendLine("// ║  策略名称 : ${config.name.padEnd(42)}║")
            sb.appendLine("// ║  运行周期 : ${config.period.padEnd(42)}║")
            sb.appendLine("// ║  交易方向 : ${config.tradeDirection.name.padEnd(42)}║")
            sb.appendLine("// ║  生成时间 : ${dateFormat.format(Date()).padEnd(42)}║")
            config.templateId.takeIf { it.isNotEmpty() }?.let {
                sb.appendLine("// ║  策略模板 : ${TemplateLibrary.allTemplates.find { t -> t.id == it }?.name ?: it.padEnd(42)}║")
            }
            sb.appendLine("// ╚══════════════════════════════════════════════════╝")
            sb.appendLine()

            // ========== 多品种声明 ==========
            config.multiInstrument?.let { mi ->
                if (mi.enabled && mi.instruments.isNotEmpty()) {
                    sb.appendLine("// === 适用品种（多品种并发） ===")
                    sb.appendLine("// ${mi.instruments.joinToString(", ")}")
                    sb.appendLine("// 本策略可应用于以上任意品种，建议在量化平台中批量加载")
                    sb.appendLine()
                }
            }

            // ========== 策略组合（终极级）==========
            config.strategyGroup?.let { sg ->
                if (sg.enabled && sg.subStrategyIds.isNotEmpty()) {
                    sb.appendLine("// === 策略组合引擎 ===")
                    sb.appendLine("// 组合模式: ${sg.combinationMode}")
                    sb.appendLine("// 子策略: ${sg.subStrategyIds.joinToString(", ")}")
                    when (sg.combinationMode) {
                        "AND" -> sb.appendLine("// 规则: 所有子策略信号同时满足才开仓")
                        "OR" -> sb.appendLine("// 规则: 任一子策略信号满足即开仓")
                        "VOTE" -> sb.appendLine("// 规则: 多数投票(≥${(sg.voteThreshold * 100).toInt()}%)通过即开仓")
                        "WEIGHTED" -> {
                            sb.appendLine("// 规则: 加权评分(阈值${(sg.voteThreshold * 100).toInt()}%)")
                            sg.weights.forEachIndexed { i, w ->
                                sb.appendLine("//   子策略${i + 1} 权重: $w")
                            }
                        }
                    }
                    if (sg.ignoreExitSignal) sb.appendLine("// 组合模式下忽略子策略平仓信号，统一由本策略管理")
                    sb.appendLine()
                }
            }

            // ========== 回测参数 ==========
            config.backtestConfig?.let { bt ->
                sb.appendLine("// === 回测参数 ===")
                sb.appendLine("// 测试区间: ${bt.startDate} ~ ${bt.endDate}")
                sb.appendLine("// 初始资金: ${bt.initialCapital}")
                sb.appendLine("// 手续费率: ${bt.commissionRate * 100}%  滑点: ${bt.slippage}跳")
                sb.appendLine("// 保证金率: ${bt.marginRate * 100}%  最大回溯: ${bt.maxBarsBack}根K线")
                sb.appendLine()
            }

            // ========== 跨周期引用 ==========
            if (config.crossPeriodRefs.isNotEmpty()) {
                sb.appendLine("// === 跨周期数据引用 ===")
                config.crossPeriodRefs.forEach { ref ->
                    val periodCode = mapPeriod(ref.period)
                    val paramsStr = if (ref.params.isNotEmpty()) ref.params.joinToString(",") else ""
                    sb.appendLine("#IMPORT[$periodCode, ${ref.indicator}${if (paramsStr.isNotEmpty()) "($paramsStr)" else ""}] AS ${ref.alias};")
                }
                sb.appendLine()
            }

            // ========== INPUT 参数 ==========
            sb.appendLine("// === 策略参数 ===")
            val paramLines = buildInputParams(config)
            paramLines.forEach { sb.appendLine(it) }
            sb.appendLine()

            // ========== 资金管理变量 ==========
            config.moneyManagement?.let { mm ->
                if (mm.enabled) {
                    sb.appendLine("// === 资金管理 ===")
                    sb.appendLine("LOTS := ${generatePositionSizing(mm)};")
                    if (mm.compoundProfit) {
                        sb.appendLine("EQUITY := MONEYREAL;  // 动态权益")
                    }
                    sb.appendLine("MAX_POSITION := INTPART(${mm.initialCapital} * ${mm.maxPositionPercent} / 100 / (CLOSE * MARGINRATE));")
                    sb.appendLine()
                }
            }

            // ========== 时间过滤 ==========
            config.timeFilter?.let { tf ->
                if (tf.enabled) {
                    sb.appendLine("// === 时间过滤 ===")
                    sb.appendLine("TRADE_TIME := TIME >= ${tf.tradeStartTime.replace(":", "")} AND TIME <= ${tf.tradeEndTime.replace(":", "")};")
                    if (tf.avoidOvernight) {
                        val endTime = tf.tradeEndTime.replace(":", "")
                        val closeBefore = (endTime.toIntOrNull() ?: 1455) - 5
                        sb.appendLine("CLOSE_BEFORE_CLOSE := TIME >= ${closeBefore};  // 收盘前5分钟强制平仓")
                    }
                    sb.appendLine()
                }
            }

            // ========== 参数优化注解（终极级）==========
            config.paramOptimization?.let { po ->
                if (po.enabled && po.optimizedParams.isNotEmpty()) {
                    sb.appendLine("// === 参数优化 - ${po.objectiveFunction} ===")
                    sb.appendLine("// 优化目标: ${when (po.objectiveFunction) { "SHARPE_RATIO" -> "最大化夏普比率"; "PROFIT_FACTOR" -> "最大化盈利因子"; "NET_PROFIT" -> "最大化净利润"; "WIN_RATE" -> "最大化胜率"; else -> po.objectiveFunction }}")
                    if (po.enableGridSearch) {
                        sb.appendLine("// 方法: 网格搜索")
                    } else {
                        sb.appendLine("// 方法: 遗传算法(种群${po.populationSize}, ${po.generations}代)")
                    }
                    sb.appendLine("// 待优化参数:")
                    po.optimizedParams.forEach { p ->
                        sb.appendLine("//   ${p.name}: [${p.start} ~ ${p.end}, 步长${p.step}]  当前值: ${p.current}")
                    }
                    sb.appendLine()
                }
            }

            // ========== 中间变量 ==========
            sb.appendLine("// === 指标计算与中间变量 ===")
            val allConditions = config.entryConditions.flatMap { it.conditions } +
                    config.exitConditions.flatMap { it.conditions } +
                    config.shortEntryConditions.flatMap { it.conditions } +
                    config.shortExitConditions.flatMap { it.conditions }

            val indicatorVars = mutableSetOf<String>()
            allConditions.forEach { cond ->
                collectIndicatorVar(cond.leftIndicator, cond.leftParams, "L", cond.leftIndicator)?.let {
                    indicatorVars.add(it)
                }
                collectIndicatorVar(cond.rightIndicator, cond.rightParams, "R", cond.rightIndicator)?.let {
                    indicatorVars.add(it)
                }
            }

            // 全局变量
            config.globalVariables.forEach { sb.appendLine("$it;") }

            // 指标变量（去重）
            val seen = mutableSetOf<String>()
            indicatorVars.forEach { line ->
                val name = line.substringBefore(" :=").trim()
                if (name !in seen) {
                    seen.add(name)
                    sb.appendLine(line)
                }
            }
            sb.appendLine()

            // ========== 条件信号 ==========
            val isLong = config.tradeDirection == TradeDirection.LONG || config.tradeDirection == TradeDirection.BOTH
            val isShort = config.tradeDirection == TradeDirection.SHORT || config.tradeDirection == TradeDirection.BOTH

            if (isLong && config.entryConditions.isNotEmpty()) {
                sb.appendLine("// === 多头开仓信号 ===")
                buildSignalBlock(sb, config.entryConditions, allConditions, "LONG_ENTRY")
            }
            if (isLong && config.exitConditions.isNotEmpty()) {
                sb.appendLine("// === 多头平仓信号 ===")
                buildSignalBlock(sb, config.exitConditions, allConditions, "LONG_EXIT")
            }
            if (isShort && config.shortEntryConditions.isNotEmpty()) {
                sb.appendLine("// === 空头开仓信号 ===")
                buildSignalBlock(sb, config.shortEntryConditions, allConditions, "SHORT_ENTRY")
            }
            if (isShort && config.shortExitConditions.isNotEmpty()) {
                sb.appendLine("// === 空头平仓信号 ===")
                buildSignalBlock(sb, config.shortExitConditions, allConditions, "SHORT_EXIT")
            }

            // ========== 信号过滤 ==========
            config.signalFilter?.let { sf ->
                if (sf.enabled) {
                    sb.appendLine("// === 信号过滤 ===")
                    if (sf.filterDuplicate) {
                        if (isLong && config.entryConditions.isNotEmpty())
                            sb.appendLine("LONG_ENTRY_FILTERED := FILTER(LONG_ENTRY_SIGNAL, ${sf.filterBars});")
                        if (isShort && config.shortEntryConditions.isNotEmpty())
                            sb.appendLine("SHORT_ENTRY_FILTERED := FILTER(SHORT_ENTRY_SIGNAL, ${sf.filterBars});")
                    }
                    if (sf.enableReverseSignal) {
                        sb.appendLine("// 反向信号自动平仓（信号覆盖模式）")
                        if (isLong && isShort)
                            sb.appendLine("LONG_EXIT_OVERRIDE := LONG_EXIT_SIGNAL OR SHORT_ENTRY_SIGNAL;")
                    }
                    sb.appendLine()
                }
            }

            // ========== 止损止盈计算 ==========
            config.stopLoss?.let { sl ->
                if (sl.enabled) {
                    sb.appendLine("// === 止损价计算 ===")
                    sb.appendLine(buildStopLossLogic(config, sl, "LONG"))
                    sb.appendLine()
                }
            }
            config.stopLoss?.let { sl ->
                if (sl.enabled && isShort) {
                    sb.appendLine(buildStopLossLogic(config, sl, "SHORT"))
                    sb.appendLine()
                }
            }
            config.takeProfit?.let { tp ->
                if (tp.enabled) {
                    sb.appendLine("// === 止盈价计算 ===")
                    sb.appendLine(buildTakeProfitLogic(config, tp, "LONG"))
                    if (isShort) sb.appendLine(buildTakeProfitLogic(config, tp, "SHORT"))
                    sb.appendLine()
                }
            }
            config.trailingStop?.let { ts ->
                if (ts.enabled) {
                    sb.appendLine("// === 移动止损 ===")
                    sb.appendLine(buildTrailingStopLogic(config, ts))
                    sb.appendLine()
                }
            }

            // ========== 日内风控熔断（终极级）==========
            config.riskControl?.let { rc ->
                if (rc.enabled) {
                    sb.appendLine("// === 日内风控熔断 ===")
                    sb.appendLine("DAILY_PL := MONEYTOT - MONEYTOT - MONEYTOT;  // 需替换为日内盈亏跟踪")
                    sb.appendLine("VAR0 := ((DAILY_PL - ${rc.maxDailyLoss}) < 0) OR (TOTALTRADE >= ${rc.maxDailyTrades});  // 熔断标志")
                    if (rc.maxConsecutiveLoss > 0)
                        sb.appendLine("VAR0 := VAR0 OR (CONSECUTIVERETREAT >= ${rc.maxConsecutiveLoss});  // 连续亏损${rc.maxConsecutiveLoss}笔")
                    if (rc.maxPositionCount > 0)
                        sb.appendLine("VAR0 := VAR0 OR (COUNT(BKVOL + SKVOL > 0, BARSCOUNT) >= ${rc.maxPositionCount});  // 持仓${rc.maxPositionCount}上限")
                    sb.appendLine("RISK_CIRCUIT_BREAKER := VAR0;  // 熔断信号")
                    sb.appendLine()
                }
            }

            // ========== 交易执行 ==========
            sb.appendLine("// ╔══════════════════════════════════════╗")
            sb.appendLine("// ║         交易执行逻辑                 ║")
            sb.appendLine("// ╚══════════════════════════════════════╝")
            sb.appendLine()

            if (isLong) {
                sb.appendLine("// === 多头交易 ===")
                val longEntrySig = getFilteredSignal(config.signalFilter, "LONG_ENTRY_SIGNAL", "LONG_ENTRY_FILTERED")
                val longExitSig = getFilteredSignal(config.signalFilter, "LONG_EXIT_SIGNAL", "LONG_EXIT_FILTERED")

                val timeCond = if (config.timeFilter?.enabled == true) " AND TRADE_TIME" else ""
                val riskCheck = if (config.riskControl?.enabled == true) " AND NOT RISK_CIRCUIT_BREAKER" else ""
                val lots = if (config.moneyManagement?.enabled == true) "LOTS" else "1"
                val pyramidEnabled = config.pyramid?.enabled == true

                if (pyramidEnabled) {
                    val pc = config.pyramid!!
                    sb.appendLine("// --- 金字塔分批建仓（${pc.entryType}模式，${pc.pyramidLayers}层） ---")
                    val intervalNote = when (pc.pyramidInterval) {
                        "ATR" -> "ATR(${pc.pyramidIntervalValue})"
                        "PERCENT" -> "当前价 × ${pc.pyramidIntervalValue}%"
                        "FIXED" -> "固定${pc.pyramidIntervalValue}点"
                        else -> "${pc.pyramidIntervalValue}"
                    }
                    sb.appendLine("// 加仓间隔: $intervalNote  |  方向: ${pc.pyramidDirection}  |  倍率: ${pc.pyramidMultiplier}×")
                    sb.appendLine("// 层数管理变量")
                    sb.appendLine("PYRAMID_LAYER := 0;                 // 当前持仓层数")
                    sb.appendLine("PYRAMID_LAST_PRICE := 0;             // 上一次开仓价")
                    sb.appendLine("PYRAMID_AVG_PRICE := 0;              // 持仓均价")
                    sb.appendLine()

                    sb.appendLine("// 首层开仓")
                    sb.appendLine("IF (BKVOL = 0 AND $longEntrySig${timeCond}${riskCheck}) THEN BEGIN")
                    sb.appendLine("    PYRAMID_LAYER := 1;")
                    sb.appendLine("    BUY(${pc.baseLots});")
                    sb.appendLine("    PYRAMID_LAST_PRICE := CLOSE;")
                    sb.appendLine("    PYRAMID_AVG_PRICE := CLOSE;")
                    sb.appendLine("END")
                    sb.appendLine()

                    sb.appendLine("// 金字塔加仓层")
                    if (pc.pyramidInterval == "ATR") {
                        sb.appendLine("PYRAMID_ATR := ATR(14);")
                        sb.appendLine("PYRAMID_ADD_COND := CLOSE > PYRAMID_LAST_PRICE + ${pc.pyramidIntervalValue} * PYRAMID_ATR;")
                    } else if (pc.pyramidInterval == "PERCENT") {
                        sb.appendLine("PYRAMID_ADD_COND := CLOSE > PYRAMID_LAST_PRICE * (1 + ${pc.pyramidIntervalValue} / 100);")
                    } else {
                        sb.appendLine("PYRAMID_ADD_COND := CLOSE > PYRAMID_LAST_PRICE + ${pc.pyramidIntervalValue} * MINDIFF;")
                    }

                    val addLots = if (pc.pyramidMultiplier > 1.0)
                        "${pc.baseLots} * POW(${pc.pyramidMultiplier}, PYRAMID_LAYER)"
                    else "${pc.baseLots}"

                    sb.appendLine("IF (BKVOL > 0 AND PYRAMID_LAYER < ${pc.pyramidLayers} AND PYRAMID_ADD_COND${timeCond}${riskCheck}) THEN BEGIN")
                    sb.appendLine("    BUY($addLots);")
                    sb.appendLine("    PYRAMID_LAYER := PYRAMID_LAYER + 1;")
                    sb.appendLine("    PYRAMID_LAST_PRICE := CLOSE;")
                    sb.appendLine("    PYRAMID_AVG_PRICE := (PYRAMID_AVG_PRICE * PYRAMID_LAYER + CLOSE) / (PYRAMID_LAYER + 1);")
                    sb.appendLine("END")
                    sb.appendLine()

                    // 平仓
                    sb.appendLine("// 金字塔平仓")
                    if (pc.averageExit) {
                        sb.append("IF (BKVOL > 0 AND ($longExitSig")
                    } else {
                        sb.append("IF (BKVOL > 0 AND ($longExitSig")
                    }
                } else {
                    sb.appendLine("// 开多仓")
                    sb.appendLine("IF $longEntrySig${timeCond}${riskCheck} THEN BEGIN")
                    config.stopLoss?.let { sl ->
                        if (sl.enabled) sb.appendLine("    LONG_STOP_PRICE := ENTRYPRICE - LONG_SL_TICKS * MINDIFF;")
                    }
                    config.takeProfit?.let { tp ->
                        if (tp.enabled) sb.appendLine("    LONG_TP_PRICE := ENTRYPRICE + LONG_TP_TICKS * MINDIFF;")
                    }
                    sb.appendLine("    BUY($lots);")
                    sb.appendLine("END")
                    sb.appendLine()

                    sb.appendLine("// 平多仓")
                    sb.append("IF $longExitSig")
                }

                // 共同平仓条件
                config.stopLoss?.let { sl ->
                    if (sl.enabled) sb.append(" OR (MARKETPOSITION = 1 AND LOW <= LONG_STOP_PRICE)")
                }
                config.takeProfit?.let { tp ->
                    if (tp.enabled) sb.append(" OR (MARKETPOSITION = 1 AND HIGH >= LONG_TP_PRICE)")
                }
                config.trailingStop?.let { ts ->
                    if (ts.enabled) sb.append(" OR (MARKETPOSITION = 1 AND LOW <= LONG_TRAIL_STOP)")
                }
                config.signalFilter?.let { sf ->
                    if (sf.enableReverseSignal && isShort)
                        sb.append(" OR (MARKETPOSITION = 1 AND SHORT_ENTRY_SIGNAL)")
                }
                config.timeFilter?.let { tf ->
                    if (tf.enabled && tf.avoidOvernight) sb.append(" OR (MARKETPOSITION = 1 AND CLOSE_BEFORE_CLOSE)")
                }
                sb.appendLine(")) THEN BEGIN")

                if (pyramidEnabled) {
                    sb.appendLine("    SELL(BKVOL);  // 一次平掉所有仓位")
                    sb.appendLine("    PYRAMID_LAYER := 0;")
                } else {
                    sb.appendLine("    SELL(LOTS);")
                }
                sb.appendLine("END")
                sb.appendLine()
            }

            if (isShort) {
                sb.appendLine("// === 空头交易 ===")
                val shortEntrySig = getFilteredSignal(config.signalFilter, "SHORT_ENTRY_SIGNAL", "SHORT_ENTRY_FILTERED")
                val shortExitSig = getFilteredSignal(config.signalFilter, "SHORT_EXIT_SIGNAL", "SHORT_EXIT_FILTERED")

                val timeCond = if (config.timeFilter?.enabled == true) " AND TRADE_TIME" else ""
                val riskCheck = if (config.riskControl?.enabled == true) " AND NOT RISK_CIRCUIT_BREAKER" else ""
                val lots = if (config.moneyManagement?.enabled == true) "LOTS" else "1"

                sb.appendLine("// 开空仓")
                sb.appendLine("IF $shortEntrySig${timeCond}${riskCheck} THEN BEGIN")
                sb.appendLine("    BUYSHORT($lots);")
                sb.appendLine("END")
                sb.appendLine()

                sb.appendLine("// 平空仓")
                sb.append("IF $shortExitSig")
                config.stopLoss?.let { sl ->
                    if (sl.enabled) sb.append(" OR (MARKETPOSITION = -1 AND HIGH >= SHORT_STOP_PRICE)")
                }
                config.takeProfit?.let { tp ->
                    if (tp.enabled) sb.append(" OR (MARKETPOSITION = -1 AND LOW <= SHORT_TP_PRICE)")
                }
                config.trailingStop?.let { ts ->
                    if (ts.enabled) sb.append(" OR (MARKETPOSITION = -1 AND HIGH >= SHORT_TRAIL_STOP)")
                }
                config.signalFilter?.let { sf ->
                    if (sf.enableReverseSignal && isLong)
                        sb.append(" OR (MARKETPOSITION = -1 AND LONG_ENTRY_SIGNAL)")
                }
                config.timeFilter?.let { tf ->
                    if (tf.enabled && tf.avoidOvernight) sb.append(" OR (MARKETPOSITION = -1 AND CLOSE_BEFORE_CLOSE)")
                }
                sb.appendLine(" THEN BEGIN")
                sb.appendLine("    SELLSHORT($lots);")
                sb.appendLine("END")
                sb.appendLine()
            }

            // ========== 绘图标注（大师级新增）==========
            buildDrawingLogic(sb, config)

            val rawCode = sb.toString()
            val code = if (config.annotateCode) annotateSourceCode(rawCode) else rawCode
            val summary = buildSummary(config)

            return GenerationResult(success = true, sourceCode = code, strategyName = config.name, summary = summary)

        } catch (e: Exception) {
            errors.add("代码生成异常: ${e.message}")
            return GenerationResult(false, sb.toString(), config.name, errors)
        }
    }

    // ================================================================
    // 从模板生成
    // ================================================================
    fun generateFromTemplate(templateId: String, paramOverrides: Map<String, String> = emptyMap()): GenerationResult {
        val config = TemplateLibrary.buildConfig(templateId, paramOverrides)
        if (config.name == "UnknownTemplate") {
            return GenerationResult(false, "", "UnknownTemplate", listOf("未知模板ID: $templateId"))
        }
        return generate(config)
    }

    // ================================================================
    // 辅助方法
    // ================================================================

    private fun buildInputParams(config: StrategyConfig): List<String> {
        val lines = mutableListOf<String>()
        val params = mutableListOf<String>()

        val allConds = config.entryConditions.flatMap { it.conditions } +
                config.exitConditions.flatMap { it.conditions } +
                config.shortEntryConditions.flatMap { it.conditions } +
                config.shortExitConditions.flatMap { it.conditions }

        val periods = mutableMapOf<String, String>()
        allConds.forEach { cond ->
            extractPeriods(cond.leftIndicator, cond.leftParams, periods)
            extractPeriods(cond.rightIndicator, cond.rightParams, periods)
        }

        periods.forEach { (name, default) ->
            params.add("$name($default,1,200,1)")
        }

        if (params.isEmpty()) {
            params.add("N(9,1,100,1)")
            params.add("M(3,1,50,1)")
        }

        config.moneyManagement?.let { mm ->
            if (mm.enabled && mm.mode == "PERCENT_RISK") {
                params.add("RISK_PCT(${mm.riskPercent},0.1,10,0.1)")
            }
        }

        config.stopLoss?.let { sl ->
            if (sl.enabled && sl.type == "ATR") {
                params.add("SL_ATR_N(${sl.atrPeriod},1,100,1)")
                params.add("SL_ATR_MULT(${sl.atrMultiplier},0.5,10,0.5)")
            }
        }
        config.takeProfit?.let { tp ->
            if (tp.enabled && tp.type == "ATR") {
                params.add("TP_ATR_MULT(${tp.atrMultiplier},0.5,10,0.5)")
            }
        }
        config.trailingStop?.let { ts ->
            if (ts.enabled) {
                params.add("TRAIL_ATR_N(${ts.atrPeriod},1,100,1)")
                params.add("TRAIL_ATR_MULT(${ts.atrMultiplier},0.5,10,0.5)")
                params.add("TRAIL_ACTIVATE(${ts.activationPercent},0,100,1)")
            }
        }

        val paramStr = params.joinToString(", ")
        lines.add("INPUT: $paramStr;")
        return lines
    }

    private fun extractPeriods(indicator: String, params: List<String>, map: MutableMap<String, String>) {
        val periodParams = setOf("MA", "EMA", "SMA", "DMA", "MACD", "RSI", "KDJ_K", "KDJ_D", "KDJ_J", "BOLL", "UB", "LB", "ATR", "SAR", "STD", "VAR", "DIFF", "DEA", "ADX", "PDI", "MDI")
        if (indicator in periodParams && params.isNotEmpty()) {
            val pName = "${indicator}_N"
            if (pName !in map) map[pName] = params[0]
        }
    }

    private fun collectIndicatorVar(indicator: String, params: List<String>, prefix: String, rawName: String): String? {
        if (indicator.isBlank()) return null
        val varName = "${prefix}_$indicator"
        return when (indicator.uppercase()) {
            "MA" -> "$varName := MA(CLOSE, ${params.getOrElse(0) { "5" }});"
            "EMA" -> "$varName := EMA(CLOSE, ${params.getOrElse(0) { "12" }});"
            "SMA" -> "$varName := SMA(CLOSE, ${params.getOrElse(0) { "3" }}, ${params.getOrElse(1) { "1" }});"
            "DMA" -> "$varName := DMA(CLOSE, ${params.getOrElse(0) { "VOL/CAPITAL" }});"
            "WMA" -> "$varName := WMA(CLOSE, ${params.getOrElse(0) { "10" }});"
            "MACD" -> {
                val s = params.getOrElse(0) { "12" }; val l = params.getOrElse(1) { "26" }; val m = params.getOrElse(2) { "9" }
                "$varName := MACD(CLOSE, $s, $l, $m);"
            }
            "DIFF" -> {
                val s = params.getOrElse(0) { "12" }; val l = params.getOrElse(1) { "26" }
                "$varName := EMA(CLOSE, $s) - EMA(CLOSE, $l);"
            }
            "DEA" -> {
                val s = params.getOrElse(0) { "12" }; val l = params.getOrElse(1) { "26" }; val m = params.getOrElse(2) { "9" }
                "$varName := EMA(EMA(CLOSE,$s)-EMA(CLOSE,$l), $m);"
            }
            "KDJ_K" -> {
                val n = params.getOrElse(0) { "9" }; val m1 = params.getOrElse(1) { "3" }; val m2 = params.getOrElse(2) { "3" }
                "RSV := (CLOSE-LLV(LOW,$n))/(HHV(HIGH,$n)-LLV(LOW,$n))*100;\n$varName := SMA(RSV, $m1, 1);"
            }
            "KDJ_D" -> {
                val n = params.getOrElse(0) { "9" }; val m1 = params.getOrElse(1) { "3" }; val m2 = params.getOrElse(2) { "3" }
                "RSV := (CLOSE-LLV(LOW,$n))/(HHV(HIGH,$n)-LLV(LOW,$n))*100;\n$varName := SMA(SMA(RSV,$m1,1), $m2, 1);"
            }
            "KDJ_J" -> {
                val n = params.getOrElse(0) { "9" }; val m1 = params.getOrElse(1) { "3" }; val m2 = params.getOrElse(2) { "3" }
                "RSV := (CLOSE-LLV(LOW,$n))/(HHV(HIGH,$n)-LLV(LOW,$n))*100;\nK_VAL := SMA(RSV,$m1,1);\nD_VAL := SMA(K_VAL,$m2,1);\n$varName := 3*K_VAL-2*D_VAL;"
            }
            "RSI" -> {
                val n = params.getOrElse(0) { "14" }
                "LC := REF(CLOSE,1);\n$varName := SMA(MAX(CLOSE-LC,0),$n,1)/SMA(ABS(CLOSE-LC),$n,1)*100;"
            }
            "BOLL" -> {
                val n = params.getOrElse(0) { "20" }; val p = params.getOrElse(1) { "2" }
                "$varName := MA(CLOSE, $n);"
            }
            "UB" -> {
                val n = params.getOrElse(0) { "20" }; val p = params.getOrElse(1) { "2" }
                "$varName := MA(CLOSE,$n) + $p * STD(CLOSE,$n);"
            }
            "LB" -> {
                val n = params.getOrElse(0) { "20" }; val p = params.getOrElse(1) { "2" }
                "$varName := MA(CLOSE,$n) - $p * STD(CLOSE,$n);"
            }
            "ATR" -> "$varName := ATR(${params.getOrElse(0) { "14" }});"
            "SAR" -> "$varName := SAR(${params.getOrElse(0) { "4" }}, ${params.getOrElse(1) { "0.02" }}, ${params.getOrElse(2) { "0.2" }});"
            "ADX" -> "$varName := ADX(${params.getOrElse(0) { "14" }}, ${params.getOrElse(1) { "6" }});"
            "PDI" -> "$varName := PDI(${params.getOrElse(0) { "14" }}, ${params.getOrElse(1) { "6" }});"
            "MDI" -> "$varName := MDI(${params.getOrElse(0) { "14" }}, ${params.getOrElse(1) { "6" }});"
            "STD" -> "$varName := STD(${params.getOrElse(0) { "CLOSE" }}, ${params.getOrElse(1) { "20" }});"
            "VAR" -> "$varName := VAR(${params.getOrElse(0) { "CLOSE" }}, ${params.getOrElse(1) { "20" }});"
            "AVEDEV" -> "$varName := AVEDEV(${params.getOrElse(0) { "CLOSE" }}, ${params.getOrElse(1) { "20" }});"
            "REF" -> "$varName := REF(${params.getOrElse(0) { "CLOSE" }}, ${params.getOrElse(1) { "1" }});"
            "HHV" -> "$varName := HHV(${params.getOrElse(0) { "HIGH" }}, ${params.getOrElse(1) { "20" }});"
            "LLV" -> "$varName := LLV(${params.getOrElse(0) { "LOW" }}, ${params.getOrElse(1) { "20" }});"
            "HHVBARS" -> "$varName := HHVBARS(${params.getOrElse(0) { "HIGH" }}, ${params.getOrElse(1) { "20" }});"
            "LLVBARS" -> "$varName := LLVBARS(${params.getOrElse(0) { "LOW" }}, ${params.getOrElse(1) { "20" }});"
            "BARSLAST" -> "$varName := BARSLAST(${params.getOrElse(0) { "CLOSE>OPEN" }});"
            "COUNT" -> "$varName := COUNT(${params.getOrElse(0) { "CLOSE>OPEN" }}, ${params.getOrElse(1) { "10" }});"
            "SUM" -> "$varName := SUM(${params.getOrElse(0) { "CLOSE" }}, ${params.getOrElse(1) { "10" }});"
            "EVERY" -> "$varName := EVERY(${params.getOrElse(0) { "CLOSE>OPEN" }}, ${params.getOrElse(1) { "10" }});"
            "ABS" -> "$varName := ABS(${params.getOrElse(0) { "CLOSE" }});"
            "MAX" -> "$varName := MAX(${params.getOrElse(0) { "CLOSE" }}, ${params.getOrElse(1) { "OPEN" }});"
            "MIN" -> "$varName := MIN(${params.getOrElse(0) { "CLOSE" }}, ${params.getOrElse(1) { "OPEN" }});"
            // 大师级新增
            "MOD" -> "$varName := MOD(${params.getOrElse(0) { "CLOSE" }}, ${params.getOrElse(1) { "2" }});"
            "IF" -> {
                val cond = params.getOrElse(0) { "CLOSE>OPEN" }; val a = params.getOrElse(1) { "1" }; val b = params.getOrElse(2) { "0" }
                "$varName := IF($cond, $a, $b);"
            }
            "VALUEWHEN" -> "$varName := VALUEWHEN(${params.getOrElse(0) { "CROSS_COND" }}, ${params.getOrElse(1) { "CLOSE" }});"
            "CLOSE", "OPEN", "HIGH", "LOW", "VOL", "OPI", "AMOUNT" -> null
            else -> "$varName := $indicator(${params.joinToString(", ")});"
        }
    }

    private fun buildSignalBlock(sb: StringBuilder, groups: List<ConditionGroup>, allConds: List<SingleCondition>, name: String) {
        groups.forEachIndexed { idx, group ->
            val condCode = buildConditionExpression(group, allConds)
            sb.appendLine("${name}_GROUP${idx + 1} := $condCode;")
        }
        val joined = groups.indices.joinToString(" OR ") { "${name}_GROUP${it + 1}" }
        sb.appendLine("${name}_SIGNAL := ${joined};")
        sb.appendLine()
    }

    private fun buildConditionExpression(group: ConditionGroup, allConditions: List<SingleCondition>): String {
        return group.conditions.filter { it.enabled }.joinToString(" AND ") { cond ->
            val left = indicatorToCode(cond.leftIndicator, cond.leftParams, "L")
            val right = indicatorToCode(cond.rightIndicator, cond.rightParams, "R")
            buildOperatorExpression(left, cond.operator, right)
        }.ifEmpty { "1" }
    }

    private fun indicatorToCode(indicator: String, params: List<String>, prefix: String): String {
        if (indicator.isBlank()) return "1"
        return when (indicator.uppercase()) {
            "CLOSE", "OPEN", "HIGH", "LOW", "VOL", "OPI", "AMOUNT" -> indicator.uppercase()
            "BKPRICE" -> "BKPRICE"
            "SKPRICE" -> "SKPRICE"
            "BKHIGH" -> "BKHIGH"
            "SKLOW" -> "SKLOW"
            "MARKETPOSITION" -> "MARKETPOSITION"
            "BARPOS" -> "BARPOS"
            else -> "${prefix}_$indicator"
        }
    }

    private fun buildOperatorExpression(left: String, op: String, right: String): String {
        return when (op) {
            "CROSS" -> "CROSS($left, $right)"
            "CROSS_UNDER" -> "CROSS($right, $left)"
            ">" -> "$left > $right"
            "<" -> "$left < $right"
            ">=" -> "$left >= $right"
            "<=" -> "$left <= $right"
            "==" -> "$left == $right"
            "!=" -> "$left != $right"
            "REF_UP" -> "$left > REF($left, 1)"
            "REF_DOWN" -> "$left < REF($left, 1)"
            "BREAK_HHV" -> "$left > HHV($left, $right)"
            "BREAK_LLV" -> "$left < LLV($left, $right)"
            "BARSLAST_GT" -> "BARSLAST($left) > $right"
            "MOD_EQ" -> "MOD($left, $right) == 0"
            else -> "$left > $right"
        }
    }

    private fun buildStopLossLogic(config: StrategyConfig, sl: StopLossConfig, direction: String): String {
        val prefix = if (direction == "LONG") "LONG" else "SHORT"
        return when (sl.type) {
            "ATR" -> {
                val atr = "ATR(${sl.atrPeriod})"
                if (direction == "LONG")
                    "${prefix}_SL_TICKS := INTPART(${sl.atrMultiplier} * $atr / MINDIFF);"
                else
                    "${prefix}_SL_TICKS := INTPART(${sl.atrMultiplier} * $atr / MINDIFF);"
            }
            "PERCENT" -> "${prefix}_SL_TICKS := INTPART(ENTRYPRICE * ${sl.value} / 100 / MINDIFF);"
            "MOVING_AVG" -> "${prefix}_SL_TICKS := INTPART((ENTRYPRICE - MA(CLOSE, ${sl.value.toInt()})) / MINDIFF);"
            "LOW_HIGH" -> if (direction == "LONG")
                "${prefix}_SL_TICKS := INTPART((ENTRYPRICE - LLV(LOW, ${sl.value.toInt()})) / MINDIFF);"
            else
                "${prefix}_SL_TICKS := INTPART((HHV(HIGH, ${sl.value.toInt()}) - ENTRYPRICE) / MINDIFF);"
            else -> "${prefix}_SL_TICKS := INTPART(${sl.value} / MINDIFF);"
        }
    }

    private fun buildTakeProfitLogic(config: StrategyConfig, tp: TakeProfitConfig, direction: String): String {
        val prefix = if (direction == "LONG") "LONG" else "SHORT"
        return when (tp.type) {
            "ATR" -> "${prefix}_TP_TICKS := INTPART(${tp.atrMultiplier} * ATR(14) / MINDIFF);"
            "PERCENT" -> "${prefix}_TP_TICKS := INTPART(ENTRYPRICE * ${tp.value} / 100 / MINDIFF);"
            "RISK_REWARD" -> "${prefix}_TP_TICKS := INTPART(${prefix}_SL_TICKS * ${tp.riskRewardRatio});"
            "FIBONACCI" -> "${prefix}_TP_TICKS := INTPART(${prefix}_SL_TICKS * 1.618);"
            else -> "${prefix}_TP_TICKS := INTPART(${tp.value} / MINDIFF);"
        }
    }

    private fun buildTrailingStopLogic(config: StrategyConfig, ts: TrailingStopConfig): String {
        val sb = StringBuilder()
        val atr = "ATR(${ts.atrPeriod})"
        val trailDist = "${ts.atrMultiplier} * $atr"

        sb.appendLine("// 移动止损 —— 多头")
        sb.appendLine("IF MARKETPOSITION = 1 THEN BEGIN")
        sb.appendLine("    HIGH_SINCE_ENTRY := HHV(HIGH, BARSLAST(MARKETPOSITION <> REF(MARKETPOSITION,1)) + 1);")
        sb.appendLine("    LONG_TRAIL_STOP := HIGH_SINCE_ENTRY - $trailDist;")
        sb.appendLine("    // 仅在盈利达到阈值后才激活")
        sb.appendLine("    IF HIGH_SINCE_ENTRY >= ENTRYPRICE * (1 + ${ts.activationPercent}/100) THEN BEGIN")
        sb.appendLine("        LONG_TRAIL_STOP := MAX(LONG_TRAIL_STOP, ENTRYPRICE);  // 保本")
        sb.appendLine("    END")
        sb.appendLine("END")
        sb.appendLine()

        if (config.tradeDirection == TradeDirection.SHORT || config.tradeDirection == TradeDirection.BOTH) {
            sb.appendLine("// 移动止损 —— 空头")
            sb.appendLine("IF MARKETPOSITION = -1 THEN BEGIN")
            sb.appendLine("    LOW_SINCE_ENTRY := LLV(LOW, BARSLAST(MARKETPOSITION <> REF(MARKETPOSITION,1)) + 1);")
            sb.appendLine("    SHORT_TRAIL_STOP := LOW_SINCE_ENTRY + $trailDist;")
            sb.appendLine("    IF LOW_SINCE_ENTRY <= ENTRYPRICE * (1 - ${ts.activationPercent}/100) THEN BEGIN")
            sb.appendLine("        SHORT_TRAIL_STOP := MIN(SHORT_TRAIL_STOP, ENTRYPRICE);")
            sb.appendLine("    END")
            sb.appendLine("END")
        }
        return sb.toString()
    }

    // ================================================================
    // 大师级新增 - 绘图标注
    // ================================================================
    private fun buildDrawingLogic(sb: StringBuilder, config: StrategyConfig) {
        config.drawing?.let { dc ->
            if (!dc.enabled) return
            sb.appendLine()
            sb.appendLine("// ╔══════════════════════════════════════╗")
            sb.appendLine("// ║         交易信号标注                 ║")
            sb.appendLine("// ╚══════════════════════════════════════╝")
            sb.appendLine()

            if (dc.showEntryIcon && config.entryConditions.isNotEmpty()) {
                sb.appendLine("// 多头开仓 —— ${if (dc.entryIconType == 1) "向上箭头" else "图标${dc.entryIconType}"}")
                sb.appendLine("DRAWICON(LONG_ENTRY_SIGNAL, LOW * 0.99, ${dc.entryIconType});")
                if (config.tradeDirection == TradeDirection.BOTH || config.tradeDirection == TradeDirection.SHORT) {
                    if (config.shortEntryConditions.isNotEmpty()) {
                        sb.appendLine("// 空头开仓")
                        sb.appendLine("DRAWICON(SHORT_ENTRY_SIGNAL, HIGH * 1.01, ${dc.exitIconType});")
                    }
                }
            }
            if (dc.showExitIcon && config.exitConditions.isNotEmpty()) {
                sb.appendLine("// 多头平仓 —— ${if (dc.exitIconType == 2) "向下箭头" else "图标${dc.exitIconType}"}")
                sb.appendLine("DRAWICON(LONG_EXIT_SIGNAL, HIGH * 1.01, ${dc.exitIconType});")
            }
            if (dc.showEntryText && config.entryConditions.isNotEmpty()) {
                sb.appendLine("// 开仓文字标注")
                sb.appendLine("DRAWTEXT(LONG_ENTRY_SIGNAL, LOW * 0.97, '${dc.entryText}');")
            }
            if (dc.showExitText && config.exitConditions.isNotEmpty()) {
                sb.appendLine("// 平仓文字标注")
                sb.appendLine("DRAWTEXT(LONG_EXIT_SIGNAL, HIGH * 1.03, '${dc.exitText}');")
            }
            if (dc.showStopLossLine && config.stopLoss?.enabled == true) {
                sb.appendLine("// 止损线标注")
                sb.appendLine("IF MARKETPOSITION = 1 THEN")
                sb.appendLine("    STICKLINE(1, LONG_STOP_PRICE, LONG_STOP_PRICE, 1, 0);  // 多头止损线")
                if (config.tradeDirection == TradeDirection.BOTH || config.tradeDirection == TradeDirection.SHORT) {
                    sb.appendLine("IF MARKETPOSITION = -1 THEN")
                    sb.appendLine("    STICKLINE(1, SHORT_STOP_PRICE, SHORT_STOP_PRICE, 1, 0);  // 空头止损线")
                }
            }
            sb.appendLine()
        }
    }

    private fun generatePositionSizing(mm: MoneyManagementConfig): String {
        return when (mm.mode) {
            "FIXED" -> "${mm.fixedLots}"
            "PERCENT_RISK" -> "INTPART(EQUITY * RISK_PCT / 100 / (SL_ATR_MULT * ATR(SL_ATR_N)))"
            "KELLY" -> "INTPART(EQUITY * 0.2 / (CLOSE * MARGINRATE))"
            "FIXED_RATIO" -> "INTPART(EQUITY / (${mm.initialCapital} / ${mm.fixedLots}))"
            "MARTINGALE" -> "IF MARKETPOSITION <> 0 AND REF(PROFIT, 1) < 0 THEN LOTS * 2 ELSE ${mm.fixedLots}"
            else -> "1"
        }
    }

    private fun getFilteredSignal(filter: SignalFilterConfig?, rawSignal: String, filteredSignal: String): String {
        return if (filter?.enabled == true && filter.filterDuplicate) filteredSignal else rawSignal
    }

    private fun mapPeriod(period: String): String {
        return when (period.uppercase()) {
            "MIN1", "1MIN" -> "MIN1"
            "MIN5", "5MIN" -> "MIN5"
            "MIN15", "15MIN" -> "MIN15"
            "MIN30", "30MIN" -> "MIN30"
            "HOUR1", "1H" -> "HOUR1"
            "HOUR4", "4H" -> "HOUR4"
            "DAY", "1D" -> "DAY"
            "WEEK", "1W" -> "WEEK"
            "MONTH", "1M" -> "MONTH"
            else -> period.uppercase()
        }
    }

    private fun buildSummary(config: StrategyConfig): String {
        val sb = StringBuilder()
        sb.appendLine("═══════════════════════════")
        sb.appendLine("  策略: ${config.name}")
        sb.appendLine("  周期: ${config.period}  |  方向: ${config.tradeDirection.name}")
        sb.appendLine("  级别: 大师级 (Level 7)")
        sb.appendLine("═══════════════════════════")
        config.templateId.takeIf { it.isNotEmpty() }?.let {
            sb.appendLine("  模板: ${TemplateLibrary.allTemplates.find { t -> t.id == it }?.name ?: it}")
        }
        config.multiInstrument?.let { mi ->
            if (mi.enabled) sb.appendLine("  多品种: ${mi.instruments.joinToString(", ")}")
        }
        config.drawing?.let { dc ->
            if (dc.enabled) sb.appendLine("  绘图标注: ${if (dc.showEntryIcon) "开仓图标" else ""}${if (dc.showExitIcon) " 平仓图标" else ""}")
        }
        sb.appendLine("  多头开仓: ${config.entryConditions.size}组(${config.entryConditions.sumOf { it.conditions.size }}条件)")
        sb.appendLine("  多头平仓: ${config.exitConditions.size}组(${config.exitConditions.sumOf { it.conditions.size }}条件)")
        if (config.tradeDirection == TradeDirection.SHORT || config.tradeDirection == TradeDirection.BOTH) {
            sb.appendLine("  空头开仓: ${config.shortEntryConditions.size}组(${config.shortEntryConditions.sumOf { it.conditions.size }}条件)")
            sb.appendLine("  空头平仓: ${config.shortExitConditions.size}组(${config.shortExitConditions.sumOf { it.conditions.size }}条件)")
        }
        config.stopLoss?.let { if (it.enabled) sb.appendLine("  止损: ${it.type}(${it.value})") }
        config.takeProfit?.let { if (it.enabled) sb.appendLine("  止盈: ${it.type}(${it.value})") }
        config.trailingStop?.let { if (it.enabled) sb.appendLine("  移动止损: ATR x${it.atrMultiplier}") }
        config.moneyManagement?.let { if (it.enabled) sb.appendLine("  资金管理: ${it.mode}") }
        config.signalFilter?.let { if (it.enabled) sb.appendLine("  信号过滤: ${it.filterBars}周期") }
        config.timeFilter?.let { if (it.enabled) sb.appendLine("  时间过滤: ${it.tradeStartTime}-${it.tradeEndTime}") }
        if (config.crossPeriodRefs.isNotEmpty()) sb.appendLine("  跨周期: ${config.crossPeriodRefs.size}个引用")
        sb.appendLine("═══════════════════════════")
        return sb.toString()
    }

    // ================================================================
    // 智能逐行注解引擎
    // ================================================================
    companion object {
        private val annotator = SmartAnnotator()
    }

    private fun annotateSourceCode(raw: String): String {
        return raw.lines().joinToString("\n") { line ->
            annotator.annotate(line)
        }
    }
}

// ================================================================
// 智能注解生成器 — 为麦语言代码自动添加中文注释
// ================================================================
class SmartAnnotator {

    private data class Pattern(val regex: Regex, val template: String)

    private val patterns = listOf(
        // 技术指标
        Pattern(Regex("""\bMA\s*\(\s*CLOSE\s*,\s*(\w+)\s*\)"""), "// ${'$'}1周期收盘价均线"),
        Pattern(Regex("""\bMA\s*\(([^,]+?)\s*,\s*(\w+)\s*\)"""), "// ${'$'}2周期 ${'$'}1 均线"),
        Pattern(Regex("""\bEMA\s*\(\s*CLOSE\s*,\s*(\w+)\s*\)"""), "// ${'$'}1周期收盘价指数均线"),
        Pattern(Regex("""\bEMA\s*\(([^,]+?)\s*,\s*(\w+)\s*\)"""), "// ${'$'}2周期 ${'$'}1 指数均线"),
        Pattern(Regex("""\bATR\s*\(\s*(\w+)\s*\)"""), "// ${'$'}1周期平均真实波幅"),
        Pattern(Regex("""\bHHV\s*\(([^,]+?)\s*,\s*(\w+)\s*\)"""), "// ${'$'}2周期内 ${'$'}1 的最高值"),
        Pattern(Regex("""\bLLV\s*\(([^,]+?)\s*,\s*(\w+)\s*\)"""), "// ${'$'}2周期内 ${'$'}1 的最低值"),
        Pattern(Regex("""\bREF\s*\(([^,]+?)\s*,\s*(\w+)\s*\)"""), "// ${'$'}2周期前的 ${'$'}1 值"),
        Pattern(Regex("""\bBARSLAST\s*\((.+?)\s*\)"""), "// 上次满足 ${'$'}1 距今K线数"),
        Pattern(Regex("""\bCOUNT\s*\(([^,]+?)\s*,\s*(\w+)\s*\)"""), "// ${'$'}2周期内 ${'$'}1 成立次数"),
        Pattern(Regex("""\bEVERY\s*\(([^,]+?)\s*,\s*(\w+)\s*\)"""), "// ${'$'}2周期内 ${'$'}1 持续成立"),
        Pattern(Regex("""\bFILTER\s*\(([^,]+?)\s*,\s*(\w+)\s*\)"""), "// ${'$'}2周期内过滤 ${'$'}1 重复信号"),
        Pattern(Regex("""\bSTD\s*\(([^,]+?)\s*,\s*(\w+)\s*\)"""), "// ${'$'}2周期 ${'$'}1 标准差值"),
        Pattern(Regex("""\bABS\s*\((.+?)\s*\)"""), "// ${'$'}1 的绝对值"),
        Pattern(Regex("""\bMAX\s*\(([^,]+?)\s*,\s*(\w+)\s*\)"""), "// ${'$'}1 和 ${'$'}2 取最大值"),
        Pattern(Regex("""\bMIN\s*\(([^,]+?)\s*,\s*(\w+)\s*\)"""), "// ${'$'}1 和 ${'$'}2 取最小值"),
        Pattern(Regex("""\bNOT\s*\((.+?)\s*\)"""), "// ${'$'}1 条件取反"),
        Pattern(Regex("""\bPOW\s*\(([^,]+?)\s*,\s*(\w+)\s*\)"""), "// ${'$'}1 的 ${'$'}2 次幂"),
        Pattern(Regex("""\bBARSCOUNT\s*\((.+?)\s*\)"""), "// ${'$'}1 的总K线数"),
        Pattern(Regex("""\bBARPOS"""), "// 当前K线位置"),
        Pattern(Regex("""\bCROSS\s*\(([^,]+?)\s*,\s*([^)]+?)\s*\)"""), "// ${'$'}1 上穿 ${'$'}2（金叉信号）"),
        Pattern(Regex("""\bCROSS_UNDER\s*\(([^,]+?)\s*,\s*([^)]+?)\s*\)"""), "// ${'$'}2 下穿 ${'$'}1（死叉信号）"),

        // 交易指令
        Pattern(Regex("""^\s*BUY\s*\((.+?)\)\s*;\s*"""), "  // 开多仓，手数=${'$'}1"),
        Pattern(Regex("""^\s*SELL\s*\((.+?)\)\s*;\s*"""), "  // 平多仓，手数=${'$'}1"),
        Pattern(Regex("""^\s*BUYSHORT\s*\((.+?)\)\s*;\s*"""), "  // 开空仓，手数=${'$'}1"),
        Pattern(Regex("""^\s*SELLSHORT\s*\((.+?)\)\s*;\s*"""), "  // 平空仓，手数=${'$'}1"),

        // 行情变量
        Pattern(Regex("""\bCLOSE\b(?!\s*\()"""), "// 收盘价"),
        Pattern(Regex("""\bOPEN\b(?!\s*\()"""), "// 开盘价"),
        Pattern(Regex("""\bHIGH\b(?!\s*\()"""), "// 最高价"),
        Pattern(Regex("""\bLOW\b(?!\s*\()"""), "// 最低价"),
        Pattern(Regex("""\bVOL\b(?!\s*\()"""), "// 成交量"),
        Pattern(Regex("""\bTIME\b(?!\s*\()"""), "// 当前时间"),
        Pattern(Regex("""\bENTRYPRICE\b"""), "// 开仓价"),
        Pattern(Regex("""\bMINDIFF\b"""), "// 最小变动价位"),
        Pattern(Regex("""\bBKVOL\b"""), "// 多头持仓手数"),
        Pattern(Regex("""\bSKVOL\b"""), "// 空头持仓手数"),
        Pattern(Regex("""\bMARKETPOSITION\b"""), "// 持仓方向(1多/-1空/0空仓)"),
        Pattern(Regex("""\bMONEYTOT\b"""), "// 账户总资金"),
        Pattern(Regex("""\bTOTALTRADE\b"""), "// 日内总交易次数"),
        Pattern(Regex("""\bCONSECUTIVERETREAT\b"""), "// 连续亏损笔数"),
        Pattern(Regex("""\bDAILY_PL\b"""), "// 日内浮动盈亏"),

        // 逻辑运算符
        Pattern(Regex("""\bAND\b(?![\w'])"""), "// 逻辑与"),
        Pattern(Regex("""\bOR\b(?![\w'])"""), "// 逻辑或"),
        Pattern(Regex("""(?<!NOT)\s>\s(?!=)"""), "// 大于"),
        Pattern(Regex("""(?<!NOT)\s>=\s"""), "// 大于等于"),
        Pattern(Regex("""\s<\s(?!=)"""), "// 小于"),
        Pattern(Regex("""\s<=\s"""), "// 小于等于"),
        Pattern(Regex("""\s=\s"""), "// 等于"),
        Pattern(Regex("""(?<!\w)IF\s"""), "// 条件满足则"),
        Pattern(Regex("""(?<!\w)THEN\s+BEGIN"""), "// 执行以下块"),
        Pattern(Regex("""^\s*END\s*;\s*$"""), "  // 块结束"),
        Pattern(Regex("""^\s*END\s*$"""), "  // 块结束"),

        // 赋值
        Pattern(Regex(""":=\s*(.+);"""), "// 计算结果赋给变量"),
    )

    fun annotate(line: String): String {
        val trimmed = line.trim()
        // 跳过空行、已有注释、块注释、分隔线
        if (trimmed.isEmpty() || trimmed.startsWith("//") || trimmed.startsWith("/*") ||
            trimmed.contains("╔") || trimmed.contains("╚") || trimmed.contains("═")
        ) return line

        // 跳过纯注释头行如 "// ==="
        if (Regex("""^//\s*={3,}""").matches(trimmed)) return line

        // 尝试匹配交易指令（优先级最高）
        for (pat in patterns) {
            val m = pat.regex.find(line)
            if (m != null) {
                // 替换模板中的 $1 $2 等
                var comment = pat.template
                for (i in 1..m.groupValues.size.coerceAtMost(9)) {
                    comment = comment.replace("$$$i", m.groupValues.getOrElse(i) { "" })
                }
                return "$line  $comment"
            }
        }

        // 变量赋值兜底
        if (Regex("""^\s*\w+\s*:=""", RegexOption.IGNORE_CASE).containsMatchIn(line)) {
            val varName = Regex("""^\s*(\w+)\s*:=""", RegexOption.IGNORE_CASE).find(line)?.groupValues?.getOrNull(1)
            if (varName != null && varName.isNotEmpty() && varName.uppercase() == varName) {
                val suffix = annotateVariable(varName)
                return "$line  // $suffix"
            }
        }

        return line
    }

    private fun annotateVariable(name: String): String {
        return when {
            name.contains("MA") || name.startsWith("MA") -> "均线相关变量"
            name.contains("EMA") -> "指数均线变量"
            name.contains("MACD") || name.contains("DIF") || name.contains("DEA") -> "MACD指标变量"
            name.contains("KDJ") || name.contains("K_") || name.contains("D_") || name.contains("J_") -> "KDJ指标变量"
            name.contains("RSI") -> "RSI指标变量"
            name.contains("ATR") -> "ATR波动率变量"
            name.contains("BOLL") || name.contains("MID") || name.contains("UPPER") || name.contains("LOWER") -> "布林带变量"
            name.contains("SAR") -> "SAR抛物线变量"
            name.contains("ADX") || name.contains("PDI") || name.contains("MDI") -> "ADX趋势变量"
            name.contains("VOL") || name.contains("OBV") -> "成交量变量"
            name.contains("HSL") || name.contains("TURN") -> "换手率变量"
            name.contains("STOP") || name.contains("SL_") -> "止损相关变量"
            name.contains("TP_") || name.contains("PROFIT") -> "止盈相关变量"
            name.contains("ENTRY") && name.contains("LONG") -> "多头开仓信号"
            name.contains("EXIT") && name.contains("LONG") -> "多头平仓信号"
            name.contains("ENTRY") && name.contains("SHORT") -> "空头开仓信号"
            name.contains("EXIT") && name.contains("SHORT") -> "空头平仓信号"
            name.contains("SIGNAL") -> "交易信号变量"
            name.contains("FILTER") -> "信号过滤变量"
            name.contains("TRAIL") -> "移动止损变量"
            name.contains("LOTS") -> "仓位手数变量"
            name.contains("TRADE_TIME") -> "交易时间过滤"
            name.contains("CLOSE_BEFORE") -> "收盘前平仓标志"
            name.contains("CIRCUIT") || name.contains("RISK") -> "风控熔断变量"
            name.contains("PYRAMID") -> "金字塔加仓变量"
            name.contains("LAYER") -> "持仓层级变量"
            name.contains("PRICE") || name.contains("AVG") -> "价格/均价变量"
            name.startsWith("VAR") -> "临时中间变量"
            name.contains("LONG") && name.contains("SHORT") -> "多空联合条件"
            name.contains("LONG") -> "多头方向变量"
            name.contains("SHORT") -> "空头方向变量"
            name.contains("ADD_COND") -> "加仓条件"
            else -> "自定义变量"
        }
    }
}
