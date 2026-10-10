package com.dndcharacterhandler.domain.dnd5e.rules

/** The sides of dice there is a die of on the dice table. */
private val TableDice = setOf(4, 6, 8, 10, 12, 20, 100)

/**
 * A sum of dice and a number, "2d6 + 1d4 + 3": [dice] by their sides (6 → 2 dice), [flat] the rest.
 * The dice themselves are always thrown on the table; this only says which.
 */
data class DiceFormula(val dice: Map<Int, Int> = emptyMap(), val flat: Int = 0) {
    val isEmpty: Boolean get() = dice.values.all { it <= 0 } && flat == 0

    val hasDice: Boolean get() = dice.values.any { it > 0 }

    /** The dice alone: what a critical hit rolls a second time. */
    val diceOnly: DiceFormula get() = DiceFormula(dice, 0)

    operator fun plus(other: DiceFormula): DiceFormula = DiceFormula(
        (dice.keys + other.dice.keys).associateWith { (dice[it] ?: 0) + (other.dice[it] ?: 0) }.filterValues { it > 0 },
        flat + other.flat
    )

    operator fun plus(number: Int): DiceFormula = copy(flat = flat + number)

    /** "2d6 + 1d4 + 3", the bigger dice first; "0" when empty. */
    fun label(): String {
        val parts = dice.filterValues { it > 0 }.toSortedMap(compareByDescending { it }).map { (sides, count) -> "${count}d$sides" }
        return when {
            parts.isEmpty() -> flat.toString()
            flat > 0 -> parts.joinToString(" + ") + " + $flat"
            flat < 0 -> parts.joinToString(" + ") + " - ${-flat}"
            else -> parts.joinToString(" + ")
        }
    }

    companion object {
        val Zero = DiceFormula()

        fun of(count: Int, sides: Int, flat: Int = 0): DiceFormula =
            DiceFormula(if (count > 0 && sides in TableDice) mapOf(sides to count) else emptyMap(), flat)

        /**
         * A formula as people write it: "1d4", "+2", "1d6+2", "2d6 + 1d4 - 1", "d8", and the Russian
         * "1к4". Blank is [Zero]; null when it isn't a formula or names a die the table hasn't got.
         */
        fun parse(text: String): DiceFormula? {
            val compact = text.lowercase().replace(" ", "").replace('к', 'd').replace('д', 'd').replace('−', '-')
            if (compact.isEmpty()) return Zero
            if (!Regex("""[+-]?(\d*d\d+|\d+)([+-](\d*d\d+|\d+))*""").matches(compact)) return null
            var formula = Zero
            Regex("""([+-]?)(\d*d\d+|\d+)""").findAll(compact).forEach { match ->
                val negative = match.groupValues[1] == "-"
                val term = match.groupValues[2]
                if ('d' in term) {
                    if (negative) return null
                    val count = term.substringBefore('d').ifEmpty { "1" }.toInt()
                    val sides = term.substringAfter('d').toInt()
                    if (sides !in TableDice || count <= 0) return null
                    formula += of(count, sides)
                } else {
                    formula += if (negative) -term.toInt() else term.toInt()
                }
            }
            return formula
        }
    }
}

/** The values thrown, by the dice's sides, in the order the table gave them. */
typealias ThrownValues = Map<Int, List<Int>>

/** A part of a throw read back: its dice, the number added, the total. */
data class RollPart(val dice: List<Int>, val flat: Int) {
    val total: Int get() = dice.sum() + flat
}

/**
 * An attack roll as set up in the roll pop-up: the attack's [bonus] as it stands (the conditions'
 * penalty in it), [mode] for one d20 or two, the situational [attackExtra] (Bless's 1d4), and the
 * [damage] with whatever was added to it. Attack and damage go onto the table in one throw; a
 * critical hit then throws the damage dice once more ([criticalExtra]).
 */
data class AttackRoll(
    val bonus: Int,
    val mode: RollMode,
    val attackExtra: DiceFormula = DiceFormula.Zero,
    val damage: DiceFormula = DiceFormula.Zero
) {
    private val d20s: Int get() = if (mode == RollMode.NORMAL) 1 else 2

    /** The dice on the table: the d20 (two with advantage or disadvantage), the extra, the damage. */
    fun selection(): Map<Int, Int> =
        (DiceFormula(mapOf(20 to d20s)) + attackExtra.diceOnly + damage.diceOnly).dice

    /** On a critical hit: every damage die once more, the numbers not. */
    val criticalExtra: DiceFormula get() = damage.diceOnly

    fun read(thrown: ThrownValues): AttackOutcome {
        val queues = thrown.mapValues { it.value.toMutableList() }
        fun take(sides: Int, count: Int): List<Int> {
            val queue = queues[sides] ?: return emptyList()
            return List(minOf(count, queue.size)) { queue.removeAt(0) }
        }
        val d20 = take(20, d20s)
        val natural = when (mode) {
            RollMode.ADVANTAGE -> d20.maxOrNull()
            RollMode.DISADVANTAGE -> d20.minOrNull()
            RollMode.NORMAL -> d20.firstOrNull()
        } ?: 0
        val extraDice = attackExtra.dice.toSortedMap(compareByDescending { it }).flatMap { (sides, count) -> take(sides, count) }
        val damageDice = damage.dice.toSortedMap(compareByDescending { it }).flatMap { (sides, count) -> take(sides, count) }
        return AttackOutcome(
            d20s = d20,
            natural = natural,
            attack = RollPart(listOf(natural) + extraDice, bonus + attackExtra.flat),
            damage = RollPart(damageDice, damage.flat)
        )
    }
}

/** What one throw of an attack's dice says. */
data class AttackOutcome(
    /** The one or two d20 thrown for the attack; [natural] is the one that counts. */
    val d20s: List<Int>,
    val natural: Int,
    val attack: RollPart,
    val damage: RollPart
) {
    /** A natural 20 hits whatever the AC and rolls the damage dice twice. */
    val critical: Boolean get() = natural == 20

    /** A natural 1 misses whatever the bonus. */
    val fumble: Boolean get() = natural == 1

    /** Damage never goes below 0. */
    val damageTotal: Int get() = damage.total.coerceAtLeast(0)
}

/** A throw of [formula] alone (a save spell's damage, healing, a critical's extra dice) read back. */
fun DiceFormula.read(thrown: ThrownValues): RollPart {
    val queues = thrown.mapValues { it.value.toMutableList() }
    val dice = dice.toSortedMap(compareByDescending { it }).flatMap { (sides, count) ->
        val queue = queues[sides] ?: mutableListOf()
        List(minOf(count, queue.size)) { queue.removeAt(0) }
    }
    return RollPart(dice, flat)
}
