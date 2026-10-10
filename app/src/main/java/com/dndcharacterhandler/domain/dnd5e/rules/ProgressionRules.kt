package com.dndcharacterhandler.domain.dnd5e.rules

import com.dndcharacterhandler.domain.dnd5e.model.AdvancementStep
import com.dndcharacterhandler.domain.dnd5e.model.Character
import com.dndcharacterhandler.domain.dnd5e.model.CharacterCatalog
import com.dndcharacterhandler.domain.dnd5e.model.CharacterClassEntry
import kotlin.math.ceil

/** Experience needed for each level, PHB 2024 (index 0 is level 1). */
val ExperienceThresholds: List<Int> = listOf(
    0, 300, 900, 2700, 6500, 14000, 23000, 34000, 48000, 64000,
    85000, 100000, 120000, 140000, 165000, 195000, 225000, 265000, 305000, 355000
)

const val MAX_CHARACTER_LEVEL = 20

/**
 * How far a character of [level] with [experience] points has come toward the next level, 0 to 1: from its own level's
 * threshold to the next one's; 1 at the last level.
 */
fun experienceProgress(level: Int, experience: Int): Float {
    if (level >= MAX_CHARACTER_LEVEL) return 1f
    val from = ExperienceThresholds[(level - 1).coerceIn(0, MAX_CHARACTER_LEVEL - 1)]
    val to = ExperienceThresholds[level.coerceIn(1, MAX_CHARACTER_LEVEL - 1)]
    return ((experience - from).toFloat() / (to - from).coerceAtLeast(1)).coerceIn(0f, 1f)
}

/** The level [experience] points are enough for. */
fun levelForExperience(experience: Int): Int =
    ExperienceThresholds.indexOfLast { experience >= it }.coerceAtLeast(0) + 1

/**
 * The level the wizard opens with when the player taps the class: the current level for a character
 * whose class is still only text (the wizard sets it up), the next level otherwise; null at level 20.
 */
fun classWizardTarget(character: Character): Int? = when {
    character.classes.isEmpty() -> character.level.coerceIn(1, MAX_CHARACTER_LEVEL)
    character.level < MAX_CHARACTER_LEVEL -> character.level + 1
    else -> null
}

/**
 * Hit dice of one size: how many the character has and how many are spent, and the classes they come
 * from ([classIds], catalog ids; none for a character whose class the catalog doesn't know).
 */
data class HitDicePool(val sides: Int, val total: Int, val spent: Int, val classIds: List<String> = emptyList()) {
    val available: Int get() = (total - spent).coerceAtLeast(0)
}

/** Hit dice by size, largest first: a Fighter 5 / Wizard 2 has five d10 and two d6. */
fun hitDicePools(classes: List<CharacterClassEntry>, catalog: CharacterCatalog): List<HitDicePool> =
    classes.groupBy { entry -> classHitDie(entry, catalog) }
        .map { (sides, entries) ->
            val total = entries.sumOf { it.levels }
            HitDicePool(sides, total, entries.sumOf { it.spentHitDice }.coerceAtMost(total), entries.map { it.classId })
        }
        .sortedByDescending { it.sides }

/** The hit die of a class: its catalog's, a d8 for a class the catalog doesn't have. */
internal fun classHitDie(entry: CharacterClassEntry, catalog: CharacterCatalog): Int =
    catalog.classes.firstOrNull { it.id == entry.classId }?.hitDie ?: 8

/** Spell slots by spell level 1..9 (index 0 is 1st level), with Pact Magic counted in at its slot level. */
data class SpellSlotTable(val slots: List<Int>, val pactSlots: Int, val pactSlotLevel: Int) {
    val isEmpty: Boolean get() = slots.all { it == 0 } && pactSlots == 0

    /** The slots as the app stores them: Pact Magic slots added to their level. */
    fun combined(): List<Int> = slots.mapIndexed { index, count ->
        count + if (pactSlots > 0 && index == pactSlotLevel - 1) pactSlots else 0
    }
}

/**
 * Spell slots for the character's classes, as Foundry and the PHB multiclass rules count them: the
 * caster levels of full, half (rounded up) and third (rounded down) casters add up on the full
 * caster table — a single caster class always rounds up — and Pact Magic stays separate.
 */
fun spellSlots(classes: List<CharacterClassEntry>, catalog: CharacterCatalog): SpellSlotTable {
    val casters = classes.mapNotNull { entry ->
        val characterClass = catalog.classes.firstOrNull { it.id == entry.classId } ?: return@mapNotNull null
        val subclass = entry.subclassId?.let { id -> catalog.subclasses.firstOrNull { it.id == id } }
        val progression = characterClass.spellcasting?.progression ?: subclass?.spellcasting?.progression
            ?: return@mapNotNull null
        progression to entry.levels
    }
    val leveled = casters.filter { it.first != "pact" }
    val singleCaster = leveled.size == 1
    val casterLevel = leveled.sumOf { (progression, levels) ->
        when (progression) {
            "full" -> levels
            "half", "artificer" -> ceil(levels / 2.0).toInt()
            "third" -> if (singleCaster) ceil(levels / 3.0).toInt() else levels / 3
            else -> 0
        }
    }.coerceAtMost(MAX_CHARACTER_LEVEL)
    val slots = if (casterLevel > 0) FULL_CASTER_SLOTS[casterLevel - 1] else List(9) { 0 }
    val pactLevels = casters.filter { it.first == "pact" }.sumOf { it.second }.coerceAtMost(MAX_CHARACTER_LEVEL)
    val (pactSlots, pactSlotLevel) = if (pactLevels > 0) PACT_SLOTS[pactLevels - 1] else (0 to 0)
    return SpellSlotTable(slots, pactSlots, pactSlotLevel)
}

