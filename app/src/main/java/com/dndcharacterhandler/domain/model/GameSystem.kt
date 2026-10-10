package com.dndcharacterhandler.domain.model

/** A tabletop game; its editions are [GameSystem]s. [localizationKey] is the game's name. */
enum class GameSystemFamily(val localizationKey: String) {
    DND("game_family_dnd"),
    PATHFINDER("game_family_pathfinder"),
    STARFINDER("game_family_starfinder"),
    VAMPIRE("game_family_vampire")
}

/**
 * The game and edition a character is made in, with its rules, catalogs and sheet (owner's wish, 2026-10-10): a
 * character belongs to one and exists in no other, and the drawer lists the picked one's characters.
 *
 * [key] is stored with the character, in its archive and in the settings: never change it. [editionKey] names the
 * edition («5-я редакция (2024)»). Only an [available] system has rules and a sheet yet; the others can be picked,
 * their list stays empty and says they are coming (docs/GAME_SYSTEMS.md).
 */
enum class GameSystem(
    val key: String,
    val family: GameSystemFamily,
    val editionKey: String,
    val available: Boolean
) {
    DND_5E_2024("dnd5e_2024", GameSystemFamily.DND, "game_edition_dnd5e_2024", available = true),
    DND_5E_2014("dnd5e_2014", GameSystemFamily.DND, "game_edition_dnd5e_2014", available = false),
    PATHFINDER_2E("pf2e", GameSystemFamily.PATHFINDER, "game_edition_pf2e", available = false),
    PATHFINDER_1E("pf1e", GameSystemFamily.PATHFINDER, "game_edition_pf1e", available = false),
    STARFINDER_2E("sf2e", GameSystemFamily.STARFINDER, "game_edition_sf2e", available = false),
    STARFINDER_1E("sf1e", GameSystemFamily.STARFINDER, "game_edition_sf1e", available = false),
    VAMPIRE_V5("vtm5", GameSystemFamily.VAMPIRE, "game_edition_vtm5", available = false);

    /** The system's own files in the app's assets (its catalogs); what every system shares lies outside. */
    val assetsRoot: String get() = "systems/$key"

    companion object {
        /** Every character made before the systems, and the pick until the player makes another. */
        val DEFAULT = DND_5E_2024

        fun fromKey(key: String?): GameSystem? = entries.firstOrNull { it.key == key }
    }
}
