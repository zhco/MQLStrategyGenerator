package com.marvis.mql.model

data class ConditionModel(
    val id: String,
    val category: String,
    val description: String = ""
)

// ============================================================
// 策略配置（大师级）
// ============================================================
data class StrategyConfig(
    val name: String = "MyStrategy",
    val period: String = "1H",
    val capital: Double = 100000.0,
    val tradeDirection: TradeDirection = TradeDirection.LONG,
    // 开/平仓条件
    val entryConditions: List<ConditionGroup> = emptyList(),
    val exitConditions: List<ConditionGroup> = emptyList(),
    val shortEntryConditions: List<ConditionGroup> = emptyList(),
    val shortExitConditions: List<ConditionGroup> = emptyList(),
    // 止损止盈
    val stopLoss: StopLossConfig? = null,
    val takeProfit: TakeProfitConfig? = null,
    val trailingStop: TrailingStopConfig? = null,
    // 资金管理
    val moneyManagement: MoneyManagementConfig? = null,
    // 信号过滤
    val signalFilter: SignalFilterConfig? = null,
    // 时间过滤
    val timeFilter: TimeFilterConfig? = null,
    // 跨周期引用
    val crossPeriodRefs: List<CrossPeriodRef> = emptyList(),
    // 回测参数
    val backtestConfig: BacktestConfig? = null,
    // 全局中间变量
    val globalVariables: List<String> = emptyList(),
    // 大师级新增
    val multiInstrument: MultiInstrumentConfig? = null,
    val drawing: DrawingConfig? = null,
    val templateId: String = "",
    // 终极级新增
    val pyramid: PyramidConfig? = null,
    val riskControl: RiskControlConfig? = null,
    val paramOptimization: ParamOptimization? = null,
    val strategyGroup: StrategyGroupConfig? = null,
    // 生成选项
    val annotateCode: Boolean = false     // 是否自动添加逐行注释
)

enum class TradeDirection { LONG, SHORT, BOTH }

// ============================================================
// 条件
// ============================================================
data class ConditionGroup(
    val id: String = "",
    val conditions: List<SingleCondition> = emptyList()
)

data class SingleCondition(
    val id: String = "",
    val leftIndicator: String = "",
    val operator: String = "CROSS",
    val rightIndicator: String = "",
    val leftParams: List<String> = emptyList(),
    val rightParams: List<String> = emptyList(),
    val enabled: Boolean = true
)

// ============================================================
// 止损止盈
// ============================================================
data class StopLossConfig(
    val type: String = "FIXED",     // FIXED / ATR / PERCENT / TRAILING / MOVING_AVG / LOW_HIGH
    val value: Double = 2.0,
    val atrPeriod: Int = 14,
    val atrMultiplier: Double = 2.0,
    val enabled: Boolean = true
)

data class TakeProfitConfig(
    val type: String = "FIXED",     // FIXED / PERCENT / ATR / FIBONACCI / RISK_REWARD
    val value: Double = 5.0,
    val atrMultiplier: Double = 3.0,
    val riskRewardRatio: Double = 2.0,
    val enabled: Boolean = true
)

data class TrailingStopConfig(
    val type: String = "ATR",       // ATR / PERCENT / FIXED / PARABOLIC
    val value: Double = 2.0,
    val atrPeriod: Int = 14,
    val atrMultiplier: Double = 3.0,
    val activationPercent: Double = 1.0, // 盈利达到多少才激活移动止损
    val enabled: Boolean = false
)

// ============================================================
// 资金管理
// ============================================================
data class MoneyManagementConfig(
    val mode: String = "FIXED",          // FIXED / PERCENT_RISK / KELLY / FIXED_RATIO / MARTINGALE
    val fixedLots: Int = 1,
    val riskPercent: Double = 2.0,       // 单笔风险占总资金百分比
    val maxPositionPercent: Double = 30.0, // 最大持仓占总资金百分比
    val initialCapital: Double = 100000.0,
    val compoundProfit: Boolean = false,  // 是否复利
    val enabled: Boolean = false
)

// ============================================================
// 信号过滤
// ============================================================
data class SignalFilterConfig(
    val filterDuplicate: Boolean = true,
    val filterBars: Int = 5,              // 过滤周期内的重复信号
    val filterSameDirection: Boolean = true,
    val enableReverseSignal: Boolean = false, // 反向信号立即平仓
    val enabled: Boolean = false
)

// ============================================================
// 时间过滤
// ============================================================
data class TimeFilterConfig(
    val tradeStartTime: String = "09:00",
    val tradeEndTime: String = "15:00",
    val avoidOvernight: Boolean = true,
    val enabled: Boolean = false
)

// ============================================================
// 跨周期引用
// ============================================================
data class CrossPeriodRef(
    val period: String,                    // MIN5 / MIN15 / MIN30 / HOUR1 / HOUR4 / DAY / WEEK
    val indicator: String,
    val alias: String,
    val params: List<String> = emptyList()
)

// ============================================================
// 回测参数
// ============================================================
data class BacktestConfig(
    val startDate: String = "2023-01-01",
    val endDate: String = "2024-12-31",
    val initialCapital: Double = 100000.0,
    val commissionRate: Double = 0.0003,   // 手续费
    val slippage: Double = 1.0,            // 滑点（最小变动价位）
    val contractSize: Int = 1,             // 每手数量
    val marginRate: Double = 0.1,          // 保证金比例
    val maxBarsBack: Int = 200             // 最大回溯K线数
)

