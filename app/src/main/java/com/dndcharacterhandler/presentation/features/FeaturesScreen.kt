package com.dndcharacterhandler.presentation.features

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.dndcharacterhandler.domain.model.AppLanguage
import com.dndcharacterhandler.domain.model.CharacterBundle
import com.dndcharacterhandler.domain.model.CharacterCatalog
import com.dndcharacterhandler.domain.model.CharacterTextField
import com.dndcharacterhandler.domain.model.Feature
import com.dndcharacterhandler.domain.model.FeatureCatalogGroup
import com.dndcharacterhandler.domain.model.FeatureCatalogItem
import com.dndcharacterhandler.domain.model.FeatureSource
import com.dndcharacterhandler.domain.repository.CharacterCatalogRepository
import com.dndcharacterhandler.domain.repository.CharacterRepository
import com.dndcharacterhandler.domain.repository.FeatureCatalogRepository
import com.dndcharacterhandler.domain.rules.CatalogFormulaText
import com.dndcharacterhandler.domain.rules.classWizardTarget
import com.dndcharacterhandler.domain.rules.FormulaContext
import com.dndcharacterhandler.domain.usecase.GetCharacterBundleUseCase
import com.dndcharacterhandler.presentation.BaseCharacterViewModel
import com.dndcharacterhandler.presentation.SelectedCharacterHolder
import com.dndcharacterhandler.presentation.components.CharacterScreenHeader
import com.dndcharacterhandler.presentation.components.ExpandableCard
import com.dndcharacterhandler.presentation.components.FloatingAddButton
import com.dndcharacterhandler.presentation.components.LocalFloatingButtonsInset
import com.dndcharacterhandler.presentation.components.ScreenBackground
import com.dndcharacterhandler.presentation.components.ScreenTopActions
import com.dndcharacterhandler.presentation.localization.LocalStrings
import com.dndcharacterhandler.presentation.localization.text
import com.dndcharacterhandler.presentation.theme.LocalDesignTokens
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class FeatureCatalogUiState(
    val items: List<FeatureCatalogItem> = emptyList(),
    val catalog: CharacterCatalog = CharacterCatalog.EMPTY,
    val isLoading: Boolean = true
)

class FeaturesViewModel(
    private val characterRepository: CharacterRepository,
    private val featureCatalogRepository: FeatureCatalogRepository,
    private val characterCatalogRepository: CharacterCatalogRepository,
    getCharacterBundleUseCase: GetCharacterBundleUseCase,
    selectedCharacterHolder: SelectedCharacterHolder
) : BaseCharacterViewModel(getCharacterBundleUseCase, selectedCharacterHolder) {
    private val _catalogUiState = MutableStateFlow(FeatureCatalogUiState())
    val catalogUiState: StateFlow<FeatureCatalogUiState> = _catalogUiState.asStateFlow()

    init {
        viewModelScope.launch {
            val catalog = characterCatalogRepository.getCatalog()
            val items = featureCatalogRepository.getItems()
            _catalogUiState.value = FeatureCatalogUiState(items = items, catalog = catalog, isLoading = false)
        }
    }

    fun updateRace(characterBundle: CharacterBundle, value: String) {
        val current = characterBundle.character
        val sanitized = value.trim()
        if (sanitized == current.race) return
        viewModelScope.launch {
            characterRepository.updateIdentity(
                characterId = current.id,
                name = current.name,
                race = sanitized,
                characterClass = current.characterClass,
                level = current.level
            )
        }
    }

    fun updateBackground(characterBundle: CharacterBundle, value: String) {
        val current = characterBundle.character
        val sanitized = value.trim()
        if (sanitized == current.background) return
        viewModelScope.launch {
            characterRepository.updateTextField(
                characterId = current.id,
                field = CharacterTextField.BACKGROUND,
                value = sanitized
            )
        }
    }

    fun updateFeature(characterBundle: CharacterBundle, feature: Feature) {
        viewModelScope.launch {
            characterRepository.upsertFeature(
                characterId = characterBundle.character.id,
                feature = if (feature.id == 0L) feature.copy(id = 0) else feature
            )
        }
    }

    fun deleteFeature(characterBundle: CharacterBundle, feature: Feature) {
        if (feature.id == 0L) return
        viewModelScope.launch {
            characterRepository.deleteFeature(
                characterId = characterBundle.character.id,
                featureId = feature.id
            )
        }
    }
}

