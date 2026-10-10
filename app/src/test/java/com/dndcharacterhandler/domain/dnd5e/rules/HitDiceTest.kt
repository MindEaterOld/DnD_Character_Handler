package com.dndcharacterhandler.domain.dnd5e.rules

import com.dndcharacterhandler.domain.dnd5e.model.CatalogClass
import com.dndcharacterhandler.domain.dnd5e.model.CatalogText
import com.dndcharacterhandler.domain.dnd5e.model.CharacterCatalog
import com.dndcharacterhandler.domain.dnd5e.model.CharacterClassEntry
import com.dndcharacterhandler.domain.dnd5e.model.defaultCharacterBundle
import org.junit.Assert.assertEquals
import org.junit.Test

class HitDiceTest {
    private fun catalogClass(id: String, hitDie: Int) = CatalogClass(
        id = id, identifier = id, name = CatalogText(id, id), book = "PHB 2024", hitDie = hitDie,
        primaryAbilities = emptyList(), primaryAbilitiesAll = true, spellcasting = null, advancement = emptyList()
    )

    private val catalog = CharacterCatalog.EMPTY.copy(
        classes = listOf(catalogClass("fighter", 10), catalogClass("paladin", 10), catalogClass("wizard", 6))
    )

    private val fighterWizard = listOf(
        CharacterClassEntry("fighter", levels = 5, isOriginal = true),
        CharacterClassEntry("wizard", levels = 2)
    )

    private fun character(classes: List<CharacterClassEntry>, spent: Int, level: Int = classes.sumOf { it.levels }) =
        defaultCharacterBundle(0).character.copy(level = level, classes = classes, spentHitDice = spent, hitDieSides = 8)

    @Test
    fun aCharacterWithoutClassesHasOnePoolOfItsDie() {
        val pools = hitDicePools(character(emptyList(), spent = 1, level = 3), catalog)
        assertEquals(listOf(HitDicePool(8, 3, 1)), pools)
    }

    @Test
    fun aMulticlassHasAPoolForEachSizeLargestFirst() {
        val pools = hitDicePools(character(fighterWizard, spent = 0), catalog)
        assertEquals(listOf(HitDicePool(10, 5, 0, listOf("fighter")), HitDicePool(6, 2, 0, listOf("wizard"))), pools)
    }

    @Test
    fun classesOfOneSizeShareAPool() {
        val classes = listOf(CharacterClassEntry("fighter", levels = 3), CharacterClassEntry("paladin", levels = 2))
        assertEquals(listOf(HitDicePool(10, 5, 0, listOf("fighter", "paladin"))), hitDicePools(character(classes, 0), catalog))
    }

    @Test
    fun aTotalTheClassesDontKeepIsSpreadOverThemInTurn() {
        // Spent before the dice were kept by class: six of seven, the fighter's five first.
        val classes = classesWithSpentHitDice(character(fighterWizard, spent = 6))
        assertEquals(listOf(5, 1), classes.map { it.spentHitDice })
    }

    @Test
    fun spendingTakesEachSizeFromItsClassesInTurn() {
        val classes = listOf(
            CharacterClassEntry("fighter", levels = 3, spentHitDice = 2),
            CharacterClassEntry("wizard", levels = 2),
            CharacterClassEntry("paladin", levels = 2)
        )
        val spent = spendClassHitDice(classes, catalog, mapOf(10 to 2, 6 to 1))
        assertEquals(listOf(3, 1, 1), spent.map { it.spentHitDice })
    }

    @Test
    fun spendingNeverTakesMoreThanAClassHas() {
        val spent = spendClassHitDice(fighterWizard, catalog, mapOf(6 to 5))
        assertEquals(listOf(0, 2), spent.map { it.spentHitDice })
    }

    @Test
    fun eachDieHealsItsRollAndConstitutionAtLeastOne() {
        assertEquals(7 + 2 + 3 + 2, hitDiceHealing(listOf(7, 3), constitutionModifier = 2))
        // A 1 with −1 would heal nothing: it heals 1.
        assertEquals(1 + 5, hitDiceHealing(listOf(1, 6), constitutionModifier = -1))
    }

    @Test
    fun theAverageCountsHalfTheDieAndAHalf() {
        assertEquals(5.5 + 2 + 3.5 + 2, averageHitDiceHealing(listOf(10, 6), constitutionModifier = 2), 0.0)
    }
}
