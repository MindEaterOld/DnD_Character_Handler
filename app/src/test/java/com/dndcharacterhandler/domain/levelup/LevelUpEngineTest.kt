package com.dndcharacterhandler.domain.levelup

import com.dndcharacterhandler.data.catalog.CharacterCatalogParser
import com.dndcharacterhandler.domain.model.CharacterBundle
import com.dndcharacterhandler.domain.model.CharacterCatalog
import com.dndcharacterhandler.domain.model.Feature
import com.dndcharacterhandler.domain.model.defaultCharacterBundle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test
import java.io.File

class LevelUpEngineTest {
    companion object {
        lateinit var catalog: CharacterCatalog
        lateinit var engine: LevelUpEngine

        @BeforeClass
        @JvmStatic
        fun load() {
            val file = listOf("src/main/assets/character_catalog.json", "app/src/main/assets/character_catalog.json")
                .map(::File).first { it.exists() }
            catalog = CharacterCatalogParser.parse(file.readText())
            engine = LevelUpEngine(catalog)
        }
    }

    private val fighter get() = catalog.classesByIdentifier.getValue("fighter")
    private val wizard get() = catalog.classesByIdentifier.getValue("wizard")

    private fun newCharacter(strength: Int = 15, constitution: Int = 14, intelligence: Int = 10): CharacterBundle {
        val bundle = defaultCharacterBundle(now = 0)
        return bundle.copy(
            character = bundle.character.copy(
                level = 1, maxHp = 1, currentHp = 1,
                strength = strength, dexterity = 12, constitution = constitution,
                intelligence = intelligence, wisdom = 10, charisma = 8
            ),
            features = emptyList()
        )
    }

    /** Answers every open page the simple way (first options, average hit points) until the draft is complete. */
    private fun complete(bundle: CharacterBundle, start: LevelUpDraft, subclass: String? = null, custom: (LevelUpPage) -> LevelUpAnswer? = { null }): LevelUpDraft {
        var draft = start
        repeat(80) {
            val run = engine.run(bundle, draft)
            if (run.isComplete) return draft
            val page = run.pages.first { !engine.isAnswered(it, draft) }
            val answer = custom(page) ?: when (page) {
                is LevelUpPage.HitPoints -> LevelUpAnswer.HitPoints(HitPointMethod.AVERAGE, page.average)
                is LevelUpPage.Traits -> LevelUpAnswer.Traits(page.groups.mapIndexed { index, group ->
                    group.options.filter { !it.alreadyHas }.take(page.required[index]).map { it.key }.toSet()
                })
                is LevelUpPage.Items -> LevelUpAnswer.Items(page.options.filter { !it.known }.take(page.required).map { it.feature.id })
                is LevelUpPage.AbilityScores -> LevelUpAnswer.AbilityScores(mapOf(page.abilities.first() to page.required.coerceAtMost(page.cap)))
                is LevelUpPage.Subclass -> LevelUpAnswer.Subclass(
                    page.options.firstOrNull { it.identifier == subclass }?.id ?: page.options.first().id
                )
                else -> error("unexpected open page $page")
            }
            draft = draft.copy(answers = draft.answers + (page.key to answer))
        }
        error("the draft never completed")
    }

    private fun CharacterBundle.featureNames() = features.map { catalog.featuresById[it.catalogId]?.name?.en ?: it.name }

    @Test
    fun aNewFighterIsBuiltFromLevelOneToThree() {
        val bundle = newCharacter()
        val setupOnly = LevelUpDraft(targetLevel = 3)
        assertFalse(engine.run(bundle, setupOnly).isComplete)
        assertTrue(engine.run(bundle, setupOnly).pages.single() is LevelUpPage.Setup)

        val draft = complete(bundle, setupOnly.copy(setup = LevelUpSetup(fighter.id, keepExistingLevels = false)), subclass = "battle-master")
        val pages = engine.run(bundle, draft).pages
        // Level 1: maximum hit points, no class page (the setup picked the class); level 3: the subclass.
        assertTrue(pages.filterIsInstance<LevelUpPage.HitPoints>().first().takesMaximum)
        assertEquals(listOf(2, 3), pages.filterIsInstance<LevelUpPage.ChooseClass>().map { it.characterLevel })
        assertEquals(1, pages.filterIsInstance<LevelUpPage.Subclass>().size)
        val maneuvers = pages.filterIsInstance<LevelUpPage.Items>().single { it.step.restriction?.subtype == "maneuver" }
        assertEquals(3, maneuvers.count)

        val result = engine.apply(bundle, draft, russian = false, now = 1)
        val character = result.character
        assertEquals(3, character.level)
        assertEquals(fighter.id, character.classes.single().classId)
        assertEquals("battle-master", catalog.subclasses.first { it.id == character.classes.single().subclassId }.identifier)
        // 10 + 6 + 6 hit points, +2 Constitution per level.
        assertEquals(28, character.maxHp)
        assertEquals(28, character.currentHp)
        assertTrue(character.strengthSaveProficient && character.constitutionSaveProficient)
        // Two Fighter skills, and one more from the Battle Master's Student of War.
        assertEquals(3, result.skills.count { it.isProficient })
        assertTrue("heavy armor training", character.armorProficiencies.contains("Heavy"))
        val names = result.featureNames()
        listOf("Second Wind", "Action Surge", "Combat Superiority", "Student of War").forEach { name ->
            assertTrue("$name among $names", name in names)
        }
        assertEquals(3, result.features.count { catalog.featuresById[it.catalogId]?.subtype == "maneuver" })
        // Uses become combat resources sized for level 3.
        val superiority = result.combatResources.single { catalog.featuresById[it.catalogId]?.name?.en == "Combat Superiority" }
        assertEquals(4, superiority.maximumUses)
        assertTrue(superiority.restoresOnShortRest)
        assertTrue("choices are kept for later", character.advancements.any { it.type == "ItemChoice" })
    }

