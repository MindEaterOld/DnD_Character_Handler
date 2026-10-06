package com.dndcharacterhandler.presentation.biography

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.tooling.preview.Preview
import com.dndcharacterhandler.data.localization.LocalizedStrings
import com.dndcharacterhandler.domain.model.AppLanguage
import com.dndcharacterhandler.domain.model.CharacterBundle
import com.dndcharacterhandler.presentation.attributes.previewFallbackCharacter
import com.dndcharacterhandler.presentation.localization.LocalStrings
import com.dndcharacterhandler.presentation.theme.DnDTheme

@Preview(
    name = "Biography Screen",
    showBackground = true,
    showSystemUi = true,
    device = "spec:width=412dp,height=915dp"
)
@Composable
fun BiographyScreenPreview() {
    val strings = LocalizedStrings(
        language = AppLanguage.ENGLISH,
        values = mapOf(
            "drawer_open_character_manager" to "Open character manager",
            "dice_open" to "Roll dice",
            "placeholder_loading_character" to "Loading character",
            "common_dash" to "—",
            "biography_identity" to "Character Identity",
            "biography_appearance" to "Appearance",
            "biography_character_history" to "Character History",
            "biography_alignment" to "Alignment",
            "biography_background" to "Background",
            "biography_faith" to "Faith",
            "biography_homeland" to "Homeland",
            "size_label" to "Size",
            "size_medium" to "Medium",
            "biography_age" to "Age",
            "biography_gender" to "Gender",
            "biography_gender_male" to "Male",
            "inventory_unit_pounds" to "lb",
            "biography_height" to "Height",
            "biography_weight" to "Weight",
            "biography_eyes" to "Eyes",
            "biography_hair" to "Hair",
            "biography_skin" to "Skin",
            "biography_personality_traits" to "Personality Traits",
            "biography_ideals" to "Ideals",
            "biography_bonds" to "Bonds",
            "biography_flaws" to "Flaws"
        )
    )
    val character = previewFallbackCharacter().copy(
        alignment = "Neutral Good",
        background = "Sage",
        faith = "Mystra",
        homeland = "Waterdeep",
        age = "27",
        gender = "Male",
        height = "5' 11\"",
        weight = "165 lb",
        eyes = "Blue",
        hair = "Black",
        skin = "Fair",
        personalityTraits = "I am constantly collecting forgotten knowledge.",
        ideals = "Knowledge should be shared freely.",
        bonds = "My former mentor disappeared under mysterious circumstances.",
        flaws = "I often underestimate danger when pursuing knowledge.",
        biography = """
            Alaric was born in a remote village near the High Forest. From an early age he displayed an unusual fascination with forgotten lore and arcane mysteries.

            His talent was recognized by a traveling sage of the Silverlight Enclave, who took him under their wing and introduced him to the ways of magic.

            Years later, his mentor vanished while searching for a lost divination artifact. Alaric set out to uncover the truth, believing that knowledge holds the key.
        """.trimIndent()
    )

    CompositionLocalProvider(LocalStrings provides strings) {
        DnDTheme {
            BiographyContent(
                characterBundle = CharacterBundle(
                    character = character,
                    skills = emptyList(),
                    attacks = emptyList(),
                    combatResources = emptyList(),
                    inventoryItems = emptyList(),
                    spells = emptyList(),
                    features = emptyList(),
                    notes = emptyList()
                ),
                onOpenDrawer = {},
                onOpenDice = {}
            )
        }
    }
}
