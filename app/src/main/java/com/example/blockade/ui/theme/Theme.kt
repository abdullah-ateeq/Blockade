package com.example.blockade.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.blockade.data.BoardTheme
import com.example.blockade.game.Player

// Player identities: muted, mature hues shared by both app themes.
@Immutable
data class PlayerColors(val base: Color, val light: Color, val dark: Color)

val Player1Colors = PlayerColors(
    base = Color(0xFFB0553D),
    light = Color(0xFFDC8F6E),
    dark = Color(0xFF5E2517)
)
val Player2Colors = PlayerColors(
    base = Color(0xFF2E7C86),
    light = Color(0xFF6FB5BC),
    dark = Color(0xFF123E45)
)

fun Player.colors(): PlayerColors = if (this == Player.PLAYER_ONE) Player1Colors else Player2Colors

/** Text glyph mirroring each token's shape, so identity never depends on colour alone. */
val Player.glyph: String
    get() = if (this == Player.PLAYER_ONE) "◆" else "●"

val Bronze = Color(0xFFB08D57)
val BronzeLight = Color(0xFFDDBD86)
val BronzeDark = Color(0xFF5E4726)
val Warning = Color(0xFFE3A33B)

@Immutable
data class BoardPalette(
    val frame: Color,
    val frameEdge: Color,
    val groove: Color,
    val cell: Color,
    val cellHighlight: Color,
    val dot: Color
)

val ClassicBoard = BoardPalette(
    frame = Color(0xFF2B2E33),
    frameEdge = Color(0xFF3B3F45),
    groove = Color(0xFF17191C),
    cell = Color(0xFF363A40),
    cellHighlight = Color(0xFF41464D),
    dot = Color(0xFF8C7650)
)

val MidnightBoard = BoardPalette(
    frame = Color(0xFF151C2B),
    frameEdge = Color(0xFF243049),
    groove = Color(0xFF0A0E17),
    cell = Color(0xFF1E283B),
    cellHighlight = Color(0xFF28344B),
    dot = Color(0xFF8A7A5C)
)

fun BoardTheme.palette(): BoardPalette = when (this) {
    BoardTheme.CLASSIC -> ClassicBoard
    BoardTheme.MIDNIGHT -> MidnightBoard
}

val LocalBoardPalette = staticCompositionLocalOf { ClassicBoard }

private val DarkColors = darkColorScheme(
    primary = Bronze,
    onPrimary = Color(0xFF1A140A),
    primaryContainer = Color(0xFF3A2F1E),
    onPrimaryContainer = BronzeLight,
    secondary = Color(0xFF8E9AA8),
    onSecondary = Color(0xFF12161B),
    background = Color(0xFF0E1116),
    onBackground = Color(0xFFE7E2D8),
    surface = Color(0xFF151A21),
    onSurface = Color(0xFFE7E2D8),
    surfaceVariant = Color(0xFF1D232C),
    onSurfaceVariant = Color(0xFF9CA3AD),
    surfaceContainerHigh = Color(0xFF1D232C),
    outline = Color(0xFF3A424D),
    outlineVariant = Color(0xFF272E37),
    error = Warning
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF8A6A3A),
    onPrimary = Color(0xFFFFFBF3),
    primaryContainer = Color(0xFFEADBC0),
    onPrimaryContainer = Color(0xFF3A2A12),
    secondary = Color(0xFF5B6673),
    onSecondary = Color.White,
    background = Color(0xFFECE8E0),
    onBackground = Color(0xFF1B1F24),
    surface = Color(0xFFF6F3ED),
    onSurface = Color(0xFF1B1F24),
    surfaceVariant = Color(0xFFE2DDD3),
    onSurfaceVariant = Color(0xFF5A6068),
    surfaceContainerHigh = Color(0xFFF6F3ED),
    outline = Color(0xFFC3BBAD),
    outlineVariant = Color(0xFFD8D2C6),
    error = Color(0xFFA8641A)
)

private val Display = FontFamily.Serif
private val Label = FontFamily.SansSerif

private val BlockadeTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = Display, fontWeight = FontWeight.Normal,
        fontSize = 48.sp, lineHeight = 54.sp, letterSpacing = 10.sp
    ),
    displaySmall = TextStyle(
        fontFamily = Display, fontWeight = FontWeight.Normal,
        fontSize = 32.sp, lineHeight = 40.sp, letterSpacing = 4.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = Display, fontWeight = FontWeight.Normal,
        fontSize = 22.sp, lineHeight = 28.sp, letterSpacing = 3.sp
    ),
    titleMedium = TextStyle(
        fontFamily = Label, fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 2.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = Label, fontWeight = FontWeight.Normal,
        fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.2.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = Label, fontWeight = FontWeight.Normal,
        fontSize = 14.sp, lineHeight = 21.sp, letterSpacing = 0.2.sp
    ),
    labelLarge = TextStyle(
        fontFamily = Label, fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp, lineHeight = 18.sp, letterSpacing = 2.4.sp
    ),
    labelMedium = TextStyle(
        fontFamily = Label, fontWeight = FontWeight.Medium,
        fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 1.8.sp
    ),
    labelSmall = TextStyle(
        fontFamily = Label, fontWeight = FontWeight.Medium,
        fontSize = 10.sp, lineHeight = 14.sp, letterSpacing = 1.6.sp
    )
)

@Composable
fun BlockadeTheme(
    darkTheme: Boolean,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = BlockadeTypography,
        content = content
    )
}
