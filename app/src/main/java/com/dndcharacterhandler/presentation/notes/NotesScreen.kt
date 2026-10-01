package com.dndcharacterhandler.presentation.notes

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import com.dndcharacterhandler.presentation.components.CharacterScreenHeader
import com.dndcharacterhandler.presentation.components.FloatingAddButton
import com.dndcharacterhandler.presentation.components.LocalFloatingButtonsInset
import com.dndcharacterhandler.presentation.components.ScreenBackground
import com.dndcharacterhandler.presentation.components.ScreenTopActions
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

@Composable
fun NotesScreen(
    viewModel: NotesViewModel,
    onOpenDrawer: () -> Unit,
    onOpenDice: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    NotesContent(
        characterBundle = state.character,
        onOpenDrawer = onOpenDrawer,
        onOpenDice = onOpenDice,
        onUpdateNote = viewModel::updateNote,
        onTogglePinned = viewModel::togglePinned
    )
}

@Composable
internal fun NotesContent(
    characterBundle: CharacterBundle?,
    onOpenDrawer: () -> Unit = {},
    onOpenDice: () -> Unit = {},
    onUpdateNote: (CharacterBundle, Note) -> Unit = { _, _ -> },
    onTogglePinned: (CharacterBundle, Note) -> Unit = { _, _ -> }
) {
    val character = characterBundle?.character
    var query by remember { mutableStateOf("") }
    var editingNote by remember { mutableStateOf<Note?>(null) }

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

    val resolvedBundle = characterBundle
    val visibleNotes = remember(resolvedBundle.notes, query) {
        resolvedBundle.notes
            .filter { note ->
                val needle = query.trim()
                needle.isBlank() ||
                    note.title.contains(needle, ignoreCase = true) ||
                    note.content.contains(needle, ignoreCase = true)
            }
            .sortedWith(compareByDescending<Note> { it.isPinned }.thenByDescending { maxOf(it.updatedDate, it.createdDate) })
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
                        onOpenDice = onOpenDice
                    )
                }

                item {
                    NotesSearchField(
                        value = query,
                        onValueChange = { query = it }
                    )
                }

                item {
                    NotesSectionTitle(text("nav_notes"))
                }

                items(visibleNotes, key = { it.id }) { note ->
                    NoteCard(
                        note = note,
                        onClick = { editingNote = note },
                        onTogglePinned = { onTogglePinned(resolvedBundle, note) }
                    )
                }
            }
            FloatingAddButton(
                onClick = { editingNote = newDraftNote() },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 24.dp, bottom = 15.dp)
            )
        }
    }

    editingNote?.let { note ->
        NoteEditDialog(
            note = note,
            onDismiss = { editingNote = null },
            onSave = { updated ->
                onUpdateNote(resolvedBundle, updated.copy(updatedDate = System.currentTimeMillis()))
                editingNote = null
            }
        )
    }
}

@Composable
private fun NotesSearchField(
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
                    text = text("notes_search_placeholder"),
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
private fun NotesSectionTitle(title: String) {
    val tokens = LocalDesignTokens.current.typography
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp),
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
            androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                drawLine(
                    color = Color(0x33FFFFFF),
                    start = androidx.compose.ui.geometry.Offset(0f, size.height / 2f),
                    end = androidx.compose.ui.geometry.Offset(size.width, size.height / 2f),
                    strokeWidth = 1.dp.toPx()
                )
            }
        }
    }
}

@Composable
private fun NoteCard(
    note: Note,
    onClick: () -> Unit,
    onTogglePinned: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF17141B).copy(alpha = 0.62f),
        border = BorderStroke(1.dp, Color(0x36FFFFFF))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (note.isPinned) {
                Icon(
                    imageVector = Icons.Outlined.PushPin,
                    contentDescription = text("notes_pin"),
                    tint = Color(0xFFF7F2EA),
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
                    color = Color(0xFFF7F2EA),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (note.content.isNotBlank()) {
                    Text(
                        text = note.content,
                        modifier = Modifier.padding(top = 8.dp),
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color(0xFFD2CAC2),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = Color(0xFFD2CAC2),
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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text("notes_edit_note")) },
        text = {
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
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        note.copy(
                            title = title.trim(),
                            content = content.trim(),
                            isPinned = isPinned
                        )
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
