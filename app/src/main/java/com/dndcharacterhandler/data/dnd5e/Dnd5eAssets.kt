package com.dndcharacterhandler.data.dnd5e

import com.dndcharacterhandler.domain.model.GameSystem

/**
 * Where D&D 5e (2024)'s own files lie in the app's assets: under its system's folder, apart from what every system
 * shares (docs/GAME_SYSTEMS.md, stage 3).
 */
object Dnd5eAssets {
    private val root = GameSystem.DND_5E_2024.assetsRoot

    /** Classes, subclasses, species, backgrounds, feats and spells; tools/foundry_catalog/convert.py builds it. */
    val characterCatalog = "$root/character_catalog.json"

    /** The inventory's items in Russian: names and descriptions by the item's id. */
    val inventoryTextRu = "$root/inventory_text_ru.json"

    /** The SRD 5.2 equipment, from the 5e-database submodule; app/build.gradle.kts copies it in at build time. */
    val srdEquipment = "$root/srd/5e-SRD-Equipment.json"

    /** The SRD 5.2 magic items, copied in as [srdEquipment]. */
    val srdMagicItems = "$root/srd/5e-SRD-Magic-Items.json"
}
