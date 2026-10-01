package com.dndcharacterhandler.presentation.inventory

import com.dndcharacterhandler.presentation.components.StatCardRow
import com.dndcharacterhandler.presentation.components.MiniStatCard
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.compose.foundation.lazy.rememberLazyListState
import com.dndcharacterhandler.data.localization.LocalizedStrings
import com.dndcharacterhandler.domain.model.CharacterBundle
import com.dndcharacterhandler.domain.model.InventoryArmorDetails
import com.dndcharacterhandler.domain.model.InventoryArmorType
import com.dndcharacterhandler.domain.model.AppLanguage
import com.dndcharacterhandler.domain.model.InventoryCatalogBonusVariant
import com.dndcharacterhandler.domain.model.InventoryCatalogItem
import com.dndcharacterhandler.domain.model.InventoryCatalogKind
import com.dndcharacterhandler.domain.model.InventoryCategory
import com.dndcharacterhandler.domain.model.InventoryItem
import com.dndcharacterhandler.domain.model.InventoryWeaponDamage
import com.dndcharacterhandler.domain.model.InventoryWeaponDetails
import com.dndcharacterhandler.domain.model.InventoryWeaponProperty
import com.dndcharacterhandler.domain.model.InventoryWeaponRangeType
import com.dndcharacterhandler.domain.model.InventoryWeaponClass
import com.dndcharacterhandler.domain.rules.abilityModifier
import com.dndcharacterhandler.domain.rules.appliedDexterityModifier
import com.dndcharacterhandler.domain.rules.armorMagicBonus
import com.dndcharacterhandler.domain.repository.CharacterRepository
import com.dndcharacterhandler.domain.repository.InventoryCatalogRepository
import com.dndcharacterhandler.domain.usecase.GetCharacterBundleUseCase
import com.dndcharacterhandler.presentation.BaseCharacterViewModel
import com.dndcharacterhandler.presentation.SelectedCharacterHolder
import com.dndcharacterhandler.presentation.components.CharacterScreenHeader
import com.dndcharacterhandler.presentation.components.CardEditButton
import com.dndcharacterhandler.presentation.components.FloatingAddButton
import com.dndcharacterhandler.presentation.components.LimitProgressBar
import com.dndcharacterhandler.presentation.components.LocalFloatingButtonsInset
import com.dndcharacterhandler.presentation.components.ScreenBackground
import com.dndcharacterhandler.presentation.components.SelectableDot
import com.dndcharacterhandler.presentation.components.ScreenTopActions
import com.dndcharacterhandler.presentation.localization.LocalStrings
import com.dndcharacterhandler.presentation.localization.text
import com.dndcharacterhandler.presentation.theme.LocalDesignTokens
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

internal enum class CurrencyType { COPPER, SILVER, GOLD }

private data class WeaponKindOption(
    val weaponClass: InventoryWeaponClass,
    val rangeType: InventoryWeaponRangeType
)

private data class BaseWeaponOption(
    val id: String,
    val labelKey: String
)

private data class WeaponDamageEditorState(
    val diceCount: String,
    val dieType: String,
    val damageType: String
)

private val CompactEditorFieldHeight = 46.dp
class InventoryViewModel(
    private val characterRepository: CharacterRepository,
    private val inventoryCatalogRepository: InventoryCatalogRepository,
    getCharacterBundleUseCase: GetCharacterBundleUseCase,
    selectedCharacterHolder: SelectedCharacterHolder
) : BaseCharacterViewModel(getCharacterBundleUseCase, selectedCharacterHolder) {
    private val _catalogUiState = MutableStateFlow(InventoryCatalogUiState())
    val catalogUiState: StateFlow<InventoryCatalogUiState> = _catalogUiState.asStateFlow()

    init {
        viewModelScope.launch {
            val items = inventoryCatalogRepository.getItems()
            _catalogUiState.value = InventoryCatalogUiState(items = items, isLoading = false)
        }
    }

    /** Adds [item] from the catalog; a magic item or enchantment with a [base] takes the base's stats. */
    fun addCatalogItem(
        characterBundle: CharacterBundle,
        item: InventoryCatalogItem,
        base: InventoryCatalogItem?,
        variant: InventoryCatalogBonusVariant?,
        russian: Boolean
    ) {
        viewModelScope.launch {
            characterRepository.upsertInventoryItem(
                characterId = characterBundle.character.id,
                item = if (base != null) item.appliedTo(base, variant, russian) else item.toInventoryItem(russian)
            )
        }
    }

    fun addInventoryItem(characterBundle: CharacterBundle, item: InventoryItem) {
        viewModelScope.launch {
            characterRepository.upsertInventoryItem(
                characterId = characterBundle.character.id,
                item = item
            )
        }
    }

    fun toggleItemEquipped(characterBundle: CharacterBundle, targetItem: InventoryItem) {
        if (targetItem.id == 0L) return
        viewModelScope.launch {
            characterRepository.toggleInventoryItemEquipped(
                characterId = characterBundle.character.id,
                itemId = targetItem.id
            )
        }
    }

    fun updateInventoryItem(
        characterBundle: CharacterBundle,
        originalItem: InventoryItem,
        updatedItem: InventoryItem
    ) {
        val itemToSave = if (updatedItem.id != 0L || originalItem.id == 0L) {
            updatedItem
        } else {
            updatedItem.copy(id = originalItem.id)
        }
        viewModelScope.launch {
            characterRepository.upsertInventoryItem(
                characterId = characterBundle.character.id,
                item = itemToSave
            )
        }
    }

    fun deleteInventoryItem(characterBundle: CharacterBundle, item: InventoryItem) {
        if (item.id == 0L) return
        viewModelScope.launch {
            characterRepository.deleteInventoryItem(
                characterId = characterBundle.character.id,
                itemId = item.id
            )
        }
    }

    fun updateCurrency(
        characterBundle: CharacterBundle,
        copperPieces: Int,
        silverPieces: Int,
        goldPieces: Int
    ) {
        viewModelScope.launch {
            characterRepository.updateCurrency(
                characterId = characterBundle.character.id,
                copperPieces = copperPieces,
                silverPieces = silverPieces,
                goldPieces = goldPieces
            )
        }
    }
}

data class InventoryCatalogUiState(
    val items: List<InventoryCatalogItem> = emptyList(),
    val isLoading: Boolean = true
)

@Composable
fun InventoryScreen(
    viewModel: InventoryViewModel,
    onOpenDrawer: () -> Unit,
    onOpenDice: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val catalogState by viewModel.catalogUiState.collectAsStateWithLifecycle()
    val strings = LocalStrings.current
    var isAddItemDialogOpen by remember { mutableStateOf(false) }
    var isCategoryPickerOpen by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<InventoryItem?>(null) }
    var creatingItem by remember { mutableStateOf<InventoryItem?>(null) }
    var isCurrencyDialogOpen by remember { mutableStateOf(false) }

    InventoryContent(
        characterBundle = state.character,
        catalogItems = catalogState.items,
        onOpenDrawer = onOpenDrawer,
        onOpenDice = onOpenDice,
        onAddItem = { isAddItemDialogOpen = true },
        onEditCurrency = { isCurrencyDialogOpen = true },
        onToggleEquipped = { characterBundle, item ->
            viewModel.toggleItemEquipped(characterBundle, item)
        },
        onEditItem = { editingItem = it }
    )

    val characterBundle = state.character
    if (isAddItemDialogOpen && characterBundle != null) {
        InventoryAddEntryDialog(
            catalogItems = catalogState.items,
            isLoading = catalogState.isLoading,
            onDismiss = { isAddItemDialogOpen = false },
            onCreateItem = {
                isAddItemDialogOpen = false
                isCategoryPickerOpen = true
            },
            onSelectCatalogItem = { item, base, variant ->
                viewModel.addCatalogItem(
                    characterBundle = characterBundle,
                    item = item,
                    base = base,
                    variant = variant,
                    russian = strings.language == AppLanguage.RUSSIAN
                )
                isAddItemDialogOpen = false
            }
        )
    }

    if (isCategoryPickerOpen) {
        InventoryCategoryPickerDialog(
            onDismiss = { isCategoryPickerOpen = false },
            onSelectCategory = { category ->
                creatingItem = defaultInventoryItem(category, strings[category.titleKey()])
                isCategoryPickerOpen = false
            }
        )
    }

    if (editingItem != null && characterBundle != null) {
        InventoryItemEditDialog(
            inventoryItem = editingItem!!,
            title = text("inventory_edit_item"),
            confirmLabel = text("common_save"),
            onDismiss = { editingItem = null },
            onSave = { updatedItem ->
                viewModel.updateInventoryItem(characterBundle, editingItem!!, updatedItem)
                editingItem = null
            },
            onDelete = {
                viewModel.deleteInventoryItem(characterBundle, editingItem!!)
                editingItem = null
            }
        )
    }

    if (creatingItem != null && characterBundle != null) {
        InventoryItemEditDialog(
            inventoryItem = creatingItem!!,
            title = text("inventory_add_item"),
            confirmLabel = text("inventory_create_action"),
            onDismiss = { creatingItem = null },
            onSave = { newItem ->
                viewModel.addInventoryItem(characterBundle, newItem)
                creatingItem = null
            }
        )
    }

    if (isCurrencyDialogOpen && characterBundle != null) {
        InventoryCurrencyDialog(
            copperPieces = characterBundle.character.copperPieces,
            silverPieces = characterBundle.character.silverPieces,
            goldPieces = characterBundle.character.goldPieces,
            onDismiss = { isCurrencyDialogOpen = false },
            onSave = { copperPieces, silverPieces, goldPieces ->
                viewModel.updateCurrency(characterBundle, copperPieces, silverPieces, goldPieces)
                isCurrencyDialogOpen = false
            }
        )
    }
}

