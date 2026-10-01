package com.dndcharacterhandler.presentation.levelup

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dndcharacterhandler.domain.levelup.AbilityMethod
import com.dndcharacterhandler.domain.levelup.EquipmentPick
import com.dndcharacterhandler.domain.levelup.HitPointMethod
import com.dndcharacterhandler.domain.levelup.LevelUpAnswer
import com.dndcharacterhandler.domain.levelup.LevelUpDraft
import com.dndcharacterhandler.domain.levelup.LevelUpEngine
import com.dndcharacterhandler.domain.levelup.LevelUpPage
import com.dndcharacterhandler.domain.levelup.LevelUpSetup
import com.dndcharacterhandler.domain.levelup.LevelUpSummary
import com.dndcharacterhandler.domain.levelup.POINT_BUY_BUDGET
import com.dndcharacterhandler.domain.levelup.POINT_BUY_COSTS
import com.dndcharacterhandler.domain.levelup.STANDARD_ARRAY
import com.dndcharacterhandler.domain.levelup.TraitOption
import com.dndcharacterhandler.domain.model.AdvancementStep
import com.dndcharacterhandler.domain.model.AppLanguage
import com.dndcharacterhandler.domain.model.CatalogEquipmentRef
import com.dndcharacterhandler.domain.model.InventoryItem
import com.dndcharacterhandler.domain.model.NewItemIds
import com.dndcharacterhandler.domain.model.startingItems
import com.dndcharacterhandler.domain.model.CatalogFeature
import com.dndcharacterhandler.domain.model.CatalogText
import com.dndcharacterhandler.domain.model.CharacterBundle
import com.dndcharacterhandler.domain.model.CharacterCatalog
import com.dndcharacterhandler.domain.rules.CatalogFormulaText
import com.dndcharacterhandler.domain.rules.FormulaContext
import com.dndcharacterhandler.domain.rules.MulticlassRequirement
import com.dndcharacterhandler.domain.rules.abilityModifier
import com.dndcharacterhandler.domain.rules.abilityScores
import com.dndcharacterhandler.domain.rules.proficiencyBonusForLevel
import com.dndcharacterhandler.data.localization.LocalizedStrings
import com.dndcharacterhandler.presentation.components.OverlayCloseButton
import com.dndcharacterhandler.presentation.components.SizeToggle
import com.dndcharacterhandler.presentation.components.labelKey
import com.dndcharacterhandler.presentation.dice.DiceTableOverlay
import com.dndcharacterhandler.presentation.dice.DieIcon
import com.dndcharacterhandler.presentation.dice.dieTypeOf
import com.dndcharacterhandler.presentation.dice.DieType
import com.dndcharacterhandler.presentation.dice.LocalDiceSkin
import com.dndcharacterhandler.presentation.inventory.CurrencyCoinCluster
import com.dndcharacterhandler.presentation.inventory.CurrencyType
import com.dndcharacterhandler.presentation.inventory.InventorySectionCard
import com.dndcharacterhandler.presentation.spells.spellRangeDisplayLabel
import com.dndcharacterhandler.presentation.localization.LocalStrings
import com.dndcharacterhandler.presentation.localization.text
import com.dndcharacterhandler.presentation.theme.LocalDesignTokens

/**
 * The level-up wizard: one page per step of the draft (class, hit points, features, choices,
 * ability scores, subclass...) and a summary; the character changes only when [onApply] is called.
 */
