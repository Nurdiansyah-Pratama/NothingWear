package com.nothingwear.app.domain.model

/**
 * Supported visual themes. Dark mode is the project default per Master Project Plan Section 5.
 */
enum class ThemeMode {
    DARK,
    LIGHT,
    SYSTEM,
}

/**
 * Curated accent options for the Nothing-inspired design system.
 */
enum class AccentColorOption(
    val id: String,
    val displayName: String,
) {
    SIGNAL_RED(id = "signal_red", displayName = "Signal Red"),
    MONOCHROME(id = "monochrome", displayName = "Monochrome"),
    AMBER_PULSE(id = "amber_pulse", displayName = "Amber Pulse"),
    CYBER_CYAN(id = "cyber_cyan", displayName = "Cyber Cyan"),
    EMERALD_SIGNAL(id = "emerald_signal", displayName = "Emerald"),
    COBALT(id = "cobalt", displayName = "Cobalt"),
    ;

    companion object {
        fun fromId(id: String?): AccentColorOption =
            entries.firstOrNull { it.id == id } ?: SIGNAL_RED
    }
}

data class AppThemePreferences(
    val themeMode: ThemeMode = ThemeMode.DARK,
    val accentColor: AccentColorOption = AccentColorOption.SIGNAL_RED,
)
