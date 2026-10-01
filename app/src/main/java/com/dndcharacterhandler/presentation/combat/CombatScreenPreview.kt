package com.dndcharacterhandler.presentation.combat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.tooling.preview.Preview
import com.dndcharacterhandler.data.localization.LocalizedStrings
import com.dndcharacterhandler.domain.model.AppLanguage
import com.dndcharacterhandler.domain.model.Attack
import com.dndcharacterhandler.domain.model.CharacterBundle
import com.dndcharacterhandler.domain.model.CombatResource
import com.dndcharacterhandler.domain.model.Spell
import com.dndcharacterhandler.presentation.attributes.previewFallbackCharacter
import com.dndcharacterhandler.presentation.localization.LocalStrings
import com.dndcharacterhandler.presentation.theme.DnDTheme

@Preview(
    name = "Combat Screen",
    showBackground = true,
    device = "spec:width=412dp,height=915dp"
)
@Composable
fun CombatScreenPreview() {
    val strings = LocalizedStrings(
        language = AppLanguage.ENGLISH,
        values = mapOf(
            "drawer_open_character_manager" to "Open character manager",
            "dice_open" to "Roll dice",
            "placeholder_loading_character" to "Loading character",
            "stat_card_armor_class" to "Armor Class",
            "stat_card_spell_bonus" to "Spell Bonus",
            "stat_card_spell_dc" to "Spell DC",
            "combat_section_attacks_title" to "Attacks",
            "combat_combat_resources_title" to "Combat Resources",
            "combat_add_attack" to "Add Attack",
            "combat_add_entry_title" to "Add to Combat",
            "combat_add_attacks_section" to "Attacks",
            "combat_create_weapon_attack" to "Weapon attack from inventory",
            "combat_create_custom_weapon_attack" to "Custom weapon attack",
            "combat_create_spell_attack" to "Spell attack from spellbook",
            "combat_create_custom_spell_attack" to "Custom spell attack",
            "combat_add_resources_section" to "Resources",
            "combat_add_resource_action" to "Add resource",
            "combat_select_weapon" to "Select Weapon",
            "combat_no_weapons_available" to "No weapons in inventory.",
            "combat_edit_attack" to "Edit Attack",
            "combat_attack_range" to "Range",
            "combat_attack_long_range" to "Long range",
            "combat_attack_ability" to "Ability",
            "combat_attack_section_attack" to "Attack",
            "combat_attack_section_damage" to "Damage",
            "combat_attack_section_alternate_damage" to "Alternate Damage",
            "combat_attack_magical_bonus" to "Magical Bonus",
            "combat_attack_main_hand" to "Main hand",
            "combat_attack_add_alternate_damage" to "Add alternate damage",
            "combat_attack_remove_alternate_damage" to "Remove alternate damage",
            "combat_attack_bonus_or_dc" to "Attack Bonus / DC",
            "combat_attack_damage" to "Damage",
            "combat_attack_damage_type" to "Damage Type",
            "combat_attack_proficient" to "Proficient",
            "combat_attack_save_dc" to "DC %1\$s",
            "combat_edit_resource" to "Edit Resource",
            "combat_resource_current" to "Current",
            "combat_resource_maximum" to "Maximum",
            "combat_resource_short_rest" to "Restores on short rest",
            "combat_resource_long_rest" to "Restores on long rest",
            "combat_empty_attacks" to "No attacks yet.",
            "combat_empty_resources" to "No combat resources yet.",
            "features_name" to "Name",
            "combat_attack_calculation_mode" to "Mode",
            "common_automatic" to "Automatic",
            "common_manual" to "Manual",
            "common_save" to "Save",
            "common_cancel" to "Cancel",
            "inventory_delete_action" to "Delete",
            "inventory_field_damage_dice_count" to "Dice count",
            "inventory_field_damage_die_type" to "Die type",
            "inventory_weapon_range_melee" to "Melee",
            "inventory_unit_feet" to "ft",
            "spells_range_touch" to "Touch",
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
            "inventory_damage_type_slashing" to "Slashing",
            "inventory_damage_type_bludgeoning" to "Bludgeoning",
            "inventory_damage_type_piercing" to "Piercing",
            "inventory_damage_type_fire" to "Fire",
            "inventory_damage_type_cold" to "Cold",
            "inventory_damage_type_force" to "Force",
            "spells_resolution_heal" to "Heal",
            "spells_area_sphere" to "Sphere",
            "spells_area_cube" to "Cube",
            "spells_level_cantrips" to "Cantrip",
            "spells_level_1" to "Level 1",
            "spells_level_2" to "Level 2",
            "spells_level_3" to "Level 3",
            "spells_ritual" to "Ritual",
            "spells_material_cost" to "GP",
            "combat_spell_component_verbal_short" to "V",
            "combat_spell_component_somatic_short" to "S",
            "combat_spell_component_material_short" to "M"
        )
    )
    val character = previewFallbackCharacter().copy(
        armorClass = 15,
        level = 7,
        characterClass = "Wizard",
        subclass = "Divination",
        intelligence = 18
    )
    val attacks = listOf(
        Attack(
            id = 1,
            name = "Dagger",
            icon = "",
            isProficient = true,
            normalRange = 5,
            damageDiceCount = 1,
            damageDieType = "d4",
            primaryDamageType = "Piercing"
        )
    )
    val spellAttacks = listOf(
        Spell(
            id = 1,
            name = "Fire Bolt",
            level = 0,
            school = "Evocation",
            isPrepared = true,
            description = "",
            range = "120 feet",
            components = "V, S",
            attackType = "attack",
            damageType = "Fire",
            damageBase = "2d10"
        ),
        Spell(
            id = 2,
            name = "Magic Missile",
            level = 1,
            school = "Evocation",
            isPrepared = true,
            description = "",
            range = "120 feet",
            components = "V, S",
            damageType = "Force",
            damageBase = "3d4",
            damageBonusValue = 3
        ),
        Spell(
            id = 3,
            name = "Fireball",
            level = 3,
            school = "Evocation",
            isPrepared = true,
            description = "",
            range = "150 feet",
            components = "V, S, M",
            damageType = "Fire",
            damageBase = "8d6",
            saveAbility = "Dexterity",
            saveEffect = "half",
            areaOfEffect = "sphere, 20 ft"
        ),
        Spell(
            id = 4,
            name = "Heal Spell",
            level = 2,
            school = "Evocation",
            isPrepared = true,
            description = "",
            range = "Touch",
            components = "V, S, M",
            material = "a diamond",
            materialCost = "100",
            isRitual = true,
            areaOfEffect = "cube, 15 ft",
            healBase = "2d8",
            healBonusValue = 3
        )
    )
    val resources = listOf(
        CombatResource(id = 1, name = "Arcane Recovery", currentUses = 99, maximumUses = 99, restoresOnShortRest = false, restoresOnLongRest = true),
        CombatResource(id = 2, name = "Portent", currentUses = 2, maximumUses = 2, restoresOnShortRest = false, restoresOnLongRest = true),
        CombatResource(id = 3, name = "Bardic Inspiration", currentUses = 3, maximumUses = 3, restoresOnShortRest = true, restoresOnLongRest = true),
        CombatResource(id = 4, name = "Second Wind", currentUses = 1, maximumUses = 1, restoresOnShortRest = true, restoresOnLongRest = false)
    )

    CompositionLocalProvider(LocalStrings provides strings) {
        DnDTheme {
            CombatContent(
                characterBundle = CharacterBundle(
                    character = character,
                    skills = emptyList(),
                    attacks = attacks,
                    combatResources = resources,
                    inventoryItems = emptyList(),
                    spells = emptyList(),
                    spellAttacks = spellAttacks,
                    features = emptyList(),
                    notes = emptyList()
                )
            )
        }
    }
}
