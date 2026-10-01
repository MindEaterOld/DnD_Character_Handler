package com.dndcharacterhandler.presentation.spells

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.tooling.preview.Preview
import com.dndcharacterhandler.data.localization.LocalizedStrings
import com.dndcharacterhandler.domain.model.AppLanguage
import com.dndcharacterhandler.domain.model.CharacterBundle
import com.dndcharacterhandler.domain.model.Spell
import com.dndcharacterhandler.presentation.attributes.previewFallbackCharacter
import com.dndcharacterhandler.presentation.localization.LocalStrings
import com.dndcharacterhandler.presentation.theme.DnDTheme

@Preview(
    name = "Spells Screen",
    showBackground = true,
    showSystemUi = true,
    device = "spec:width=412dp,height=915dp"
)
@Composable
fun SpellsScreenPreview() {
    val strings = LocalizedStrings(
        language = AppLanguage.ENGLISH,
        values = mapOf(
            "drawer_open_character_manager" to "Open character manager",
            "dice_open" to "Roll dice",
            "placeholder_loading_character" to "Loading character",
            "spells_search_placeholder" to "Search Spells",
            "spells_spellcasting_class" to "Spellcasting Class",
            "combat_spell_bonus" to "Spell Bonus",
            "combat_spell_dc" to "Spell DC",
            "combat_spellcasting_ability_title" to "Spellcasting Ability",
            "spells_level_cantrips" to "Cantrips",
            "spells_level_1" to "Level 1",
            "spells_level_2" to "Level 2",
            "spells_level_3" to "Level 3",
            "spells_level_4" to "Level 4",
            "spells_level_5" to "Level 5",
            "spells_level_6" to "Level 6",
            "spells_level_7" to "Level 7",
            "spells_level_8" to "Level 8",
            "spells_level_9" to "Level 9",
            "spells_empty_cantrips" to "No cantrips yet.",
            "spells_empty_level" to "No spells at this level yet.",
            "spells_untitled" to "Untitled Spell",
            "spells_school_abjuration" to "Abjuration",
            "spells_school_conjuration" to "Conjuration",
            "spells_school_divination" to "Divination",
            "spells_school_enchantment" to "Enchantment",
            "spells_school_evocation" to "Evocation",
            "spells_school_illusion" to "Illusion",
            "spells_school_necromancy" to "Necromancy",
            "spells_school_transmutation" to "Transmutation",
            "ability_strength" to "Strength",
            "ability_dexterity" to "Dexterity",
            "ability_constitution" to "Constitution",
            "ability_intelligence" to "Intelligence",
            "ability_wisdom" to "Wisdom",
            "ability_charisma" to "Charisma"
        )
    )

    val character = previewFallbackCharacter().copy(
        characterClass = "Wizard",
        subclass = "Divination",
        level = 7,
        intelligence = 18,
        spellSlotMaximums = "4,3,3,1,0,0,0,0,0",
        spellSlotRemaining = "3,2,1,0,0,0,0,0,0"
    )
    val spells = listOf(
        Spell(id = 1, name = "Fire Bolt", level = 0, school = "Evocation", isPrepared = true, description = ""),
        Spell(id = 2, name = "Mage Hand", level = 0, school = "Conjuration", isPrepared = true, description = ""),
        Spell(id = 3, name = "Shield", level = 1, school = "Abjuration", isPrepared = true, description = ""),
        Spell(id = 4, name = "Magic Missile", level = 1, school = "Evocation", isPrepared = true, description = ""),
        Spell(id = 5, name = "Misty Step", level = 2, school = "Conjuration", isPrepared = true, description = ""),
        Spell(id = 6, name = "Scorching Ray", level = 2, school = "Evocation", isPrepared = false, description = ""),
        Spell(id = 7, name = "Fireball", level = 3, school = "Evocation", isPrepared = true, description = "")
    )

    CompositionLocalProvider(LocalStrings provides strings) {
        DnDTheme {
            SpellsContent(
                characterBundle = CharacterBundle(
                    character = character,
                    skills = emptyList(),
                    attacks = emptyList(),
                    combatResources = emptyList(),
                    inventoryItems = emptyList(),
                    spells = spells,
                    features = emptyList(),
                    notes = emptyList()
                )
            )
        }
    }
}
