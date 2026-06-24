package com.marvis.mql.parser

/**
 * 麦语言 AST 节点定义
 * 覆盖完整的麦语言语法结构
 */
sealed class AstNode

// === 程序根节点 ===
data class ProgramNode(val statements: List<AstNode>) : AstNode()

// === 语句 ===
data class IfStatement(
    val condition: AstNode,
    val thenBlock: List<AstNode>,
    val elseBlock: List<AstNode> = emptyList()
) : AstNode()

data class BlockStatement(val statements: List<AstNode>) : AstNode()

data class AssignmentStatement(
    val name: String,
    val value: AstNode,
    val isColonAssign: Boolean = true
) : AstNode()

data class TradeInstruction(
    val type: TradeType,
    val condition: AstNode? = null,
    val price: AstNode? = null,
    val volume: AstNode? = null
) : AstNode()

data class ExpressionStatement(val expression: AstNode) : AstNode()

// === 表达式 ===
data class BinaryExpression(
    val left: AstNode,
    val operator: String,
    val right: AstNode
) : AstNode()

data class UnaryExpression(
    val operator: String,
    val operand: AstNode
) : AstNode()

data class FunctionCall(
    val name: String,
    val arguments: List<AstNode>
) : AstNode()

data class CrossExpression(
    val left: AstNode,
    val right: AstNode,
    val isUnder: Boolean = false
) : AstNode()

data class ConditionalExpression(
    val condition: AstNode,
    val trueExpr: AstNode,
    val falseExpr: AstNode
) : AstNode()

data class DataRef(val name: String) : AstNode()

data class NumberLiteral(val value: Double) : AstNode()

data class StringLiteral(val value: String) : AstNode()

data class VariableRef(val name: String) : AstNode()

// === 指标定义 ===
data class IndicatorDefinition(
    val name: String,
    val params: List<ParamDef>,
    val body: List<AstNode>,
    val overlay: Boolean = false
) : AstNode()

data class ParamDef(
    val name: String,
    val defaultValue: AstNode? = null,
    val min: Double? = null,
    val max: Double? = null,
    val step: Double? = null
) : AstNode()

data class DrawStatement(
    val drawType: String,
    val condition: AstNode?,
    val value: AstNode?,
    val color: String? = null,
    val width: Int? = null
) : AstNode()

// === 跨周期引用 ===
data class ImportDeclaration(
    val period: String,
    val indicator: String,
    val alias: String
) : AstNode()

// === 交易指令类型 ===
enum class TradeType {
    BUY, SELL, SHORT, COVER,
    BUYSHORT, SELLSHORT,
    ENTERLONG, EXITLONG, ENTERSHORT, EXITSHORT
}
