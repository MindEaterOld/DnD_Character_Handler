package com.dndcharacterhandler.presentation.attributes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dndcharacterhandler.data.localization.LocalizedStrings
import com.dndcharacterhandler.domain.model.AppLanguage
import com.dndcharacterhandler.domain.model.CharacterBundle
import com.dndcharacterhandler.domain.model.Skill
import com.dndcharacterhandler.presentation.components.ScreenBackground
import com.dndcharacterhandler.presentation.components.StatsPanel
import com.dndcharacterhandler.presentation.localization.LocalStrings
import com.dndcharacterhandler.presentation.theme.DnDTheme

@Preview(
    name = "Attributes Section",
    showBackground = true,
    showSystemUi = true,
    device = "spec:width=412dp,height=915dp"
)
@Composable
fun AttributesScreenPreview() {
    val strings = LocalizedStrings(
        language = AppLanguage.ENGLISH,
        values = mapOf(
            "attributes_ability_scores" to "Ability Scores",
            "attributes_skills" to "Skills",
            "attributes_proficiencies" to "Proficiencies",
            "attributes_proficiency_armor" to "Armor",
            "attributes_proficiency_weapons" to "Weapons",
            "attributes_proficiency_tools" to "Tools",
            "attributes_proficiency_languages" to "Languages",
            "attributes_proficiency_masteries" to "Weapon Mastery",
            "attributes_proficiency_masteries_short" to "Mastery",
            "attributes_defenses" to "Defenses & Weaknesses",
            "attributes_defense_resistances_short" to "Resistant",
            "attributes_defense_immunities_short" to "Immune",
            "attributes_defense_vulnerabilities_short" to "Vulnerable",
            "condition_poisoned" to "Poisoned",
            "attributes_weapon_simple_short" to "Simple",
            "attributes_weapon_martial_short" to "Martial",
            "common_none" to "None",
            "attributes_saving_throw_short" to "Save",
            "stat_card_proficiency" to "Proficiency",
            "stat_card_passive_perception" to "Pass. Perc.",
            "stat_card_darkvision" to "Darkvision",
            "ability_strength" to "Strength",
            "ability_dexterity" to "Dexterity",
            "ability_constitution" to "Constitution",
            "ability_intelligence" to "Intelligence",
            "ability_wisdom" to "Wisdom",
            "ability_charisma" to "Charisma",
            "ability_str_short" to "STR",
            "ability_dex_short" to "DEX",
            "ability_con_short" to "CON",
            "ability_int_short" to "INT",
            "ability_wis_short" to "WIS",
            "ability_cha_short" to "CHA",
            "skill_acrobatics" to "Acrobatics",
            "skill_acrobatics_short" to "Acrobatics",
            "skill_animal_handling" to "Animal Handling",
            "skill_animal_handling_short" to "Animal Handl.",
            "skill_arcana" to "Arcana",
            "skill_arcana_short" to "Arcana",
            "skill_athletics" to "Athletics",
            "skill_athletics_short" to "Athletics",
            "skill_deception" to "Deception",
            "skill_deception_short" to "Deception",
            "skill_history" to "History",
            "skill_history_short" to "History",
            "skill_insight" to "Insight",
            "skill_insight_short" to "Insight",
            "skill_intimidation" to "Intimidation",
            "skill_intimidation_short" to "Intimidation",
            "skill_investigation" to "Investigation",
            "skill_investigation_short" to "Investigation",
            "skill_medicine" to "Medicine",
            "skill_medicine_short" to "Medicine",
            "skill_nature" to "Nature",
            "skill_nature_short" to "Nature",
            "skill_perception" to "Perception",
            "skill_perception_short" to "Perception",
            "skill_performance" to "Performance",
            "skill_performance_short" to "Performance",
            "skill_persuasion" to "Persuasion",
            "skill_persuasion_short" to "Persuasion",
            "skill_religion" to "Religion",
            "skill_religion_short" to "Religion",
            "skill_sleight_of_hand" to "Sleight of Hand",
            "skill_sleight_of_hand_short" to "Sleight of H.",
            "skill_stealth" to "Stealth",
            "skill_stealth_short" to "Stealth",
            "skill_survival" to "Survival",
            "skill_survival_short" to "Survival"
        )
    )

    CompositionLocalProvider(LocalStrings provides strings) {
        DnDTheme {
            // The section as the overview lists it, below its own cards.
            val parts = attributesSectionItems(
                characterBundle = CharacterBundle(
                    // A Fighter's proficiencies as the sheet stores them, with three weapon masteries and a dwarf's defenses.
                    character = previewFallbackCharacter().copy(
                        armorProficiencies = "heavy_armor|light_armor|medium_armor|shields",
                        weaponProficiencies = "martial_weapons|simple_weapons",
                        languageProficiencies = "common|custom:Chondathan|elvish",
                        weaponMasteries = "battleaxe|greatsword|longsword",
                        defenses = "ci:poisoned|di:poison|dr:fire|dr:slashing"
                    ),
                    skills = listOf(
                        Skill(name = "skill_arcana", isProficient = true),
                        Skill(name = "skill_history", isProficient = true),
                        Skill(name = "skill_insight", isProficient = true),
                        Skill(name = "skill_investigation", isProficient = true),
                        Skill(name = "skill_persuasion", isProficient = false, hasJackOfAllTrades = true),
                        Skill(name = "skill_perception", isProficient = true, isExpertise = true)
                    ),
                    attacks = emptyList(),
                    combatResources = emptyList(),
                    inventoryItems = emptyList(),
                    spells = emptyList(),
                    features = emptyList(),
                    notes = emptyList()
                ),
                onUpdatePassivePerceptionBonus = { _, _ -> }
            )
            ScreenBackground {
                LazyColumn(
                    contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item { StatsPanel(top = parts.senses) }
                    parts.items(this)
                }
            }
        }
    }
}