@Composable
fun FeaturesScreen(
    viewModel: FeaturesViewModel,
    onOpenDrawer: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenLevelUp: (targetLevel: Int) -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val catalogState by viewModel.catalogUiState.collectAsStateWithLifecycle()
    FeaturesContent(
        characterBundle = state.character,
        catalogItems = catalogState.items,
        catalog = catalogState.catalog,
        isCatalogLoading = catalogState.isLoading,
        onOpenDrawer = onOpenDrawer,
        onOpenSettings = onOpenSettings,
        onUpdateFeature = viewModel::updateFeature,
        onDeleteFeature = viewModel::deleteFeature,
        onOpenLevelUp = onOpenLevelUp,
        onUpdateRace = viewModel::updateRace,
        onUpdateBackground = viewModel::updateBackground
    )
}

@Composable
internal fun FeaturesContent(
    characterBundle: CharacterBundle?,
    catalogItems: List<FeatureCatalogItem> = emptyList(),
    catalog: CharacterCatalog = CharacterCatalog.EMPTY,
    isCatalogLoading: Boolean = false,
    onOpenDrawer: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onUpdateFeature: (CharacterBundle, Feature) -> Unit = { _, _ -> },
    onDeleteFeature: (CharacterBundle, Feature) -> Unit = { _, _ -> },
    onOpenLevelUp: (Int) -> Unit = {},
    onUpdateRace: (CharacterBundle, String) -> Unit = { _, _ -> },
    onUpdateBackground: (CharacterBundle, String) -> Unit = { _, _ -> },
    /** Features shown unfolded at first (the screen preview uses it). */
    initiallyExpanded: Set<Long> = emptySet()
) {
    val character = characterBundle?.character
    val russian = LocalStrings.current.language == AppLanguage.RUSSIAN
    var query by remember { mutableStateOf("") }
    var editingFeature by remember { mutableStateOf<Feature?>(null) }
    // Unfolded cards, kept while scrolling; several can be open at once to compare them.
    var expandedFeatures by remember(characterBundle?.character?.id) { mutableStateOf(initiallyExpanded) }
    var isAddEntryDialogOpen by remember { mutableStateOf(false) }
    var editingSummaryField by remember { mutableStateOf<FeatureSummaryField?>(null) }
    var summaryDraft by remember { mutableStateOf("") }

    if (character == null) {
        ScreenBackground {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 24.dp, end = 24.dp, top = 4.dp)
            ) {
                ScreenTopActions(
                    onOpenDrawer = onOpenDrawer,
                    onOpenSettings = onOpenSettings,
                    modifier = Modifier.align(Alignment.TopCenter)
                )
                Text(
                    text = text("placeholder_loading_character"),
                    modifier = Modifier.align(Alignment.Center),
                    style = MaterialTheme.typography.bodyLarge,
                    color = LocalDesignTokens.current.colors.text.muted
                )
            }
        }
        return
    }

    val resolvedBundle = characterBundle
    val catalogLookup = remember(catalogItems) { FeatureCatalogLookup(catalogItems) }
    val formulas = remember(catalog) { CatalogFormulaText(catalog) }
    val formulaContext = remember(character, catalog) { FormulaContext.of(character, catalog) }
    val renderText: (String) -> String = { value -> formulas.render(value, russian, formulaContext) }
    val displayedFeatures = remember(resolvedBundle.features, catalogLookup, russian) {
        resolvedBundle.features.localizedWith(catalogLookup, russian)
    }
    val visibleFeatures = remember(displayedFeatures, query) {
        val needle = query.trim()
        displayedFeatures.filter { feature ->
            needle.isBlank() ||
                feature.name.contains(needle, ignoreCase = true) ||
                feature.description.contains(needle, ignoreCase = true)
        }
    }
    val groupedFeatures = remember(visibleFeatures) {
        featureSourceOrder.mapNotNull { source ->
            visibleFeatures
                .filter { it.source == source }
                .takeIf { it.isNotEmpty() }
                ?.let { source to it }
        }
    }

    ScreenBackground {
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 4.dp, bottom = LocalFloatingButtonsInset.current),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    CharacterScreenHeader(
                        character = character,
                        onOpenDrawer = onOpenDrawer,
                        onOpenSettings = onOpenSettings
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        FeatureSummaryCard(
                            modifier = Modifier.weight(1f),
                            label = text("placeholder_class"),
                            value = character.characterClass,
                            icon = Icons.Outlined.Shield,
                            // The class is chosen in the level-up wizard, not typed in.
                            onClick = { classWizardTarget(character)?.let(onOpenLevelUp) }
                        )
                        FeatureSummaryCard(
                            modifier = Modifier.weight(1f),
                            label = text("placeholder_race"),
                            value = character.race,
                            icon = Icons.Outlined.Person,
                            onClick = {
                                summaryDraft = character.race
                                editingSummaryField = FeatureSummaryField.RACE
                            }
                        )
                        FeatureSummaryCard(
                            modifier = Modifier.weight(1f),
                            label = text("biography_background"),
                            value = character.background,
                            icon = Icons.Outlined.AutoStories,
                            onClick = {
                                summaryDraft = character.background
                                editingSummaryField = FeatureSummaryField.BACKGROUND
                            }
                        )
                    }
                }

                item {
                    FeaturesSearchField(
                        value = query,
                        onValueChange = { query = it }
                    )
                }

                groupedFeatures.forEach { (source, features) ->
                    item(key = "section_${source.name}") {
                        FeaturesSectionTitle(featureSourceLabel(source))
                    }

                    items(features, key = { it.id }) { feature ->
                        FeatureCard(
                            feature = feature,
                            expanded = feature.id in expandedFeatures,
                            onExpandedChange = { open ->
                                expandedFeatures = if (open) expandedFeatures + feature.id else expandedFeatures - feature.id
                            },
                            onEdit = { editingFeature = feature },
                            renderText = renderText
                        )
                    }
                }
            }

            FloatingAddButton(
                onClick = { isAddEntryDialogOpen = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 24.dp, bottom = 15.dp)
            )
        }
    }

    if (isAddEntryDialogOpen) {
        FeaturesAddEntryDialog(
            catalogItems = catalogItems,
            isLoading = isCatalogLoading,
            russian = russian,
            onDismiss = { isAddEntryDialogOpen = false },
            onCreateFeature = {
                isAddEntryDialogOpen = false
                editingFeature = newDraftFeature()
            },
            onSelectCatalogItem = { item ->
                onUpdateFeature(resolvedBundle, item.toFeature(russian))
                isAddEntryDialogOpen = false
            }
        )
    }

    editingFeature?.let { feature ->
        FeatureEditDialog(
            feature = feature,
            renderText = renderText,
            onDismiss = { editingFeature = null },
            onSave = { updated ->
                onUpdateFeature(resolvedBundle, updated)
                editingFeature = null
            },
            onDelete = if (feature.id != 0L) {
                {
                    onDeleteFeature(resolvedBundle, feature)
                    editingFeature = null
                }
            } else {
                null
            }
        )
    }

    editingSummaryField?.let { field ->
        val titleKey = when (field) {
            FeatureSummaryField.RACE -> "overview_edit_race_title"
            FeatureSummaryField.BACKGROUND -> "biography_background"
        }
        val labelKey = when (field) {
            FeatureSummaryField.RACE -> "placeholder_race"
            FeatureSummaryField.BACKGROUND -> "biography_background"
        }
        AlertDialog(
            onDismissRequest = { editingSummaryField = null },
            title = { Text(text(titleKey)) },
            text = {
                OutlinedTextField(
                    value = summaryDraft,
                    onValueChange = { summaryDraft = it },
                    label = { Text(text(labelKey)) },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        when (field) {
                            FeatureSummaryField.RACE -> onUpdateRace(resolvedBundle, summaryDraft)
                            FeatureSummaryField.BACKGROUND -> onUpdateBackground(resolvedBundle, summaryDraft)
                        }
                        editingSummaryField = null
                    }
                ) {
                    Text(text("common_save"))
                }
            },
            dismissButton = {
                TextButton(onClick = { editingSummaryField = null }) {
                    Text(text("common_cancel"))
                }
            }
        )
    }
}

