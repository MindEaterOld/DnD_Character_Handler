package com.dndcharacterhandler.presentation.components

/**
 * A card another screen is asked to show (owner, 2026-10-15 — a tap on a line of the armor class's sources opens its
 * feature or its item): the screen unfolds it, scrolls it into sight and says it is done.
 */
sealed interface ScreenFocus {
    /** A feature on the Features screen, by its id among the character's features. */
    data class OnFeature(val featureId: Long) : ScreenFocus

    /** An item on the Inventory screen, by its id in the inventory. */
    data class OnItem(val itemId: Long) : ScreenFocus
}
