package com.dndcharacterhandler.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dndcharacterhandler.presentation.CharacterSectionUiState
import com.dndcharacterhandler.presentation.localization.LocalStrings
import com.dndcharacterhandler.presentation.localization.text

@Composable
fun PlaceholderScreen(
    state: CharacterSectionUiState,
    sections: List<Pair<String, String>>
) {
    val character = state.character?.character
    val strings = LocalStrings.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            CharacterHeader(
                name = character?.name?.ifBlank { text("overview_name_placeholder") }
                    ?: text("placeholder_loading_character"),
                subtitle = strings.format(
                    "character_header_subtitle",
                    character?.race?.ifBlank { text("placeholder_race") } ?: text("placeholder_race"),
                    character?.characterClass?.ifBlank { text("placeholder_class") } ?: text("placeholder_class"),
                    character?.subclass?.ifBlank { text("placeholder_subclass") } ?: text("placeholder_subclass"),
                    text("placeholder_level_short"),
                    character?.level ?: "-"
                )
            )
        }
        items(sections) { section ->
            PlaceholderSection(text(section.first), text(section.second))
        }
    }
}
