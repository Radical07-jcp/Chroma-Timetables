package com.jpagdi.cromascheduler.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = CromaColors.Blue,
    onPrimary = CromaColors.White,
    primaryContainer = Color(0xFFDCE3FC),
    onPrimaryContainer = Color(0xFF17296B),
    secondary = CromaColors.Gold,
    onSecondary = CromaColors.Navy,
    secondaryContainer = Color(0xFFFFEACC),
    onSecondaryContainer = Color(0xFF4A2E00),
    tertiary = CromaColors.Mint,
    onTertiary = CromaColors.Navy,
    tertiaryContainer = Color(0xFFCDF7E7),
    onTertiaryContainer = Color(0xFF0A3F2E),
    error = CromaColors.Red,
    onError = CromaColors.White,
    errorContainer = Color(0xFFFBD8DB),
    onErrorContainer = Color(0xFF5C0E17),
    background = CromaColors.SurfaceLight,
    onBackground = CromaColors.TextLight,
    surface = CromaColors.CardLight,
    onSurface = CromaColors.TextLight,
    surfaceVariant = CromaColors.SurfaceLight,
    onSurfaceVariant = CromaColors.TextLight.copy(alpha = 0.7f),
    surfaceContainer = CromaColors.SurfaceLight,
    surfaceContainerHigh = Color(0xFFEFF1F5),
    surfaceContainerHighest = Color(0xFFE7E9EF),
    surfaceContainerLow = CromaColors.CardLight,
    surfaceContainerLowest = CromaColors.CardLight,
    outline = CromaColors.OutlineLight,
    outlineVariant = CromaColors.OutlineLight,
)

private val DarkColors = darkColorScheme(
    primary = CromaColors.Blue,
    onPrimary = CromaColors.White,
    primaryContainer = Color(0xFF2A3B6B),
    onPrimaryContainer = Color(0xFFC9D4FF),
    secondary = CromaColors.Gold,
    onSecondary = CromaColors.Navy,
    secondaryContainer = Color(0xFF4A3A15),
    onSecondaryContainer = Color(0xFFFFDFA0),
    tertiary = CromaColors.Mint,
    onTertiary = CromaColors.Navy,
    tertiaryContainer = Color(0xFF1E4A3D),
    onTertiaryContainer = Color(0xFFAFF3D9),
    error = CromaColors.Red,
    onError = CromaColors.White,
    errorContainer = Color(0xFF5C1A22),
    onErrorContainer = Color(0xFFFFB3BA),
    background = CromaColors.SurfaceDark,
    onBackground = CromaColors.TextDark,
    surface = CromaColors.CardDark,
    onSurface = CromaColors.TextDark,
    surfaceVariant = CromaColors.SurfaceDark,
    onSurfaceVariant = CromaColors.TextDark.copy(alpha = 0.7f),
    surfaceContainer = CromaColors.CardDark,
    surfaceContainerHigh = Color(0xFF283142),
    surfaceContainerHighest = Color(0xFF313B4D),
    surfaceContainerLow = CromaColors.SurfaceDark,
    surfaceContainerLowest = CromaColors.NavyDark,
    outline = CromaColors.OutlineDark,
    outlineVariant = CromaColors.OutlineDark,
)

private val BlackColors = darkColorScheme(
    primary = CromaColors.Blue,
    onPrimary = CromaColors.White,
    primaryContainer = Color(0xFF23305A),
    onPrimaryContainer = Color(0xFFC9D4FF),
    secondary = CromaColors.Gold,
    onSecondary = CromaColors.Navy,
    secondaryContainer = Color(0xFF3D3010),
    onSecondaryContainer = Color(0xFFFFDFA0),
    tertiary = CromaColors.Mint,
    onTertiary = CromaColors.Navy,
    tertiaryContainer = Color(0xFF163A2F),
    onTertiaryContainer = Color(0xFFAFF3D9),
    error = CromaColors.Red,
    onError = CromaColors.White,
    errorContainer = Color(0xFF4A151C),
    onErrorContainer = Color(0xFFFFB3BA),
    background = CromaColors.SurfaceBlack,
    onBackground = CromaColors.TextBlack,
    surface = CromaColors.CardBlack,
    onSurface = CromaColors.TextBlack,
    surfaceVariant = CromaColors.SurfaceBlack,
    onSurfaceVariant = CromaColors.TextBlack.copy(alpha = 0.7f),
    surfaceContainer = CromaColors.CardBlack,
    surfaceContainerHigh = Color(0xFF1C1C1C),
    surfaceContainerHighest = Color(0xFF262626),
    surfaceContainerLow = CromaColors.SurfaceBlack,
    surfaceContainerLowest = CromaColors.SurfaceBlack,
    outline = CromaColors.OutlineBlack,
    outlineVariant = CromaColors.OutlineBlack,
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
    displayLarge = baseTypography.displayLarge.copy(fontFamily = MontserratFamily, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
    displayMedium = baseTypography.displayMedium.copy(fontFamily = MontserratFamily, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
    displaySmall = baseTypography.displaySmall.copy(fontFamily = MontserratFamily, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold),
    headlineLarge = baseTypography.headlineLarge.copy(fontFamily = MontserratFamily, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold),
    headlineMedium = baseTypography.headlineMedium.copy(fontFamily = MontserratFamily, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold),
    headlineSmall = baseTypography.headlineSmall.copy(fontFamily = MontserratFamily, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold),
    titleLarge = baseTypography.titleLarge.copy(fontFamily = MontserratFamily, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
    titleMedium = baseTypography.titleMedium.copy(fontFamily = MontserratFamily, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold),
    titleSmall = baseTypography.titleSmall.copy(fontFamily = MontserratFamily, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold),
    bodyLarge = baseTypography.bodyLarge.copy(fontFamily = MontserratFamily, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium),
    bodyMedium = baseTypography.bodyMedium.copy(fontFamily = MontserratFamily, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium),
    bodySmall = baseTypography.bodySmall.copy(fontFamily = MontserratFamily, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium),
    labelLarge = baseTypography.labelLarge.copy(fontFamily = MontserratFamily, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold),
    labelMedium = baseTypography.labelMedium.copy(fontFamily = MontserratFamily, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold),
    labelSmall = baseTypography.labelSmall.copy(fontFamily = MontserratFamily, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold),
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
