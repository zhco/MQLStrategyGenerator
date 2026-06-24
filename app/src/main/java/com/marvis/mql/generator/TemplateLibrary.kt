package com.marvis.mql.generator

import com.marvis.mql.model.*

/**
 * 大师级策略模板库 — 8套经典交易策略一键生成
 */
object TemplateLibrary {

    data class Template(
        val id: String,
        val name: String,
        val description: String,
        val category: String,
        val difficulty: String,
        val params: List<ParamDef> = emptyList()
    )

    data class ParamDef(
        val name: String,
        val label: String,
        val default: String,
        val min: String = "1",
        val max: String = "200"
    )

    val allTemplates = listOf(
        Template("MA_CROSS", "双均线交叉", "快线上穿慢线做多，死叉平多；金叉+双均线排列多空", "趋势跟踪", "入门",
            listOf(ParamDef("FAST", "快线周期", "5"), ParamDef("SLOW", "慢线周期", "20"))),
        Template("MACD_ZERO", "MACD零轴突破", "DIFF上穿零轴开多，下穿零轴平多；可配置做空方向", "趋势跟踪", "入门",
            listOf(ParamDef("S", "短周期", "12"), ParamDef("L", "长周期", "26"), ParamDef("M", "平滑周期", "9"))),
        Template("BOLL_BREAK", "布林带突破", "价格突破上轨开多，回穿中轨平多；跌破下轨开空", "突破交易", "进阶",
            listOf(ParamDef("N", "布林周期", "20"), ParamDef("P", "标准差倍数", "2"))),
        Template("KDJ_OVERBOUGHT", "KDJ超买超卖", "J值低于20超卖金叉开多，高于80死叉平多；双向超买超卖", "震荡交易", "进阶",
            listOf(ParamDef("N", "KDJ周期", "9"), ParamDef("OVERBOUGHT", "超买线", "80"), ParamDef("OVERSOLD", "超卖线", "20"))),
        Template("TURTLE", "海龟交易", "20日突破开仓+10日反向突破平仓；ATR动态止损+金字塔加仓", "趋势跟踪", "专业",
            listOf(ParamDef("ENTRY_N", "突破周期", "20"), ParamDef("EXIT_N", "退出周期", "10"), ParamDef("ATR_N", "ATR周期", "20"))),
        Template("ADX_TREND", "ADX趋势过滤", "ADX>25趋势确认+DI交叉开仓；动态阈值趋势跟踪", "趋势跟踪", "专业",
            listOf(ParamDef("ADX_N", "ADX周期", "14"), ParamDef("ADX_THRESHOLD", "趋势阈值", "25"))),
        Template("RSI_MEAN_REV", "RSI均值回归", "RSI超卖反弹+超买回落；配合布林带外轨确认", "均值回归", "进阶",
            listOf(ParamDef("RSI_N", "RSI周期", "14"), ParamDef("RSI_OVERBOUGHT", "超买线", "70"), ParamDef("RSI_OVERSOLD", "超卖线", "30"))),
        Template("SAR_REVERSAL", "SAR反转", "SAR转向点开仓+趋势线止损；支持双向反转交易", "反转交易", "专业",
            listOf(ParamDef("SAR_N", "SAR步长", "4"), ParamDef("SAR_STEP", "加速因子", "0.02"), ParamDef("SAR_MAX", "最大因子", "0.2")))
    )

    fun buildConfig(templateId: String, paramOverrides: Map<String, String> = emptyMap()): StrategyConfig {
        val template = allTemplates.find { it.id == templateId }
            ?: return StrategyConfig(name = "UnknownTemplate")

        return when (templateId) {
            "MA_CROSS" -> buildMACross(paramOverrides)
            "MACD_ZERO" -> buildMACDZero(paramOverrides)
            "BOLL_BREAK" -> buildBollBreak(paramOverrides)
            "KDJ_OVERBOUGHT" -> buildKDJOverbought(paramOverrides)
            "TURTLE" -> buildTurtle(paramOverrides)
            "ADX_TREND" -> buildADXTrend(paramOverrides)
            "RSI_MEAN_REV" -> buildRSIMeanRev(paramOverrides)
            "SAR_REVERSAL" -> buildSARReversal(paramOverrides)
            else -> StrategyConfig(name = "UnknownTemplate")
        }
    }

