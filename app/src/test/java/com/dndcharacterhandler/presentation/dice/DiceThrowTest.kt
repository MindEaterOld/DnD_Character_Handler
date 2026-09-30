package com.dndcharacterhandler.presentation.dice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class DiceThrowTest {
    /** A die of [kind] lying with the face worth [value] on top. */
    private fun showing(kind: DieShapeKind, value: Int): DieBody {
        val shape = DieShapes.of(kind)
        val normal = shape.faces.single { it.value == value }.normal
        return DieBody(shape, Vec3.ZERO, Quat.rotationBetween(normal, Vec3.UP))
    }

    @Test
    fun helperPutsTheRequestedFaceOnTop() {
        for (kind in DieShapeKind.entries.filter { it != DieShapeKind.D4 }) {
            DieShapes.of(kind).faces.forEach { face ->
                assertEquals("$kind", face.value, showing(kind, face.value).topValue())
            }
        }
    }

    @Test
    fun d10ReadsZeroAsTen() {
        assertEquals(10, ThrownDie(DieType.D10, listOf(showing(DieShapeKind.D10, 0))).value())
        assertEquals(7, ThrownDie(DieType.D10, listOf(showing(DieShapeKind.D10, 7))).value())
    }

    @Test
    fun d100CombinesTensAndUnits() {
        fun d100(tens: Int, units: Int) =
            ThrownDie(DieType.D100, listOf(showing(DieShapeKind.D10_TENS, tens), showing(DieShapeKind.D10, units))).value()
        assertEquals("00 + 0", 100, d100(0, 0))
        assertEquals("00 + 5", 5, d100(0, 5))
        assertEquals("30 + 0", 30, d100(3, 0))
        assertEquals("90 + 9", 99, d100(9, 9))
        assertEquals("40 + 2", 42, d100(4, 2))
    }

    @Test
    fun otherDiceReadTheirTopFace() {
        assertEquals(20, ThrownDie(DieType.D20, listOf(showing(DieShapeKind.D20, 20))).value())
        assertEquals(1, ThrownDie(DieType.D6, listOf(showing(DieShapeKind.D6, 1))).value())
    }

    @Test
    fun selectionIsSpawnedInDieTypeOrderWithTwoBodiesPerD100() {
        val world = DiceWorld(Random(3))
        world.setAspectRatio(2.1)
        val dice = world.spawnSelection(
            selection = mapOf(DieType.D100 to 1, DieType.D20 to 2, DieType.D6 to 0, DieType.D4 to 1),
            handPosition = Vec3(0.0, world.handHeight, 0.0)
        )
        assertEquals(listOf(DieType.D4, DieType.D20, DieType.D20, DieType.D100), dice.map { it.type })
        assertEquals(
            listOf(DieShapeKind.D10_TENS, DieShapeKind.D10),
            dice.last().bodies.map { it.shape.kind }
        )
        // Every body belongs to exactly one die, in the world's order.
        assertEquals(world.bodies, dice.flatMap { it.bodies })
        assertTrue(world.bodies.all { it.held })
    }
}
