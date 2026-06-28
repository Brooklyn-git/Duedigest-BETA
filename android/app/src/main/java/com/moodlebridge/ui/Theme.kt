package com.moodlebridge.ui

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

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
fun DueNestTheme(
    themeMode: String = "system",
    content: @Composable () -> Unit,
) {
    val isDark = when (themeMode) {
        "amoled_dark" -> true
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
    }
    val colorScheme = when (themeMode) {
        "amoled_dark" -> AmoledDarkColorScheme
        "dark" -> DarkColorScheme
        "light" -> LightColorScheme
        else -> if (isSystemInDarkTheme()) DarkColorScheme else LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDark
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = { androidx.compose.material3.Surface { content() } },
    )
}
