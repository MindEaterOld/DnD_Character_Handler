package com.dndcharacterhandler.domain.levelup

import com.dndcharacterhandler.domain.model.AdvancementRecord
import com.dndcharacterhandler.domain.model.AdvancementStep
import com.dndcharacterhandler.domain.model.CatalogBackground
import com.dndcharacterhandler.domain.model.CatalogClass
import com.dndcharacterhandler.domain.model.CatalogEquipmentRef
import com.dndcharacterhandler.domain.model.CatalogFeature
import com.dndcharacterhandler.domain.model.CatalogFeatureKind
import com.dndcharacterhandler.domain.model.CatalogSpecies
import com.dndcharacterhandler.domain.model.CatalogSpellRef
import com.dndcharacterhandler.domain.model.CatalogText
import com.dndcharacterhandler.domain.model.CharacterBundle
import com.dndcharacterhandler.domain.model.CharacterCatalog
import com.dndcharacterhandler.domain.model.CharacterClassEntry
import com.dndcharacterhandler.domain.model.ClassRestriction
import com.dndcharacterhandler.domain.model.CombatResource
import com.dndcharacterhandler.domain.model.EquipmentNode
import com.dndcharacterhandler.domain.model.Feature
import com.dndcharacterhandler.domain.model.FeatureSource
import com.dndcharacterhandler.domain.model.InventoryCategory
import com.dndcharacterhandler.domain.model.InventoryItem
import com.dndcharacterhandler.domain.rules.CatalogFormulaText
import com.dndcharacterhandler.domain.rules.FormulaContext
import com.dndcharacterhandler.domain.rules.MAX_CHARACTER_LEVEL
import com.dndcharacterhandler.domain.rules.abilityModifier
import com.dndcharacterhandler.domain.rules.abilityScores
import com.dndcharacterhandler.domain.rules.classLabel
import com.dndcharacterhandler.domain.rules.multiclassRequirements
import com.dndcharacterhandler.domain.rules.proficiencyBonusForLevel
import com.dndcharacterhandler.domain.rules.spellSlots

/** An inventory item with just the name, for equipment the app's item catalog doesn't know. */
fun plainEquipmentItem(item: CatalogEquipmentRef, count: Int, russian: Boolean): InventoryItem = InventoryItem(
    name = item.name.get(russian),
    category = when (item.type) {
        "weapon" -> InventoryCategory.WEAPON
        "consumable" -> InventoryCategory.CONSUMABLE
        else -> InventoryCategory.OTHER
    },
    weight = 0.0,
    quantity = count,
    isEquipped = false,
    icon = ""
)

/**
 * The level-up wizard, as Foundry's advancement runs it: every level gained walks the steps of the
 * class, its subclass and the features they grant — hit points, features, choices, ability score
 * improvements or feats, proficiencies — and nothing changes until the whole draft is applied.
 *
 * Pages are rebuilt from the [LevelUpDraft] after every answer, so earlier answers (a different
 * class for a level, another subclass) reshape the pages that follow.
 */
class LevelUpEngine(private val catalog: CharacterCatalog) {
    private val formulas = CatalogFormulaText(catalog)

    /** The pages for [draft] on [bundle]'s character. */
    fun run(bundle: CharacterBundle, draft: LevelUpDraft): LevelUpRun {
        val simulation = simulate(bundle, draft)
        val complete = simulation.pages.all { isAnswered(it, draft) } && simulation.ready
        return LevelUpRun(simulation.pages, complete)
    }

    /** Whether [page] has everything it needs in [draft]. */
    fun isAnswered(page: LevelUpPage, draft: LevelUpDraft): Boolean = when (page) {
        is LevelUpPage.Setup -> draft.setup?.classId?.let { id -> catalog.classes.any { it.id == id } } == true
        is LevelUpPage.ChooseClass -> true
        is LevelUpPage.HitPoints -> page.takesMaximum || draft.answers[page.key] is LevelUpAnswer.HitPoints
        is LevelUpPage.Features -> true
        is LevelUpPage.Traits -> (draft.answers[page.key] as? LevelUpAnswer.Traits)?.let { answer ->
            page.required.indices.all { index -> (answer.picks.getOrNull(index)?.size ?: 0) == page.required[index] }
        } ?: page.required.all { it == 0 }
        is LevelUpPage.Items -> (draft.answers[page.key] as? LevelUpAnswer.Items)?.let { answer ->
            answer.picks.size == page.required + if (answer.replacedId != null) 1 else 0
        } ?: (page.required == 0)
        is LevelUpPage.AbilityScores -> when (val answer = draft.answers[page.key]) {
            is LevelUpAnswer.Feat -> page.allowFeat && page.feats.any { it.id == answer.featId }
            is LevelUpAnswer.AbilityScores -> answer.increases.values.sum() == page.required
            else -> page.required == 0
        }
        is LevelUpPage.Subclass -> draft.answers[page.key] is LevelUpAnswer.Subclass
        is LevelUpPage.BaseAbilities -> (draft.answers[page.key] as? LevelUpAnswer.BaseAbilities)?.let(::validAbilities) == true
        is LevelUpPage.Species, is LevelUpPage.Background -> draft.answers[page.key] is LevelUpAnswer.Origin
        is LevelUpPage.Equipment -> (draft.answers[page.key] as? LevelUpAnswer.Equipment)?.let { answer ->
            val option = page.options.getOrNull(answer.option) ?: return@let false
            option.choices.indices.all { index -> pickFor(option.choices[index], answer.picks.getOrNull(index)) != null }
        } == true
        is LevelUpPage.Summary -> true
    }

    /** Whether new ability scores follow their method: the standard array once each, 27 points, or the rolls. */
    fun validAbilities(answer: LevelUpAnswer.BaseAbilities): Boolean {
        val scores = LevelUpPage.AbilityScores.ABILITIES.map { answer.scores[it] ?: return false }
        return when (answer.method) {
            AbilityMethod.STANDARD_ARRAY -> scores.sorted() == STANDARD_ARRAY.sorted()
            AbilityMethod.POINT_BUY -> scores.all { it in 8..15 } && scores.sumOf { POINT_BUY_COSTS.getValue(it) } <= POINT_BUY_BUDGET
            AbilityMethod.ROLL -> answer.rolls.size == 6 && scores.sorted() == answer.rolls.sorted()
            AbilityMethod.KEEP -> true
        }
    }

    /** The pick of an equipment choice: the player's, or the suggested one. */
    fun pickFor(choice: EquipmentChoice, picked: String?): EquipmentPick? =
        choice.options.firstOrNull { it.id == picked } ?: choice.options.firstOrNull { it.id == choice.suggested }

    /**
     * The character after the level-up: classes, level, hit points, ability scores, proficiencies,
     * features, resources and spell slots, plus the choices in [com.dndcharacterhandler.domain.model.Character.advancements].
     */
    fun apply(
        bundle: CharacterBundle,
        draft: LevelUpDraft,
        russian: Boolean,
        now: Long,
        /** Turns starting equipment into inventory items (the app matches its item catalog by name). */
        equipmentItem: (CatalogEquipmentRef, Int) -> InventoryItem = { item, count -> plainEquipmentItem(item, count, russian) }
    ): CharacterBundle {
        val simulation = simulate(bundle, draft)
        require(simulation.ready && simulation.pages.all { isAnswered(it, draft) }) { "The level-up draft isn't complete" }
        return simulation.applyTo(bundle, russian, now, equipmentItem)
    }


