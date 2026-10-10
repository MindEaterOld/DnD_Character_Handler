package com.dndcharacterhandler.domain.dnd5e.model

/*
 * The character's proficiency fields (armor, weapons, tools, languages) and weapon masteries hold
 * ids joined by "|": the options below ("simple_weapons", "elvish", "smiths_tools"), or
 * "custom:<name>" for anything else (a Faerûn language, a pistol). The Attributes screen edits
 * them; Character Wizard writes what Foundry's traits give in the same ids.
 */

const val CustomProficiencyPrefix = "custom:"
const val WeaponGroupSimpleId = "simple_weapons"
const val WeaponGroupMartialId = "martial_weapons"

fun encodeProficiencyIds(ids: Set<String>): String = ids.toList().sorted().joinToString("|")

fun decodeProficiencyIds(value: String): Set<String> =
    value.split("|").mapNotNull { it.trim().takeIf(String::isNotEmpty) }.toSet()

/** Every option id of the four fields, for telling ids from old free text. */
val knownProficiencyIds: Set<String> by lazy {
    (armorProficiencyOptions + simpleWeaponOptions + martialWeaponOptions +
        toolProficiencyCategories.flatMap { it.options } + languageProficiencyCategories.flatMap { it.options })
        .map { it.id }.toSet() + setOf(WeaponGroupSimpleId, WeaponGroupMartialId)
}

/** The sheet's weapon id for Foundry's ("lighthammer" -> "light_hammer"), or null when the sheet has no such option. */
fun weaponIdForFoundry(foundryId: String): String? =
    (simpleWeaponOptions + martialWeaponOptions).firstOrNull { it.id.replace("_", "") == foundryId.lowercase() }?.id

/** Foundry's weapon id for the sheet's ("light_hammer" -> "lighthammer"). */
fun foundryWeaponId(weaponId: String): String = weaponId.replace("_", "").replace("-", "")

/**
 * The field id for a Foundry trait key ("weapon:mar" -> "martial_weapons", "languages:standard:elvish"
 * -> "elvish", "tool:art:smith" -> "smiths_tools"), or null when the sheet has no option for it.
 */
fun proficiencyIdForTrait(key: String): String? {
    val parts = key.split(':')
    return when (parts.first()) {
        "armor" -> ARMOR_IDS[parts.take(2).joinToString(":")]?.takeIf { parts.size <= 2 || parts[1] == "shl" }
        "weapon" -> when {
            parts.size == 2 -> WEAPON_GROUP_IDS[key]
            parts.size == 3 -> weaponIdForFoundry(parts[2])
            else -> null
        }
        "tool" -> TOOL_IDS[key]
        // "languages:standard:elvish", "languages:exotic:primordial"; dialects and Faerûn's are custom.
        "languages" -> parts.takeIf { it.size == 3 }?.let { LANGUAGE_IDS[it[2]] ?: it[2] }
            ?.takeIf { id -> languageProficiencyCategories.any { category -> category.options.any { it.id == id } } }
        else -> null
    }
}

/** The group a Foundry trait key belongs to on the sheet: Martial weapons for a longsword, Light armor for leather. */
fun proficiencyGroupForTrait(key: String): String? {
    val parts = key.split(':')
    if (parts.size < 3) return null
    return when (parts.first()) {
        "weapon" -> WEAPON_GROUP_IDS["weapon:${parts[1]}"]
        "armor" -> ARMOR_IDS["armor:${parts[1]}"]
        else -> null
    }
}

/** A field with a weapon group drops the group's own weapons, as the sheet does. */
fun withoutCoveredWeapons(ids: Set<String>): Set<String> {
    var result = ids
    if (WeaponGroupSimpleId in ids) result = result - simpleWeaponOptions.map { it.id }.toSet()
    if (WeaponGroupMartialId in ids) result = result - martialWeaponOptions.map { it.id }.toSet()
    return result
}

private val ARMOR_IDS = mapOf(
    "armor:lgt" to "light_armor",
    "armor:med" to "medium_armor",
    "armor:hvy" to "heavy_armor",
    "armor:shl" to "shields"
)

private val WEAPON_GROUP_IDS = mapOf("weapon:sim" to WeaponGroupSimpleId, "weapon:mar" to WeaponGroupMartialId)

