package com.dndcharacterhandler.domain.dnd5e.rules

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DiceRollsTest {
    @Test
    fun formulasAsPeopleWriteThem() {
        assertEquals(DiceFormula(mapOf(4 to 1)), DiceFormula.parse("1d4"))
        assertEquals(DiceFormula(mapOf(4 to 1)), DiceFormula.parse("1к4"))
        assertEquals(DiceFormula(mapOf(8 to 1)), DiceFormula.parse("d8"))
        assertEquals(DiceFormula(flat = 2), DiceFormula.parse("+2"))
        assertEquals(DiceFormula(flat = -1), DiceFormula.parse("-1"))
        assertEquals(DiceFormula(mapOf(6 to 2, 4 to 1), -1), DiceFormula.parse("2d6 + 1d4 - 1"))
        assertEquals(DiceFormula(mapOf(6 to 3)), DiceFormula.parse("1d6+2d6"))
        assertEquals(DiceFormula.Zero, DiceFormula.parse("  "))
        assertNull(DiceFormula.parse("1d7"))
        assertNull(DiceFormula.parse("abc"))
        assertNull(DiceFormula.parse("-1d4"))
        assertNull(DiceFormula.parse("2+"))
    }

    @Test
    fun aFormulaReadsBack() {
        assertEquals("2d6 + 1d4 + 3", DiceFormula(mapOf(4 to 1, 6 to 2), 3).label())
        assertEquals("1d8 - 1", DiceFormula.of(1, 8, -1).label())
        assertEquals("5", DiceFormula(flat = 5).label())
        assertEquals(DiceFormula(mapOf(8 to 2), 3), DiceFormula.of(1, 8, 3) + DiceFormula.of(1, 8))
    }

    @Test
    fun aNormalAttackThrowsOneD20AndTheDamageDice() {
        val roll = AttackRoll(bonus = 5, mode = RollMode.NORMAL, damage = DiceFormula.of(1, 8, 3))
        assertEquals(mapOf(20 to 1, 8 to 1), roll.selection())
        val outcome = roll.read(mapOf(20 to listOf(14), 8 to listOf(6)))
        assertEquals(19, outcome.attack.total)
        assertEquals(9, outcome.damageTotal)
        assertFalse(outcome.critical)
    }

    @Test
    fun advantageKeepsTheHigherD20AndDisadvantageTheLower() {
        val thrown = mapOf(20 to listOf(8, 14), 8 to listOf(5))
        val damage = DiceFormula.of(1, 8, 3)
        assertEquals(mapOf(20 to 2, 8 to 1), AttackRoll(5, RollMode.ADVANTAGE, damage = damage).selection())
        assertEquals(14, AttackRoll(5, RollMode.ADVANTAGE, damage = damage).read(thrown).natural)
        assertEquals(8, AttackRoll(5, RollMode.DISADVANTAGE, damage = damage).read(thrown).natural)
        assertEquals(13, AttackRoll(5, RollMode.DISADVANTAGE, damage = damage).read(thrown).attack.total)
    }

    @Test
    fun theSituationalDiceGoToTheAttackFirst() {
        // Bless's 1d4 on a dagger's 1d4: the first d4 is the attack's, the second the damage's.
        val roll = AttackRoll(bonus = 4, mode = RollMode.NORMAL, attackExtra = DiceFormula.of(1, 4), damage = DiceFormula.of(1, 4, 2))
        assertEquals(mapOf(20 to 1, 4 to 2), roll.selection())
        val outcome = roll.read(mapOf(20 to listOf(10), 4 to listOf(3, 1)))
        assertEquals(17, outcome.attack.total)
        assertEquals(3, outcome.damageTotal)
    }

    @Test
    fun naturalTwentyAndOne() {
        val roll = AttackRoll(bonus = 2, mode = RollMode.NORMAL, damage = DiceFormula.of(2, 6, 1))
        val critical = roll.read(mapOf(20 to listOf(20), 6 to listOf(4, 5)))
        assertTrue(critical.critical)
        assertEquals(DiceFormula(mapOf(6 to 2)), roll.criticalExtra)
        assertTrue(roll.read(mapOf(20 to listOf(1), 6 to listOf(4, 5))).fumble)
    }

    @Test
    fun damageNeverGoesBelowZero() {
        val outcome = AttackRoll(bonus = 0, mode = RollMode.NORMAL, damage = DiceFormula.of(1, 4, -3)).read(mapOf(20 to listOf(10), 4 to listOf(1)))
        assertEquals(0, outcome.damageTotal)
    }

    @Test
    fun aFormulaAloneReadsItsDice() {
        val part = DiceFormula.of(2, 4, 2).read(mapOf(4 to listOf(3, 4)))
        assertEquals(listOf(3, 4), part.dice)
        assertEquals(9, part.total)
    }
}
