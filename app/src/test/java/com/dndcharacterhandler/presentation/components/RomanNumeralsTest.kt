package com.dndcharacterhandler.presentation.components

import org.junit.Assert.assertEquals
import org.junit.Test

class RomanNumeralsTest {
    @Test
    fun theLevelsAreWrittenInRomanNumerals() {
        val expected = listOf(
            "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X",
            "XI", "XII", "XIII", "XIV", "XV", "XVI", "XVII", "XVIII", "XIX", "XX"
        )
        assertEquals(expected, (1..20).map { romanNumeral(it) })
    }

    @Test
    fun nothingBelowOne() {
        assertEquals("", romanNumeral(0))
    }
}