    // --- Simulation ------------------------------------------------------------------------------

    private fun simulate(bundle: CharacterBundle, draft: LevelUpDraft): Simulation {
        val simulation = Simulation(bundle)
        val currentLevel = bundle.character.level.coerceIn(1, MAX_CHARACTER_LEVEL)
        var firstClassId: String? = null
        var startLevel = currentLevel
        if (simulation.classes.isEmpty()) {
            simulation.pages += LevelUpPage.Setup(
                classes = sortedClasses(),
                currentLevel = currentLevel,
                suggestKeep = bundle.features.isNotEmpty()
            )
            val setup = draft.setup?.takeIf { catalog.classes.any { c -> c.id == it.classId } }
            if (setup == null) {
                simulation.ready = false
                return simulation
            }
            if (setup.keepExistingLevels) {
                val subclass = setup.subclassId?.takeIf { id -> catalog.subclasses.any { it.id == id } }
                simulation.classes += CharacterClassEntry(setup.classId, subclass, levels = currentLevel, isOriginal = true)
            } else {
                simulation.rebuild = true
                firstClassId = setup.classId
                startLevel = 0
            }
        }
        firstClassId?.let { id -> createOrigin(simulation, draft, catalog.classes.first { it.id == id }) }
        simulation.fromLevel = startLevel
        val target = draft.targetLevel.coerceIn(startLevel, MAX_CHARACTER_LEVEL)
        var previousPick: String? = null
        for (level in startLevel + 1..target) {
            val classId = if (level == 1 && firstClassId != null) {
                firstClassId
            } else {
                val default = previousPick
                    ?: simulation.classes.maxByOrNull { it.levels }?.classId
                    ?: firstClassId
                    ?: catalog.classes.first().id
                val picked = draft.classPicks[level]?.takeIf { id -> catalog.classes.any { it.id == id } } ?: default
                simulation.pages += classPage(simulation, level, picked)
                picked
            }
            previousPick = classId
            levelUp(simulation, draft, level, classId)
            if (simulation.rebuild && level == 1) equipmentPages(simulation, draft, catalog.classes.first { it.id == classId })
        }
        simulation.toLevel = target
        simulation.pages += LevelUpPage.Summary(simulation.summary())
        return simulation
    }

    /**
     * A new character's origin, before its first class level: ability scores, then the species and
     * the background with their own steps (traits, lineage choices, the background's ability
     * increases, skills, tools, languages and origin feat).
     */
    private fun createOrigin(simulation: Simulation, draft: LevelUpDraft, firstClass: CatalogClass) {
        val context = StepContext(characterLevel = 1, characterClass = firstClass, classLevel = 1, isOriginal = true)
        simulation.pages += LevelUpPage.BaseAbilities(simulation.character.abilityScores())
        (draft.answers["abilities"] as? LevelUpAnswer.BaseAbilities)?.takeIf(::validAbilities)?.let { answer ->
            if (answer.method != AbilityMethod.KEEP) simulation.useBaseScores(answer.scores)
            simulation.record(
                context, "abilities", "abilities", "Abilities",
                "method=${answer.method};scores=${answer.scores.entries.joinToString(",") { "${it.key}:${it.value}" }}" +
                    if (answer.rolls.isEmpty()) "" else ";rolls=${answer.rolls.joinToString(",")}"
            )
        }

        // Steps of later character levels (a species' spells at 3rd and 5th level) come with those levels.
        fun firstLevel(steps: List<AdvancementStep>) = steps.filter { (it.level ?: 0) <= 1 }

        simulation.pages += LevelUpPage.Species(
            options = catalog.species.sortedWith(compareBy({ it.book != PHB }, { it.name.ru.ifBlank { it.name.en } })),
            currentName = simulation.character.race
        )
        val speciesId = (draft.answers["species"] as? LevelUpAnswer.Origin)?.id
        simulation.species = catalog.species.firstOrNull { it.id == speciesId }
        simulation.species?.let { species ->
            simulation.record(context, species.id, "species", "Species", "species=${species.id}")
            processOrigin(simulation, draft, context, species.id, species.name, firstLevel(species.advancement))
        }

        simulation.pages += LevelUpPage.Background(
            options = catalog.backgrounds.sortedWith(compareBy({ it.book != PHB }, { it.name.ru.ifBlank { it.name.en } })),
            currentName = simulation.character.background
        )
        val backgroundId = (draft.answers["background"] as? LevelUpAnswer.Origin)?.id
        simulation.background = catalog.backgrounds.firstOrNull { it.id == backgroundId }
        simulation.background?.let { background ->
            simulation.record(context, background.id, "background", "Background", "background=${background.id}")
            processOrigin(simulation, draft, context, background.id, background.name, firstLevel(background.advancement))
        }
    }

    /** A species' or background's [steps]: the features they grant, their choices, then those features' own steps. */
    private fun processOrigin(
        simulation: Simulation,
        draft: LevelUpDraft,
        context: StepContext,
        sourceId: String,
        name: CatalogText,
        steps: List<AdvancementStep>
    ) {
        val newFeatures = mutableListOf<CatalogFeature>()
        steps.filterIsInstance<AdvancementStep.ItemGrant>().forEach { step ->
            val taken = step.items.mapNotNull { catalog.featuresById[it] }
            taken.forEach { simulation.addFeature(it, context) }
            newFeatures += taken
            simulation.spellsGranted += step.spells.mapNotNull { catalog.spells[it] }
            simulation.record(context, sourceId, step.id, "ItemGrant", "features=${taken.joinToString(",") { it.id }}")
        }
        processChoices(simulation, draft, context, StepSource(sourceId, name, steps, allowFeat = false), newFeatures, arrival = true)
        processNewFeatureSteps(simulation, draft, context, newFeatures, mutableSetOf())
    }

    /** The starting equipment pages of a new character: its first class's, then its background's. */
    private fun equipmentPages(simulation: Simulation, draft: LevelUpDraft, firstClass: CatalogClass) {
        val sources = listOf(EquipmentSource(firstClass.id, firstClass.name, firstClass.startingEquipment, firstClass.wealth)) +
            listOfNotNull(simulation.background?.let { EquipmentSource(it.id, it.name, it.startingEquipment, it.wealth) })
        val context = StepContext(characterLevel = 1, characterClass = firstClass, classLevel = 1, isOriginal = true)
        sources.forEach { source ->
            val options = equipmentOptions(simulation, source.nodes, source.wealth)
            if (options.isEmpty()) return@forEach
            val page = LevelUpPage.Equipment(key = "equipment:${source.id}", source = source.name, options = options)
            simulation.pages += page
            val answer = draft.answers[page.key] as? LevelUpAnswer.Equipment ?: return@forEach
            val option = options.getOrNull(answer.option) ?: return@forEach
            val picks = option.choices.mapIndexedNotNull { index, choice -> pickFor(choice, answer.picks.getOrNull(index)) }
            simulation.equipment += option.items + picks
            option.coins.forEach { (currency, count) -> simulation.coins[currency] = (simulation.coins[currency] ?: 0) + count }
            simulation.record(
                context, source.id, "equipment", "StartingEquipment",
                "option=${answer.option};picks=${picks.joinToString(",") { it.id }}"
            )
        }
    }

