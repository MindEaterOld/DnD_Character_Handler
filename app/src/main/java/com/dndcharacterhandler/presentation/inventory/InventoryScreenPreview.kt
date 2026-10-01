package com.dndcharacterhandler.presentation.inventory

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.tooling.preview.Preview
import com.dndcharacterhandler.data.localization.LocalizedStrings
import com.dndcharacterhandler.domain.model.AppLanguage
import com.dndcharacterhandler.domain.model.CharacterBundle
import com.dndcharacterhandler.domain.model.InventoryArmorDetails
import com.dndcharacterhandler.domain.model.InventoryArmorType
import com.dndcharacterhandler.domain.model.InventoryCategory
import com.dndcharacterhandler.domain.model.InventoryContainerDetails
import com.dndcharacterhandler.domain.model.InventoryItem
import com.dndcharacterhandler.domain.model.InventoryWeaponClass
import com.dndcharacterhandler.domain.model.InventoryWeaponDamage
import com.dndcharacterhandler.domain.model.InventoryWeaponDetails
import com.dndcharacterhandler.domain.model.InventoryWeaponProperty
import com.dndcharacterhandler.domain.model.InventoryWeaponRangeType
import com.dndcharacterhandler.presentation.attributes.previewFallbackCharacter
import com.dndcharacterhandler.presentation.localization.LocalStrings
import com.dndcharacterhandler.presentation.theme.DnDTheme

@Preview(
    showBackground = true,
    showSystemUi = true,
    device = "spec:width=412dp,height=915dp"
)
@Composable
fun InventoryScreenPreview() {
    val strings = inventoryPreviewStrings()
    val items = listOf(
        InventoryItem(
            id = 1L,
            name = "Longsword",
            description = "A reliable martial blade.",
            isMagical = false,
            category = InventoryCategory.WEAPON,
            weight = 3.0,
            quantity = 1,
            isEquipped = true,
            icon = "",
            weaponDetails = InventoryWeaponDetails(
                weaponClass = InventoryWeaponClass.MARTIAL,
                rangeType = InventoryWeaponRangeType.MELEE,
                baseWeaponId = "longsword",
                normalRange = 5,
                longRange = null,
                damages = listOf(InventoryWeaponDamage("1d8", "Slashing")),
                twoHandedDamage = InventoryWeaponDamage("1d10", "Slashing"),
                properties = setOf(InventoryWeaponProperty.VERSATILE)
            )
        ),
        InventoryItem(
            id = 2L,
            name = "Shortbow",
            description = "A simple ranged weapon for steady shots.",
            isMagical = false,
            category = InventoryCategory.WEAPON,
            weight = 2.0,
            quantity = 1,
            isEquipped = false,
            icon = "",
            weaponDetails = InventoryWeaponDetails(
                weaponClass = InventoryWeaponClass.SIMPLE,
                rangeType = InventoryWeaponRangeType.RANGED,
                baseWeaponId = "shortbow",
                normalRange = 80,
                longRange = 320,
                damages = listOf(InventoryWeaponDamage("1d6", "Piercing")),
                properties = setOf(InventoryWeaponProperty.AMMUNITION)
            )
        ),
        InventoryItem(
            id = 3L,
            name = "Dagger",
            description = "A compact blade that can be thrown.",
            isMagical = true,
            magicalBonus = 1,
            category = InventoryCategory.WEAPON,
            weight = 1.0,
            quantity = 3,
            isEquipped = false,
            icon = "",
            weaponDetails = InventoryWeaponDetails(
                weaponClass = InventoryWeaponClass.SIMPLE,
                rangeType = InventoryWeaponRangeType.MELEE,
                baseWeaponId = "dagger",
                normalRange = 20,
                longRange = 60,
                damages = listOf(InventoryWeaponDamage("1d4", "Piercing")),
                properties = setOf(
                    InventoryWeaponProperty.FINESSE,
                    InventoryWeaponProperty.LIGHT,
                    InventoryWeaponProperty.THROWN
                )
            )
        ),
        InventoryItem(
            id = 4L,
            name = "Chain Mail",
            description = "Heavy interlocking metal rings.",
            isMagical = false,
            category = InventoryCategory.ARMOR,
            weight = 55.0,
            quantity = 1,
            isEquipped = true,
            icon = "",
            armorDetails = InventoryArmorDetails(
                armorType = InventoryArmorType.HEAVY,
                armorClass = 16,
                appliesDexterityBonus = false,
                maxDexterityBonus = null,
                strengthMinimum = 13,
                hasStealthDisadvantage = true
            )
        ),
        InventoryItem(
            id = 5L,
            name = "Shield",
            description = "A sturdy shield.",
            isMagical = true,
            magicalBonus = 2,
            category = InventoryCategory.ARMOR,
            weight = 6.0,
            quantity = 1,
            isEquipped = true,
            icon = "",
            armorDetails = InventoryArmorDetails(
                armorType = InventoryArmorType.SHIELD,
                armorClass = 2,
                appliesDexterityBonus = false,
                maxDexterityBonus = null,
                strengthMinimum = 0,
                hasStealthDisadvantage = false
            )
        ),
        InventoryItem(
            id = 6L,
            name = "Healing Potion",
            description = "A small vial that restores hit points.",
            isMagical = true,
            magicalBonus = 1,
            category = InventoryCategory.CONSUMABLE,
            weight = 0.5,
            quantity = 3,
            isEquipped = false,
            icon = ""
        ),
        InventoryItem(
            id = 7L,
            name = "Breastplate",
            description = "A fitted medium armor chest piece.",
            isMagical = false,
            category = InventoryCategory.ARMOR,
            weight = 20.0,
            quantity = 1,
            isEquipped = false,
            icon = "",
            armorDetails = InventoryArmorDetails(
                armorType = InventoryArmorType.MEDIUM,
                armorClass = 14,
                appliesDexterityBonus = true,
                maxDexterityBonus = 2,
                strengthMinimum = 0,
                hasStealthDisadvantage = false
            )
        ),
        InventoryItem(
            id = 8L,
            name = "Rope (50 ft)",
            description = "Sturdy hempen rope.",
            isMagical = false,
            category = InventoryCategory.OTHER,
            weight = 10.0,
            quantity = 1,
            isEquipped = false,
            icon = ""
        )
    )

    CompositionLocalProvider(LocalStrings provides strings) {
        DnDTheme {
            InventoryContent(
                characterBundle = CharacterBundle(
                    character = previewFallbackCharacter().copy(
                        copperPieces = 125,
                        silverPieces = 42,
                        goldPieces = 387
                    ),
                    skills = emptyList(),
                    attacks = emptyList(),
                    combatResources = emptyList(),
                    inventoryItems = items,
                    spells = emptyList(),
                    features = emptyList(),
                    notes = emptyList()
                ),
                // The longsword unfolded: its description and the Edit button.
                initiallyExpanded = setOf(1L)
            )
        }
    }
}

