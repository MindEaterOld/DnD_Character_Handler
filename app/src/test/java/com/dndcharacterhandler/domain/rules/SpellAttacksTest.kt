package com.dndcharacterhandler.domain.rules

import com.dndcharacterhandler.domain.model.Spell
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SpellAttacksTest {
    private fun spell(id: Long, name: String, catalogId: String? = null, damage: String = "8d6") =
        Spell(id = id, catalogId = catalogId, name = name, level = 3, school = "Evocation", isPrepared = true, description = "", damageBase = damage)

    private val book = listOf(spell(1, "Огненный шар", "fireball", "9d6"), spell(2, "Свой луч", damage = "2d8"))

    @Test
    fun aCombatSpellShowsTheBooksSpellAsItIsNow() {
        val entry = spell(10, "Огненный шар", "fireball", "8d6")
        val now = entry.asIn(bookSpellOf(entry, book)!!)
        assertEquals(10L, now.id)
        assertEquals("9d6", now.damageBase)
        assertEquals(book[1], bookSpellOf(spell(11, " свой луч "), book))
    }

    @Test
    fun aRenamedVariantOrAnotherCatalogSpellStaysItsOwn() {
        assertNull(bookSpellOf(spell(12, "Огненный шар (5 круг)", "fireball"), book))
        assertNull(bookSpellOf(spell(13, "Огненный шар", "delayed-fireball"), book))
        assertNull(bookSpellOf(spell(14, "Огненный шар", "fireball"), emptyList()))
    }
}
