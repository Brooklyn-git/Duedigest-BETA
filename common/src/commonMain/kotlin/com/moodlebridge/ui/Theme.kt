package com.moodlebridge.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val BrandPrimary = Color(0xFF4F46E5)
val BrandPrimaryLight = Color(0xFF6366F1)
val BrandSecondary = Color(0xFF14B8A6)
val BrandTertiary = Color(0xFFF59E0B)
val BrandError = Color(0xFFEF4444)
val BrandSuccess = Color(0xFF22C55E)

private val LightColorScheme = lightColorScheme(
    primary = BrandPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0E7FF),
    onPrimaryContainer = Color(0xFF312E81),
    secondary = BrandSecondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCCFBF1),
    onSecondaryContainer = Color(0xFF134E4A),
    tertiary = BrandTertiary,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFEF3C7),
    onTertiaryContainer = Color(0xFF92400E),
    error = BrandError,
    onError = Color.White,
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF991B1B),
    surface = Color(0xFFFAFAFA),
    onSurface = Color(0xFF1E293B),
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF64748B),
    outline = Color(0xFFCBD5E1),
    outlineVariant = Color(0xFFE2E8F0),
    background = Color(0xFFF8FAFC),
    onBackground = Color(0xFF0F172A),
)

private val AmoledDarkColorScheme = darkColorScheme(
    primary = Color(0xFF818CF8),
    onPrimary = Color(0xFF1E1B4B),
    primaryContainer = Color(0xFF3730A3),
    onPrimaryContainer = Color(0xFFC7D2FE),
    secondary = Color(0xFF2DD4BF),
    onSecondary = Color(0xFF134E4A),
    secondaryContainer = Color(0xFF115E59),
    onSecondaryContainer = Color(0xFF99F6E4),
    tertiary = Color(0xFFFBBF24),
    onTertiary = Color(0xFF78350F),
    tertiaryContainer = Color(0xFF92400E),
    onTertiaryContainer = Color(0xFFFDE68A),
    error = Color(0xFFF87171),
    onError = Color(0xFF450A0A),
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = Color(0xFFFECACA),
    surface = Color.Black,
    onSurface = Color(0xFFE2E8F0),
    surfaceVariant = Color(0xFF111111),
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = Color(0xFF1E1E1E),
    outlineVariant = Color(0xFF2A2A2A),
    background = Color.Black,
    onBackground = Color(0xFFF1F5F9),
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF818CF8),
    onPrimary = Color(0xFF1E1B4B),
    primaryContainer = Color(0xFF3730A3),
    onPrimaryContainer = Color(0xFFC7D2FE),
    secondary = Color(0xFF2DD4BF),
    onSecondary = Color(0xFF134E4A),
    secondaryContainer = Color(0xFF115E59),
    onSecondaryContainer = Color(0xFF99F6E4),
    tertiary = Color(0xFFFBBF24),
    onTertiary = Color(0xFF78350F),
    tertiaryContainer = Color(0xFF92400E),
    onTertiaryContainer = Color(0xFFFDE68A),
    error = Color(0xFFF87171),
    onError = Color(0xFF450A0A),
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = Color(0xFFFECACA),
    surface = Color(0xFF0F0F12),
    onSurface = Color(0xFFE2E8F0),
    surfaceVariant = Color(0xFF1E1E24),
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = Color(0xFF334155),
    outlineVariant = Color(0xFF1E293B),
    background = Color(0xFF0A0A0D),
    onBackground = Color(0xFFF1F5F9),
)

private val SolarizedLightColorScheme = lightColorScheme(
    primary = Color(0xFF268BD2),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD4E6F1),
    onPrimaryContainer = Color(0xFF1A5276),
    secondary = Color(0xFF2AA198),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD1F2EB),
    onSecondaryContainer = Color(0xFF0E6655),
    tertiary = Color(0xFFCB4B16),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFDEBD0),
    onTertiaryContainer = Color(0xFF935116),
    error = Color(0xFFDC322F),
    onError = Color.White,
    errorContainer = Color(0xFFFADBD8),
    onErrorContainer = Color(0xFF922B21),
    surface = Color(0xFFFDF6E3),
    onSurface = Color(0xFF586E75),
    surfaceVariant = Color(0xFFEEE8D5),
    onSurfaceVariant = Color(0xFF93A1A1),
    outline = Color(0xFFD3CFC4),
    outlineVariant = Color(0xFFE8E2D0),
    background = Color(0xFFFDF6E3),
    onBackground = Color(0xFF586E75),
)

