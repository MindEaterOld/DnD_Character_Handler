package com.dndcharacterhandler.presentation.biography

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.dndcharacterhandler.domain.model.Character
import com.dndcharacterhandler.domain.model.CharacterBundle
import com.dndcharacterhandler.domain.model.CharacterTextField
import com.dndcharacterhandler.domain.repository.CharacterRepository
import com.dndcharacterhandler.domain.usecase.GetCharacterBundleUseCase
import com.dndcharacterhandler.presentation.BaseCharacterViewModel
import com.dndcharacterhandler.presentation.SelectedCharacterHolder
import com.dndcharacterhandler.presentation.components.CharacterScreenHeader
import com.dndcharacterhandler.presentation.components.ScreenBackground
import com.dndcharacterhandler.presentation.components.ScreenTopActions
import com.dndcharacterhandler.presentation.localization.text
import com.dndcharacterhandler.presentation.theme.LocalDesignTokens
import kotlinx.coroutines.launch

class BiographyViewModel(
    private val characterRepository: CharacterRepository,
    getCharacterBundleUseCase: GetCharacterBundleUseCase,
    selectedCharacterHolder: SelectedCharacterHolder
) : BaseCharacterViewModel(getCharacterBundleUseCase, selectedCharacterHolder) {
    fun updateBiography(characterBundle: CharacterBundle, value: String) {
        val current = characterBundle.character
        if (current.biography == value) return
        viewModelScope.launch {
            characterRepository.updateTextField(
                characterId = current.id,
                field = CharacterTextField.BIOGRAPHY,
                value = value
            )
        }
    }

    fun updateBiographyField(characterBundle: CharacterBundle, field: BiographyField, value: String) {
        val current = characterBundle.character
        val sanitized = value.trim()
        val updated = when (field) {
            BiographyField.ALIGNMENT -> current.copy(alignment = sanitized)
            BiographyField.BACKGROUND -> current.copy(background = sanitized)
            BiographyField.FAITH -> current.copy(faith = sanitized)
            BiographyField.HOMELAND -> current.copy(homeland = sanitized)
            BiographyField.PERSONALITY_TRAITS -> current.copy(personalityTraits = sanitized)
            BiographyField.IDEALS -> current.copy(ideals = sanitized)
            BiographyField.BONDS -> current.copy(bonds = sanitized)
            BiographyField.FLAWS -> current.copy(flaws = sanitized)
            BiographyField.AGE -> current.copy(age = sanitized)
            BiographyField.GENDER -> current.copy(gender = sanitized)
            BiographyField.HEIGHT -> current.copy(height = sanitized)
            BiographyField.WEIGHT -> current.copy(weight = sanitized)
            BiographyField.EYES -> current.copy(eyes = sanitized)
            BiographyField.HAIR -> current.copy(hair = sanitized)
            BiographyField.SKIN -> current.copy(skin = sanitized)
        }
        if (updated == current) return

        viewModelScope.launch {
            characterRepository.updateTextField(
                characterId = current.id,
                field = field.toCharacterTextField(),
                value = sanitized
            )
        }
    }
}

private fun BiographyField.toCharacterTextField(): CharacterTextField =
    when (this) {
        BiographyField.ALIGNMENT -> CharacterTextField.ALIGNMENT
        BiographyField.BACKGROUND -> CharacterTextField.BACKGROUND
        BiographyField.FAITH -> CharacterTextField.FAITH
        BiographyField.HOMELAND -> CharacterTextField.HOMELAND
        BiographyField.PERSONALITY_TRAITS -> CharacterTextField.PERSONALITY_TRAITS
        BiographyField.IDEALS -> CharacterTextField.IDEALS
        BiographyField.BONDS -> CharacterTextField.BONDS
        BiographyField.FLAWS -> CharacterTextField.FLAWS
        BiographyField.AGE -> CharacterTextField.AGE
        BiographyField.GENDER -> CharacterTextField.GENDER
        BiographyField.HEIGHT -> CharacterTextField.HEIGHT
        BiographyField.WEIGHT -> CharacterTextField.WEIGHT
        BiographyField.EYES -> CharacterTextField.EYES
        BiographyField.HAIR -> CharacterTextField.HAIR
        BiographyField.SKIN -> CharacterTextField.SKIN
    }

