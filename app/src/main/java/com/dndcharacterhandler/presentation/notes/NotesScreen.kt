package com.dndcharacterhandler.presentation.notes

import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.dndcharacterhandler.domain.model.AppLanguage
import com.dndcharacterhandler.domain.model.CharacterBundle
import com.dndcharacterhandler.domain.model.Note
import com.dndcharacterhandler.domain.repository.CharacterRepository
import com.dndcharacterhandler.domain.usecase.GetCharacterBundleUseCase
import com.dndcharacterhandler.presentation.BaseCharacterViewModel
import com.dndcharacterhandler.presentation.SelectedCharacterHolder
import com.dndcharacterhandler.presentation.components.LocalAppSnackbar
import com.dndcharacterhandler.presentation.components.OutlinedPanel
import com.dndcharacterhandler.presentation.components.toggleContent
import com.dndcharacterhandler.presentation.components.toggleFill
import com.dndcharacterhandler.presentation.localization.LocalStrings
import com.dndcharacterhandler.presentation.localization.text
import com.dndcharacterhandler.presentation.theme.LocalDesignTokens
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.launch

class NotesViewModel(
    private val characterRepository: CharacterRepository,
    getCharacterBundleUseCase: GetCharacterBundleUseCase,
    selectedCharacterHolder: SelectedCharacterHolder
) : BaseCharacterViewModel(getCharacterBundleUseCase, selectedCharacterHolder) {
    /** Saves [note], a new one (id 0) as made now; [onSaved] gets the id it is kept under. */
    fun updateNote(characterBundle: CharacterBundle, note: Note, onSaved: (Long) -> Unit = {}) {
        val now = System.currentTimeMillis()
        val noteToSave = if (note.id == 0L) {
            note.copy(
                id = 0,
                createdDate = now,
                updatedDate = now
            )
        } else {
            note
        }
        viewModelScope.launch {
            val id = characterRepository.upsertNote(
                characterId = characterBundle.character.id,
                note = noteToSave
            )
            onSaved(id)
        }
    }

    fun deleteNote(characterBundle: CharacterBundle, note: Note) {
        viewModelScope.launch {
            characterRepository.deleteNote(characterId = characterBundle.character.id, noteId = note.id)
        }
    }

    /** Puts a deleted note back as it was, its dates and its pin kept. */
    fun restoreNote(characterBundle: CharacterBundle, note: Note) {
        viewModelScope.launch {
            characterRepository.upsertNote(characterId = characterBundle.character.id, note = note)
        }
    }
}

/**
 * The notes as a section of another screen's list: the biography's, below its own sections (owner's
 * choice, 2026-10-04: the notes screen merged into the biography). [content] lays the list out, puts the
 * section's items where they go and gives the screen's "+" the way to start a note (null until the
 * character loads). Until the character loads there are no items.
 *
 * The notes unfold in place, one at a time, and are written right there (owner's choice from boards, 2026-10-07:
 * N1): a note saves itself once it is left; the "+" opens a new one at the top, which goes if nothing is written in
 * it. A deleted note can be brought back from the notice that says so.
 */
@Composable
fun NotesSection(
    viewModel: NotesViewModel,
    content: @Composable (items: LazyListScope.() -> Unit, onAddNote: (() -> Unit)?) -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val bundle = state.character
    val snackbar = LocalAppSnackbar.current
    val strings = LocalStrings.current
    // Kept per character: switching characters closes what is open, and the card that goes saves to its own character.
    val characterId = bundle?.character?.id
    var expandedId by remember(characterId) { mutableStateOf<Long?>(null) }
    var draft by remember(characterId) { mutableStateOf<Note?>(null) }
    // A new note on its way to the database: it is saved once, whatever else leaves its card meanwhile.
    var savingDraft by remember(characterId) { mutableStateOf(false) }
    val items = if (bundle == null) {
        NoItems
    } else {
        fun save(note: Note) {
            if (note.id != 0L) {
                viewModel.updateNote(bundle, note)
                return
            }
            if (savingDraft) return
            if (note.title.isBlank() && note.content.isBlank()) {
                // Nothing written: the new note goes, unless only its pin was changed while it is still open.
                draft = if (expandedId == NewNoteId) note else null
                return
            }
            draft = note
            savingDraft = true
            viewModel.updateNote(bundle, note) { id ->
                draft = null
                savingDraft = false
                if (expandedId == NewNoteId) expandedId = id
            }
        }
        notesSectionItems(
            characterBundle = bundle,
            draft = draft,
            expandedId = expandedId,
            onExpand = { id -> expandedId = id },
            onSave = ::save,
            onLeaveDraft = { note -> if (note.title.isBlank() && note.content.isBlank() && !savingDraft) draft = null else save(note) },
            onDelete = { note ->
                if (note.id == 0L) {
                    draft = null
                } else {
                    viewModel.deleteNote(bundle, note)
                    snackbar.show(strings["notes_deleted"], strings["common_undo"]) { viewModel.restoreNote(bundle, note) }
                }
                expandedId = null
            }
        )
    }
    val onAddNote: (() -> Unit)? = bundle?.let {
        {
            if (draft == null) draft = newDraftNote()
            expandedId = NewNoteId
        }
    }
    content(items, onAddNote)
}

