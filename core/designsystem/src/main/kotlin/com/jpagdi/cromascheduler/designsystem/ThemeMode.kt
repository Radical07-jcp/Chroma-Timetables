package com.jpagdi.cromascheduler.designsystem

/**
 * LIGHT / DARK / BLACK. Black is a distinct mode from Dark — true near-black surfaces for
 * OLED screens — not a synonym for Dark, per the explicit "add themes (light/dark/black)"
 * request this was built for.
 */
enum class ThemeMode {
    LIGHT,
    DARK,
    BLACK,
    ;

    companion object {
        fun fromName(name: String?): ThemeMode = entries.firstOrNull { it.name == name } ?: LIGHT
    }
}
