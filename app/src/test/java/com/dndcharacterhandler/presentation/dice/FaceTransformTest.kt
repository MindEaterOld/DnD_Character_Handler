package com.dndcharacterhandler.presentation.dice

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Matrix
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The face frame goes onto the screen where its corners are: what Android's setPolyToPoly did. */
class FaceTransformTest {
    private fun assertMaps(source: FloatArray, destination: FloatArray, count: Int) {
        val matrix = Matrix()
        assertTrue(faceTransform(source, destination, count, matrix))
        for (i in 0 until count) {
            val mapped = matrix.map(Offset(source[2 * i], source[2 * i + 1]))
            assertEquals("x of $i", destination[2 * i], mapped.x, 0.01f)
            assertEquals("y of $i", destination[2 * i + 1], mapped.y, 0.01f)
        }
    }

    @Test
    fun fourCornersMapInPerspective() {
        // A square face seen at a slant: a trapezoid on screen.
        assertMaps(
            floatArrayOf(-70f, -70f, 70f, -70f, 70f, 70f, -70f, 70f),
            floatArrayOf(410f, 300f, 590f, 310f, 640f, 520f, 380f, 500f),
            4
        )
    }

    @Test
    fun threeCornersMapAffinely() {
        assertMaps(floatArrayOf(0f, -100f, 86.6f, 50f, -86.6f, 50f), floatArrayOf(200f, 120f, 260f, 220f, 150f, 210f), 3)
    }

    @Test
    fun theDiceFacesThemselvesMap() {
        // Every face of every die onto a scaled, shifted copy of itself, through its own canonical corners.
        DieShapeKind.entries.forEach { kind ->
            DieShapes.of(kind).faces.forEach { face ->
                val count = face.canonical.size / 2
                val destination = FloatArray(count * 2) { if (it % 2 == 0) face.canonical[it] * 1.7f + 300f else face.canonical[it] * 1.4f + 200f }
                assertMaps(face.canonical, destination, count)
            }
        }
    }

    @Test
    fun anEdgeOnFaceIsRefused() {
        assertFalse(faceTransform(floatArrayOf(0f, 0f, 10f, 0f, 20f, 0f), floatArrayOf(0f, 0f, 1f, 1f, 2f, 2f), 3, Matrix()))
    }
}
