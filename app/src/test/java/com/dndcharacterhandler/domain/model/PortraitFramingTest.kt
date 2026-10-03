package com.dndcharacterhandler.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class PortraitFramingTest {
    private val eps = 0.001f

    @Test
    fun theDefaultIsACentreCrop() {
        // A landscape picture in a square frame: as tall as the frame, its middle showing.
        val placed = PortraitFraming().placement(400f, 200f, 100f, 100f)
        assertEquals(200f, placed.width, eps)
        assertEquals(100f, placed.height, eps)
        assertEquals(-50f, placed.left, eps)
        assertEquals(0f, placed.top, eps)
    }

    @Test
    fun anEdgeNeverComesIntoTheFrame() {
        val placed = PortraitFraming(focusX = 0f, focusY = 1f).placement(400f, 200f, 100f, 100f)
        assertEquals(0f, placed.left, eps)
        assertEquals(0f, placed.top, eps)
    }

    @Test
    fun aDragMovesThePictureWithTheFinger() {
        val start = PortraitFraming()
        // Dragged 30 to the right: the picture's left part comes into view.
        val moved = start.panned(30f, 0f, 400f, 200f, 100f, 100f)
        assertEquals(-20f, moved.placement(400f, 200f, 100f, 100f).left, eps)
        // As far as it goes, and a drag back starts from there.
        val far = start.panned(500f, 0f, 400f, 200f, 100f, 100f)
        assertEquals(0f, far.placement(400f, 200f, 100f, 100f).left, eps)
        assertEquals(-10f, far.panned(-10f, 0f, 400f, 200f, 100f, 100f).placement(400f, 200f, 100f, 100f).left, eps)
    }

    @Test
    fun zoomStaysBetweenOneAndTheMaximum() {
        val start = PortraitFraming()
        assertEquals(PortraitFraming.MAX_ZOOM, start.zoomedTo(10f, 400f, 200f, 100f, 100f).zoom, eps)
        assertEquals(1f, start.zoomedTo(0.2f, 400f, 200f, 100f, 100f).zoom, eps)
        val doubled = start.zoomedTo(2f, 400f, 200f, 100f, 100f).placement(400f, 200f, 100f, 100f)
        assertEquals(400f, doubled.width, eps)
        assertEquals(-150f, doubled.left, eps)
        assertEquals(-50f, doubled.top, eps)
    }

    @Test
    fun theSameFramingShowsTheSamePartAtAnySize() {
        val framing = PortraitFraming(focusX = 0.3f, focusY = 0.6f, zoom = 1.5f)
        val big = framing.placement(300f, 400f, 200f, 200f)
        val small = framing.placement(300f, 400f, 50f, 50f)
        assertEquals(big.left / 200f, small.left / 50f, eps)
        assertEquals(big.top / 200f, small.top / 50f, eps)
    }
}