private enum class FeatureSummaryField { RACE, BACKGROUND }

@Composable
private fun FeatureSummaryCard(
    label: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val tokens = LocalDesignTokens.current.typography
    Surface(
        modifier = modifier
            .height(92.dp)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, LocalDesignTokens.current.colors.border.miniCard),
        color = LocalDesignTokens.current.colors.surface.card.copy(alpha = 0.62f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = LocalDesignTokens.current.colors.text.label,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = tokens.miniStatLabel.fontSizeSp.sp),
                    color = LocalDesignTokens.current.colors.text.miniLabel,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = value.ifBlank { "—" },
                style = MaterialTheme.typography.titleMedium,
                color = LocalDesignTokens.current.colors.text.primary,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun FeaturesSearchField(
    value: String,
    onValueChange: (String) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = LocalDesignTokens.current.colors.surface.card.copy(alpha = 0.62f),
        border = BorderStroke(1.dp, LocalDesignTokens.current.colors.border.muted)
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = null,
                    tint = LocalDesignTokens.current.colors.text.muted,
                    modifier = Modifier.size(28.dp)
                )
            },
            placeholder = {
                Text(
                    text = text("features_search_placeholder"),
                    style = MaterialTheme.typography.bodyLarge,
                    color = LocalDesignTokens.current.colors.text.muted.copy(alpha = 0.72f)
                )
            },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                cursorColor = LocalDesignTokens.current.colors.text.warmPrimary
            )
        )
    }
}

