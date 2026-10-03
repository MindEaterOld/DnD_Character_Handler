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

    /** What the sheet shows for one condition alone: each value the screens mark. */
    private data class Shown(
        val attack: RollMode = RollMode.NORMAL,
        val checks: RollMode = RollMode.NORMAL,
        val failsStrengthAndDexteritySaves: Boolean = false,
        val dexteritySave: RollMode = RollMode.NORMAL,
        val initiative: RollMode = RollMode.NORMAL,
        val speed: Int = 30,
        val attacksAgainst: RollMode = RollMode.NORMAL,
        val damageOf10: Int = 10
    )

    private val expected = mapOf(
        Condition.BLINDED to Shown(attack = RollMode.DISADVANTAGE, attacksAgainst = RollMode.ADVANTAGE),
        Condition.CHARMED to Shown(),
        Condition.DEAFENED to Shown(),
        Condition.FRIGHTENED to Shown(attack = RollMode.DISADVANTAGE, checks = RollMode.DISADVANTAGE, initiative = RollMode.DISADVANTAGE),
        Condition.GRAPPLED to Shown(speed = 0),
        Condition.INCAPACITATED to Shown(initiative = RollMode.DISADVANTAGE),
        Condition.INVISIBLE to Shown(attack = RollMode.ADVANTAGE, initiative = RollMode.ADVANTAGE, attacksAgainst = RollMode.DISADVANTAGE),
        Condition.PARALYZED to Shown(failsStrengthAndDexteritySaves = true, initiative = RollMode.DISADVANTAGE, speed = 0, attacksAgainst = RollMode.ADVANTAGE),
        Condition.PETRIFIED to Shown(failsStrengthAndDexteritySaves = true, initiative = RollMode.DISADVANTAGE, speed = 0, attacksAgainst = RollMode.ADVANTAGE, damageOf10 = 5),
        Condition.POISONED to Shown(attack = RollMode.DISADVANTAGE, checks = RollMode.DISADVANTAGE, initiative = RollMode.DISADVANTAGE),
        Condition.PRONE to Shown(attack = RollMode.DISADVANTAGE),
        Condition.RESTRAINED to Shown(attack = RollMode.DISADVANTAGE, dexteritySave = RollMode.DISADVANTAGE, speed = 0, attacksAgainst = RollMode.ADVANTAGE),
        Condition.STUNNED to Shown(failsStrengthAndDexteritySaves = true, initiative = RollMode.DISADVANTAGE, attacksAgainst = RollMode.ADVANTAGE),
        Condition.UNCONSCIOUS to Shown(attack = RollMode.DISADVANTAGE, failsStrengthAndDexteritySaves = true, initiative = RollMode.DISADVANTAGE, speed = 0, attacksAgainst = RollMode.ADVANTAGE)
    )

    private fun shown(conditions: Set<Condition>, exhaustion: Int = 0): Shown {
        val strengthSave = rollEffects(D20Test.SavingThrow(SpellcastingAbility.STRENGTH), conditions, exhaustion)
        val dexteritySave = rollEffects(D20Test.SavingThrow(SpellcastingAbility.DEXTERITY), conditions, exhaustion)
        val checks = SpellcastingAbility.entries.map { rollEffects(D20Test.AbilityCheck(it), conditions, exhaustion).mode }.distinct()
        assertEquals("every ability's check alike", 1, checks.size)
        return Shown(
            attack = rollEffects(D20Test.Attack, conditions, exhaustion).mode,
            checks = checks.single(),
            failsStrengthAndDexteritySaves = strengthSave.autoFail.isNotEmpty() && dexteritySave.autoFail.isNotEmpty(),
            dexteritySave = dexteritySave.mode,
            initiative = rollEffects(D20Test.Initiative, conditions, exhaustion).mode,
            speed = effectiveSpeed(30, conditions, exhaustion),
            attacksAgainst = attacksAgainst(conditions),
            damageOf10 = damageTaken(10, conditions)
        )
    }

    @Test
    fun everyConditionShowsWhatItDoes() {
        assertEquals(Condition.entries.toSet(), expected.keys)
        expected.forEach { (condition, want) -> assertEquals(condition.name, want, shown(setOf(condition))) }
        // The other saves never fail, whatever the condition.
        Condition.entries.forEach { condition ->
            listOf(SpellcastingAbility.CONSTITUTION, SpellcastingAbility.INTELLIGENCE, SpellcastingAbility.WISDOM, SpellcastingAbility.CHARISMA).forEach {
                assertTrue(rollEffects(D20Test.SavingThrow(it), setOf(condition), 0).autoFail.isEmpty())
            }
        }
    }

    @Test
    fun takingConditionsOffBringsEverythingBack() {
        // All of them at once, then none: nothing left over.
        assertEquals(Shown(), shown(emptySet()))
        val tests = listOf(D20Test.Attack, D20Test.Initiative, D20Test.DeathSave) +
            SpellcastingAbility.entries.flatMap { listOf(D20Test.AbilityCheck(it), D20Test.SavingThrow(it)) }
        tests.forEach { assertFalse(rollEffects(it, emptySet(), 0).changesRoll) }
        assertTrue(rollEffects(D20Test.Attack, Condition.entries.toSet(), 3).changesRoll)
        // The stored keys go round and come back the same, and an empty list stays empty.
        assertEquals(Condition.entries.toSet(), Condition.parse(Condition.join(Condition.entries.toSet())))
        assertEquals(emptySet<Condition>(), Condition.parse(Condition.join(emptySet())))
        assertEquals(emptySet<Condition>(), Condition.parse(""))
    }

    @Test
    fun exhaustionShowsOnEveryRollAndOnSpeed() {
        val tired = shown(emptySet(), exhaustion = 3)
        assertEquals(Shown(speed = 15), tired)
        val tests = listOf(D20Test.Attack, D20Test.Initiative, D20Test.DeathSave) +
            SpellcastingAbility.entries.flatMap { listOf(D20Test.AbilityCheck(it), D20Test.SavingThrow(it)) }
        tests.forEach { assertEquals(-6, rollEffects(it, emptySet(), 3).modifier) }
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
