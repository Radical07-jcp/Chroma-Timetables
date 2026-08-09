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
