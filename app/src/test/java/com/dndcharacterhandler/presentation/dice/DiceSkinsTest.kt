package com.dndcharacterhandler.presentation.dice

import com.dndcharacterhandler.data.dice.decodeDiceSkins
import com.dndcharacterhandler.data.dice.encodeDiceSkins
import com.dndcharacterhandler.domain.model.CustomDiceSkin
import com.dndcharacterhandler.domain.model.DiceFontIds
import com.dndcharacterhandler.domain.model.DicePattern
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class DiceSkinsTest {
    @Test
    fun skinsRoundTripThroughTheirFile() {
        val skins = listOf(
            CustomDiceSkin(
                id = "a", name = "Obsidian", bodyColor = 0xFF141217.toInt(), bodyOpacity = 0.55f, gloss = 0.7f,
                edgeColor = 0xFFE0B548.toInt(), edgeWidth = 1.2f, numberColor = 0xFFF2EBDD.toInt(),
                numberOutlineColor = 0xFF000000.toInt(), font = DiceFontIds.NEW_ROCKER, numberScale = 1.15f,
                pattern = DicePattern.Nebula(0xFF6A3FA0.toInt(), 0xFF8AD1E8.toInt(), seed = 42),
                faceArt = setOf("D20", "D6"), numbersOverArt = false, updatedAt = 7
            ),
            CustomDiceSkin(
                id = "b", name = "Wood", bodyColor = 0xFFC9824B.toInt(), edgeColor = 0xFF3B1F52.toInt(),
                numberColor = 0xFF141217.toInt(), pattern = DicePattern.Picture(scale = 2f, rotation = 45f, strength = 0.8f)
            ),
            CustomDiceSkin(id = "c", name = "Plain", bodyColor = 1, edgeColor = 2, numberColor = 3, pattern = DicePattern.Marble(4, 5)),
            CustomDiceSkin(id = "d", name = "Web", bodyColor = 1, edgeColor = 2, numberColor = 3, pattern = DicePattern.Web(9))
        )
        assertEquals(skins, decodeDiceSkins(encodeDiceSkins(skins)))
    }

    @Test
    fun aDamagedFileKeepsWhatItCanAndSaneValues() {
        val skins = decodeDiceSkins("""{"skins":[{"name":"no id"},{"id":"x","name":"X","bodyOpacity":5,"font":"comic sans","pattern":{"type":"plaid"}}]}""")
        val skin = skins.single()
        assertEquals(1f, skin.bodyOpacity)
        assertEquals(DiceFontIds.APP, skin.font)
        assertEquals(DicePattern.None, skin.pattern)
    }

    @Test
    fun patternsComeOutTheSameForTheSameSeed() {
        val one = DicePatterns.marble(0xFFF2EBDD.toInt(), 0xFF141217.toInt(), seed = 3, size = 64)
        assertEquals(64 * 64, one.size)
        assertTrue(one.contentEquals(DicePatterns.marble(0xFFF2EBDD.toInt(), 0xFF141217.toInt(), seed = 3, size = 64)))
        assertTrue(!one.contentEquals(DicePatterns.marble(0xFFF2EBDD.toInt(), 0xFF141217.toInt(), seed = 4, size = 64)))
        // Veins: both colours show up.
        assertTrue(one.distinct().size > 20)
        val nebula = DicePatterns.nebula(0xFF141217.toInt(), 0xFF6A3FA0.toInt(), 0xFF8AD1E8.toInt(), seed = 3, size = 64)
        assertTrue(nebula.all { it ushr 24 == 0xFF })
    }

    @Test
    fun everyFaceHasACellOfItsOwnThatHoldsIt() {
        DieShapeKind.entries.forEach { kind ->
            val shape = DieShapes.of(kind)
            val cells = shape.faces.indices.map { DiceFaceAtlas.cellBounds(kind, it, DiceFaceAtlas.SIZE) }
            assertEquals("$kind cells", shape.faces.size, cells.toSet().size)
            cells.forEach { (left, top, right, bottom) ->
                assertTrue("$kind inside", left >= 0 && top >= 0 && right <= DiceFaceAtlas.SIZE && bottom <= DiceFaceAtlas.SIZE)
                assertEquals(right - left, bottom - top)
            }
            // A face fits its cell with room to spare (the margin to paint over).
            shape.faces.forEach { face ->
                assertTrue("$kind face inside the cell", face.outline.all { abs(it) < DiceFaceAtlas.EXTENT })
            }
        }
        assertNotEquals(DiceFaceAtlas.columns(DieShapeKind.D4), DiceFaceAtlas.columns(DieShapeKind.D20))
    }
}
