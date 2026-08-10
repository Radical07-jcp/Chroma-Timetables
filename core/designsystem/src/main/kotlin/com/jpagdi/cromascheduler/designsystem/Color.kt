package com.jpagdi.cromascheduler.designsystem

import androidx.compose.ui.graphics.Color

/**
 * Palette recreated to exactly match a reference app (internally: "MCQ Quick Check" /
 * mcq_scanner v1.0.0) per explicit request — maroon + gold + cream, not the earlier
 * icon-derived navy/mint/blue palette. The app's own launcher icon (calendar + graph, unrelated
 * to this theme swap) is untouched; this file only governs in-app surface/accent colors.
 *
 * Every color the app uses comes from here — one source of truth, not duplicated into XML
 * resources anywhere, for the same reason documented on this file before: two copies of "the
 * app's accent gold" drift out of sync the first time either one gets nudged half a shade.
 */
data class AccentColor(
    val surface: Color,
    val onIcon: Color,
    val onText: Color,
)

object CromaColors {
    // Core brand pair — these two are also GroupA/GroupB's DEFAULT values in AccentPrefs.kt,
    // restored exactly by "Revert to Default" there, same as the reference app's own behavior.
    val Maroon = Color(0xFF6B0F14)
    val MaroonDark = Color(0xFF4A0709)
    val Gold = Color(0xFFE8B923)
    val GoldLight = Color(0xFFF5D97A)
    val Cream = Color(0xFFFFF8E7)
    val CreamDark = Color(0xFFF2E9D3)
    val Ink = Color(0xFF221111)
    val White = Color(0xFFFFFFFF)
    val Black = Color(0xFF000000)

    // Dark theme's window background — a maroon-tinted near-black (red channel visibly higher
    // than green/blue), not a neutral gray; deliberately lighter than Black theme's true black so
    // the two stay visually distinct tiers instead of converging on nearly the same near-black.
    val SurfaceDark = Color(0xFF242424)

    // Black theme — true near-black, OLED-friendly.
    val SurfaceBlack = Color(0xFF0D0D0D)
    val CardBlack = Color(0xFF1A1A1A)

    // Solid (not translucent) light text for Dark/Black — Material's own default for
    // textColorPrimary-equivalent roles in a dark scheme is ~87%-alpha white, not solid; using
    // solid white here avoids the faded-text look that default produces.
    val TextDark = White

    // One dedicated card-surface tone per theme, each a deliberate step lighter than that theme's
    // own background so cards read as raised panels rather than blending flat into the page — not
    // reused from primary/secondary, independent of whichever accent colors are active.
    val CardSurfaceLight = White
    val CardSurfaceDark = Color(0xFF2E2E2E)
    val CardSurfaceBlack = CardBlack

    // Hairline card border — just enough definition to read as an edge without a heavy outline.
    val CardStrokeOnLight = Color(0x14000000)
    val CardStrokeOnDark = Color(0x1FFFFFFF)
}

object CromaAccents {
    val Maroon = AccentColor(surface = CromaColors.Maroon, onIcon = Color.White, onText = Color.White)
    val MaroonLight = AccentColor(surface = Color(0xFFB0293A), onIcon = Color.White, onText = Color.White)
    val MaroonDark = AccentColor(surface = CromaColors.MaroonDark, onIcon = Color.White, onText = Color.White)
    val Gold = AccentColor(surface = CromaColors.Gold, onIcon = CromaColors.Ink, onText = CromaColors.Ink)
    val GoldLight = AccentColor(surface = CromaColors.GoldLight, onIcon = CromaColors.Ink, onText = CromaColors.Ink)

    val default = Maroon
}

/** Status-pill colors — Home's timetable list and every "N conflicts" badge draw from these three, never a locally invented color. Deliberately independent of the maroon/gold accent system (clean/conflict meaning shouldn't shift if someone picks an unusual accent color). */
object CromaStatus {
    val Clean = Color(0xFF2FAE68)
    val Conflicts = Color(0xFFC62828)
    val Pending = Color(0xFF8A93A3)
}
