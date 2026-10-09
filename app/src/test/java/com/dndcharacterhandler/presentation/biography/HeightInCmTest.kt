package com.dndcharacterhandler.presentation.biography

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HeightInCmTest {
    @Test
    fun centimetresAreReadAsTheyAre() {
        assertEquals(180.0, heightInCm("180 cm")!!, 0.01)
    }

    @Test
    fun feetAloneAreConverted() {
        assertEquals(182.88, heightInCm("6 ft")!!, 0.01)
        assertEquals(179.83, heightInCm("5.9 ft")!!, 0.01)
    }

    @Test
    fun feetAndInchesAreReadWhole() {
        assertEquals(177.8, heightInCm("5'10\"")!!, 0.01)
        assertEquals(177.8, heightInCm("5 ft 10 in")!!, 0.01)
        assertEquals(152.4, heightInCm("5'")!!, 0.01)
    }

    @Test
    fun noNumberIsNoHeight() {
        assertNull(heightInCm("tall"))
    }
}