@Composable
internal fun InventoryContent(
    characterBundle: CharacterBundle?,
    catalogItems: List<InventoryCatalogItem> = emptyList(),
    onOpenDrawer: () -> Unit = {},
    onOpenDice: () -> Unit = {},
    onAddItem: () -> Unit = {},
    onEditCurrency: () -> Unit = {},
    onToggleEquipped: (CharacterBundle, InventoryItem) -> Unit = { _, _ -> },
    onEditItem: (InventoryItem) -> Unit = {},
    /** Items shown unfolded at first (the screen preview uses it). */
    initiallyExpanded: Set<Long> = emptySet()
) {
    val character = characterBundle?.character
    var query by remember { mutableStateOf("") }
    // Unfolded rows, kept while scrolling; several can be open at once.
    var expandedItems by remember(characterBundle?.character?.id) { mutableStateOf(initiallyExpanded) }

    if (character == null) {
        ScreenBackground {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 24.dp, end = 24.dp, top = 4.dp)
            ) {
                ScreenTopActions(
                    onOpenDrawer = onOpenDrawer,
                    onOpenDice = onOpenDice,
                    modifier = Modifier.align(Alignment.TopCenter)
                )
                Text(
                    text = text("placeholder_loading_character"),
                    modifier = Modifier.align(Alignment.Center),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFFD7D1CC)
                )
            }
        }
        return
    }

    val russian = LocalStrings.current.language == AppLanguage.RUSSIAN
    val catalogLookup = remember(catalogItems) { InventoryCatalogLookup(catalogItems) }
    val displayedItems = remember(characterBundle.inventoryItems, catalogLookup, russian) {
        characterBundle.inventoryItems.localizedWith(catalogLookup, russian)
    }
    val items = remember(displayedItems, query) {
        val needle = query.trim()
        displayedItems.filter { item ->
            needle.isBlank() ||
                item.name.contains(needle, ignoreCase = true) ||
                item.category.name.contains(needle, ignoreCase = true)
        }
    }
    val totalWeight = remember(characterBundle.inventoryItems) {
        characterBundle.inventoryItems.sumOf { it.weight * it.quantity }
    }
    val carryLimit = (character.strength.coerceAtLeast(1) * 15).toDouble()
    val listState = rememberLazyListState()

    ScreenBackground {
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 4.dp, bottom = LocalFloatingButtonsInset.current),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item(key = "header") {
                    CharacterScreenHeader(
                        character = character,
                        onOpenDrawer = onOpenDrawer,
                        onOpenDice = onOpenDice
                    )
                }

                item(key = "currency_row") {
                    CurrencyCardRow(
                        copperPieces = character.copperPieces,
                        silverPieces = character.silverPieces,
                        goldPieces = character.goldPieces,
                        onClick = onEditCurrency
                    )
                }

                item(key = "carry_weight") {
                    CarryWeightBlock(
                        current = totalWeight,
                        maximum = carryLimit
                    )
                }

                item(key = "search") {
                    InventorySearchField(
                        value = query,
                        onValueChange = { query = it }
                    )
                }

                InventoryCategory.values().forEach { category ->
                    val categoryItems = items.filter { it.category == category }
                    if (categoryItems.isNotEmpty()) {
                        item(key = "section_title_${category.name}") {
                            InventorySectionTitle(title = category.title())
                        }
                        item(key = "section_card_${category.name}") {
                            InventorySectionCard(
                                items = categoryItems,
                                dexterityScore = character.dexterity,
                                onToggleEquipped = { item -> onToggleEquipped(characterBundle, item) },
                                onEditItem = onEditItem,
                                expanded = expandedItems,
                                onExpandedChange = { item, open ->
                                    expandedItems = if (open) expandedItems + item.id else expandedItems - item.id
                                }
                            )
                        }
                    }
                }

                if (items.isEmpty()) {
                    item(key = "empty_inventory") {
                        EmptyInventoryMessage()
                    }
                }
            }

            FloatingAddButton(
                onClick = onAddItem,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 24.dp, bottom = 15.dp)
            )
        }
    }
}

/** Gold, silver and copper (the most valuable first) as stat cards, each with its coins; a tap edits them. */
@Composable
private fun CurrencyCardRow(
    copperPieces: Int,
    silverPieces: Int,
    goldPieces: Int,
    onClick: () -> Unit
) {
    val accent = LocalDesignTokens.current.colors.accent
    StatCardRow {
        listOf(
            Triple("stat_card_gold", goldPieces, CurrencyType.GOLD to accent.coinGold),
            Triple("stat_card_silver", silverPieces, CurrencyType.SILVER to accent.coinSilver),
            Triple("stat_card_copper", copperPieces, CurrencyType.COPPER to accent.coinCopper)
        ).forEach { (label, amount, coin) ->
            MiniStatCard(
                modifier = Modifier.weight(1f),
                label = text(label),
                value = amount.toString(),
                icon = { CurrencyCoinCluster(modifier = Modifier.size(24.dp), color = coin.second, type = coin.first) },
                onClick = onClick
            )
        }
    }
}

/** The coins drawn on the currency cards; Character Wizard shows starting gold with them too. */
@Composable
internal fun CurrencyCoinCluster(
    modifier: Modifier = Modifier,
    color: Color,
    type: CurrencyType
) {
    Canvas(modifier = modifier) {
        val stroke = 1.2.dp.toPx()
        when (type) {
            CurrencyType.COPPER -> {
                val radius = size.minDimension * 0.19f
                val positions = listOf(
                    Offset(size.width * 0.34f, size.height * 0.34f),
                    Offset(size.width * 0.68f, size.height * 0.64f),
                    Offset(size.width * 0.22f, size.height * 0.72f)
                )
                positions.forEach { center ->
                    drawCoin(center, radius, color, stroke)
                }
            }

            CurrencyType.SILVER -> {
                val backRadius = size.minDimension * 0.16f
                val frontRadius = size.minDimension * 0.2f
                drawCoin(Offset(size.width * 0.28f, size.height * 0.68f), backRadius, color, stroke)
                drawCoin(Offset(size.width * 0.6f, size.height * 0.4f), frontRadius, color, stroke)
                drawCoin(Offset(size.width * 0.66f, size.height * 0.68f), frontRadius, color, stroke)
            }

            CurrencyType.GOLD -> {
                val radius = size.minDimension * 0.2f
                drawCoin(Offset(size.width * 0.38f, size.height * 0.7f), radius, color, stroke)
                drawCoin(Offset(size.width * 0.64f, size.height * 0.42f), radius, color, stroke)
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCoin(
    center: Offset,
    radius: Float,
    color: Color,
    strokeWidth: Float
) {
    drawCircle(
        color = color.copy(alpha = 0.18f),
        radius = radius,
        center = center
    )
    drawCircle(
        color = color,
        radius = radius,
        center = center,
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth)
    )
    drawCircle(
        color = color.copy(alpha = 0.8f),
        radius = radius * 0.42f,
        center = center,
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth * 0.85f)
    )
}

@Composable
private fun InventoryCurrencyDialog(
    copperPieces: Int,
    silverPieces: Int,
    goldPieces: Int,
    onDismiss: () -> Unit,
    onSave: (Int, Int, Int) -> Unit
) {
    var copperDraft by remember(copperPieces) { mutableStateOf(copperPieces.toString()) }
    var silverDraft by remember(silverPieces) { mutableStateOf(silverPieces.toString()) }
    var goldDraft by remember(goldPieces) { mutableStateOf(goldPieces.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text("inventory_currency_edit_title")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // In the cards' order: gold first.
                OutlinedTextField(
                    value = goldDraft,
                    onValueChange = { goldDraft = it.filter(Char::isDigit) },
                    label = { Text(text("inventory_currency_gp")) },
                    singleLine = true
                )
                OutlinedTextField(
                    value = silverDraft,
                    onValueChange = { silverDraft = it.filter(Char::isDigit) },
                    label = { Text(text("inventory_currency_sp")) },
                    singleLine = true
                )
                OutlinedTextField(
                    value = copperDraft,
                    onValueChange = { copperDraft = it.filter(Char::isDigit) },
                    label = { Text(text("inventory_currency_cp")) },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        copperDraft.toIntOrNull() ?: 0,
                        silverDraft.toIntOrNull() ?: 0,
                        goldDraft.toIntOrNull() ?: 0
                    )
                }
            ) {
                Text(text("common_save"))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text("common_cancel"))
            }
        }
    )
}