// ============================================================
// 大师级新增 - 多品种并发
// ============================================================
data class MultiInstrumentConfig(
    val instruments: List<String> = listOf("RB"),
    val groupName: String = "MultiInstrument",
    val enabled: Boolean = false
)

// ============================================================
// 大师级新增 - 绘图标注
// ============================================================
data class DrawingConfig(
    val showEntryIcon: Boolean = true,     // 开仓点画图标
    val showExitIcon: Boolean = true,      // 平仓点画图标
    val entryIconType: Int = 1,            // 图标类型: 1=向上箭头 2=向下箭头 3=圆形 4=笑脸 5=三角
    val exitIconType: Int = 2,
    val showEntryText: Boolean = false,    // 开仓文字标注
    val entryText: String = "买入",
    val showExitText: Boolean = false,     // 平仓文字标注
    val exitText: String = "卖出",
    val showStopLossLine: Boolean = false, // 是否画止损线
    val enabled: Boolean = false
)

// ============================================================
// 终极级新增 - 金字塔/分批建仓
// ============================================================
data class PyramidConfig(
    val entryType: String = "PYRAMID",     // PYRAMID / GRID / SCALED / MARTIN
    val baseLots: Int = 1,                 // 基础手数
    val pyramidLayers: Int = 3,            // 加仓层数
    val pyramidInterval: String = "ATR",   // ATR / PERCENT / FIXED
    val pyramidIntervalValue: Double = 1.0,// 加仓间隔（ATR倍数/%/固定点数）
    val pyramidDirection: String = "SAME", // SAME=同向加仓 COUNTER=反向加仓
    val pyramidMultiplier: Double = 1.0,   // 手数递增倍率 (1=等量, 2=翻倍)
    val takeProfitOnLastLayer: Boolean = false, // 最后一层才设止盈
    val averageExit: Boolean = true,       // 按均价平仓
    val enabled: Boolean = false
)

// ============================================================
// 终极级新增 - 日内风控熔断
// ============================================================
data class RiskControlConfig(
    val maxDailyLoss: Double = 5000.0,     // 日内最大亏损金额，触发后当日停止交易
    val maxDailyLossPercent: Double = 5.0, // 日内最大亏损百分比
    val maxConsecutiveLoss: Int = 5,       // 连续亏损笔数后暂停
    val maxDailyTrades: Int = 20,          // 日内最大交易次数
    val cooldownMinutes: Int = 30,         // 熔断冷却时间（分钟）
    val maxPositionCount: Int = 3,         // 最大同时持仓品种数
    val maxtradeLoss: Double = 1000.0,     // 单笔最大亏损
    val enabled: Boolean = false
)

// ============================================================
// 终极级新增 - 参数优化注解
// ============================================================
data class ParamOptimization(
    val enableGridSearch: Boolean = true,  // 启用网格搜索
    val optimizedParams: List<OptimizedParam> = emptyList(),
    val objectiveFunction: String = "SHARPE_RATIO", // SHARPE_RATIO / PROFIT_FACTOR / NET_PROFIT / WIN_RATE
    val populationSize: Int = 50,          // 遗传算法种群大小
    val generations: Int = 20,             // 遗传算法代数
    val enabled: Boolean = false
)

data class OptimizedParam(
    val name: String,                      // 参数名
    val start: Double,
    val end: Double,
    val step: Double,
    val current: Double
)

// ============================================================
// 终极级新增 - 策略组合引擎
// ============================================================
data class StrategyGroupConfig(
    val subStrategyIds: List<String> = emptyList(), // 子策略ID列表
    val combinationMode: String = "AND",   // AND=全部满足 / OR=任一满足 / VOTE=多数投票 / WEIGHTED=加权
    val voteThreshold: Double = 0.5,       // 投票/加权阈值
    val weights: List<Double> = emptyList(), // 各子策略权重
    val ignoreExitSignal: Boolean = false, // 组合模式下忽略子策略平仓信号
    val enabled: Boolean = false
)

// ============================================================
// 自定义指标
// ============================================================
data class IndicatorConfig(
    val name: String = "MyIndicator",
    val params: List<IndicatorParam> = emptyList(),
    val lines: List<IndicatorLine> = emptyList(),
    val sourceCode: String = ""
)

data class IndicatorParam(
    val name: String,
    val min: Double = 0.0,
    val max: Double = 100.0,
    val default: Double = 20.0,
    val step: Double = 1.0
)

data class IndicatorLine(
    val name: String,
    val color: String = "#FF0000",
    val width: Int = 1,
    val style: String = "SOLID"
)

// ============================================================
// 结果
// ============================================================
data class CompileResult(
    val success: Boolean,
    val sourceCode: String = "",
    val errors: List<String> = emptyList(),
    val warnings: List<String> = emptyList(),
    val generatedIndicator: IndicatorConfig? = null
)

data class GenerationResult(
    val success: Boolean,
    val sourceCode: String = "",
    val strategyName: String = "",
    val errors: List<String> = emptyList(),
    val summary: String = ""
)
