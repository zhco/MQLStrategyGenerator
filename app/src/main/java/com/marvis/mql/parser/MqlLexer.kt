
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

        // 函数名列表（不含 IF，IF 由上下文决定是关键字还是函数）
        private val FUNCTION_NAMES = setOf(
            "O", "OPEN", "H", "HIGH", "L", "LOW", "C", "CLOSE", "V", "VOL",
            "OPI", "AMOUNT",
            "REF", "HHV", "LLV", "HHVBARS", "LLVBARS", "BARSLAST",
            "COUNT", "SUM", "EVERY", "FILTER",
            "MA", "EMA", "SMA", "DMA", "WMA",
            "MACD", "DIFF", "DEA",
            "KDJ_K", "KDJ_D", "KDJ_J",
            "RSI",
            "BOLL", "UB", "LB",
            "SAR", "ATR",
            "PDI", "MDI", "ADX",
            "STD", "VAR", "AVEDEV",
            "ABS", "MAX", "MIN", "MOD",
            "VALUEWHEN", "BACKSET",
            "ENTERLONG", "EXITLONG", "ENTERSHORT", "EXITSHORT",
            "MARKETPOSITION",
            "BKPRICE", "SKPRICE", "BKHIGH", "SKLOW",
            "BARPOS", "CURRENTTIME", "CURRENTDATE",
            "NUMTOSTR", "STRFIND",
            "DRAWLINE", "DRAWICON", "DRAWTEXT",
            "STICKLINE", "VERTLINE", "PLAINTEXT",
            "DRAWSL", "DRAWNUMBER", "STICK",
            "ALERT", "PLAYSOUND"
        )
    }

    fun tokenize(): List<Token> {
        while (pos < source.length) {
            val ch = peek()
            when {
                ch == '\n' -> { addToken(TokenType.NEWLINE, "\n"); advance(); line++; column = 1 }
                ch?.isWhitespace() == true -> advance()
                ch == '/' && peek(1) == '/' -> skipLineComment()
                ch == '/' && peek(1) == '*' -> skipBlockComment()
                ch == '{' -> skipBraceComment()
                ch?.isDigit() == true || (ch == '.' && peek(1)?.isDigit() == true) -> scanNumber()
                ch?.isLetter() == true || ch == '_' -> scanIdentifier()
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
                ch == '<' && peek(1) == '>' -> { addToken(TokenType.NEQ, "<>"); advance(2) }
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
        val value = try {
            if (isFloat) numStr.toDouble() else numStr.toLong()
        } catch (e: NumberFormatException) {
            0L
        }
        addToken(TokenType.NUMBER, numStr, value)
    }

    private fun scanIdentifier() {
        val start = pos
        while (pos < source.length && (peek()?.isLetterOrDigit() == true || peek() == '_')) advance()
        val text = source.substring(start, pos)
        val upper = text.uppercase()

        val type = when {
            // IF 特殊处理：如果是 IF 关键字，看后面是否跟 ( 来决定是函数还是关键字
            upper == "IF" -> {
                val savedPos = pos
                // 跳过空白，看下一个非空白字符是不是 (
                var lookPos = pos
                while (lookPos < source.length && source[lookPos].isWhitespace()) lookPos++
                if (lookPos < source.length && source[lookPos] == '(') TokenType.IF_COND
                else TokenType.IF
            }
            // CROSS 不是函数名，是关键字
            upper == "CROSS" -> TokenType.CROSS
            // 其他函数名
            upper in FUNCTION_NAMES -> {
                val lookPos = pos
                var lp = pos
                while (lp < source.length && source[lp].isWhitespace()) lp++
                // 数据引用（O/H/L/C/V 等）不需要括号也可以作为函数
                when (upper) {
                    "O", "OPEN" -> TokenType.OPEN
                    "H", "HIGH" -> TokenType.HIGH
                    "L", "LOW" -> TokenType.LOW
                    "C", "CLOSE" -> TokenType.CLOSE
                    "V", "VOL" -> TokenType.VOL
                    "OPI" -> TokenType.OPI
                    "AMOUNT" -> TokenType.AMOUNT
                    "MA" -> TokenType.MA
                    "EMA" -> TokenType.EMA
                    "SMA" -> TokenType.SMA
                    "DMA" -> TokenType.DMA
                    "WMA" -> TokenType.WMA
                    "MACD" -> TokenType.MACD
                    "DIFF" -> TokenType.MACDDIFF
                    "DEA" -> TokenType.MACDDEA
                    "KDJ_K" -> TokenType.KDJ_K
                    "KDJ_D" -> TokenType.KDJ_D
                    "KDJ_J" -> TokenType.KDJ_J
                    "RSI" -> TokenType.RSI
                    "BOLL" -> TokenType.BOLL_MID
                    "UB" -> TokenType.BOLL_UPPER
                    "LB" -> TokenType.BOLL_LOWER
                    "SAR" -> TokenType.SAR
                    "ATR" -> TokenType.ATR
                    "PDI" -> TokenType.DMI_PDI
                    "MDI" -> TokenType.DMI_MDI
                    "ADX" -> TokenType.DMI_ADX
                    "REF" -> TokenType.REF
                    "HHV" -> TokenType.HHV
                    "LLV" -> TokenType.LLV
                    "HHVBARS" -> TokenType.HHVBARS
                    "LLVBARS" -> TokenType.LLVBARS
                    "BARSLAST" -> TokenType.BARSLAST
                    "COUNT" -> TokenType.COUNT
                    "SUM" -> TokenType.SUM
                    "EVERY" -> TokenType.EVERY
                    "FILTER" -> TokenType.FILTER
                    "STD" -> TokenType.STD
                    "VAR" -> TokenType.VAR
                    "AVEDEV" -> TokenType.AVEDEV
                    "ABS" -> TokenType.ABS
                    "MAX" -> TokenType.MAX
                    "MIN" -> TokenType.MIN
                    "MOD" -> TokenType.MOD
                    "VALUEWHEN" -> TokenType.VALUEWHEN
                    "BACKSET" -> TokenType.BACKSET
                    "ENTERLONG" -> TokenType.ENTERLONG
                    "EXITLONG" -> TokenType.EXITLONG
                    "ENTERSHORT" -> TokenType.ENTERSHORT
                    "EXITSHORT" -> TokenType.EXITSHORT
                    "MARKETPOSITION" -> TokenType.MARKETPOSITION
                    "BKPRICE" -> TokenType.BKPRICE
                    "SKPRICE" -> TokenType.SKPRICE
                    "BKHIGH" -> TokenType.BKHIGH
                    "SKLOW" -> TokenType.SKLOW
                    "BARPOS" -> TokenType.BARPOS
                    "CURRENTTIME" -> TokenType.CURRENTTIME
                    "CURRENTDATE" -> TokenType.CURRENTDATE
                    "NUMTOSTR" -> TokenType.NUMTOSTR
                    "STRFIND" -> TokenType.STRFIND
                    "DRAWLINE" -> TokenType.DRAWLINE
                    "DRAWICON" -> TokenType.DRAWICON
                    "DRAWTEXT" -> TokenType.DRAWTEXT
                    "STICKLINE" -> TokenType.STICKLINE
                    "VERTLINE" -> TokenType.VERTLINE
                    "PLAINTEXT" -> TokenType.PLAINTEXT
                    "DRAWSL" -> TokenType.DRAWSL
                    "DRAWNUMBER" -> TokenType.DRAWNUMBER
                    "STICK" -> TokenType.STICK
                    "ALERT" -> TokenType.ALERT
                    "PLAYSOUND" -> TokenType.PLAYSOUND
                    else -> TokenType.IDENTIFIER
                }
            }
            upper in KEYWORDS -> KEYWORDS[upper]!!
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