@Composable
private fun CarryWeightBlock(
    current: Double,
    maximum: Double
) {
    val safeMaximum = maximum.coerceAtLeast(1.0)
    LimitProgressBar(
        label = text("inventory_carry_weight"),
        value = "${formatWeight(current)} / ${formatWeight(safeMaximum)} ${text("inventory_unit_pounds")}",
        progress = (current / safeMaximum).toFloat()
    )
}

@Composable
private fun InventorySearchField(
    value: String,
    onValueChange: (String) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF17141B).copy(alpha = 0.62f),
        border = BorderStroke(1.dp, Color(0x36FFFFFF))
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = null,
                    tint = Color(0xFFD2CAC2),
                    modifier = Modifier.size(28.dp)
                )
            },
            placeholder = {
                Text(
                    text = text("inventory_search_placeholder"),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFFD2CAC2).copy(alpha = 0.72f)
                )
            },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                cursorColor = Color(0xFFFFF6EA)
            )
        )
    }
}

@Composable
private fun InventorySectionTitle(title: String) {
    val tokens = LocalDesignTokens.current.typography
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium.copy(fontSize = tokens.headlineMedium.fontSizeSp.sp),
            color = Color(0xFFF7F2EA),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Box(
            modifier = Modifier
                .padding(start = 12.dp)
                .weight(1f)
                .height(1.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawLine(
                    color = Color(0x33FFFFFF),
                    start = Offset(0f, size.height / 2f),
                    end = Offset(size.width, size.height / 2f),
                    strokeWidth = 1.dp.toPx()
                )
            }
        }
    }
}