@Composable
fun BiographyScreen(
    viewModel: BiographyViewModel,
    onOpenDrawer: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    BiographyContent(
        characterBundle = state.character,
        onOpenDrawer = onOpenDrawer,
        onOpenSettings = onOpenSettings,
        onUpdateBiography = viewModel::updateBiography,
        onUpdateField = viewModel::updateBiographyField
    )
}

@Composable
internal fun BiographyContent(
    characterBundle: CharacterBundle?,
    onOpenDrawer: () -> Unit,
    onOpenSettings: () -> Unit,
    onUpdateBiography: (CharacterBundle, String) -> Unit = { _, _ -> },
    onUpdateField: (CharacterBundle, BiographyField, String) -> Unit = { _, _, _ -> }
) {
    val character = characterBundle?.character
    var editingField by remember { mutableStateOf<BiographyField?>(null) }
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
                style = MaterialTheme.typography.titleLarge,
                color = Color(0xFFF7F2EA)
            )
            }
        }
        return
    }
    val resolvedCharacter = character
    val resolvedBundle = characterBundle
    ScreenBackground {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 4.dp, bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            item {
                CharacterScreenHeader(
                    character = resolvedCharacter,
                    onOpenDrawer = onOpenDrawer,
                    onOpenSettings = onOpenSettings
                )
            }
            item {
                BiographySection(
                    title = text("biography_identity"),
                    rows = listOf(
                        BiographyRow(
                            BiographyField.ALIGNMENT,
                            Icons.Outlined.Shield,
                            text("biography_alignment"),
                            localizedAlignment(resolvedCharacter.alignment)
                        ),
                        BiographyRow(BiographyField.BACKGROUND, Icons.Outlined.Description, text("biography_background"), resolvedCharacter.background),
                        BiographyRow(BiographyField.FAITH, Icons.Outlined.AutoAwesome, text("biography_faith"), resolvedCharacter.faith),
                        BiographyRow(BiographyField.HOMELAND, Icons.Outlined.Home, text("biography_homeland"), resolvedCharacter.homeland),
                        BiographyRow(BiographyField.PERSONALITY_TRAITS, Icons.Outlined.Badge, text("biography_personality_traits"), resolvedCharacter.personalityTraits),
                        BiographyRow(BiographyField.IDEALS, Icons.Outlined.AutoAwesome, text("biography_ideals"), resolvedCharacter.ideals),
                        BiographyRow(BiographyField.BONDS, Icons.Outlined.Shield, text("biography_bonds"), resolvedCharacter.bonds),
                        BiographyRow(BiographyField.FLAWS, Icons.Outlined.Description, text("biography_flaws"), resolvedCharacter.flaws)
                    ),
                    valueWeight = 1.45f,
                    onRowClick = { editingField = it.field }
                )
            }
            item {
                BiographySection(
                    modifier = Modifier.padding(top = 14.dp),
                    title = text("biography_appearance"),
                    rows = listOf(
                        BiographyRow(BiographyField.AGE, Icons.Outlined.Inventory2, text("biography_age"), resolvedCharacter.age),
                        BiographyRow(BiographyField.GENDER, Icons.Outlined.Badge, text("biography_gender"), localizedGender(resolvedCharacter.gender)),
                        BiographyRow(BiographyField.HEIGHT, Icons.Outlined.Badge, text("biography_height"), localizedMeasuredValue(resolvedCharacter.height)),
                        BiographyRow(BiographyField.WEIGHT, Icons.Outlined.Inventory2, text("biography_weight"), localizedMeasuredValue(resolvedCharacter.weight)),
                        BiographyRow(BiographyField.EYES, Icons.Outlined.Visibility, text("biography_eyes"), resolvedCharacter.eyes),
                        BiographyRow(BiographyField.HAIR, Icons.Outlined.AutoAwesome, text("biography_hair"), resolvedCharacter.hair),
                        BiographyRow(BiographyField.SKIN, Icons.Outlined.Badge, text("biography_skin"), resolvedCharacter.skin)
                    ),
                    onRowClick = { editingField = it.field }
                )
            }
            item {
                BiographyHistorySection(
                    characterId = resolvedCharacter.id,
                    history = resolvedCharacter.biography,
                    onHistoryChange = { value ->
                        onUpdateBiography(resolvedBundle, value)
                    },
                    modifier = Modifier.padding(top = 14.dp)
                )
            }
        }
    }

    val field = editingField
    if (field != null) {
        BiographyEditDialog(
            field = field,
            currentValue = field.valueFrom(resolvedCharacter),
            onDismiss = { editingField = null },
            onSave = { value ->
                onUpdateField(resolvedBundle, field, value)
                editingField = null
            }
        )
    }
}

