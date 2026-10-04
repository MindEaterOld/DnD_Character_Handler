package com.dndcharacterhandler.presentation.dice

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * A throw a screen asks for: [selection] goes onto the dice table over the whole app (the screen
 * edges its walls), and [onSettled] gets the dice each time they settle — a new throw on the same
 * table replaces the last, so it should count from the same starting point. [result], when given,
 * reads the settled dice in the table's panel instead of the plain total (an attack's breakdown).
 */
internal class DiceRollRequest(
    val selection: Map<DieType, Int>,
    val result: (@Composable (List<ThrownDie>) -> Unit)? = null,
    // Last, so a trailing lambda is always this one and never the composable [result].
    val onSettled: (List<ThrownDie>) -> Unit = {}
)

/** The faces of a die: 20 for a d20, 100 for the d100 thrown as two d10. */
internal val DieType.sides: Int get() = label.drop(1).toInt()

/** Puts a [DiceRollRequest] on the app's dice table; the app provides it (see DndCharacterApp). */
internal val LocalDiceRoller = staticCompositionLocalOf<(DiceRollRequest) -> Unit> { {} }
