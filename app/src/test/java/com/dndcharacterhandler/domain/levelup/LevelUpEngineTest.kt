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
                is LevelUpPage.Spells -> LevelUpAnswer.Items(page.options.filter { !it.known }.take(page.required).map { it.spell.id })
                is LevelUpPage.AbilityScores -> LevelUpAnswer.AbilityScores(mapOf(page.abilities.first() to page.required.coerceAtMost(page.cap)))
                is LevelUpPage.Subclass -> LevelUpAnswer.Subclass(
                    page.options.firstOrNull { it.identifier == subclass }?.id ?: page.options.first().id
                )
                is LevelUpPage.BaseAbilities -> LevelUpAnswer.BaseAbilities(AbilityMethod.KEEP, page.currentScores)
                is LevelUpPage.Species, is LevelUpPage.Background -> LevelUpAnswer.Origin(null)
                is LevelUpPage.Equipment -> LevelUpAnswer.Equipment(0)
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

    @Test
    fun aNewCharacterGetsScoresSpeciesBackgroundAndStartingEquipment() {
        val bundle = newCharacter()
        val elf = catalog.species.first { it.identifier == "elf" && it.book == "PHB 2024" }
        val acolyte = catalog.backgrounds.first { it.identifier == "acolyte" && it.book == "PHB 2024" }
        val start = LevelUpDraft(
            targetLevel = 1,
            setup = LevelUpSetup(fighter.id, keepExistingLevels = false),
            answers = mapOf(
                "abilities" to LevelUpAnswer.BaseAbilities(
                    AbilityMethod.STANDARD_ARRAY,
                    mapOf("str" to 15, "dex" to 13, "con" to 14, "int" to 8, "wis" to 12, "cha" to 10)
                ),
                "species" to LevelUpAnswer.Origin(elf.id),
                "background" to LevelUpAnswer.Origin(acolyte.id)
            )
        )
        val draft = complete(bundle, start) { page ->
            // The background's +2/+1, only to Intelligence, Wisdom or Charisma.
            if (page is LevelUpPage.AbilityScores && !page.allowFeat) LevelUpAnswer.AbilityScores(mapOf("wis" to 2, "cha" to 1)) else null
        }
        val pages = engine.run(bundle, draft).pages
        // Scores, species and background come before the first class level; the equipment after it.
        val order = pages.map { it::class.simpleName }
        assertEquals(listOf("Setup", "BaseAbilities", "Species"), order.take(3))
        assertTrue(order.indexOf("Background") < order.indexOf("HitPoints"))
        assertEquals(listOf("equipment:${fighter.id}", "equipment:${acolyte.id}"), pages.filterIsInstance<LevelUpPage.Equipment>().map { it.key })
        val background = pages.filterIsInstance<LevelUpPage.AbilityScores>().single()
        assertEquals(listOf("int", "wis", "cha"), background.abilities)
        assertEquals(3, background.required)
        // The Fighter: option A, option B, or 155 GP.
        val fighterEquipment = pages.filterIsInstance<LevelUpPage.Equipment>().first().options
        assertEquals(3, fighterEquipment.size)
        assertEquals(mapOf("gp" to 155), fighterEquipment.last().coins)

        val result = engine.apply(bundle, draft, russian = false, now = 1)
        val character = result.character
        assertEquals("Elf", character.race)
        assertEquals("Acolyte", character.background)
        assertEquals(30, character.speed)
        assertEquals(
            listOf(15, 13, 14, 8, 14, 11),
            listOf(character.strength, character.dexterity, character.constitution, character.intelligence, character.wisdom, character.charisma)
        )
        // 10 + Constitution +2 at 1st level.
        assertEquals(12, character.maxHp)
        val darkvision = result.features.single { it.name == "Darkvision" }
        assertTrue(darkvision.description.contains("60 feet"))
        assertTrue("elf traits", result.featureNames().containsAll(listOf("Fey Ancestry", "Keen Senses", "Trance")))
        assertTrue("an elven lineage", result.features.any { catalog.featuresById[it.catalogId]?.name?.en == "High Elf" })
        assertTrue("the origin feat", result.features.any { it.catalogId == "fZm3Di2wqEdQcn3E" })
        // Insight and Religion from the Acolyte; Keen Senses then offers Perception first.
        val skills = result.skills.filter { it.isProficient }.map { it.name }
        assertTrue(skills.toString(), skills.containsAll(listOf("skill_insight", "skill_religion", "skill_perception")))
        assertTrue(character.toolProficiencies, character.toolProficiencies.contains("Calligrapher"))
        // Option A of the Fighter and of the Acolyte: 4 + 8 GP.
        val inventory = result.inventoryItems.associate { it.name to it.quantity }
        assertEquals(8, inventory["Javelin"])
        assertTrue(inventory.keys.toString(), inventory.keys.containsAll(listOf("Chain Mail", "Greatsword", "Book", "Robe")))
        assertEquals(12, character.goldPieces)
        assertTrue(character.advancements.any { it.type == "Species" && it.sourceId == elf.id })
        assertTrue(character.advancements.any { it.type == "Abilities" && it.value.startsWith("method=STANDARD_ARRAY") })
    }

    @Test
    fun abilityMethodsAreChecked() {
        val scores = mapOf("str" to 15, "dex" to 14, "con" to 13, "int" to 12, "wis" to 10, "cha" to 8)
        assertTrue(engine.validAbilities(LevelUpAnswer.BaseAbilities(AbilityMethod.STANDARD_ARRAY, scores)))
        assertFalse(engine.validAbilities(LevelUpAnswer.BaseAbilities(AbilityMethod.STANDARD_ARRAY, scores + ("cha" to 15))))
        // 9 + 9 + 9 = 27 points; one more is over the budget.
        val bought = mapOf("str" to 15, "dex" to 15, "con" to 15, "int" to 8, "wis" to 8, "cha" to 8)
        assertTrue(engine.validAbilities(LevelUpAnswer.BaseAbilities(AbilityMethod.POINT_BUY, bought)))
        assertFalse(engine.validAbilities(LevelUpAnswer.BaseAbilities(AbilityMethod.POINT_BUY, bought + ("int" to 9))))
        assertFalse(engine.validAbilities(LevelUpAnswer.BaseAbilities(AbilityMethod.POINT_BUY, bought + ("int" to 16))))
        val rolls = listOf(17, 9, 12, 12, 14, 6)
        val rolled = mapOf("str" to 17, "dex" to 14, "con" to 12, "int" to 9, "wis" to 12, "cha" to 6)
        assertTrue(engine.validAbilities(LevelUpAnswer.BaseAbilities(AbilityMethod.ROLL, rolled, rolls)))
        assertFalse(engine.validAbilities(LevelUpAnswer.BaseAbilities(AbilityMethod.ROLL, rolled + ("cha" to 18), rolls)))
        assertFalse("six throws", engine.validAbilities(LevelUpAnswer.BaseAbilities(AbilityMethod.ROLL, rolled, rolls.take(5))))
    }

    @Test
    fun aToolOfChoiceDefaultsToTheOneTheBackgroundTaught() {
        val bundle = newCharacter()
        val artisan = catalog.backgrounds.first { it.identifier == "artisan" && it.book == "PHB 2024" }
        val start = LevelUpDraft(
            targetLevel = 1,
            setup = LevelUpSetup(fighter.id, keepExistingLevels = false),
            answers = mapOf("background" to LevelUpAnswer.Origin(artisan.id))
        )
        val draft = complete(bundle, start) { page ->
            when {
                page is LevelUpPage.AbilityScores -> LevelUpAnswer.AbilityScores(mapOf("str" to 2, "int" to 1))
                // The background's single tool (the Crafter feat then picks three more).
                page is LevelUpPage.Traits && page.required == listOf(1) && page.groups.single().options.any { it.key == "tool:art:smith" } ->
                    LevelUpAnswer.Traits(listOf(setOf("tool:art:smith")))
                else -> null
            }
        }
        val run = engine.run(bundle, draft)
        val equipment = run.pages.filterIsInstance<LevelUpPage.Equipment>().single { it.key == "equipment:${artisan.id}" }
        val choice = equipment.options.first().choices.single()
        assertEquals("tool:art:smith", choice.suggested)
        assertTrue("every artisan's tool", choice.options.size > 10)
        // Not a spell choice: the artisan's tool item comes with the equipment.
        assertTrue(run.pages.filterIsInstance<LevelUpPage.Summary>().single().summary.spellChoices.isEmpty())
        val result = engine.apply(bundle, draft, russian = false, now = 1)
        assertTrue(result.inventoryItems.map { it.name }.toString(), result.inventoryItems.any { it.name == "Smith's Tools" })
        assertEquals(2, result.inventoryItems.single { it.name == "Pouch" }.quantity)
    }

    @Test
    fun aSpeciesStepOfALaterLevelComesWithThatLevel() {
        val bundle = newCharacter()
        val goliath = catalog.species.first { it.identifier == "goliath" && it.book == "PHB 2024" }
        val start = LevelUpDraft(
            targetLevel = 4,
            setup = LevelUpSetup(fighter.id, keepExistingLevels = false),
            answers = mapOf("species" to LevelUpAnswer.Origin(goliath.id))
        )
        val fourth = engine.apply(bundle, complete(bundle, start), russian = false, now = 1)
        assertFalse("Large Form" in fourth.featureNames())
        assertTrue("Powerful Build" in fourth.featureNames())
        assertEquals(35, fourth.character.speed)
        val fifth = engine.apply(fourth, complete(fourth, LevelUpDraft(5)), russian = false, now = 2)
        assertTrue("Large Form" in fifth.featureNames())
    }

    private fun pageSpells(page: LevelUpPage.Spells) = page.options.map { it.spell }

    @Test
    fun aNewWizardPicksCantripsAndTheSpellbookFromItsList() {
        val bundle = newCharacter(intelligence = 15)
        val draft = complete(bundle, LevelUpDraft(1, setup = LevelUpSetup(wizard.id, keepExistingLevels = false)))
        val run = engine.run(bundle, draft)
        val spellPages = run.pages.filterIsInstance<LevelUpPage.Spells>()
        val cantrips = spellPages.first { it.step.restriction?.level == "0" && it.source.en == "Wizard" }
        assertEquals(3, cantrips.count)
        assertTrue(pageSpells(cantrips).all { it.level == 0 && "class:wizard" in it.lists })
        assertTrue("Fire Bolt" in pageSpells(cantrips).map { it.name.en })
        val spellbook = spellPages.first { it.step.restriction?.level == "available" }
        assertEquals(6, spellbook.count)
        // Only 1st-level slots at 1st level.
        assertTrue(pageSpells(spellbook).all { it.level == 1 && "class:wizard" in it.lists })
        assertTrue(run.pages.filterIsInstance<LevelUpPage.Summary>().single().summary.spellChoices.isEmpty())

        val result = engine.apply(bundle, draft, russian = false, now = 1)
        val learned = run.pages.filterIsInstance<LevelUpPage.Summary>().single().summary.spellsLearned
        assertEquals(9, learned.size)
        assertEquals(9, result.spells.size)
        // Cantrips are at hand; the spellbook's spells wait to be prepared.
        assertTrue(result.spells.filter { it.level == 0 }.all { it.isPrepared })
        assertTrue(result.spells.filter { it.level == 1 }.none { it.isPrepared })
        assertTrue(result.spells.all { it.description.isNotBlank() && it.range.isNotBlank() })
        assertTrue(result.character.advancements.any { it.type == "SpellChoice" })
    }

    @Test
    fun spellChoicesReachTheHighestSlotLevelTheClassHas() {
        val warlock = catalog.classesByIdentifier.getValue("warlock")
        val bundle = newCharacter()
        val draft = complete(bundle, LevelUpDraft(3, setup = LevelUpSetup(warlock.id, keepExistingLevels = false)))
        val pages = engine.run(bundle, draft).pages.filterIsInstance<LevelUpPage.Spells>()
            .filter { it.step.restriction?.level.isNullOrBlank() || it.step.restriction?.level == "available" }
        // Pact Magic slots are 2nd level at 3rd level.
        val third = pages.last { it.characterLevel == 3 }
        assertEquals(2, pageSpells(third).maxOf { it.level ?: 0 })
        assertEquals(1, pages.first { it.characterLevel == 1 }.let(::pageSpells).maxOf { it.level ?: 0 })
    }

    @Test
    fun aKnownSpellCanBeSwappedWhereTheStepAllows() {
        val bard = catalog.classesByIdentifier.getValue("bard")
        val bundle = newCharacter()
        val firstLevel = complete(bundle, LevelUpDraft(1, setup = LevelUpSetup(bard.id, keepExistingLevels = false)))
        val bardOne = engine.apply(bundle, firstLevel, russian = false, now = 1)
        val draft = LevelUpDraft(2)
        val page = engine.run(bardOne, complete(bardOne, draft)).pages.filterIsInstance<LevelUpPage.Spells>()
            .first { it.replaceable.isNotEmpty() && it.step.restriction?.level != "0" }
        val swapped = page.replaceable.first()
        val newSpell = page.options.first { !it.known }.spell
        val withSwap = complete(bardOne, draft) { candidate ->
            if (candidate is LevelUpPage.Spells && candidate.key == page.key) {
                LevelUpAnswer.Items(listOf(newSpell.id) + candidate.options.filter { !it.known && it.spell.id != newSpell.id }
                    .take(candidate.required).map { it.spell.id }, replacedId = swapped.id)
            } else {
                null
            }
        }
        val summary = engine.run(bardOne, withSwap).pages.filterIsInstance<LevelUpPage.Summary>().single().summary
        assertEquals(listOf(swapped.id), summary.spellsForgotten.map { it.id })
        val result = engine.apply(bardOne, withSwap, russian = false, now = 2)
        val names = result.spells.map { it.name }
        assertFalse(swapped.name.en in names)
        assertTrue(newSpell.name.en in names)
    }

    @Test
    fun domainSpellsComeAlwaysPrepared() {
        val cleric = catalog.classesByIdentifier.getValue("cleric")
        val bundle = newCharacter()
        val draft = complete(bundle, LevelUpDraft(3, setup = LevelUpSetup(cleric.id, keepExistingLevels = false)), subclass = "life-domain")
        val result = engine.apply(bundle, draft, russian = false, now = 1)
        val domain = result.spells.filter { it.name in listOf("Aid", "Bless", "Cure Wounds", "Lesser Restoration") }
        assertEquals(4, domain.size)
        assertTrue(domain.all { it.isPrepared })
        // And the cleric's cantrips: three at 1st level.
        assertEquals(3, result.spells.count { it.level == 0 })
    }
}
