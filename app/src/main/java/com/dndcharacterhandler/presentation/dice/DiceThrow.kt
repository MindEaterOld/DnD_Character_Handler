package com.dndcharacterhandler.presentation.dice

/** One picked die and the bodies thrown for it (two for a d100: the tens die, then the units die). */
internal class ThrownDie(val type: DieType, val bodies: List<DieBody>) {
    /** The rolled number: a d10's 0 reads as 10, a d100's 00 and 0 as 100. */
    fun value(): Int = when (type) {
        DieType.D10 -> bodies[0].topValue().let { if (it == 0) 10 else it }
        DieType.D100 -> (bodies[0].topValue() * 10 + bodies[1].topValue()).let { if (it == 0) 100 else it }
        else -> bodies[0].topValue()
    }
}

/**
 * Puts the dice of [selection] (how many of each type) into the player's hand at [handPosition],
 * in [DieType] order, and returns them one entry per picked die.
 */
internal fun DiceWorld.spawnSelection(selection: Map<DieType, Int>, handPosition: Vec3): List<ThrownDie> {
    val types = DieType.entries.flatMap { type -> List(selection[type] ?: 0) { type } }
    spawnInHand(types.flatMap(DieShapes::kindsFor), handPosition)
    var next = 0
    return types.map { type ->
        val count = DieShapes.kindsFor(type).size
        ThrownDie(type, bodies.subList(next, next + count).toList()).also { next += count }
    }
}
