package com.dndcharacterhandler.presentation.dice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class DiceShapesTest {
    @Test
    fun everyDieHasTheRightNumberOfFacesAndValues() {
        val expectedFaces = mapOf(
            DieShapeKind.D4 to 4,
            DieShapeKind.D6 to 6,
            DieShapeKind.D8 to 8,
            DieShapeKind.D10 to 10,
            DieShapeKind.D10_TENS to 10,
            DieShapeKind.D12 to 12,
            DieShapeKind.D20 to 20
        )
        expectedFaces.forEach { (kind, faces) ->
            val shape = DieShapes.of(kind)
            assertEquals("$kind faces", faces, shape.faces.size)
            val values = shape.vertexValues?.toList() ?: shape.faces.map { it.value }
            assertEquals("$kind values are unique", values.size, values.toSet().size)
        }
    }

    @Test
    fun oppositeFacesSumLikeRealDice() {
        for (kind in DieShapeKind.entries.filter { it != DieShapeKind.D4 }) {
            val shape = DieShapes.of(kind)
            val zeroBased = kind == DieShapeKind.D10 || kind == DieShapeKind.D10_TENS
            val expectedSum = if (zeroBased) shape.faces.size - 1 else shape.faces.size + 1
            shape.faces.forEach { face ->
                val opposite = shape.faces.single { (it.normal dot face.normal) < -0.999 }
                assertEquals("$kind ${face.value}", expectedSum, face.value + opposite.value)
            }
        }
    }

    @Test
    fun facesArePlanarAndPointOutward() {
        for (kind in DieShapeKind.entries) {
            val shape = DieShapes.of(kind)
            shape.faces.forEach { face ->
                val offset = face.normal dot face.center
                face.vertexIndices.forEach { index ->
                    assertTrue("$kind vertex off its face plane", abs((face.normal dot shape.vertices[index]) - offset) < 1e-6)
                }
                assertTrue("$kind normal points inward", offset > 0)
            }
        }
    }
}
