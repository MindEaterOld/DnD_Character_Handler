package com.dndcharacterhandler.presentation.attributes

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.tooling.preview.Preview
import com.dndcharacterhandler.data.localization.LocalizedStrings
import com.dndcharacterhandler.domain.model.AppLanguage
import com.dndcharacterhandler.domain.model.CharacterBundle
import com.dndcharacterhandler.domain.model.Skill
import com.dndcharacterhandler.presentation.localization.LocalStrings
import com.dndcharacterhandler.presentation.theme.DnDTheme

@Preview(
    name = "Attributes Screen",
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
            "skill_animal_handling" to "Animal Handling",
            "skill_arcana" to "Arcana",
            "skill_athletics" to "Athletics",
            "skill_deception" to "Deception",
            "skill_history" to "History",
            "skill_insight" to "Insight",
            "skill_intimidation" to "Intimidation",
            "skill_investigation" to "Investigation",
            "skill_medicine" to "Medicine",
            "skill_nature" to "Nature",
            "skill_perception" to "Perception",
            "skill_performance" to "Performance",
            "skill_persuasion" to "Persuasion",
            "skill_religion" to "Religion",
            "skill_sleight_of_hand" to "Sleight of Hand",
            "skill_stealth" to "Stealth",
            "skill_survival" to "Survival"
        )
    )

    CompositionLocalProvider(LocalStrings provides strings) {
        DnDTheme {
            AttributesContent(
                characterBundle = CharacterBundle(
                    character = previewFallbackCharacter(),
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
        }
    }
}
