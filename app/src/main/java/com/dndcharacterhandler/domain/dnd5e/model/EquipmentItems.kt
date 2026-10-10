package com.dndcharacterhandler.domain.dnd5e.model

/** "Thieves’ Tools" and "Thieves' Tools", "Alchemists Supplies" and "Alchemist's Supplies" alike. */
fun equipmentNameKey(name: String): String =
    name.lowercase().replace("’", "").replace("'", "").replace(Regex("\\s+"), " ").trim()

/** An inventory item with just the name (and weight, when known), for equipment the app's item catalog doesn't know. */
fun plainEquipmentItem(item: CatalogEquipmentRef, count: Int, russian: Boolean): InventoryItem = InventoryItem(
    name = item.name.get(russian),
    category = when (item.type) {
        "weapon" -> InventoryCategory.WEAPON
        "consumable" -> InventoryCategory.CONSUMABLE
        else -> InventoryCategory.OTHER
    },
    weight = item.weight ?: 0.0,
    quantity = count,
    isEquipped = false,
    icon = ""
)

/**
 * A Foundry item as the character's: the app's catalog item of that name ([itemsByName], keyed by
 * [equipmentNameKey] of the English name) with its stats, or just the name.
 */
fun matchedEquipmentItem(
    itemsByName: Map<String, InventoryCatalogItem>,
    item: CatalogEquipmentRef,
    count: Int,
    russian: Boolean
): InventoryItem =
    (itemsByName[equipmentNameKey(item.name.en)] ?: EquipmentNameAliases[equipmentNameKey(item.name.en)]?.let(itemsByName::get))
        ?.toInventoryItem(russian)?.copy(quantity = count)
        ?: plainEquipmentItem(item, count, russian)

/** Foundry's names the item catalog knows by another: a druid's wooden staff is the SRD's Staff (a druidic focus). */
private val EquipmentNameAliases = mapOf(
    "wooden staff" to "staff"
)

/**
 * [container] as the character's items: the container, then what it holds (a pack's tinderbox and
 * rations, its waterskin with the water inside), each made by [itemFor]. They are new and point at
 * their container through ids from [ids]; saving gives them real ones.
 */
fun CharacterCatalog.containerItems(
    container: CatalogContainer,
    russian: Boolean,
    itemFor: (CatalogEquipmentRef, Int) -> InventoryItem,
    ids: NewItemIds,
    insideId: Long? = null
): List<InventoryItem> {
    val id = ids.next()
    val self = InventoryItem(
        id = id,
        name = equipment[container.id]?.name?.get(russian).orEmpty(),
        description = container.text.get(russian),
        category = InventoryCategory.CONTAINER,
        weight = container.weight,
        quantity = 1,
        isEquipped = false,
        icon = "",
        costQuantity = container.price,
        costUnit = container.priceUnit,
        containerDetails = InventoryContainerDetails(container.capacity, container.weightlessContents),
        containerId = insideId
    )
    return listOf(self) + container.contents.flatMap { content ->
        val nested = containers[content.itemId]?.takeIf { it.id != container.id }
        val item = equipment[content.itemId]
        when {
            nested != null -> (1..content.count).flatMap { containerItems(nested, russian, itemFor, ids, id) }
            item != null -> listOf(itemFor(item, content.count).copy(containerId = id, isEquipped = false))
            else -> emptyList()
        }
    }
}

/**
 * What a starting-equipment pick gives: the item made by [itemFor], or a container. Empty ones stack
 * (2 Pouches); each pack comes on its own, with its contents.
 */
fun CharacterCatalog.startingItems(
    item: CatalogEquipmentRef,
    count: Int,
    russian: Boolean,
    itemFor: (CatalogEquipmentRef, Int) -> InventoryItem,
    ids: NewItemIds = NewItemIds()
): List<InventoryItem> {
    val container = containers[item.id] ?: return listOf(itemFor(item, count))
    if (container.contents.isEmpty()) {
        return containerItems(container, russian, itemFor, ids).map { it.copy(quantity = count.coerceAtLeast(1)) }
    }
    return (1..count.coerceAtLeast(1)).flatMap { containerItems(container, russian, itemFor, ids) }
}

/** The container sold by itself (not a pack's waterskin) named [englishName], if Foundry has one. */
fun CharacterCatalog.containerNamed(englishName: String): CatalogContainer? = containersByName[equipmentNameKey(englishName)]
