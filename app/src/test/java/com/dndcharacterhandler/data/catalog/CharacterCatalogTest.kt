package com.dndcharacterhandler.data.catalog

import com.dndcharacterhandler.data.repository.featureCatalogItems
import com.dndcharacterhandler.domain.model.AdvancementStep
import com.dndcharacterhandler.domain.model.CharacterCatalog
import com.dndcharacterhandler.domain.model.ClassRestriction
import com.dndcharacterhandler.domain.model.FeatureCatalogGroup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test
import java.io.File

/** Checks the shipped character_catalog.json (built by tools/foundry_catalog/convert.py). */
class CharacterCatalogTest {
    companion object {
        lateinit var catalog: CharacterCatalog

        @BeforeClass
        @JvmStatic
        fun load() {
            // Unit tests run from the module directory; allow the repository root too.
            val file = listOf("src/main/assets/character_catalog.json", "app/src/main/assets/character_catalog.json")
                .map(::File).first { it.exists() }
            catalog = CharacterCatalogParser.parse(file.readText())
        }
    }

    private fun characterClass(identifier: String) = catalog.classesByIdentifier.getValue(identifier)

    private inline fun <reified T : AdvancementStep> List<AdvancementStep>.steps() = filterIsInstance<T>()

    @Test
    fun hasThePlayersHandbookClassesAndTheirSubclasses() {
        val phb = listOf("barbarian", "bard", "cleric", "druid", "fighter", "monk", "paladin", "ranger", "rogue", "sorcerer", "warlock", "wizard")
        phb.forEach { identifier ->
            val characterClass = characterClass(identifier)
            assertEquals("$identifier book", "PHB 2024", characterClass.book)
            val subclasses = catalog.subclasses.count { it.classIdentifier == identifier && it.book == "PHB 2024" }
            assertEquals("$identifier PHB subclasses", 4, subclasses)
        }
        assertNotNull("artificer from the Eberron supplement", catalog.classesByIdentifier["artificer"])
    }

    @Test
    fun classesCarryTheirProgression() {
        val fighter = characterClass("fighter")
        assertEquals(10, fighter.hitDie)
        assertEquals(listOf("str", "dex"), fighter.primaryAbilities)
        assertTrue("any one primary ability is enough", !fighter.primaryAbilitiesAll)
        assertEquals(listOf(4, 6, 8, 12, 14, 16, 19), fighter.advancement.steps<AdvancementStep.AbilityScoreImprovement>().mapNotNull { it.level }.sorted())
        assertEquals(3, fighter.advancement.steps<AdvancementStep.Subclass>().single().level)

        // Multiclassing: heavy armor only for a first-level fighter, a lighter set when multiclassing.
        val armor = fighter.advancement.steps<AdvancementStep.Trait>().filter { step -> step.grants.any { it.startsWith("armor:") } }
        assertTrue(armor.any { it.classRestriction == ClassRestriction.PRIMARY && "armor:hvy" in it.grants })
        assertTrue(armor.any { it.classRestriction == ClassRestriction.SECONDARY && "armor:hvy" !in it.grants })

        val rages = characterClass("barbarian").advancement.steps<AdvancementStep.ScaleValue>().single { it.identifier == "rages" }
        assertEquals("2", rages.valueAt(1))
        assertEquals("3", rages.valueAt(5))
        assertEquals("6", rages.valueAt(20))

        val battleMaster = catalog.subclassesByIdentifier.getValue("battle-master")
        val superiority = battleMaster.advancement.steps<AdvancementStep.ScaleValue>().single { it.identifier == "superiority" }
        assertEquals("5d10", superiority.valueAt(10))
        val maneuvers = battleMaster.advancement.steps<AdvancementStep.ItemChoice>().single()
        assertEquals(mapOf(3 to 3, 7 to 2, 10 to 2, 15 to 2), maneuvers.counts)
        assertEquals("maneuver", maneuvers.restriction?.subtype)
        assertTrue("there are maneuvers to pick from", catalog.features.count { it.subtype == "maneuver" } >= 16)

        assertEquals("pact", characterClass("warlock").spellcasting?.progression)
        assertEquals("third", catalog.subclassesByIdentifier.getValue("eldritch-knight").spellcasting?.progression)
    }

    @Test
    fun everyGrantedItemIsInTheCatalog() {
        val steps = catalog.classes.flatMap { it.advancement } + catalog.subclasses.flatMap { it.advancement } +
            catalog.species.flatMap { it.advancement } + catalog.backgrounds.flatMap { it.advancement } +
            catalog.features.flatMap { it.advancement }
        val items = steps.flatMap {
            when (it) {
                is AdvancementStep.ItemGrant -> it.items
                is AdvancementStep.ItemChoice -> it.items
                else -> emptyList()
            }
        }
        assertTrue(items.size > 500)
        items.forEach { id -> assertNotNull("granted item $id", catalog.featuresById[id]) }
        steps.flatMap { (it as? AdvancementStep.ItemGrant)?.spells.orEmpty() }.forEach { id ->
            assertNotNull("granted spell $id", catalog.spells[id])
        }
        catalog.legacyIds.values.forEach { id -> assertNotNull("legacy target $id", catalog.featuresById[id]) }
    }