    /** The options of a starting equipment tree ("A or B"), plus "the gold instead". */
    private fun equipmentOptions(simulation: Simulation, nodes: List<EquipmentNode>, wealth: String): List<EquipmentOption> {
        val root = nodes.singleOrNull() as? EquipmentNode.Group
        val groups = when {
            root != null && root.any -> root.children
            nodes.isNotEmpty() -> listOf(EquipmentNode.Group(any = false, children = nodes))
            else -> emptyList()
        }
        val gold = wealth.trim().toIntOrNull()?.takeIf { it > 0 }
        return groups.map { equipmentOption(simulation, it) } +
            listOfNotNull(gold?.let { EquipmentOption(emptyList(), mapOf("gp" to it), emptyList(), isWealth = true) })
    }

    private fun equipmentOption(simulation: Simulation, node: EquipmentNode): EquipmentOption {
        val items = mutableListOf<EquipmentPick>()
        val coins = mutableMapOf<String, Int>()
        val choices = mutableListOf<EquipmentChoice>()
        fun choice(options: List<EquipmentPick>) {
            // The tool the character was given proficiency with first (the background's own one,
            // before an origin feat's) is the natural pick.
            val suggested = options.singleOrNull()?.id ?: simulation.gainedTraits.firstOrNull { key -> options.any { it.id == key } }
            if (options.isNotEmpty()) choices += EquipmentChoice(options, suggested)
        }
        fun collect(part: EquipmentNode) {
            when (part) {
                is EquipmentNode.Group -> if (part.any) choice(part.children.flatMap(::equipmentAlternatives)) else part.children.forEach(::collect)
                is EquipmentNode.Item -> items += itemPick(part)
                is EquipmentNode.Currency -> coins[part.currency] = (coins[part.currency] ?: 0) + part.count
                is EquipmentNode.Category -> choice(equipmentAlternatives(part))
            }
        }
        collect(node)
        return EquipmentOption(items, coins, choices)
    }

    /** What a choice can be: an item, any tool of a kind ("tool:art:smith"), or the items of a group. */
    private fun equipmentAlternatives(node: EquipmentNode): List<EquipmentPick> = when (node) {
        is EquipmentNode.Item -> listOf(itemPick(node))
        is EquipmentNode.Category -> catalog.traits["${node.category}:${node.key}"]?.children.orEmpty().map { key ->
            EquipmentPick(key, CatalogEquipmentRef(key, traitName(key), node.category), node.count)
        }
        is EquipmentNode.Group -> node.children.flatMap(::equipmentAlternatives)
        is EquipmentNode.Currency -> emptyList()
    }

    private fun itemPick(node: EquipmentNode.Item): EquipmentPick = EquipmentPick(
        id = "item:${node.itemId}",
        item = catalog.equipment[node.itemId] ?: CatalogEquipmentRef(node.itemId, CatalogText(node.itemId), ""),
        count = node.count
    )

    private fun classPage(simulation: Simulation, characterLevel: Int, selected: String): LevelUpPage.ChooseClass {
        val current = simulation.classes.mapNotNull { entry ->
            val characterClass = catalog.classes.firstOrNull { it.id == entry.classId } ?: return@mapNotNull null
            ClassChoice(characterClass, entry.levels, entry.subclassId?.let { id -> catalog.subclasses.firstOrNull { it.id == id } })
        }
        val isNewClass = simulation.classes.none { it.classId == selected }
        return LevelUpPage.ChooseClass(
            characterLevel = characterLevel,
            current = current,
            others = sortedClasses().filter { candidate -> current.none { it.characterClass.id == candidate.id } },
            selectedClassId = selected,
            requirements = if (isNewClass && simulation.classes.isNotEmpty()) {
                multiclassRequirements(simulation.character, simulation.classes, selected, catalog, simulation.abilities)
            } else {
                emptyList()
            }
        )
    }

