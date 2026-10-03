package com.dndcharacterhandler.domain.rules

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeathSavesTest {
    @Test
    fun tenOrMoreSucceedsBelowTenFails() {
        assertEquals(DeathSaves(successes = 1), deathSave(10, DeathSaves()).saves)
        assertEquals(DeathSaves(successes = 2, failures = 1), deathSave(19, DeathSaves(1, 1)).saves)
        assertEquals(DeathSaves(failures = 1), deathSave(9, DeathSaves()).saves)
        assertEquals(DeathSaves(successes = 2, failures = 2), deathSave(2, DeathSaves(2, 1)).saves)
        assertFalse(deathSave(19, DeathSaves()).regainsHitPoint)
    }

    @Test
    fun aOneIsTwoFailuresButNoMoreThanThree() {
        assertEquals(DeathSaves(failures = 2), deathSave(1, DeathSaves()).saves)
        val dead = deathSave(1, DeathSaves(successes = 2, failures = 2)).saves
        assertEquals(DeathSaves(successes = 2, failures = 3), dead)
        assertTrue(dead.isDead)
        assertFalse(dead.isStable)
    }

    @Test
    fun aTwentyRegainsAHitPointAndClearsTheSaves() {
        val result = deathSave(20, DeathSaves(successes = 1, failures = 2))
        assertTrue(result.regainsHitPoint)
        assertEquals(DeathSaves(), result.saves)
    }

    @Test
    fun damageAtZeroHitPointsFailsADeathSaveACriticalTwo() {
        val saves = DeathSaves(successes = 1, failures = 1)
        assertEquals(DamageResult(0, 0, DeathSaves(1, 2)), takeDamage(5, false, 0, 0, 30, saves))
        assertEquals(DamageResult(0, 0, DeathSaves(1, 3)), takeDamage(5, true, 0, 0, 30, saves))
        assertEquals(DeathSaves(0, 3), takeDamage(5, true, 0, 0, 30, DeathSaves(0, 2)).saves)
    }

    @Test
    fun damageAsBigAsTheMaximumKills() {
        assertTrue(takeDamage(30, false, 0, 0, 30, DeathSaves()).saves.isDead)
        assertFalse(takeDamage(29, false, 0, 0, 30, DeathSaves()).saves.isDead)
        // Dropped to 0 with 30 left over: dead on the spot.
        val massive = takeDamage(40, false, 10, 0, 30, DeathSaves())
        assertEquals(0, massive.currentHp)
        assertTrue(massive.saves.isDead)
        assertEquals(DamageResult(0, 0, DeathSaves()), takeDamage(39, false, 10, 0, 30, DeathSaves()))
    }

    @Test
    fun temporaryHitPointsTakeTheDamageFirst() {
        assertEquals(DamageResult(0, 2, DeathSaves(failures = 1)), takeDamage(3, false, 0, 5, 30, DeathSaves(failures = 1)))
        assertEquals(DamageResult(0, 0, DeathSaves(failures = 2)), takeDamage(8, false, 0, 5, 30, DeathSaves(failures = 1)))
        assertEquals(DamageResult(7, 0, DeathSaves()), takeDamage(8, false, 10, 5, 30, DeathSaves()))
    }

    @Test
    fun damageAboveZeroLeavesTheSavesAlone() {
        assertEquals(DamageResult(4, 0, DeathSaves()), takeDamage(6, true, 10, 0, 30, DeathSaves()))
        assertEquals(DamageResult(10, 0, DeathSaves()), takeDamage(0, true, 10, 0, 30, DeathSaves()))
    }

    @Test
    fun threeSuccessesStabilize() {
        val stable = deathSave(12, DeathSaves(successes = 2, failures = 1)).saves
        assertTrue(stable.isStable)
        assertFalse(stable.isDead)
    }
}