    @Test
    fun everyProficiencyAndLanguageHasBothNames() {
        // Picked on Character Wizard pages: an untranslated one would show in English in Russian mode.
        val untranslated = catalog.traits.values.filter { it.name.ru.isBlank() || it.name.en.isBlank() }.map { it.key }
        assertTrue("add them to tools/foundry_catalog/translations/ru_traits.json: $untranslated", untranslated.isEmpty())
    }

    @Test
    fun textsAreCleanOfFoundryMarkup() {
        catalog.features.forEach { feature ->
            assertTrue("${feature.id} has a Russian name", feature.name.ru.isNotBlank())
            for (text in listOf(feature.text.en, feature.text.ru)) {
                // Also enricher options that leaked into the text ("Unconscious apply=false").
                listOf("@UUID[", "[[/", "&Reference[", "&reference[", "apply=", "@Embed[", "<p>", "</").forEach { markup ->
                    assertTrue("${feature.id} still has $markup", markup !in text)
                }
                // Formulas are well-formed tokens: every {= is closed.
                assertEquals("${feature.id} formula tokens", text.split("{=").size - 1, Regex("""\{=[^{}]*\}""").findAll(text).count())
            }
        }
    }

    @Test
    fun spellsAreOnTheirListsWithCleanTexts() {
        val wizardList = catalog.spells.values.filter { "class:wizard" in it.lists }
        assertTrue("the wizard's list", wizardList.size > 150)
        assertTrue(wizardList.any { it.name.en == "Fireball" && it.level == 3 })
        catalog.spells.values.forEach { spell ->
            assertTrue("${spell.id} has a Russian name", spell.name.ru.isNotBlank())
            for (text in listOf(spell.text.en, spell.text.ru)) {
                listOf("@UUID[", "[[", "&Reference[", "&reference[", "apply=", "@Embed[", "<p>", "</", "{=").forEach { markup ->
                    assertTrue("${spell.id} still has $markup", markup !in text)
                }
            }
        }
        // Damage types read as Foundry shows them.
        val fireball = catalog.spells.values.first { it.name.en == "Fireball" }
        assertTrue(fireball.text.ru, "8d6 Огонь" in fireball.text.ru)
        assertEquals("150 feet", fireball.range)
    }

    @Test
    fun addFeatureCatalogKeepsOldIdsAndGroups() {
        val items = featureCatalogItems(catalog)
        assertEquals(catalog.features.size, items.size)
        val rage = items.single { it.ruName == "Ярость" && it.group == FeatureCatalogGroup.CLASS }
        assertEquals("Rage", rage.name)
        assertEquals("Варвар", rage.ruCategory)
        assertTrue("the old SRD id of Rage still finds it", rage.legacyIds.any { it.startsWith("CLASS:") })
        val invocation = items.first { it.group == FeatureCatalogGroup.OPTION && it.ruCategory == "Потусторонняя инвокация" }
        assertNotNull(invocation)
        // Entries only published in Russian still show a name in English mode.
        assertTrue(items.all { it.name.isNotBlank() })
    }

    @Test
    fun spellCardsComeFromThe2024Catalog() {
        val items = com.dndcharacterhandler.data.repository.spellCatalogItems(catalog)
        val fireball = items.single { it.name == "Fireball" }
        assertEquals("Огненный шар", fireball.ruName)
        assertEquals("Evocation", fireball.school)
        assertEquals(3, fireball.level)
        assertEquals("8d6", fireball.damageBase)
        assertEquals("Fire", fireball.damageType)
        assertEquals("DEX", fireball.saveAbility)
        assertEquals("half", fireball.saveEffect)
        assertEquals("sphere, 20 ft", fireball.areaOfEffect)
        assertTrue(fireball.availableClasses, "Wizard" in fireball.availableClasses && "Sorcerer" in fireball.availableClasses)
        // The higher-level paragraph has its own field, as on the SRD cards.
        assertTrue(fireball.higherLevelDescription.isNotBlank() && fireball.ruHigherLevel.isNotBlank())
        assertFalse("Using a Higher-Level" in fireball.description)
        val cure = items.single { it.name == "Cure Wounds" }
        assertEquals("2d8", cure.healBase)
        assertTrue(cure.healBonusIsModifier)
        val bolt = items.single { it.name == "Fire Bolt" }
        assertEquals("ranged", bolt.attackType)
        assertTrue(bolt.damage.startsWith("1: 1d10"))
    }
}