private val SolarizedDarkColorScheme = darkColorScheme(
    primary = Color(0xFF268BD2),
    onPrimary = Color(0xFF002B36),
    primaryContainer = Color(0xFF073642),
    onPrimaryContainer = Color(0xFF839496),
    secondary = Color(0xFF2AA198),
    onSecondary = Color(0xFF002B36),
    secondaryContainer = Color(0xFF073642),
    onSecondaryContainer = Color(0xFF93A1A1),
    tertiary = Color(0xFFCB4B16),
    onTertiary = Color(0xFF002B36),
    tertiaryContainer = Color(0xFF586E75),
    onTertiaryContainer = Color(0xFFFDF6E3),
    error = Color(0xFFDC322F),
    onError = Color(0xFF002B36),
    errorContainer = Color(0xFF586E75),
    onErrorContainer = Color(0xFFFDF6E3),
    surface = Color(0xFF002B36),
    onSurface = Color(0xFF839496),
    surfaceVariant = Color(0xFF073642),
    onSurfaceVariant = Color(0xFF93A1A1),
    outline = Color(0xFF586E75),
    outlineVariant = Color(0xFF073642),
    background = Color(0xFF002B36),
    onBackground = Color(0xFF839496),
)

private val NordColorScheme = darkColorScheme(
    primary = Color(0xFF88C0D0),
    onPrimary = Color(0xFF2E3440),
    primaryContainer = Color(0xFF3B4252),
    onPrimaryContainer = Color(0xFFD8DEE9),
    secondary = Color(0xFFA3BE8C),
    onSecondary = Color(0xFF2E3440),
    secondaryContainer = Color(0xFF3B4252),
    onSecondaryContainer = Color(0xFFECEFF4),
    tertiary = Color(0xFFEBCB8B),
    onTertiary = Color(0xFF2E3440),
    tertiaryContainer = Color(0xFF434C5E),
    onTertiaryContainer = Color(0xFFECEFF4),
    error = Color(0xFFBF616A),
    onError = Color(0xFF2E3440),
    errorContainer = Color(0xFF434C5E),
    onErrorContainer = Color(0xFFECEFF4),
    surface = Color(0xFF2E3440),
    onSurface = Color(0xFFD8DEE9),
    surfaceVariant = Color(0xFF3B4252),
    onSurfaceVariant = Color(0xFFA5ADCB),
    outline = Color(0xFF4C566A),
    outlineVariant = Color(0xFF3B4252),
    background = Color(0xFF2E3440),
    onBackground = Color(0xFFECEFF4),
)

private val DraculaColorScheme = darkColorScheme(
    primary = Color(0xFFF8F8F2),
    onPrimary = Color(0xFF282A36),
    primaryContainer = Color(0xFF44475A),
    onPrimaryContainer = Color(0xFFF8F8F2),
    secondary = Color(0xFFF1FA8C),
    onSecondary = Color(0xFF282A36),
    secondaryContainer = Color(0xFF44475A),
    onSecondaryContainer = Color(0xFF282A36),
    tertiary = Color(0xFFFFB86C),
    onTertiary = Color(0xFF282A36),
    tertiaryContainer = Color(0xFF44475A),
    onTertiaryContainer = Color(0xFF282A36),
    error = Color(0xFFFF5555),
    onError = Color(0xFF282A36),
    errorContainer = Color(0xFF44475A),
    onErrorContainer = Color(0xFFF8F8F2),
    surface = Color(0xFF282A36),
    onSurface = Color(0xFFF8F8F2),
    surfaceVariant = Color(0xFF44475A),
    onSurfaceVariant = Color(0xFF6272A4),
    outline = Color(0xFF6272A4),
    outlineVariant = Color(0xFF44475A),
    background = Color(0xFF282A36),
    onBackground = Color(0xFFF8F8F2),
)

