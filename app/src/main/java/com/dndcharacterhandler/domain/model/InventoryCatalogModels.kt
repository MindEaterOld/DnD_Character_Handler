package com.dndcharacterhandler.domain.model

enum class InventoryCatalogSource { EQUIPMENT, MAGIC_ITEM }

/**
 * [ITEM] is something you can own as is (a longsword, a Bag of Holding, a Sun Blade).
 * [ENCHANTMENT] is a magic property that needs a mundane base item ("Weapon +1", "Flame Tongue",
 * "Mithral Armor") — it only becomes an item once applied to one.
 */
enum class InventoryCatalogKind { ITEM, ENCHANTMENT }

/** One "+N" option of an enchantment such as "Weapon +1, +2, or +3". */
data class InventoryCatalogBonusVariant(
    /** Catalog id of the SRD variant ("magic:weapon-2"). */
    val id: String,
    val bonus: Int,
    val name: String,
    val ruName: String = ""
)

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
    val ruDetailLine: String = "",
    val kind: InventoryCatalogKind = InventoryCatalogKind.ITEM,
    /** Catalog ids of the mundane items this magic item can be made from; empty when it needs none. */
    val baseItemIds: List<String> = emptyList(),
    /** Fixed +N to attack/damage (weapons) or to AC (armor, shields) granted by this magic item. */
    val magicBonus: Int = 0,
    /** Selectable "+N" variants; the chosen one replaces [magicBonus]. */
    val bonusVariants: List<InventoryCatalogBonusVariant> = emptyList()
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
            magicalBonus = magicBonus,
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

    /**
     * The character's item made by applying this magic item or enchantment to the mundane [base]:
     * it keeps the base's stats (damage, AC, weight) and adds the magic bonus and description.
     * Its catalogId ("magic:weapon-1@equipment:longsword") lets the name be re-localized later.
     */
    fun appliedTo(
        base: InventoryCatalogItem,
        variant: InventoryCatalogBonusVariant?,
        russian: Boolean
    ): InventoryItem =
        InventoryItem(
            name = composedName(base, variant, russian),
            description = displayDescription(russian),
            isMagical = true,
            magicalBonus = variant?.bonus ?: magicBonus,
            category = base.category,
            weight = base.weight,
            quantity = 1,
            isEquipped = false,
            icon = "",
            armorDetails = base.armorDetails?.let { armor ->
                // Mithral removes the Strength requirement and the Stealth disadvantage.
                if (id == MITHRAL_ARMOR_ID) armor.copy(strengthMinimum = 0, hasStealthDisadvantage = false) else armor
            },
            weaponDetails = base.weaponDetails,
            catalogId = composedCatalogId(base, variant)
        )

    /** "Longsword +1", "Sun Blade", "Flame Tongue (Longsword)" — in the requested language. */
    fun composedName(base: InventoryCatalogItem, variant: InventoryCatalogBonusVariant?, russian: Boolean): String {
        val baseName = base.displayName(russian)
        return when {
            variant != null -> "$baseName +${variant.bonus}"
            kind == InventoryCatalogKind.ITEM && baseItemIds.size <= 1 -> displayName(russian)
            else -> "${displayName(russian)} ($baseName)"
        }
    }

    fun composedCatalogId(base: InventoryCatalogItem, variant: InventoryCatalogBonusVariant?): String =
        "${variant?.id ?: id}$COMPOSED_ID_SEPARATOR${base.id}"

    companion object {
        /** Separates the magic part from the base item in a composed catalogId. */
        const val COMPOSED_ID_SEPARATOR = "@"
        private const val MITHRAL_ARMOR_ID = "magic:mithral-armor"
    }
}