@Composable
private fun FeaturesSectionTitle(title: String) {
    val tokens = LocalDesignTokens.current.typography
    val dividerColor = LocalDesignTokens.current.colors.border.muted
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium.copy(fontSize = tokens.headlineMedium.fontSizeSp.sp),
            color = LocalDesignTokens.current.colors.text.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Box(
            modifier = Modifier
                .padding(start = 12.dp)
                .weight(1f)
                .height(1.dp)
        ) {
            androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                drawLine(
                    color = dividerColor,
                    start = androidx.compose.ui.geometry.Offset(0f, size.height / 2f),
                    end = androidx.compose.ui.geometry.Offset(size.width, size.height / 2f),
                    strokeWidth = 1.dp.toPx()
                )
            }
        }
    }
}

/**
 * A feature of the character as an unfolding card: its name with category and level; unfolded, the
 * description with the character's numbers ([renderText]) and an Edit button. A long press edits
 * right away; deleting stays inside the editor.
 */
@Composable
internal fun FeatureCard(
    feature: Feature,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onEdit: () -> Unit,
    renderText: (String) -> String = { it }
) {
    val colors = LocalDesignTokens.current.colors
    val subtitle = listOfNotNull(
        feature.category.takeIf { it.isNotBlank() },
        feature.level?.let { "${text("features_level")} $it" }
    ).joinToString(" • ")
    ExpandableCard(
        title = feature.name.ifBlank { text("features_untitled") },
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        subtitle = subtitle,
        body = renderText(feature.description).ifBlank { text("features_no_description") },
        leading = {
            Icon(
                imageVector = featureSourceIcon(feature.source),
                contentDescription = null,
                tint = colors.text.muted,
                modifier = Modifier.size(22.dp)
            )
        },
        onLongClick = onEdit,
        actions = {
            TextButton(onClick = onEdit) {
                Icon(Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text("features_edit_action"))
            }
        }
    )
}

private fun featureSourceIcon(source: FeatureSource): ImageVector = when (source) {
    FeatureSource.CLASS -> Icons.Outlined.Shield
    FeatureSource.RACE -> Icons.Outlined.Person
    FeatureSource.BACKGROUND -> Icons.Outlined.AutoStories
    FeatureSource.OTHER -> Icons.Outlined.AutoAwesome
}

