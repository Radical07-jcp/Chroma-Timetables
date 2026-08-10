package com.jpagdi.cromascheduler.designsystem

import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight

/**
 * One bundled variable-weight TTF (res/font/montserrat.ttf, the real Google Fonts release —
 * fetched from google/fonts on GitHub, not a Google Fonts *downloadable* runtime dependency, so
 * this stays fully offline/on-device like the rest of the app) covers every weight the type scale
 * below needs via FontVariation's "wght" axis — no separate Regular/Medium/SemiBold/Bold files to
 * bundle and keep in sync.
 */
@OptIn(ExperimentalTextApi::class)
private fun montserrat(weight: FontWeight) = Font(
    resId = R.font.montserrat,
    weight = weight,
    style = FontStyle.Normal,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
)

@OptIn(ExperimentalTextApi::class)
val MontserratFamily = FontFamily(
    montserrat(FontWeight.Normal),
    montserrat(FontWeight.Medium),
    montserrat(FontWeight.SemiBold),
    montserrat(FontWeight.Bold),
)

/**
 * Used in exactly two places, per the reference app's own scope for this font: the Home header's
 * brand title and the sidebar's brand title (see BrandWordmark in CommonUi.kt) — everywhere else
 * stays Montserrat. A single static weight is enough since PressStart2P-Regular.ttf is the only
 * cut Google Fonts publishes for this typeface (an 8-bit/arcade display face has no natural bold).
 */
val PressStart2PFamily = FontFamily(Font(R.font.press_start_2p, FontWeight.Normal))
