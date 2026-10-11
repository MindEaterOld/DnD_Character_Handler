package com.dndcharacterhandler.domain.model

/**
 * The app's look, picked in the settings (owner's choice, 2026-10-05): the same screens in another palette. [key] names
 * the theme in design_tokens.json (`themes.<key>`) and in the settings store. The strict palettes the owner kept from
 * the phone (2026-10-11): Graphite, the default, and Plum — one ladder of ten neutrals each, the gold and the accents
 * shared. The GRAPHITE_* themes try accent sets narrowed to six on Graphite (2026-10-11). Classic and Engraving are gone: a stored key the list lacks reads as [DEFAULT].
 */
enum class AppTheme(val key: String, val localizationKey: String) {
    GRAPHITE("graphite", "settings_theme_graphite"),
    GRAPHITE_A("graphite_a", "settings_theme_graphite_a"),
    GRAPHITE_B("graphite_b", "settings_theme_graphite_b"),
    GRAPHITE_C("graphite_c", "settings_theme_graphite_c"),
    GRAPHITE_D("graphite_d", "settings_theme_graphite_d"),
    GRAPHITE_E("graphite_e", "settings_theme_graphite_e"),
    PLUM("plum", "settings_theme_plum");

    companion object {
        /** The look until the player picks another, and what a stored key the list lacks reads as. */
        val DEFAULT = GRAPHITE
    }
}