    // ================================================================
    // 1. 双均线交叉
    // ================================================================
    private fun buildMACross(params: Map<String, String>): StrategyConfig {
        val fast = params["FAST"] ?: "5"
        val slow = params["SLOW"] ?: "20"
        return StrategyConfig(
            name = "MA_Cross",
            period = "1H",
            tradeDirection = TradeDirection.LONG,
            entryConditions = listOf(ConditionGroup(conditions = listOf(
                SingleCondition(leftIndicator = "MA", operator = "CROSS", rightIndicator = "MA",
                    leftParams = listOf(fast), rightParams = listOf(slow))
            ))),
            exitConditions = listOf(ConditionGroup(conditions = listOf(
                SingleCondition(leftIndicator = "MA", operator = "CROSS_UNDER", rightIndicator = "MA",
                    leftParams = listOf(fast), rightParams = listOf(slow))
            ))),
            stopLoss = StopLossConfig(type = "ATR", atrPeriod = 14, atrMultiplier = 2.0),
            takeProfit = TakeProfitConfig(type = "RISK_REWARD", riskRewardRatio = 2.0),
            templateId = "MA_CROSS"
        )
    }

    // ================================================================
    // 2. MACD零轴突破
    // ================================================================
    private fun buildMACDZero(params: Map<String, String>): StrategyConfig {
        val s = params["S"] ?: "12"; val l = params["L"] ?: "26"; val m = params["M"] ?: "9"
        return StrategyConfig(
            name = "MACD_Zero",
            period = "1H",
            tradeDirection = TradeDirection.BOTH,
            entryConditions = listOf(ConditionGroup(conditions = listOf(
                SingleCondition(leftIndicator = "DIFF", operator = "CROSS", rightIndicator = "CLOSE",
                    leftParams = listOf(s, l), rightParams = listOf("0"))
            ))),
            exitConditions = listOf(ConditionGroup(conditions = listOf(
                SingleCondition(leftIndicator = "DIFF", operator = "CROSS_UNDER", rightIndicator = "CLOSE",
                    leftParams = listOf(s, l), rightParams = listOf("0"))
            ))),
            shortEntryConditions = listOf(ConditionGroup(conditions = listOf(
                SingleCondition(leftIndicator = "DIFF", operator = "CROSS", rightIndicator = "CLOSE",
                    leftParams = listOf(s, l), rightParams = listOf("0"))
            ))),
            shortExitConditions = listOf(ConditionGroup(conditions = listOf(
                SingleCondition(leftIndicator = "DIFF", operator = "CROSS_UNDER", rightIndicator = "CLOSE",
                    leftParams = listOf(s, l), rightParams = listOf("0"))
            ))),
            stopLoss = StopLossConfig(type = "ATR", atrPeriod = 14, atrMultiplier = 2.5),
            takeProfit = TakeProfitConfig(type = "ATR", atrMultiplier = 4.0),
            signalFilter = SignalFilterConfig(filterBars = 5, enabled = true),
            templateId = "MACD_ZERO"
        )
    }