    private fun levelUp(simulation: Simulation, draft: LevelUpDraft, characterLevel: Int, classId: String) {
        val characterClass = catalog.classes.first { it.id == classId }
        val index = simulation.classes.indexOfFirst { it.classId == classId }
        val isOriginal = if (index < 0) simulation.classes.isEmpty() else simulation.classes[index].isOriginal
        val classLevel = if (index < 0) 1 else simulation.classes[index].levels + 1
        if (index < 0) {
            if (simulation.classes.isNotEmpty()) {
                simulation.unmetRequirements += multiclassRequirements(
                    simulation.character, simulation.classes, classId, catalog, simulation.abilities
                ).filterNot { it.isMet }
            }
            simulation.classes += CharacterClassEntry(classId, levels = 1, isOriginal = isOriginal)
        } else {
            simulation.classes[index] = simulation.classes[index].copy(levels = classLevel)
        }
        val entryIndex = simulation.classes.indexOfFirst { it.classId == classId }
        val context = StepContext(characterLevel, characterClass, classLevel, isOriginal)
        val prefix = "L$characterLevel"

        // Hit points: the maximum of the die at the character's very first level.
        val hitPoints = LevelUpPage.HitPoints(
            key = "$prefix:hp",
            characterLevel = characterLevel,
            characterClass = characterClass,
            classLevel = classLevel,
            takesMaximum = isOriginal && classLevel == 1,
            constitutionModifier = abilityModifier(simulation.abilities["con"] ?: 10)
        )
        simulation.pages += hitPoints
        val hpAnswer = draft.answers[hitPoints.key] as? LevelUpAnswer.HitPoints
        val hpValue = when {
            hitPoints.takesMaximum -> characterClass.hitDie
            hpAnswer != null -> hpAnswer.value.coerceIn(1, characterClass.hitDie)
            else -> hitPoints.average
        }
        simulation.hitPoints += hpValue
        simulation.record(context, characterClass.id, "hp", "HitPoints", "hp=$hpValue;method=${hpAnswer?.method ?: HitPointMethod.MAXIMUM}")

        val classSteps = characterClass.advancement.filter { applies(it, classLevel, isOriginal) }

        // The subclass comes first, so its features of this level join the class's.
        classSteps.filterIsInstance<AdvancementStep.Subclass>().forEach { step ->
            val page = LevelUpPage.Subclass(
                key = "$prefix:${characterClass.id}:${step.id}",
                characterLevel = characterLevel,
                characterClass = characterClass,
                options = catalog.subclasses.filter { it.classIdentifier == characterClass.identifier }
                    .sortedWith(compareBy({ it.book != PHB }, { it.name.ru.ifBlank { it.name.en } }))
            )
            simulation.pages += page
            val answer = draft.answers[page.key] as? LevelUpAnswer.Subclass
            if (answer != null && page.options.any { it.id == answer.subclassId }) {
                simulation.classes[entryIndex] = simulation.classes[entryIndex].copy(subclassId = answer.subclassId)
                simulation.record(context, characterClass.id, step.id, "Subclass", "subclass=${answer.subclassId}")
            }
        }
        val subclass = simulation.classes[entryIndex].subclassId?.let { id -> catalog.subclasses.firstOrNull { it.id == id } }
        val subclassSteps = subclass?.advancement?.filter { applies(it, classLevel, isOriginal) }.orEmpty()

        // New features and the values that grow with the level.
        val grantSteps = classSteps.filterIsInstance<AdvancementStep.ItemGrant>().map { characterClass.id to it } +
            subclassSteps.filterIsInstance<AdvancementStep.ItemGrant>().map { subclass!!.id to it }
        val grants = grantSteps.flatMap { (_, step) ->
            step.items.mapNotNull { id -> catalog.featuresById[id]?.let { GrantedFeature(it, step.optional) } }
        }
        val scaleChanges = (characterClass.advancement + subclass?.advancement.orEmpty())
            .filterIsInstance<AdvancementStep.ScaleValue>()
            .mapNotNull { scale ->
                val now = scale.valueAt(classLevel) ?: return@mapNotNull null
                val before = scale.valueAt(classLevel - 1)
                if (now == before) null else ScaleChange(scale.title, before, now)
            }
        val grantedSpells = grantSteps.flatMap { (_, step) -> step.spells.mapNotNull { catalog.spells[it] } }
        val featuresPage = LevelUpPage.Features(
            key = "$prefix:features",
            characterLevel = characterLevel,
            characterClass = characterClass,
            classLevel = classLevel,
            grants = grants,
            scaleChanges = scaleChanges,
            spells = grantedSpells
        )
        if (grants.isNotEmpty() || scaleChanges.isNotEmpty() || grantedSpells.isNotEmpty()) simulation.pages += featuresPage
        val declined = (draft.answers[featuresPage.key] as? LevelUpAnswer.Grants)?.declined.orEmpty()
        val newFeatures = mutableListOf<CatalogFeature>()
        grantSteps.forEach { (sourceId, step) ->
            val taken = step.items.filter { id -> !(step.optional && id in declined) }.mapNotNull { catalog.featuresById[it] }
            taken.forEach { feature -> simulation.addFeature(feature, context) }
            newFeatures += taken
            simulation.spellsGranted += step.spells.mapNotNull { catalog.spells[it] }
            simulation.record(context, sourceId, step.id, "ItemGrant", "features=${taken.joinToString(",") { it.id }}")
        }

        // Choices of the class and the subclass.
        val sources = listOf(StepSource(characterClass.id, characterClass.name, classSteps, allowFeat = true)) +
            listOfNotNull(subclass?.let { StepSource(it.id, it.name, subclassSteps, allowFeat = true) })
        sources.forEach { source -> processChoices(simulation, draft, context, source, newFeatures, arrival = false) }

        // Features with steps of their own: new ones now, older ones when their class reaches the step's level.
        processFeatureSteps(simulation, draft, context, newFeatures, subclass?.id)

        // The species' steps of this character level (Celestial Revelation at 3rd, Large Form at 5th).
        simulation.knownSpecies?.takeIf { characterLevel > 1 }?.let { species ->
            val due = species.advancement.filter { it.level == characterLevel }
            if (due.isNotEmpty()) processOrigin(simulation, draft, context, species.id, species.name, due)
        }
    }

    /**
     * The trait, choice and ability steps of [source]; features they add go to [newFeatures].
     * [arrival]: the source is a feature just gained, whose choices listed at "level 0" happen now.
     */
    private fun processChoices(
        simulation: Simulation,
        draft: LevelUpDraft,
        context: StepContext,
        source: StepSource,
        newFeatures: MutableList<CatalogFeature>,
        arrival: Boolean
    ) {
        val prefix = "L${context.characterLevel}:${source.id}"
        source.steps.forEach { step ->
            when (step) {
                is AdvancementStep.Trait -> processTrait(simulation, draft, context, source, step, "$prefix:${step.id}")
                is AdvancementStep.ItemChoice ->
                    processItemChoice(simulation, draft, context, source, step, "$prefix:${step.id}", newFeatures, arrival)
                is AdvancementStep.AbilityScoreImprovement ->
                    processAbilityScores(simulation, draft, context, source, step, "$prefix:${step.id}", newFeatures)
                else -> Unit
            }
        }
    }

    private fun processFeatureSteps(
        simulation: Simulation,
        draft: LevelUpDraft,
        context: StepContext,
        newFeatures: MutableList<CatalogFeature>,
        subclassId: String?
    ) {
        val owners = setOfNotNull(context.characterClass.id, subclassId)
        val processed = mutableSetOf<String>()
        // Older features of this class whose own steps start at this level (e.g. Circle Spells).
        val older = simulation.knownFeatureIds.toList().mapNotNull { catalog.featuresById[it] }
            .filter { feature -> feature.id !in newFeatures.map { it.id } && feature.grantedBy.any { it.ownerId in owners } }
        older.forEach { feature ->
            val steps = feature.advancement.filter { step ->
                val due = step.level == context.classLevel ||
                    (step is AdvancementStep.ItemChoice && step.level == null && context.classLevel in step.counts)
                due && applies(step, context.classLevel, context.isOriginal, ignoreLevel = true)
            }
            if (steps.isNotEmpty()) {
                processed += feature.id
                processNested(simulation, draft, context, feature, steps, newFeatures, arrival = false)
            }
        }
        processNewFeatureSteps(simulation, draft, context, newFeatures, processed)
    }

    /** New features (and the features they bring, one level deep) apply their steps on arrival. */
    private fun processNewFeatureSteps(
        simulation: Simulation,
        draft: LevelUpDraft,
        context: StepContext,
        newFeatures: MutableList<CatalogFeature>,
        processed: MutableSet<String>
    ) {
        var queue = newFeatures.toList()
        repeat(2) {
            val next = mutableListOf<CatalogFeature>()
            queue.filter { it.id !in processed && it.advancement.isNotEmpty() }.forEach { feature ->
                processed += feature.id
                val steps = feature.advancement.filter { step -> (step.level ?: 0) <= context.classLevel }
                val before = newFeatures.size
                processNested(simulation, draft, context, feature, steps, newFeatures, arrival = true)
                next += newFeatures.drop(before)
            }
            queue = next
        }
    }

    private fun processNested(
        simulation: Simulation,
        draft: LevelUpDraft,
        context: StepContext,
        feature: CatalogFeature,
        steps: List<AdvancementStep>,
        newFeatures: MutableList<CatalogFeature>,
        arrival: Boolean
    ) {
        steps.filterIsInstance<AdvancementStep.ItemGrant>().forEach { step ->
            val taken = step.items.mapNotNull { catalog.featuresById[it] }
            taken.forEach { simulation.addFeature(it, context) }
            newFeatures += taken
            simulation.spellsGranted += step.spells.mapNotNull { catalog.spells[it] }
            simulation.record(context, feature.id, step.id, "ItemGrant", "features=${taken.joinToString(",") { it.id }}")
        }
        processChoices(simulation, draft, context, StepSource(feature.id, feature.name, steps, allowFeat = false), newFeatures, arrival)
    }

