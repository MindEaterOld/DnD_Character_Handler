package com.dndcharacterhandler.presentation.dice

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * A throw a screen asks for: [selection] goes onto the dice table over the whole app (the screen
 * edges its walls), and [onSettled] gets the dice each time they settle — a new throw on the same
 * table replaces the last, so it should count from the same starting point.
 */
internal class DiceRollRequest(val selection: Map<DieType, Int>, val onSettled: (List<ThrownDie>) -> Unit)

/** Puts a [DiceRollRequest] on the app's dice table; the app provides it (see DndCharacterApp). */
internal val LocalDiceRoller = staticCompositionLocalOf<(DiceRollRequest) -> Unit> { {} }