    @Test
    fun multiclassingWarnsAboutRequirementsAndUsesTheReducedProficiencies() {
        val fighterThree = engine.apply(
            newCharacter(),
            complete(newCharacter(), LevelUpDraft(3, setup = LevelUpSetup(fighter.id, keepExistingLevels = false))),
            russian = false, now = 1
        )
        val draft = LevelUpDraft(targetLevel = 4, classPicks = mapOf(4 to wizard.id))
        val classPage = engine.run(fighterThree, draft).pages.filterIsInstance<LevelUpPage.ChooseClass>().single()
        assertTrue("Intelligence 10 is below 13", classPage.requirements.any { !it.isMet && it.classId == wizard.id })

        val result = engine.apply(fighterThree, complete(fighterThree, draft), russian = false, now = 2)
        val character = result.character
        assertEquals(4, character.level)
        assertEquals(listOf(3, 1), character.classes.map { it.levels })
        assertFalse("no saving throws from a second class", character.intelligenceSaveProficient)
        // d6 average (4) plus Constitution +2.
        assertEquals(fighterThree.character.maxHp + 6, character.maxHp)
        // A single caster class at level 1: two 1st-level slots.
        assertEquals("2,0,0,0,0,0,0,0,0", character.spellSlotMaximums)
        assertTrue(engine.run(fighterThree, draft).pages.filterIsInstance<LevelUpPage.Summary>().single().summary.unmetRequirements.isNotEmpty())
        assertTrue(character.characterClass.contains("/"))
    }

    @Test
    fun abilityScoreImprovementRaisesScoresOrTakesAFeat() {
        val start = newCharacter(strength = 15)
        val base = LevelUpDraft(4, setup = LevelUpSetup(fighter.id, keepExistingLevels = false))
        val withScores = complete(start, base) { page ->
            if (page is LevelUpPage.AbilityScores) LevelUpAnswer.AbilityScores(mapOf("str" to 1, "con" to 1)) else null
        }
        val raised = engine.apply(start, withScores, russian = false, now = 1).character
        assertEquals(16, raised.strength)
        assertEquals(15, raised.constitution)

        val withFeat = complete(start, base) { page ->
            if (page is LevelUpPage.AbilityScores && page.allowFeat) {
                LevelUpAnswer.Feat(page.feats.first { it.name.en == "Alert" || it.name.en == "Tough" }.id)
            } else {
                null
            }
        }
        val featPage = engine.run(start, withFeat).pages.filterIsInstance<LevelUpPage.AbilityScores>().first { it.allowFeat }
        assertTrue("feats are offered", featPage.feats.size > 20)
        val result = engine.apply(start, withFeat, russian = false, now = 1)
        assertTrue(result.featureNames().any { it == "Alert" || it == "Tough" })
    }

    @Test
    fun aFilledInCharacterOnlyRecordsItsClassAndGoesOn() {
        val bundle = newCharacter().let { b ->
            b.copy(
                character = b.character.copy(level = 5, maxHp = 44, currentHp = 30),
                features = listOf(Feature(name = "Rage", description = "", catalogId = null))
            )
        }
        val barbarian = catalog.classesByIdentifier.getValue("barbarian")
        val draft = LevelUpDraft(6, setup = LevelUpSetup(barbarian.id, keepExistingLevels = true))
        val setup = engine.run(bundle, draft).pages.first() as LevelUpPage.Setup
        assertTrue(setup.suggestKeep)
        val completed = complete(bundle, draft, subclass = "berserker")
        val pages = engine.run(bundle, completed).pages
        // Only level 6 is walked; hit points use the average.
        assertEquals(listOf(6), pages.filterIsInstance<LevelUpPage.HitPoints>().map { it.characterLevel })
        val result = engine.apply(bundle, completed, russian = true, now = 1)
        assertEquals(6, result.character.level)
        assertEquals(44 + 7 + 2, result.character.maxHp)
        assertEquals(30 + 7 + 2, result.character.currentHp)
        assertEquals(6, result.character.classes.single().levels)
        assertNotNull(result.character.classes.single().isOriginal)
        assertEquals("Варвар", result.character.characterClass)
    }
}