@Composable
internal fun LevelUpWizard(
    bundle: CharacterBundle,
    catalog: CharacterCatalog,
    targetLevel: Int,
    /** Starting equipment as the inventory will hold it (the app's item catalog where it matches). */
    equipmentItem: (CatalogEquipmentRef, Int) -> InventoryItem,
    onDismiss: () -> Unit,
    onApply: (LevelUpDraft) -> Unit
) {
    val colors = LocalDesignTokens.current.colors
    val strings = LocalStrings.current
    val russian = strings.language == AppLanguage.RUSSIAN
    val engine = remember(catalog) { LevelUpEngine(catalog) }
    val formulas = remember(catalog) { CatalogFormulaText(catalog) }
    var draft by remember(bundle.character.id, targetLevel) { mutableStateOf(LevelUpDraft(targetLevel)) }
    var pageIndex by remember(bundle.character.id, targetLevel) { mutableIntStateOf(0) }
    val run = remember(draft, bundle, engine) { engine.run(bundle, draft) }
    val index = pageIndex.coerceIn(0, run.pages.lastIndex)
    val page = run.pages[index]
    val summary = (run.pages.last() as? LevelUpPage.Summary)?.summary
    // Texts show the numbers of the character as it will be after the level-up.
    val context = remember(summary, bundle) { summary?.let { formulaContext(bundle, it, catalog) } }
    val render: (CatalogText) -> String = { value -> formulas.render(value.get(russian), russian, context) }
    // The armour tags show the Dexterity the character will have.
    val dexterity = (summary?.baseScores ?: bundle.character.abilityScores())["dex"]
        ?.plus(summary?.abilityIncreases?.get("dex") ?: 0) ?: bundle.character.dexterity
    val answer = draft.answers[page.key]
    fun setAnswer(value: LevelUpAnswer) {
        draft = draft.copy(answers = draft.answers + (page.key to value))
    }
    // Hit points are rolled on the 3D dice table, never with a hidden random number.
    var roll by remember { mutableStateOf<HitDieRoll?>(null) }
    // So are a new character's ability scores: the index of the 4d6 throw on the table.
    var abilityRoll by remember { mutableStateOf<Int?>(null) }
    // The search of a spell page, kept while its spells are picked.
    var spellQuery by remember(page.key) { mutableStateOf("") }

    BackHandler(onBack = onDismiss)
    // A layer over the whole app rather than a dialog window: a dialog doesn't get the navigation
    // bar's inset, which pushed the buttons off the bottom of the screen.
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background.radialEnd)
            // Taps on empty space must not reach the screen underneath.
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
            .systemBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.padding(start = 20.dp, end = 72.dp, top = 20.dp, bottom = 8.dp)) {
                Text(
                    text = text("levelup_title"),
                    style = MaterialTheme.typography.headlineMedium,
                    color = colors.text.primary
                )
                Text(
                    text = strings.format("levelup_levels", summary?.fromLevel ?: bundle.character.level, summary?.toLevel ?: targetLevel) +
                        " • " + strings.format("levelup_step_counter", index + 1, run.pages.size),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.text.muted
                )
            }
            // Every page starts at its top.
            val listState = remember(page.key) { LazyListState() }
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                when (page) {
                    is LevelUpPage.Setup -> setupPage(strings, page, draft.setup, russian, catalog) { draft = draft.copy(setup = it) }
                    is LevelUpPage.ChooseClass -> chooseClassPage(strings, page, russian) { classId ->
                        draft = draft.copy(classPicks = draft.classPicks + (page.characterLevel to classId))
                    }
                    is LevelUpPage.HitPoints -> hitPointsPage(
                        strings, page, answer as? LevelUpAnswer.HitPoints, russian, ::setAnswer,
                        onRoll = { roll = HitDieRoll(page.key, page.characterClass.hitDie) }
                    )
                    is LevelUpPage.Features -> featuresPage(strings, page, answer as? LevelUpAnswer.Grants, russian, render, ::setAnswer)
                    is LevelUpPage.Traits -> traitsPage(strings, page, answer as? LevelUpAnswer.Traits, russian, ::setAnswer)
                    is LevelUpPage.Items -> itemsPage(strings, page, answer as? LevelUpAnswer.Items, russian, render, ::setAnswer)
                    is LevelUpPage.Spells -> spellsPage(
                        strings, page, answer as? LevelUpAnswer.Items, russian, spellQuery, { spellQuery = it }, ::setAnswer
                    )
                    is LevelUpPage.AbilityScores -> abilityScoresPage(strings, page, answer, russian, render, ::setAnswer)
                    is LevelUpPage.Subclass -> subclassPage(strings, page, answer as? LevelUpAnswer.Subclass, russian, render, ::setAnswer)
                    is LevelUpPage.BaseAbilities -> baseAbilitiesPage(
                        strings, page, answer as? LevelUpAnswer.BaseAbilities, ::setAnswer,
                        onRoll = { rollIndex -> abilityRoll = rollIndex }
                    )
                    is LevelUpPage.Species -> originPage(
                        strings = strings,
                        title = strings["levelup_species_title"],
                        currentName = page.currentName,
                        options = page.options.map { species ->
                            OriginChoice(
                                id = species.id,
                                name = species.name.get(russian),
                                subtitle = listOfNotNull(
                                    catalog.books[species.book]?.get(russian) ?: species.book,
                                    species.movement["walk"]?.let { strings.format("levelup_species_speed", it.toInt()) },
                                    species.senses["darkvision"]?.let { strings.format("levelup_species_darkvision", it.toInt()) }
                                ).joinToString(" • "),
                                body = render(species.text)
                            )
                        },
                        answer = answer as? LevelUpAnswer.Origin,
                        onAnswer = ::setAnswer
                    )
                    is LevelUpPage.Background -> originPage(
                        strings = strings,
                        title = strings["levelup_background_title"],
                        currentName = page.currentName,
                        options = page.options.map { background ->
                            OriginChoice(
                                id = background.id,
                                name = background.name.get(russian),
                                subtitle = catalog.books[background.book]?.get(russian) ?: background.book,
                                body = render(background.text)
                            )
                        },
                        answer = answer as? LevelUpAnswer.Origin,
                        onAnswer = ::setAnswer
                    )
                    is LevelUpPage.Size -> sizePage(strings, page, answer as? LevelUpAnswer.Size, russian, ::setAnswer)
                    is LevelUpPage.Equipment -> equipmentPage(
                        strings, page, answer as? LevelUpAnswer.Equipment, russian, engine, equipmentItem, dexterity, ::setAnswer
                    )
                    is LevelUpPage.Summary -> summaryPage(strings, page.summary, russian, catalog, equipmentItem, dexterity)
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = { pageIndex = index - 1 }, enabled = index > 0) {
                    Text(text("levelup_back"))
                }
                Spacer(modifier = Modifier.weight(1f))
                if (page is LevelUpPage.Summary) {
                    Button(onClick = { onApply(draft) }, enabled = run.isComplete) {
                        Text(text("levelup_apply"))
                    }
                } else {
                    Button(onClick = { pageIndex = index + 1 }, enabled = engine.isAnswered(page, draft)) {
                        Text(text("levelup_next"))
                    }
                }
            }
        }
        OverlayCloseButton(onClick = onDismiss, modifier = Modifier.align(Alignment.TopEnd))

        abilityRoll?.let { rollIndex ->
            // 4d6, the lowest die doesn't count; another throw on the open table replaces this one.
            DiceTableOverlay(
                selection = mapOf(DieType.D6 to 4),
                skin = LocalDiceSkin.current,
                onClose = { abilityRoll = null },
                onSettled = { dice ->
                    val values = dice.map { it.value() }
                    val total = values.sum() - (values.minOrNull() ?: 0)
                    val previous = (draft.answers[ABILITIES_PAGE] as? LevelUpAnswer.BaseAbilities)
                        ?.takeIf { it.method == AbilityMethod.ROLL }?.rolls.orEmpty()
                    val rolls = previous.toMutableList().also { list -> if (rollIndex < list.size) list[rollIndex] = total else list += total }
                    // Nothing is assigned for the player: once all six are in, they place each one.
                    draft = draft.copy(answers = draft.answers + (ABILITIES_PAGE to LevelUpAnswer.BaseAbilities(AbilityMethod.ROLL, emptyMap(), rolls)))
                }
            )
        }

        roll?.let { current ->
            DiceTableOverlay(
                selection = mapOf(current.dieType to 1),
                skin = LocalDiceSkin.current,
                onClose = { roll = null },
                onSettled = { dice ->
                    val value = dice.sumOf { it.value() }
                    draft = draft.copy(answers = draft.answers + (current.pageKey to LevelUpAnswer.HitPoints(HitPointMethod.ROLL, value)))
                }
            )
        }
    }
}

private const val ABILITIES_PAGE = "abilities"

/** A hit die being thrown on the dice table for the hit point page [pageKey]. */
private data class HitDieRoll(val pageKey: String, val sides: Int) {
    val dieType: DieType
        get() = dieTypeOf(sides) ?: DieType.D8
}

private fun formulaContext(bundle: CharacterBundle, summary: LevelUpSummary, catalog: CharacterCatalog): FormulaContext {
    val level = summary.classes.sumOf { it.levels }.coerceAtLeast(1)
    val base = summary.baseScores ?: bundle.character.abilityScores()
    val scores = base.mapValues { (ability, score) -> score + (summary.abilityIncreases[ability] ?: 0) }
    return FormulaContext(
        totalLevel = level,
        classLevels = summary.classes.mapNotNull { entry ->
            catalog.classes.firstOrNull { it.id == entry.classId }?.let { it.identifier to entry.levels }
        }.toMap(),
        subclasses = summary.classes.mapNotNull { entry ->
            entry.subclassId?.let { id -> catalog.subclasses.firstOrNull { it.id == id }?.identifier }
        }.toSet(),
        abilityModifiers = scores.mapValues { abilityModifier(it.value) },
        proficiencyBonus = proficiencyBonusForLevel(level)
    )
}

// --- Pages -------------------------------------------------------------------------------------------

private fun LazyListScope.pageTitle(title: String, hint: String? = null) {
    item(key = "title") {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, color = LocalDesignTokens.current.colors.text.primary)
            if (!hint.isNullOrBlank()) {
                Text(hint, style = MaterialTheme.typography.bodyMedium, color = LocalDesignTokens.current.colors.text.muted)
            }
        }
    }
}

