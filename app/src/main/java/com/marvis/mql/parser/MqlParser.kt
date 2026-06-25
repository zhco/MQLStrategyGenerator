
package com.marvis.mql.parser

/**
 * 麦语言语法解析器 —— 递归下降解析
 * 将 Token 流解析为 AST（抽象语法树）
 */
class MqlParser(private val tokens: List<Token>) {

    private var pos = 0
    private val errors = mutableListOf<String>()

    fun parse(): ProgramNode {
        val statements = mutableListOf<AstNode>()
        while (!isAtEnd()) {
            val stmt = parseStatement()
            if (stmt != null) statements.add(stmt)
        }
        return ProgramNode(statements)
    }

    private fun parseStatement(): AstNode? {
        skipNewlines()
        if (isAtEnd()) return null

        return when {
            // 跳过 INPUT 声明行（视为注释）
            match(TokenType.IDENTIFIER) -> {
                val name = previous().lexeme.uppercase()
                if (name == "INPUT") {
                    // 跳过整个 INPUT 声明: INPUT: N(20,1,100,1);
                    // 跳过 : 和所有 token 直到 ; 或 NEWLINE
                    skipUntilSemicolonOrNewline()
                    null
                } else if (name in listOf("VAR", "VARIABLE", "GLOBALVARIABLE", "NUMERIC", "STRING", "ARRAY")) {
                    // 跳过变量声明
                    skipUntilSemicolonOrNewline()
                    null
                } else {
                    // 退回，正常处理
                    pos--
                    parseNormalStatement()
                }
            }
            match(TokenType.IF) -> parseIfStatement()
            match(TokenType.BEGIN) -> parseBlockStatement()
            match(TokenType.BUY) -> parseTradeInstruction(TradeType.BUY)
            match(TokenType.SELL) -> parseTradeInstruction(TradeType.SELL)
            match(TokenType.SHORT) -> parseTradeInstruction(TradeType.SHORT)
            match(TokenType.COVER) -> parseTradeInstruction(TradeType.COVER)
            match(TokenType.BUYSHORT) -> parseTradeInstruction(TradeType.BUYSHORT)
            match(TokenType.SELLSHORT) -> parseTradeInstruction(TradeType.SELLSHORT)
            match(TokenType.ENTERLONG) -> parseTradeInstruction(TradeType.ENTERLONG)
            match(TokenType.EXITLONG) -> parseTradeInstruction(TradeType.EXITLONG)
            match(TokenType.ENTERSHORT) -> parseTradeInstruction(TradeType.ENTERSHORT)
            match(TokenType.EXITSHORT) -> parseTradeInstruction(TradeType.EXITSHORT)
            match(TokenType.LINE_COMMENT) -> null
            match(TokenType.NEWLINE) -> null
            match(TokenType.SEMICOLON) -> null
            match(TokenType.COLON) -> {
                // 孤立的冒号，可能是 INPUT：后面的内容，跳过直到语句结束
                skipUntilSemicolonOrNewline()
                null
            }
            else -> parseNormalStatement()
        }
    }

    private fun parseNormalStatement(): AstNode? {
        if (isAtEnd()) return null

        // 检查是否是赋值语句: IDENTIFIER := ...
        if (check(TokenType.IDENTIFIER) && checkNext(TokenType.COLON_ASSIGN)) {
            return parseAssignment()
        }

        val expr = parseExpression()
        skipNewlines()
        if (match(TokenType.SEMICOLON)) skipNewlines()
        // 如果表达式后跟着孤立的 COLON，跳过它及后续内容
        if (match(TokenType.COLON)) skipUntilSemicolonOrNewline()
        return ExpressionStatement(expr)
    }

    private fun skipUntilSemicolonOrNewline() {
        while (!isAtEnd() && !check(TokenType.SEMICOLON) && !check(TokenType.NEWLINE) && !check(TokenType.EOF)) {
            advance()
        }
        if (match(TokenType.SEMICOLON)) skipNewlines()
        skipNewlines()
    }

    private fun parseIfStatement(): IfStatement {
        val condition = parseExpression()
        consume(TokenType.THEN, "期望 'THEN' 关键字")
        skipNewlines()

        val thenBlock = if (check(TokenType.BEGIN)) {
            listOf(parseBlockStatement())
        } else {
            val stmt = parseStatement()
            if (stmt != null) listOf(stmt) else emptyList()
        }

        var elseBlock = emptyList<AstNode>()
        if (match(TokenType.ELSE)) {
            skipNewlines()
            elseBlock = if (check(TokenType.BEGIN)) {
                listOf(parseBlockStatement())
            } else {
                val stmt = parseStatement()
                if (stmt != null) listOf(stmt) else emptyList()
            }
        }

        consume(TokenType.END, "期望 'END' 关键字")
        return IfStatement(condition, thenBlock, elseBlock)
    }

