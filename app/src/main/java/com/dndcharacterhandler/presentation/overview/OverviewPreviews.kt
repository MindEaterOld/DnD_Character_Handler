package com.dndcharacterhandler.presentation.overview

import com.dndcharacterhandler.domain.model.AppTheme
import com.dndcharacterhandler.domain.dnd5e.model.Condition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.outlined.Add
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.dndcharacterhandler.data.localization.LocalizedStrings
import com.dndcharacterhandler.domain.dnd5e.model.ArmorClassMode
import com.dndcharacterhandler.domain.dnd5e.model.Character
import com.dndcharacterhandler.domain.dnd5e.model.CharacterBundle
import com.dndcharacterhandler.domain.model.AppLanguage
import com.dndcharacterhandler.presentation.localization.LocalStrings
import com.dndcharacterhandler.presentation.theme.DnDTheme

@Preview(showBackground = true, showSystemUi = true, device = "spec:width=412dp,height=915dp")
@Composable
private fun OverviewScreenPreview() {
    OverviewPreviewContent(currentHp = 38, temporaryHp = 10)
}

@Preview(name = "Classic overview · Russian", showBackground = true, showSystemUi = true, device = "spec:width=412dp,height=915dp")
@Composable
private fun ClassicOverviewRussianPreview() {
    OverviewPreviewContent(currentHp = 8, temporaryHp = 0, russian = true)
}

/** At 0 hit points, the death saving throws' tray open: one success, two failures. */
@Preview(showBackground = true, showSystemUi = true, device = "spec:width=412dp,height=915dp")
@Composable
private fun OverviewDyingPreview() {
    OverviewPreviewContent(currentHp = 0, temporaryHp = 0, deathSaveSuccesses = 1, deathSaveFailures = 2)
}

/** Three failed death saves: dead, the portrait in black and white. */
@Preview(showBackground = true, showSystemUi = true, device = "spec:width=412dp,height=915dp")
@Composable
private fun OverviewDeadPreview() {
    OverviewPreviewContent(currentHp = 0, temporaryHp = 0, deathSaveSuccesses = 1, deathSaveFailures = 3)
}