private fun LazyListScope.setupPage(
    strings: LocalizedStrings,
    page: LevelUpPage.Setup,
    setup: LevelUpSetup?,
    russian: Boolean,
    catalog: CharacterCatalog,
    onChange: (LevelUpSetup) -> Unit
) {
    pageTitle(strings["levelup_setup_title"], strings["levelup_setup_hint"])
    // How the character got to its level: built by the wizard now, or already filled in by hand.
    val keep = setup?.keepExistingLevels ?: page.suggestKeep
    fun pickMode(value: Boolean) {
        // No class yet: remember the mode with an empty class, which the wizard doesn't accept.
        onChange((setup ?: LevelUpSetup(classId = "", keepExistingLevels = value)).copy(keepExistingLevels = value))
    }
    item(key = "rebuild") {
        ChoiceCard(
            title = strings["levelup_setup_rebuild"],
            subtitle = strings["levelup_setup_rebuild_hint"],
            selected = !keep,
            leading = { RadioButton(selected = !keep, onClick = null) },
            onClick = { pickMode(false) }
        )
    }
    item(key = "keep") {
        ChoiceCard(
            title = strings.format("levelup_setup_keep", page.currentLevel),
            subtitle = strings["levelup_setup_keep_hint"],
            selected = keep,
            leading = { RadioButton(selected = keep, onClick = null) },
            onClick = { pickMode(true) }
        )
    }
    item(key = "class_title") { SectionLabel(strings["levelup_class_pick"]) }
    items(page.classes, key = { it.id }) { characterClass ->
        val selected = setup?.classId == characterClass.id
        ChoiceCard(
            title = characterClass.name.get(russian),
            subtitle = catalog.books[characterClass.book]?.get(russian) ?: characterClass.book,
            selected = selected,
            leading = { RadioButton(selected = selected, onClick = null) },
            onClick = {
                onChange(LevelUpSetup(characterClass.id, subclassId = null, keepExistingLevels = setup?.keepExistingLevels ?: page.suggestKeep))
            }
        )
    }
    // A filled-in character past its subclass level names the subclass too, so later levels bring its features.
    val selectedClass = page.classes.firstOrNull { it.id == setup?.classId }
    val subclassLevel = selectedClass?.advancement?.filterIsInstance<AdvancementStep.Subclass>()?.firstOrNull()?.level
    if (setup != null && selectedClass != null && setup.keepExistingLevels && subclassLevel != null && page.currentLevel >= subclassLevel) {
        item(key = "subclass_title") {
            Text(strings["levelup_setup_subclass"], style = MaterialTheme.typography.titleMedium, color = LocalDesignTokens.current.colors.text.primary)
        }
        items(catalog.subclasses.filter { it.classIdentifier == selectedClass.identifier }, key = { "sub_" + it.id }) { subclass ->
            val selected = setup.subclassId == subclass.id
            ChoiceCard(
                title = subclass.name.get(russian),
                subtitle = catalog.books[subclass.book]?.get(russian) ?: subclass.book,
                selected = selected,
                leading = { RadioButton(selected = selected, onClick = null) },
                onClick = { onChange(setup.copy(subclassId = if (selected) null else subclass.id)) }
            )
        }
    }
}

private fun LazyListScope.chooseClassPage(strings: LocalizedStrings, page: LevelUpPage.ChooseClass, russian: Boolean, onPick: (String) -> Unit) {
    pageTitle(strings.format("levelup_class_title", page.characterLevel))
    if (page.current.isNotEmpty()) {
        item(key = "current") { SectionLabel(strings["levelup_class_current"]) }
        items(page.current, key = { "current_" + it.characterClass.id }) { choice ->
            val selected = page.selectedClassId == choice.characterClass.id
            ChoiceCard(
                title = strings.format("levelup_class_level", choice.characterClass.name.get(russian), choice.levels, choice.levels + 1),
                subtitle = choice.subclass?.name?.get(russian),
                selected = selected,
                leading = { RadioButton(selected = selected, onClick = null) },
                onClick = { onPick(choice.characterClass.id) }
            )
        }
    }
    item(key = "new") { SectionLabel(strings[if (page.current.isEmpty()) "levelup_class_pick" else "levelup_class_new"]) }
    if (page.requirements.any { !it.isMet }) {
        item(key = "warning") { RequirementWarning(strings, page.requirements.filterNot { it.isMet }) }
    }
    items(page.others, key = { "other_" + it.id }) { characterClass ->
        val selected = page.selectedClassId == characterClass.id
        ChoiceCard(
            title = characterClass.name.get(russian),
            subtitle = "d${characterClass.hitDie}",
            selected = selected,
            leading = { RadioButton(selected = selected, onClick = null) },
            onClick = { onPick(characterClass.id) }
        )
    }
}

private fun LazyListScope.hitPointsPage(
    strings: LocalizedStrings,
    page: LevelUpPage.HitPoints,
    answer: LevelUpAnswer.HitPoints?,
    russian: Boolean,
    onAnswer: (LevelUpAnswer) -> Unit,
    onRoll: () -> Unit
) {
    val die = page.characterClass.hitDie
    pageTitle(strings["levelup_hp_title"], strings.format("levelup_class_level", page.characterClass.name.get(russian), page.classLevel - 1, page.classLevel))
    if (page.takesMaximum) {
        item(key = "max") { InfoCard(strings.format("levelup_hp_maximum", die)) }
    } else {
        item(key = "average") {
            ChoiceCard(
                title = strings.format("levelup_hp_average", page.average),
                selected = answer?.method == HitPointMethod.AVERAGE,
                leading = { RadioButton(selected = answer?.method == HitPointMethod.AVERAGE, onClick = null) },
                onClick = { onAnswer(LevelUpAnswer.HitPoints(HitPointMethod.AVERAGE, page.average)) }
            )
        }
        item(key = "roll") {
            val rolled = answer?.takeIf { it.method == HitPointMethod.ROLL }
            ChoiceCard(
                title = if (rolled != null) strings.format("levelup_hp_rolled", rolled.value) else strings.format("levelup_hp_roll", "d$die"),
                selected = rolled != null,
                // The die that will be thrown, in the player's dice skin.
                leading = { DieIcon(dieTypeOf(die) ?: DieType.D8, LocalDiceSkin.current, Modifier.size(32.dp)) },
                onClick = onRoll
            )
        }
    }
    item(key = "con") { InfoCard(strings.format("levelup_hp_constitution", signed(page.constitutionModifier))) }
}

private fun LazyListScope.featuresPage(
    strings: LocalizedStrings,
    page: LevelUpPage.Features,
    answer: LevelUpAnswer.Grants?,
    russian: Boolean,
    render: (CatalogText) -> String,
    onAnswer: (LevelUpAnswer) -> Unit
) {
    val declined = answer?.declined.orEmpty()
    pageTitle(strings["levelup_features_title"], strings.format("levelup_class_level", page.characterClass.name.get(russian), page.classLevel - 1, page.classLevel))
    items(page.grants, key = { "grant_" + it.feature.id }) { grant ->
        val taken = grant.feature.id !in declined
        FeatureCard(
            feature = grant.feature,
            russian = russian,
            render = render,
            subtitle = if (grant.optional) strings["levelup_optional"] else null,
            selected = taken,
            leading = if (grant.optional) {
                {
                    Checkbox(checked = taken, onCheckedChange = { checked ->
                        onAnswer(LevelUpAnswer.Grants(if (checked) declined - grant.feature.id else declined + grant.feature.id))
                    })
                }
            } else {
                null
            }
        )
    }
    if (page.scaleChanges.isNotEmpty()) {
        item(key = "scales") {
            InfoCard(
                title = strings["levelup_scale_title"],
                body = page.scaleChanges.joinToString("\n") { change ->
                    "${change.title.get(russian)}: ${change.from?.let { "$it → " }.orEmpty()}${change.to}"
                }
            )
        }
    }
    if (page.spells.isNotEmpty()) {
        item(key = "spells") {
            InfoCard(title = strings["levelup_spells_granted"], body = page.spells.joinToString(", ") { it.name.get(russian) })
        }
    }
}