    private fun parseBlockStatement(): BlockStatement {
        val statements = mutableListOf<AstNode>()
        while (!check(TokenType.END) && !isAtEnd()) {
            val stmt = parseStatement()
            if (stmt != null) statements.add(stmt)
        }
        consume(TokenType.END, "期望 'END' 关键字")
        return BlockStatement(statements)
    }

    private fun parseTradeInstruction(type: TradeType): TradeInstruction {
        skipNewlines()
        val condition = if (!check(TokenType.SEMICOLON) && !check(TokenType.NEWLINE) && !isAtEnd()) {
            parseExpression()
        } else null

        skipNewlines()
        if (match(TokenType.SEMICOLON)) skipNewlines()
        return TradeInstruction(type, condition)
    }

    private fun parseAssignment(): AssignmentStatement {
        val name = advance().lexeme
        advance() // consume :=
        val value = parseExpression()
        skipNewlines()
        if (match(TokenType.SEMICOLON)) skipNewlines()
        return AssignmentStatement(name, value)
    }

    private fun parseExpression(): AstNode = parseOr()

    private fun parseOr(): AstNode {
        var left = parseAnd()
        while (match(TokenType.OR)) {
            val op = previous().lexeme
            val right = parseAnd()
            left = BinaryExpression(left, op, right)
        }
        return left
    }

    private fun parseAnd(): AstNode {
        var left = parseComparison()
        while (match(TokenType.AND)) {
            val op = previous().lexeme
            val right = parseComparison()
            left = BinaryExpression(left, op, right)
        }
        return left
    }

    private fun parseComparison(): AstNode {
        var left = parseAddition()
        while (match(TokenType.GT, TokenType.LT, TokenType.GTE, TokenType.LTE,
                TokenType.EQ, TokenType.NEQ)) {
            val op = previous().lexeme
            val right = parseAddition()
            left = BinaryExpression(left, op, right)
        }
        return left
    }

    private fun parseAddition(): AstNode {
        var left = parseMultiplication()
        while (match(TokenType.PLUS, TokenType.MINUS)) {
            val op = previous().lexeme
            val right = parseMultiplication()
            left = BinaryExpression(left, op, right)
        }
        return left
    }

    private fun parseMultiplication(): AstNode {
        var left = parseUnary()
        while (match(TokenType.MUL, TokenType.DIV)) {
            val op = previous().lexeme
            val right = parseUnary()
            left = BinaryExpression(left, op, right)
        }
        return left
    }

    private fun parseUnary(): AstNode {
        if (match(TokenType.MINUS)) {
            return UnaryExpression("-", parseUnary())
        }
        if (match(TokenType.NOT)) {
            return UnaryExpression("NOT", parseUnary())
        }
        return parsePrimary()
    }

    private fun parsePrimary(): AstNode {
        // 函数调用: MA(CLOSE, 5) / CROSS(A, B)
        if (isFunctionToken(peek()) && checkNext(TokenType.LPAREN)) {
            return parseFunctionCall()
        }

        // 标识符后跟 '(' → 用户自定义函数调用
        if (check(TokenType.IDENTIFIER) && checkNext(TokenType.LPAREN)) {
            return parseFunctionCall()
        }

        // CROSS 作为二元运算符（优先级最低）
        if (match(TokenType.CROSS)) {
            return parseCrossExpression()
        }

        return when {
            match(TokenType.NUMBER) -> {
                val token = previous()
                val value = when (token.value) {
                    is Number -> token.value.toDouble()
                    is Long -> token.value.toDouble()
                    else -> token.lexeme.toDoubleOrNull() ?: 0.0
                }
                NumberLiteral(value)
            }
            match(TokenType.STRING) -> {
                StringLiteral(previous().value as? String ?: "")
            }
            match(TokenType.OPEN) -> DataRef("OPEN")
            match(TokenType.HIGH) -> DataRef("HIGH")
            match(TokenType.LOW) -> DataRef("LOW")
            match(TokenType.CLOSE) -> DataRef("CLOSE")
            match(TokenType.VOL) -> DataRef("VOL")
            match(TokenType.OPI) -> DataRef("OPI")
            match(TokenType.AMOUNT) -> DataRef("AMOUNT")
            match(TokenType.IDENTIFIER) -> VariableRef(previous().lexeme)
            match(TokenType.LPAREN) -> {
                val expr = parseExpression()
                consume(TokenType.RPAREN, "期望 ')'")
                expr
            }
            else -> {
                errors.add("行${peek().line}: 意外的 token '${peek().lexeme}'")
                advance()
                NumberLiteral(0.0)
            }
        }
    }