@Composable
private fun InventoryCatalogRow(
    item: InventoryCatalogItem,
    russian: Boolean,
    onAdd: () -> Unit
) {
    val colors = LocalDesignTokens.current.colors
    val description = item.displayDescription(russian)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = colors.surface.card.copy(alpha = 0.62f),
        border = BorderStroke(1.dp, colors.border.muted)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = item.displayName(russian),
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.text.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                item.displayDetailLine(russian)?.let { detail ->
                    Text(
                        text = detail,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.text.label,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (description.isNotBlank()) {
                    Text(
                        text = description.replace('\n', ' '),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.text.muted,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (item.weight > 0.0) {
                    Text(
                        text = "${formatWeight(item.weight)} ${text("inventory_unit_pounds")}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.text.label
                    )
                }
                IconButton(onClick = onAdd) {
                    Icon(
                        imageVector = Icons.Outlined.Add,
                        contentDescription = text("inventory_add_item"),
                        tint = colors.text.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun InventoryAddEntryDialog(
    catalogItems: List<InventoryCatalogItem>,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onCreateItem: () -> Unit,
    onSelectCatalogItem: (
        item: InventoryCatalogItem,
        base: InventoryCatalogItem?,
        variant: InventoryCatalogBonusVariant?
    ) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var shownKind by remember { mutableStateOf(InventoryCatalogKind.ITEM) }
    var choosingBaseFor by remember { mutableStateOf<InventoryCatalogItem?>(null) }
    val russian = LocalStrings.current.language == AppLanguage.RUSSIAN
    val catalogById = remember(catalogItems) { catalogItems.associateBy { it.id } }
    val filteredItems = remember(catalogItems, query, russian, shownKind) {
        val needle = query.trim()
        catalogItems
            .filter { it.kind == shownKind }
            .filter { item ->
                needle.isBlank() ||
                    item.name.contains(needle, ignoreCase = true) ||
                    item.ruName.contains(needle, ignoreCase = true) ||
                    item.displayDescription(russian).contains(needle, ignoreCase = true)
            }
            .sortedBy { it.displayName(russian) }
    }
    val onAddCatalogEntry = { item: InventoryCatalogItem ->
        val bases = item.baseItemIds.mapNotNull(catalogById::get)
        // Enchantments always need a base; magic items with several possible bases ask which one.
        if (item.kind == InventoryCatalogKind.ENCHANTMENT || bases.size > 1) {
            choosingBaseFor = item
        } else {
            onSelectCatalogItem(item, bases.firstOrNull(), null)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = text("inventory_add_item"),
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    InventoryDialogSection(text("inventory_add_create_section"))
                    TextButton(onClick = onCreateItem) {
                        Text(text("inventory_create_action"))
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    InventoryDialogSection(text("inventory_add_catalog_section"))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        InventoryToggleButton(
                            modifier = Modifier.weight(1f),
                            label = text("inventory_catalog_tab_items"),
                            selected = shownKind == InventoryCatalogKind.ITEM,
                            onClick = { shownKind = InventoryCatalogKind.ITEM }
                        )
                        InventoryToggleButton(
                            modifier = Modifier.weight(1f),
                            label = text("inventory_catalog_tab_enchantments"),
                            selected = shownKind == InventoryCatalogKind.ENCHANTMENT,
                            onClick = { shownKind = InventoryCatalogKind.ENCHANTMENT }
                        )
                    }
                    InventorySearchField(
                        value = query,
                        onValueChange = { query = it }
                    )
                    when {
                        isLoading -> {
                            Text(
                                text = text("inventory_catalog_loading"),
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFFD2CAC2)
                            )
                        }

                        filteredItems.isEmpty() -> {
                            Text(
                                text = text("inventory_catalog_empty"),
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFFD2CAC2)
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
                                    InventoryCatalogRow(
                                        item = item,
                                        russian = russian,
                                        onAdd = { onAddCatalogEntry(item) }
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

    choosingBaseFor?.let { magicItem ->
        InventoryBaseItemDialog(
            magicItem = magicItem,
            bases = magicItem.baseItemIds.mapNotNull(catalogById::get),
            russian = russian,
            onDismiss = { choosingBaseFor = null },
            onSelect = { base, variant ->
                choosingBaseFor = null
                onSelectCatalogItem(magicItem, base, variant)
            }
        )
    }
}

/**
 * Picks the mundane item an enchantment ("Weapon +1", "Flame Tongue") or a magic item with several
 * possible bases ("Frost Brand") is made from, plus the "+N" for enchantments that have variants.
 */
@Composable
private fun InventoryBaseItemDialog(
    magicItem: InventoryCatalogItem,
    bases: List<InventoryCatalogItem>,
    russian: Boolean,
    onDismiss: () -> Unit,
    onSelect: (base: InventoryCatalogItem, variant: InventoryCatalogBonusVariant?) -> Unit
) {
    val colors = LocalDesignTokens.current.colors
    var variant by remember(magicItem) { mutableStateOf(magicItem.bonusVariants.firstOrNull()) }
    val sortedBases = remember(bases, russian) { bases.sortedBy { it.displayName(russian) } }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = magicItem.displayName(russian),
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (magicItem.bonusVariants.isNotEmpty()) {
                    InventoryDialogSection(text("inventory_enchant_bonus"))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        magicItem.bonusVariants.forEach { option ->
                            InventoryToggleButton(
                                modifier = Modifier.weight(1f),
                                label = "+${option.bonus}",
                                selected = option == variant,
                                onClick = { variant = option }
                            )
                        }
                    }
                }
                InventoryDialogSection(text("inventory_enchant_choose_base"))
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(sortedBases, key = { it.id }) { base ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            color = colors.surface.card.copy(alpha = 0.62f),
                            border = BorderStroke(1.dp, colors.border.muted),
                            onClick = { onSelect(base, variant) }
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = magicItem.composedName(base, variant, russian),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = colors.text.primary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                base.displayDetailLine(russian)?.let { detail ->
                                    Text(
                                        text = detail,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = colors.text.label,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
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
private fun InventoryToggleButton(
    modifier: Modifier = Modifier,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val colors = LocalDesignTokens.current.colors
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = if (selected) colors.surface.selected else colors.surface.button,
        border = BorderStroke(1.dp, if (selected) colors.border.selected else colors.border.muted),
        onClick = onClick
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = if (selected) colors.text.warmPrimary else colors.text.muted,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun InventoryCategoryPickerDialog(
    onDismiss: () -> Unit,
    onSelectCategory: (InventoryCategory) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = text("inventory_select_item_type"),
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(InventoryCategory.entries) { category ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectCategory(category) },
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0x14FFFFFF),
                        border = BorderStroke(1.dp, Color(0x30FFFFFF))
                    ) {
                        Text(
                            text = category.title(),
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color(0xFFF7F2EA)
                        )
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

/**
 * Items as the inventory lists them: a card of rows with weight, quantity and tags; a row unfolds
 * its description and an Edit button, as the Features cards do. Without [onToggleEquipped] and
 * [onEditItem] it only shows them (Character Wizard's starting equipment).
 */
@Composable
internal fun InventorySectionCard(
    items: List<InventoryItem>,
    dexterityScore: Int,
    onToggleEquipped: ((InventoryItem) -> Unit)? = null,
    onEditItem: ((InventoryItem) -> Unit)? = null,
    expanded: Set<Long> = emptySet(),
    onExpandedChange: (InventoryItem, Boolean) -> Unit = { _, _ -> }
) {
    val colors = LocalDesignTokens.current.colors
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = colors.surface.card.copy(alpha = 0.62f),
        border = BorderStroke(1.dp, colors.border.muted)
    ) {
        Column {
            items.forEachIndexed { index, item ->
                key(item.renderKey()) {
                    InventoryItemRow(
                        item = item,
                        dexterityScore = dexterityScore,
                        onToggleEquipped = onToggleEquipped?.let { toggle -> { toggle(item) } },
                        onEdit = onEditItem?.let { edit -> { edit(item) } },
                        expanded = item.id in expanded,
                        onExpandedChange = { open -> onExpandedChange(item, open) }
                    )
                    if (index != items.lastIndex) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(colors.ornament.outer)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun InventoryItemRow(
    item: InventoryItem,
    dexterityScore: Int,
    onToggleEquipped: (() -> Unit)?,
    onEdit: (() -> Unit)?,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit
) {
    val strings = LocalStrings.current
    val colors = LocalDesignTokens.current.colors
    val propertyTags = remember(item, dexterityScore, strings.language) { item.propertyTags(dexterityScore, strings) }
    // A tap unfolds the row, a long press edits; Character Wizard's rows only show the item.
    val canExpand = onEdit != null

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (canExpand) {
                    Modifier.combinedClickable(onClick = { onExpandedChange(!expanded) }, onLongClick = onEdit)
                } else {
                    Modifier
                }
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Outlined.Inventory2,
                contentDescription = null,
                tint = colors.text.muted,
                modifier = Modifier.size(26.dp)
            )
            Text(
                text = item.name,
                modifier = Modifier
                    .padding(start = 12.dp)
                    .weight(1f),
                style = MaterialTheme.typography.bodyLarge,
                color = if (item.isMagical) colors.accent.hpTemporary else colors.text.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${formatWeight(item.weight)} ${text("inventory_unit_pounds")}",
                modifier = Modifier.padding(start = 10.dp, end = 8.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.text.label,
                maxLines = 1
            )
            Text(
                text = "x${item.quantity}",
                modifier = Modifier.padding(horizontal = 12.dp),
                style = MaterialTheme.typography.bodyLarge,
                color = colors.text.muted
            )
            if (onToggleEquipped != null) {
                SelectableDot(
                    selected = item.isEquipped,
                    onClick = onToggleEquipped
                )
            }
            if (canExpand) {
                IconButton(onClick = { onExpandedChange(!expanded) }, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                        contentDescription = null,
                        tint = colors.text.muted
                    )
                }
            }
        }

        if (propertyTags.isNotEmpty()) {
            InventoryPropertyTags(
                tags = propertyTags,
                modifier = Modifier.padding(start = 38.dp)
            )
        }
        if (expanded && onEdit != null) {
            Text(
                text = item.description.trim().ifBlank { text("inventory_no_description") },
                modifier = Modifier.padding(start = 38.dp, top = 2.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.text.muted
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                CardEditButton(onClick = onEdit)
            }
        }
    }
}

@Composable
private fun InventoryItemEditDialog(
    inventoryItem: InventoryItem,
    title: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onSave: (InventoryItem) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    val strings = LocalStrings.current
    var name by remember(inventoryItem) { mutableStateOf(inventoryItem.name) }
    var description by remember(inventoryItem) { mutableStateOf(inventoryItem.description) }
    var isMagical by remember(inventoryItem) { mutableStateOf(inventoryItem.isMagical) }
    var magicalBonus by remember(inventoryItem) { mutableStateOf(inventoryItem.magicalBonus.toString()) }
    var quantity by remember(inventoryItem) { mutableStateOf(inventoryItem.quantity.toString()) }
    var weight by remember(inventoryItem) { mutableStateOf(formatEditableNumber(inventoryItem.weight)) }
    var costQuantity by remember(inventoryItem) { mutableStateOf(inventoryItem.costQuantity?.toString().orEmpty()) }
    var costUnit by remember(inventoryItem) { mutableStateOf(inventoryItem.costUnit ?: defaultCurrencyUnit()) }

    var armorType by remember(inventoryItem) { mutableStateOf(inventoryItem.armorDetails?.armorType ?: InventoryArmorType.LIGHT) }
    var armorClass by remember(inventoryItem) { mutableStateOf(inventoryItem.armorDetails?.armorClass?.toString().orEmpty()) }
    var appliesDexterityBonus by remember(inventoryItem) { mutableStateOf(inventoryItem.armorDetails?.appliesDexterityBonus ?: false) }
    var maxDexterityBonus by remember(inventoryItem) { mutableStateOf(inventoryItem.armorDetails?.maxDexterityBonus?.toString().orEmpty()) }
    var strengthMinimum by remember(inventoryItem) { mutableStateOf(inventoryItem.armorDetails?.strengthMinimum?.toString().orEmpty()) }
    var hasStealthDisadvantage by remember(inventoryItem) { mutableStateOf(inventoryItem.armorDetails?.hasStealthDisadvantage ?: false) }

    // Derived purely from inventoryItem and only used to seed the remember(...) drafts below;
    // memoize so the dice-string parsing isn't redone on every recomposition of the open dialog.
    val initialWeaponKind = remember(inventoryItem) { inventoryItem.weaponDetails.toWeaponKindOption() }
    val initialPrimaryDamage = remember(inventoryItem) { inventoryItem.weaponDetails.toPrimaryDamageEditorState() }
    val initialAlternateDamage = remember(inventoryItem) { inventoryItem.weaponDetails?.twoHandedDamage.toEditorState() }

    var weaponKind by remember(inventoryItem) { mutableStateOf(initialWeaponKind) }
    var isWeaponTypeDialogOpen by remember { mutableStateOf(false) }
    var isBaseWeaponDialogOpen by remember { mutableStateOf(false) }
    var isCurrencyUnitDialogOpen by remember { mutableStateOf(false) }
    var isWeaponPropertiesDialogOpen by remember { mutableStateOf(false) }
    var hasAlternateDamage by remember(inventoryItem) {
        mutableStateOf(inventoryItem.weaponDetails?.twoHandedDamage != null)
    }
    var weaponNormalRange by remember(inventoryItem) { mutableStateOf(inventoryItem.weaponDetails?.normalRange?.toString().orEmpty()) }
    var weaponLongRange by remember(inventoryItem) { mutableStateOf(inventoryItem.weaponDetails?.longRange?.toString().orEmpty()) }
    var primaryDamageCount by remember(inventoryItem) { mutableStateOf(initialPrimaryDamage.diceCount) }
    var primaryDamageDieType by remember(inventoryItem) { mutableStateOf(initialPrimaryDamage.dieType) }
    var primaryDamageType by remember(inventoryItem) { mutableStateOf(initialPrimaryDamage.damageType) }
    var alternateDamageCount by remember(inventoryItem) { mutableStateOf(initialAlternateDamage.diceCount) }
    var alternateDamageDieType by remember(inventoryItem) { mutableStateOf(initialAlternateDamage.dieType) }
    var alternateDamageType by remember(inventoryItem) { mutableStateOf(initialAlternateDamage.damageType) }
    var weaponProperties by remember(inventoryItem) {
        mutableStateOf(inventoryItem.weaponDetails?.properties ?: emptySet())
    }
    var baseWeaponId by remember(inventoryItem) {
        // Items added from the catalog before ids were normalized may still hold "light-hammer".
        mutableStateOf(inventoryItem.weaponDetails?.baseWeaponId.orEmpty().replace('-', '_'))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        InventoryDialogSection(text("inventory_section_description"))
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text(text("inventory_field_name")) },
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text(text("inventory_field_description")) },
                            minLines = 3,
                            maxLines = 6,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isMagical,
                                    onCheckedChange = {
                                        isMagical = it
                                        if (it && magicalBonus.isBlank()) magicalBonus = "1"
                                    }
                                )
                                Text(text("inventory_field_magical"))
                            }
                            if (isMagical && inventoryItem.category.allowsMagicalBonus()) {
                                CompactTextField(
                                    value = magicalBonus,
                                    onValueChange = { magicalBonus = sanitizeSignedIntegerInput(it) },
                                    label = text("inventory_field_magical_bonus"),
                                    prefixText = "+",
                                    modifier = Modifier.widthIn(max = 112.dp)
                                )
                            }
                        }
                        CompactNumberStepperField(
                            label = text("inventory_field_quantity"),
                            value = quantity.toIntOrNull() ?: 1,
                            onValueChange = { quantity = it.toString() },
                            minValue = 1,
                            modifier = Modifier.widthIn(max = 118.dp)
                        )
                    }
                }

                if (inventoryItem.armorDetails != null) {
                    item {
                        InventoryDialogSection(text("inventory_section_armor"))
                    }
                    item {
                        EnumSelectorRow(
                            label = text("inventory_field_armor_type"),
                            options = InventoryArmorType.entries,
                            selected = armorType,
                            labelForOption = { strings[it.localizationKey()] },
                            onSelected = { armorType = it }
                        )
                    }
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = armorClass,
                                onValueChange = { armorClass = it.filter(Char::isDigit) },
                                label = { Text(text("inventory_field_armor_class")) },
                                singleLine = true
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = appliesDexterityBonus,
                                    onCheckedChange = { appliesDexterityBonus = it }
                                )
                                Text(text("inventory_field_applies_dex"))
                            }
                            if (appliesDexterityBonus) {
                                OutlinedTextField(
                                    value = maxDexterityBonus,
                                    onValueChange = { maxDexterityBonus = it.filter(Char::isDigit) },
                                    label = { Text(text("inventory_field_max_dex_bonus")) },
                                    singleLine = true
                                )
                            }
                            OutlinedTextField(
                                value = strengthMinimum,
                                onValueChange = { strengthMinimum = it.filter(Char::isDigit) },
                                label = { Text(text("inventory_field_strength_minimum")) },
                                singleLine = true
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = hasStealthDisadvantage,
                                    onCheckedChange = { hasStealthDisadvantage = it }
                                )
                                Text(text("inventory_field_stealth_disadvantage"))
                            }
                        }
                    }
                }

                if (inventoryItem.weaponDetails != null) {
                    item {
                        InventoryDialogSection(text("inventory_section_weapon"))
                    }
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            InventoryDialogReadOnlyField(
                                label = text("inventory_field_category"),
                                value = inventoryItem.category.title(),
                                modifier = Modifier.weight(1f)
                            )
                            SelectionField(
                                label = text("inventory_field_weapon_type"),
                                value = "${strings[weaponKind.weaponClass.localizationKey()]} ${strings[weaponKind.rangeType.localizationKey()]}",
                                onClick = { isWeaponTypeDialogOpen = true },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    item {
                        SelectionField(
                            label = text("inventory_field_base_weapon"),
                            value = strings[baseWeaponLabelKey(baseWeaponId)],
                            onClick = { isBaseWeaponDialogOpen = true }
                        )
                    }
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            CompactTextField(
                                value = weaponNormalRange,
                                onValueChange = { weaponNormalRange = it.filter(Char::isDigit) },
                                label = text("inventory_field_normal_range"),
                                suffixText = text("inventory_unit_feet"),
                                modifier = Modifier.weight(1f)
                            )
                            CompactTextField(
                                value = weaponLongRange,
                                onValueChange = { weaponLongRange = it.filter(Char::isDigit) },
                                label = text("inventory_field_long_range"),
                                suffixText = text("inventory_unit_feet"),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    item {
                        SelectionTagField(
                            label = text("inventory_field_weapon_properties"),
                            tags = weaponProperties.map { strings[it.localizationKey()] },
                            placeholder = text("common_none"),
                            onClick = { isWeaponPropertiesDialogOpen = true }
                        )
                    }
                    item {
                        WeaponDamageEditor(
                            title = text("inventory_field_damage"),
                            diceCount = primaryDamageCount,
                            onDiceCountChange = { primaryDamageCount = it.filter(Char::isDigit) },
                            dieType = primaryDamageDieType,
                            onDieTypeChange = { primaryDamageDieType = it },
                            damageType = primaryDamageType,
                            onDamageTypeChange = { primaryDamageType = it }
                        )
                    }
                    if (hasAlternateDamage) {
                        item {
                            WeaponDamageEditor(
                                title = text("inventory_field_two_handed_damage"),
                                diceCount = alternateDamageCount,
                                onDiceCountChange = { alternateDamageCount = it.filter(Char::isDigit) },
                                dieType = alternateDamageDieType,
                                onDieTypeChange = { alternateDamageDieType = it },
                                damageType = alternateDamageType,
                                onDamageTypeChange = { alternateDamageType = it }
                            )
                        }
                        item {
                            TextButton(
                                onClick = {
                                    hasAlternateDamage = false
                                    alternateDamageCount = "1"
                                    alternateDamageDieType = "d4"
                                    alternateDamageType = defaultDamageType()
                                }
                            ) {
                                Text(text("inventory_remove_alternate_damage"))
                            }
                        }
                    } else {
                        item {
                            TextButton(
                                onClick = {
                                    hasAlternateDamage = true
                                    if (alternateDamageCount.isBlank()) alternateDamageCount = "1"
                                    if (alternateDamageDieType.isBlank()) {
                                        alternateDamageDieType = "d4"
                                    }
                                    if (alternateDamageType.isBlank()) {
                                        alternateDamageType = defaultDamageType()
                                    }
                                }
                            ) {
                                Text(text("inventory_add_alternate_damage"))
                            }
                        }
                    }
                }

                item {
                    InventoryDialogSection(text("inventory_section_inventory"))
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        CompactTextField(
                            value = weight,
                            onValueChange = { weight = sanitizeDecimalInput(it) },
                            label = text("inventory_field_weight"),
                            suffixText = text("inventory_unit_pounds"),
                            modifier = Modifier.weight(1f)
                        )
                        CompactTextField(
                            value = costQuantity,
                            onValueChange = { costQuantity = it.filter(Char::isDigit) },
                            label = text("inventory_field_cost_quantity"),
                            modifier = Modifier.weight(1f)
                        )
                        CompactSelectionField(
                            label = text("inventory_field_cost_unit"),
                            value = currencyShortLabel(costUnit.ifBlank { defaultCurrencyUnit() }, strings),
                            onClick = { isCurrencyUnitDialogOpen = true },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val primaryDamage = WeaponDamageEditorState(
                        diceCount = primaryDamageCount,
                        dieType = primaryDamageDieType,
                        damageType = primaryDamageType
                    ).toWeaponDamage()
                    val alternateDamage = WeaponDamageEditorState(
                        diceCount = alternateDamageCount,
                        dieType = alternateDamageDieType,
                        damageType = alternateDamageType
                    ).toWeaponDamage()

                    val updatedArmorDetails = if (inventoryItem.armorDetails != null) {
                        InventoryArmorDetails(
                            armorType = armorType,
                            armorClass = armorClass.toIntOrNull()?.coerceAtLeast(1) ?: (inventoryItem.armorDetails.armorClass),
                            appliesDexterityBonus = appliesDexterityBonus,
                            maxDexterityBonus = if (appliesDexterityBonus) maxDexterityBonus.toIntOrNull() else null,
                            strengthMinimum = strengthMinimum.toIntOrNull()?.coerceAtLeast(0) ?: 0,
                            hasStealthDisadvantage = hasStealthDisadvantage
                        )
                    } else {
                        null
                    }

                    val updatedWeaponDetails = if (inventoryItem.weaponDetails != null) {
                        InventoryWeaponDetails(
                            weaponClass = weaponKind.weaponClass,
                            rangeType = weaponKind.rangeType,
                            baseWeaponId = baseWeaponId.ifBlank { null },
                            normalRange = weaponNormalRange.toIntOrNull(),
                            longRange = weaponLongRange.toIntOrNull(),
                            damages = listOfNotNull(primaryDamage),
                            twoHandedDamage = if (hasAlternateDamage) alternateDamage else null,
                            properties = weaponProperties
                        )
                    } else {
                        null
                    }

                    onSave(
                        inventoryItem.copy(
                            name = name.trim().ifBlank { inventoryItem.name },
                            description = description.trim(),
                            isMagical = isMagical,
                            magicalBonus = if (isMagical && inventoryItem.category.allowsMagicalBonus()) {
                                magicalBonus.toIntOrNull() ?: 1
                            } else {
                                1
                            },
                            quantity = quantity.toIntOrNull()?.coerceAtLeast(1) ?: inventoryItem.quantity,
                            weight = weight.toDoubleOrNull()?.coerceAtLeast(0.0) ?: inventoryItem.weight,
                            costQuantity = costQuantity.toIntOrNull(),
                            costUnit = costUnit.trim().ifBlank { defaultCurrencyUnit() },
                            armorDetails = updatedArmorDetails,
                            weaponDetails = updatedWeaponDetails
                        )
                    )
                }
            ) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onDelete != null) {
                    TextButton(onClick = onDelete) {
                        Text(text("inventory_delete_action"))
                    }
                } else {
                    Box {}
                }
                TextButton(onClick = onDismiss) {
                    Text(text("common_cancel"))
                }
            }
        }
    )

    if (isWeaponTypeDialogOpen) {
        SelectionDialog(
            title = text("inventory_field_weapon_type"),
            options = weaponKindOptions(),
            selected = weaponKind,
            labelForOption = { "${strings[it.weaponClass.localizationKey()]} ${strings[it.rangeType.localizationKey()]}" },
            onDismiss = { isWeaponTypeDialogOpen = false },
            onSelect = {
                weaponKind = it
                isWeaponTypeDialogOpen = false
            }
        )
    }

    if (isBaseWeaponDialogOpen) {
        SelectionDialog(
            title = text("inventory_field_base_weapon"),
            options = baseWeaponOptions,
            selected = baseWeaponOptions.firstOrNull { it.id == baseWeaponId } ?: baseWeaponOptions.first(),
            labelForOption = { strings[it.labelKey] },
            onDismiss = { isBaseWeaponDialogOpen = false },
            onSelect = {
                baseWeaponId = it.id
                isBaseWeaponDialogOpen = false
            }
        )
    }

    if (isCurrencyUnitDialogOpen) {
        SelectionDialog(
            title = text("inventory_field_cost_unit"),
            options = currencyUnitOptions(),
            selected = costUnit.ifBlank { defaultCurrencyUnit() },
            labelForOption = { currencyShortLabel(it, strings) },
            onDismiss = { isCurrencyUnitDialogOpen = false },
            onSelect = {
                costUnit = it
                isCurrencyUnitDialogOpen = false
            }
        )
    }

    if (isWeaponPropertiesDialogOpen) {
        MultiSelectionDialog(
            title = text("inventory_field_weapon_properties"),
            options = InventoryWeaponProperty.entries,
            selected = weaponProperties,
            labelForOption = { strings[it.localizationKey()] },
            onDismiss = { isWeaponPropertiesDialogOpen = false },
            onToggle = { option ->
                weaponProperties = weaponProperties.toggled(option, option !in weaponProperties)
            }
        )
    }
}

@Composable
private fun InventoryDialogSection(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = Color(0xFFF7F2EA)
    )
}

@Composable
private fun InventoryDialogReadOnlyField(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFFD2CAC2)
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            color = Color(0x14FFFFFF),
            border = BorderStroke(1.dp, Color(0x30FFFFFF))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(CompactEditorFieldHeight)
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFFF7F2EA),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun WeaponDamageEditor(
    title: String,
    diceCount: String,
    onDiceCountChange: (String) -> Unit,
    dieType: String,
    onDieTypeChange: (String) -> Unit,
    damageType: String,
    onDamageTypeChange: (String) -> Unit
) {
    val strings = LocalStrings.current
    var isDieTypeDialogOpen by remember { mutableStateOf(false) }
    var isDamageTypeDialogOpen by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        InventoryDialogSection(title)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top
        ) {
            CompactNumberStepperField(
                label = text("inventory_field_damage_dice_count"),
                value = diceCount.toIntOrNull() ?: 1,
                onValueChange = { onDiceCountChange(it.toString()) },
                minValue = 0,
                modifier = Modifier.weight(1.25f)
            )
            CompactSelectionField(
                modifier = Modifier.weight(1f),
                label = text("inventory_field_damage_die_type"),
                value = dieType,
                onClick = { isDieTypeDialogOpen = true }
            )
        }
        CompactSelectionField(
            modifier = Modifier.fillMaxWidth(),
            label = text("inventory_field_damage_type"),
            value = strings[damageTypeLocalizationKey(damageType)],
            onClick = { isDamageTypeDialogOpen = true }
        )
    }

    if (isDieTypeDialogOpen) {
        SelectionDialog(
            title = text("inventory_field_damage_die_type"),
            options = weaponDieTypeOptions(),
            selected = dieType,
            labelForOption = { it },
            onDismiss = { isDieTypeDialogOpen = false },
            onSelect = {
                onDieTypeChange(it)
                isDieTypeDialogOpen = false
            }
        )
    }

    if (isDamageTypeDialogOpen) {
        SelectionDialog(
            title = text("inventory_field_damage_type"),
            options = damageTypeOptions(),
            selected = damageType,
            labelForOption = { strings[damageTypeLocalizationKey(it)] },
            onDismiss = { isDamageTypeDialogOpen = false },
            onSelect = {
                onDamageTypeChange(it)
                isDamageTypeDialogOpen = false
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SelectionTagField(
    label: String,
    tags: List<String>,
    placeholder: String,
    onClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFFD2CAC2)
        )
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (tags.isEmpty()) {
                Text(
                    text = placeholder,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFAAA29A)
                )
            } else {
                tags.forEach { tag ->
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = Color(0x22FFF6EA),
                        border = BorderStroke(1.dp, Color(0x30FFFFFF))
                    ) {
                        Text(
                            text = tag,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFFE6DED3)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectionField(
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFFD2CAC2)
        )
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
            shape = RoundedCornerShape(10.dp),
            color = Color(0x14FFFFFF),
            border = BorderStroke(1.dp, Color(0x30FFFFFF))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(CompactEditorFieldHeight)
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFFF7F2EA),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun CompactSelectionField(
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFFD2CAC2)
        )
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
            shape = RoundedCornerShape(10.dp),
            color = Color(0x14FFFFFF),
            border = BorderStroke(1.dp, Color(0x30FFFFFF))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(CompactEditorFieldHeight)
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFFF7F2EA),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun CompactTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    suffixText: String? = null,
    prefixText: String? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFFD2CAC2),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(CompactEditorFieldHeight),
            shape = RoundedCornerShape(10.dp),
            color = Color(0x14FFFFFF),
            border = BorderStroke(1.dp, Color(0x30FFFFFF))
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color(0xFFF7F2EA)),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { innerTextField ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(CompactEditorFieldHeight)
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (prefixText != null && value.isNotBlank()) {
                            Text(
                                text = prefixText,
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color(0xFFF7F2EA),
                                maxLines = 1
                            )
                        }
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            innerTextField()
                        }
                        if (suffixText != null) {
                            Text(
                                text = suffixText,
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color(0xFFD2CAC2),
                                maxLines = 1
                            )
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun CompactNumberStepperField(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    minValue: Int = 0,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFFD2CAC2)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StepperButton(label = "-") {
                onValueChange((value - 1).coerceAtLeast(minValue))
            }
            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp),
                color = Color(0x14FFFFFF),
                border = BorderStroke(1.dp, Color(0x30FFFFFF))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(CompactEditorFieldHeight)
                        .padding(horizontal = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = value.toString(),
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color(0xFFF7F2EA),
                        textAlign = TextAlign.Center
                    )
                }
            }
            StepperButton(label = "+") {
                onValueChange(value + 1)
            }
        }
    }
}