/**
 * How many spells the character may prepare: each class's (or spellcasting subclass's)
 * "prepared-spells" scale at its level, added up; null when none of its classes casts spells.
 */
fun preparedSpellLimit(classes: List<CharacterClassEntry>, catalog: CharacterCatalog): Int? {
    val limits = classes.mapNotNull { entry ->
        val characterClass = catalog.classes.firstOrNull { it.id == entry.classId }
        val subclass = entry.subclassId?.let { id -> catalog.subclasses.firstOrNull { it.id == id } }
        (characterClass?.advancement.orEmpty() + subclass?.advancement.orEmpty())
            .filterIsInstance<AdvancementStep.ScaleValue>()
            .firstOrNull { it.identifier == "prepared-spells" }
            ?.valueAt(entry.levels)?.trim()?.toIntOrNull()
    }
    return limits.takeIf { it.isNotEmpty() }?.sum()
}

/** One class's multiclass requirement: 13+ in all (or any one) of its primary abilities. */
data class MulticlassRequirement(
    val classId: String,
    val abilities: List<String>,
    val needsAll: Boolean,
    val isMet: Boolean
)

/**
 * The multiclass requirements for adding [newClassId]: the new class and every class the character
 * already has. The wizard only warns about unmet ones, as Foundry does.
 */
fun multiclassRequirements(
    character: Character,
    classes: List<CharacterClassEntry>,
    newClassId: String,
    catalog: CharacterCatalog,
    abilityScores: Map<String, Int> = character.abilityScores()
): List<MulticlassRequirement> =
    (classes.map { it.classId } + newClassId).distinct().mapNotNull { classId ->
        val characterClass = catalog.classes.firstOrNull { it.id == classId } ?: return@mapNotNull null
        val abilities = characterClass.primaryAbilities
        if (abilities.isEmpty()) return@mapNotNull null
        val passes = abilities.map { (abilityScores[it] ?: 10) >= 13 }
        MulticlassRequirement(
            classId = classId,
            abilities = abilities,
            needsAll = characterClass.primaryAbilitiesAll,
            isMet = if (characterClass.primaryAbilitiesAll) passes.all { it } else passes.any { it }
        )
    }

/** Ability scores by Foundry key ("str", "dex"...). */
fun Character.abilityScores(): Map<String, Int> = mapOf(
    "str" to strength, "dex" to dexterity, "con" to constitution,
    "int" to intelligence, "wis" to wisdom, "cha" to charisma
)

/**
 * How the character's classes read on the sheet: "Battle Master Fighter" for one class, and each
 * class with its levels for several ("Fighter 5 / Rogue 2").
 */
fun classLabel(classes: List<CharacterClassEntry>, catalog: CharacterCatalog, russian: Boolean): String {
    val named = classes.mapNotNull { entry ->
        val name = catalog.classes.firstOrNull { it.id == entry.classId }?.name?.get(russian) ?: return@mapNotNull null
        val subclass = entry.subclassId?.let { id -> catalog.subclasses.firstOrNull { it.id == id }?.name?.get(russian) }
        Triple(entry, name, subclass)
    }
    return when (named.size) {
        0 -> ""
        1 -> named.single().let { (_, name, subclass) -> if (subclass != null) "$subclass $name" else name }
        else -> named.sortedByDescending { it.first.levels }.joinToString(" / ") { (entry, name, _) -> "$name ${entry.levels}" }
    }
}

/** Slots of a full caster by caster level (PHB), spell levels 1..9. */
private val FULL_CASTER_SLOTS: List<List<Int>> = listOf(
    listOf(2, 0, 0, 0, 0, 0, 0, 0, 0),
    listOf(3, 0, 0, 0, 0, 0, 0, 0, 0),
    listOf(4, 2, 0, 0, 0, 0, 0, 0, 0),
    listOf(4, 3, 0, 0, 0, 0, 0, 0, 0),
    listOf(4, 3, 2, 0, 0, 0, 0, 0, 0),
    listOf(4, 3, 3, 0, 0, 0, 0, 0, 0),
    listOf(4, 3, 3, 1, 0, 0, 0, 0, 0),
    listOf(4, 3, 3, 2, 0, 0, 0, 0, 0),
    listOf(4, 3, 3, 3, 1, 0, 0, 0, 0),
    listOf(4, 3, 3, 3, 2, 0, 0, 0, 0),
    listOf(4, 3, 3, 3, 2, 1, 0, 0, 0),
    listOf(4, 3, 3, 3, 2, 1, 0, 0, 0),
    listOf(4, 3, 3, 3, 2, 1, 1, 0, 0),
    listOf(4, 3, 3, 3, 2, 1, 1, 0, 0),
    listOf(4, 3, 3, 3, 2, 1, 1, 1, 0),
    listOf(4, 3, 3, 3, 2, 1, 1, 1, 0),
    listOf(4, 3, 3, 3, 2, 1, 1, 1, 1),
    listOf(4, 3, 3, 3, 3, 1, 1, 1, 1),
    listOf(4, 3, 3, 3, 3, 2, 1, 1, 1),
    listOf(4, 3, 3, 3, 3, 2, 2, 1, 1)
)

/** Pact Magic by Warlock level: number of slots and their level. */
private val PACT_SLOTS: List<Pair<Int, Int>> = listOf(
    1 to 1, 2 to 1, 2 to 2, 2 to 2, 2 to 3, 2 to 3, 2 to 4, 2 to 4, 2 to 5, 2 to 5,
    3 to 5, 3 to 5, 3 to 5, 3 to 5, 3 to 5, 3 to 5, 4 to 5, 4 to 5, 4 to 5, 4 to 5
)
