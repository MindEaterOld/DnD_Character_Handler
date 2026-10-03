package com.dndcharacterhandler.domain.rules

import com.dndcharacterhandler.domain.model.Condition
import com.dndcharacterhandler.domain.model.SpellcastingAbility
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConditionRulesTest {
    @Test
    fun exhaustionTakesTwoALevelFromEveryD20Test() {
        val tests = listOf(
            D20Test.Attack,
            D20Test.AbilityCheck(SpellcastingAbility.WISDOM),
            D20Test.SavingThrow(SpellcastingAbility.CONSTITUTION),
            D20Test.Initiative,
            D20Test.DeathSave
        )
        tests.forEach { assertEquals(-4, rollEffects(it, emptySet(), exhaustion = 2).modifier) }
        assertEquals(-12, rollEffects(D20Test.Attack, emptySet(), exhaustion = 9).modifier)
        assertFalse(rollEffects(D20Test.Attack, emptySet(), exhaustion = 0).changesRoll)
    }

    @Test
    fun poisonedHasDisadvantageOnAttacksAndChecksButNotSaves() {
        val poisoned = setOf(Condition.POISONED)
        assertEquals(RollMode.DISADVANTAGE, rollEffects(D20Test.Attack, poisoned, 0).mode)
        assertEquals(RollMode.DISADVANTAGE, rollEffects(D20Test.AbilityCheck(SpellcastingAbility.STRENGTH), poisoned, 0).mode)
        assertEquals(RollMode.DISADVANTAGE, rollEffects(D20Test.Initiative, poisoned, 0).mode)
        assertEquals(RollMode.NORMAL, rollEffects(D20Test.SavingThrow(SpellcastingAbility.CONSTITUTION), poisoned, 0).mode)
    }

    @Test
    fun advantageAndDisadvantageCancelOut() {
        val effects = rollEffects(D20Test.Attack, setOf(Condition.INVISIBLE, Condition.POISONED), 0)
        assertEquals(RollMode.NORMAL, effects.mode)
        assertTrue(effects.changesRoll)
        assertEquals(RollMode.ADVANTAGE, rollEffects(D20Test.Attack, setOf(Condition.INVISIBLE), 0).mode)
    }

    @Test
    fun stunnedFailsStrengthAndDexteritySavesOnly() {
        val stunned = setOf(Condition.STUNNED)
        assertEquals(listOf(Condition.STUNNED), rollEffects(D20Test.SavingThrow(SpellcastingAbility.DEXTERITY), stunned, 0).autoFail)
        assertEquals(listOf(Condition.STUNNED), rollEffects(D20Test.SavingThrow(SpellcastingAbility.STRENGTH), stunned, 0).autoFail)
        assertTrue(rollEffects(D20Test.SavingThrow(SpellcastingAbility.WISDOM), stunned, 0).autoFail.isEmpty())
    }

    @Test
    fun restrainedHasDisadvantageOnDexteritySaves() {
        val restrained = setOf(Condition.RESTRAINED)
        assertEquals(RollMode.DISADVANTAGE, rollEffects(D20Test.SavingThrow(SpellcastingAbility.DEXTERITY), restrained, 0).mode)
        assertEquals(RollMode.NORMAL, rollEffects(D20Test.SavingThrow(SpellcastingAbility.STRENGTH), restrained, 0).mode)
    }

    @Test
    fun unconsciousIsIncapacitatedAndProneToo() {
        val active = effectiveConditions(setOf(Condition.UNCONSCIOUS))
        assertTrue(Condition.INCAPACITATED in active)
        assertTrue(Condition.PRONE in active)
        // Prone: disadvantage on its attacks; incapacitated: on initiative.
        assertEquals(RollMode.DISADVANTAGE, rollEffects(D20Test.Attack, setOf(Condition.UNCONSCIOUS), 0).mode)
        assertEquals(RollMode.DISADVANTAGE, rollEffects(D20Test.Initiative, setOf(Condition.UNCONSCIOUS), 0).mode)
    }

    @Test
    fun speedDropsWithExhaustionAndStopsWhenHeld() {
        assertEquals(20, effectiveSpeed(30, emptySet(), exhaustion = 2))
        assertEquals(0, effectiveSpeed(30, emptySet(), exhaustion = 6))
        assertEquals(0, effectiveSpeed(30, setOf(Condition.GRAPPLED), exhaustion = 0))
        assertEquals(30, effectiveSpeed(30, setOf(Condition.POISONED), exhaustion = 0))
        // Stunned doesn't stop you by itself in the 2024 rules.
        assertEquals(30, effectiveSpeed(30, setOf(Condition.STUNNED), exhaustion = 0))
    }

    @Test
    fun zeroHitPointsMeanUnconsciousAndTheSixthLevelOfExhaustionKills() {
        assertTrue(Condition.UNCONSCIOUS in activeConditions(emptySet(), currentHp = 0))
        assertFalse(Condition.UNCONSCIOUS in activeConditions(emptySet(), currentHp = 3))
        assertTrue(isDead(DeathSaves(), exhaustion = 6))
        assertFalse(isDead(DeathSaves(failures = 2), exhaustion = 5))
        assertTrue(isDead(DeathSaves(failures = 3), exhaustion = 0))
    }

    @Test
    fun concentration() {
        assertTrue(breaksConcentration(setOf(Condition.STUNNED)))
        assertTrue(breaksConcentration(setOf(Condition.INCAPACITATED)))
        assertFalse(breaksConcentration(setOf(Condition.POISONED, Condition.PRONE)))
        assertEquals(10, concentrationSaveDc(7))
        assertEquals(11, concentrationSaveDc(22))
        assertEquals(30, concentrationSaveDc(90))
    }
}
