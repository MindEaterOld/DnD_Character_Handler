package com.dndcharacterhandler.domain.rules

import com.dndcharacterhandler.domain.model.AdvancementStep
import com.dndcharacterhandler.domain.model.CatalogClass
import com.dndcharacterhandler.domain.model.CatalogSubclass
import com.dndcharacterhandler.domain.model.CatalogText
import com.dndcharacterhandler.domain.model.CharacterCatalog
import org.junit.Assert.assertEquals
import org.junit.Test

class CatalogFormulaTextTest {
    private fun scale(identifier: String, vararg values: Pair<Int, String>) = AdvancementStep.ScaleValue(
        id = identifier, level = null, title = CatalogText(identifier, identifier), classRestriction = null,
        identifier = identifier, scaleType = "number", values = values.toMap(), units = ""
    )

    private val catalog = CharacterCatalog.EMPTY.copy(
        classes = listOf(
            CatalogClass(
                id = "b", identifier = "barbarian", name = CatalogText("Barbarian", "Варвар"), book = "PHB 2024",
                hitDie = 12, primaryAbilities = listOf("str"), primaryAbilitiesAll = true, spellcasting = null,
                advancement = listOf(scale("rages", 1 to "2", 3 to "3", 6 to "4", 12 to "5", 17 to "6"))
            ),
            CatalogClass(
                id = "d", identifier = "druid", name = CatalogText("Druid", "Друид"), book = "PHB 2024",
                hitDie = 8, primaryAbilities = listOf("wis"), primaryAbilitiesAll = true, spellcasting = null,
                advancement = emptyList()
            ),
            CatalogClass(
                id = "f", identifier = "fighter", name = CatalogText("Fighter", "Воин"), book = "PHB 2024",
                hitDie = 10, primaryAbilities = listOf("str", "dex"), primaryAbilitiesAll = false, spellcasting = null,
                advancement = emptyList()
            )
        ),
        subclasses = listOf(
            CatalogSubclass(
                id = "bm", identifier = "battle-master", classIdentifier = "fighter",
                name = CatalogText("Battle Master", "Мастер битвы"), text = CatalogText(), book = "PHB 2024",
                spellcasting = null,
                advancement = listOf(scale("superiority", 3 to "4d8", 7 to "5d8", 10 to "5d10", 15 to "6d10", 18 to "6d12"))
            )
        )
    )
    private val formulas = CatalogFormulaText(catalog)

    private fun context(
        classLevels: Map<String, Int> = emptyMap(),
        subclasses: Set<String> = emptySet(),
        wisdom: Int = 0,
        level: Int = classLevels.values.sum().coerceAtLeast(1)
    ) = FormulaContext(
        totalLevel = level,
        classLevels = classLevels,
        subclasses = subclasses,
        abilityModifiers = mapOf("wis" to wisdom),
        proficiencyBonus = proficiencyBonusForLevel(level)
    )

    @Test
    fun scaleValuesFollowTheClassLevel() {
        val text = "Rages: {=@scale.barbarian.rages}."
        assertEquals("Rages: 3.", formulas.render(text, russian = false, context(mapOf("barbarian" to 5))))
        assertEquals("Rages: 6.", formulas.render(text, russian = false, context(mapOf("barbarian" to 20))))
    }

    @Test
    fun withoutACharacterScalesShowTheirProgression() {
        assertEquals("2/3/4/5/6", formulas.render("{=@scale.barbarian.rages}", russian = true, context = null))
        // A class the character doesn't have counts as unknown too.
        assertEquals("2/3/4/5/6", formulas.render("{=@scale.barbarian.rages}", russian = true, context(mapOf("druid" to 4))))
    }

    @Test
    fun subclassScalesUseTheParentClassLevelAndProperties() {
        val battleMaster = context(mapOf("fighter" to 10), subclasses = setOf("battle-master"))
        assertEquals("d10", formulas.render("{=@scale.battle-master.superiority.die}", false, battleMaster))
        assertEquals("5", formulas.render("{=@scale.battle-master.superiority.number}", false, battleMaster))
        assertEquals("5d10", formulas.render("{=@scale.battle-master.superiority}", false, battleMaster))
        // A fighter of another subclass doesn't have the dice: the label shows the progression.
        assertEquals("d8/d10/d12", formulas.render("{=@scale.battle-master.superiority.die}", false, context(mapOf("fighter" to 10))))
    }

    @Test
    fun abilityModifiersProficiencyAndLevelsAreFilledIn() {
        assertEquals("1d8 + 3", formulas.render("{=1d8 + @abilities.wis.mod}", false, context(wisdom = 3)))
        assertEquals("1d8 − 1", formulas.render("{=1d8 + @abilities.wis.mod}", false, context(wisdom = -1)))
        assertEquals("7", formulas.render("{=4 + @prof}", false, context(level = 5)))
        assertEquals("2", formulas.render("{=floor(@classes.druid.levels / 3)}", false, context(mapOf("druid" to 7))))
        assertEquals("4", formulas.render("{=max(1, @abilities.wis.mod + 2)}", false, context(wisdom = 2)))
    }

    @Test
    fun unknownPartsBecomeReadableLabels() {
        assertEquals("1d8 + модификатор Мудрости", formulas.render("{=1d8 + @abilities.wis.mod}", true, null))
        assertEquals("(уровень Друида / 3)", formulas.render("{=floor(@classes.druid.levels / 3)}", true, null))
        assertEquals("4 + Proficiency Bonus", formulas.render("{=4 + @prof}", false, null))
        assertEquals("Plain text stays as it is.", formulas.render("Plain text stays as it is.", false, null))
    }
}
