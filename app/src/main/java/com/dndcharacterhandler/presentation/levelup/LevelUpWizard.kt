package com.dndcharacterhandler.presentation.levelup

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Casino
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import com.dndcharacterhandler.domain.levelup.HitPointMethod
import com.dndcharacterhandler.domain.levelup.LevelUpAnswer
import com.dndcharacterhandler.domain.levelup.LevelUpDraft
import com.dndcharacterhandler.domain.levelup.LevelUpEngine
import com.dndcharacterhandler.domain.levelup.LevelUpPage
import com.dndcharacterhandler.domain.levelup.LevelUpSetup
import com.dndcharacterhandler.domain.levelup.LevelUpSummary
import com.dndcharacterhandler.domain.levelup.TraitOption
import com.dndcharacterhandler.domain.model.AdvancementStep
import com.dndcharacterhandler.domain.model.AppLanguage
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
import com.dndcharacterhandler.presentation.localization.LocalStrings
import com.dndcharacterhandler.presentation.localization.text
import com.dndcharacterhandler.presentation.theme.LocalDesignTokens
import kotlin.random.Random

/**
 * The level-up wizard: one page per step of the draft (class, hit points, features, choices,
 * ability scores, subclass...) and a summary; the character changes only when [onApply] is called.
 */
@Composable
internal fun LevelUpWizard(
    bundle: CharacterBundle,
    catalog: CharacterCatalog,
    targetLevel: Int,
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
    val answer = draft.answers[page.key]
    fun setAnswer(value: LevelUpAnswer) {
        draft = draft.copy(answers = draft.answers + (page.key to value))
    }

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
                    is LevelUpPage.HitPoints -> hitPointsPage(strings, page, answer as? LevelUpAnswer.HitPoints, russian, ::setAnswer)
                    is LevelUpPage.Features -> featuresPage(strings, page, answer as? LevelUpAnswer.Grants, russian, render, ::setAnswer)
                    is LevelUpPage.Traits -> traitsPage(strings, page, answer as? LevelUpAnswer.Traits, russian, ::setAnswer)
                    is LevelUpPage.Items -> itemsPage(strings, page, answer as? LevelUpAnswer.Items, russian, render, ::setAnswer)
                    is LevelUpPage.AbilityScores -> abilityScoresPage(strings, page, answer, russian, render, ::setAnswer)
                    is LevelUpPage.Subclass -> subclassPage(strings, page, answer as? LevelUpAnswer.Subclass, russian, render, ::setAnswer)
                    is LevelUpPage.Summary -> summaryPage(strings, page.summary, russian, catalog)
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
    }
}

private fun formulaContext(bundle: CharacterBundle, summary: LevelUpSummary, catalog: CharacterCatalog): FormulaContext {
    val level = summary.classes.sumOf { it.levels }.coerceAtLeast(1)
    val scores = bundle.character.abilityScores().mapValues { (ability, score) -> score + (summary.abilityIncreases[ability] ?: 0) }
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
    item(key = "keep") {
        val keep = setup?.keepExistingLevels ?: page.suggestKeep
        ChoiceCard(
            title = strings.format("levelup_setup_keep", page.currentLevel),
            subtitle = strings[if (keep) "levelup_setup_keep_hint" else "levelup_setup_rebuild_hint"],
            selected = keep,
            trailing = {
                Switch(checked = keep, onCheckedChange = { value ->
                    // No class yet: remember the switch with an empty class, which the wizard doesn't accept.
                    onChange((setup ?: LevelUpSetup(classId = "", keepExistingLevels = value)).copy(keepExistingLevels = value))
                })
            },
            onClick = null
        )
    }
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
    onAnswer: (LevelUpAnswer) -> Unit
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
                leading = { Icon(Icons.Outlined.Casino, contentDescription = null, tint = LocalDesignTokens.current.colors.accent.inspiration) },
                onClick = { onAnswer(LevelUpAnswer.HitPoints(HitPointMethod.ROLL, Random.nextInt(1, die + 1))) }
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

private fun LazyListScope.summaryPage(strings: LocalizedStrings, summary: LevelUpSummary, russian: Boolean, catalog: CharacterCatalog) {
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
    summaryList(strings, "granted_spells", "levelup_spells_granted", summary.spellsGranted.map { it.name.get(russian) })
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

/** A choice with a description that unfolds with the arrow. */
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
    val colors = LocalDesignTokens.current.colors
    var expanded by remember { mutableStateOf(false) }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null && enabled) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(12.dp),
        color = if (selected) colors.surface.selected else colors.surface.card,
        border = BorderStroke(1.dp, if (selected) colors.border.selected else colors.border.muted)
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                leading?.invoke()
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.bodyLarge, color = if (enabled) colors.text.primary else colors.text.subtle)
                    if (!subtitle.isNullOrBlank()) {
                        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = colors.text.muted, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
                if (body.isNotBlank()) {
                    IconButton(onClick = { expanded = !expanded }) {
                        Icon(
                            if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                            contentDescription = null,
                            tint = colors.text.muted
                        )
                    }
                }
            }
            if (expanded) {
                Text(
                    body,
                    modifier = Modifier.padding(top = 4.dp, bottom = 6.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.text.muted
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
