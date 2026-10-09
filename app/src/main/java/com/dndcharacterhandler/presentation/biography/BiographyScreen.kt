package com.dndcharacterhandler.presentation.biography

import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.selection.selectable
import com.dndcharacterhandler.presentation.components.CharacterLoadingScreen
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.dndcharacterhandler.presentation.components.rememberHeaderBackdrop
import com.dndcharacterhandler.presentation.components.headerBackdrop
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import com.dndcharacterhandler.presentation.components.FloatingAddButton
import com.dndcharacterhandler.presentation.components.FloatingAddButtonBottom
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.dndcharacterhandler.domain.model.Character
import com.dndcharacterhandler.domain.model.CharacterBundle
import com.dndcharacterhandler.domain.model.CreatureSize
import com.dndcharacterhandler.domain.model.CharacterTextField
import com.dndcharacterhandler.domain.repository.CharacterRepository
import com.dndcharacterhandler.domain.usecase.GetCharacterBundleUseCase
import com.dndcharacterhandler.presentation.BaseCharacterViewModel
import com.dndcharacterhandler.presentation.SelectedCharacterHolder
import com.dndcharacterhandler.presentation.components.CharacterHeaderInset
import com.dndcharacterhandler.presentation.components.PinnedCharacterHeader
import com.dndcharacterhandler.presentation.components.LocalFloatingButtonsInset
import com.dndcharacterhandler.presentation.components.OutlinedPanel
import com.dndcharacterhandler.presentation.components.SheetLabel
import com.dndcharacterhandler.presentation.components.SheetOrnament
import com.dndcharacterhandler.presentation.components.ScreenBackground
import com.dndcharacterhandler.presentation.components.figure
import com.dndcharacterhandler.presentation.components.labelKey
import com.dndcharacterhandler.presentation.components.toggleContent
import com.dndcharacterhandler.presentation.components.toggleFill
import com.dndcharacterhandler.presentation.localization.text
import com.dndcharacterhandler.presentation.notes.NotesNewNoteItem
import com.dndcharacterhandler.presentation.notes.NotesSection
import com.dndcharacterhandler.presentation.notes.NotesViewModel
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

    fun updateSize(characterBundle: CharacterBundle, size: CreatureSize) {
        if (characterBundle.character.size == size) return
        viewModelScope.launch {
            characterRepository.updateSize(characterBundle.character.id, size)
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

/**
 * The biography: identity, appearance and history, then — further down the same list — the notes, the
 * screen that used to be their own tab (owner's choice, 2026-10-04).
 */
@Composable
fun BiographyScreen(
    viewModel: BiographyViewModel,
    notesViewModel: NotesViewModel,
    onOpenDrawer: () -> Unit,
    onOpenDice: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    NotesSection(viewModel = notesViewModel) { notesItems, onAddNote ->
        BiographyContent(
            characterBundle = state.character,
            onOpenDrawer = onOpenDrawer,
            onOpenDice = onOpenDice,
            onUpdateBiography = viewModel::updateBiography,
            onUpdateField = viewModel::updateBiographyField,
            onUpdateSize = viewModel::updateSize,
            moreItems = notesItems,
            onAddNote = onAddNote
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun BiographyContent(
    characterBundle: CharacterBundle?,
    onOpenDrawer: () -> Unit,
    onOpenDice: () -> Unit,
    onUpdateBiography: (CharacterBundle, String) -> Unit = { _, _ -> },
    onUpdateField: (CharacterBundle, BiographyField, String) -> Unit = { _, _, _ -> },
    onUpdateSize: (CharacterBundle, CreatureSize) -> Unit = { _, _ -> },
    /** The list's items after the biography's own: the notes section. */
    moreItems: LazyListScope.() -> Unit = {},
    /** The screen's "+", which starts a note (owner's choice, 2026-10-06); null hides it. */
    onAddNote: (() -> Unit)? = null
) {
    val colors = LocalDesignTokens.current.colors
    val character = characterBundle?.character
    var editingField by remember { mutableStateOf<BiographyField?>(null) }
    fun row(field: BiographyField, icon: ImageVector, label: String, value: String, dot: Color? = null) =
        BiographyRow(icon, label, value, dot) { editingField = field }

    // A colour picked from the swatches: its name in the language in use and a dot of it; the player's own word as is.
    @Composable
    fun swatchRow(field: BiographyField, group: SwatchGroup, icon: ImageVector, value: String): BiographyRow {
        val swatch = swatchOf(group, value)
        return row(field, icon, field.label(), swatch?.let { swatchName(group, it) } ?: value, swatch?.color)
    }
    if (character == null) {
        CharacterLoadingScreen(onOpenDrawer = onOpenDrawer, onOpenDice = onOpenDice)
        return
    }
    val resolvedCharacter = character
    val resolvedBundle = characterBundle
    val focusManager = LocalFocusManager.current
    val listState = rememberLazyListState()
    val backdrop = rememberHeaderBackdrop(listState)
    val scope = rememberCoroutineScope()
    ScreenBackground {
        Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .headerBackdrop(backdrop)
                // A tap that nothing else takes — off the texts, the buttons and the cells — ends the typing, and
                // what was typed is saved as the field is left (owner's wish, 2026-10-07).
                .pointerInput(Unit) { detectTapGestures(onTap = { focusManager.clearFocus() }) },
            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = CharacterHeaderInset, bottom = LocalFloatingButtonsInset.current),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            item {
                BiographyPersonaSection(
                    character = resolvedCharacter,
                    onEditAlignment = { editingField = BiographyField.ALIGNMENT },
                    onUpdateField = { field, value -> onUpdateField(resolvedBundle, field, value) },
                    // Two to a line: the size, height, age and weight down the left, the gender, eyes, hair and skin
                    // down the right (owner's choice, 2026-10-06). The size and the height open one pop-up; the size's
                    // and the gender's icons change with their value.
                    appearance = listOf(
                        BiographyRow(
                            resolvedCharacter.size.figure,
                            text("size_label"),
                            text(resolvedCharacter.size.labelKey)
                        ) { editingField = BiographyField.HEIGHT },
                        row(BiographyField.GENDER, genderIcon(resolvedCharacter.gender), text("biography_gender"), localizedGender(resolvedCharacter.gender)),
                        row(BiographyField.HEIGHT, BiographyIconHeight, text("biography_height"), localizedMeasuredValue(resolvedCharacter.height)),
                        swatchRow(BiographyField.EYES, SwatchGroup.EYES, BiographyIconEyes, resolvedCharacter.eyes),
                        row(BiographyField.AGE, BiographyIconAge, text("biography_age"), ageDisplay(resolvedCharacter.age)),
                        swatchRow(BiographyField.HAIR, SwatchGroup.HAIR, BiographyIconHair, resolvedCharacter.hair),
                        // The weight alone: its build is the weight pop-up's guide, not the sheet's (owner's wish, 2026-10-08).
                        row(BiographyField.WEIGHT, BiographyIconWeight, text("biography_weight"), localizedMeasuredValue(resolvedCharacter.weight)),
                        swatchRow(BiographyField.SKIN, SwatchGroup.SKIN, BiographyIconSkin, resolvedCharacter.skin)
                    )
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

            moreItems()
        }
        PinnedCharacterHeader(character = resolvedCharacter, onOpenDrawer = onOpenDrawer, onOpenDice = onOpenDice, backdrop = backdrop)
        // Hidden while the keyboard is up: nothing is started while typing, and it would lie on the note's foot.
        onAddNote?.takeUnless { WindowInsets.isImeVisible }?.let { add ->
            FloatingAddButton(
                onClick = {
                    add()
                    // The new note opens at the top of the notes, after the biography's own two items.
                    scope.launch { listState.animateScrollToItem(BiographyOwnItems + NotesNewNoteItem) }
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 24.dp, bottom = FloatingAddButtonBottom)
            )
        }
        }
    }

    val field = editingField
    if (field == BiographyField.HEIGHT) {
        HeightSizeDialog(
            currentHeight = resolvedCharacter.height,
            currentSize = resolvedCharacter.size,
            onDismiss = { editingField = null },
            onSave = { height, size ->
                onUpdateField(resolvedBundle, BiographyField.HEIGHT, height)
                onUpdateSize(resolvedBundle, size)
                editingField = null
            }
        )
    } else if (field != null) {
        BiographyEditDialog(
            field = field,
            currentValue = field.valueFrom(resolvedCharacter),
            height = resolvedCharacter.height,
            onDismiss = { editingField = null },
            onSave = { value ->
                onUpdateField(resolvedBundle, field, value)
                editingField = null
            }
        )
    }
}

/** The biography's own items before the notes: who the character is and the history. */
private const val BiographyOwnItems = 2

/**
 * The appearance two to a line, no rules: the space parts them (owner's choice from boards, 2026-10-06: S4).
 * A tap on a cell opens its editor.
 */
@Composable
private fun BiographyGrid(rows: List<BiographyRow>) {
    Column(modifier = Modifier.padding(bottom = 4.dp)) {
        rows.chunked(2).forEach { line ->
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                line.forEach { row -> BiographyGridCell(row, Modifier.weight(1f)) }
                if (line.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

/** A cell of the grid: the icon, and the label over the value; an empty value is a quiet dash. */
@Composable
private fun BiographyGridCell(row: BiographyRow, modifier: Modifier) {
    val colors = LocalDesignTokens.current.colors
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = row.onClick)
            .padding(vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Gold, as the labels (owner's choice, 2026-10-06); only the alignment keeps its own colour.
        Icon(
            imageVector = row.icon,
            contentDescription = null,
            modifier = Modifier.size(BiographyIconSize),
            tint = MaterialTheme.colorScheme.primary
        )
        Column(modifier = Modifier.padding(start = PersonaIconGap)) {
            BiographyLabel(row.label)
            Row(verticalAlignment = Alignment.CenterVertically) {
                row.dot?.let { dot ->
                    SwatchDot(dot, SwatchDotSize)
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text(
                    text = row.value.ifBlank { text("common_dash") },
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (row.value.isBlank()) colors.text.subtle else colors.text.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private val SwatchDotSize = 12.dp

/** A field's label: gold capitals, spaced out, as a printed character sheet's (owner's choice from boards, 2026-10-06: S2). */
@Composable
internal fun BiographyLabel(label: String, modifier: Modifier = Modifier) = SheetLabel(label, modifier)

/** A rule with a small diamond in its middle, the ornament of the section titles' lines. */
@Composable
private fun BiographyOrnament() = SheetOrnament()

/** The alignment's seal on the frame's top edge: its icon in its colour inside a ring; a tap opens the cards. */
@Composable
private fun AlignmentSeal(alignment: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalDesignTokens.current.colors
    Box(
        modifier = modifier
            .size(SealSize)
            .clip(CircleShape)
            .background(colors.surface.card)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(SealSize)) {
            drawCircle(
                color = colors.border.miniCard,
                radius = size.minDimension / 2 - 0.5.dp.toPx(),
                style = Stroke(width = 1.dp.toPx())
            )
        }
        AlignmentIcon(alignment, Icons.Outlined.Shield, Modifier.size(SealIconSize))
    }
}

private val SealSize = 56.dp
private val SealIconSize = 30.dp

@Composable
private fun BiographySectionTitle(title: String) {
    val tokens = LocalDesignTokens.current.typography
    val colors = LocalDesignTokens.current.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium.copy(fontSize = tokens.headlineMedium.fontSizeSp.sp),
            color = colors.text.primary
        )
        Canvas(
            modifier = Modifier
                .padding(start = 14.dp)
                .weight(1f)
                .height(18.dp)
        ) {
            val centerY = size.height / 2f
            drawLine(
                color = colors.ornament.stroke,
                start = Offset(0f, centerY),
                end = Offset(size.width - 18.dp.toPx(), centerY),
                strokeWidth = 1.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
    }
}

/**
 * Who the character is (owner's choices from boards, 2026-10-06): the alignment, then the ideals, bonds and flaws,
 * as the character sheet has them (P3), and the appearance two to a line, all in one frame (M1b). Drawn as S4 with
 * S2's labels: the alignment is a seal on the frame's top edge with its name centred under it, ornaments part the
 * three parts, the labels are gold capitals. The alignment opens its cards; the three texts are written right on
 * the sheet with a quiet hint while empty and saved when the field is left, as the history; a cell opens its editor.
 * The sheet no longer shows the background, faith, homeland and traits; their text stays in the character.
 */
@Composable
private fun BiographyPersonaSection(
    character: Character,
    onEditAlignment: () -> Unit,
    onUpdateField: (BiographyField, String) -> Unit,
    appearance: List<BiographyRow>,
    modifier: Modifier = Modifier
) {
    val colors = LocalDesignTokens.current.colors
    val texts = listOf(
        PersonaText(BiographyField.IDEALS, BiographyIconIdeals, text("biography_ideals"), text("biography_ideals_hint"), character.ideals),
        PersonaText(BiographyField.BONDS, BiographyIconBonds, text("biography_bonds"), text("biography_bonds_hint"), character.bonds),
        PersonaText(BiographyField.FLAWS, BiographyIconFlaws, text("biography_flaws"), text("biography_flaws_hint"), character.flaws)
    )
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        BiographySectionTitle(text("biography_identity"))
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedPanel(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = SealSize / 2)
            ) {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 14.dp)
                        .padding(top = SealSize / 2 + 4.dp, bottom = 6.dp)
                ) {
                    // The alignment's name under its seal; none picked: a quiet hint to tap.
                    val none = character.alignment.isBlank()
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable(onClick = onEditAlignment)
                            .padding(vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        BiographyLabel(text("biography_alignment"))
                        Text(
                            text = if (none) text("biography_alignment_hint") else localizedAlignment(character.alignment),
                            style = if (none) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.titleMedium,
                            color = if (none) colors.text.subtle else colors.text.primary,
                            textAlign = TextAlign.Center,
                            maxLines = if (none) 1 else 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    BiographyOrnament()
                    texts.forEach { entry ->
                        // A tap on the label puts the cursor in its text, as a tap on the text does.
                        val focus = remember { FocusRequester() }
                        PersonaEntry(
                            icon = { iconModifier ->
                                Icon(entry.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = iconModifier)
                            },
                            label = entry.label,
                            modifier = Modifier.clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { focus.requestFocus() }
                            )
                        ) {
                            BiographyInlineText(
                                characterId = character.id,
                                value = entry.value,
                                hint = entry.hint,
                                onCommit = { onUpdateField(entry.field, it) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(focus)
                            )
                        }
                    }
                    BiographyOrnament()
                    BiographyGrid(appearance)
                }
            }
            AlignmentSeal(
                alignment = character.alignment,
                onClick = onEditAlignment,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }
}

/** The gender's sign as it is picked: male, female, or a DNA helix for any other or none (owner's choice, 2026-10-06). */
private fun genderIcon(value: String): ImageVector = when (value) {
    GenderMaleOption -> BiographyIconMale
    GenderFemaleOption -> BiographyIconFemale
    else -> BiographyIconGenderOther
}

private class PersonaText(
    val field: BiographyField,
    val icon: ImageVector,
    val label: String,
    val hint: String,
    val value: String
)

// Every icon of the block is 24dp (owner's choice from boards, 2026-10-06); in the persona it stands at the middle
// of the label and the first line under it, 16 + 24.
private val BiographyIconSize = 24.dp
private val PersonaIconTop = 8.dp
private val PersonaIconGap = 12.dp

/** An entry of the persona: its icon beside the label over the content, at the middle of their first two lines. */
@Composable
private fun PersonaEntry(
    icon: @Composable (Modifier) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        icon(
            Modifier
                .padding(top = PersonaIconTop)
                .size(BiographyIconSize)
        )
        Column(modifier = Modifier.padding(start = PersonaIconGap)) {
            BiographyLabel(label)
            content()
        }
    }
}

/**
 * A text written right on the sheet, no frame of its own: a draft while typing, saved once the field is left (or
 * the screen goes), as the history — not a write per keystroke. Keyed on the character, so two characters with
 * the same text never share a draft; [hint] stands in quiet grey while it is empty.
 */
@Composable
private fun BiographyInlineText(
    characterId: Long,
    value: String,
    hint: String,
    onCommit: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalDesignTokens.current.colors
    val draftState = remember(characterId, value) { mutableStateOf(value) }
    var draft by draftState
    var wasFocused by remember { mutableStateOf(false) }
    DisposableEffect(draftState) {
        val saveForThisCharacter = onCommit
        onDispose { saveForThisCharacter(draftState.value) }
    }
    // Saved as the app goes to the background too: a killed process loses nothing typed.
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val commitLatest by rememberUpdatedState(onCommit)
    DisposableEffect(lifecycle, draftState) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP && draftState.value != value) commitLatest(draftState.value)
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    val style = MaterialTheme.typography.bodyLarge
    BasicTextField(
        value = draft,
        onValueChange = { draft = it },
        modifier = modifier.onFocusChanged { focusState ->
            if (wasFocused && !focusState.isFocused) {
                onCommit(draft)
            }
            wasFocused = focusState.isFocused
        },
        textStyle = style.copy(color = colors.text.primary),
        cursorBrush = SolidColor(colors.text.warmPrimary),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        decorationBox = { field ->
            Box {
                if (draft.isEmpty()) {
                    // One line in every language (their maxChars); cut, never wrapped, if a font runs wide.
                    Text(text = hint, style = style, color = colors.text.subtle, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                field()
            }
        }
    )
}

@Composable
private fun BiographyHistorySection(
    characterId: Long,
    history: String,
    onHistoryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalDesignTokens.current.colors
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
            color = colors.surface.card.copy(alpha = 0.62f),
            border = BorderStroke(1.dp, colors.border.muted)
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
                        color = colors.text.muted.copy(alpha = 0.48f)
                    )
                },
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.text.muted),
                minLines = 4,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                shape = RoundedCornerShape(10.dp),
                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    cursorColor = colors.text.warmPrimary
                )
            )
        }
    }
}

@Composable
private fun BiographyEditDialog(
    field: BiographyField,
    currentValue: String,
    /** The height, for the weight's build. */
    height: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    when (field.editor) {
        BiographyEditor.ALIGNMENT -> AlignmentDialog(
            currentValue = currentValue,
            onDismiss = onDismiss,
            onSelect = onSave
        )

        BiographyEditor.GENDER -> GenderDialog(
            currentValue = currentValue,
            onDismiss = onDismiss,
            onSave = onSave
        )

        // The height opens HeightSizeDialog with the size instead (see BiographyContent).
        BiographyEditor.HEIGHT -> Unit

        BiographyEditor.WEIGHT -> WeightDialog(
            currentValue = currentValue,
            height = height,
            onDismiss = onDismiss,
            onSave = onSave
        )

        BiographyEditor.AGE -> AgeDialog(
            currentValue = currentValue,
            onDismiss = onDismiss,
            onSave = onSave
        )

        BiographyEditor.SWATCH -> SwatchDialog(
            title = field.label(),
            group = when (field) {
                BiographyField.EYES -> SwatchGroup.EYES
                BiographyField.HAIR -> SwatchGroup.HAIR
                else -> SwatchGroup.SKIN
            },
            currentValue = currentValue,
            onDismiss = onDismiss,
            onSave = onSave
        )

        // The story's fields are written in place (BiographyInlineText): no pop-up.
        BiographyEditor.TEXT -> Unit
    }
}

@Composable
internal fun UnitSwitcher(
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
                    .background(toggleFill(selected == option))
                    .selectable(selected = selected == option, role = Role.RadioButton) { onSelected(option) }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                style = MaterialTheme.typography.bodyLarge,
                color = toggleContent(selected == option)
            )
        }
    }
}

private data class BiographyRow(
    val icon: ImageVector,
    val label: String,
    val value: String,
    /** The colour the value names, a dot before it: the eyes', the hair's, the skin's swatch. */
    val dot: Color? = null,
    val onClick: () -> Unit
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
    AGE(BiographyEditor.AGE),
    GENDER(BiographyEditor.GENDER),
    HEIGHT(BiographyEditor.HEIGHT),
    WEIGHT(BiographyEditor.WEIGHT),
    EYES(BiographyEditor.SWATCH),
    HAIR(BiographyEditor.SWATCH),
    SKIN(BiographyEditor.SWATCH);

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
    AGE,
    GENDER,
    HEIGHT,
    WEIGHT,
    SWATCH
}

/** [code] is what gets saved in the character ("180 cm"); [labelKey] is how it's shown. */
internal enum class HeightUnit(val code: String, val labelKey: String) {
    CM("cm", "biography_unit_cm"),
    FT("ft", "inventory_unit_feet")
}

internal enum class WeightUnit(val code: String, val labelKey: String) {
    LB("lb", "inventory_unit_pounds"),
    KG("kg", "biography_unit_kg")
}

private val measuredValueRegex = Regex("""^(-?\d+(?:[.,]\d+)?)\s*(cm|ft|lb|kg)$""", RegexOption.IGNORE_CASE)

/** Shows a saved "180 cm" / "150 lb" value with the unit in the current language; other text as is. */
@Composable
internal fun localizedMeasuredValue(value: String): String {
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
internal const val GenderCustomOption = "Custom"
internal const val GenderMaleOption = "Male"
internal const val GenderFemaleOption = "Female"

@Composable
private fun localizedGender(value: String): String =
    when (value) {
        GenderMaleOption -> text("biography_gender_male")
        GenderFemaleOption -> text("biography_gender_female")
        GenderCustomOption -> text("biography_gender_custom")
        else -> value
    }

internal fun detectHeightUnit(value: String): HeightUnit =
    if (value.contains("ft", ignoreCase = true) || value.contains("'") || value.contains("\"")) HeightUnit.FT else HeightUnit.CM

internal fun detectWeightUnit(value: String): WeightUnit =
    if (value.contains("kg", ignoreCase = true)) WeightUnit.KG else WeightUnit.LB

internal fun parseLeadingNumber(value: String): Double? =
    Regex("""-?\d+(?:[.,]\d+)?""").find(value)?.value?.replace(',', '.')?.toDoubleOrNull()

internal fun convertWeightAmount(value: String, from: WeightUnit, to: WeightUnit): String {
    val amount = value.replace(',', '.').toDoubleOrNull() ?: return value
    if (from == to) return formatNumber(amount)
    val converted = when {
        from == WeightUnit.LB && to == WeightUnit.KG -> amount * 0.45359237
        from == WeightUnit.KG && to == WeightUnit.LB -> amount / 0.45359237
        else -> amount
    }
    return formatNumber(converted)
}

internal fun formatMeasuredValue(amount: String, unit: String): String =
    amount.trim().takeIf { it.isNotEmpty() }?.let { value ->
        val numeric = value.replace(',', '.').toDoubleOrNull()
        "${numeric?.let(::formatNumber) ?: value} $unit"
    }.orEmpty()

internal fun formatNumber(value: Double): String {
    val rounded = kotlin.math.round(value * 10.0) / 10.0
    return if (rounded % 1.0 == 0.0) rounded.toInt().toString() else rounded.toString()
}
