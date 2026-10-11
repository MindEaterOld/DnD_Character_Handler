package com.dndcharacterhandler.domain.model

/**
 * The app's look, picked in the settings (owner's choice, 2026-10-05): the same screens in another palette. [key] names
 * the theme in design_tokens.json (`themes.<key>`) and in the settings store. The strict palettes the owner kept from
 * the phone (2026-10-11): Graphite, the default, and Plum — one ladder of ten neutrals each, the gold and the accents
 * shared. Classic and Engraving are gone: a stored key the list lacks reads as [DEFAULT].
 */
enum class AppTheme(val key: String, val localizationKey: String) {
    GRAPHITE("graphite", "settings_theme_graphite"),
    PLUM("plum", "settings_theme_plum");

    companion object {
        /** The look until the player picks another, and what a stored key the list lacks reads as. */
        val DEFAULT = GRAPHITE
    }
}
