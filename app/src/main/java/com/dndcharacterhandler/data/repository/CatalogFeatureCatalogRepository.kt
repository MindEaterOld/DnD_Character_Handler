package com.dndcharacterhandler.data.repository

import com.dndcharacterhandler.domain.model.CatalogFeature
import com.dndcharacterhandler.domain.model.CatalogFeatureKind
import com.dndcharacterhandler.domain.model.CatalogText
import com.dndcharacterhandler.domain.model.CharacterCatalog
import com.dndcharacterhandler.domain.model.FeatureCatalogGroup
import com.dndcharacterhandler.domain.model.FeatureCatalogItem
import com.dndcharacterhandler.domain.model.FeatureSource
import com.dndcharacterhandler.domain.repository.CharacterCatalogRepository
import com.dndcharacterhandler.domain.repository.FeatureCatalogRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** The add-feature catalog: every feature, option and feat of the imported character catalog. */
class CatalogFeatureCatalogRepository(
    private val characterCatalogRepository: CharacterCatalogRepository
) : FeatureCatalogRepository {
    @Volatile
    private var cachedItems: List<FeatureCatalogItem>? = null
    private val loadMutex = Mutex()

    override suspend fun getItems(): List<FeatureCatalogItem> {
        cachedItems?.let { return it }
        return loadMutex.withLock {
            cachedItems ?: withContext(Dispatchers.Default) {
                featureCatalogItems(characterCatalogRepository.getCatalog())
            }.also { cachedItems = it }
        }
    }
}

/** Catalog features as add-feature entries, sorted by name. */
internal fun featureCatalogItems(catalog: CharacterCatalog): List<FeatureCatalogItem> {
    val legacyByFeature = catalog.legacyIds.entries.groupBy({ it.value }, { it.key })
    return catalog.features
        .map { feature -> feature.toCatalogItem(catalog, legacyByFeature[feature.id].orEmpty()) }
        .sortedBy { it.name.lowercase() }
}

private fun CatalogFeature.toCatalogItem(catalog: CharacterCatalog, legacyIds: List<String>): FeatureCatalogItem {
    val category = category(catalog)
    return FeatureCatalogItem(
        id = id,
        // English falls back to Russian for the many non-SRD entries that only come in Russian.
        name = name.en.ifBlank { name.ru },
        description = text.en.ifBlank { text.ru },
        level = level,
        source = source(),
        category = category.en.ifBlank { category.ru },
        ruName = name.ru,
        ruDescription = text.ru,
        ruCategory = category.ru,
        group = group(),
        book = catalog.books[book] ?: CatalogText(book, book),
        legacyIds = legacyIds
    )
}

/** Where it comes from: the class, subclass, species or background granting it, or its kind of option. */
private fun CatalogFeature.category(catalog: CharacterCatalog): CatalogText {
    val owners = grantedBy.mapNotNull { catalog.ownerName(it.ownerId) }.distinct()
    val subtypeLabel = catalog.subtypes[subtype]
    return when {
        kind == CatalogFeatureKind.CLASS_OPTION && subtypeLabel != null -> subtypeLabel
        kind == CatalogFeatureKind.FEAT -> subtypeLabel ?: CatalogText("Feat", "Черта")
        owners.isNotEmpty() -> CatalogText(
            en = owners.joinToString(", ") { it.en.ifBlank { it.ru } },
            ru = owners.joinToString(", ") { it.ru.ifBlank { it.en } }
        )
        subtypeLabel != null -> subtypeLabel
        else -> CatalogText()
    }
}

private fun CatalogFeature.group(): FeatureCatalogGroup = when (kind) {
    CatalogFeatureKind.CLASS_FEATURE -> FeatureCatalogGroup.CLASS
    CatalogFeatureKind.SUBCLASS_FEATURE -> FeatureCatalogGroup.SUBCLASS
    CatalogFeatureKind.CLASS_OPTION, CatalogFeatureKind.FEATURE_OPTION -> FeatureCatalogGroup.OPTION
    CatalogFeatureKind.FEAT -> FeatureCatalogGroup.FEAT
    CatalogFeatureKind.SPECIES_TRAIT -> FeatureCatalogGroup.SPECIES
    CatalogFeatureKind.BACKGROUND_FEATURE -> FeatureCatalogGroup.BACKGROUND
    CatalogFeatureKind.ITEM, CatalogFeatureKind.OTHER -> FeatureCatalogGroup.OTHER
}

private fun CatalogFeature.source(): FeatureSource = when (kind) {
    CatalogFeatureKind.CLASS_FEATURE, CatalogFeatureKind.SUBCLASS_FEATURE, CatalogFeatureKind.CLASS_OPTION -> FeatureSource.CLASS
    CatalogFeatureKind.SPECIES_TRAIT -> FeatureSource.RACE
    CatalogFeatureKind.BACKGROUND_FEATURE -> FeatureSource.BACKGROUND
    // Origin feats come with the background, as the old catalog filed them.
    CatalogFeatureKind.FEAT -> if (subtype == "origin") FeatureSource.BACKGROUND else FeatureSource.OTHER
    CatalogFeatureKind.ITEM -> if (grantedBy.isNotEmpty()) FeatureSource.CLASS else FeatureSource.OTHER
    CatalogFeatureKind.FEATURE_OPTION, CatalogFeatureKind.OTHER -> FeatureSource.OTHER
}