private val TOOL_IDS = mapOf(
    "tool:art:alchemist" to "alchemists_supplies",
    "tool:art:brewer" to "brewers_supplies",
    "tool:art:calligrapher" to "calligraphers_supplies",
    "tool:art:carpenter" to "carpenters_tools",
    "tool:art:cartographer" to "cartographers_tools",
    "tool:art:cobbler" to "cobblers_tools",
    "tool:art:cook" to "cooks_utensils",
    "tool:art:glassblower" to "glassblowers_tools",
    "tool:art:jeweler" to "jewelers_tools",
    "tool:art:leatherworker" to "leatherworkers_tools",
    "tool:art:mason" to "masons_tools",
    "tool:art:painter" to "painters_supplies",
    "tool:art:potter" to "potters_tools",
    "tool:art:smith" to "smiths_tools",
    "tool:art:tinker" to "tinkers_tools",
    "tool:art:weaver" to "weavers_tools",
    "tool:art:woodcarver" to "woodcarvers_tools",
    "tool:game:dice" to "dice_set",
    "tool:game:chess" to "dragonchess_set",
    "tool:game:card" to "playing_card_set",
    "tool:game:ante" to "three_dragon_ante_set",
    "tool:music:bagpipes" to "bagpipes",
    "tool:music:drum" to "drum",
    "tool:music:dulcimer" to "dulcimer",
    "tool:music:flute" to "flute",
    "tool:music:lute" to "lute",
    "tool:music:lyre" to "lyre",
    "tool:music:horn" to "horn",
    "tool:music:panflute" to "pan_flute",
    "tool:music:shawm" to "shawm",
    "tool:music:viol" to "viol",
    "tool:disg" to "disguise_kit",
    "tool:forg" to "forgery_kit",
    "tool:herb" to "herbalism_kit",
    "tool:navg" to "navigators_tools",
    "tool:pois" to "poisoners_kit",
    "tool:thief" to "thieves_tools",
    "tool:vehicle:land" to "land_vehicles",
    "tool:vehicle:water" to "water_vehicles"
)

/** Foundry language codes the sheet spells otherwise. */
private val LANGUAGE_IDS = mapOf("deep" to "deep_speech", "cant" to "thieves_cant")

/** An option of a proficiency field: its id in the field and its label's localization key. */
data class ProficiencyOption(val id: String, val labelKey: String)

data class ProficiencyCategory(
    val id: String,
    val labelKey: String,
    val options: List<ProficiencyOption>
)


val armorProficiencyOptions = listOf(
    ProficiencyOption("light_armor", "Light Armor"),
    ProficiencyOption("medium_armor", "Medium Armor"),
    ProficiencyOption("heavy_armor", "Heavy Armor"),
    ProficiencyOption("shields", "Shields")
)


val simpleWeaponOptions = listOf(
    ProficiencyOption("club", "Club"),
    ProficiencyOption("dagger", "Dagger"),
    ProficiencyOption("greatclub", "Greatclub"),
    ProficiencyOption("handaxe", "Handaxe"),
    ProficiencyOption("javelin", "Javelin"),
    ProficiencyOption("light_hammer", "Light Hammer"),
    ProficiencyOption("mace", "Mace"),
    ProficiencyOption("quarterstaff", "Quarterstaff"),
    ProficiencyOption("sickle", "Sickle"),
    ProficiencyOption("spear", "Spear"),
    ProficiencyOption("light_crossbow", "Light Crossbow"),
    ProficiencyOption("dart", "Dart"),
    ProficiencyOption("shortbow", "Shortbow"),
    ProficiencyOption("sling", "Sling")
)

val martialWeaponOptions = listOf(
    ProficiencyOption("battleaxe", "Battleaxe"),
    ProficiencyOption("flail", "Flail"),
    ProficiencyOption("glaive", "Glaive"),
    ProficiencyOption("greataxe", "Greataxe"),
    ProficiencyOption("greatsword", "Greatsword"),
    ProficiencyOption("halberd", "Halberd"),
    ProficiencyOption("lance", "Lance"),
    ProficiencyOption("longsword", "Longsword"),
    ProficiencyOption("maul", "Maul"),
    ProficiencyOption("morningstar", "Morningstar"),
    ProficiencyOption("musket", "Musket"),
    ProficiencyOption("pike", "Pike"),
    ProficiencyOption("rapier", "Rapier"),
    ProficiencyOption("scimitar", "Scimitar"),
    ProficiencyOption("shortsword", "Shortsword"),
    ProficiencyOption("trident", "Trident"),
    ProficiencyOption("war_pick", "War Pick"),
    ProficiencyOption("warhammer", "Warhammer"),
    ProficiencyOption("whip", "Whip"),
    ProficiencyOption("blowgun", "Blowgun"),
    ProficiencyOption("hand_crossbow", "Hand Crossbow"),
    ProficiencyOption("heavy_crossbow", "Heavy Crossbow"),
    ProficiencyOption("longbow", "Longbow"),
    ProficiencyOption("net", "Net")
)