@Composable
private fun StepperButton(
    label: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = Color(0x14FFFFFF),
        border = BorderStroke(1.dp, Color(0x30FFFFFF))
    ) {
        Box(
            modifier = Modifier
                .height(CompactEditorFieldHeight)
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFFF7F2EA)
            )
        }
    }
}

@Composable
private fun <T> SelectionDialog(
    title: String,
    options: List<T>,
    selected: T,
    labelForOption: (T) -> String,
    onDismiss: () -> Unit,
    onSelect: (T) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(options) { option ->
                    val isSelected = option == selected
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(option) },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) Color(0x22FFF6EA) else Color.Transparent,
                        border = BorderStroke(1.dp, if (isSelected) Color(0x70FFFFFF) else Color(0x20FFFFFF))
                    ) {
                        Text(
                            text = labelForOption(option),
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color(0xFFF7F2EA)
                        )
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
private fun <T> MultiSelectionDialog(
    title: String,
    options: List<T>,
    selected: Set<T>,
    labelForOption: (T) -> String,
    onDismiss: () -> Unit,
    onToggle: (T) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(options) { option ->
                    val isSelected = option in selected
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onToggle(option) },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) Color(0x22FFF6EA) else Color.Transparent,
                        border = BorderStroke(1.dp, if (isSelected) Color(0x70FFFFFF) else Color(0x20FFFFFF))
                    ) {
                        Text(
                            text = labelForOption(option),
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color(0xFFF7F2EA)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text("common_save"))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text("common_cancel"))
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> EnumSelectorRow(
    label: String,
    options: List<T>,
    selected: T,
    labelForOption: (T) -> String,
    onSelected: (T) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFFD2CAC2)
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            options.forEach { option ->
                val isSelected = option == selected
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = if (isSelected) Color(0x22FFF6EA) else Color(0x141FFFFFF),
                    border = BorderStroke(1.dp, if (isSelected) Color(0x70FFFFFF) else Color(0x30FFFFFF)),
                    modifier = Modifier.clickable { onSelected(option) }
                ) {
                    Text(
                        text = labelForOption(option),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFFE6DED3)
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyInventoryMessage() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF17141B).copy(alpha = 0.42f),
        border = BorderStroke(1.dp, Color(0x30FFFFFF))
    ) {
        Text(
            text = text("inventory_empty"),
            modifier = Modifier.padding(18.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = Color(0xFFD2CAC2)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun InventoryPropertyTags(
    tags: List<String>,
    modifier: Modifier = Modifier
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        tags.forEach { tag ->
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = Color(0x22FFF6EA),
                border = BorderStroke(1.dp, Color(0x30FFFFFF))
            ) {
                Text(
                    text = tag,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFFE6DED3),
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun InventoryCategory.title(): String = text(titleKey())

private fun InventoryCategory.titleKey(): String =
    when (this) {
        InventoryCategory.WEAPON -> "inventory_category_weapon"
        InventoryCategory.ARMOR -> "inventory_category_armor"
        InventoryCategory.CONSUMABLE -> "inventory_category_consumable"
        InventoryCategory.OTHER -> "inventory_category_other"
    }

private fun formatWeight(value: Double): String {
    val rounded = (value * 10).roundToInt() / 10.0
    return if (rounded % 1.0 == 0.0) {
        rounded.toInt().toString()
    } else {
        rounded.toString()
    }
}

private fun formatEditableNumber(value: Double): String = formatWeight(value)

private fun sanitizeDecimalInput(value: String): String {
    var separatorUsed = false
    return buildString {
        value.forEach { char ->
            when {
                char.isDigit() -> append(char)
                (char == '.' || char == ',') && !separatorUsed -> {
                    append('.')
                    separatorUsed = true
                }
            }
        }
    }
}

private fun sanitizeSignedIntegerInput(value: String): String {
    return buildString {
        value.forEachIndexed { index, char ->
            when {
                char.isDigit() -> append(char)
                char == '-' && index == 0 && isEmpty() -> append(char)
                char == '+' && index == 0 && isEmpty() -> append(char)
            }
        }
    }
}

private fun weaponKindOptions(): List<WeaponKindOption> = listOf(
    WeaponKindOption(InventoryWeaponClass.SIMPLE, InventoryWeaponRangeType.MELEE),
    WeaponKindOption(InventoryWeaponClass.SIMPLE, InventoryWeaponRangeType.RANGED),
    WeaponKindOption(InventoryWeaponClass.MARTIAL, InventoryWeaponRangeType.MELEE),
    WeaponKindOption(InventoryWeaponClass.MARTIAL, InventoryWeaponRangeType.RANGED)
)

private val baseWeaponOptions: List<BaseWeaponOption> = listOf(
    BaseWeaponOption("", "common_none"),
    BaseWeaponOption("club", "Club"),
    BaseWeaponOption("dagger", "Dagger"),
    BaseWeaponOption("greatclub", "Greatclub"),
    BaseWeaponOption("handaxe", "Handaxe"),
    BaseWeaponOption("javelin", "Javelin"),
    BaseWeaponOption("light_hammer", "Light Hammer"),
    BaseWeaponOption("mace", "Mace"),
    BaseWeaponOption("quarterstaff", "Quarterstaff"),
    BaseWeaponOption("sickle", "Sickle"),
    BaseWeaponOption("spear", "Spear"),
    BaseWeaponOption("light_crossbow", "Light Crossbow"),
    BaseWeaponOption("dart", "Dart"),
    BaseWeaponOption("shortbow", "Shortbow"),
    BaseWeaponOption("sling", "Sling"),
    BaseWeaponOption("battleaxe", "Battleaxe"),
    BaseWeaponOption("flail", "Flail"),
    BaseWeaponOption("glaive", "Glaive"),
    BaseWeaponOption("greataxe", "Greataxe"),
    BaseWeaponOption("greatsword", "Greatsword"),
    BaseWeaponOption("halberd", "Halberd"),
    BaseWeaponOption("lance", "Lance"),
    BaseWeaponOption("longsword", "Longsword"),
    BaseWeaponOption("maul", "Maul"),
    BaseWeaponOption("morningstar", "Morningstar"),
    BaseWeaponOption("musket", "Musket"),
    BaseWeaponOption("pike", "Pike"),
    BaseWeaponOption("rapier", "Rapier"),
    BaseWeaponOption("scimitar", "Scimitar"),
    BaseWeaponOption("shortsword", "Shortsword"),
    BaseWeaponOption("trident", "Trident"),
    BaseWeaponOption("war_pick", "War Pick"),
    BaseWeaponOption("warhammer", "Warhammer"),
    BaseWeaponOption("whip", "Whip"),
    BaseWeaponOption("blowgun", "Blowgun"),
    BaseWeaponOption("hand_crossbow", "Hand Crossbow"),
    BaseWeaponOption("heavy_crossbow", "Heavy Crossbow"),
    BaseWeaponOption("longbow", "Longbow"),
    BaseWeaponOption("net", "Net")
)

private fun baseWeaponLabelKey(id: String): String =
    baseWeaponOptions.firstOrNull { it.id == id }?.labelKey ?: "common_none"

private fun InventoryWeaponDetails?.toWeaponKindOption(): WeaponKindOption =
    WeaponKindOption(
        weaponClass = this?.weaponClass ?: InventoryWeaponClass.SIMPLE,
        rangeType = this?.rangeType ?: InventoryWeaponRangeType.MELEE
    )

private fun InventoryWeaponDetails?.toPrimaryDamageEditorState(): WeaponDamageEditorState =
    this?.damages?.firstOrNull().toEditorState()

private fun InventoryWeaponDamage?.toEditorState(): WeaponDamageEditorState {
    if (this == null) {
        return WeaponDamageEditorState("1", "d4", defaultDamageType())
    }

    val resolvedDamageType = damageType.ifBlank { defaultDamageType() }
    val normalized = dice.replace(" ", "")
    val pureBonus = normalized.toIntOrNull()
    if (pureBonus != null) {
        return WeaponDamageEditorState(
            diceCount = "1",
            dieType = "d4",
            damageType = resolvedDamageType
        )
    }

    val match = Regex("""^(\d+)d(\d+)([+-]\d+)?$""").matchEntire(normalized)
    return if (match != null) {
        WeaponDamageEditorState(
            diceCount = match.groupValues[1],
            dieType = "d${match.groupValues[2]}",
            damageType = resolvedDamageType
        )
    } else {
        WeaponDamageEditorState(
            diceCount = "1",
            dieType = "d4",
            damageType = resolvedDamageType
        )
    }
}

private fun WeaponDamageEditorState.toWeaponDamage(): InventoryWeaponDamage? {
    val sanitizedCount = diceCount.toIntOrNull()?.takeIf { it > 0 } ?: return null
    val diceValue = "${sanitizedCount}${dieType.lowercase()}"
    val resolvedType = damageType.takeIf { it in damageTypeOptions() } ?: damageTypeOptions().first()
    return InventoryWeaponDamage(dice = diceValue, damageType = resolvedType)
}

private fun weaponDieTypeOptions(): List<String> = listOf("d4", "d6", "d8", "d10", "d12")

private fun defaultDamageType(): String = "Slashing"

private fun currencyUnitOptions(): List<String> = listOf("gp", "sp", "cp", "pp")

private fun defaultCurrencyUnit(): String = "gp"

/** Localized coin abbreviation for a stored SRD unit code ("gp" → "GP" / "зм"). */
private fun currencyShortLabel(unit: String, strings: LocalizedStrings): String {
    val key = "inventory_currency_short_${unit.trim().lowercase()}"
    val label = strings[key]
    return if (label == key) unit.uppercase() else label
}

private fun damageTypeOptions(): List<String> = listOf(
    "Acid",
    "Bludgeoning",
    "Cold",
    "Fire",
    "Force",
    "Lightning",
    "Necrotic",
    "Piercing",
    "Poison",
    "Psychic",
    "Radiant",
    "Slashing",
    "Thunder"
)

/** A new hand-made item, pre-named after its category in the current language ([defaultName]). */
private fun defaultInventoryItem(category: InventoryCategory, defaultName: String): InventoryItem {
    return InventoryItem(
        name = defaultName,
        isMagical = false,
        category = category,
        weight = 0.0,
        quantity = 1,
        isEquipped = false,
        icon = "",
        costUnit = defaultCurrencyUnit(),
        armorDetails = if (category == InventoryCategory.ARMOR) {
            InventoryArmorDetails(
                armorType = InventoryArmorType.LIGHT,
                armorClass = 10,
                appliesDexterityBonus = true,
                maxDexterityBonus = null,
                strengthMinimum = 0,
                hasStealthDisadvantage = false
            )
        } else {
            null
        },
        weaponDetails = if (category == InventoryCategory.WEAPON) {
            InventoryWeaponDetails(
                weaponClass = InventoryWeaponClass.SIMPLE,
                rangeType = InventoryWeaponRangeType.MELEE,
                baseWeaponId = null,
                normalRange = 5,
                longRange = null,
                damages = listOf(
                    InventoryWeaponDamage(dice = "1d4", damageType = defaultDamageType())
                ),
                properties = emptySet()
            )
        } else {
            null
        }
    )
}

/** Weapons add it to attack/damage, armor and shields to AC. */
private fun InventoryCategory.allowsMagicalBonus(): Boolean =
    this == InventoryCategory.WEAPON || this == InventoryCategory.ARMOR

private fun InventoryItem.propertyTags(dexterityScore: Int, strings: LocalizedStrings): List<String> {
    return buildList {
        armorDetails?.let { armor ->
            add(strings[armor.armorType.localizationKey()])
            add("${armor.armorClass + armorMagicBonus()} ${strings["inventory_tag_ac_short"]}")
            armor.currentDexterityTag(dexterityScore, strings)?.let(::add)
            if (armor.strengthMinimum > 0) {
                add("${strings["inventory_tag_strength_short"]} ${armor.strengthMinimum}")
            }
            if (armor.hasStealthDisadvantage) {
                add(strings["inventory_tag_stealth_dis"])
            }
        }
        weaponDetails?.let { weapon ->
            add(weapon.primaryTypeTag(strings))
            addAll(weapon.damageTags(strings, if (isMagical) magicalBonus else 0))
        }
    }
}

private fun InventoryArmorType.localizationKey(): String =
    when (this) {
        InventoryArmorType.LIGHT -> "inventory_armor_type_light"
        InventoryArmorType.MEDIUM -> "inventory_armor_type_medium"
        InventoryArmorType.HEAVY -> "inventory_armor_type_heavy"
        InventoryArmorType.SHIELD -> "inventory_armor_type_shield"
    }

private fun InventoryArmorDetails.currentDexterityTag(dexterityScore: Int, strings: LocalizedStrings): String? {
    if (!appliesDexterityBonus) return null

    val rawModifier = abilityModifier(dexterityScore)
    val appliedModifier = maxDexterityBonus?.let { cap ->
        rawModifier.coerceAtMost(cap)
    } ?: rawModifier

    return "${appliedModifier.signedValue()} ${strings["inventory_tag_dex_short"]}"
}

private fun InventoryWeaponDetails.primaryTypeTag(strings: LocalizedStrings): String {
    val parts = mutableListOf<String>()
    parts += strings[weaponClass.localizationKey()]
    parts += strings[rangeType.localizationKey()]
    formatRangeTag(strings)?.let(parts::add)
    return parts.joinToString(" ")
}

private fun InventoryWeaponDetails.damageTags(strings: LocalizedStrings, magicalBonus: Int): List<String> {
    if (damages.isEmpty()) return emptyList()

    val primaryDamage = damages.first()
    val primaryTag = primaryDamage.toTag(strings, magicalBonus)
    val twoHanded = twoHandedDamage
    return if (twoHanded != null && primaryDamage.damageType.equals(twoHanded.damageType, ignoreCase = true)) {
        listOf("${appendBonusToDice(primaryDamage.dice, magicalBonus)}/${twoHanded.dice} ${strings[damageTypeLocalizationKey(primaryDamage.damageType)]}")
    } else {
        buildList {
            add(primaryTag)
            addAll(damages.drop(1).map { it.toTag(strings, 0) })
            twoHanded?.let { add(it.toTag(strings, 0)) }
        }
    }
}

private fun InventoryWeaponDetails.formatRangeTag(strings: LocalizedStrings): String? {
    val normal = normalRange ?: return null
    val long = longRange
    return when {
        rangeType == InventoryWeaponRangeType.MELEE && normal <= 5 && long == null -> null
        long != null -> "$normal/$long ${strings["inventory_unit_feet"]}"
        else -> "$normal ${strings["inventory_unit_feet"]}"
    }
}

private fun InventoryWeaponDamage.toTag(strings: LocalizedStrings, magicalBonus: Int): String =
    "${appendBonusToDice(dice, magicalBonus)} ${strings[damageTypeLocalizationKey(damageType)]}"

private fun appendBonusToDice(dice: String, magicalBonus: Int): String {
    if (magicalBonus == 0) return dice
    val normalized = dice.replace(" ", "")
    val match = Regex("""^(\d+d\d+)([+-]\d+)?$""").matchEntire(normalized) ?: return dice
    val baseDice = match.groupValues[1]
    val existingBonus = match.groupValues.getOrNull(2)?.toIntOrNull() ?: 0
    val totalBonus = existingBonus + magicalBonus
    return buildString {
        append(baseDice)
        if (totalBonus > 0) append("+$totalBonus")
        if (totalBonus < 0) append(totalBonus)
    }
}

private fun InventoryWeaponClass.localizationKey(): String =
    when (this) {
        InventoryWeaponClass.SIMPLE -> "attributes_weapon_simple_short"
        InventoryWeaponClass.MARTIAL -> "attributes_weapon_martial_short"
    }

private fun InventoryWeaponRangeType.localizationKey(): String =
    when (this) {
        InventoryWeaponRangeType.MELEE -> "inventory_weapon_range_melee"
        InventoryWeaponRangeType.RANGED -> "inventory_weapon_range_ranged"
    }

private fun InventoryWeaponProperty.localizationKey(): String =
    when (this) {
        InventoryWeaponProperty.AMMUNITION -> "inventory_weapon_property_ammunition"
        InventoryWeaponProperty.FINESSE -> "inventory_weapon_property_finesse"
        InventoryWeaponProperty.HEAVY -> "inventory_weapon_property_heavy"
        InventoryWeaponProperty.LIGHT -> "inventory_weapon_property_light"
        InventoryWeaponProperty.LOADING -> "inventory_weapon_property_loading"
        InventoryWeaponProperty.REACH -> "inventory_weapon_property_reach"
        InventoryWeaponProperty.THROWN -> "inventory_weapon_property_thrown"
        InventoryWeaponProperty.TWO_HANDED -> "inventory_weapon_property_two_handed"
        InventoryWeaponProperty.VERSATILE -> "inventory_weapon_property_versatile"
    }

private fun damageTypeLocalizationKey(type: String): String =
    when (type) {
        "Acid" -> "inventory_damage_type_acid"
        "Bludgeoning" -> "inventory_damage_type_bludgeoning"
        "Cold" -> "inventory_damage_type_cold"
        "Fire" -> "inventory_damage_type_fire"
        "Force" -> "inventory_damage_type_force"
        "Lightning" -> "inventory_damage_type_lightning"
        "Necrotic" -> "inventory_damage_type_necrotic"
        "Piercing" -> "inventory_damage_type_piercing"
        "Poison" -> "inventory_damage_type_poison"
        "Psychic" -> "inventory_damage_type_psychic"
        "Radiant" -> "inventory_damage_type_radiant"
        "Slashing" -> "inventory_damage_type_slashing"
        "Thunder" -> "inventory_damage_type_thunder"
        else -> type
    }

private fun Int.signedValue(): String = if (this >= 0) "+$this" else toString()

private fun <T> Set<T>.toggled(value: T, isChecked: Boolean): Set<T> =
    if (isChecked) this + value else this - value

private fun InventoryItem.renderKey(): Any =
    if (id != 0L) {
        id
    } else {
        listOf(category.name, name, icon, weight, costQuantity, costUnit)
    }