    // ================================================================
    // 3. 布林带突破
    // ================================================================
    private fun buildBollBreak(params: Map<String, String>): StrategyConfig {
        val n = params["N"] ?: "20"; val p = params["P"] ?: "2"
        return StrategyConfig(
            name = "Boll_Break",
            period = "30min",
            tradeDirection = TradeDirection.BOTH,
            entryConditions = listOf(ConditionGroup(conditions = listOf(
                SingleCondition(leftIndicator = "CLOSE", operator = "CROSS", rightIndicator = "UB",
                    leftParams = listOf(), rightParams = listOf("CLOSE", n, p))
            ))),
            exitConditions = listOf(ConditionGroup(conditions = listOf(
                SingleCondition(leftIndicator = "CLOSE", operator = "CROSS_UNDER", rightIndicator = "BOLL",
                    leftParams = listOf(), rightParams = listOf("CLOSE", n, p))
            ))),
            shortEntryConditions = listOf(ConditionGroup(conditions = listOf(
                SingleCondition(leftIndicator = "CLOSE", operator = "CROSS", rightIndicator = "LB",
                    leftParams = listOf(), rightParams = listOf("CLOSE", n, p))
            ))),
            shortExitConditions = listOf(ConditionGroup(conditions = listOf(
                SingleCondition(leftIndicator = "CLOSE", operator = "CROSS_UNDER", rightIndicator = "BOLL",
                    leftParams = listOf(), rightParams = listOf("CLOSE", n, p))
            ))),
            stopLoss = StopLossConfig(type = "ATR", atrPeriod = 20, atrMultiplier = 1.5),
            takeProfit = TakeProfitConfig(type = "ATR", atrMultiplier = 3.0),
            templateId = "BOLL_BREAK"
        )
    }

    // ================================================================
    // 4. KDJ超买超卖
    // ================================================================
    private fun buildKDJOverbought(params: Map<String, String>): StrategyConfig {
        val n = params["N"] ?: "9"; val overbought = params["OVERBOUGHT"] ?: "80"; val oversold = params["OVERSOLD"] ?: "20"
        return StrategyConfig(
            name = "KDJ_Overbought",
            period = "15min",
            tradeDirection = TradeDirection.BOTH,
            entryConditions = listOf(ConditionGroup(conditions = listOf(
                SingleCondition(leftIndicator = "KDJ_J", operator = "CROSS", rightIndicator = "KDJ_D",
                    leftParams = listOf(n, "3", "3"), rightParams = listOf(n, "3", "3"))
            )), ConditionGroup(conditions = listOf(
                SingleCondition(leftIndicator = "KDJ_J", operator = "<", rightIndicator = "CLOSE",
                    leftParams = listOf(n, "3", "3"), rightParams = listOf(oversold))
            ))),
            exitConditions = listOf(ConditionGroup(conditions = listOf(
                SingleCondition(leftIndicator = "KDJ_J", operator = ">", rightIndicator = "CLOSE",
                    leftParams = listOf(n, "3", "3"), rightParams = listOf(overbought))
            ))),
            shortEntryConditions = listOf(ConditionGroup(conditions = listOf(
                SingleCondition(leftIndicator = "KDJ_J", operator = "CROSS_UNDER", rightIndicator = "KDJ_D",
                    leftParams = listOf(n, "3", "3"), rightParams = listOf(n, "3", "3"))
            )), ConditionGroup(conditions = listOf(
                SingleCondition(leftIndicator = "KDJ_J", operator = ">", rightIndicator = "CLOSE",
                    leftParams = listOf(n, "3", "3"), rightParams = listOf(overbought))
            ))),
            shortExitConditions = listOf(ConditionGroup(conditions = listOf(
                SingleCondition(leftIndicator = "KDJ_J", operator = "<", rightIndicator = "CLOSE",
                    leftParams = listOf(n, "3", "3"), rightParams = listOf(oversold))
            ))),
            stopLoss = StopLossConfig(type = "FIXED", value = 2.0),
            takeProfit = TakeProfitConfig(type = "FIXED", value = 4.0),
            templateId = "KDJ_OVERBOUGHT"
        )
    }

