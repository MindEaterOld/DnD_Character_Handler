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
    fun threeSuccessesStabilize() {
        val stable = deathSave(12, DeathSaves(successes = 2, failures = 1)).saves
        assertTrue(stable.isStable)
        assertFalse(stable.isDead)
    }
}
