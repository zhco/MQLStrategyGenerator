package com.marvis.mql.generator

import com.marvis.mql.parser.MqlLexer
import com.marvis.mql.parser.MqlParser
import com.marvis.mql.model.*

/**
 * 麦语言自定义指标编译器
 * 将手写的麦语言指标代码解析并编译为可交付的指标定义
 */
class IndicatorCompiler {

    fun compile(sourceCode: String, name: String = "MyIndicator"): CompileResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        // 1. 词法分析
        val lexer = MqlLexer(sourceCode)
        val tokens = lexer.tokenize()
        val lexerErrors = lexer.getErrors()
        if (lexerErrors.isNotEmpty()) {
            errors.addAll(lexerErrors.map { "词法错误: $it" })
            return CompileResult(false, sourceCode, errors, warnings)
        }

        // 2. 语法分析
        val parser = MqlParser(tokens)
        val ast = parser.parse()
        val parserErrors = parser.getErrors()
        if (parserErrors.isNotEmpty()) {
            errors.addAll(parserErrors.map { "语法错误: $it" })
            return CompileResult(false, sourceCode, errors, warnings)
        }

        // 3. 语义分析 & 提取指标结构
        val indicatorConfig = extractIndicatorConfig(ast, name, sourceCode, warnings)

        // 4. 生成标准化代码
        val normalizedCode = normalizeIndicatorCode(sourceCode, indicatorConfig)

        return if (errors.isEmpty()) {
            CompileResult(
                success = true,
                sourceCode = normalizedCode,
                errors = errors,
                warnings = warnings,
                generatedIndicator = indicatorConfig
            )
        } else {
            CompileResult(false, sourceCode, errors, warnings)
        }
    }

    private fun extractIndicatorConfig(
        ast: com.marvis.mql.parser.ProgramNode,
        name: String,
        sourceCode: String,
        warnings: MutableList<String>
    ): IndicatorConfig {
        val params = mutableListOf<IndicatorParam>()
        val lines = mutableListOf<IndicatorLine>()

        // 分析 AST 提取线定义和绘图语句
        val lineNames = mutableSetOf<String>()
        val drawColors = mutableMapOf<String, String>()
        val drawWidths = mutableMapOf<String, Int>()

        ast.statements.forEach { stmt ->
            when (stmt) {
                is com.marvis.mql.parser.AssignmentStatement -> {
                    val normalized = stmt.name.uppercase()
                    if (!lineNames.contains(normalized)) {
                        lineNames.add(normalized)
                        lines.add(IndicatorLine(normalized))
                    }
                }
                is com.marvis.mql.parser.DrawStatement -> {
                    stmt.color?.let { color ->
                        stmt.value?.let {
                            // 可据此关联线和颜色
                        }
                    }
                }
                else -> {}
            }
        }

        // 如果没有找到线，默认添加一条主指标线
        if (lines.isEmpty()) {
            lines.add(IndicatorLine(name.uppercase(), "#FF6B35", 2))
        }

        // 提取 INPUT 参数
        val inputRegex = Regex("""INPUT\s*:\s*(.+?)\s*;""", RegexOption.IGNORE_CASE)
        val inputMatch = inputRegex.find(sourceCode)
        if (inputMatch != null) {
            val paramDefs = inputMatch.groupValues[1].split(",")
            paramDefs.forEach { def ->
                val parts = def.trim().split("(", ")")
                if (parts.size >= 2) {
                    val name = parts[0].trim()
                    val valParts = parts[1].split(",")
                    if (valParts.size >= 4) {
                        params.add(
                            IndicatorParam(
                                name = name,
                                default = valParts[0].trim().toDoubleOrNull() ?: 20.0,
                                min = valParts[1].trim().toDoubleOrNull() ?: 0.0,
                                max = valParts[2].trim().toDoubleOrNull() ?: 100.0,
                                step = valParts[3].trim().toDoubleOrNull() ?: 1.0
                            )
                        )
                    }
                }
            }
        }

        if (params.isEmpty() && lines.size == 1) {
            params.add(IndicatorParam("N", 1.0, 100.0, 20.0, 1.0))
            warnings.add("未检测到INPUT参数，已自动添加默认参数 N(1,100,20,1)")
        }

        return IndicatorConfig(
            name = name,
            params = params,
            lines = lines,
            sourceCode = sourceCode
        )
    }

    private fun normalizeIndicatorCode(
        sourceCode: String,
        config: IndicatorConfig
    ): String {
        val sb = StringBuilder()

        sb.appendLine("// ============================================")
        sb.appendLine("// 指标名称: ${config.name}")
        sb.appendLine("// 指标线数: ${config.lines.size}")
        sb.appendLine("// 参数数量: ${config.params.size}")
        sb.appendLine("// ============================================")
        sb.appendLine()

        // 参数定义
        if (config.params.isNotEmpty()) {
            val paramStr = config.params.joinToString(", ") {
                "${it.name}(${it.default.toInt()},${it.min.toInt()},${it.max.toInt()},${it.step.toInt()})"
            }
            sb.appendLine("INPUT: $paramStr;")
            sb.appendLine()
        }

        // 原始代码主体（去除注释后）
        val cleanCode = sourceCode
            .replace(Regex("""//[^\n]*"""), "")
            .replace(Regex("""\{[^}]*\}"""), "")
            .replace(Regex("""INPUT\s*:\s*.+?;""", RegexOption.IGNORE_CASE), "")
            .trim()

        sb.appendLine(cleanCode)

        return sb.toString()
    }

    /** 快速语法检查 */
    fun quickCheck(sourceCode: String): List<String> {
        val lexer = MqlLexer(sourceCode)
        lexer.tokenize()
        val lexerErrors = lexer.getErrors()
        if (lexerErrors.isNotEmpty()) return lexerErrors.map { "词法: $it" }

        val tokens = lexer.tokenize()
        val parser = MqlParser(tokens.subList(0, tokens.size - 1))
        parser.parse()
        return parser.getErrors().map { "语法: $it" }
    }
}