    // ================================================================
    // 5. 海龟交易
    // ================================================================
    private fun buildTurtle(params: Map<String, String>): StrategyConfig {
        val entryN = params["ENTRY_N"] ?: "20"; val exitN = params["EXIT_N"] ?: "10"; val atrN = params["ATR_N"] ?: "20"
        return StrategyConfig(
            name = "Turtle",
            period = "1D",
            tradeDirection = TradeDirection.LONG,
            entryConditions = listOf(ConditionGroup(conditions = listOf(
                SingleCondition(leftIndicator = "HIGH", operator = "BREAK_HHV", rightIndicator = "CLOSE",
                    leftParams = listOf(), rightParams = listOf(entryN))
            ))),
            exitConditions = listOf(ConditionGroup(conditions = listOf(
                SingleCondition(leftIndicator = "LOW", operator = "BREAK_LLV", rightIndicator = "CLOSE",
                    leftParams = listOf(), rightParams = listOf(exitN))
            ))),
            stopLoss = StopLossConfig(type = "ATR", atrPeriod = atrN.toIntOrNull() ?: 20, atrMultiplier = 2.0),
            takeProfit = TakeProfitConfig(type = "RISK_REWARD", riskRewardRatio = 3.0),
            moneyManagement = MoneyManagementConfig(mode = "PERCENT_RISK", riskPercent = 2.0, maxPositionPercent = 20.0, enabled = true),
            trailingStop = TrailingStopConfig(atrPeriod = atrN.toIntOrNull() ?: 20, atrMultiplier = 2.0, activationPercent = 2.0, enabled = true),
            signalFilter = SignalFilterConfig(filterBars = 2, enabled = true),
            templateId = "TURTLE"
        )
    }

    // ================================================================
    // 6. ADX趋势过滤
    // ================================================================
    private fun buildADXTrend(params: Map<String, String>): StrategyConfig {
        val adxN = params["ADX_N"] ?: "14"; val threshold = params["ADX_THRESHOLD"] ?: "25"
        return StrategyConfig(
            name = "ADX_Trend",
            period = "4H",
            tradeDirection = TradeDirection.BOTH,
            entryConditions = listOf(ConditionGroup(conditions = listOf(
                SingleCondition(leftIndicator = "PDI", operator = "CROSS", rightIndicator = "MDI",
                    leftParams = listOf(adxN, "6"), rightParams = listOf(adxN, "6")),
                SingleCondition(leftIndicator = "ADX", operator = ">", rightIndicator = "CLOSE",
                    leftParams = listOf(adxN, "6"), rightParams = listOf(threshold))
            ))),
            exitConditions = listOf(ConditionGroup(conditions = listOf(
                SingleCondition(leftIndicator = "PDI", operator = "CROSS_UNDER", rightIndicator = "MDI",
                    leftParams = listOf(adxN, "6"), rightParams = listOf(adxN, "6"))
            ))),
            shortEntryConditions = listOf(ConditionGroup(conditions = listOf(
                SingleCondition(leftIndicator = "MDI", operator = "CROSS", rightIndicator = "PDI",
                    leftParams = listOf(adxN, "6"), rightParams = listOf(adxN, "6")),
                SingleCondition(leftIndicator = "ADX", operator = ">", rightIndicator = "CLOSE",
                    leftParams = listOf(adxN, "6"), rightParams = listOf(threshold))
            ))),
            shortExitConditions = listOf(ConditionGroup(conditions = listOf(
                SingleCondition(leftIndicator = "MDI", operator = "CROSS_UNDER", rightIndicator = "PDI",
                    leftParams = listOf(adxN, "6"), rightParams = listOf(adxN, "6"))
            ))),
            stopLoss = StopLossConfig(type = "ATR", atrPeriod = 14, atrMultiplier = 3.0),
            takeProfit = TakeProfitConfig(type = "RISK_REWARD", riskRewardRatio = 2.5),
            trailingStop = TrailingStopConfig(atrPeriod = 14, atrMultiplier = 3.0, activationPercent = 2.0, enabled = true),
            templateId = "ADX_TREND"
        )
    }