private fun LazyListScope.traitsPage(
    strings: LocalizedStrings,
    page: LevelUpPage.Traits,
    answer: LevelUpAnswer.Traits?,
    russian: Boolean,
    onAnswer: (LevelUpAnswer) -> Unit
) {
    pageTitle(page.step.title.get(russian).ifBlank { page.source.get(russian) }, page.step.hint.get(russian).ifBlank { page.source.get(russian) })
    if (page.granted.isNotEmpty()) {
        item(key = "granted") {
            InfoCard(title = strings["levelup_traits_granted"], body = page.granted.joinToString(", ") { it.name.get(russian) })
        }
    }
    val picks = answer?.picks ?: List(page.groups.size) { emptySet() }
    page.groups.forEachIndexed { groupIndex, group ->
        val required = page.required[groupIndex]
        val chosen = picks.getOrNull(groupIndex).orEmpty()
        item(key = "group_$groupIndex") { SectionLabel(strings.format("levelup_pick_count", chosen.size, required)) }
        items(group.options, key = { "trait_${groupIndex}_" + it.key }) { option ->
            val selected = option.key in chosen
            val blocked = option.alreadyHas && !page.step.allowReplacements
            ChoiceCard(
                title = option.name.get(russian),
                subtitle = if (option.alreadyHas) strings["levelup_already_have"] else null,
                selected = selected,
                enabled = !blocked && (selected || chosen.size < required),
                leading = { Checkbox(checked = selected, onCheckedChange = null, enabled = !blocked) },
                onClick = {
                    val updated = if (selected) chosen - option.key else chosen + option.key
                    onAnswer(LevelUpAnswer.Traits(picks.toMutableList().also { list ->
                        while (list.size <= groupIndex) list.add(emptySet())
                        list[groupIndex] = updated
                    }))
                }
            )
        }
    }
}

private fun LazyListScope.itemsPage(
    strings: LocalizedStrings,
    page: LevelUpPage.Items,
    answer: LevelUpAnswer.Items?,
    russian: Boolean,
    render: (CatalogText) -> String,
    onAnswer: (LevelUpAnswer) -> Unit
) {
    val picks = answer?.picks.orEmpty()
    val replaced = answer?.replacedId
    val required = page.required + if (replaced != null) 1 else 0
    pageTitle(page.step.title.get(russian).ifBlank { page.source.get(russian) }, page.source.get(russian))
    if (page.replaceable.isNotEmpty()) {
        item(key = "replace_title") { SectionLabel(strings["levelup_items_replace"]) }
        item(key = "replace_none") {
            ChoiceCard(
                title = strings["levelup_items_replace_none"],
                selected = replaced == null,
                leading = { RadioButton(selected = replaced == null, onClick = null) },
                onClick = { onAnswer(LevelUpAnswer.Items(picks, replacedId = null)) }
            )
        }
        items(page.replaceable, key = { "replace_" + it.id }) { feature ->
            ChoiceCard(
                title = feature.name.get(russian),
                selected = replaced == feature.id,
                leading = { RadioButton(selected = replaced == feature.id, onClick = null) },
                onClick = { onAnswer(LevelUpAnswer.Items(picks - feature.id, replacedId = feature.id)) }
            )
        }
    }
    item(key = "count") { SectionLabel(strings.format("levelup_pick_count", picks.size, required)) }
    items(page.options, key = { "item_" + it.feature.id }) { option ->
        val selected = option.feature.id in picks
        val blocked = option.known && !option.feature.repeatable && option.feature.id != replaced
        FeatureCard(
            feature = option.feature,
            russian = russian,
            render = render,
            subtitle = listOfNotNull(
                option.feature.requirements.takeIf { it.isNotBlank() },
                if (option.known) strings["levelup_already_have"] else null
            ).joinToString(" • ").ifBlank { null },
            selected = selected,
            enabled = !blocked && (selected || picks.size < required),
            leading = {
                Checkbox(checked = selected, onCheckedChange = null, enabled = !blocked)
            },
            onClick = {
                onAnswer(LevelUpAnswer.Items(if (selected) picks - option.feature.id else picks + option.feature.id, replaced))
            }
        )
    }
}

/**
 * Spells to pick, grouped by level, with a search: each one's school, range and tags, and its text
 * unfolding; a known one may be swapped where the step allows it.
 */
private fun LazyListScope.spellsPage(
    strings: LocalizedStrings,
    page: LevelUpPage.Spells,
    answer: LevelUpAnswer.Items?,
    russian: Boolean,
    query: String,
    onQueryChange: (String) -> Unit,
    onAnswer: (LevelUpAnswer) -> Unit
) {
    val picks = answer?.picks.orEmpty()
    val replaced = answer?.replacedId
    val required = page.required + if (replaced != null) 1 else 0
    pageTitle(page.step.title.get(russian).ifBlank { page.source.get(russian) }, page.source.get(russian))
    if (page.replaceable.isNotEmpty()) {
        item(key = "replace_title") { SectionLabel(strings["levelup_items_replace"]) }
        item(key = "replace_none") {
            ChoiceCard(
                title = strings["levelup_items_replace_none"],
                selected = replaced == null,
                leading = { RadioButton(selected = replaced == null, onClick = null) },
                onClick = { onAnswer(LevelUpAnswer.Items(picks, replacedId = null)) }
            )
        }
        items(page.replaceable, key = { "replace_" + it.id }) { spell ->
            ChoiceCard(
                title = spell.name.get(russian),
                subtitle = spellLevelLabel(strings, spell.level ?: 0),
                selected = replaced == spell.id,
                leading = { RadioButton(selected = replaced == spell.id, onClick = null) },
                onClick = { onAnswer(LevelUpAnswer.Items(picks - spell.id, replacedId = spell.id)) }
            )
        }
    }
    item(key = "count") { SectionLabel(strings.format("levelup_pick_count", picks.size, required)) }
    // Long lists (a spellbook, every class's cantrips) get a search.
    if (page.options.size > 12) {
        item(key = "search") {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(strings["spells_search_placeholder"]) },
                singleLine = true
            )
        }
    }
    val needle = query.trim()
    val shown = page.options.filter { option ->
        needle.isEmpty() || option.spell.id in picks ||
            listOf(option.spell.name.en, option.spell.name.ru).any { it.contains(needle, ignoreCase = true) }
    }
    shown.groupBy { it.spell.level ?: 0 }.toSortedMap().forEach { (level, options) ->
        // One level only (cantrips, a Mystic Arcanum): the title says it already.
        if (shown.map { it.spell.level }.distinct().size > 1) {
            item(key = "level_$level") { SectionLabel(spellLevelLabel(strings, level)) }
        }
        items(options, key = { "spell_" + it.spell.id }) { option ->
            val spell = option.spell
            val selected = spell.id in picks
            val blocked = option.known && spell.id != replaced
            ExpandableCard(
                title = spell.name.get(russian),
                subtitle = listOfNotNull(
                    spellSchoolLabel(strings, spell.school),
                    spellRangeDisplayLabel(spell.range, strings),
                    if (spell.concentration) strings["spells_concentration"] else null,
                    if (spell.ritual) strings["spells_ritual"] else null,
                    if (option.known) strings["levelup_already_have"] else null
                ).joinToString(" • "),
                body = spell.text.get(russian),
                selected = selected,
                enabled = !blocked && (selected || picks.size < required),
                leading = { Checkbox(checked = selected, onCheckedChange = null, enabled = !blocked) },
                onClick = { onAnswer(LevelUpAnswer.Items(if (selected) picks - spell.id else picks + spell.id, replaced)) }
            )
        }
    }
}

