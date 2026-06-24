package com.marvis.mql.library

/**
 * 麦语言完整函数库定义
 * 包含所有标准函数、参数说明、用法示例
 */
object MqlLibrary {

    data class FunctionDef(
        val name: String,
        val category: String,
        val description: String,
        val syntax: String,
        val params: List<ParamInfo> = emptyList(),
        val returns: String = "数值",
        val example: String = ""
    )

    data class ParamInfo(
        val name: String,
        val description: String,
        val required: Boolean = true,
        val defaultValue: String = ""
    )

    enum class Category(val label: String) {
        DATA_REF("数据引用"),
        MATH_OP("数学运算"),
        LOGIC_OP("逻辑运算"),
        MOVING_AVERAGE("移动平均"),
        OSCILLATOR("震荡指标"),
        TREND("趋势指标"),
        VOLATILITY("波动率指标"),
        VOLUME("成交量指标"),
        REFERENCE("引用函数"),
        STATISTICS("统计函数"),
        TRADE("交易指令"),
        DRAWING("绘图函数"),
        UTIL("工具函数"),
        CROSS_PERIOD("跨周期引用"),
        SYSTEM("系统函数")
    }

    val allFunctions: List<FunctionDef> = buildList {
        // ===== 数据引用 =====
        add(FunctionDef("OPEN", "数据引用", "开盘价", "OPEN", returns = "K线开盘价"))
        add(FunctionDef("HIGH", "数据引用", "最高价", "HIGH", returns = "K线最高价"))
        add(FunctionDef("LOW", "数据引用", "最低价", "LOW", returns = "K线最低价"))
        add(FunctionDef("CLOSE", "数据引用", "收盘价", "CLOSE", returns = "K线收盘价"))
        add(FunctionDef("VOL", "数据引用", "成交量", "VOL", returns = "成交量"))
        add(FunctionDef("OPI", "数据引用", "持仓量", "OPI", returns = "持仓量"))
        add(FunctionDef("AMOUNT", "数据引用", "成交额", "AMOUNT", returns = "成交额"))

        // ===== 移动平均 =====
        add(FunctionDef("MA", "移动平均", "简单移动平均", "MA(X, N)",
            listOf(ParamInfo("X", "数据源"), ParamInfo("N", "周期")),
            returns = "N周期均值", example = "MA(CLOSE, 5)"))
        add(FunctionDef("EMA", "移动平均", "指数移动平均", "EMA(X, N)",
            listOf(ParamInfo("X", "数据源"), ParamInfo("N", "周期")),
            returns = "指数平滑值", example = "EMA(CLOSE, 12)"))
        add(FunctionDef("SMA", "移动平均", "加权移动平均", "SMA(X, N, M)",
            listOf(ParamInfo("X", "数据源"), ParamInfo("N", "周期"), ParamInfo("M", "权重")),
            returns = "加权移动平均值", example = "SMA(CLOSE, 3, 1)"))
        add(FunctionDef("DMA", "移动平均", "动态移动平均", "DMA(X, A)",
            listOf(ParamInfo("X", "数据源"), ParamInfo("A", "动态因子")),
            returns = "动态均值", example = "DMA(CLOSE, VOL/CAPITAL)"))
        add(FunctionDef("WMA", "移动平均", "线性加权移动平均", "WMA(X, N)",
            listOf(ParamInfo("X", "数据源"), ParamInfo("N", "周期")),
            returns = "线性加权均值", example = "WMA(CLOSE, 10)"))

        // ===== 震荡指标 =====
        add(FunctionDef("MACD", "震荡指标", "MACD指标（DIFF值）", "MACD(CLOSE, SHORT, LONG, M)",
            listOf(ParamInfo("CLOSE", "收盘价"), ParamInfo("SHORT", "短周期", true, "12"),
                ParamInfo("LONG", "长周期", true, "26"), ParamInfo("M", "平滑周期", true, "9")),
            returns = "MACD柱线值", example = "MACD(CLOSE, 12, 26, 9)"))
        add(FunctionDef("DIFF", "震荡指标", "MACD快线", "DIFF(CLOSE, SHORT, LONG)",
            listOf(ParamInfo("CLOSE", "收盘价"), ParamInfo("SHORT", "短周期", true, "12"),
                ParamInfo("LONG", "长周期", true, "26")),
            returns = "DIFF值"))
        add(FunctionDef("DEA", "震荡指标", "MACD慢线", "DEA(CLOSE, SHORT, LONG, M)",
            listOf(ParamInfo("CLOSE", "收盘价"), ParamInfo("SHORT", "短周期", true, "12"),
                ParamInfo("LONG", "长周期", true, "26"), ParamInfo("M", "平滑周期", true, "9")),
            returns = "DEA值"))
        add(FunctionDef("KDJ_K", "震荡指标", "KDJ K值", "KDJ_K(N, M1, M2)",
            listOf(ParamInfo("N", "周期", true, "9"), ParamInfo("M1", "平滑因子1", true, "3"),
                ParamInfo("M2", "平滑因子2", true, "3")),
            returns = "K值", example = "KDJ_K(9, 3, 3)"))
        add(FunctionDef("KDJ_D", "震荡指标", "KDJ D值", "KDJ_D(N, M1, M2)",
            listOf(ParamInfo("N", "周期", true, "9"), ParamInfo("M1", "平滑因子1", true, "3"),
                ParamInfo("M2", "平滑因子2", true, "3")),
            returns = "D值"))
        add(FunctionDef("KDJ_J", "震荡指标", "KDJ J值", "KDJ_J(N, M1, M2)",
            listOf(ParamInfo("N", "周期", true, "9"), ParamInfo("M1", "平滑因子1", true, "3"),
                ParamInfo("M2", "平滑因子2", true, "3")),
            returns = "J值"))
        add(FunctionDef("RSI", "震荡指标", "相对强弱指标", "RSI(X, N)",
            listOf(ParamInfo("X", "数据源（通常CLOSE）"), ParamInfo("N", "周期", true, "14")),
            returns = "0-100数值", example = "RSI(CLOSE, 14)"))

        // ===== 趋势指标 =====
        add(FunctionDef("BOLL", "趋势指标", "布林带中轨", "BOLL(CLOSE, N, P)",
            listOf(ParamInfo("CLOSE", "收盘价"), ParamInfo("N", "周期", true, "20"),
                ParamInfo("P", "标准差倍数", true, "2")),
            returns = "中轨值"))
        add(FunctionDef("UB", "趋势指标", "布林带上轨", "UB(CLOSE, N, P)",
            listOf(ParamInfo("CLOSE", "收盘价"), ParamInfo("N", "周期", true, "20"),
                ParamInfo("P", "标准差倍数", true, "2")),
            returns = "上轨值", example = "UB(CLOSE, 20, 2)"))
        add(FunctionDef("LB", "趋势指标", "布林带下轨", "LB(CLOSE, N, P)",
            listOf(ParamInfo("CLOSE", "收盘价"), ParamInfo("N", "周期", true, "20"),
                ParamInfo("P", "标准差倍数", true, "2")),
            returns = "下轨值"))
        add(FunctionDef("SAR", "趋势指标", "抛物线转向", "SAR(N, STEP, MAXP)",
            listOf(ParamInfo("N", "步长因子"), ParamInfo("STEP", "加速因子"),
                ParamInfo("MAXP", "最大加速因子")),
            returns = "SAR值", example = "SAR(4, 0.02, 0.2)"))

        // ===== 波动率指标 =====
        add(FunctionDef("ATR", "波动率指标", "平均真实波幅", "ATR(N)",
            listOf(ParamInfo("N", "周期", true, "14")),
            returns = "ATR值", example = "ATR(14)"))

        // ===== 趋势指标 DMI =====
        add(FunctionDef("PDI", "趋势指标", "上升动向指标", "PDI(N, M)",
            listOf(ParamInfo("N", "周期", true, "14"), ParamInfo("M", "平滑周期", true, "6")),
            returns = "PDI值"))
        add(FunctionDef("MDI", "趋势指标", "下降动向指标", "MDI(N, M)",
            listOf(ParamInfo("N", "周期", true, "14"), ParamInfo("M", "平滑周期", true, "6")),
            returns = "MDI值"))
        add(FunctionDef("ADX", "趋势指标", "平均趋向指数", "ADX(N, M)",
            listOf(ParamInfo("N", "周期", true, "14"), ParamInfo("M", "平滑周期", true, "6")),
            returns = "ADX值", example = "ADX(14, 6)"))

        // ===== 引用函数 =====
        add(FunctionDef("REF", "引用函数", "N周期前的值", "REF(X, N)",
            listOf(ParamInfo("X", "数据源"), ParamInfo("N", "偏移周期")),
            returns = "前N周期值", example = "REF(CLOSE, 1)"))
        add(FunctionDef("HHV", "引用函数", "N周期内最高值", "HHV(X, N)",
            listOf(ParamInfo("X", "数据源"), ParamInfo("N", "周期")),
            returns = "周期内最大值", example = "HHV(HIGH, 20)"))
        add(FunctionDef("LLV", "引用函数", "N周期内最低值", "LLV(X, N)",
            listOf(ParamInfo("X", "数据源"), ParamInfo("N", "周期")),
            returns = "周期内最小值", example = "LLV(LOW, 20)"))
        add(FunctionDef("HHVBARS", "引用函数", "N周期内最高值距当前周期数", "HHVBARS(X, N)",
            listOf(ParamInfo("X", "数据源"), ParamInfo("N", "周期")),
            returns = "周期数", example = "HHVBARS(HIGH, 20)"))
        add(FunctionDef("LLVBARS", "引用函数", "N周期内最低值距当前周期数", "LLVBARS(X, N)",
            listOf(ParamInfo("X", "数据源"), ParamInfo("N", "周期")),
            returns = "周期数", example = "LLVBARS(LOW, 20)"))
        add(FunctionDef("BARSLAST", "引用函数", "上次条件成立距当前周期数", "BARSLAST(X)",
            listOf(ParamInfo("X", "条件")),
            returns = "周期数", example = "BARSLAST(CLOSE>OPEN)"))

        // ===== 统计函数 =====
        add(FunctionDef("COUNT", "统计函数", "N周期内满足条件次数", "COUNT(X, N)",
            listOf(ParamInfo("X", "条件"), ParamInfo("N", "周期")),
            returns = "计数", example = "COUNT(CLOSE>REF(CLOSE,1), 10)"))
        add(FunctionDef("SUM", "统计函数", "N周期内求和", "SUM(X, N)",
            listOf(ParamInfo("X", "数据源"), ParamInfo("N", "周期")),
            returns = "和"))
        add(FunctionDef("EVERY", "统计函数", "N周期内是否每个都满足条件", "EVERY(X, N)",
            listOf(ParamInfo("X", "条件"), ParamInfo("N", "周期")),
            returns = "1或0", example = "EVERY(CLOSE>MA(CLOSE,5), 10)"))
        add(FunctionDef("STD", "统计函数", "N周期内标准差", "STD(X, N)",
            listOf(ParamInfo("X", "数据源"), ParamInfo("N", "周期")),
            returns = "标准差"))
        add(FunctionDef("VAR", "统计函数", "N周期内方差", "VAR(X, N)",
            listOf(ParamInfo("X", "数据源"), ParamInfo("N", "周期")),
            returns = "方差"))
        add(FunctionDef("AVEDEV", "统计函数", "平均绝对偏差", "AVEDEV(X, N)",
            listOf(ParamInfo("X", "数据源"), ParamInfo("N", "周期")),
            returns = "平均绝对偏差"))

        // ===== 数学运算 =====
        add(FunctionDef("ABS", "数学运算", "绝对值", "ABS(X)",
            listOf(ParamInfo("X", "数值")), returns = "绝对值"))
        add(FunctionDef("MAX", "数学运算", "取两值中较大者", "MAX(A, B)",
            listOf(ParamInfo("A", "值A"), ParamInfo("B", "值B")),
            returns = "较大值", example = "MAX(CLOSE, OPEN)"))
        add(FunctionDef("MIN", "数学运算", "取两值中较小者", "MIN(A, B)",
            listOf(ParamInfo("A", "值A"), ParamInfo("B", "值B")),
            returns = "较小值"))
        add(FunctionDef("MOD", "数学运算", "取模", "MOD(A, B)",
            listOf(ParamInfo("A", "被除数"), ParamInfo("B", "除数")),
            returns = "余数"))

        // ===== 逻辑函数 =====
        add(FunctionDef("CROSS", "逻辑运算", "上穿（A从下方向上穿B）", "CROSS(A, B)",
            listOf(ParamInfo("A", "快线"), ParamInfo("B", "慢线")),
            returns = "1(上穿)或0", example = "CROSS(MA(CLOSE,5), MA(CLOSE,10))"))
        add(FunctionDef("IF", "逻辑运算", "条件判断", "IF(COND, A, B)",
            listOf(ParamInfo("COND", "条件"), ParamInfo("A", "成立返回值"), ParamInfo("B", "不成立返回值")),
            returns = "条件值", example = "IF(CLOSE>OPEN, 1, 0)"))
        add(FunctionDef("VALUEWHEN", "逻辑运算", "条件成立时的值", "VALUEWHEN(COND, X)",
            listOf(ParamInfo("COND", "条件"), ParamInfo("X", "取值")),
            returns = "最近满足条件时的X值"))
        add(FunctionDef("FILTER", "逻辑运算", "过滤连续信号", "FILTER(X, N)",
            listOf(ParamInfo("X", "信号"), ParamInfo("N", "过滤周期")),
            returns = "过滤后信号", example = "FILTER(CROSS_COND, 5)"))

        // ===== 交易指令 =====
        add(FunctionDef("BUY", "交易指令", "开多仓", "BUY(COND)",
            listOf(ParamInfo("COND", "买入条件")),
            returns = "多头开仓"))
        add(FunctionDef("SELL", "交易指令", "平多仓", "SELL(COND)",
            listOf(ParamInfo("COND", "卖出条件")),
            returns = "多头平仓"))
        add(FunctionDef("SHORT", "交易指令", "开空仓", "SHORT(COND)",
            listOf(ParamInfo("COND", "做空条件")),
            returns = "空头开仓"))
        add(FunctionDef("COVER", "交易指令", "平空仓", "COVER(COND)",
            listOf(ParamInfo("COND", "回补条件")),
            returns = "空头平仓"))
        add(FunctionDef("BUYSHORT", "交易指令", "开空（同SHORT）", "BUYSHORT(COND)",
            listOf(ParamInfo("COND", "做空条件")),
            returns = "空头开仓"))
        add(FunctionDef("SELLSHORT", "交易指令", "平空（同COVER）", "SELLSHORT(COND)",
            listOf(ParamInfo("COND", "回补条件")),
            returns = "空头平仓"))
        add(FunctionDef("MARKETPOSITION", "交易指令", "当前持仓状态", "MARKETPOSITION",
            returns = "1多头/-1空头/0空仓"))

        // ===== 绘图函数 =====
        add(FunctionDef("DRAWLINE", "绘图函数", "绘制直线", "DRAWLINE(COND1, PRICE1, COND2, PRICE2, EXPAND)",
            listOf(ParamInfo("COND1", "起点条件"), ParamInfo("PRICE1", "起点价格"),
                ParamInfo("COND2", "终点条件"), ParamInfo("PRICE2", "终点价格"),
                ParamInfo("EXPAND", "是否延长", true, "0")),
            returns = "图形"))
        add(FunctionDef("DRAWICON", "绘图函数", "绘制图标", "DRAWICON(COND, PRICE, TYPE)",
            listOf(ParamInfo("COND", "条件"), ParamInfo("PRICE", "位置"),
                ParamInfo("TYPE", "图标类型")),
            returns = "图形"))
        add(FunctionDef("DRAWTEXT", "绘图函数", "绘制文字", "DRAWTEXT(COND, PRICE, TEXT)",
            listOf(ParamInfo("COND", "条件"), ParamInfo("PRICE", "位置"),
                ParamInfo("TEXT", "文字内容")),
            returns = "图形"))
        add(FunctionDef("STICKLINE", "绘图函数", "绘制柱线", "STICKLINE(COND, PRICE1, PRICE2, WIDTH, EMPTY)",
            listOf(ParamInfo("COND", "条件"), ParamInfo("PRICE1", "起点"),
                ParamInfo("PRICE2", "终点"), ParamInfo("WIDTH", "宽度"),
                ParamInfo("EMPTY", "空心", true, "0")),
            returns = "图形"))

        // ===== 跨周期引用 =====
        add(FunctionDef("#IMPORT", "跨周期引用", "跨周期调用指标", "#IMPORT[PERIOD, INDICATOR] AS ALIAS",
            listOf(ParamInfo("PERIOD", "周期(MIN/HOUR/DAY/WEEK/MONTH)"),
                ParamInfo("INDICATOR", "指标名称"), ParamInfo("ALIAS", "别名")),
            returns = "跨周期数据"))
    }

    /** 按分类获取函数列表 */
    fun getFunctionsByCategory(category: String): List<FunctionDef> {
        return allFunctions.filter { it.category == category }
    }

    /** 获取所有分类 */
    fun getAllCategories(): List<String> {
        return Category.entries.map { it.label }
    }

    /** 搜索函数 */
    fun searchFunctions(query: String): List<FunctionDef> {
        val q = query.uppercase()
        return allFunctions.filter {
            it.name.contains(q) || it.description.contains(query) ||
            it.category.contains(query) || it.syntax.contains(q)
        }
    }
}
