package com.dndcharacterhandler.domain.model

/**
 * The app's look, picked in the settings (owner's choice, 2026-10-05): the same screens drawn in another
 * palette and style. [key] names the theme in design_tokens.json (`themes.<key>`) and in the settings store.
 */
enum class AppTheme(val key: String, val localizationKey: String) {
    CLASSIC("classic", "settings_theme_classic"),
    ENGRAVED("engraved", "settings_theme_engraved")
}