@Composable
private fun OverviewPreviewContent(
    currentHp: Int,
    temporaryHp: Int,
    deathSaveSuccesses: Int = 0,
    deathSaveFailures: Int = 0,
    russian: Boolean = false,
    theme: AppTheme = AppTheme.DEFAULT
) {
    val previewStrings = LocalizedStrings(
        language = AppLanguage.ENGLISH,
        values = mapOf(
            "overview_name_placeholder" to "Character Name",
            "placeholder_race" to "Human",
            "placeholder_class" to "Wizard",
            "overview_subtitle_format" to "%1\$s • %2\$s • Level %3\$s",
            "overview_level_format" to "Level %1\$s",
            "overview_short_rest" to "Short Rest",
            "overview_long_rest" to "Long Rest",
            "overview_long_rest_ends" to "Ends",
            "overview_long_rest_dice_hint" to "The spent hit dice come back.",
            "overview_long_rest_concentration" to "Concentration: %1\$s",
            "overview_long_rest_temporary" to "Temporary hit points",
            "overview_long_rest_all_full" to "Hit points, hit dice and abilities are already full.",
            "overview_long_rest_confirm_button" to "Rest",
            "overview_inspiration" to "Inspiration",
            "levelup_badge" to "Level UP",
            "overview_level_short" to "lvl",
            "overview_hp" to "HP",
            "conditions_add_chip" to "Condition",
            "overview_xp_coin" to "XP",
            "overview_add_experience" to "Add experience",
            "overview_death_saves" to "Death saves",
            "overview_death_saves_successes" to "Successes",
            "overview_death_saves_failures" to "Failures",
            "overview_death_saves_roll" to "Roll a death save",
            "overview_death_saves_stable" to "Stable",
            "overview_death_saves_dead" to "Dead",
            "stat_card_armor_class" to "Armor Class",
            "overview_initiative" to "Initiative",
            "overview_speed" to "Speed",
            "inventory_unit_feet" to "ft",
            "dice_open" to "Roll dice",
            "overview_rename_title" to "Rename Character",
            "overview_edit_race_title" to "Edit Race",
            "overview_edit_class_title" to "Edit Class",
            "overview_level_picker_title" to "Select Level",
            "overview_exp_dialog_title" to "Edit Experience",
            "overview_exp_add" to "Add",
            "overview_exp_set" to "Set",
            "overview_exp_current" to "Current EXP: %1\$s",
            "overview_exp_result" to "Result: %1\$s EXP",
            "overview_hp_max_dialog_title" to "Edit Max HP",
            "overview_hp_max" to "Max HP",
            "overview_hit_dice" to "Hit Point Dice",
            "overview_rest_restores" to "Restores",
            "overview_short_rest_roll" to "Roll %1\$s",
            "overview_short_rest_confirm" to "Rest",
            "overview_ac_full" to "Armor Class",
            "overview_ac_base" to "Base AC",
            "overview_ac_manual" to "Manual AC",
            "overview_ac_mode" to "Armor class mode",
            "overview_ac_mode_automatic" to "Automatic",
            "overview_ac_mode_automatic_hint" to "Base AC, equipped armor, shield, and Dexterity are combined automatically.",
            "overview_ac_mode_manual" to "Manual",
            "overview_ac_mode_manual_hint" to "Enter the armor class value yourself and stop automatic recalculation.",
            "overview_edit_ac_title" to "Edit Armor Class",
            "overview_edit_initiative_title" to "Edit Initiative",
            "overview_initiative_bonus" to "Initiative bonus",
            "overview_initiative_base_value" to "Dexterity modifier: %1\$s",
            "overview_initiative_result" to "Result: %1\$s",
            "overview_edit_speed_title" to "Edit Speed",
            "overview_portrait_menu_title" to "Portrait",
            "overview_portrait_show" to "View portrait",
            "overview_portrait_change" to "Change portrait",
            "common_save" to "Save",
            "common_cancel" to "Cancel",
            "common_close" to "Close",
            "drawer_open_character_manager" to "Open character manager"
        )
    )
    val previewCharacter = Character(
        id = 1,
        name = "Alaric Stormwind",
        race = "Human",
        characterClass = "Wizard",
        subclass = "Divination",
        level = 7,
        portraitUri = null,
        currentHp = currentHp,
        maxHp = 42,
        temporaryHp = temporaryHp,
        deathSaveSuccesses = deathSaveSuccesses,
        deathSaveFailures = deathSaveFailures,
        hitDieSides = 8,
        spentHitDice = 0,
        hasInspiration = true,
        armorClass = 15,
        baseArmorClass = 10,
        armorClassMode = ArmorClassMode.AUTOMATIC,
        speed = 30,
        initiative = 3,
        initiativeBonus = 0,
        experience = 36000,
        strength = 8,
        dexterity = 16,
        constitution = 14,
        intelligence = 18,
        wisdom = 14,
        charisma = 12,
        strengthSaveProficient = false,
        dexteritySaveProficient = false,
        constitutionSaveProficient = false,
        intelligenceSaveProficient = true,
        wisdomSaveProficient = true,
        charismaSaveProficient = false,
        passivePerceptionBonus = 0,
        armorProficiencies = "light_armor",
        weaponProficiencies = "dagger|quarterstaff|light_crossbow",
        toolProficiencies = "calligraphers_supplies",
        languageProficiencies = "common|elvish|draconic",
        alignment = "",
        background = "",
        faith = "",
        homeland = "",
        age = "",
        gender = "",
        height = "",
        weight = "",
        eyes = "",
        hair = "",
        skin = "",
        personalityTraits = "",
        ideals = "",
        bonds = "",
        flaws = "",
        biography = "",
        createdAt = 0L,
        updatedAt = 0L
    )

    val context = LocalContext.current
    val localized = if (russian) remember(context) {
        com.dndcharacterhandler.data.localization.LocalizationRepository(context).getStrings(AppLanguage.RUSSIAN)
    } else previewStrings
    val sample = if (russian) previewCharacter.copy(name = "Имя персонажа", race = "Раса", characterClass = "Класс", level = 1, experience = 0, maxHp = 8, armorClass = 10, dexterity = 10) else previewCharacter
    CompositionLocalProvider(LocalStrings provides localized) {
        DnDTheme(theme) {
            OverviewContent(
                characterBundle = CharacterBundle(
                    character = sample,
                    skills = emptyList(),
                    attacks = emptyList(),
                    combatResources = emptyList(),
                    inventoryItems = emptyList(),
                    spells = emptyList(),
                    features = emptyList(),
                    notes = emptyList()
                ),
                onOpenDrawer = {},
                onOpenDice = {},
                onUpdateIdentity = { _, _, _, _, _ -> },
                onUpdateExperience = { _, _ -> },
                onUpdatePortrait = { _, _ -> },
                onSetHitPoints = { _, _, _ -> },
                onNudgeHitPoints = { _, _ -> },
                onUpdateMaxHitPoints = { _, _ -> },
                onUpdateArmorClass = { _, _, _, _ -> },
                onUpdateInitiative = { _, _ -> },
                onUpdateSpeed = { _, _ -> },
                onSpendHitDice = { _, _, _ -> },
                onToggleInspiration = {},
                onShortRest = {},
                onLongRest = {}
            )
        }
    }
}