    private fun processTrait(
        simulation: Simulation,
        draft: LevelUpDraft,
        context: StepContext,
        source: StepSource,
        step: AdvancementStep.Trait,
        key: String
    ) {
        val granted = step.grants.flatMap(::expandTrait).distinct()
        val groups = step.choices.map { choice ->
            val pool = choice.pool.flatMap(::expandTrait).distinct().filter { it !in granted }
            val options = pool.map { traitKey ->
                TraitOption(traitKey, traitName(traitKey), alreadyHas = simulation.hasTrait(traitKey, step.mode))
            }.let { options -> if (step.mode == "expertise") options.filter { simulation.isProficient(it.key) } else options }
            TraitGroup(choice.count, options)
        }
        if (groups.isNotEmpty()) {
            simulation.pages += LevelUpPage.Traits(
                key = key,
                characterLevel = context.characterLevel,
                source = source.name,
                step = step,
                granted = granted.map { TraitOption(it, traitName(it), simulation.hasTrait(it, step.mode)) },
                groups = groups
            )
        }
        val picks = (draft.answers[key] as? LevelUpAnswer.Traits)?.picks.orEmpty()
        val chosen = groups.indices.flatMap { index ->
            val allowed = groups[index].options.map { it.key }.toSet()
            picks.getOrNull(index).orEmpty().filter { it in allowed }
        }
        val applied = (granted + chosen).distinct()
        applied.forEach { simulation.gainTrait(it, step.mode) }
        simulation.record(context, source.id, step.id, "Trait", "mode=${step.mode};added=${applied.joinToString(",")}")
    }

    private fun processItemChoice(
        simulation: Simulation,
        draft: LevelUpDraft,
        context: StepContext,
        source: StepSource,
        step: AdvancementStep.ItemChoice,
        key: String,
        newFeatures: MutableList<CatalogFeature>,
        arrival: Boolean
    ) {
        val count = step.counts[context.classLevel] ?: (if (arrival) step.counts[0] else null) ?: 0
        if (count <= 0) return
        val pool = choicePool(step, context.classLevel)
        if (pool.isEmpty()) {
            // Spell picks (cantrips, spells known): left to the Spells screen for now. Item picks (an
            // artisan's tool, a holy symbol) are part of the starting equipment.
            if (step.itemType == "spell" || step.spells.isNotEmpty()) simulation.spellChoices += SpellChoiceNote(source.name, step.title, count)
            return
        }
        val options = pool.map { ItemOption(it, known = it.id in simulation.knownFeatureIds) }
        val replaceable = if (context.classLevel in step.replacementLevels) options.filter { it.known }.map { it.feature } else emptyList()
        val page = LevelUpPage.Items(key, context.characterLevel, source.name, step, count, options, replaceable)
        simulation.pages += page
        val answer = draft.answers[key] as? LevelUpAnswer.Items ?: return
        val allowed = options.filter { !it.known || it.feature.repeatable }.map { it.feature.id }.toSet() +
            if (answer.replacedId != null) options.map { it.feature.id } else emptyList()
        val picks = answer.picks.filter { it in allowed }.mapNotNull { catalog.featuresById[it] }
        answer.replacedId?.takeIf { id -> replaceable.any { it.id == id } }?.let { simulation.removeFeature(it) }
        picks.forEach { simulation.addFeature(it, context) }
        newFeatures += picks
        simulation.record(
            context, source.id, step.id, "ItemChoice",
            "picks=${picks.joinToString(",") { it.id }}" + (answer.replacedId?.let { ";replaced=$it" } ?: "")
        )
    }

    private fun processAbilityScores(
        simulation: Simulation,
        draft: LevelUpDraft,
        context: StepContext,
        source: StepSource,
        step: AdvancementStep.AbilityScoreImprovement,
        key: String,
        newFeatures: MutableList<CatalogFeature>
    ) {
        // A feat's fixed bonus (+1 Charisma) needs no page.
        step.fixed.forEach { (ability, bonus) -> simulation.increase(ability, bonus) }
        val page = LevelUpPage.AbilityScores(
            key = key,
            characterLevel = context.characterLevel,
            source = source.name,
            step = step,
            scores = simulation.abilities.toMap(),
            allowFeat = source.allowFeat,
            feats = if (source.allowFeat) availableFeats(simulation) else emptyList()
        )
        if (step.points <= 0 && !source.allowFeat) {
            if (step.fixed.isNotEmpty()) simulation.record(context, source.id, step.id, "AbilityScoreImprovement", "fixed=${step.fixed.entries.joinToString(",") { "${it.key}:${it.value}" }}")
            return
        }
        simulation.pages += page
        when (val answer = draft.answers[key]) {
            is LevelUpAnswer.Feat -> {
                val feat = page.feats.firstOrNull { it.id == answer.featId } ?: return
                simulation.addFeature(feat, context)
                newFeatures += feat
                simulation.record(context, source.id, step.id, "AbilityScoreImprovement", "feat=${feat.id}")
            }
            is LevelUpAnswer.AbilityScores -> {
                val increases = answer.increases.filter { (ability, points) ->
                    ability in page.abilities && points in 1..page.cap
                }
                increases.forEach { (ability, points) -> simulation.increase(ability, points, page.maxScore) }
                simulation.record(
                    context, source.id, step.id, "AbilityScoreImprovement",
                    "increases=${increases.entries.joinToString(",") { "${it.key}:${it.value}" }}"
                )
            }
            else -> Unit
        }
    }

    /** Features an ItemChoice offers: its own list, or every catalog feature its restriction allows. */
    private fun choicePool(step: AdvancementStep.ItemChoice, classLevel: Int): List<CatalogFeature> {
        if (step.itemType == "spell" || (step.items.isEmpty() && step.spells.isNotEmpty())) return emptyList()
        if (step.items.isNotEmpty()) return step.items.mapNotNull { catalog.featuresById[it] }
        val restriction = step.restriction ?: return emptyList()
        return catalog.features.filter { feature ->
            feature.type == "feat" &&
                (restriction.type.isBlank() || feature.featureType == restriction.type) &&
                (restriction.subtype.isBlank() || feature.subtype == restriction.subtype) &&
                (feature.prerequisiteLevel ?: 0) <= classLevel
        }.sortedWith(compareBy({ it.book != PHB }, { it.name.ru.ifBlank { it.name.en } }))
    }

    /** Feats the character may take instead of an ability score improvement. */
    private fun availableFeats(simulation: Simulation): List<CatalogFeature> =
        catalog.features.filter { feature ->
            feature.kind == CatalogFeatureKind.FEAT &&
                feature.identifier != "ability-score-improvement" &&
                (feature.prerequisiteLevel ?: 0) <= simulation.level &&
                (feature.repeatable || feature.id !in simulation.knownFeatureIds)
        }.sortedWith(compareBy({ it.book != PHB }, { it.subtype != "general" }, { it.name.ru.ifBlank { it.name.en } }))

    private fun applies(step: AdvancementStep, classLevel: Int, isOriginal: Boolean, ignoreLevel: Boolean = false): Boolean {
        val restrictionOk = when (step.classRestriction) {
            ClassRestriction.PRIMARY -> isOriginal
            ClassRestriction.SECONDARY -> !isOriginal
            null -> true
        }
        if (!restrictionOk) return false
        if (ignoreLevel) return true
        return when (step) {
            // Spread over the levels: counted where they change.
            is AdvancementStep.ItemChoice, is AdvancementStep.ScaleValue -> true
            is AdvancementStep.HitPoints -> false
            else -> step.level == classLevel
        }
    }

