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

/**
 * Two roles a plain Material3 ColorScheme can't cleanly express on its own, ported from the
 * reference app's "GroupA / GroupB" accent system (see AccentPrefs.kt for the math/presets, and
 * its doc comment for why the actual recoloring mechanism — View-tree tag-walking — didn't need to
 * come along, only the design). [LocalHeaderAccent] is what CromaTopBar/CromaHomeHeader/
 * SidebarDrawer read for their panel background; [LocalButtonAccent] is what primary
 * buttons/FABs read. Both default to the same values CromaSchedulerTheme uses when no
 * AccentColorViewModel is in the tree (e.g. a lone @Preview), so nothing breaks outside the real
 * app — but in the real app, CromaSchedulerTheme's groupA/groupB params always override these.
 */
val LocalHeaderAccent = compositionLocalOf { CromaColors.Maroon }
val LocalButtonAccent = compositionLocalOf { CromaColors.Gold }

private fun colorsFor(mode: ThemeMode, buttonAccent: Color, panelAccent: Color) = when (mode) {
    ThemeMode.LIGHT -> lightColorScheme(
        primary = buttonAccent,
        onPrimary = AccentPrefs.textColorFor(buttonAccent),
        secondary = panelAccent,
        onSecondary = AccentPrefs.textColorFor(panelAccent),
        error = CromaStatus.Conflicts,
        onError = Color.White,
        background = CromaColors.Cream,
        onBackground = CromaColors.Ink,
        surface = CromaColors.CardSurfaceLight,
        onSurface = CromaColors.Ink,
        surfaceVariant = CromaColors.CreamDark,
        onSurfaceVariant = CromaColors.Ink.copy(alpha = 0.7f),
        surfaceContainer = CromaColors.CreamDark,
        surfaceContainerLow = CromaColors.CardSurfaceLight,
        surfaceContainerLowest = CromaColors.CardSurfaceLight,
        outline = CromaColors.CardStrokeOnLight,
        outlineVariant = CromaColors.CardStrokeOnLight,
    )
    ThemeMode.DARK -> darkColorScheme(
        primary = buttonAccent,
        onPrimary = AccentPrefs.textColorFor(buttonAccent),
        secondary = panelAccent,
        onSecondary = AccentPrefs.textColorFor(panelAccent),
        error = CromaStatus.Conflicts,
        onError = Color.White,
        background = CromaColors.SurfaceDark,
        onBackground = CromaColors.TextDark,
        surface = CromaColors.CardSurfaceDark,
        onSurface = CromaColors.TextDark,
        surfaceVariant = CromaColors.SurfaceDark,
        onSurfaceVariant = CromaColors.TextDark.copy(alpha = 0.7f),
        surfaceContainer = CromaColors.CardSurfaceDark,
        surfaceContainerLow = CromaColors.SurfaceDark,
        surfaceContainerLowest = CromaColors.SurfaceDark,
        outline = CromaColors.CardStrokeOnDark,
        outlineVariant = CromaColors.CardStrokeOnDark,
    )
    ThemeMode.BLACK -> darkColorScheme(
        primary = buttonAccent,
        onPrimary = AccentPrefs.textColorFor(buttonAccent),
        secondary = panelAccent,
        onSecondary = AccentPrefs.textColorFor(panelAccent),
        error = CromaStatus.Conflicts,
        onError = Color.White,
        background = CromaColors.SurfaceBlack,
        onBackground = CromaColors.TextDark,
        surface = CromaColors.CardSurfaceBlack,
        onSurface = CromaColors.TextDark,
        surfaceVariant = CromaColors.SurfaceBlack,
        onSurfaceVariant = CromaColors.TextDark.copy(alpha = 0.7f),
        surfaceContainer = CromaColors.CardSurfaceBlack,
        surfaceContainerLow = CromaColors.SurfaceBlack,
        surfaceContainerLowest = CromaColors.SurfaceBlack,
        outline = CromaColors.CardStrokeOnDark,
        outlineVariant = CromaColors.CardStrokeOnDark,
    )
}

