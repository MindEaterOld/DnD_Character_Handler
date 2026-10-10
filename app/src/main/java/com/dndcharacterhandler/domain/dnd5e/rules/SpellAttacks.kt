package com.dndcharacterhandler.domain.dnd5e.rules

import com.dndcharacterhandler.domain.dnd5e.model.Spell

/*
 * A spell added to combat from the spellbook is that spell, as in Foundry: it shows what the book
 * has now, and editing it in combat edits the book's. A copy renamed in combat ("Fireball (5th
 * level)") is the player's own variant and stays as it is.
 */

/**
 * The spellbook's spell [entry] is: the one of the same name (as both are shown), and of the same
 * catalog spell when both come from the catalog; null for a spell made by hand in combat, a renamed
 * variant or one taken out of the book.
 */
fun bookSpellOf(entry: Spell, book: List<Spell>): Spell? {
    val name = entry.name.trim()
    if (name.isEmpty()) return null
    return book.firstOrNull { spell ->
        spell.name.trim().equals(name, ignoreCase = true) &&
            (spell.catalogId == null || entry.catalogId == null || spell.catalogId == entry.catalogId)
    }
}

/** The combat entry as its book spell is now; it keeps its own id. */
fun Spell.asIn(bookSpell: Spell): Spell = bookSpell.copy(id = id)