private fun spellLevelLabel(strings: LocalizedStrings, level: Int): String =
    strings[if (level == 0) "spells_level_cantrips" else "spells_level_$level"]

/** Foundry's school keys -> the app's school labels. */
private fun spellSchoolLabel(strings: LocalizedStrings, school: String): String? = when (school) {
    "abj" -> "spells_school_abjuration"
    "con" -> "spells_school_conjuration"
    "div" -> "spells_school_divination"
    "enc" -> "spells_school_enchantment"
    "evo" -> "spells_school_evocation"
    "ill" -> "spells_school_illusion"
    "nec" -> "spells_school_necromancy"
    "trs" -> "spells_school_transmutation"
    else -> null
}?.let { strings[it] }

private fun LazyListScope.abilityScoresPage(
    strings: LocalizedStrings,
    page: LevelUpPage.AbilityScores,
    answer: LevelUpAnswer?,
    russian: Boolean,
    render: (CatalogText) -> String,
    onAnswer: (LevelUpAnswer) -> Unit
) {
    pageTitle(strings["levelup_asi_title"], page.source.get(russian))
    if (page.allowFeat) {
        item(key = "modes") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = answer !is LevelUpAnswer.Feat,
                    onClick = { onAnswer(LevelUpAnswer.AbilityScores(emptyMap())) },
                    label = { Text(strings["levelup_asi_scores"]) }
                )
                FilterChip(
                    selected = answer is LevelUpAnswer.Feat,
                    onClick = { if (answer !is LevelUpAnswer.Feat) onAnswer(LevelUpAnswer.Feat("")) },
                    label = { Text(strings["levelup_asi_feat"]) }
                )
            }
        }
    }
    if (answer is LevelUpAnswer.Feat) {
        featList(strings, page, answer, russian, render, onAnswer)
        return
    }
    val increases = (answer as? LevelUpAnswer.AbilityScores)?.increases.orEmpty()
    val spent = increases.values.sum()
    item(key = "points") { SectionLabel(strings.format("levelup_asi_points", page.required - spent)) }
    items(LevelUpPage.AbilityScores.ABILITIES, key = { "ability_$it" }) { ability ->
        val current = page.scores[ability] ?: 10
        val added = increases[ability] ?: 0
        val locked = ability !in page.abilities
        AbilityRow(
            label = abilityLabel(strings, ability),
            value = current + added,
            added = added,
            canAdd = !locked && added < page.cap && current + added < page.maxScore && spent < page.required,
            canRemove = added > 0,
            onAdd = { onAnswer(LevelUpAnswer.AbilityScores(increases + (ability to added + 1))) },
            onRemove = {
                onAnswer(LevelUpAnswer.AbilityScores((increases + (ability to added - 1)).filterValues { it > 0 }))
            }
        )
    }
}

private fun LazyListScope.featList(
    strings: LocalizedStrings,
    page: LevelUpPage.AbilityScores,
    answer: LevelUpAnswer.Feat,
    russian: Boolean,
    render: (CatalogText) -> String,
    onAnswer: (LevelUpAnswer) -> Unit
) {
    items(page.feats, key = { "feat_" + it.id }) { feat ->
        val selected = answer.featId == feat.id
        FeatureCard(
            feature = feat,
            russian = russian,
            render = render,
            subtitle = feat.requirements.takeIf { it.isNotBlank() }?.let { strings.format("levelup_asi_requirements", it) },
            selected = selected,
            leading = { RadioButton(selected = selected, onClick = null) },
            onClick = { onAnswer(LevelUpAnswer.Feat(feat.id)) }
        )
    }
}

private fun LazyListScope.subclassPage(
    strings: LocalizedStrings,
    page: LevelUpPage.Subclass,
    answer: LevelUpAnswer.Subclass?,
    russian: Boolean,
    render: (CatalogText) -> String,
    onAnswer: (LevelUpAnswer) -> Unit
) {
    pageTitle(strings["levelup_subclass_title"], page.characterClass.name.get(russian))
    items(page.options, key = { "subclass_" + it.id }) { subclass ->
        val selected = answer?.subclassId == subclass.id
        ExpandableCard(
            title = subclass.name.get(russian),
            subtitle = null,
            body = render(subclass.text),
            selected = selected,
            leading = { RadioButton(selected = selected, onClick = null) },
            onClick = { onAnswer(LevelUpAnswer.Subclass(subclass.id)) }
        )
    }
}

