package com.dndcharacterhandler.domain.dnd5e.rules

import com.dndcharacterhandler.domain.dnd5e.model.Character
import com.dndcharacterhandler.domain.dnd5e.model.CharacterCatalog
import com.dndcharacterhandler.domain.dnd5e.model.CustomProficiencyPrefix
import com.dndcharacterhandler.domain.dnd5e.model.decodeProficiencyIds
import com.dndcharacterhandler.domain.dnd5e.model.encodeProficiencyIds
import com.dndcharacterhandler.domain.dnd5e.model.knownProficiencyIds
import com.dndcharacterhandler.domain.dnd5e.model.proficiencyIdForTrait
import com.dndcharacterhandler.domain.dnd5e.model.weaponIdForFoundry
import com.dndcharacterhandler.domain.dnd5e.model.withoutCoveredWeapons

/** How Character Wizard began the masteries it wrote into the weapon field. */
private val MasteryPrefixes = listOf("Мастерство:", "Mastery:")

/**
 * [character] with the proficiencies Character Wizard once wrote as names ("Простое оружие,
 * Воинское оружие, Мастерство: Длинный меч, Боевой топор") turned into the fields' ids: the sheet's
 * options, custom entries for the rest, and the masteries into their own field. Null when there is
 * nothing to turn.
 */
fun repairWrittenProficiencies(character: Character, catalog: CharacterCatalog): Character? {
    var masteries = decodeProficiencyIds(character.weaponMasteries)

    fun keyNamed(name: String, kind: String): String? = catalog.traits.entries.firstOrNull { (key, trait) ->
        key.startsWith("$kind:") && listOf(trait.name.en, trait.name.ru).any { it.isNotBlank() && it.equals(name, ignoreCase = true) }
    }?.key

    fun repair(field: String, kind: String): String {
        val ids = decodeProficiencyIds(field)
        val written = ids.filter { it !in knownProficiencyIds && !it.startsWith(CustomProficiencyPrefix) }
        if (written.isEmpty()) return field
        val repaired = (ids - written.toSet()).toMutableSet()
        var inMasteries = false
        written.flatMap { it.split(",") }.map(String::trim).filter(String::isNotEmpty).forEach { piece ->
            var name = piece
            MasteryPrefixes.firstOrNull { name.startsWith(it) }?.let { prefix ->
                inMasteries = true
                name = name.removePrefix(prefix).trim()
            }
            val key = keyNamed(name, kind)
            // After "Мастерство:" the old wizard went on writing the next level's proficiencies on the
            // same line: only a weapon that has a mastery property is a mastery, a group ("Воинское
            // оружие") or anything else stays a proficiency.
            val masteryWeapon = key?.substringAfterLast(':')?.let { weaponIdForFoundry(it) ?: it }
                ?.takeIf { inMasteries && catalog.masteryOf(it) != null }
            when {
                name in knownProficiencyIds || name.startsWith(CustomProficiencyPrefix) -> repaired += name
                masteryWeapon != null -> masteries = masteries + masteryWeapon
                else -> repaired += key?.let(::proficiencyIdForTrait) ?: (CustomProficiencyPrefix + name)
            }
        }
        return encodeProficiencyIds(if (kind == "weapon") withoutCoveredWeapons(repaired) else repaired)
    }

    val updated = character.copy(
        armorProficiencies = repair(character.armorProficiencies, "armor"),
        weaponProficiencies = repair(character.weaponProficiencies, "weapon"),
        toolProficiencies = repair(character.toolProficiencies, "tool"),
        languageProficiencies = repair(character.languageProficiencies, "languages")
    ).let { it.copy(weaponMasteries = if (masteries == decodeProficiencyIds(character.weaponMasteries)) character.weaponMasteries else encodeProficiencyIds(masteries)) }
    return updated.takeIf { it != character }
}