val toolProficiencyCategories = listOf(
    ProficiencyCategory(
        id = "artisans_tools",
        labelKey = "Artisan's Tools",
        options = listOf(
            ProficiencyOption("alchemists_supplies", "Alchemist's Supplies"),
            ProficiencyOption("brewers_supplies", "Brewer's Supplies"),
            ProficiencyOption("calligraphers_supplies", "Calligrapher's Supplies"),
            ProficiencyOption("carpenters_tools", "Carpenter's Tools"),
            ProficiencyOption("cartographers_tools", "Cartographer's Tools"),
            ProficiencyOption("cobblers_tools", "Cobbler's Tools"),
            ProficiencyOption("cooks_utensils", "Cook's Utensils"),
            ProficiencyOption("glassblowers_tools", "Glassblower's Tools"),
            ProficiencyOption("jewelers_tools", "Jeweler's Tools"),
            ProficiencyOption("leatherworkers_tools", "Leatherworker's Tools"),
            ProficiencyOption("masons_tools", "Mason's Tools"),
            ProficiencyOption("painters_supplies", "Painter's Supplies"),
            ProficiencyOption("potters_tools", "Potter's Tools"),
            ProficiencyOption("smiths_tools", "Smith's Tools"),
            ProficiencyOption("tinkers_tools", "Tinker's Tools"),
            ProficiencyOption("weavers_tools", "Weaver's Tools"),
            ProficiencyOption("woodcarvers_tools", "Woodcarver's Tools")
        )
    ),
    ProficiencyCategory(
        id = "gaming_sets",
        labelKey = "Gaming Sets",
        options = listOf(
            ProficiencyOption("dice_set", "Dice Set"),
            ProficiencyOption("dragonchess_set", "Dragonchess Set"),
            ProficiencyOption("playing_card_set", "Playing Card Set"),
            ProficiencyOption("three_dragon_ante_set", "Three-Dragon Ante Set")
        )
    ),
    ProficiencyCategory(
        id = "musical_instruments",
        labelKey = "Musical Instruments",
        options = listOf(
            ProficiencyOption("bagpipes", "Bagpipes"),
            ProficiencyOption("drum", "Drum"),
            ProficiencyOption("dulcimer", "Dulcimer"),
            ProficiencyOption("flute", "Flute"),
            ProficiencyOption("lute", "Lute"),
            ProficiencyOption("lyre", "Lyre"),
            ProficiencyOption("horn", "Horn"),
            ProficiencyOption("pan_flute", "Pan Flute"),
            ProficiencyOption("shawm", "Shawm"),
            ProficiencyOption("viol", "Viol")
        )
    ),
    ProficiencyCategory(
        id = "other_tools",
        labelKey = "Other Tools",
        options = listOf(
            ProficiencyOption("disguise_kit", "Disguise Kit"),
            ProficiencyOption("forgery_kit", "Forgery Kit"),
            ProficiencyOption("herbalism_kit", "Herbalism Kit"),
            ProficiencyOption("navigators_tools", "Navigator's Tools"),
            ProficiencyOption("poisoners_kit", "Poisoner's Kit"),
            ProficiencyOption("thieves_tools", "Thieves' Tools")
        )
    ),
    ProficiencyCategory(
        id = "vehicles",
        labelKey = "Vehicles",
        options = listOf(
            ProficiencyOption("land_vehicles", "Land Vehicles"),
            ProficiencyOption("water_vehicles", "Water Vehicles")
        )
    )
)

val languageProficiencyCategories = listOf(
    ProficiencyCategory(
        id = "standard_languages",
        labelKey = "Standard Languages",
        options = listOf(
            ProficiencyOption("common", "Common"),
            ProficiencyOption("dwarvish", "Dwarvish"),
            ProficiencyOption("elvish", "Elvish"),
            ProficiencyOption("giant", "Giant"),
            ProficiencyOption("gnomish", "Gnomish"),
            ProficiencyOption("goblin", "Goblin"),
            ProficiencyOption("halfling", "Halfling"),
            ProficiencyOption("orc", "Orc")
        )
    ),
    ProficiencyCategory(
        id = "exotic_languages",
        labelKey = "Exotic Languages",
        options = listOf(
            ProficiencyOption("abyssal", "Abyssal"),
            ProficiencyOption("celestial", "Celestial"),
            ProficiencyOption("draconic", "Draconic"),
            ProficiencyOption("deep_speech", "Deep Speech"),
            ProficiencyOption("infernal", "Infernal"),
            ProficiencyOption("primordial", "Primordial"),
            ProficiencyOption("sylvan", "Sylvan"),
            ProficiencyOption("undercommon", "Undercommon")
        )
    ),
    ProficiencyCategory(
        id = "special_languages",
        labelKey = "Special Languages",
        options = listOf(
            ProficiencyOption("druidic", "Druidic"),
            ProficiencyOption("thieves_cant", "Thieves' Cant"),
            ProficiencyOption("telepathy", "Telepathy")
        )
    )
)