/** A backpack unfolded: what it holds, a waterskin with its water inside. */
@Preview(
    showBackground = true,
    showSystemUi = true,
    device = "spec:width=412dp,height=915dp"
)
@Composable
fun InventoryContainersPreview() {
    fun item(id: Long, name: String, weight: Double, quantity: Int = 1, containerId: Long? = null, category: InventoryCategory = InventoryCategory.OTHER) =
        InventoryItem(id = id, name = name, category = category, weight = weight, quantity = quantity, isEquipped = false, icon = "", containerId = containerId)
    val items = listOf(
        item(1, "Backpack", 5.0, category = InventoryCategory.CONTAINER).copy(
            description = "A backpack holds up to 30 pounds.",
            containerDetails = InventoryContainerDetails(capacity = 30.0)
        ),
        item(2, "Bedroll", 7.0, containerId = 1),
        item(3, "Torch", 1.0, quantity = 10, containerId = 1),
        item(4, "Waterskin", 1.0, containerId = 1, category = InventoryCategory.CONTAINER).copy(
            containerDetails = InventoryContainerDetails(capacity = 4.0)
        ),
        item(5, "Water (Pint)", 1.0, quantity = 4, containerId = 4),
        item(6, "Bag of Holding", 5.0, category = InventoryCategory.CONTAINER).copy(
            isMagical = true,
            containerDetails = InventoryContainerDetails(capacity = 500.0, weightlessContents = true)
        ),
        item(7, "Rope", 5.0)
    )
    CompositionLocalProvider(LocalStrings provides inventoryPreviewStrings()) {
        DnDTheme {
            InventoryContent(
                characterBundle = CharacterBundle(
                    character = previewFallbackCharacter(),
                    skills = emptyList(),
                    attacks = emptyList(),
                    combatResources = emptyList(),
                    inventoryItems = items,
                    spells = emptyList(),
                    features = emptyList(),
                    notes = emptyList()
                ),
                initiallyExpanded = setOf(1L)
            )
        }
    }
}