private val NoItems: LazyListScope.() -> Unit = {}

/** What every key of the section's items starts with: unique in the list it shares. */
private const val NotesKeyPrefix = "notes:"

/** The new note's place among the section's items, after the title and the search: where the "+" scrolls to. */
const val NotesNewNoteItem = 2

/** The id the new note is unfolded under until it is saved. */
private const val NewNoteId = -1L

/**
 * The section's list items: its title, the search, a new note being written, the notes — the pinned first, then
 * the latest — or a quiet "none yet" / "none found". A note is started by the screen's "+", as on every screen
 * (owner's choice, 2026-10-06). They space themselves, as the biography's list has no spacing of its own.
 */
@Composable
internal fun notesSectionItems(
    characterBundle: CharacterBundle,
    draft: Note? = null,
    expandedId: Long? = null,
    onExpand: (Long?) -> Unit = {},
    onSave: (Note) -> Unit = {},
    onLeaveDraft: (Note) -> Unit = {},
    onDelete: (Note) -> Unit = {}
): LazyListScope.() -> Unit {
    var query by remember(characterBundle.character.id) { mutableStateOf("") }
    val visibleNotes = remember(characterBundle.notes, query) {
        characterBundle.notes
            .filter { note ->
                val needle = query.trim()
                needle.isBlank() ||
                    note.title.contains(needle, ignoreCase = true) ||
                    note.content.contains(needle, ignoreCase = true)
            }
            .sortedWith(compareByDescending<Note> { it.isPinned }.thenByDescending { maxOf(it.updatedDate, it.createdDate) })
    }
    return {
        item(key = "${NotesKeyPrefix}title") {
            NotesSectionTitle(
                title = text("nav_notes"),
                modifier = Modifier.padding(top = 14.dp)
            )
        }
        item(key = "${NotesKeyPrefix}search") {
            NotesSearchField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.padding(top = 6.dp)
            )
        }
        if (draft != null) {
            item(key = "${NotesKeyPrefix}new") {
                NoteCard(
                    note = draft,
                    expanded = expandedId == NewNoteId,
                    onToggle = { onExpand(if (expandedId == NewNoteId) null else NewNoteId) },
                    onSave = onSave,
                    onLeave = onLeaveDraft,
                    onDelete = onDelete,
                    focusOnOpen = true,
                    modifier = Modifier.padding(top = 10.dp)
                )
            }
        }
        items(visibleNotes, key = { "$NotesKeyPrefix${it.id}" }) { note ->
            NoteCard(
                note = note,
                expanded = expandedId == note.id,
                onToggle = { onExpand(if (expandedId == note.id) null else note.id) },
                onSave = onSave,
                onLeave = onSave,
                onDelete = onDelete,
                modifier = Modifier.padding(top = 10.dp)
            )
        }
        if (visibleNotes.isEmpty() && draft == null) {
            item(key = "${NotesKeyPrefix}empty") {
                // Plain quiet text, as on the other screens: none at all, or none the search finds.
                Text(
                    text = if (characterBundle.notes.isEmpty()) text("notes_none_yet") else text("notes_search_empty"),
                    modifier = Modifier.padding(start = 2.dp, top = 12.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = LocalDesignTokens.current.colors.text.subtle
                )
            }
        }
    }
}

@Composable
private fun NotesSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalDesignTokens.current.colors
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = colors.surface.card.copy(alpha = 0.7f),
        border = BorderStroke(1.dp, colors.border.muted)
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = null,
                    tint = colors.text.muted,
                    modifier = Modifier.size(28.dp)
                )
            },
            placeholder = {
                Text(
                    text = text("notes_search_placeholder"),
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.text.muted.copy(alpha = 0.7f)
                )
            },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                cursorColor = colors.text.warmPrimary
            )
        )
    }
}