    /** "skills:*" or "tool:art:*" -> every single trait under it; a plain key stays itself. */
    private fun expandTrait(key: String): List<String> {
        if (!key.endsWith(":*")) return listOf(key)
        val root = key.removeSuffix(":*")
        val children = catalog.traits[root]?.children
            ?: catalog.traits.keys.filter { it.startsWith("$root:") && it.count { c -> c == ':' } == root.count { c -> c == ':' } + 1 }
        return children.flatMap { child -> catalog.traits[child]?.children?.takeIf { it.isNotEmpty() }?.let { expandTrait("$child:*") } ?: listOf(child) }
    }

    private fun traitName(key: String): CatalogText =
        catalog.traits[key]?.name ?: CatalogText(key.substringAfterLast(':'), "")

    private fun sortedClasses(): List<CatalogClass> =
        catalog.classes.sortedWith(compareBy({ it.book != PHB }, { it.name.ru.ifBlank { it.name.en } }))

    private data class StepContext(
        val characterLevel: Int,
        val characterClass: CatalogClass,
        val classLevel: Int,
        val isOriginal: Boolean
    )

    private class StepSource(val id: String, val name: CatalogText, val steps: List<AdvancementStep>, val allowFeat: Boolean)

    private class EquipmentSource(val id: String, val name: CatalogText, val nodes: List<EquipmentNode>, val wealth: String)

