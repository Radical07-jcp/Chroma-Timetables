package com.jpagdi.cromascheduler.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = CromaColors.Blue,
    secondary = CromaColors.Gold,
    tertiary = CromaColors.Mint,
    error = CromaColors.Red,
    background = CromaColors.SurfaceLight,
    surface = CromaColors.CardLight,
    onBackground = CromaColors.TextLight,
    onSurface = CromaColors.TextLight,
    outline = CromaColors.OutlineLight,
)

private val DarkColors = darkColorScheme(
    primary = CromaColors.Blue,
    secondary = CromaColors.Gold,
    tertiary = CromaColors.Mint,
    error = CromaColors.Red,
    background = CromaColors.SurfaceDark,
    surface = CromaColors.CardDark,
    onBackground = CromaColors.TextDark,
    onSurface = CromaColors.TextDark,
    outline = CromaColors.OutlineDark,
)

private val BlackColors = darkColorScheme(
    primary = CromaColors.Blue,
    secondary = CromaColors.Gold,
    tertiary = CromaColors.Mint,
    error = CromaColors.Red,
    background = CromaColors.SurfaceBlack,
    surface = CromaColors.CardBlack,
    onBackground = CromaColors.TextBlack,
    onSurface = CromaColors.TextBlack,
    outline = CromaColors.OutlineBlack,
)

/**
 * [themeMode] defaults to null, which means "follow the system light/dark setting" (Light or Dark
 * only — Black is never auto-selected, since it's an explicit OLED-battery choice, not something
 * to guess at). Pass an explicit [ThemeMode] once the user has picked one in Settings.
 */
@Composable
fun CromaSchedulerTheme(
    themeMode: ThemeMode? = null,
    content: @Composable () -> Unit,
) {
    val resolvedMode = themeMode ?: if (isSystemInDarkTheme()) ThemeMode.DARK else ThemeMode.LIGHT
    val colors = when (resolvedMode) {
        ThemeMode.LIGHT -> LightColors
        ThemeMode.DARK -> DarkColors
        ThemeMode.BLACK -> BlackColors
    }
    MaterialTheme(
        colorScheme = colors,
        shapes = CromaShapes,
        content = content,
    )
}
