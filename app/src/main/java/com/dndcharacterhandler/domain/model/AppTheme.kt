package com.dndcharacterhandler.domain.model

/**
 * The app's look, picked in the settings (owner's choice, 2026-10-05): the same screens in another palette. [key] names
 * the theme in design_tokens.json (`themes.<key>`) and in the settings store. Besides Classic, the strict palettes the
 * owner tries on the phone to pick one (2026-10-11): one ladder of ten neutrals each, the gold and the accents the
 * same; Engraving is gone (a stored "engraved" reads as Classic).
 */
enum class AppTheme(val key: String, val localizationKey: String) {
    CLASSIC("classic", "settings_theme_classic"),
    SEPIA("sepia", "settings_theme_sepia"),
    GRAPHITE("graphite", "settings_theme_graphite"),
    MIDNIGHT("midnight", "settings_theme_midnight"),
    WINE("wine", "settings_theme_wine"),
    ASH("ash", "settings_theme_ash"),
    UMBER("umber", "settings_theme_umber"),
    BRONZE("bronze", "settings_theme_bronze"),
    OLIVE("olive", "settings_theme_olive"),
    MOSS("moss", "settings_theme_moss"),
    DEEP_SEA("teal", "settings_theme_teal"),
    STEEL("steel", "settings_theme_steel"),
    SAPPHIRE("sapphire", "settings_theme_sapphire"),
    INDIGO("indigo", "settings_theme_indigo"),
    PLUM("plum", "settings_theme_plum")
}
