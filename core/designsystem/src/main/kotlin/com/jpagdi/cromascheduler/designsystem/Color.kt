package com.jpagdi.cromascheduler.designsystem

import androidx.compose.ui.graphics.Color

data class AccentColor(
    val surface: Color,
    val onIcon: Color,
    val onText: Color,
)

object CromaColors {
    // Brand defaults. User-selected accents remain fully supported by AccentPrefs.
    val Maroon = Color(0xFF7C2330)
    val MaroonDark = Color(0xFF55131D)
    val Gold = Color(0xFFE4A72C)
    val GoldLight = Color(0xFFF6D98B)

    // Neutral foundation — deliberately restrained so the user's accent remains the visual focus.
    val Cream = Color(0xFFF9F7F2)
    val CreamDark = Color(0xFFEDEAE3)
    val Ink = Color(0xFF17181B)
    val White = Color(0xFFFFFFFF)
    val Black = Color(0xFF000000)

    val SurfaceDark = Color(0xFF15161A)
    val SurfaceBlack = Color(0xFF000000)
    val CardBlack = Color(0xFF0E0F12)
    val TextDark = Color(0xFFF1F1F3)

    val CardSurfaceLight = Color(0xFFFFFFFF)
    val CardSurfaceDark = Color(0xFF1E2025)
    val CardSurfaceBlack = Color(0xFF101114)

    val CardStrokeOnLight = Color(0x16000000)
    val CardStrokeOnDark = Color(0x22FFFFFF)
}

object CromaAccents {
    val Maroon = AccentColor(CromaColors.Maroon, Color.White, Color.White)
    val MaroonLight = AccentColor(Color(0xFFB43A4B), Color.White, Color.White)
    val MaroonDark = AccentColor(CromaColors.MaroonDark, Color.White, Color.White)
    val Gold = AccentColor(CromaColors.Gold, CromaColors.Ink, CromaColors.Ink)
    val GoldLight = AccentColor(CromaColors.GoldLight, CromaColors.Ink, CromaColors.Ink)
    val default = Maroon
}

object CromaStatus {
    val Clean = Color(0xFF2E9B68)
    val Conflicts = Color(0xFFD64B4B)
    val Pending = Color(0xFF7C8492)
}
