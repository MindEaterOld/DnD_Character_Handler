package com.dndcharacterhandler.domain.dnd5e.rules

import org.junit.Assert.assertEquals
import org.junit.Test

class ExperienceProgressTest {
    private val eps = 0.001f

    @Test
    fun countsFromTheLevelsOwnThresholdToTheNext() {
        // Level 1 runs from 0 to 300; level 2 from 300 to 900.
        assertEquals(0f, experienceProgress(1, 0), eps)
        assertEquals(0.5f, experienceProgress(1, 150), eps)
        assertEquals(0.5f, experienceProgress(2, 600), eps)
    }

    @Test
    fun staysBetweenNothingAndFull() {
        // More than the next level needs (a level up due) is full; less than the level's own is empty.
        assertEquals(1f, experienceProgress(1, 1000), eps)
        assertEquals(0f, experienceProgress(3, 100), eps)
    }

    @Test
    fun theLastLevelIsFull() {
        assertEquals(1f, experienceProgress(MAX_CHARACTER_LEVEL, 0), eps)
    }
}
