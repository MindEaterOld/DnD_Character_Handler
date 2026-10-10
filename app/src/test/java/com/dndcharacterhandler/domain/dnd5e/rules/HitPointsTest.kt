package com.dndcharacterhandler.domain.dnd5e.rules

import org.junit.Assert.assertEquals
import org.junit.Test

class HitPointsTest {
    @Test
    fun healingStopsAtTheMaximum() {
        assertEquals(12, heal(3, 9, 13))
        assertEquals(13, heal(5, 9, 13))
        assertEquals(5, heal(5, 0, 13))
    }

    @Test
    fun healingNeverTakesHitPointsAway() {
        assertEquals(9, heal(-4, 9, 13))
        // Over a lowered maximum: healing doesn't cut it down.
        assertEquals(15, heal(2, 15, 13))
    }

    @Test
    fun minusTakesTheTemporaryOnesFirstAndStopsAtZero() {
        assertEquals(8 to 4, stepHitPoints(-1, 8, 5, 13))
        assertEquals(7 to 0, stepHitPoints(-1, 8, 0, 13))
        assertEquals(0 to 0, stepHitPoints(-1, 0, 0, 13))
        assertEquals(0 to 2, stepHitPoints(-1, 0, 3, 13))
    }

    @Test
    fun plusHealsOneUpToTheMaximumAndLeavesTheTemporaryOnes() {
        assertEquals(9 to 5, stepHitPoints(1, 8, 5, 13))
        assertEquals(13 to 0, stepHitPoints(1, 13, 0, 13))
        assertEquals(1 to 0, stepHitPoints(1, 0, 0, 13))
    }

    @Test
    fun temporaryHitPointsDoNotAddUp() {
        assertEquals(5, gainTemporaryHitPoints(5, 3))
        assertEquals(8, gainTemporaryHitPoints(5, 8))
        assertEquals(4, gainTemporaryHitPoints(4, 0))
        assertEquals(3, gainTemporaryHitPoints(-2, 3))
    }
}
