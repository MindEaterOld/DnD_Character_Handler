package com.dndcharacterhandler.domain.dnd5e.rules

import com.dndcharacterhandler.domain.dnd5e.model.Spell
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SpellCastingTest {
    private val fireball = Spell(
        name = "Fireball", level = 3, school = "Evocation", isPrepared = true, description = "",
        damageBase = "8d6", damage = "3: 8d6\n4: 9d6\n5: 10d6\n6: 11d6\n7: 12d6\n8: 13d6\n9: 14d6"
    )
    private val acidSplash = Spell(
        name = "Acid Splash", level = 0, school = "Evocation", isPrepared = true, description = "",
        damageBase = "1d6", damage = "1: 1d6\n5: 2d6\n11: 3d6\n17: 4d6"
    )
    private val cureWounds = Spell(
        name = "Cure Wounds", level = 1, school = "Abjuration", isPrepared = true, description = "",
        healBase = "2d8", healing = "1: 2d8 + MOD\n2: 4d8 + MOD\n3: 6d8 + MOD"
    )

    @Test
    fun aHigherSlotAddsTheSpellsDice() {
        assertEquals("8d6", fireball.castAt(3).damageBase)
        assertEquals("10d6", fireball.castAt(5).damageBase)
        assertEquals("6d8", cureWounds.castAt(3).healBase)
        // Past the last line it stays at the last one.
        assertEquals("6d8", cureWounds.castAt(9).healBase)
    }

    @Test
    fun aCantripGrowsWithTheCharactersLevel() {
        assertEquals("1d6", acidSplash.castAt(castLevel(0, null, 4)).damageBase)
        assertEquals("2d6", acidSplash.castAt(castLevel(0, null, 5)).damageBase)
        assertEquals("4d6", acidSplash.castAt(castLevel(0, null, 20)).damageBase)
    }

    @Test
    fun aBaseThePlayerChangedGrowsTheSameWay() {
        assertEquals("3d6", acidSplash.copy(damageBase = "2d6").castAt(5).damageBase)
        assertEquals("2d10", scaledDice("2d10", "", 5))
        assertEquals("9d6", scaledDice("9d6", "3: 8d6", 3))
    }

    @Test
    fun slotsFromTheSpellsLevelUpAndTheLowestLeftFirst() {
        val maximums = spellSlots("4,3,3,1")
        val remaining = spellSlots("2,0,1,1")
        val options = slotOptions(2, maximums, remaining)
        assertEquals(listOf(2, 3, 4), options.map { it.level })
        assertEquals(3, defaultSlotLevel(options))
        assertNull(defaultSlotLevel(slotOptions(2, maximums, spellSlots("2,0,0,0"))))
        assertEquals(emptyList<SlotOption>(), slotOptions(0, maximums, remaining))
    }

    @Test
    fun castingSpendsOneSlot() {
        val remaining = spellSlots("2,0,1,1")
        assertEquals("2,0,0,1,0,0,0,0,0", encodeSpellSlots(spendSlot(remaining, 3)))
        assertEquals("2,0,1,1,0,0,0,0,0", encodeSpellSlots(spendSlot(remaining, 2)))
        assertEquals(3, castLevel(3, null, 7))
        assertEquals(5, castLevel(3, 5, 7))
    }
}