/** The section's title, drawn as the biography's own section titles, which it follows. */
@Composable
private fun NotesSectionTitle(title: String, modifier: Modifier = Modifier) {
    val tokens = LocalDesignTokens.current.typography
    val colors = LocalDesignTokens.current.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 6.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium.copy(fontSize = tokens.headlineMedium.fontSizeSp.sp),
            color = colors.text.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
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
 * A note's card (owner's choice from boards, 2026-10-07: N1). Folded: a gold pin if it is pinned, the title and two
 * lines of the text, an arrow down; a tap unfolds it. Unfolded: the title and the text written right in it, and
 * under them the pin (a toggle), when it was changed and the delete. What is written is saved once the card is left
 * (the focus goes out of it, it folds, or the list lets it go), not on every key.
 */
@Composable
private fun NoteCard(
    note: Note,
    expanded: Boolean,
    onToggle: () -> Unit,
    /** The pin changed: the note with what is written in it. */
    onSave: (Note) -> Unit,
    /** The card left with something changed in it — or a new note left, written or not. */
    onLeave: (Note) -> Unit,
    onDelete: (Note) -> Unit,
    modifier: Modifier = Modifier,
    /** The new note: the cursor goes in its title as it opens. */
    focusOnOpen: Boolean = false
) {
    OutlinedPanel(modifier = modifier.fillMaxWidth()) {
        if (expanded) {
            NoteEditor(note = note, onFold = onToggle, onSave = onSave, onLeave = onLeave, onDelete = onDelete, focusOnOpen = focusOnOpen)
        } else {
            NoteFolded(note = note, onUnfold = onToggle)
        }
    }
}

