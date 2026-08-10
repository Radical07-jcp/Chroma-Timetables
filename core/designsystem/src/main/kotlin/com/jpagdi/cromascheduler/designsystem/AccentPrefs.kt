package com.jpagdi.cromascheduler.designsystem

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlin.math.pow

/**
 * The "two independently-recolorable accent groups" concept, ported from the reference app's
 * AccentColorPrefs — but the actual recoloring mechanism doesn't carry over, on purpose. That app
 * had to walk its whole View tree and manually retint every tagged view, because Android's
 * classic View theme system has no way to swap a live ?attr/colorAccent at runtime. Compose has no
 * such limitation: GroupA/GroupB are just Color state threaded through CromaSchedulerTheme's
 * ColorScheme, so every composable that reads MaterialTheme.colorScheme.primary/secondary already
 * recomposes automatically the moment either value changes — no tags, no tree-walk, no per-view
 * tinting code needed at all. What's kept 1:1 is the actual DESIGN of the feature: which two
 * defaults, the same 16-swatch preset grid, and the same luminance-based contrast math.
 *
 * GroupA = "Top Panel Accent" (headers, sidebar) — defaults to Maroon.
 * GroupB = "Button Accent" (primary buttons, FAB) — defaults to Gold.
 */
object AccentPrefs {
    val DEFAULT_GROUP_A = CromaColors.Maroon
    val DEFAULT_GROUP_B = CromaColors.Gold

    /** Same fixed 16-swatch grid as the reference app — a deliberate choice, not a continuous picker — including both current defaults as the first entry of their own family. */
    val PRESET_SWATCHES: List<Color> = listOf(
        Color(0xFF6B0F14), Color(0xFFB0293A), Color(0xFF7A1F3D), Color(0xFF4A0709),
        Color(0xFFE8B923), Color(0xFFD4A017), Color(0xFFC77B2E), Color(0xFF8A6D3B),
        Color(0xFF1F5C4A), Color(0xFF2E7D32), Color(0xFF1565A8), Color(0xFF2A3F8F),
        Color(0xFF5C2D91), Color(0xFF8E44AD), Color(0xFF37474F), Color(0xFF212121),
    )

    /** Relative luminance (WCAG-style, sRGB) — decides readable text/icon color on an arbitrary chosen background instead of a manual per-color guess. */
    fun luminance(color: Color): Double {
        fun channel(c: Float): Double {
            val s = c.toDouble()
            return if (s <= 0.03928) s / 12.92 else ((s + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * channel(color.red) + 0.7152 * channel(color.green) + 0.0722 * channel(color.blue)
    }

    /** Simple decisive threshold rather than full WCAG contrast-ratio math against both candidates — for a single swatch this reduces to "closer to white or black," which luminance alone answers. */
    fun textColorFor(background: Color): Color = if (luminance(background) > 0.42) Color.Black else Color.White

    /** Lightens (positive) or darkens (negative) a color in HSV space. [amount] is -1f..1f, used by the picker's slider. */
    fun adjustLightness(color: Color, amount: Float): Color {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(color.toArgb(), hsv)
        hsv[2] = (hsv[2] + amount).coerceIn(0.08f, 1f) // never fully black or blown-out white
        return Color(android.graphics.Color.HSVToColor(hsv)).copy(alpha = color.alpha)
    }

    fun nearestPreset(color: Color): Color = PRESET_SWATCHES.minByOrNull { preset ->
        val dr = (preset.red - color.red)
        val dg = (preset.green - color.green)
        val db = (preset.blue - color.blue)
        dr * dr + dg * dg + db * db
    } ?: PRESET_SWATCHES.first()

    fun lightnessDeltaFrom(base: Color, actual: Color): Float {
        val hsvBase = FloatArray(3); android.graphics.Color.colorToHSV(base.toArgb(), hsvBase)
        val hsvActual = FloatArray(3); android.graphics.Color.colorToHSV(actual.toArgb(), hsvActual)
        return (hsvActual[2] - hsvBase[2]).coerceIn(-1f, 1f)
    }
}
