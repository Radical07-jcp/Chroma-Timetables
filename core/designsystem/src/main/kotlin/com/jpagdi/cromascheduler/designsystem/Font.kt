package com.jpagdi.cromascheduler.designsystem

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight

/**
 * Chroma's primary UI typeface. The app ships the Space Grotesk variable font as a bundled asset
 * so it works offline and does not rely on Google Fonts runtime download behavior.
 */
private fun spaceGrotesk(weight: FontWeight) = Font(
    resId = R.font.space_grotesk_regular,
    weight = weight,
    style = FontStyle.Normal,
)

val SpaceGroteskFamily = FontFamily(
    spaceGrotesk(FontWeight.Normal),
    spaceGrotesk(FontWeight.Medium),
    spaceGrotesk(FontWeight.SemiBold),
    spaceGrotesk(FontWeight.Bold),
)

/** Pixel display face reserved for the CHROMA / TIMETABLES brand wordmark. */
val PressStart2PFamily = FontFamily(Font(R.font.press_start_2p, FontWeight.Normal))
