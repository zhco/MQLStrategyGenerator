package com.marvis.mql.parser

/**
 * 麦语言词法分析器 —— 将源代码分解为 Token 流
 * 支持完整的麦语言语法：数据引用、运算符、关键字、函数、字面量
 */
class MqlLexer(private val source: String) {

    private var pos = 0
    private var line = 1
    private var column = 1
    private val tokens = mutableListOf<Token>()
    private val errors = mutableListOf<String>()

    companion object {
        private val KEYWORDS = mapOf(
            "IF" to TokenType.IF, "THEN" to TokenType.THEN,
            "ELSE" to TokenType.ELSE, "BEGIN" to TokenType.BEGIN,
            "END" to TokenType.END,
            "BUY" to TokenType.BUY, "SELL" to TokenType.SELL,
            "SHORT" to TokenType.SHORT, "COVER" to TokenType.COVER,
            "BUYSHORT" to TokenType.BUYSHORT, "SELLSHORT" to TokenType.SELLSHORT,
            "AND" to TokenType.AND, "OR" to TokenType.OR, "NOT" to TokenType.NOT,
            "CROSS" to TokenType.CROSS
        )

        private val FUNCTION_MAP = mapOf(
            "O" to TokenType.OPEN, "OPEN" to TokenType.OPEN,
            "H" to TokenType.HIGH, "HIGH" to TokenType.HIGH,
            "L" to TokenType.LOW, "LOW" to TokenType.LOW,
            "C" to TokenType.CLOSE, "CLOSE" to TokenType.CLOSE,
            "V" to TokenType.VOL, "VOL" to TokenType.VOL,
            "OPI" to TokenType.OPI, "AMOUNT" to TokenType.AMOUNT,
            "REF" to TokenType.REF, "HHV" to TokenType.HHV,
            "LLV" to TokenType.LLV, "HHVBARS" to TokenType.HHVBARS,
            "LLVBARS" to TokenType.LLVBARS, "BARSLAST" to TokenType.BARSLAST,
            "COUNT" to TokenType.COUNT, "SUM" to TokenType.SUM,
            "EVERY" to TokenType.EVERY, "FILTER" to TokenType.FILTER,
            "MA" to TokenType.MA, "EMA" to TokenType.EMA,
            "SMA" to TokenType.SMA, "DMA" to TokenType.DMA,
            "WMA" to TokenType.WMA,
            "MACD" to TokenType.MACD, "DIFF" to TokenType.MACDDIFF,
            "DEA" to TokenType.MACDDEA,
            "KDJ_K" to TokenType.KDJ_K, "KDJ_D" to TokenType.KDJ_D,
            "KDJ_J" to TokenType.KDJ_J,
            "RSI" to TokenType.RSI,
            "BOLL" to TokenType.BOLL_MID, "UB" to TokenType.BOLL_UPPER,
            "LB" to TokenType.BOLL_LOWER,
            "SAR" to TokenType.SAR, "ATR" to TokenType.ATR,
            "PDI" to TokenType.DMI_PDI, "MDI" to TokenType.DMI_MDI,
            "ADX" to TokenType.DMI_ADX,
            "STD" to TokenType.STD, "VAR" to TokenType.VAR,
            "AVEDEV" to TokenType.AVEDEV,
            "ABS" to TokenType.ABS, "MAX" to TokenType.MAX,
            "MIN" to TokenType.MIN, "MOD" to TokenType.MOD,
            "IF" to TokenType.IF_COND, "VALUEWHEN" to TokenType.VALUEWHEN,
            "BACKSET" to TokenType.BACKSET,
            "ENTERLONG" to TokenType.ENTERLONG, "EXITLONG" to TokenType.EXITLONG,
            "ENTERSHORT" to TokenType.ENTERSHORT, "EXITSHORT" to TokenType.EXITSHORT,
            "MARKETPOSITION" to TokenType.MARKETPOSITION,
            "BKPRICE" to TokenType.BKPRICE, "SKPRICE" to TokenType.SKPRICE,
            "BKHIGH" to TokenType.BKHIGH, "SKLOW" to TokenType.SKLOW,
            "BARPOS" to TokenType.BARPOS, "CURRENTTIME" to TokenType.CURRENTTIME,
            "CURRENTDATE" to TokenType.CURRENTDATE,
            "NUMTOSTR" to TokenType.NUMTOSTR, "STRFIND" to TokenType.STRFIND
        )
    }

