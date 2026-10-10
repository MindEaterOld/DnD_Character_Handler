package com.dndcharacterhandler.presentation.notes

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dndcharacterhandler.data.localization.LocalizedStrings
import com.dndcharacterhandler.domain.model.AppLanguage
import com.dndcharacterhandler.domain.dnd5e.model.CharacterBundle
import com.dndcharacterhandler.domain.dnd5e.model.Note
import com.dndcharacterhandler.presentation.attributes.previewFallbackCharacter
import com.dndcharacterhandler.presentation.components.ScreenBackground
import com.dndcharacterhandler.presentation.localization.LocalStrings
import com.dndcharacterhandler.presentation.theme.DnDTheme

@Preview(
    showBackground = true,
    showSystemUi = true,
    device = "spec:width=412dp,height=915dp"
)
@Composable
fun NotesScreenPreview() {
    val strings = LocalizedStrings(
        language = AppLanguage.ENGLISH,
        values = mapOf(
            "drawer_open_character_manager" to "Open character manager",
            "dice_open" to "Roll dice",
            "placeholder_loading_character" to "Loading character",
            "nav_notes" to "Notes",
            "notes_search_placeholder" to "Search Notes",
            "notes_none_yet" to "No notes yet.",
            "notes_search_empty" to "No notes found.",
            "notes_title" to "Title",
            "notes_text_hint" to "Note text",
            "notes_pin" to "Pin note",
            "notes_unpin" to "Unpin note",
            "notes_untitled" to "Untitled Note",
            "notes_changed" to "Edited %1\$s",
            "notes_delete" to "Delete note",
            "notes_expand" to "Open note",
            "notes_collapse" to "Close note",
            "common_save" to "Save",
            "common_cancel" to "Cancel"
        )
    )
    val character = previewFallbackCharacter()
    val notes = listOf(
        Note(
            id = 1,
            title = "Dragon Weakness",
            createdDate = 1742245200000L,
            updatedDate = 1742245200000L,
            content = "Black scales vulnerable near the neck.",
            isPinned = true
        ),
        Note(
            id = 2,
            title = "Secret Entrance",
            createdDate = 1742158800000L,
            updatedDate = 1742158800000L,
            content = "Hidden tunnel beneath the old mill.",
            isPinned = true
        ),
        Note(
            id = 3,
            title = "The Mayor's Request",
            createdDate = 1742072400000L,
            updatedDate = 1742072400000L,
            content = "The mayor hired us to investigate disappearances in the mines."
        ),
        Note(
            id = 4,
            title = "Temple Symbols",
            createdDate = 1742070600000L,
            updatedDate = 1742070600000L,
            content = "Discovered four strange runes near the altar. The first symbol matched the markings from the mine."
        ),
        Note(
            id = 5,
            title = "Abandoned Camp",
            createdDate = 1741813200000L,
            updatedDate = 1741813200000L,
            content = "Found a destroyed camp along the river. One survivor was taken north."
        )
    )

    CompositionLocalProvider(LocalStrings provides strings) {
        DnDTheme {
            // The section as the biography lists it, below its own sections.
            val items = notesSectionItems(
                characterBundle = CharacterBundle(
                    character = character,
                    skills = emptyList(),
                    attacks = emptyList(),
                    combatResources = emptyList(),
                    inventoryItems = emptyList(),
                    spells = emptyList(),
                    features = emptyList(),
                    notes = notes
                ),
                // The first note unfolded, as it is written in place.
                expandedId = 1
            )
            ScreenBackground {
                LazyColumn(contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 4.dp)) {
                    items()
                }
            }
        }
    }
}
