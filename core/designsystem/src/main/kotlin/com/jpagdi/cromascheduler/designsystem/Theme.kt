package com.jpagdi.cromascheduler.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
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
 * Every named slot in Material3's type scale, swapped onto Montserrat — this is what makes the
 * font app-wide rather than something applied Text-by-Text. Sizes/line-heights/letter-spacing are
 * Material3's own defaults (M3's base Typography()), only fontFamily changes; this file is not the
 * place to fix the "no line/paragraph spacing" header request — that's a per-Text lineHeight
 * override where the CHROMA/TIMETABLES two-line header is actually built (see SidebarDrawer.kt and
 * CommonUi.kt's CromaHeader), since collapsing spacing there but not everywhere else in the app is
 * the actual ask, not a global type-scale change.
 */
private val baseTypography = Typography()
val CromaTypography = Typography(
    displayLarge = baseTypography.displayLarge.copy(fontFamily = MontserratFamily),
    displayMedium = baseTypography.displayMedium.copy(fontFamily = MontserratFamily),
    displaySmall = baseTypography.displaySmall.copy(fontFamily = MontserratFamily),
    headlineLarge = baseTypography.headlineLarge.copy(fontFamily = MontserratFamily),
    headlineMedium = baseTypography.headlineMedium.copy(fontFamily = MontserratFamily),
    headlineSmall = baseTypography.headlineSmall.copy(fontFamily = MontserratFamily),
    titleLarge = baseTypography.titleLarge.copy(fontFamily = MontserratFamily, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold),
    titleMedium = baseTypography.titleMedium.copy(fontFamily = MontserratFamily, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold),
    titleSmall = baseTypography.titleSmall.copy(fontFamily = MontserratFamily, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium),
    bodyLarge = baseTypography.bodyLarge.copy(fontFamily = MontserratFamily),
    bodyMedium = baseTypography.bodyMedium.copy(fontFamily = MontserratFamily),
    bodySmall = baseTypography.bodySmall.copy(fontFamily = MontserratFamily),
    labelLarge = baseTypography.labelLarge.copy(fontFamily = MontserratFamily, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium),
    labelMedium = baseTypography.labelMedium.copy(fontFamily = MontserratFamily, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium),
    labelSmall = baseTypography.labelSmall.copy(fontFamily = MontserratFamily, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium),
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
        typography = CromaTypography,
        content = content,
    )
}
