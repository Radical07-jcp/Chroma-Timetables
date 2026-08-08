package com.jpagdi.cromascheduler.designsystem

import androidx.compose.ui.graphics.Color

/**
 * Every color the app uses comes from here — this replaced an earlier setup that duplicated a
 * near-identical palette in both this file and a set of XML `<color>` resources for a since-removed
 * View-based Home/Timetable-Detail screen pair. Keeping ONE source of truth is the actual fix, not
 * just a preference: two copies of "the app's accent gold" drift out of sync the first time either
 * one gets nudged half a shade during polish, and the app ends up looking like two apps stitched
 * together — which is exactly what happened before this rewrite.
 *
 * Same lesson learned on MCQ Quick Check applies here too: an accent that works as a card fill can
 * still fail contrast for icon/text drawn ON TOP of it, so every accent ships its own pre-picked
 * onIcon/onText color instead of the UI layer assuming white or black always works.
 */
data class AccentColor(
    val surface: Color,
    val onIcon: Color,
    val onText: Color,
)

object CromaColors {
    // Straight from the launcher icon's graph nodes.
    val Navy = Color(0xFF1B2430)
    val NavyDark = Color(0xFF10151D)
    val Mint = Color(0xFF4FE3B0)
    val Blue = Color(0xFF4C6FF2)
    val Gold = Color(0xFFFFB020)
    val Red = Color(0xFFF0374A)
    val White = Color(0xFFFFFFFF)

    // Neutral surfaces — Light / Dark / Black variants (Black is true near-#000 for OLED,
    // not just a darker version of Dark — that distinction is the entire point of a third mode).
    val SurfaceLight = Color(0xFFF7F8FA)
    val CardLight = Color(0xFFFFFFFF)
    val OutlineLight = Color(0xFFE3E6EC)
    val TextLight = Color(0xFF1B2430)

    val SurfaceDark = Color(0xFF161D27)
    val CardDark = Color(0xFF212A38)
    val OutlineDark = Color(0xFF323D4E)
    val TextDark = Color(0xFFF1F3F7)

    val SurfaceBlack = Color(0xFF000000)
    val CardBlack = Color(0xFF121212)
    val OutlineBlack = Color(0xFF2A2A2A)
    val TextBlack = Color(0xFFEDEDED)
}

object CromaAccents {
    val Mint = AccentColor(surface = CromaColors.Mint, onIcon = CromaColors.Navy, onText = CromaColors.Navy)
    val Blue = AccentColor(surface = CromaColors.Blue, onIcon = Color.White, onText = Color.White)
    val Gold = AccentColor(surface = CromaColors.Gold, onIcon = CromaColors.Navy, onText = CromaColors.Navy)
    val Red = AccentColor(surface = CromaColors.Red, onIcon = Color.White, onText = Color.White)
    val Navy = AccentColor(surface = CromaColors.Navy, onIcon = Color.White, onText = Color.White)

    val default = Blue
}

/** Status-pill colors — Home's timetable list and every "N conflicts" badge draw from these three, never a locally invented color. */
object CromaStatus {
    val Clean = Color(0xFF2FAE68)
    val Conflicts = CromaColors.Red
    val Pending = Color(0xFF8A93A3)
}