    /** The character while the draft is walked through: what it has and gains, level by level. */
    private inner class Simulation(bundle: CharacterBundle) {
        val character = bundle.character
        val pages = mutableListOf<LevelUpPage>()
        var ready = true
        var rebuild = false
        var fromLevel = character.level
        var toLevel = character.level
        val classes = character.classes.toMutableList()
        val abilities = character.abilityScores().toMutableMap()
        val abilityIncreases = mutableMapOf<String, Int>()
        val hitPoints = mutableListOf<Int>()
        val saves = SAVES.filter { ability -> character.saveProficient(ability) }.toMutableSet()
        val proficientSkills = bundle.skills.filter { it.isProficient }.mapNotNull { SKILL_KEYS[it.name] }.toMutableSet()
        val expertSkills = bundle.skills.filter { it.isExpertise }.mapNotNull { SKILL_KEYS[it.name] }.toMutableSet()
        private val proficiencyText = listOf(
            character.armorProficiencies, character.weaponProficiencies,
            character.toolProficiencies, character.languageProficiencies
        ).joinToString(" ").lowercase()
        val gainedTraits = mutableListOf<String>()
        val gainedExpertise = mutableListOf<String>()
        val masteries = mutableListOf<String>()
        val defenses = mutableListOf<String>()
        val knownFeatureIds = bundle.features.mapNotNull { it.catalogId }.toMutableSet()
        val addedFeatures = mutableListOf<Pair<CatalogFeature, StepContext>>()
        val removedFeatureIds = mutableListOf<String>()
        val spellsGranted = mutableListOf<CatalogSpellRef>()
        val spellChoices = mutableListOf<SpellChoiceNote>()
        val unmetRequirements = mutableListOf<com.dndcharacterhandler.domain.rules.MulticlassRequirement>()
        val records = mutableListOf<AdvancementRecord>()
        /** The species and background chosen in this draft (a new character's). */
        var species: CatalogSpecies? = null
        var background: CatalogBackground? = null
        var baseScores: Map<String, Int>? = null
        val equipment = mutableListOf<EquipmentPick>()
        val coins = mutableMapOf<String, Int>()

        /** The character's species from the catalog: chosen now, or when it was created. */
        val knownSpecies: CatalogSpecies?
            get() = species ?: character.advancements.lastOrNull { it.type == "Species" }?.sourceId
                ?.let { id -> catalog.species.firstOrNull { it.id == id } }

        /** A new character's own ability scores: the bonuses that follow add to them. */
        fun useBaseScores(scores: Map<String, Int>) {
            baseScores = scores
            abilities.putAll(scores)
        }

        val level: Int get() = classes.sumOf { it.levels }.coerceAtLeast(1)

        fun record(context: StepContext, sourceId: String, stepId: String, type: String, value: String) {
            records += AdvancementRecord(
                characterLevel = context.characterLevel,
                classId = context.characterClass.id,
                classLevel = context.classLevel,
                sourceId = sourceId,
                stepId = stepId,
                type = type,
                value = value
            )
        }

        fun addFeature(feature: CatalogFeature, context: StepContext) {
            if (feature.id in knownFeatureIds && !feature.repeatable) return
            knownFeatureIds += feature.id
            addedFeatures += feature to context
        }

        fun removeFeature(id: String) {
            knownFeatureIds -= id
            val added = addedFeatures.indexOfFirst { it.first.id == id }
            if (added >= 0) addedFeatures.removeAt(added) else removedFeatureIds += id
        }

        fun increase(ability: String, points: Int, max: Int = 20) {
            val current = abilities[ability] ?: return
            val next = (current + points).coerceAtMost(max)
            abilityIncreases[ability] = (abilityIncreases[ability] ?: 0) + (next - current)
            abilities[ability] = next
        }

        fun isProficient(key: String): Boolean = when {
            key.startsWith("skills:") -> key.removePrefix("skills:") in proficientSkills
            else -> hasTrait(key, "default")
        }

        fun hasTrait(key: String, mode: String): Boolean {
            val (kind, code) = key.substringBefore(':') to key.substringAfter(':')
            return when {
                mode == "expertise" || mode == "forcedExpertise" -> kind == "skills" && code in expertSkills
                mode == "mastery" -> key in masteries
                kind == "saves" -> code in saves
                kind == "skills" -> code in proficientSkills
                kind in DEFENSES -> key in defenses
                else -> key in gainedTraits || traitName(key).let { name ->
                    listOf(name.en, name.ru).any { it.isNotBlank() && it.lowercase() in proficiencyText }
                }
            }
        }

        fun gainTrait(key: String, mode: String) {
            val (kind, code) = key.substringBefore(':') to key.substringAfter(':')
            when {
                mode == "mastery" -> if (key !in masteries) masteries += key
                mode == "expertise" || mode == "forcedExpertise" -> if (kind == "skills") {
                    proficientSkills += code
                    if (expertSkills.add(code)) gainedExpertise += key
                }
                mode == "upgrade" && kind == "skills" && code in proficientSkills -> if (expertSkills.add(code)) gainedExpertise += key
                kind == "saves" -> if (saves.add(code)) gainedTraits += key
                kind == "skills" -> if (proficientSkills.add(code)) gainedTraits += key
                kind in DEFENSES -> if (key !in defenses) defenses += key
                else -> if (!hasTrait(key, mode)) gainedTraits += key
            }
        }

        fun hitPointGain(): Int {
            val oldModifier = abilityModifier(character.constitution)
            val newModifier = abilityModifier(abilities["con"] ?: character.constitution)
            return if (rebuild) {
                (hitPoints.sum() + newModifier * level).coerceAtLeast(1)
            } else {
                hitPoints.sum() + newModifier * level - oldModifier * character.level.coerceAtLeast(1)
            }
        }

        fun summary(): LevelUpSummary = LevelUpSummary(
            fromLevel = fromLevel,
            toLevel = toLevel,
            classes = classes.toList(),
            hitPointGain = hitPointGain(),
            addedFeatures = addedFeatures.map { it.first },
            removedFeatures = removedFeatureIds.mapNotNull { catalog.featuresById[it] },
            abilityIncreases = abilityIncreases.filterValues { it != 0 },
            proficiencies = gainedTraits.map { TraitOption(it, traitName(it), false) },
            expertise = gainedExpertise.map { TraitOption(it, traitName(it), false) },
            masteries = masteries.map { TraitOption(it, traitName(it), false) },
            defenses = defenses.map { TraitOption(it, traitName(it), false) },
            spellsGranted = spellsGranted.distinctBy { it.id },
            spellChoices = spellChoices.toList(),
            spellSlots = spellSlots(classes, catalog),
            unmetRequirements = unmetRequirements.distinctBy { it.classId },
            species = species,
            background = background,
            baseScores = baseScores,
            equipment = equipment.toList(),
            coins = coins.toMap()
        )

        fun applyTo(
            bundle: CharacterBundle,
            russian: Boolean,
            now: Long,
            equipmentItem: (CatalogEquipmentRef, Int) -> InventoryItem
        ): CharacterBundle {
            val newLevel = level
            val gain = hitPointGain()
            val newAbilities = abilities
            val maxHp = if (rebuild) gain else (character.maxHp + gain).coerceAtLeast(1)
            val currentHp = if (rebuild) maxHp else (character.currentHp + gain).coerceIn(0, maxHp)

            // Features: the removed ones out (one row each), the new ones in, in the current language.
            val features = bundle.features.toMutableList()
            removedFeatureIds.forEach { id ->
                val index = features.indexOfFirst { it.catalogId == id }
                if (index >= 0) features.removeAt(index)
            }
            features += addedFeatures.map { (feature, context) -> feature.toCharacterFeature(context, russian) }
            // A species' darkvision is a sense of the species, not a feature: add one for the
            // Attributes screen, which reads the range from the darkvision features.
            val darkvision = species?.senses?.get("darkvision")?.toInt()?.takeIf { it > 0 }
            if (darkvision != null && features.none { it.name.trim().lowercase() in DARKVISION_NAMES }) {
                features += Feature(
                    name = if (russian) "Ночное зрение" else "Darkvision",
                    description = if (russian) {
                        "Вы видите в тусклом свете на расстоянии $darkvision футов как при ярком свете, а в темноте — как при тусклом."
                    } else {
                        "You can see in dim light within $darkvision feet as if it were bright light, and in darkness as if it were dim light."
                    },
                    level = 1,
                    source = FeatureSource.RACE,
                    category = species?.name?.get(russian).orEmpty()
                )
            }
            val inventory = bundle.inventoryItems + equipment.map { pick -> equipmentItem(pick.item, pick.count) }
            // The sheet keeps gold, silver and copper: platinum goes in as gold, electrum as silver.
            val gold = (coins["gp"] ?: 0) + 10 * (coins["pp"] ?: 0)
            val silver = (coins["sp"] ?: 0) + 5 * (coins["ep"] ?: 0)

            val context = formulaContext(newLevel, newAbilities)
            val resources = syncResources(bundle.combatResources, features, addedFeatures.map { it.first.id }.toSet(), context, russian)

            val oldSlots = bundle.character.spellSlotMaximums.toSlotList()
            val oldRemaining = bundle.character.spellSlotRemaining.toSlotList()
            val table = spellSlots(classes, catalog)
            val hadSlots = spellSlots(character.classes, catalog)
            val (slotMaximums, slotRemaining) = if (table.isEmpty) {
                bundle.character.spellSlotMaximums to bundle.character.spellSlotRemaining
            } else {
                val maximums = table.combined()
                val remaining = maximums.indices.map { index ->
                    ((oldRemaining.getOrElse(index) { 0 }) + maximums[index] - oldSlots.getOrElse(index) { 0 }).coerceIn(0, maximums[index])
                }
                maximums.joinToString(",") to remaining.joinToString(",")
            }
            val onlyPact = !table.isEmpty && table.slots.all { it == 0 }
            val original = classes.firstOrNull { it.isOriginal } ?: classes.first()
            val originalClass = catalog.classes.firstOrNull { it.id == original.classId }
            val spellcastingAbility = if (hadSlots.isEmpty && !table.isEmpty) {
                classes.firstNotNullOfOrNull { entry ->
                    val characterClass = catalog.classes.firstOrNull { it.id == entry.classId }
                    val subclass = entry.subclassId?.let { id -> catalog.subclasses.firstOrNull { it.id == id } }
                    (characterClass?.spellcasting ?: subclass?.spellcasting)?.ability?.let(::spellcastingAbilityOf)
                } ?: character.spellcastingAbility
            } else {
                character.spellcastingAbility
            }

            val traitsByKind = gainedTraits.groupBy { it.substringBefore(':') }
            fun appendTraits(text: String, kinds: List<String>, extra: List<String> = emptyList()): String {
                val labels = kinds.flatMap { traitsByKind[it].orEmpty() }.map { traitName(it).get(russian) } + extra
                return (listOf(text.trim()).filter { it.isNotEmpty() } + labels).distinct().joinToString(", ")
            }
            val masteryText = if (masteries.isEmpty()) emptyList() else {
                listOf((if (russian) "Мастерство: " else "Mastery: ") + masteries.joinToString(", ") { traitName(it).get(russian) })
            }

            val updatedClasses = classes.toList()
            val skills = bundle.skills.map { skill ->
                val key = SKILL_KEYS[skill.name] ?: return@map skill
                skill.copy(isProficient = skill.isProficient || key in proficientSkills, isExpertise = skill.isExpertise || key in expertSkills)
            }
            val newCharacter = character.copy(
                level = newLevel,
                characterClass = className(updatedClasses, russian),
                subclass = original.subclassId?.let { id -> catalog.subclasses.firstOrNull { it.id == id }?.name?.get(russian) } ?: character.subclass,
                maxHp = maxHp,
                currentHp = currentHp,
                hitDieSides = originalClass?.hitDie ?: character.hitDieSides,
                spentHitDice = updatedClasses.sumOf { it.spentHitDice }.coerceAtMost(newLevel),
                strength = newAbilities["str"] ?: character.strength,
                dexterity = newAbilities["dex"] ?: character.dexterity,
                constitution = newAbilities["con"] ?: character.constitution,
                intelligence = newAbilities["int"] ?: character.intelligence,
                wisdom = newAbilities["wis"] ?: character.wisdom,
                charisma = newAbilities["cha"] ?: character.charisma,
                strengthSaveProficient = "str" in saves,
                dexteritySaveProficient = "dex" in saves,
                constitutionSaveProficient = "con" in saves,
                intelligenceSaveProficient = "int" in saves,
                wisdomSaveProficient = "wis" in saves,
                charismaSaveProficient = "cha" in saves,
                armorProficiencies = appendTraits(character.armorProficiencies, listOf("armor")),
                weaponProficiencies = appendTraits(character.weaponProficiencies, listOf("weapon"), masteryText),
                toolProficiencies = appendTraits(character.toolProficiencies, listOf("tool")),
                languageProficiencies = appendTraits(character.languageProficiencies, listOf("languages")),
                spellSlotMaximums = slotMaximums,
                spellSlotRemaining = slotRemaining,
                spellSlotsRestoreOnShortRest = if (onlyPact) true else character.spellSlotsRestoreOnShortRest,
                spellcastingAbility = spellcastingAbility,
                classes = updatedClasses,
                advancements = character.advancements + records,
                race = species?.name?.get(russian) ?: character.race,
                background = background?.name?.get(russian) ?: character.background,
                speed = species?.movement?.get("walk")?.toInt() ?: character.speed,
                goldPieces = character.goldPieces + gold,
                silverPieces = character.silverPieces + silver,
                copperPieces = character.copperPieces + (coins["cp"] ?: 0),
                updatedAt = now
            )
            return bundle.copy(
                character = newCharacter,
                skills = skills,
                features = features,
                combatResources = resources,
                inventoryItems = inventory
            )
        }

        private fun formulaContext(level: Int, abilities: Map<String, Int>) = FormulaContext(
            totalLevel = level,
            classLevels = classes.mapNotNull { entry ->
                catalog.classes.firstOrNull { it.id == entry.classId }?.let { it.identifier to entry.levels }
            }.toMap(),
            subclasses = classes.mapNotNull { entry ->
                entry.subclassId?.let { id -> catalog.subclasses.firstOrNull { it.id == id }?.identifier }
            }.toSet(),
            abilityModifiers = abilities.mapValues { abilityModifier(it.value) },
            proficiencyBonus = proficiencyBonusForLevel(level)
        )

        /** Uses of catalog features (Rage, Superiority Dice...) as combat resources, sized for the new level. */
        private fun syncResources(
            resources: List<CombatResource>,
            features: List<Feature>,
            newFeatureIds: Set<String>,
            context: FormulaContext,
            russian: Boolean
        ): List<CombatResource> {
            val result = resources.toMutableList()
            features.mapNotNull { it.catalogId }.distinct().forEach { id ->
                val feature = catalog.featuresById[id] ?: return@forEach
                val uses = feature.uses?.takeIf { it.max.isNotBlank() } ?: return@forEach
                val maximum = formulas.value(uses.max, context)?.toIntOrNull()?.takeIf { it > 0 } ?: return@forEach
                val index = result.indexOfFirst { it.catalogId == id }
                if (index >= 0) {
                    val existing = result[index]
                    result[index] = existing.copy(
                        maximumUses = maximum,
                        currentUses = (existing.currentUses + maximum - existing.maximumUses).coerceIn(0, maximum)
                    )
                } else if (id in newFeatureIds) {
                    val periods = uses.recovery.map { it.period }
                    result += CombatResource(
                        name = feature.name.get(russian),
                        currentUses = maximum,
                        maximumUses = maximum,
                        restoresOnShortRest = "sr" in periods,
                        restoresOnLongRest = "lr" in periods || "sr" in periods,
                        catalogId = id
                    )
                }
            }
            return result
        }

        private fun CatalogFeature.toCharacterFeature(context: StepContext, russian: Boolean): Feature {
            val owner = grantedBy.firstNotNullOfOrNull { grant -> catalog.ownerName(grant.ownerId) }
            val category = when (kind) {
                CatalogFeatureKind.CLASS_OPTION, CatalogFeatureKind.FEAT -> catalog.subtypes[subtype] ?: owner
                else -> owner
            } ?: context.characterClass.name
            return Feature(
                name = name.get(russian),
                description = text.get(russian),
                level = context.classLevel,
                source = when (kind) {
                    CatalogFeatureKind.FEAT -> if (subtype == "origin") FeatureSource.BACKGROUND else FeatureSource.OTHER
                    CatalogFeatureKind.SPECIES_TRAIT -> FeatureSource.RACE
                    CatalogFeatureKind.BACKGROUND_FEATURE -> FeatureSource.BACKGROUND
                    else -> FeatureSource.CLASS
                },
                category = category.get(russian),
                catalogId = id
            )
        }

        private fun className(classes: List<CharacterClassEntry>, russian: Boolean): String =
            if (classes.size == 1) {
                catalog.classes.firstOrNull { it.id == classes.single().classId }?.name?.get(russian) ?: character.characterClass
            } else {
                // Without subclasses: the class label with levels ("Fighter 5 / Rogue 2").
                classLabel(classes.map { it.copy(subclassId = null) }, catalog, russian)
            }
    }

