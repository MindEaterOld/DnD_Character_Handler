package com.dndcharacterhandler.presentation.inventory

import com.dndcharacterhandler.domain.model.InventoryCatalogBonusVariant
import com.dndcharacterhandler.domain.model.InventoryCatalogItem
import com.dndcharacterhandler.domain.model.InventoryItem
import com.dndcharacterhandler.presentation.localization.CatalogIndex
import com.dndcharacterhandler.presentation.localization.catalogFieldText

/** Finds the catalog entry a character's inventory item was added from. */
internal class InventoryCatalogLookup(items: List<InventoryCatalogItem>) {
    private val index = CatalogIndex(items, id = { it.id }, names = { listOf(it.name, it.ruName) })

    /** "+N" variant id ("magic:weapon-2") -> the enchantment it belongs to and the variant itself. */
    private val variantsById: Map<String, Pair<InventoryCatalogItem, InventoryCatalogBonusVariant>> =
        items.flatMap { item -> item.bonusVariants.map { variant -> variant.id to (item to variant) } }.toMap()

    /**
     * The entry for [item], and whether it is certain. Items saved before catalogId existed are matched
     * by name (English or Russian) and category; an ambiguous match is used for display only.
     */
    fun find(item: InventoryItem): Pair<InventoryCatalogItem, Boolean>? {
        item.catalogId?.let { id -> return resolve(id)?.let { it to true } }
        val candidates = index.named(item.name).filter { it.category == item.category }
        return when (candidates.size) {
            0 -> null
            1 -> candidates.first() to true
            else -> candidates.first() to false
        }
    }

    /**
     * Plain ids resolve to their entry. Composed ids ("magic:weapon-1@equipment:longsword", see
     * [InventoryCatalogItem.appliedTo]) resolve to an entry named after the magic part and the base in
     * both languages, so the item's name follows the app language like any other catalog item.
     */
    private fun resolve(id: String): InventoryCatalogItem? {
        val magicId = id.substringBefore(InventoryCatalogItem.COMPOSED_ID_SEPARATOR)
        val baseId = id.substringAfter(InventoryCatalogItem.COMPOSED_ID_SEPARATOR, missingDelimiterValue = "")
        val owner = variantsById[magicId]
        val magic = index.byId(magicId) ?: owner?.first ?: return null
        val variant = owner?.second
        val base = baseId.takeIf { it.isNotEmpty() }?.let(index::byId)
        return when {
            base != null -> magic.copy(
                id = id,
                name = magic.composedName(base, variant, russian = false),
                ruName = magic.composedName(base, variant, russian = true)
            )
            // A "+N" entry added on its own before enchantments needed a base ("Armor +1").
            variant != null -> magic.copy(id = id, name = variant.name, ruName = variant.ruName)
            else -> magic
        }
    }
}

/**
 * A character's catalog item with its name and description in the current language ([russian] or
 * English); text the user edited is kept (see [catalogFieldText]). A confidently matched legacy item
 * also gets its catalogId, so it is stored the next time the item is saved.
 */
internal fun InventoryItem.localizedWith(catalog: InventoryCatalogLookup, russian: Boolean): InventoryItem {
    val (entry, certain) = catalog.find(this) ?: return this
    return copy(
        name = catalogFieldText(name, entry.name, entry.ruName, entry.displayName(russian)),
        description = catalogFieldText(description, entry.description, entry.ruDescription, entry.displayDescription(russian)),
        catalogId = if (certain) entry.id else catalogId
    )
}

internal fun List<InventoryItem>.localizedWith(catalog: InventoryCatalogLookup, russian: Boolean): List<InventoryItem> =
    map { it.localizedWith(catalog, russian) }