private val CatppuccinColorScheme = darkColorScheme(
    primary = Color(0xFFCBA6F7),
    onPrimary = Color(0xFF1E1E2E),
    primaryContainer = Color(0xFF313244),
    onPrimaryContainer = Color(0xFFF5C2E7),
    secondary = Color(0xFF94E2D5),
    onSecondary = Color(0xFF1E1E2E),
    secondaryContainer = Color(0xFF313244),
    onSecondaryContainer = Color(0xFFA6E3A1),
    tertiary = Color(0xFFF9E2AF),
    onTertiary = Color(0xFF1E1E2E),
    tertiaryContainer = Color(0xFF313244),
    onTertiaryContainer = Color(0xFFFAB387),
    error = Color(0xFFF38BA8),
    onError = Color(0xFF1E1E2E),
    errorContainer = Color(0xFF313244),
    onErrorContainer = Color(0xFFF5C2E7),
    surface = Color(0xFF1E1E2E),
    onSurface = Color(0xFFCDD6F4),
    surfaceVariant = Color(0xFF313244),
    onSurfaceVariant = Color(0xFFA6ADC8),
    outline = Color(0xFF45475A),
    outlineVariant = Color(0xFF313244),
    background = Color(0xFF1E1E2E),
    onBackground = Color(0xFFCDD6F4),
)

private val HighContrastLightColorScheme = lightColorScheme(
    primary = Color(0xFF000000),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF1A1A1A),
    onPrimaryContainer = Color.White,
    secondary = Color(0xFF000000),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF1A1A1A),
    onSecondaryContainer = Color.White,
    tertiary = Color(0xFF000000),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFF1A1A1A),
    onTertiaryContainer = Color.White,
    error = Color(0xFFB00020),
    onError = Color.White,
    errorContainer = Color(0xFFB00020),
    onErrorContainer = Color.White,
    surface = Color.White,
    onSurface = Color.Black,
    surfaceVariant = Color(0xFFF0F0F0),
    onSurfaceVariant = Color.Black,
    outline = Color.Black,
    outlineVariant = Color(0xFF1A1A1A),
    background = Color.White,
    onBackground = Color.Black,
)

private val SakuraColorScheme = lightColorScheme(
    primary = Color(0xFFD4577A),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFCDCE6),
    onPrimaryContainer = Color(0xFF6B1A35),
    secondary = Color(0xFF8B4572),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF5DAE8),
    onSecondaryContainer = Color(0xFF5B2D4A),
    tertiary = Color(0xFFC76B8A),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFF9E2EC),
    onTertiaryContainer = Color(0xFF7A3B55),
    error = Color(0xFFCF3055),
    onError = Color.White,
    errorContainer = Color(0xFFFADCE5),
    onErrorContainer = Color(0xFF931A35),
    surface = Color(0xFFFFF8F9),
    onSurface = Color(0xFF3B2230),
    surfaceVariant = Color(0xFFF8EAF0),
    onSurfaceVariant = Color(0xFF7A5668),
    outline = Color(0xFFD4BFC8),
    outlineVariant = Color(0xFFF0E0E8),
    background = Color(0xFFFFF8F9),
    onBackground = Color(0xFF3B2230),
)

private val AppTypography = Typography(
    displayLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 36.sp, lineHeight = 44.sp),
    displayMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 36.sp),
    headlineLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 32.sp),
    headlineMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 28.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 18.sp, lineHeight = 24.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 22.sp, letterSpacing = 0.15.sp),
    titleSmall = TextStyle(fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp),
    bodyLarge = TextStyle(fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.5.sp),
    bodyMedium = TextStyle(fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.25.sp),
    bodySmall = TextStyle(fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.4.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp),
    labelMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp),
    labelSmall = TextStyle(fontWeight = FontWeight.Medium, fontSize = 10.sp, lineHeight = 14.sp, letterSpacing = 0.5.sp),
)

@Composable
fun DuedigestTheme(
    themeMode: String = "system",
    content: @Composable () -> Unit,
) {
    val isDark = when (themeMode) {
        "amoled_dark", "dark", "solarized_dark", "nord", "dracula", "catppuccin" -> true
        "light", "solarized_light", "high_contrast", "sakura" -> false
        else -> isSystemInDarkTheme()
    }
    val colorScheme = when (themeMode) {
        "amoled_dark" -> AmoledDarkColorScheme
        "dark" -> DarkColorScheme
        "light" -> LightColorScheme
        "solarized_light" -> SolarizedLightColorScheme
        "solarized_dark" -> SolarizedDarkColorScheme
        "nord" -> NordColorScheme
        "dracula" -> DraculaColorScheme
        "catppuccin" -> CatppuccinColorScheme
        "high_contrast" -> HighContrastLightColorScheme
        "sakura" -> SakuraColorScheme
        else -> if (isSystemInDarkTheme()) DarkColorScheme else LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = { Surface { content() } },
    )
}
