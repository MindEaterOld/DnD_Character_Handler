package com.dndcharacterhandler.presentation.features

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.tooling.preview.Preview
import com.dndcharacterhandler.data.localization.LocalizedStrings
import com.dndcharacterhandler.domain.model.AppLanguage
import com.dndcharacterhandler.domain.model.CharacterBundle
import com.dndcharacterhandler.domain.model.Feature
import com.dndcharacterhandler.domain.model.FeatureSource
import com.dndcharacterhandler.presentation.attributes.previewFallbackCharacter
import com.dndcharacterhandler.presentation.localization.LocalStrings
import com.dndcharacterhandler.presentation.theme.DnDTheme

@Preview(
    showBackground = true,
    showSystemUi = true,
    device = "spec:width=412dp,height=915dp"
)
@Composable
fun FeaturesScreenPreview() {
    val strings = LocalizedStrings(
        language = AppLanguage.ENGLISH,
        values = mapOf(
            "drawer_open_character_manager" to "Open character manager",
            "dice_open" to "Roll dice",
            "placeholder_loading_character" to "Loading character",
            "nav_features" to "Features",
            "features_search_placeholder" to "Search Features",
            "features_empty" to "No features yet. Add a class, race or background.",
            "features_catalog_empty" to "No features found.",
            "features_untitled" to "Untitled Feature",
            "features_edit_feature" to "Edit Feature",
            "features_editor_edit" to "Edit",
            "features_name" to "Name",
            "features_description" to "Description",
            "features_level" to "Level",
            "features_source" to "Source",
            "features_source_race" to "Race",
            "features_source_background" to "Background",
            "features_source_class" to "Class",
            "features_source_other" to "Other",
            "inventory_delete_action" to "Delete",
            "common_save" to "Save",
            "common_cancel" to "Cancel",
            "features_level" to "Level",
            "common_edit" to "Edit",
            "features_no_description" to "No description"
        )
    )
    val character = previewFallbackCharacter()
    val features = listOf(
        Feature(
            id = 1,
            name = "Portent",
            description = "Replace attack rolls, saving throws, or ability checks with foreseen d20 rolls.",
            level = 2,
            source = FeatureSource.CLASS,
            category = "Diviner"
        ),
        Feature(
            id = 2,
            name = "Arcane Recovery",
            description = "Recover expended spell slots on a short rest.",
            level = 1,
            source = FeatureSource.CLASS
        ),
        Feature(
            id = 3,
            name = "Ritual Casting",
            description = "Cast ritual spells without expending a spell slot.",
            level = 1,
            source = FeatureSource.CLASS
        ),
        Feature(
            id = 4,
            name = "Darkvision",
            description = "See in darkness within 60 feet as if it were dim light.",
            level = 1,
            source = FeatureSource.RACE
        ),
        Feature(
            id = 5,
            name = "War Caster",
            description = "Gain advantage on Constitution saves to maintain concentration.",
            level = 4,
            source = FeatureSource.OTHER
        ),
        Feature(
            id = 6,
            name = "Researcher",
            description = "You know where to obtain a piece of lore.",
            level = 1,
            source = FeatureSource.BACKGROUND
        )
    )

    CompositionLocalProvider(LocalStrings provides strings) {
        DnDTheme {
            FeaturesContent(
                characterBundle = CharacterBundle(
                    character = character,
                    skills = emptyList(),
                    attacks = emptyList(),
                    combatResources = emptyList(),
                    inventoryItems = emptyList(),
                    spells = emptyList(),
                    features = features,
                    notes = emptyList()
                ),
                // One card unfolded, to show the description and the Edit button.
                initiallyExpanded = setOf(1L)
            )
        }
    }
}
