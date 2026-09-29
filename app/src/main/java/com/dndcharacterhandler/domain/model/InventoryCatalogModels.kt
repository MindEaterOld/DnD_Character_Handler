package com.dndcharacterhandler.domain.model

enum class InventoryCatalogSource { EQUIPMENT, MAGIC_ITEM }

data class InventoryCatalogItem(
    val id: String,
    val name: String,
    val category: InventoryCategory,
    val weight: Double,
    val description: String,
    val isMagical: Boolean = false,
    val source: InventoryCatalogSource,
    val detailLine: String? = null,
    val costQuantity: Int? = null,
    val costUnit: String? = null,
    val armorDetails: InventoryArmorDetails? = null,
    val weaponDetails: InventoryWeaponDetails? = null,
    val ruName: String = "",
    val ruDescription: String = "",
    val ruDetailLine: String = ""
) {
    /** Display name, preferring the Russian translation when [russian] is requested and available. */
    fun displayName(russian: Boolean): String =
        if (russian && ruName.isNotBlank()) ruName else name

    /** Display description, preferring the Russian translation when [russian] is requested and available. */
    fun displayDescription(russian: Boolean): String =
        if (russian && ruDescription.isNotBlank()) ruDescription else description

    /** Short "cost - damage - AC" / "type - rarity" line in the requested language. */
    fun displayDetailLine(russian: Boolean): String? =
        if (russian && ruDetailLine.isNotBlank()) ruDetailLine else detailLine

    fun toInventoryItem(russian: Boolean = false): InventoryItem =
        InventoryItem(
            name = displayName(russian),
            description = displayDescription(russian),
            isMagical = isMagical,
            category = category,
            weight = weight,
            quantity = 1,
            isEquipped = false,
            icon = "",
            costQuantity = costQuantity,
            costUnit = costUnit,
            armorDetails = armorDetails,
            weaponDetails = weaponDetails,
            catalogId = id
        )
}
