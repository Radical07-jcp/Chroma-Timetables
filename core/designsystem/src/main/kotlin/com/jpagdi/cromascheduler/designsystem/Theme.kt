package com.jpagdi.cromascheduler.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = CromaAccents.default.surface,
    surface = CromaSurfaces.cardLight,
    outline = CromaSurfaces.cardStrokeLight,
)

private val DarkColors = darkColorScheme(
    primary = CromaAccents.default.surface,
    surface = CromaSurfaces.cardDark,
    outline = CromaSurfaces.cardStrokeDark,
)

@Composable
fun CromaSchedulerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colors,
        shapes = CromaShapes,
        content = content,
    )
}
