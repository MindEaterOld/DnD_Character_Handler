package com.dndcharacterhandler.presentation.features

import com.dndcharacterhandler.domain.model.Feature
import com.dndcharacterhandler.domain.model.FeatureCatalogItem
import com.dndcharacterhandler.presentation.localization.CatalogIndex
import com.dndcharacterhandler.presentation.localization.catalogFieldText

/** Finds the catalog entry a character's feature was added from. */
internal class FeatureCatalogLookup(items: List<FeatureCatalogItem>) {
    private val index = CatalogIndex(items, id = { it.id }, names = { listOf(it.name, it.ruName) })

    /**
     * The entry for [feature], and whether it is certain. Features saved before catalogId existed are
     * matched by name (English or Russian), source and level; several same-named entries (e.g. "Ability
     * Score Improvement") are narrowed by category, otherwise the first one is used for display only.
     */
    fun find(feature: Feature): Pair<FeatureCatalogItem, Boolean>? {
        feature.catalogId?.let { id -> return index.byId(id)?.let { it to true } }
        val candidates = index.named(feature.name)
            .filter { it.source == feature.source && it.level == feature.level }
        if (candidates.size <= 1) return candidates.firstOrNull()?.let { it to true }
        val category = feature.category.trim()
        val byCategory = candidates.filter { category == it.category.trim() || category == it.ruCategory.trim() }
        return if (byCategory.size == 1) byCategory.first() to true else candidates.first() to false
    }
}

/**
 * A character's catalog feature with its name, description and category in the current language
 * ([russian] or English); fields the user edited are kept (see [catalogFieldText]). A confidently
 * matched legacy feature also gets its catalogId, so it is stored the next time the feature is saved.
 */
internal fun Feature.localizedWith(catalog: FeatureCatalogLookup, russian: Boolean): Feature {
    val (item, certain) = catalog.find(this) ?: return this
    return copy(
        name = catalogFieldText(name, item.name, item.ruName, item.displayName(russian)),
        description = catalogFieldText(description, item.description, item.ruDescription, item.displayDescription(russian)),
        category = catalogFieldText(category, item.category, item.ruCategory, item.displayCategory(russian)),
        catalogId = if (certain) item.id else catalogId
    )
}

internal fun List<Feature>.localizedWith(catalog: FeatureCatalogLookup, russian: Boolean): List<Feature> =
    map { it.localizedWith(catalog, russian) }