private fun LazyListScope.baseAbilitiesPage(
    strings: LocalizedStrings,
    page: LevelUpPage.BaseAbilities,
    answer: LevelUpAnswer.BaseAbilities?,
    onAnswer: (LevelUpAnswer) -> Unit,
    onRoll: (Int) -> Unit
) {
    val abilities = LevelUpPage.AbilityScores.ABILITIES
    pageTitle(strings["levelup_abilities_title"], strings["levelup_abilities_hint"])
    val methods = listOf(
        AbilityMethod.STANDARD_ARRAY to "levelup_abilities_standard",
        AbilityMethod.POINT_BUY to "levelup_abilities_point_buy",
        AbilityMethod.ROLL to "levelup_abilities_roll",
        AbilityMethod.KEEP to "levelup_abilities_keep"
    )
    items(methods, key = { "method_" + it.first.name }) { (method, label) ->
        val selected = answer?.method == method
        ChoiceCard(
            title = strings[label],
            subtitle = strings[label + "_hint"],
            selected = selected,
            leading = { RadioButton(selected = selected, onClick = null) },
            onClick = {
                if (!selected) {
                    onAnswer(
                        when (method) {
                            AbilityMethod.STANDARD_ARRAY -> LevelUpAnswer.BaseAbilities(method, emptyMap())
                            AbilityMethod.POINT_BUY -> LevelUpAnswer.BaseAbilities(method, abilities.associateWith { 8 })
                            AbilityMethod.ROLL -> LevelUpAnswer.BaseAbilities(method, emptyMap())
                            AbilityMethod.KEEP -> LevelUpAnswer.BaseAbilities(method, page.currentScores)
                        }
                    )
                }
            }
        )
    }
    if (answer == null) return
    val scores = answer.scores
    // The standard array or the rolls, placed by the player one by one: every ability starts empty.
    fun assignment(pool: List<Int>) {
        val free = freeValues(pool, scores)
        item(key = "assign") {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                SectionLabel(strings["levelup_abilities_assign"])
                if (free.isNotEmpty()) {
                    Text(
                        strings.format("levelup_abilities_free", free.joinToString(", ")),
                        style = MaterialTheme.typography.bodyMedium,
                        color = LocalDesignTokens.current.colors.text.muted
                    )
                }
            }
        }
        items(abilities, key = { "score_$it" }) { ability ->
            AssignedScoreRow(
                strings = strings,
                ability = ability,
                value = scores[ability],
                free = free,
                onPick = { value -> onAnswer(answer.copy(scores = if (value == null) scores - ability else scores + (ability to value))) }
            )
        }
    }
    when (answer.method) {
        AbilityMethod.STANDARD_ARRAY -> assignment(STANDARD_ARRAY)
        AbilityMethod.POINT_BUY -> {
            val spent = abilities.sumOf { POINT_BUY_COSTS[scores[it] ?: 8] ?: 0 }
            item(key = "points") { SectionLabel(strings.format("levelup_asi_points", POINT_BUY_BUDGET - spent)) }
            items(abilities, key = { "score_$it" }) { ability ->
                val value = scores[ability] ?: 8
                val nextCost = POINT_BUY_COSTS[value + 1]?.minus(POINT_BUY_COSTS[value] ?: 0)
                ScoreRow(abilityLabel(strings, ability), value) {
                    IconButton(onClick = { onAnswer(answer.copy(scores = scores + (ability to value - 1))) }, enabled = value > 8) {
                        Icon(Icons.Outlined.Remove, contentDescription = null)
                    }
                    IconButton(
                        onClick = { onAnswer(answer.copy(scores = scores + (ability to value + 1))) },
                        enabled = nextCost != null && spent + nextCost <= POINT_BUY_BUDGET
                    ) {
                        Icon(Icons.Outlined.Add, contentDescription = null)
                    }
                }
            }
        }
        AbilityMethod.ROLL -> {
            val rolls = answer.rolls
            if (rolls.isNotEmpty()) {
                item(key = "rolls") { InfoCard(strings.format("levelup_abilities_rolls", rolls.joinToString(", "))) }
            }
            if (rolls.size < abilities.size) {
                item(key = "throw") {
                    ChoiceCard(
                        title = strings.format("levelup_abilities_roll_next", rolls.size + 1),
                        subtitle = strings["levelup_abilities_roll_hint"],
                        selected = false,
                        leading = { DieIcon(DieType.D6, LocalDiceSkin.current, Modifier.size(32.dp)) },
                        onClick = { onRoll(rolls.size) }
                    )
                }
            } else {
                assignment(rolls)
            }
        }
        AbilityMethod.KEEP -> items(abilities, key = { "score_$it" }) { ability ->
            ScoreRow(abilityLabel(strings, ability), scores[ability] ?: 10)
        }
    }
}

/** A species or background as offered on its page. */
private data class OriginChoice(val id: String, val name: String, val subtitle: String, val body: String)

private fun LazyListScope.originPage(
    strings: LocalizedStrings,
    title: String,
    currentName: String,
    options: List<OriginChoice>,
    answer: LevelUpAnswer.Origin?,
    onAnswer: (LevelUpAnswer) -> Unit
) {
    pageTitle(title)
    // The character's own (homebrew) one: nothing from the catalog.
    item(key = "own") {
        val selected = answer != null && answer.id == null
        ChoiceCard(
            title = strings["levelup_origin_keep"] + currentName.trim().takeIf { it.isNotEmpty() }?.let { " — $it" }.orEmpty(),
            subtitle = strings["levelup_origin_keep_hint"],
            selected = selected,
            leading = { RadioButton(selected = selected, onClick = null) },
            onClick = { onAnswer(LevelUpAnswer.Origin(null)) }
        )
    }
    items(options, key = { "origin_" + it.id }) { option ->
        val selected = answer?.id == option.id
        ExpandableCard(
            title = option.name,
            subtitle = option.subtitle,
            body = option.body,
            selected = selected,
            leading = { RadioButton(selected = selected, onClick = null) },
            onClick = { onAnswer(LevelUpAnswer.Origin(option.id)) }
        )
    }
}

/**
 * The starting equipment options: each one's items as the inventory lists them, its "of your
 * choice" items and its coins; or just the gold.
 */
private fun LazyListScope.equipmentPage(
    strings: LocalizedStrings,
    page: LevelUpPage.Equipment,
    answer: LevelUpAnswer.Equipment?,
    russian: Boolean,
    engine: LevelUpEngine,
    equipmentItem: (CatalogEquipmentRef, Int) -> InventoryItem,
    dexterity: Int,
    onAnswer: (LevelUpAnswer) -> Unit
) {
    pageTitle(strings["levelup_equipment_title"], page.source.get(russian))
    val letters = if (russian) "АБВГДЕЖЗ" else "ABCDEFGH"
    page.options.forEachIndexed { index, option ->
        val selected = answer?.option == index
        item(key = "option_$index") {
            // A pack shows as a container with its contents.
            val items = remember(option, equipmentItem) {
                val ids = NewItemIds()
                option.items.flatMap { engine.startingItems(it.item, it.count, russian, equipmentItem, ids) }
            }
            EquipmentOptionCard(
                title = if (option.isWealth) {
                    strings["levelup_equipment_gold"]
                } else {
                    strings.format("levelup_equipment_option", letters.getOrElse(index) { '?' }.toString())
                },
                selected = selected,
                items = items,
                dexterity = dexterity,
                coins = option.coins,
                coinsOnTitle = option.isWealth,
                strings = strings,
                onSelect = { if (!selected) onAnswer(LevelUpAnswer.Equipment(index)) }
            ) {
                // Picking one of the choices takes this option too.
                val picks = if (selected) answer?.picks.orEmpty() else emptyList()
                option.choices.forEachIndexed { choiceIndex, choice ->
                    EquipmentChoiceField(
                        label = strings["levelup_equipment_choose"],
                        choice = choice.options,
                        selected = engine.pickFor(choice, picks.getOrNull(choiceIndex)),
                        russian = russian,
                        onPick = { id ->
                            val updated = List(option.choices.size) { picks.getOrNull(it).orEmpty() }.toMutableList()
                            updated[choiceIndex] = id
                            onAnswer(LevelUpAnswer.Equipment(index, updated))
                        }
                    )
                }
            }
        }
    }
}

/** The species' size, where it may be one of several: the figures of the Biography screen. */
private fun LazyListScope.sizePage(
    strings: LocalizedStrings,
    page: LevelUpPage.Size,
    answer: LevelUpAnswer.Size?,
    russian: Boolean,
    onAnswer: (LevelUpAnswer) -> Unit
) {
    pageTitle(strings["levelup_size_title"], page.source.get(russian))
    item(key = "sizes") {
        SizeToggle(
            selected = answer?.size,
            sizes = page.sizes,
            onSelect = { onAnswer(LevelUpAnswer.Size(it)) }
        )
    }
}

private fun equipmentLabel(pick: EquipmentPick, russian: Boolean): String =
    pick.item.name.get(russian) + if (pick.count > 1) " ×${pick.count}" else ""