    private companion object {
        const val PHB = "PHB 2024"
        val SAVES = listOf("str", "dex", "con", "int", "wis", "cha")
        val DEFENSES = setOf("dr", "di", "dv", "ci")
        val DARKVISION_NAMES = setOf("darkvision", "ночное зрение", "тёмное зрение", "темное зрение")

        /** App skill names -> Foundry skill codes. */
        val SKILL_KEYS = mapOf(
            "skill_acrobatics" to "acr", "skill_animal_handling" to "ani", "skill_arcana" to "arc",
            "skill_athletics" to "ath", "skill_deception" to "dec", "skill_history" to "his",
            "skill_insight" to "ins", "skill_intimidation" to "itm", "skill_investigation" to "inv",
            "skill_medicine" to "med", "skill_nature" to "nat", "skill_perception" to "prc",
            "skill_performance" to "prf", "skill_persuasion" to "per", "skill_religion" to "rel",
            "skill_sleight_of_hand" to "slt", "skill_stealth" to "ste", "skill_survival" to "sur"
        )

        fun com.dndcharacterhandler.domain.model.Character.saveProficient(ability: String): Boolean = when (ability) {
            "str" -> strengthSaveProficient
            "dex" -> dexteritySaveProficient
            "con" -> constitutionSaveProficient
            "int" -> intelligenceSaveProficient
            "wis" -> wisdomSaveProficient
            "cha" -> charismaSaveProficient
            else -> false
        }

        fun spellcastingAbilityOf(key: String): com.dndcharacterhandler.domain.model.SpellcastingAbility? = when (key) {
            "str" -> com.dndcharacterhandler.domain.model.SpellcastingAbility.STRENGTH
            "dex" -> com.dndcharacterhandler.domain.model.SpellcastingAbility.DEXTERITY
            "con" -> com.dndcharacterhandler.domain.model.SpellcastingAbility.CONSTITUTION
            "int" -> com.dndcharacterhandler.domain.model.SpellcastingAbility.INTELLIGENCE
            "wis" -> com.dndcharacterhandler.domain.model.SpellcastingAbility.WISDOM
            "cha" -> com.dndcharacterhandler.domain.model.SpellcastingAbility.CHARISMA
            else -> null
        }

        fun String.toSlotList(): List<Int> =
            split(',').map { it.trim().toIntOrNull() ?: 0 }.let { values -> List(9) { values.getOrElse(it) { 0 } } }
    }
}
