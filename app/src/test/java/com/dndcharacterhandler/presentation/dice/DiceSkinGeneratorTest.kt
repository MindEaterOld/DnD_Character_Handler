package com.dndcharacterhandler.presentation.dice

import com.dndcharacterhandler.domain.model.CustomDiceSkin
import com.dndcharacterhandler.domain.model.DiceFontIds
import com.dndcharacterhandler.domain.model.DicePattern
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class DiceSkinGeneratorTest {
    private val base = CustomDiceSkin(
        id = "skin-1",
        name = "Mine",
        bodyColor = 0xFF202020.toInt(),
        edgeColor = 0xFF808080.toInt(),
        numberColor = 0xFFFFFFFF.toInt(),
        faceArt = setOf("D20"),
        numbersOverArt = false,
        updatedAt = 42L
    )

    private val skins = (0 until 1000).map { DiceSkinGenerator.generate(base, Random(it)) }

    @Test
    fun numbersReadOnTheFaces() {
        skins.forEach { skin ->
            val contrast = DiceSkinGenerator.contrast(skin.numberColor, skin.bodyColor)
            assertTrue("numbers $contrast in $skin", contrast >= DiceSkinGenerator.NUMBER_CONTRAST)
        }
    }

    @Test
    fun edgesShowButStayQuieterThanTheNumbers() {
        skins.forEach { skin ->
            val edges = DiceSkinGenerator.contrast(skin.edgeColor, skin.bodyColor)
            val numbers = DiceSkinGenerator.contrast(skin.numberColor, skin.bodyColor)
            assertTrue("edges $edges in $skin", edges >= DiceSkinGenerator.EDGE_CONTRAST.start - 0.1)
            assertTrue("edges $edges, numbers $numbers in $skin", edges < numbers)
        }
    }

    @Test
    fun patternsStayATexture() {
        skins.forEach { skin ->
            val (color, range) = when (val pattern = skin.pattern) {
                is DicePattern.Web -> pattern.color to DiceSkinGenerator.THREAD_CONTRAST
                is DicePattern.Marble -> pattern.color to DiceSkinGenerator.PATTERN_CONTRAST
                is DicePattern.Nebula -> pattern.color to DiceSkinGenerator.PATTERN_CONTRAST
                else -> return@forEach
            }
            // Quietened toward the faces where the numbers needed it, never louder than a texture.
            val contrast = DiceSkinGenerator.contrast(color, skin.bodyColor)
            assertTrue("pattern $contrast in $skin", contrast <= range.endInclusive + 0.1)
        }
    }

    @Test
    fun numbersReadOnEverythingUnderThem() {
        val unreadable = skins.filter { skin ->
            DiceSkinGenerator.weakest(skin.numberColor, skin.bodyColor, skin.pattern, skin.gloss, skin.bodyOpacity) != DiceSkinGenerator.Weak.NONE
        }
        unreadable.forEach { assertTrue("no outline in $it", it.numberOutlineColor != null) }
        assertTrue("${unreadable.size} skins need an outline to read", unreadable.size < skins.size / 20)
    }

    @Test
    fun outlinesStandOffTheNumbers() {
        skins.forEach { skin ->
            skin.numberOutlineColor?.let { outline ->
                val contrast = DiceSkinGenerator.contrast(outline, skin.numberColor)
                assertTrue("outline $contrast in $skin", contrast >= 3.0)
            }
        }
    }

    @Test
    fun onlyPlainDiceAreSeeThrough() {
        skins.filter { it.bodyOpacity < 1f }.forEach { assertEquals(DicePattern.None, it.pattern) }
    }

    @Test
    fun theGeneratorFindsTheShineUnderWhiteNumbers() {
        val white = 0xFFFFFFFF.toInt()
        val night = 0xFF141030.toInt()
        assertEquals(DiceSkinGenerator.Weak.NONE, DiceSkinGenerator.weakest(white, night, DicePattern.None, 0f, 1f))
        assertEquals(DiceSkinGenerator.Weak.SHINE, DiceSkinGenerator.weakest(white, night, DicePattern.None, 1f, 1f))
    }

    @Test
    fun theRestStaysInTheWorkshopsRanges() {
        skins.forEach { skin ->
            assertTrue(skin.font in DiceFontIds.all)
            assertTrue(skin.bodyOpacity in 0.3f..1f)
            assertTrue(skin.gloss in 0f..1f)
            assertTrue(skin.edgeWidth in 0f..3f)
            assertTrue(skin.numberScale in 0.7f..1.3f)
            assertTrue(skin.pattern !is DicePattern.Picture)
        }
    }

    @Test
    fun onlyTheLookChanges() {
        skins.forEach { skin ->
            assertEquals(base.id, skin.id)
            assertEquals(base.name, skin.name)
            assertEquals(base.faceArt, skin.faceArt)
            assertEquals(base.numbersOverArt, skin.numbersOverArt)
            assertEquals(base.updatedAt, skin.updatedAt)
        }
    }

    @Test
    fun theLooksVary() {
        assertTrue(skins.map { it.font }.toSet().size >= 8)
        assertTrue(skins.map { it.pattern::class }.toSet().size == 4)
        assertTrue(skins.map { it.bodyColor }.toSet().size > 900)
    }

    @Test
    fun theSameSeedGivesTheSameSkin() {
        assertEquals(DiceSkinGenerator.generate(base, Random(7)), DiceSkinGenerator.generate(base, Random(7)))
    }
}