private fun LazyListScope.summaryPage(
    strings: LocalizedStrings,
    summary: LevelUpSummary,
    russian: Boolean,
    catalog: CharacterCatalog,
    equipmentItem: (CatalogEquipmentRef, Int) -> InventoryItem,
    dexterity: Int
) {
    pageTitle(strings["levelup_summary_title"])
    if (summary.fromLevel == summary.toLevel) {
        item(key = "setup_only") { InfoCard(strings["levelup_summary_setup_only"]) }
    }
    if (summary.unmetRequirements.isNotEmpty()) {
        item(key = "warning") { RequirementWarning(strings, summary.unmetRequirements) }
    }
    item(key = "classes") {
        InfoCard(
            title = strings["levelup_summary_classes"],
            body = summary.classes.joinToString("\n") { entry ->
                val name = catalog.classes.firstOrNull { it.id == entry.classId }?.name?.get(russian).orEmpty()
                val subclass = entry.subclassId?.let { id -> catalog.subclasses.firstOrNull { it.id == id }?.name?.get(russian) }
                "$name ${entry.levels}" + (subclass?.let { " — $it" } ?: "")
            }
        )
    }
    if (summary.species != null || summary.background != null) {
        item(key = "origin") {
            InfoCard(
                title = strings["levelup_summary_origin"],
                body = listOfNotNull(
                    summary.species?.let { strings.format("levelup_summary_species", it.name.get(russian)) },
                    summary.size?.let { strings.format("levelup_summary_size", strings[it.labelKey]) },
                    summary.background?.let { strings.format("levelup_summary_background", it.name.get(russian)) }
                ).joinToString("\n")
            )
        }
    }
    summary.baseScores?.let { scores ->
        item(key = "base_scores") {
            InfoCard(
                title = strings["levelup_summary_base_scores"],
                body = LevelUpPage.AbilityScores.ABILITIES.joinToString(", ") { "${abilityLabel(strings, it)} ${scores[it] ?: 10}" }
            )
        }
    }
    if (summary.toLevel > summary.fromLevel) {
        item(key = "hp") { InfoCard(strings.format("levelup_summary_hp", signed(summary.hitPointGain))) }
    }
    summaryList(strings, "features", "levelup_summary_features", summary.addedFeatures.map { it.name.get(russian) })
    summaryList(strings, "removed", "levelup_summary_removed", summary.removedFeatures.map { it.name.get(russian) })
    if (summary.abilityIncreases.isNotEmpty()) {
        item(key = "abilities") {
            InfoCard(
                title = strings["levelup_summary_abilities"],
                body = summary.abilityIncreases.entries.joinToString(", ") { "${abilityLabel(strings, it.key)} ${signed(it.value)}" }
            )
        }
    }
    summaryList(strings, "proficiencies", "levelup_summary_proficiencies", summary.proficiencies.names(russian))
    summaryList(strings, "expertise", "levelup_summary_expertise", summary.expertise.names(russian))
    summaryList(strings, "masteries", "levelup_summary_mastery", summary.masteries.names(russian))
    summaryList(strings, "defenses", "levelup_summary_defenses", summary.defenses.names(russian))
    if (!summary.spellSlots.isEmpty) {
        item(key = "slots") {
            val slots = summary.spellSlots.combined().mapIndexedNotNull { index, count -> if (count > 0) "${index + 1}: $count" else null }
            InfoCard(title = strings["levelup_summary_spell_slots"], body = slots.joinToString(" • "))
        }
    }
    summaryList(strings, "learned_spells", "levelup_summary_spells_learned", summary.spellsLearned.map { it.name.get(russian) })
    summaryList(strings, "forgotten_spells", "levelup_summary_spells_forgotten", summary.spellsForgotten.map { it.name.get(russian) })
    summaryList(strings, "granted_spells", "levelup_spells_granted", summary.spellsGranted.map { it.name.get(russian) })
    if (summary.equipment.isNotEmpty() || summary.coins.values.any { it > 0 }) {
        item(key = "equipment") {
            val items = remember(summary.equipment, equipmentItem) {
                val ids = NewItemIds()
                summary.equipment.flatMap { catalog.startingItems(it.item, it.count, russian, equipmentItem, ids) }
            }
            val colors = LocalDesignTokens.current.colors
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = colors.surface.card,
                border = BorderStroke(1.dp, colors.border.muted)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(strings["levelup_summary_equipment"], style = MaterialTheme.typography.titleMedium, color = colors.text.primary)
                    if (items.isNotEmpty()) InventorySectionCard(items, dexterity)
                    CoinsAmount(summary.coins, strings)
                }
            }
        }
    }
    if (summary.spellChoices.isNotEmpty()) {
        item(key = "spell_choices") {
            InfoCard(
                title = strings["levelup_summary_spells_todo"],
                body = summary.spellChoices.joinToString("\n") { note ->
                    "${note.title.get(russian).ifBlank { note.source.get(russian) }}: ${note.count}"
                }
            )
        }
    }
}

private fun List<TraitOption>.names(russian: Boolean) = map { it.name.get(russian) }

private fun LazyListScope.summaryList(strings: LocalizedStrings, key: String, titleKey: String, values: List<String>) {
    if (values.isEmpty()) return
    item(key = key) { InfoCard(title = strings[titleKey], body = values.joinToString(", ")) }
}

// --- Building blocks ---------------------------------------------------------------------------

private fun abilityLabel(strings: LocalizedStrings, ability: String): String = strings[
    when (ability) {
        "str" -> "ability_strength"
        "dex" -> "ability_dexterity"
        "con" -> "ability_constitution"
        "int" -> "ability_intelligence"
        "wis" -> "ability_wisdom"
        else -> "ability_charisma"
    }
]

private fun signed(value: Int) = if (value >= 0) "+$value" else value.toString()

@Composable
private fun SectionLabel(label: String) {
    Text(
        text = label,
        modifier = Modifier.padding(top = 6.dp),
        style = MaterialTheme.typography.titleMedium,
        color = LocalDesignTokens.current.colors.text.primary
    )
}

@Composable
private fun RequirementWarning(strings: LocalizedStrings, requirements: List<MulticlassRequirement>) {
    val colors = LocalDesignTokens.current.colors
    val catalogNames = requirements.joinToString("\n") { requirement ->
        val abilities = requirement.abilities.map { abilityLabel(strings, it) }
        val joined = abilities.joinToString(if (requirement.needsAll) strings["levelup_and"] else strings["levelup_or"])
        strings.format("levelup_requirement_warning", joined)
    }
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = colors.surface.inspiration,
        border = BorderStroke(1.dp, colors.accent.xpCapped)
    ) {
        Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Outlined.WarningAmber, contentDescription = null, tint = colors.accent.inspiration)
            Text(catalogNames, style = MaterialTheme.typography.bodyMedium, color = colors.text.primary)
        }
    }
}

@Composable
private fun InfoCard(title: String? = null, body: String? = null) {
    val colors = LocalDesignTokens.current.colors
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = colors.surface.card,
        border = BorderStroke(1.dp, colors.border.muted)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (title != null) Text(title, style = MaterialTheme.typography.titleMedium, color = colors.text.primary)
            if (body != null) Text(body, style = MaterialTheme.typography.bodyMedium, color = colors.text.muted)
        }
    }
}

@Composable
private fun InfoCard(text: String) = InfoCard(title = null, body = text)

