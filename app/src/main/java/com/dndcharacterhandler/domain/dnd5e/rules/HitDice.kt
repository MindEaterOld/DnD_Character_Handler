package com.dndcharacterhandler.domain.dnd5e.rules

import com.dndcharacterhandler.domain.dnd5e.model.Character
import com.dndcharacterhandler.domain.dnd5e.model.CharacterCatalog
import com.dndcharacterhandler.domain.dnd5e.model.CharacterClassEntry
import kotlin.math.max

/**
 * The character's hit dice by size, largest first. A character whose classes are only text (no
 * Character Wizard, or no catalog yet) has one pool: as many [Character.hitDieSides] as its level.
 */
fun hitDicePools(character: Character, catalog: CharacterCatalog?): List<HitDicePool> {
    if (catalog == null || character.classes.isEmpty()) {
        val total = character.level.coerceAtLeast(1)
        return listOf(HitDicePool(character.hitDieSides, total, character.spentHitDice.coerceIn(0, total)))
    }
    return hitDicePools(classesWithSpentHitDice(character), catalog)
}

/**
 * The classes with the character's spent hit dice in them. [Character.spentHitDice] is the total;
 * when the classes don't add up to it (sheets from before the dice were kept by class) it is
 * spread over the classes in turn.
 */
fun classesWithSpentHitDice(character: Character): List<CharacterClassEntry> {
    val classes = character.classes
    val total = character.spentHitDice.coerceIn(0, classes.sumOf { it.levels })
    if (classes.sumOf { it.spentHitDice } == total) return classes
    var left = total
    return classes.map { entry ->
        val spent = left.coerceAtMost(entry.levels)
        left -= spent
        entry.copy(spentHitDice = spent)
    }
}

/**
 * The classes after a short rest that spends [picked] dice of each size (sides to count): each
 * size's dice come out of its classes in turn, never more than a class has left.
 */
fun spendClassHitDice(
    classes: List<CharacterClassEntry>,
    catalog: CharacterCatalog,
    picked: Map<Int, Int>
): List<CharacterClassEntry> {
    val left = picked.toMutableMap()
    return classes.map { entry ->
        val sides = classHitDie(entry, catalog)
        val spend = (left[sides] ?: 0).coerceAtMost(entry.levels - entry.spentHitDice).coerceAtLeast(0)
        left[sides] = (left[sides] ?: 0) - spend
        entry.copy(spentHitDice = entry.spentHitDice + spend)
    }
}

/**
 * Hit points the hit dice of a short rest give back (PHB 2024): each die's roll plus the
 * Constitution modifier, at least 1 a die.
 */
fun hitDiceHealing(rolls: List<Int>, constitutionModifier: Int): Int =
    rolls.sumOf { max(1, it + constitutionModifier) }

/** What dice of [sides] give back on average, as [hitDiceHealing] counts them. */
fun averageHitDiceHealing(sides: List<Int>, constitutionModifier: Int): Double =
    sides.sumOf { max(1.0, (it + 1) / 2.0 + constitutionModifier) }