@Composable
private fun FeaturesAddEntryDialog(
    catalogItems: List<FeatureCatalogItem>,
    isLoading: Boolean,
    russian: Boolean,
    onDismiss: () -> Unit,
    onCreateFeature: () -> Unit,
    onSelectCatalogItem: (FeatureCatalogItem) -> Unit
) {
    val colors = LocalDesignTokens.current.colors
    var query by remember { mutableStateOf("") }
    var group by remember { mutableStateOf<FeatureCatalogGroup?>(null) }
    val groups = remember(catalogItems) {
        catalogGroupOrder.filter { candidate -> catalogItems.any { it.group == candidate } }
    }
    val filteredItems = remember(catalogItems, query, group) {
        val needle = query.trim()
        catalogItems.filter { item ->
            (group == null || item.group == group) && (
                needle.isBlank() ||
                    item.name.contains(needle, ignoreCase = true) ||
                    item.ruName.contains(needle, ignoreCase = true) ||
                    item.category.contains(needle, ignoreCase = true) ||
                    item.ruCategory.contains(needle, ignoreCase = true) ||
                    item.description.contains(needle, ignoreCase = true) ||
                    item.ruDescription.contains(needle, ignoreCase = true)
                )
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text("features_add_feature")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FeaturesDialogSection(text("features_add_create_section"))
                    TextButton(onClick = onCreateFeature) {
                        Text(text("features_create_action"))
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FeaturesDialogSection(text("features_add_catalog_section"))
                    FeaturesSearchField(
                        value = query,
                        onValueChange = { query = it }
                    )
                    if (groups.size > 1) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            item {
                                FilterChip(
                                    selected = group == null,
                                    onClick = { group = null },
                                    label = { Text(text("features_filter_all")) }
                                )
                            }
                            items(groups) { option ->
                                FilterChip(
                                    selected = group == option,
                                    onClick = { group = if (group == option) null else option },
                                    label = { Text(catalogGroupLabel(option)) }
                                )
                            }
                        }
                    }
                    when {
                        isLoading -> {
                            Text(
                                text = text("features_catalog_loading"),
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.text.muted
                            )
                        }
                        filteredItems.isEmpty() -> {
                            Text(
                                text = text("features_catalog_empty"),
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.text.muted
                            )
                        }
                        else -> {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 280.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(filteredItems, key = { it.id }) { item ->
                                    FeatureCatalogRow(
                                        item = item,
                                        russian = russian,
                                        onAdd = { onSelectCatalogItem(item) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text("common_cancel"))
            }
        }
    )
}

@Composable
private fun FeaturesDialogSection(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = LocalDesignTokens.current.colors.text.primary
    )
}

@Composable
internal fun FeatureCatalogRow(
    item: FeatureCatalogItem,
    russian: Boolean,
    onAdd: () -> Unit
) {
    val colors = LocalDesignTokens.current.colors
    val subtitle = buildList {
        item.displayCategory(russian).takeIf { it.isNotBlank() }?.let(::add)
        item.level?.let { add("${text("features_level")} $it") }
    }.joinToString(" • ")
    val book = item.book.get(russian)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = LocalDesignTokens.current.colors.surface.button,
        border = BorderStroke(1.dp, LocalDesignTokens.current.colors.border.muted)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.displayName(russian),
                    style = MaterialTheme.typography.bodyLarge,
                    color = LocalDesignTokens.current.colors.text.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (subtitle.isNotBlank()) {
                    Text(
                        text = subtitle,
                        modifier = Modifier.padding(top = 4.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.text.muted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (book.isNotBlank()) {
                    Text(
                        text = book,
                        modifier = Modifier.padding(top = 2.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.text.subtle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            TextButton(onClick = onAdd) {
                Text(text("common_add"))
            }
        }
    }
}

@Composable
internal fun FeatureEditDialog(
    feature: Feature,
    onDismiss: () -> Unit,
    onSave: (Feature) -> Unit,
    onDelete: (() -> Unit)? = null,
    renderText: (String) -> String = { it }
) {
    val colors = LocalDesignTokens.current.colors
    // Catalog texts keep their formulas ({=...}, see CatalogFormulaText); the dialog shows this
    // character's numbers, and an untouched text is saved with its formulas so it keeps following them.
    val shownDescription = remember(feature) { renderText(feature.description) }
    var name by remember(feature) { mutableStateOf(feature.name) }
    var description by remember(feature) { mutableStateOf(shownDescription) }
    var level by remember(feature) { mutableStateOf(feature.level?.toString().orEmpty()) }
    var source by remember(feature) { mutableStateOf(feature.source) }
    var category by remember(feature) { mutableStateOf(feature.category) }

    AlertDialog(
        onDismissRequest = onDismiss,
        // Closing without saving is the cross in the corner, not a Cancel button.
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    // Short, so it stays on one line next to the cross.
                    text = text(if (feature.id == 0L) "features_editor_add" else "features_editor_edit"),
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = text("common_close"),
                        tint = colors.text.muted
                    )
                }
            }
        },
        text = {
            // Scrolls as a whole, so a long description never pushes the buttons away.
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(text("features_name")) },
                    singleLine = true
                )
                FeatureSourceField(
                    value = source,
                    onValueChange = { source = it }
                )
                // The level is at most two digits: a narrow field next to the category.
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        modifier = Modifier.weight(1f),
                        label = { Text(text("features_category")) },
                        singleLine = true
                    )
                    LevelField(value = level, onValueChange = { value -> level = value.filter(Char::isDigit).take(2) })
                }
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(text("features_description")) },
                    minLines = 4,
                    maxLines = 10
                )
            }
        },
        confirmButton = {
            // Delete in the bottom-left corner, apart from Save on the right.
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                if (onDelete != null) {
                    // A round tonal button as tall as Save: the danger red, faint behind the icon.
                    FilledIconButton(
                        onClick = onDelete,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = colors.accent.dangerHpZero.copy(alpha = 0.16f),
                            contentColor = colors.accent.dangerHpZero
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = text("inventory_delete_action")
                        )
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
                Button(
                    onClick = {
                        onSave(
                            feature.copy(
                                name = name.trim(),
                                description = if (description == shownDescription) feature.description else description.trim(),
                                level = level.toIntOrNull(),
                                source = source,
                                category = category.trim()
                            )
                        )
                    }
                ) {
                    Text(text("common_save"))
                }
            }
        }
    )
}

