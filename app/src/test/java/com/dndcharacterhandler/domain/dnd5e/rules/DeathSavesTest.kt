package com.dndcharacterhandler.domain.dnd5e.rules

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
        // Exhaustion 2: a 12 is a 8, a failure; a natural 20 still brings them round.
        assertEquals(DeathSaves(failures = 1), deathSave(12, DeathSaves(), modifier = -4).saves)
        assertTrue(deathSave(20, DeathSaves(), modifier = -4).regainsHitPoint)
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
    fun damageAtZeroHitPointsFailsADeathSave() {
        assertEquals(DamageResult(0, 0, DeathSaves(1, 2)), takeDamage(5, 0, 0, 30, DeathSaves(successes = 1, failures = 1)))
        assertEquals(DeathSaves(0, 3), takeDamage(5, 0, 0, 30, DeathSaves(0, 2)).saves)
    }

    @Test
    fun damageAsBigAsTheMaximumKills() {
        assertTrue(takeDamage(30, 0, 0, 30, DeathSaves()).saves.isDead)
        assertFalse(takeDamage(29, 0, 0, 30, DeathSaves()).saves.isDead)
        // Dropped to 0 with 30 left over: dead on the spot.
        val massive = takeDamage(40, 10, 0, 30, DeathSaves())
        assertEquals(0, massive.currentHp)
        assertTrue(massive.saves.isDead)
        assertEquals(DamageResult(0, 0, DeathSaves()), takeDamage(39, 10, 0, 30, DeathSaves()))
    }

    @Test
    fun aHitThatWouldLeaveMinusTheMaximumKills() {
        // 27 maximum, 10 left: 37 would leave -27, dead; 36 would leave -26, dying at 0.
        assertEquals(DamageResult(0, 0, DeathSaves(failures = 3)), takeDamage(37, 10, 0, 27, DeathSaves()))
        assertEquals(DamageResult(0, 0, DeathSaves()), takeDamage(36, 10, 0, 27, DeathSaves()))
    }

    @Test
    fun temporaryHitPointsTakeTheDamageFirst() {
        assertEquals(DamageResult(0, 2, DeathSaves(failures = 1)), takeDamage(3, 0, 5, 30, DeathSaves(failures = 1)))
        assertEquals(DamageResult(0, 0, DeathSaves(failures = 2)), takeDamage(8, 0, 5, 30, DeathSaves(failures = 1)))
        assertEquals(DamageResult(7, 0, DeathSaves()), takeDamage(8, 10, 5, 30, DeathSaves()))
    }

    @Test
    fun damageAboveZeroLeavesTheSavesAlone() {
        assertEquals(DamageResult(4, 0, DeathSaves()), takeDamage(6, 10, 0, 30, DeathSaves()))
        assertEquals(DamageResult(10, 0, DeathSaves()), takeDamage(0, 10, 0, 30, DeathSaves()))
    }

    @Test
    fun threeSuccessesStabilize() {
        val stable = deathSave(12, DeathSaves(successes = 2, failures = 1)).saves
        assertTrue(stable.isStable)
        assertFalse(stable.isDead)
    }
}
