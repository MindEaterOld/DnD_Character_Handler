package com.dndcharacterhandler.domain.dnd5e.rules

import com.dndcharacterhandler.domain.dnd5e.model.Spell

/*
 * Casting a spell: the slot it takes (its own level or higher, none for a cantrip, a ritual or a
 * free cast), the dice it rolls at that level, and the concentration it starts.
 */

/** The nine slot counts stored as "4,3,2,0,…"; missing or broken ones are 0. */
fun spellSlots(value: String): List<Int> {
    val values = value.split(',').map { it.trim().toIntOrNull() ?: 0 }
    return List(9) { values.getOrElse(it) { 0 }.coerceAtLeast(0) }
}

fun encodeSpellSlots(slots: List<Int>): String = List(9) { (slots.getOrNull(it) ?: 0).coerceAtLeast(0) }.joinToString(",")

/** A slot level a spell can take: [remaining] of [maximum] left. */
data class SlotOption(val level: Int, val remaining: Int, val maximum: Int)

/** The slots a spell of [spellLevel] can be cast with: its own level and up, where the character has any. */
fun slotOptions(spellLevel: Int, maximums: List<Int>, remaining: List<Int>): List<SlotOption> {
    if (spellLevel !in 1..9) return emptyList()
    return (spellLevel..9).mapNotNull { level ->
        val maximum = maximums.getOrElse(level - 1) { 0 }
        if (maximum <= 0) null else SlotOption(level, remaining.getOrElse(level - 1) { 0 }.coerceIn(0, maximum), maximum)
    }
}

/** The slot a cast takes unless the player picks another: the lowest one left; null when none is. */
fun defaultSlotLevel(options: List<SlotOption>): Int? = options.firstOrNull { it.remaining > 0 }?.level

/** [remaining] with one slot of [level] spent (never below 0). */
fun spendSlot(remaining: List<Int>, level: Int): List<Int> =
    List(9) { index -> (remaining.getOrElse(index) { 0 } - if (index == level - 1) 1 else 0).coerceAtLeast(0) }

/** The level the dice grow by: a cantrip's is the character's, another spell's the slot's (its own without one). */
fun castLevel(spellLevel: Int, slotLevel: Int?, characterLevel: Int): Int =
    if (spellLevel <= 0) characterLevel else maxOf(spellLevel, slotLevel ?: spellLevel)

/**
 * [base] dice ("8d6") cast at [level], grown as the spell's [lines] say ("3: 8d6\n4: 9d6", a
 * cantrip's "1: 1d6\n5: 2d6"): by as many dice as the lines add from their first level to the last
 * one at or below [level]. So a base the player changed grows the same way. Without lines, as is.
 */
fun scaledDice(base: String, lines: String, level: Int): String {
    val baseMatch = Regex("""^\s*(\d+)\s*d\s*(\d+)""").find(base) ?: return base
    val steps = lines.lines().mapNotNull { line ->
        Regex("""^\s*(\d+)\s*:\s*(\d+)d\d+""").find(line)?.let { it.groupValues[1].toInt() to it.groupValues[2].toInt() }
    }.sortedBy { it.first }
    val first = steps.firstOrNull() ?: return base
    val reached = steps.lastOrNull { it.first <= level } ?: return base
    val count = baseMatch.groupValues[1].toInt() + reached.second - first.second
    return base.replaceRange(baseMatch.range, "${count.coerceAtLeast(0)}d${baseMatch.groupValues[2]}")
}

/** The spell as cast at [level] (see [castLevel]): its damage and healing dice grown for it. */
fun Spell.castAt(level: Int): Spell = copy(
    damageBase = scaledDice(damageBase, damage, level),
    healBase = scaledDice(healBase, healing, level)
)
