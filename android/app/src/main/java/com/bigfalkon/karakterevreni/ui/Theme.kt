package com.bigfalkon.karakterevreni.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.bigfalkon.karakterevreni.R

// Web galerisinin paleti
val Primary = Color(0xFFBDB3FF)
val OnPrimary = Color(0xFF2D235C)
val PrimaryContainer = Color(0xFF443A74)
val OnPrimaryContainer = Color(0xFFE6DEFF)
val FusionColor = Color(0xFFE0B3FF)
val DismissedColor = Color(0xFFC8C4D0)
val AuColor = Color(0xFF64DCB4)
val BackgroundDark = Color(0xFF131318)
val SurfaceDark = Color(0xFF201F25)
val SurfaceHigh = Color(0xFF2A2830)
val OnSurfaceDark = Color(0xFFE5E1E6)
val OnSurfaceVar = Color(0xFFC8C4D0)
val OutlineVar = Color(0xFF48454E)

val StarColors = mapOf(
    1 to Color(0xFFFB923C),
    2 to Color(0xFF94A3B8),
    3 to Color(0xFFFBBF24),
    4 to Color(0xFFE879F9)
)

private val DarkColors = darkColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = OnPrimaryContainer,
    secondary = FusionColor,
    onSecondary = Color(0xFF490071),
    tertiary = AuColor,
    background = BackgroundDark,
    onBackground = OnSurfaceDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceHigh,
    onSurfaceVariant = OnSurfaceVar,
    surfaceContainer = SurfaceDark,
    surfaceContainerHigh = SurfaceHigh,
    surfaceContainerHighest = SurfaceHigh,
    outline = OutlineVar,
    outlineVariant = OutlineVar
)

/** Sitedeki tipografi: gövde Manrope, fantastik başlıklar Metamorphous. */
val Manrope = FontFamily(
    Font(R.font.manrope_400, FontWeight.Normal),
    Font(R.font.manrope_700, FontWeight.Bold),
    Font(R.font.manrope_800, FontWeight.ExtraBold)
)
val Fantastical = FontFamily(Font(R.font.metamorphous_regular, FontWeight.Normal))

private val AppTypography = Typography().run {
    copy(
        displaySmall = displaySmall.copy(fontFamily = Fantastical),
        headlineSmall = headlineSmall.copy(fontFamily = Manrope, fontWeight = FontWeight.ExtraBold),
        headlineMedium = headlineMedium.copy(fontFamily = Manrope, fontWeight = FontWeight.ExtraBold),
        titleLarge = titleLarge.copy(fontFamily = Manrope, fontWeight = FontWeight.Bold),
        titleMedium = titleMedium.copy(fontFamily = Manrope, fontWeight = FontWeight.Bold),
        titleSmall = titleSmall.copy(fontFamily = Manrope, fontWeight = FontWeight.Bold),
        bodyLarge = bodyLarge.copy(fontFamily = Manrope),
        bodyMedium = bodyMedium.copy(fontFamily = Manrope),
        bodySmall = TextStyle(fontFamily = Manrope, fontSize = 12.sp),
        labelLarge = labelLarge.copy(fontFamily = Manrope, fontWeight = FontWeight.Bold),
        labelMedium = labelMedium.copy(fontFamily = Manrope),
        labelSmall = labelSmall.copy(fontFamily = Manrope)
    )
}

/**
 * Uygulama her zaman koyu temada. Bir alternatif evren seçiliyken sitedeki
 * `au-theme-active` davranışının karşılığı olarak vurgu rengi o evrenin rengine döner.
 */
@Composable
fun KarakterEvreniTheme(accent: Color? = null, content: @Composable () -> Unit) {
    val scheme = if (accent == null) DarkColors else DarkColors.copy(
        primary = accent,
        onPrimary = Color.Black,
        primaryContainer = accent.copy(alpha = 0.28f).compositeOverBackground(),
        onPrimaryContainer = accent,
        tertiary = accent
    )
    MaterialTheme(colorScheme = scheme, typography = AppTypography, content = content)
}

private fun Color.compositeOverBackground(): Color {
    val a = alpha
    return Color(
        red = red * a + BackgroundDark.red * (1 - a),
        green = green * a + BackgroundDark.green * (1 - a),
        blue = blue * a + BackgroundDark.blue * (1 - a),
        alpha = 1f
    )
}

/** "#64dcb4" gibi hex renkleri Compose rengine çevirir. */
fun parseHexColor(hex: String?, fallback: Color = AuColor): Color {
    val clean = hex?.trim()?.removePrefix("#") ?: return fallback
    return runCatching {
        when (clean.length) {
            6 -> Color(("ff$clean").toLong(16))
            8 -> Color(clean.toLong(16))
            else -> fallback
        }
    }.getOrDefault(fallback)
}