@Composable
private fun BiographySection(
    title: String,
    rows: List<BiographyRow>,
    modifier: Modifier = Modifier,
    valueWeight: Float = 1f,
    onRowClick: (BiographyRow) -> Unit = {}
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        BiographySectionTitle(title)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            color = Color(0xFF17141B).copy(alpha = 0.62f),
            border = BorderStroke(1.dp, Color(0x36FFFFFF))
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
                rows.forEachIndexed { index, row ->
                    BiographyValueRow(
                        row = row,
                        valueWeight = valueWeight,
                        onClick = { onRowClick(row) }
                    )
                    if (index != rows.lastIndex) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(Color(0x2EFFFFFF))
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BiographySectionTitle(title: String) {
    val tokens = LocalDesignTokens.current.typography
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium.copy(fontSize = tokens.headlineMedium.fontSizeSp.sp),
            color = Color(0xFFF7F2EA)
        )
        Canvas(
            modifier = Modifier
                .padding(start = 14.dp)
                .weight(1f)
                .height(18.dp)
        ) {
            val centerY = size.height / 2f
            drawLine(
                color = Color(0x55A19892),
                start = Offset(0f, centerY),
                end = Offset(size.width - 18.dp.toPx(), centerY),
                strokeWidth = 1.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
private fun BiographyValueRow(
    row: BiographyRow,
    valueWeight: Float,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = row.icon,
            contentDescription = null,
            modifier = Modifier.size(22.dp),
            tint = Color(0xFFC2BBB3)
        )
        Text(
            text = row.label,
            modifier = Modifier
                .padding(start = 12.dp)
                .weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            color = Color(0xFFD2CAC2),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = row.value.ifBlank { text("common_dash") },
            modifier = Modifier
                .padding(start = 12.dp)
                .weight(valueWeight),
            style = MaterialTheme.typography.bodyLarge,
            color = Color(0xFFF7F2EA),
            textAlign = TextAlign.End,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun BiographyHistorySection(
    characterId: Long,
    history: String,
    onHistoryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // Keep edits local while typing and persist only once the field loses focus, so we don't
    // issue one DB write per keystroke (which also re-keyed this draft mid-typing).
    // Keyed on the character too: two characters with the same (e.g. empty) history must not
    // share a draft.
    val draftState = remember(characterId, history) { mutableStateOf(history) }
    var draft by draftState
    var wasFocused by remember { mutableStateOf(false) }

    // Safety net: persist the draft when it is replaced (another character was selected or its
    // saved history changed) or the screen leaves composition before a focus-lost event. Each
    // effect keeps the callback of the character its draft belongs to, so switching characters
    // can't write one character's text into another. updateBiography de-dupes equal values.
    DisposableEffect(draftState) {
        val saveForThisCharacter = onHistoryChange
        onDispose { saveForThisCharacter(draftState.value) }
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        BiographySectionTitle(text("biography_character_history"))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            color = Color(0xFF17141B).copy(alpha = 0.62f),
            border = BorderStroke(1.dp, Color(0x36FFFFFF))
        ) {
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { focusState ->
                        if (wasFocused && !focusState.isFocused) {
                            onHistoryChange(draft)
                        }
                        wasFocused = focusState.isFocused
                    },
                placeholder = {
                    Text(
                        text = text("biography_history_placeholder"),
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color(0xFFD2CAC2).copy(alpha = 0.48f)
                    )
                },
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color(0xFFD2CAC2)),
                minLines = 4,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                shape = RoundedCornerShape(10.dp),
                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    cursorColor = Color(0xFFFFF6EA)
                )
            )
        }
    }
}

@Composable
private fun BiographyEditDialog(
    field: BiographyField,
    currentValue: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    when (field.editor) {
        BiographyEditor.ALIGNMENT -> BiographyChoiceDialog(
            title = field.label(),
            currentValue = currentValue,
            options = alignmentOptions,
            onDismiss = onDismiss,
            onSelect = onSave
        )

        BiographyEditor.GENDER -> BiographyGenderDialog(
            currentValue = currentValue,
            onDismiss = onDismiss,
            onSave = onSave
        )

        BiographyEditor.HEIGHT -> BiographyHeightDialog(
            currentValue = currentValue,
            onDismiss = onDismiss,
            onSave = onSave
        )

        BiographyEditor.WEIGHT -> BiographyWeightDialog(
            currentValue = currentValue,
            onDismiss = onDismiss,
            onSave = onSave
        )

        BiographyEditor.NUMBER -> BiographyTextInputDialog(
            title = field.label(),
            currentValue = currentValue,
            keyboardType = KeyboardType.Number,
            onDismiss = onDismiss,
            onSave = onSave
        )

        BiographyEditor.TEXT -> BiographyTextInputDialog(
            title = field.label(),
            currentValue = currentValue,
            keyboardType = KeyboardType.Text,
            onDismiss = onDismiss,
            onSave = onSave
        )
    }
}

@Composable
private fun BiographyChoiceDialog(
    title: String,
    currentValue: String,
    options: List<BiographyChoiceOption>,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                options.forEach { option ->
                    BiographySelectionOption(
                        text = text(option.labelKey),
                        selected = option.value == currentValue,
                        onClick = { onSelect(option.value) }
                    )
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
private fun BiographyTextInputDialog(
    title: String,
    currentValue: String,
    keyboardType: KeyboardType,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var draft by remember(title, currentValue) { mutableStateOf(currentValue) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                singleLine = keyboardType != KeyboardType.Text,
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType)
            )
        },
        confirmButton = {
            Button(onClick = { onSave(draft) }) {
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
private fun BiographyGenderDialog(
    currentValue: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var selected by remember(currentValue) {
        mutableStateOf(
            when {
                currentValue.isBlank() -> GenderMaleOption
                currentValue in genderOptions -> currentValue
                else -> GenderCustomOption
            }
        )
    }
    var custom by remember(currentValue) {
        mutableStateOf(currentValue.takeUnless { it in genderOptions }.orEmpty())
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text("biography_gender")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                genderOptions.forEach { option ->
                    BiographySelectionOption(
                        text = localizedGender(option),
                        selected = selected == option,
                        onClick = { selected = option }
                    )
                }
                if (selected == GenderCustomOption) {
                    OutlinedTextField(
                        value = custom,
                        onValueChange = { custom = it },
                        singleLine = true
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(if (selected == GenderCustomOption) custom else selected) }) {
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
private fun BiographySelectionOption(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = if (selected) Color(0xFF3A3244) else Color(0xFF1A171D),
        border = BorderStroke(1.dp, if (selected) Color(0x66FFF6EA) else Color(0x30FFFFFF))
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = if (selected) Color(0xFFFFF6EA) else Color(0xFFD2CAC2)
        )
    }
}

@Composable
private fun BiographyHeightDialog(
    currentValue: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var unit by remember(currentValue) { mutableStateOf(detectHeightUnit(currentValue)) }
    var amount by remember(currentValue) { mutableStateOf(parseLeadingNumber(currentValue)?.let(::formatNumber).orEmpty()) }
    val cmLabel = text(HeightUnit.CM.labelKey)
    val ftLabel = text(HeightUnit.FT.labelKey)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text("biography_height")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                UnitSwitcher(
                    first = cmLabel,
                    second = ftLabel,
                    selected = if (unit == HeightUnit.CM) cmLabel else ftLabel,
                    onSelected = { next ->
                        val nextUnit = if (next == cmLabel) HeightUnit.CM else HeightUnit.FT
                        amount = convertHeightAmount(amount, unit, nextUnit)
                        unit = nextUnit
                    }
                )
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSave(formatMeasuredValue(amount, unit.code)) }) {
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
private fun BiographyWeightDialog(
    currentValue: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var unit by remember(currentValue) { mutableStateOf(detectWeightUnit(currentValue)) }
    var amount by remember(currentValue) { mutableStateOf(parseLeadingNumber(currentValue)?.let(::formatNumber).orEmpty()) }
    val lbLabel = text(WeightUnit.LB.labelKey)
    val kgLabel = text(WeightUnit.KG.labelKey)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text("biography_weight")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                UnitSwitcher(
                    first = lbLabel,
                    second = kgLabel,
                    selected = if (unit == WeightUnit.LB) lbLabel else kgLabel,
                    onSelected = { next ->
                        val nextUnit = if (next == lbLabel) WeightUnit.LB else WeightUnit.KG
                        amount = convertWeightAmount(amount, unit, nextUnit)
                        unit = nextUnit
                    }
                )
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSave(formatMeasuredValue(amount, unit.code)) }) {
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
private fun UnitSwitcher(
    first: String,
    second: String,
    selected: String,
    onSelected: (String) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(first, second).forEach { option ->
            Text(
                text = option,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (selected == option) Color(0xFF3A3244) else Color.Transparent)
                    .clickable { onSelected(option) }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFFF7F2EA)
            )
        }
    }
}

private data class BiographyRow(
    val field: BiographyField,
    val icon: ImageVector,
    val label: String,
    val value: String
)

private data class BiographyChoiceOption(
    val value: String,
    val labelKey: String
)

enum class BiographyField(val editor: BiographyEditor) {
    ALIGNMENT(BiographyEditor.ALIGNMENT),
    BACKGROUND(BiographyEditor.TEXT),
    FAITH(BiographyEditor.TEXT),
    HOMELAND(BiographyEditor.TEXT),
    PERSONALITY_TRAITS(BiographyEditor.TEXT),
    IDEALS(BiographyEditor.TEXT),
    BONDS(BiographyEditor.TEXT),
    FLAWS(BiographyEditor.TEXT),
    AGE(BiographyEditor.NUMBER),
    GENDER(BiographyEditor.GENDER),
    HEIGHT(BiographyEditor.HEIGHT),
    WEIGHT(BiographyEditor.WEIGHT),
    EYES(BiographyEditor.TEXT),
    HAIR(BiographyEditor.TEXT),
    SKIN(BiographyEditor.TEXT);

    @Composable
    fun label(): String = when (this) {
        ALIGNMENT -> text("biography_alignment")
        BACKGROUND -> text("biography_background")
        FAITH -> text("biography_faith")
        HOMELAND -> text("biography_homeland")
        PERSONALITY_TRAITS -> text("biography_personality_traits")
        IDEALS -> text("biography_ideals")
        BONDS -> text("biography_bonds")
        FLAWS -> text("biography_flaws")
        AGE -> text("biography_age")
        GENDER -> text("biography_gender")
        HEIGHT -> text("biography_height")
        WEIGHT -> text("biography_weight")
        EYES -> text("biography_eyes")
        HAIR -> text("biography_hair")
        SKIN -> text("biography_skin")
    }

    fun valueFrom(character: Character): String = when (this) {
        ALIGNMENT -> character.alignment
        BACKGROUND -> character.background
        FAITH -> character.faith
        HOMELAND -> character.homeland
        PERSONALITY_TRAITS -> character.personalityTraits
        IDEALS -> character.ideals
        BONDS -> character.bonds
        FLAWS -> character.flaws
        AGE -> character.age
        GENDER -> character.gender
        HEIGHT -> character.height
        WEIGHT -> character.weight
        EYES -> character.eyes
        HAIR -> character.hair
        SKIN -> character.skin
    }
}

enum class BiographyEditor {
    ALIGNMENT,
    TEXT,
    NUMBER,
    GENDER,
    HEIGHT,
    WEIGHT
}

/** [code] is what gets saved in the character ("180 cm"); [labelKey] is how it's shown. */
private enum class HeightUnit(val code: String, val labelKey: String) {
    CM("cm", "biography_unit_cm"),
    FT("ft", "inventory_unit_feet")
}

private enum class WeightUnit(val code: String, val labelKey: String) {
    LB("lb", "inventory_unit_pounds"),
    KG("kg", "biography_unit_kg")
}

private val measuredValueRegex = Regex("""^(-?\d+(?:[.,]\d+)?)\s*(cm|ft|lb|kg)$""", RegexOption.IGNORE_CASE)

/** Shows a saved "180 cm" / "150 lb" value with the unit in the current language; other text as is. */
@Composable
private fun localizedMeasuredValue(value: String): String {
    val match = measuredValueRegex.matchEntire(value.trim()) ?: return value
    val (amount, code) = match.destructured
    val labelKey = HeightUnit.entries.firstOrNull { it.code.equals(code, ignoreCase = true) }?.labelKey
        ?: WeightUnit.entries.firstOrNull { it.code.equals(code, ignoreCase = true) }?.labelKey
        ?: return value
    return "$amount ${text(labelKey)}"
}

@Composable
private fun localizedAlignment(value: String): String {
    return alignmentOptions.firstOrNull { it.value == value }?.let { text(it.labelKey) }
        ?: value.ifBlank { text("common_dash") }
}

private val alignmentOptions = listOf(
    BiographyChoiceOption("Lawful Good", "alignment_lawful_good"),
    BiographyChoiceOption("Neutral Good", "alignment_neutral_good"),
    BiographyChoiceOption("Chaotic Good", "alignment_chaotic_good"),
    BiographyChoiceOption("Lawful Neutral", "alignment_lawful_neutral"),
    BiographyChoiceOption("True Neutral", "alignment_true_neutral"),
    BiographyChoiceOption("Chaotic Neutral", "alignment_chaotic_neutral"),
    BiographyChoiceOption("Lawful Evil", "alignment_lawful_evil"),
    BiographyChoiceOption("Neutral Evil", "alignment_neutral_evil"),
    BiographyChoiceOption("Chaotic Evil", "alignment_chaotic_evil"),
    BiographyChoiceOption("Unaligned", "alignment_unaligned")
)

// Stored as these English codes (like alignment) and shown through localization keys.
private const val GenderCustomOption = "Custom"
private const val GenderMaleOption = "Male"
private const val GenderFemaleOption = "Female"
private val genderOptions = listOf(GenderMaleOption, GenderFemaleOption, GenderCustomOption)

@Composable
private fun localizedGender(value: String): String =
    when (value) {
        GenderMaleOption -> text("biography_gender_male")
        GenderFemaleOption -> text("biography_gender_female")
        GenderCustomOption -> text("biography_gender_custom")
        else -> value
    }

private fun detectHeightUnit(value: String): HeightUnit =
    if (value.contains("ft", ignoreCase = true) || value.contains("'") || value.contains("\"")) HeightUnit.FT else HeightUnit.CM

private fun detectWeightUnit(value: String): WeightUnit =
    if (value.contains("kg", ignoreCase = true)) WeightUnit.KG else WeightUnit.LB

private fun parseLeadingNumber(value: String): Double? =
    Regex("""-?\d+(?:[.,]\d+)?""").find(value)?.value?.replace(',', '.')?.toDoubleOrNull()

private fun convertHeightAmount(value: String, from: HeightUnit, to: HeightUnit): String {
    val amount = value.replace(',', '.').toDoubleOrNull() ?: return value
    if (from == to) return formatNumber(amount)
    val converted = when {
        from == HeightUnit.CM && to == HeightUnit.FT -> amount / 30.48
        from == HeightUnit.FT && to == HeightUnit.CM -> amount * 30.48
        else -> amount
    }
    return formatNumber(converted)
}

private fun convertWeightAmount(value: String, from: WeightUnit, to: WeightUnit): String {
    val amount = value.replace(',', '.').toDoubleOrNull() ?: return value
    if (from == to) return formatNumber(amount)
    val converted = when {
        from == WeightUnit.LB && to == WeightUnit.KG -> amount * 0.45359237
        from == WeightUnit.KG && to == WeightUnit.LB -> amount / 0.45359237
        else -> amount
    }
    return formatNumber(converted)
}

private fun formatMeasuredValue(amount: String, unit: String): String =
    amount.trim().takeIf { it.isNotEmpty() }?.let { value ->
        val numeric = value.replace(',', '.').toDoubleOrNull()
        "${numeric?.let(::formatNumber) ?: value} $unit"
    }.orEmpty()

private fun formatNumber(value: Double): String {
    val rounded = kotlin.math.round(value * 10.0) / 10.0
    return if (rounded % 1.0 == 0.0) rounded.toInt().toString() else rounded.toString()
}