    private fun parseFunctionCall(): FunctionCall {
        val name = advance().lexeme
        advance() // consume (
        val args = parseArgumentList()
        consume(TokenType.RPAREN, "期望 ')'")
        return FunctionCall(name.uppercase(), args)
    }

    private fun parseArgumentList(): List<AstNode> {
        val args = mutableListOf<AstNode>()
        if (!check(TokenType.RPAREN)) {
            do {
                skipNewlines()
                args.add(parseExpression())
                skipNewlines()
            } while (match(TokenType.COMMA))
        }
        return args
    }

    /** 解析 CROSS 表达式，支持 CROSS(A, B) 格式 */
    private fun parseCrossExpression(): CrossExpression {
        if (match(TokenType.LPAREN)) {
            val left = parseExpression()
            consume(TokenType.COMMA, "期望 ','")
            val right = parseExpression()
            consume(TokenType.RPAREN, "期望 ')'")
            return CrossExpression(left, right)
        } else {
            // 备用格式（不太常见）
            val left = parsePrimary()
            if (match(TokenType.COMMA)) {
                val right = parsePrimary()
                return CrossExpression(left, right)
            }
            return CrossExpression(left, NumberLiteral(0.0))
        }
    }

    private fun isFunctionToken(token: Token): Boolean {
        return token.type in setOf(
            TokenType.MA, TokenType.EMA, TokenType.SMA, TokenType.DMA, TokenType.WMA,
            TokenType.MACD, TokenType.MACDDIFF, TokenType.MACDDEA,
            TokenType.KDJ_K, TokenType.KDJ_D, TokenType.KDJ_J,
            TokenType.RSI, TokenType.BOLL_UPPER, TokenType.BOLL_MID, TokenType.BOLL_LOWER,
            TokenType.SAR, TokenType.ATR,
            TokenType.DMI_PDI, TokenType.DMI_MDI, TokenType.DMI_ADX,
            TokenType.REF, TokenType.HHV, TokenType.LLV,
            TokenType.HHVBARS, TokenType.LLVBARS, TokenType.BARSLAST,
            TokenType.COUNT, TokenType.SUM, TokenType.EVERY,
            TokenType.STD, TokenType.VAR, TokenType.AVEDEV,
            TokenType.ABS, TokenType.MAX, TokenType.MIN, TokenType.MOD,
            TokenType.IF_COND, TokenType.VALUEWHEN, TokenType.BACKSET,
            TokenType.FILTER,
            TokenType.ENTERLONG, TokenType.EXITLONG,
            TokenType.ENTERSHORT, TokenType.EXITSHORT,
            TokenType.MARKETPOSITION,
            TokenType.BKPRICE, TokenType.SKPRICE,
            TokenType.BKHIGH, TokenType.SKLOW,
            TokenType.BARPOS, TokenType.CURRENTTIME, TokenType.CURRENTDATE,
            TokenType.NUMTOSTR, TokenType.STRFIND,
            TokenType.DRAWLINE, TokenType.DRAWICON, TokenType.DRAWTEXT,
            TokenType.STICKLINE, TokenType.VERTLINE, TokenType.PLAINTEXT,
            TokenType.DRAWSL, TokenType.DRAWNUMBER, TokenType.STICK,
            TokenType.ALERT, TokenType.PLAYSOUND,
            TokenType.AT
        )
    }

    private fun match(vararg types: TokenType): Boolean {
        for (type in types) {
            if (check(type)) { advance(); return true }
        }
        return false
    }

    private fun check(type: TokenType): Boolean = !isAtEnd() && peek().type == type
    private fun checkNext(type: TokenType): Boolean = pos + 1 < tokens.size && tokens[pos + 1].type == type
    private fun advance(): Token = tokens[pos++]
    private fun previous(): Token = tokens[pos - 1]
    private fun peek(): Token = tokens[pos]
    private fun isAtEnd(): Boolean = peek().type == TokenType.EOF
    private fun skipNewlines() { while (match(TokenType.NEWLINE)) {} }

    private fun consume(type: TokenType, message: String): Token {
        if (check(type)) return advance()
        errors.add("行${peek().line}: $message, 实际 '${peek().lexeme}'")
        // 不回退，尝试继续
        return peek()
    }

    fun getErrors(): List<String> = errors
}
