
package com.marvis.mql.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// ===== 亮色主题 =====
val Primary = Color(0xFF1A73E8)
val PrimaryDark = Color(0xFF1557B0)
val Secondary = Color(0xFF34A853)
val Error = Color(0xFFEA4335)
val Warning = Color(0xFFFBBC04)
val Background = Color(0xFFF8F9FA)
val Surface = Color(0xFFFFFFFF)
val OnPrimary = Color(0xFFFFFFFF)
val OnBackground = Color(0xFF202124)
val OnSurface = Color(0xFF3C4043)
val CardBorder = Color(0xFFDADCE0)
val DividerColor = Color(0xFFE8EAED)

// ===== 暗色主题 =====
val DarkBackground = Color(0xFF121212)
val DarkSurface = Color(0xFF1E1E1E)
val DarkSurfaceVariant = Color(0xFF2D2D2D)
val DarkOnBackground = Color(0xFFE0E0E0)
val DarkOnSurface = Color(0xFFB0B0B0)
val DarkCardBorder = Color(0xFF333333)
val DarkDivider = Color(0xFF2A2A2A)
val DarkPrimary = Color(0xFF8AB4F8)
val DarkSecondary = Color(0xFF81C995)

// ===== 交易颜色（亮/暗通用）=====
val BuyColor = Color(0xFFEF5350)
val SellColor = Color(0xFF26A69A)
val CodeBg = Color(0xFF1E1E1E)
val CodeBgLight = Color(0xFFF5F5F5)
val CodeText = Color(0xFFD4D4D4)
val CodeTextLight = Color(0xFF333333)
val KeywordColor = Color(0xFF569CD6)
val FunctionColor = Color(0xFFDCDCAA)
val NumberColor = Color(0xFFB5CEA8)
val CommentColor = Color(0xFF6A9955)
val StringColor = Color(0xFFCE9178)

fun appLightColorScheme() = lightColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    secondary = Secondary,
    error = Error,
    background = Background,
    surface = Surface,
    onBackground = OnBackground,
    onSurface = OnSurface
)

fun appDarkColorScheme() = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = Color(0xFF003D73),
    secondary = DarkSecondary,
    error = Color(0xFFCF6679),
    background = DarkBackground,
    surface = DarkSurface,
    onBackground = DarkOnBackground,
    onSurface = DarkOnSurface
)