/**
 * The level: a narrow outlined field (two digits) with its label centred on the outline. Material
 * puts a field's label at the start, so this one draws its own over the border, on the dialog's
 * colour, lined up with the floating labels of the fields beside it.
 */
@Composable
private fun LevelField(value: String, onValueChange: (String) -> Unit) {
    Box(modifier = Modifier.width(72.dp)) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            // A label floats half above the outline: the same room up top keeps the borders level.
            modifier = Modifier
                .padding(top = 8.dp)
                .fillMaxWidth(),
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        Text(
            text = text("features_level"),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .background(AlertDialogDefaults.containerColor)
                .padding(horizontal = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** The source as a dropdown that looks like the other fields of the dialog. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FeatureSourceField(
    value: FeatureSource,
    onValueChange: (FeatureSource) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = featureSourceLabel(value),
            onValueChange = {},
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
            readOnly = true,
            singleLine = true,
            label = { Text(text("features_source")) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            featureSourceOrder.forEach { option ->
                DropdownMenuItem(
                    text = { Text(featureSourceLabel(option)) },
                    onClick = {
                        onValueChange(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun featureSourceLabel(source: FeatureSource): String =
    when (source) {
        FeatureSource.RACE -> text("features_source_race")
        FeatureSource.BACKGROUND -> text("features_source_background")
        FeatureSource.CLASS -> text("features_source_class")
        FeatureSource.OTHER -> text("features_source_other")
    }

@Composable
private fun catalogGroupLabel(group: FeatureCatalogGroup): String =
    when (group) {
        FeatureCatalogGroup.CLASS -> text("features_filter_class")
        FeatureCatalogGroup.SUBCLASS -> text("features_filter_subclass")
        FeatureCatalogGroup.OPTION -> text("features_filter_option")
        FeatureCatalogGroup.FEAT -> text("features_filter_feat")
        FeatureCatalogGroup.SPECIES -> text("features_filter_species")
        FeatureCatalogGroup.BACKGROUND -> text("features_filter_background")
        FeatureCatalogGroup.OTHER -> text("features_filter_other")
    }

private val catalogGroupOrder = listOf(
    FeatureCatalogGroup.CLASS,
    FeatureCatalogGroup.SUBCLASS,
    FeatureCatalogGroup.OPTION,
    FeatureCatalogGroup.FEAT,
    FeatureCatalogGroup.SPECIES,
    FeatureCatalogGroup.BACKGROUND,
    FeatureCatalogGroup.OTHER
)

private fun newDraftFeature(): Feature =
    Feature(
        id = 0,
        name = "",
        description = "",
        level = null,
        source = FeatureSource.OTHER
    )

private val featureSourceOrder = listOf(
    FeatureSource.RACE,
    FeatureSource.BACKGROUND,
    FeatureSource.CLASS,
    FeatureSource.OTHER
)