@Composable
private fun NoteFolded(note: Note, onUnfold: () -> Unit) {
    val colors = LocalDesignTokens.current.colors
    // Laid out as the unfolded card: the pin moves only the title, the text starts at the card's edge either way
    // (owner's wish, 2026-10-08), so nothing jumps as a note folds and unfolds.
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onUnfold)
            .padding(start = 14.dp, end = 8.dp, top = 12.dp, bottom = 12.dp)
    ) {
        Row {
            if (note.isPinned) NotePinMark()
            Text(
                text = note.title.ifBlank { text("notes_untitled") },
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleLarge,
                color = colors.text.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Icon(
                imageVector = Icons.Outlined.KeyboardArrowDown,
                contentDescription = text("notes_expand"),
                tint = colors.text.muted,
                modifier = Modifier.padding(start = 6.dp, top = 4.dp)
            )
        }
        if (note.content.isNotBlank()) {
            Text(
                text = note.content,
                modifier = Modifier.padding(top = 6.dp, end = 6.dp),
                style = MaterialTheme.typography.bodyLarge,
                color = colors.text.muted,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** A pinned note's mark before its title, folded or not: a gold pin on the title's first line. */
@Composable
private fun NotePinMark() {
    Icon(
        imageVector = Icons.Outlined.PushPin,
        contentDescription = text("notes_pin"),
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .padding(top = 4.dp, end = 8.dp)
            .size(20.dp)
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NoteEditor(
    note: Note,
    onFold: () -> Unit,
    onSave: (Note) -> Unit,
    onLeave: (Note) -> Unit,
    onDelete: (Note) -> Unit,
    focusOnOpen: Boolean
) {
    val colors = LocalDesignTokens.current.colors
    var title by remember(note.id) { mutableStateOf(note.title) }
    var content by remember(note.id) { mutableStateOf(note.content) }
    // Once deleted, the card leaving is no reason to save it back.
    var deleted by remember { mutableStateOf(false) }
    var hadFocus by remember { mutableStateOf(false) }
    // What the card last handed over: a new note is saved once, not again as its card goes.
    var handedOver by remember { mutableStateOf<Note?>(null) }
    val latest by rememberUpdatedState(note)
    val save by rememberUpdatedState(onSave)
    val leave by rememberUpdatedState(onLeave)
    fun edited(): Note = latest.copy(title = title.trim(), content = content.trim())
    fun hand(next: Note, to: (Note) -> Unit) {
        handedOver = next
        to(next.copy(updatedDate = System.currentTimeMillis()))
    }
    fun commit() {
        if (deleted) return
        val next = edited()
        val before = handedOver ?: latest
        val unchanged = next.title == before.title && next.content == before.content
        // A new note left blank goes, every time; one written is handed over once.
        val blankNew = next.id == 0L && next.title.isBlank() && next.content.isBlank()
        if (unchanged && !blankNew && (next.id != 0L || handedOver != null)) return
        hand(next, leave)
    }
    DisposableEffect(Unit) {
        onDispose { commit() }
    }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                val next = edited()
                if (next.id != 0L || next.title.isNotBlank() || next.content.isNotBlank()) commit()
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    val titleFocus = remember { FocusRequester() }
    val textFocus = remember { FocusRequester() }
    // A new note opens with the cursor in its title and the whole card above the keyboard, as the keyboard comes up;
    // in one already written the system keeps the line being written in sight.
    val bringIntoView = remember { BringIntoViewRequester() }
    if (focusOnOpen) {
        LaunchedEffect(Unit) { titleFocus.requestFocus() }
        val keyboard = WindowInsets.ime.getBottom(LocalDensity.current)
        LaunchedEffect(hadFocus, keyboard) {
            if (hadFocus) bringIntoView.bringIntoView()
        }
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .bringIntoViewRequester(bringIntoView)
            // A tap on the card's free space writes on in the text, rather than ending the typing as off the card.
            .pointerInput(Unit) { detectTapGestures { textFocus.requestFocus() } }
            .onFocusChanged { focus ->
                if (hadFocus && !focus.hasFocus) commit()
                hadFocus = focus.hasFocus
            }
            .padding(start = 14.dp, end = 8.dp, top = 12.dp, bottom = 10.dp)
    ) {
        Row {
            if (note.isPinned) NotePinMark()
            NoteField(
                value = title,
                onValueChange = { title = it },
                hint = text("notes_title"),
                style = MaterialTheme.typography.titleLarge.copy(color = colors.text.primary),
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(titleFocus)
            )
            Icon(
                imageVector = Icons.Outlined.KeyboardArrowUp,
                contentDescription = text("notes_collapse"),
                tint = colors.text.muted,
                modifier = Modifier
                    .padding(start = 6.dp, top = 4.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onFold)
            )
        }
        NoteField(
            value = content,
            onValueChange = { content = it },
            hint = text("notes_text_hint"),
            style = MaterialTheme.typography.bodyLarge.copy(color = colors.text.muted),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp, end = 6.dp)
                .focusRequester(textFocus)
        )
        Row(
            modifier = Modifier.padding(top = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NoteRoundButton(
                icon = Icons.Outlined.PushPin,
                contentDescription = text(if (note.isPinned) "notes_unpin" else "notes_pin"),
                fill = toggleFill(note.isPinned),
                tint = toggleContent(note.isPinned),
                onClick = { hand(edited().copy(isPinned = !note.isPinned), save) }
            )
            Text(
                text = LocalStrings.current.format("notes_changed", noteDate(note.updatedDate)),
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                style = MaterialTheme.typography.labelMedium,
                color = colors.text.subtle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            NoteRoundButton(
                icon = Icons.Outlined.Delete,
                contentDescription = text("notes_delete"),
                fill = colors.accent.dangerHpZero.copy(alpha = 0.16f),
                tint = colors.accent.dangerHpZero,
                onClick = {
                    deleted = true
                    onDelete(edited())
                }
            )
        }
    }
}

/** A text written right in the card, no frame of its own; [hint] in quiet grey while it is empty. */
@Composable
private fun NoteField(
    value: String,
    onValueChange: (String) -> Unit,
    hint: String,
    style: TextStyle,
    modifier: Modifier = Modifier
) {
    val colors = LocalDesignTokens.current.colors
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        textStyle = style,
        cursorBrush = SolidColor(colors.text.warmPrimary),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        decorationBox = { field ->
            Box {
                if (value.isEmpty()) Text(text = hint, style = style.copy(color = colors.text.subtle), maxLines = 1)
                field()
            }
        }
    )
}

/** A 40dp round button of the card's foot: the pin (a toggle) or the delete. */
@Composable
private fun NoteRoundButton(icon: ImageVector, contentDescription: String, fill: Color, tint: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(fill)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(20.dp))
    }
}

/** A note's date as the language writes a day and a month: "7 окт.", "Oct 7". */
@Composable
private fun noteDate(millis: Long): String {
    val language = LocalStrings.current.language
    val pattern = if (language == AppLanguage.ENGLISH) "MMM d" else "d MMM"
    return DateTimeFormatter.ofPattern(pattern, Locale.forLanguageTag(language.code))
        .format(Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()))
}

private fun newDraftNote(): Note {
    val now = System.currentTimeMillis()
    return Note(
        id = 0,
        title = "",
        createdDate = now,
        updatedDate = now,
        content = "",
        isPinned = false
    )
}