    fun tokenize(): List<Token> {
        while (pos < source.length) {
            val ch = peek()
            when {
                ch == '\n' -> { addToken(TokenType.NEWLINE, "\n"); advance(); line++; column = 1 }
                ch.isWhitespace() -> advance()
                ch == '/' && peek(1) == '/' -> skipLineComment()
                ch == '/' && peek(1) == '*' -> skipBlockComment()
                ch == '{' -> skipBraceComment()
                ch.isDigit() || (ch == '.' && peek(1)?.isDigit() == true) -> scanNumber()
                ch.isLetter() || ch == '_' -> scanIdentifier()
                ch == '"' || ch == '\'' -> scanString(ch)
                ch == ':' && peek(1) == '=' -> { addToken(TokenType.COLON_ASSIGN, ":="); advance(2) }
                ch == ':' -> { addToken(TokenType.COLON, ":"); advance() }
                ch == '+' -> { addToken(TokenType.PLUS, "+"); advance() }
                ch == '-' -> { addToken(TokenType.MINUS, "-"); advance() }
                ch == '*' -> { addToken(TokenType.MUL, "*"); advance() }
                ch == '/' -> { addToken(TokenType.DIV, "/"); advance() }
                ch == '>' && peek(1) == '=' -> { addToken(TokenType.GTE, ">="); advance(2) }
                ch == '>' -> { addToken(TokenType.GT, ">"); advance() }
                ch == '<' && peek(1) == '=' -> { addToken(TokenType.LTE, "<="); advance(2) }
                ch == '<' -> { addToken(TokenType.LT, "<"); advance() }
                ch == '=' && peek(1) == '=' -> { addToken(TokenType.EQ, "=="); advance(2) }
                ch == '!' && peek(1) == '=' -> { addToken(TokenType.NEQ, "!="); advance(2) }
                ch == '&' && peek(1) == '&' -> { addToken(TokenType.AND, "&&"); advance(2) }
                ch == '|' && peek(1) == '|' -> { addToken(TokenType.OR, "||"); advance(2) }
                ch == '(' -> { addToken(TokenType.LPAREN, "("); advance() }
                ch == ')' -> { addToken(TokenType.RPAREN, ")"); advance() }
                ch == '[' -> { addToken(TokenType.LBRACKET, "["); advance() }
                ch == ']' -> { addToken(TokenType.RBRACKET, "]"); advance() }
                ch == ',' -> { addToken(TokenType.COMMA, ","); advance() }
                ch == ';' -> { addToken(TokenType.SEMICOLON, ";"); advance() }
                ch == '=' -> { addToken(TokenType.ASSIGN, "="); advance() }
                ch == '#' -> skipPreprocessorDirective()
                else -> {
                    errors.add("行$line 列$column: 未识别的字符 '$ch'")
                    advance()
                }
            }
        }
        addToken(TokenType.EOF, "")
        return tokens
    }

    private fun peek(offset: Int = 0): Char? =
        if (pos + offset < source.length) source[pos + offset] else null

    private fun advance(n: Int = 1) { pos += n; column += n }

    private fun addToken(type: TokenType, lexeme: String, value: Any? = null) {
        tokens.add(Token(type, lexeme, line, column, value))
    }

    private fun skipLineComment() { while (pos < source.length && peek() != '\n') advance() }

    private fun skipBlockComment() {
        advance(2)
        while (pos < source.length && !(peek() == '*' && peek(1) == '/')) {
            if (peek() == '\n') { line++; column = 1 }
            advance()
        }
        if (pos < source.length) advance(2)
    }

    private fun skipBraceComment() {
        while (pos < source.length && peek() != '}') {
            if (peek() == '\n') { line++; column = 1 }
            advance()
        }
        if (pos < source.length) advance()
    }

    private fun skipPreprocessorDirective() {
        while (pos < source.length && peek() != '\n') advance()
    }

    private fun scanNumber() {
        val start = pos
        var isFloat = false
        while (pos < source.length && (peek()?.isDigit() == true || peek() == '.')) {
            if (peek() == '.') {
                if (isFloat || peek(1)?.isDigit() != true) break
                isFloat = true
            }
            advance()
        }
        val numStr = source.substring(start, pos)
        val value = if (isFloat) numStr.toDouble() else numStr.toLong()
        addToken(TokenType.NUMBER, numStr, value)
    }

    private fun scanIdentifier() {
        val start = pos
        while (pos < source.length && (peek()?.isLetterOrDigit() == true || peek() == '_')) advance()
        val text = source.substring(start, pos)
        val upper = text.uppercase()

        val type = when {
            FUNCTION_MAP.containsKey(upper) -> FUNCTION_MAP[upper]!!
            KEYWORDS.containsKey(upper) -> KEYWORDS[upper]!!
            else -> TokenType.IDENTIFIER
        }
        addToken(type, text)
    }

    private fun scanString(quote: Char) {
        advance()
        val start = pos
        while (pos < source.length && peek() != quote) {
            if (peek() == '\n') { line++; column = 1 }
            advance()
        }
        val str = source.substring(start, pos)
        if (pos < source.length) advance()
        addToken(TokenType.STRING, "$quote$str$quote", str)
    }

    fun getErrors(): List<String> = errors
}
