package com.jpagdi.cromascheduler.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight

val LocalHeaderAccent = compositionLocalOf { CromaColors.Maroon }
val LocalButtonAccent = compositionLocalOf { CromaColors.Gold }

private fun colorsFor(mode: ThemeMode, buttonAccent: Color, panelAccent: Color) = when (mode) {
    ThemeMode.LIGHT -> lightColorScheme(
        primary = buttonAccent,
        onPrimary = AccentPrefs.textColorFor(buttonAccent),
        primaryContainer = buttonAccent.copy(alpha = 0.14f),
        onPrimaryContainer = buttonAccent,
        secondary = panelAccent,
        onSecondary = AccentPrefs.textColorFor(panelAccent),
        secondaryContainer = panelAccent.copy(alpha = 0.12f),
        onSecondaryContainer = panelAccent,
        tertiary = Color(0xFF5F5AA8),
        error = CromaStatus.Conflicts,
        onError = Color.White,
        background = CromaColors.Cream,
        onBackground = CromaColors.Ink,
        surface = CromaColors.Cream,
        onSurface = CromaColors.Ink,
        surfaceVariant = CromaColors.CreamDark,
        onSurfaceVariant = CromaColors.Ink.copy(alpha = 0.72f),
        surfaceContainer = Color(0xFFF1EFE9),
        surfaceContainerLow = Color(0xFFF5F3EE),
        surfaceContainerHigh = Color.White,
        surfaceContainerHighest = Color(0xFFE7E4DD),
        outline = Color(0xFF7A7771),
        outlineVariant = CromaColors.CardStrokeOnLight,
    )
    ThemeMode.DARK -> darkColorScheme(
        primary = buttonAccent,
        onPrimary = AccentPrefs.textColorFor(buttonAccent),
        primaryContainer = buttonAccent.copy(alpha = 0.20f),
        onPrimaryContainer = buttonAccent,
        secondary = panelAccent,
        onSecondary = AccentPrefs.textColorFor(panelAccent),
        secondaryContainer = panelAccent.copy(alpha = 0.18f),
        onSecondaryContainer = panelAccent,
        tertiary = Color(0xFFB9B3FF),
        error = CromaStatus.Conflicts,
        onError = Color.White,
        background = CromaColors.SurfaceDark,
        onBackground = CromaColors.TextDark,
        surface = CromaColors.SurfaceDark,
        onSurface = CromaColors.TextDark,
        surfaceVariant = Color(0xFF25272D),
        onSurfaceVariant = CromaColors.TextDark.copy(alpha = 0.72f),
        surfaceContainer = Color(0xFF1A1B20),
        surfaceContainerLow = Color(0xFF18191E),
        surfaceContainerHigh = CromaColors.CardSurfaceDark,
        surfaceContainerHighest = Color(0xFF292B31),
        outline = Color(0xFF8D9098),
        outlineVariant = CromaColors.CardStrokeOnDark,
    )
    ThemeMode.BLACK -> darkColorScheme(
        primary = buttonAccent,
        onPrimary = AccentPrefs.textColorFor(buttonAccent),
        primaryContainer = buttonAccent.copy(alpha = 0.20f),
        onPrimaryContainer = buttonAccent,
        secondary = panelAccent,
        onSecondary = AccentPrefs.textColorFor(panelAccent),
        secondaryContainer = panelAccent.copy(alpha = 0.18f),
        onSecondaryContainer = panelAccent,
        tertiary = Color(0xFFB9B3FF),
        error = CromaStatus.Conflicts,
        onError = Color.White,
        background = CromaColors.Black,
        onBackground = CromaColors.TextDark,
        surface = CromaColors.Black,
        onSurface = CromaColors.TextDark,
        surfaceVariant = Color(0xFF101114),
        onSurfaceVariant = CromaColors.TextDark.copy(alpha = 0.72f),
        surfaceContainer = Color(0xFF08090B),
        surfaceContainerLow = Color(0xFF050506),
        surfaceContainerHigh = CromaColors.CardSurfaceBlack,
        surfaceContainerHighest = Color(0xFF18191C),
        outline = Color(0xFF777A82),
        outlineVariant = CromaColors.CardStrokeOnDark,
    )
}

private val baseTypography = Typography()
val CromaTypography = Typography(
    displayLarge = baseTypography.displayLarge.copy(fontWeight = FontWeight.Bold),
    displayMedium = baseTypography.displayMedium.copy(fontWeight = FontWeight.Bold),
    displaySmall = baseTypography.displaySmall.copy(fontWeight = FontWeight.Bold),
    headlineLarge = baseTypography.headlineLarge.copy(fontWeight = FontWeight.Bold),
    headlineMedium = baseTypography.headlineMedium.copy(fontWeight = FontWeight.Bold),
    headlineSmall = baseTypography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
    titleLarge = baseTypography.titleLarge.copy(fontWeight = FontWeight.Bold),
    titleMedium = baseTypography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    titleSmall = baseTypography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
    bodyLarge = baseTypography.bodyLarge.copy(fontWeight = FontWeight.Normal),
    bodyMedium = baseTypography.bodyMedium.copy(fontWeight = FontWeight.Normal),
    bodySmall = baseTypography.bodySmall.copy(fontWeight = FontWeight.Normal),
    labelLarge = baseTypography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
    labelMedium = baseTypography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
    labelSmall = baseTypography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
)

@Composable
fun CromaSchedulerTheme(
    themeMode: ThemeMode? = null,
    groupA: Color = AccentPrefs.DEFAULT_GROUP_A,
    groupB: Color = AccentPrefs.DEFAULT_GROUP_B,
    content: @Composable () -> Unit,
) {
    val resolvedMode = themeMode ?: if (isSystemInDarkTheme()) ThemeMode.DARK else ThemeMode.LIGHT
    // In Black mode, keep the app shell black; the chosen panel accent still appears in controls.
    val headerAccent = if (resolvedMode == ThemeMode.BLACK) CromaColors.Black else groupA
    CompositionLocalProvider(
        LocalHeaderAccent provides headerAccent,
        LocalButtonAccent provides groupB,
    ) {
        MaterialTheme(
            colorScheme = colorsFor(resolvedMode, groupB, groupA),
            shapes = CromaShapes,
            typography = CromaTypography,
            content = content,
        )
    }
}