private fun inventoryPreviewStrings(): LocalizedStrings = LocalizedStrings(
    language = AppLanguage.ENGLISH,
    values = mapOf(
        "drawer_open_character_manager" to "Open character manager",
        "dice_open" to "Roll dice",
        "placeholder_loading_character" to "Loading character",
        "inventory_search_placeholder" to "Search Inventory",
        "inventory_carry_weight" to "Carry Weight",
        "common_edit" to "Edit",
        "inventory_unit_pounds" to "lb",
        "inventory_category_weapon" to "Weapon",
        "inventory_category_armor" to "Armor",
        "inventory_category_consumable" to "Consumable",
        "inventory_category_other" to "Other",
        "inventory_empty" to "No items found.",
        "inventory_catalog_title" to "Add from catalog",
        "inventory_catalog_loading" to "Loading catalog...",
        "inventory_catalog_empty" to "No matching items found.",
        "inventory_add_item" to "Add item",
        "common_cancel" to "Cancel",
        "common_save" to "Save",
        "inventory_edit_item" to "Edit item",
        "inventory_currency_edit_title" to "Edit currency",
        "inventory_currency_cp" to "Copper pieces",
        "inventory_currency_sp" to "Silver pieces",
        "inventory_currency_gp" to "Gold pieces",
        "stat_card_copper" to "Copper",
        "stat_card_silver" to "Silver",
        "stat_card_gold" to "Gold",
        "inventory_field_name" to "Name",
        "inventory_field_description" to "Description",
        "inventory_field_magical" to "Magical",
        "inventory_field_magical_bonus" to "Bonus",
        "inventory_field_category" to "Category",
        "inventory_field_quantity" to "Quantity",
        "inventory_field_weight" to "Weight",
        "inventory_field_cost_quantity" to "Cost value",
        "inventory_field_cost_unit" to "Cost unit",
        "inventory_section_description" to "Description",
        "inventory_section_inventory" to "Inventory",
        "inventory_section_armor" to "Armor",
        "inventory_field_armor_type" to "Armor type",
        "inventory_field_armor_class" to "Armor class",
        "inventory_field_applies_dex" to "Add Dexterity modifier",
        "inventory_field_max_dex_bonus" to "Max Dexterity bonus",
        "inventory_field_strength_minimum" to "Strength minimum",
        "inventory_field_stealth_disadvantage" to "Stealth disadvantage",
        "inventory_section_weapon" to "Weapon",
        "inventory_field_weapon_type" to "Weapon type",
        "inventory_field_base_weapon" to "Base weapon",
        "inventory_field_normal_range" to "Normal range",
        "inventory_field_long_range" to "Long range",
        "inventory_field_damage" to "Damage",
        "inventory_field_damage_dice" to "Damage dice",
        "inventory_field_damage_dice_count" to "Dice count",
        "inventory_field_damage_die_type" to "Die type",
        "inventory_field_damage_type" to "Damage type",
        "inventory_field_weapon_properties" to "Weapon properties",
        "inventory_field_two_handed_damage" to "Alternate damage",
        "inventory_add_alternate_damage" to "Add alternate damage",
        "inventory_remove_alternate_damage" to "Remove alternate damage",
        "inventory_damage_type_placeholder" to "Tap to choose damage type",
        "inventory_field_two_handed_damage_dice" to "Two-handed damage dice",
        "inventory_field_two_handed_damage_type" to "Two-handed damage type",
        "inventory_category_container" to "Container",
        "inventory_container_load" to "Inside %1\$s/%2\$s lb",
        "inventory_container_load_open" to "Inside %1\$s lb",
        "inventory_container_weightless" to "Weightless contents",
        "inventory_container_empty" to "Empty",
        "inventory_move" to "Move",
        "inventory_no_description" to "No description."
    )
)
