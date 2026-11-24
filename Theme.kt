package com.shafii.encyclopedia.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp

// Scholarly Islamic Palette: Deep Emerald, Antique Gold, Parchment Ivory, Obsidian Night
val EmeraldPrimary = Color(0xFF0F4C3A)
val EmeraldContainer = Color(0xFFD7EFE6)
val GoldAccent = Color(0xFFB5892E)
val GoldContainer = Color(0xFFF9EED2)
val ParchmentBg = Color(0xFFFAF7F0)
val ParchmentSurface = Color(0xFFFFFFFF)
val InkOnLight = Color(0xFF1A211E)
val MutedOnLight = Color(0xFF53615B)

val NightBg = Color(0xFF101614)
val NightSurface = Color(0xFF18221F)
val EmeraldNight = Color(0xFF6CD3B0)
val GoldNight = Color(0xFFE5C168)
val InkOnDark = Color(0xFFECEFE9)

val MutamadGreen = Color(0xFF156B49)
val QadimAmber = Color(0xFF9C6615)
val JadidBlue = Color(0xFF1B5A7A)
val ContemporaryPurple = Color(0xFF6B3FA0)
val DisputedRed = Color(0xFFA63A3A)

data class ReaderTypographyConfig(
    val fontScale: Float = 1.0f,
    val lineSpacingMultiplier: Float = 1.55f
)

val LocalReaderConfig = staticCompositionLocalOf { ReaderTypographyConfig() }

private val LightColorScheme = lightColorScheme(
    primary = EmeraldPrimary,
    onPrimary = Color.White,
    primaryContainer = EmeraldContainer,
    onPrimaryContainer = Color(0xFF082B20),
    secondary = GoldAccent,
    onSecondary = Color.White,
    secondaryContainer = GoldContainer,
    onSecondaryContainer = Color(0xFF3A2805),
    background = ParchmentBg,
    onBackground = InkOnLight,
    surface = ParchmentSurface,
    onSurface = InkOnLight,
    surfaceVariant = Color(0xFFF0EBE0),
    onSurfaceVariant = MutedOnLight
)

private val DarkColorScheme = darkColorScheme(
    primary = EmeraldNight,
    onPrimary = Color(0xFF04261B),
    primaryContainer = Color(0xFF123C2F),
    onPrimaryContainer = Color(0xFFCFF3E4),
    secondary = GoldNight,
    onSecondary = Color(0xFF2E2002),
    secondaryContainer = Color(0xFF3E3114),
    onSecondaryContainer = Color(0xFFF9EED2),
    background = NightBg,
    onBackground = InkOnDark,
    surface = NightSurface,
    onSurface = InkOnDark,
    surfaceVariant = Color(0xFF222E2A),
    onSurfaceVariant = Color(0xFFB1C0B9)
)

@Composable
fun ShafiiEncyclopediaTheme(
    darkTheme: Boolean,
    readerConfig: ReaderTypographyConfig,
    content: @Composable () -> Unit
) {
    val scale = readerConfig.fontScale.coerceIn(0.85f, 1.55f)
    val lineMult = readerConfig.lineSpacingMultiplier.coerceIn(1.25f, 2.0f)

    val typography = Typography(
        headlineMedium = TextStyle(
            fontSize = (22f * scale).sp,
            lineHeight = (22f * scale * 1.35f).sp,
            fontWeight = FontWeight.Bold
        ),
        titleLarge = TextStyle(
            fontSize = (19f * scale).sp,
            lineHeight = (19f * scale * 1.35f).sp,
            fontWeight = FontWeight.Bold
        ),
        titleMedium = TextStyle(
            fontSize = (16f * scale).sp,
            lineHeight = (16f * scale * 1.4f).sp,
            fontWeight = FontWeight.SemiBold
        ),
        bodyLarge = TextStyle(
            fontSize = (15.5f * scale).sp,
            lineHeight = (15.5f * scale * lineMult).sp
        ),
        bodyMedium = TextStyle(
            fontSize = (14f * scale).sp,
            lineHeight = (14f * scale * lineMult).sp
        ),
        labelMedium = TextStyle(
            fontSize = (12f * scale).sp,
            fontWeight = FontWeight.Medium
        )
    )

    // Always enforce Arabic-first RTL layout direction across the entire app
    CompositionLocalProvider(
        LocalLayoutDirection provides LayoutDirection.Rtl,
        LocalReaderConfig provides readerConfig
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
            typography = typography,
            content = content
        )
    }
}
