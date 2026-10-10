package com.dndcharacterhandler.domain.dnd5e.rules

import com.dndcharacterhandler.domain.dnd5e.model.AdvancementRecord
import com.dndcharacterhandler.domain.dnd5e.model.Condition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DefensesTest {
    private fun trait(value: String, type: String = "Trait") = AdvancementRecord(1, "fighter", 1, "dwarf", "step", type, value)

    @Test
    fun theWizardsRecordsGiveTheDefensesBack() {
        val records = listOf(
            trait("mode=default;added=dr:poison,languages:common"),
            trait("mode=default;added=ci:poisoned"),
            trait("mode=default;added=skills:ath"),
            trait("dr:fire", type = "HitPoints")
        )
        assertEquals(setOf("dr:poison", "ci:poisoned"), Defenses.fromAdvancements(records))
        assertEquals(emptySet<String>(), Defenses.fromAdvancements(emptyList()))
    }

    @Test
    fun kindsAndConditionImmunities() {
        val defenses = setOf("dr:fire", "dr:cold", "di:poison", "ci:poisoned", "ci:exhaustion", "ci:diseased")
        assertEquals(listOf("dr:cold", "dr:fire"), Defenses.ofKind(defenses, Defenses.RESISTANCE))
        assertEquals(setOf(Condition.POISONED), Defenses.immuneConditions(defenses))
        assertTrue(Defenses.immuneToExhaustion(defenses))
        assertFalse(Defenses.immuneToExhaustion(setOf("dr:fire")))
        assertTrue(Defenses.isDefense("dv:cold"))
        assertFalse(Defenses.isDefense("languages:common"))
    }
}