@Composable
private fun ChoiceCard(
    title: String,
    subtitle: String? = null,
    selected: Boolean,
    enabled: Boolean = true,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)?
) {
    val colors = LocalDesignTokens.current.colors
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null && enabled) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(12.dp),
        color = if (selected) colors.surface.selected else colors.surface.card,
        border = BorderStroke(1.dp, if (selected) colors.border.selected else colors.border.muted)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            leading?.invoke()
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (enabled) colors.text.primary else colors.text.subtle
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = colors.text.muted)
                }
            }
            trailing?.invoke()
        }
    }
}

@Composable
private fun FeatureCard(
    feature: CatalogFeature,
    russian: Boolean,
    render: (CatalogText) -> String,
    subtitle: String?,
    selected: Boolean,
    enabled: Boolean = true,
    leading: (@Composable () -> Unit)?,
    onClick: (() -> Unit)? = null
) = ExpandableCard(
    title = feature.name.get(russian),
    subtitle = subtitle,
    body = render(feature.text),
    selected = selected,
    enabled = enabled,
    leading = leading,
    onClick = onClick
)

/** A choice with a description that unfolds with the arrow (the shared [ExpandableCard]). */
@Composable
private fun ExpandableCard(
    title: String,
    subtitle: String?,
    body: String,
    selected: Boolean,
    enabled: Boolean = true,
    leading: (@Composable () -> Unit)?,
    onClick: (() -> Unit)?
) {
    var expanded by remember { mutableStateOf(false) }
    com.dndcharacterhandler.presentation.components.ExpandableCard(
        title = title,
        expanded = expanded,
        onExpandedChange = { expanded = it },
        subtitle = subtitle,
        body = body,
        selected = selected,
        enabled = enabled,
        leading = leading,
        onClick = onClick
    )
}

/** Values of [pool] no ability has yet, largest first; a value rolled twice is there twice. */
private fun freeValues(pool: List<Int>, scores: Map<String, Int>): List<Int> {
    val left = pool.toMutableList()
    scores.values.forEach { left.remove(it) }
    return left.sortedDescending()
}

/** An ability with its score and modifier ("—" while it has none); [trailing] holds the controls. */
@Composable
private fun ScoreRow(
    label: String,
    value: Int?,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null
) {
    val colors = LocalDesignTokens.current.colors
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(12.dp),
        color = colors.surface.card,
        border = BorderStroke(1.dp, colors.border.muted)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp).heightIn(min = 48.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, color = colors.text.primary)
            Text(
                text = value?.let { "$it (${signed(abilityModifier(it))})" } ?: "—",
                modifier = Modifier.padding(horizontal = 8.dp),
                style = MaterialTheme.typography.titleMedium,
                color = colors.text.primary
            )
            trailing?.invoke(this)
        }
    }
}

/**
 * An ability taking one value of a fixed set (the standard array, the rolls): a tap lists the
 * values still [free]; a placed value can be changed or taken back.
 */
@Composable
private fun AssignedScoreRow(
    strings: LocalizedStrings,
    ability: String,
    value: Int?,
    free: List<Int>,
    onPick: (Int?) -> Unit
) {
    var open by remember { mutableStateOf(false) }
    ScoreRow(abilityLabel(strings, ability), value, onClick = { open = true }) {
        Box {
            IconButton(onClick = { open = true }) {
                Icon(if (open) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, contentDescription = null)
            }
            DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                free.distinct().forEach { option ->
                    DropdownMenuItem(
                        text = { Text("$option (${signed(abilityModifier(option))})") },
                        onClick = {
                            open = false
                            onPick(option)
                        }
                    )
                }
                if (value != null) {
                    DropdownMenuItem(
                        text = { Text(strings["levelup_abilities_clear"]) },
                        onClick = {
                            open = false
                            onPick(null)
                        }
                    )
                }
            }
        }
    }
}

/**
 * A starting equipment option: a radio and its title, its items as an inventory block, [choices]
 * (the "of your choice" fields) and its coins; the gold-only option has its coins on the title row.
 */
@Composable
private fun EquipmentOptionCard(
    title: String,
    selected: Boolean,
    items: List<InventoryItem>,
    dexterity: Int,
    coins: Map<String, Int>,
    coinsOnTitle: Boolean,
    strings: LocalizedStrings,
    onSelect: () -> Unit,
    choices: @Composable ColumnScope.() -> Unit
) {
    val colors = LocalDesignTokens.current.colors
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect),
        shape = RoundedCornerShape(12.dp),
        color = if (selected) colors.surface.selected else colors.surface.card,
        border = BorderStroke(1.dp, if (selected) colors.border.selected else colors.border.muted)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                RadioButton(selected = selected, onClick = null)
                Text(title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, color = colors.text.primary)
                if (coinsOnTitle) CoinsAmount(coins, strings)
            }
            if (items.isNotEmpty()) InventorySectionCard(items, dexterity)
            choices()
            if (!coinsOnTitle) CoinsAmount(coins, strings)
        }
    }
}

/** Coins as an amount and the inventory's coins: "50 ⛁". Gold has its icon; other coins their short name. */
@Composable
private fun CoinsAmount(coins: Map<String, Int>, strings: LocalizedStrings, modifier: Modifier = Modifier) {
    val shown = coins.filterValues { it > 0 }
    if (shown.isEmpty()) return
    val colors = LocalDesignTokens.current.colors
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        shown.forEach { (currency, count) ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(count.toString(), style = MaterialTheme.typography.titleMedium, color = colors.text.primary)
                if (currency == "gp") {
                    CurrencyCoinCluster(
                        modifier = Modifier.size(24.dp),
                        color = colors.accent.coinGold,
                        type = CurrencyType.GOLD
                    )
                } else {
                    Text(strings["inventory_currency_short_$currency"], style = MaterialTheme.typography.bodyLarge, color = colors.text.muted)
                }
            }
        }
    }
}

/** One "of your choice" item of a starting equipment option. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EquipmentChoiceField(
    label: String,
    choice: List<EquipmentPick>,
    selected: EquipmentPick?,
    russian: Boolean,
    onPick: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selected?.let { equipmentLabel(it, russian) }.orEmpty(),
            onValueChange = {},
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
            readOnly = true,
            singleLine = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            choice.forEach { option ->
                DropdownMenuItem(
                    text = { Text(equipmentLabel(option, russian)) },
                    onClick = {
                        onPick(option.id)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun AbilityRow(
    label: String,
    value: Int,
    added: Int,
    canAdd: Boolean,
    canRemove: Boolean,
    onAdd: () -> Unit,
    onRemove: () -> Unit
) {
    val colors = LocalDesignTokens.current.colors
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = if (added > 0) colors.surface.selected else colors.surface.card,
        border = BorderStroke(1.dp, if (added > 0) colors.border.selected else colors.border.muted)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, color = colors.text.primary)
            IconButton(onClick = onRemove, enabled = canRemove) { Icon(Icons.Outlined.Remove, contentDescription = null) }
            Text(
                text = value.toString() + if (added > 0) " (+$added)" else "",
                modifier = Modifier.width(72.dp),
                style = MaterialTheme.typography.titleMedium,
                color = if (added > 0) colors.accent.inspiration else colors.text.primary
            )
            IconButton(onClick = onAdd, enabled = canAdd) { Icon(Icons.Outlined.Add, contentDescription = null) }
        }
    }
}
