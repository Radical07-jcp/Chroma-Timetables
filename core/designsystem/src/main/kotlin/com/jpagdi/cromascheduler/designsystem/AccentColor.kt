package com.jpagdi.cromascheduler.designsystem

import androidx.compose.ui.graphics.Color

/**
 * Same lesson learned on MCQ Quick Check applied from day one here: an accent color
 * that works as a card fill can still fail contrast for icon/text drawn ON TOP of it
 * (e.g. gold-on-gold, maroon-on-maroon were bugs fixed late in MCQ v3.7.x). So every
 * accent below ships its own pre-picked onIcon/onText color instead of the UI layer
 * assuming white or black always works.
 */
data class AccentColor(
    val surface: Color,
    val onIcon: Color,
    val onText: Color,
)

object CromaAccents {
    val Teal = AccentColor(surface = Color(0xFF1F8A70), onIcon = Color.White, onText = Color.White)
    val Indigo = AccentColor(surface = Color(0xFF3949AB), onIcon = Color.White, onText = Color.White)
    val Amber = AccentColor(surface = Color(0xFFFFB300), onIcon = Color(0xFF1A1A1A), onText = Color(0xFF1A1A1A))
    val Maroon = AccentColor(surface = Color(0xFF8E2434), onIcon = Color.White, onText = Color.White)

    val default = Teal
}

// Card surface colors — independent of the accent system, same split MCQ uses
// (colorSurfaceCard / colorSurfaceCardStroke) so cards read consistently regardless
// of which accent a given screen/button is using.
object CromaSurfaces {
    val cardLight = Color(0xFFF7F7F9)
    val cardStrokeLight = Color(0xFFE1E1E6)
    val cardDark = Color(0xFF232326)
    val cardStrokeDark = Color(0xFF35353A)
}
