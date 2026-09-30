package com.dndcharacterhandler.domain.model

/** How the add-feature catalog groups its entries. */
enum class FeatureCatalogGroup { CLASS, SUBCLASS, OPTION, FEAT, SPECIES, BACKGROUND, OTHER }

data class FeatureCatalogItem(
    val id: String,
    val name: String,
    val description: String,
    val level: Int? = null,
    val source: FeatureSource,
    val category: String = "",
    val ruName: String = "",
    val ruDescription: String = "",
    val ruCategory: String = "",
    val group: FeatureCatalogGroup = FeatureCatalogGroup.OTHER,
    /** The book it is from, e.g. "Player's Handbook (2024)". */
    val book: CatalogText = CatalogText(),
    /** Ids this entry had in the catalog the app used before, so features saved with them still match. */
    val legacyIds: List<String> = emptyList()
) {
    /** Display name, preferring the Russian translation when [russian] is requested and available. */
    fun displayName(russian: Boolean): String =
        if (russian && ruName.isNotBlank()) ruName else name

    /** Display description, preferring the Russian translation when [russian] is requested and available. */
    fun displayDescription(russian: Boolean): String =
        if (russian && ruDescription.isNotBlank()) ruDescription else description

    /** Display category/origin, preferring the Russian translation when [russian] is requested and available. */
    fun displayCategory(russian: Boolean): String =
        if (russian && ruCategory.isNotBlank()) ruCategory else category

    fun toFeature(russian: Boolean = false): Feature =
        Feature(
            name = displayName(russian),
            description = displayDescription(russian),
            level = level,
            source = source,
            category = displayCategory(russian),
            catalogId = id
        )
}