/**
 * Every named slot in Material3's type scale, swapped onto Montserrat — this is what makes the
 * font app-wide rather than something applied Text-by-Text. Sizes/line-heights/letter-spacing are
 * Material3's own defaults (M3's base Typography()), only fontFamily/weight change. Press Start 2P
 * is NOT part of this type scale — it's scoped to exactly the CHROMA/TIMETABLES brand wordmark
 * (see BrandWordmark in CommonUi.kt), same "just those two spots" scope the reference app used it
 * with, not a general-purpose type-scale entry.
 */
private val baseTypography = Typography()
val CromaTypography = Typography(
    displayLarge = baseTypography.displayLarge.copy(fontFamily = MontserratFamily, fontWeight = FontWeight.Bold),
    displayMedium = baseTypography.displayMedium.copy(fontFamily = MontserratFamily, fontWeight = FontWeight.Bold),
    displaySmall = baseTypography.displaySmall.copy(fontFamily = MontserratFamily, fontWeight = FontWeight.SemiBold),
    headlineLarge = baseTypography.headlineLarge.copy(fontFamily = MontserratFamily, fontWeight = FontWeight.SemiBold),
    headlineMedium = baseTypography.headlineMedium.copy(fontFamily = MontserratFamily, fontWeight = FontWeight.SemiBold),
    headlineSmall = baseTypography.headlineSmall.copy(fontFamily = MontserratFamily, fontWeight = FontWeight.SemiBold),
    titleLarge = baseTypography.titleLarge.copy(fontFamily = MontserratFamily, fontWeight = FontWeight.Bold),
    titleMedium = baseTypography.titleMedium.copy(fontFamily = MontserratFamily, fontWeight = FontWeight.SemiBold),
    titleSmall = baseTypography.titleSmall.copy(fontFamily = MontserratFamily, fontWeight = FontWeight.SemiBold),
    bodyLarge = baseTypography.bodyLarge.copy(fontFamily = MontserratFamily, fontWeight = FontWeight.Medium),
    bodyMedium = baseTypography.bodyMedium.copy(fontFamily = MontserratFamily, fontWeight = FontWeight.Medium),
    bodySmall = baseTypography.bodySmall.copy(fontFamily = MontserratFamily, fontWeight = FontWeight.Medium),
    labelLarge = baseTypography.labelLarge.copy(fontFamily = MontserratFamily, fontWeight = FontWeight.SemiBold),
    labelMedium = baseTypography.labelMedium.copy(fontFamily = MontserratFamily, fontWeight = FontWeight.SemiBold),
    labelSmall = baseTypography.labelSmall.copy(fontFamily = MontserratFamily, fontWeight = FontWeight.SemiBold),
)

/**
 * [themeMode] defaults to null, which means "follow the system light/dark setting" (Light or Dark
 * only — Black is never auto-selected, since it's an explicit OLED-battery choice). [groupA]
 * ("Top Panel Accent") and [groupB] ("Button Accent") default to the reference app's own defaults
 * (maroon/gold) and are what AccentColorViewModel overrides once a person picks something else in
 * Settings.
 *
 * Black theme is the one place [groupA] is deliberately NOT what headers use — same as the
 * reference app's own `Theme.MCQScanner.Black` (colorPrimary = a near-black surface tone there,
 * not the maroon every other theme uses), so panels recede into the near-black background instead
 * of standing out as a lighter maroon block; gold (groupB) stays the only real accent that pops in
 * that mode. See [LocalHeaderAccent].
 */
@Composable
fun CromaSchedulerTheme(
    themeMode: ThemeMode? = null,
    groupA: Color = AccentPrefs.DEFAULT_GROUP_A,
    groupB: Color = AccentPrefs.DEFAULT_GROUP_B,
    content: @Composable () -> Unit,
) {
    val resolvedMode = themeMode ?: if (isSystemInDarkTheme()) ThemeMode.DARK else ThemeMode.LIGHT
    val headerAccent = if (resolvedMode == ThemeMode.BLACK) CromaColors.CardSurfaceBlack else groupA
    val colors = colorsFor(resolvedMode, buttonAccent = groupB, panelAccent = groupA)

    CompositionLocalProvider(
        LocalHeaderAccent provides headerAccent,
        LocalButtonAccent provides groupB,
    ) {
        MaterialTheme(
            colorScheme = colors,
            shapes = CromaShapes,
            typography = CromaTypography,
            content = content,
        )
    }
}