    // ================================================================
    // 7. RSI均值回归
    // ================================================================
    private fun buildRSIMeanRev(params: Map<String, String>): StrategyConfig {
        val rsiN = params["RSI_N"] ?: "14"; val overbought = params["RSI_OVERBOUGHT"] ?: "70"; val oversold = params["RSI_OVERSOLD"] ?: "30"
        return StrategyConfig(
            name = "RSI_MeanRev",
            period = "30min",
            tradeDirection = TradeDirection.BOTH,
            entryConditions = listOf(ConditionGroup(conditions = listOf(
                SingleCondition(leftIndicator = "RSI", operator = "REF_UP", rightIndicator = "CLOSE",
                    leftParams = listOf("CLOSE", rsiN), rightParams = listOf(oversold)),
                SingleCondition(leftIndicator = "RSI", operator = ">", rightIndicator = "CLOSE",
                    leftParams = listOf("CLOSE", rsiN), rightParams = listOf(oversold))
            ))),
            exitConditions = listOf(ConditionGroup(conditions = listOf(
                SingleCondition(leftIndicator = "RSI", operator = ">", rightIndicator = "CLOSE",
                    leftParams = listOf("CLOSE", rsiN), rightParams = listOf(overbought))
            ))),
            shortEntryConditions = listOf(ConditionGroup(conditions = listOf(
                SingleCondition(leftIndicator = "RSI", operator = "REF_DOWN", rightIndicator = "CLOSE",
                    leftParams = listOf("CLOSE", rsiN), rightParams = listOf(overbought)),
                SingleCondition(leftIndicator = "RSI", operator = "<", rightIndicator = "CLOSE",
                    leftParams = listOf("CLOSE", rsiN), rightParams = listOf(overbought))
            ))),
            shortExitConditions = listOf(ConditionGroup(conditions = listOf(
                SingleCondition(leftIndicator = "RSI", operator = "<", rightIndicator = "CLOSE",
                    leftParams = listOf("CLOSE", rsiN), rightParams = listOf(oversold))
            ))),
            stopLoss = StopLossConfig(type = "FIXED", value = 3.0),
            takeProfit = TakeProfitConfig(type = "FIXED", value = 5.0),
            templateId = "RSI_MEAN_REV"
        )
    }

    // ================================================================
    // 8. SAR反转
    // ================================================================
    private fun buildSARReversal(params: Map<String, String>): StrategyConfig {
        val sarN = params["SAR_N"] ?: "4"; val step = params["SAR_STEP"] ?: "0.02"; val maxp = params["SAR_MAX"] ?: "0.2"
        return StrategyConfig(
            name = "SAR_Reversal",
            period = "1H",
            tradeDirection = TradeDirection.BOTH,
            entryConditions = listOf(ConditionGroup(conditions = listOf(
                SingleCondition(leftIndicator = "CLOSE", operator = "CROSS", rightIndicator = "SAR",
                    leftParams = listOf(), rightParams = listOf(sarN, step, maxp))
            ))),
            exitConditions = listOf(ConditionGroup(conditions = listOf(
                SingleCondition(leftIndicator = "CLOSE", operator = "CROSS_UNDER", rightIndicator = "SAR",
                    leftParams = listOf(), rightParams = listOf(sarN, step, maxp))
            ))),
            shortEntryConditions = listOf(ConditionGroup(conditions = listOf(
                SingleCondition(leftIndicator = "CLOSE", operator = "CROSS_UNDER", rightIndicator = "SAR",
                    leftParams = listOf(), rightParams = listOf(sarN, step, maxp))
            ))),
            shortExitConditions = listOf(ConditionGroup(conditions = listOf(
                SingleCondition(leftIndicator = "CLOSE", operator = "CROSS", rightIndicator = "SAR",
                    leftParams = listOf(), rightParams = listOf(sarN, step, maxp))
            ))),
            stopLoss = StopLossConfig(type = "LOW_HIGH", value = 5.0),
            takeProfit = TakeProfitConfig(type = "RISK_REWARD", riskRewardRatio = 2.0),
            signalFilter = SignalFilterConfig(filterBars = 3, enableReverseSignal = true, enabled = true),
            templateId = "SAR_REVERSAL"
        )
    }
}
