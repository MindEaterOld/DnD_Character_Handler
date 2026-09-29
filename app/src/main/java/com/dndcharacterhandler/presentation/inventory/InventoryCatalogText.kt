package com.dndcharacterhandler.presentation.inventory

import com.dndcharacterhandler.domain.model.InventoryCatalogItem
import com.dndcharacterhandler.domain.model.InventoryItem
import com.dndcharacterhandler.presentation.localization.catalogFieldText

/** Finds the catalog entry a character's inventory item was added from. */
internal class InventoryCatalogLookup(items: List<InventoryCatalogItem>) {
    private val byId = items.associateBy { it.id }
    private val byName: Map<String, List<InventoryCatalogItem>> = buildMap<String, MutableList<InventoryCatalogItem>> {
        items.forEach { item ->
            listOf(item.name, item.ruName)
                .map(String::trim)
                .filter(String::isNotEmpty)
                .distinct()
                .forEach { name -> getOrPut(name) { mutableListOf() }.add(item) }
        }
    }

    /**
     * The entry for [item], and whether it is certain. Items saved before catalogId existed are matched
     * by name (English or Russian) and category; an ambiguous match is used for display only.
     */
    fun find(item: InventoryItem): Pair<InventoryCatalogItem, Boolean>? {
        item.catalogId?.let { id -> return byId[id]?.let { it to true } }
        val candidates = byName[item.name.trim()].orEmpty().filter { it.category == item.category }
        return when (candidates.size) {
            0 -> null
            1 -> candidates.first() to true
            else -> candidates.first() to false
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
