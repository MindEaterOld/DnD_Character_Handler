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
    fun faceOutlinesHaveEveryCornerAroundTheCentre() {
        for (kind in DieShapeKind.entries) {
            DieShapes.of(kind).faces.forEach { face ->
                val outline = face.outline
                assertEquals("$kind outline corners", face.vertexIndices.size * 2, outline.size)
                // The mapping points are the outline's first corners.
                face.canonical.indices.forEach { assertEquals("$kind canonical", outline[it], face.canonical[it], 1e-4f) }
                // Convex and wound one way around the centre (0, 0), where the number sits.
                val corners = outline.size / 2
                val turns = (0 until corners).map { i ->
                    val next = (i + 1) % corners
                    outline[2 * i] * outline[2 * next + 1] - outline[2 * i + 1] * outline[2 * next]
                }
                assertTrue("$kind outline doesn't go around the centre", turns.all { it > 0 } || turns.all { it < 0 })
            }
        }
    }

    @Test
    fun rotationBetweenTurnsOneDirectionOntoAnother() {
        val directions = listOf(
            Vec3.UP, -Vec3.UP, Vec3(1.0, 0.0, 0.0), Vec3(-1.0, 0.0, 0.0),
            Vec3(0.3, -0.8, 0.5).normalized(), Vec3(-0.6, 0.1, -0.7).normalized()
        )
        for (from in directions) for (to in directions) {
            val turned = Quat.rotationBetween(from, to).rotate(from)
            assertTrue("$from -> $to gave $turned", (turned - to).length < 1e-9)
        }
        val quarter = Quat.axisAngle(Vec3(0.0, 0.0, 2.0), Math.PI / 2).rotate(Vec3(1.0, 0.0, 0.0))
        assertTrue("quarter turn about z gave $quarter", (quarter - Vec3(0.0, 1.0, 0.0)).length < 1e-9)
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
                // The number's "up" lies in the face.
                assertTrue("$kind label up is off the face", abs(face.up dot face.normal) < 1e-9 && abs(face.up.length - 1) < 1e-9)
            }
        }
    }
}
