package com.dndcharacterhandler.presentation.notes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.dndcharacterhandler.domain.model.CharacterBundle
import com.dndcharacterhandler.domain.model.Note
import com.dndcharacterhandler.domain.repository.CharacterRepository
import com.dndcharacterhandler.domain.usecase.GetCharacterBundleUseCase
import com.dndcharacterhandler.presentation.BaseCharacterViewModel
import com.dndcharacterhandler.presentation.SelectedCharacterHolder
import com.dndcharacterhandler.presentation.components.EditDialog
import com.dndcharacterhandler.presentation.localization.text
import com.dndcharacterhandler.presentation.theme.LocalDesignTokens
import kotlinx.coroutines.launch

class NotesViewModel(
    private val characterRepository: CharacterRepository,
    getCharacterBundleUseCase: GetCharacterBundleUseCase,
    selectedCharacterHolder: SelectedCharacterHolder
) : BaseCharacterViewModel(getCharacterBundleUseCase, selectedCharacterHolder) {
    fun updateNote(characterBundle: CharacterBundle, note: Note) {
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
            characterRepository.upsertNote(
                characterId = characterBundle.character.id,
                note = noteToSave
            )
        }
    }

    fun togglePinned(characterBundle: CharacterBundle, note: Note) {
        updateNote(characterBundle, note.copy(isPinned = !note.isPinned, updatedDate = System.currentTimeMillis()))
    }
}

/**
 * The notes as a section of another screen's list: the biography's, below its own sections (owner's
 * choice, 2026-10-04: the notes screen merged into the biography). [content] lays the list out, puts the
 * section's items where they go and gives the screen's "+" the way to start a note (null until the
 * character loads); the note's editor is the section's own. Until the character loads there are no items.
 */
@Composable
fun NotesSection(
    viewModel: NotesViewModel,
    content: @Composable (items: LazyListScope.() -> Unit, onAddNote: (() -> Unit)?) -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val bundle = state.character
    var editingNote by remember { mutableStateOf<Note?>(null) }
    val items = if (bundle == null) {
        NoItems
    } else {
        notesSectionItems(
            characterBundle = bundle,
            onOpenNote = { editingNote = it },
            onTogglePinned = viewModel::togglePinned
        )
    }
    val onAddNote: (() -> Unit)? = bundle?.let { { editingNote = newDraftNote() } }
    content(items, onAddNote)

    val note = editingNote
    if (note != null && bundle != null) {
        NoteEditDialog(
            note = note,
            onDismiss = { editingNote = null },
            onSave = { updated ->
                viewModel.updateNote(bundle, updated.copy(updatedDate = System.currentTimeMillis()))
                editingNote = null
            }
        )
    }
}

private val NoItems: LazyListScope.() -> Unit = {}

/** What every key of the section's items starts with: unique in the list it shares. */
private const val NotesKeyPrefix = "notes:"

/**
 * The section's list items: its title, the search, the notes — the pinned first, then the latest — or a
 * quiet "none yet" / "none found". A note is started by the screen's "+", as on every screen (owner's
 * choice, 2026-10-06). They space themselves, as the biography's list has no spacing of its own.
 */
@Composable
internal fun notesSectionItems(
    characterBundle: CharacterBundle,
    onOpenNote: (Note) -> Unit = {},
    onTogglePinned: (CharacterBundle, Note) -> Unit = { _, _ -> }
): LazyListScope.() -> Unit {
    var query by remember { mutableStateOf("") }
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
        items(visibleNotes, key = { "$NotesKeyPrefix${it.id}" }) { note ->
            NoteCard(
                note = note,
                onClick = { onOpenNote(note) },
                onTogglePinned = { onTogglePinned(characterBundle, note) },
                modifier = Modifier.padding(top = 10.dp)
            )
        }
        if (visibleNotes.isEmpty()) {
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
        color = colors.surface.card.copy(alpha = 0.62f),
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
                    color = colors.text.muted.copy(alpha = 0.72f)
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

@Composable
private fun NoteCard(
    note: Note,
    onClick: () -> Unit,
    onTogglePinned: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalDesignTokens.current.colors
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = colors.surface.card.copy(alpha = 0.62f),
        border = BorderStroke(1.dp, colors.border.muted)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (note.isPinned) {
                Icon(
                    imageVector = Icons.Outlined.PushPin,
                    contentDescription = text("notes_pin"),
                    tint = colors.text.primary,
                    modifier = Modifier
                        .padding(end = 12.dp)
                        .size(28.dp)
                        .clickable(onClick = onTogglePinned)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = note.title.ifBlank { text("notes_untitled") },
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.text.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (note.content.isNotBlank()) {
                    Text(
                        text = note.content,
                        modifier = Modifier.padding(top = 8.dp),
                        style = MaterialTheme.typography.bodyLarge,
                        color = colors.text.muted,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = colors.text.muted,
                modifier = Modifier.padding(start = 10.dp)
            )
        }
    }
}

@Composable
private fun NoteEditDialog(
    note: Note,
    onDismiss: () -> Unit,
    onSave: (Note) -> Unit
) {
    var title by remember(note) { mutableStateOf(note.title) }
    var content by remember(note) { mutableStateOf(note.content) }
    var isPinned by remember(note) { mutableStateOf(note.isPinned) }

    EditDialog(
        // A note not saved yet is a new one.
        title = text(if (note.id == 0L) "notes_new_note" else "notes_edit_note"),
        onDismiss = onDismiss,
        onConfirm = {
            onSave(
                note.copy(
                    title = title.trim(),
                    content = content.trim(),
                    isPinned = isPinned
                )
            )
        }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text(text("notes_title")) },
                singleLine = true
            )
            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                label = { Text(text("notes_content")) },
                minLines = 4
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = isPinned,
                    onCheckedChange = { isPinned = it }
                )
                Text(
                    text = text("notes_pinned"),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
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
