package com.dndcharacterhandler.data.repository

import android.content.Context
import com.dndcharacterhandler.domain.model.FeatureCatalogItem
import com.dndcharacterhandler.domain.model.FeatureSource
import com.dndcharacterhandler.domain.repository.FeatureCatalogRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class AssetFeatureCatalogRepository(
    private val context: Context
) : FeatureCatalogRepository {
    @Volatile
    private var cachedItems: List<FeatureCatalogItem>? = null
    private val loadMutex = Mutex()

    @Volatile
    private var ruTextCache: JSONObject? = null

    @Volatile
    private var categoryRuCache: JSONObject? = null

    private fun ruText(): JSONObject {
        ruTextCache?.let { return it }
        val raw = runCatching {
            context.assets.open("feature_text_ru.json").bufferedReader().use { it.readText() }
        }.getOrNull()
        val obj = if (raw != null) runCatching { JSONObject(raw) }.getOrNull() ?: JSONObject() else JSONObject()
        ruTextCache = obj
        return obj
    }

    private fun categoryRu(): JSONObject {
        categoryRuCache?.let { return it }
        val raw = runCatching {
            context.assets.open("category_text_ru.json").bufferedReader().use { it.readText() }
        }.getOrNull()
        val obj = if (raw != null) runCatching { JSONObject(raw) }.getOrNull() ?: JSONObject() else JSONObject()
        categoryRuCache = obj
        return obj
    }

    /** Localizes an English category, mapping each comma-separated part (handles multi-species traits). */
    private fun localizeCategory(category: String): String {
        if (category.isBlank()) return ""
        val map = categoryRu()
        return category.split(", ").joinToString(", ") { part ->
            map.optString(part).ifBlank { part }
        }
    }

    override suspend fun getItems(): List<FeatureCatalogItem> {
        cachedItems?.let { return it }
        // Several ViewModels request the catalog at startup; parse the assets only once.
        return loadMutex.withLock { cachedItems ?: loadItems().also { cachedItems = it } }
    }

    private suspend fun loadItems(): List<FeatureCatalogItem> = withContext(Dispatchers.IO) {
        val classFeatures = readArray("5e-SRD-Features.json")
            .mapNotNull { parseFeature(it, FeatureSource.CLASS) }
        val racialTraits = readArray("5e-SRD-Traits.json")
            .mapNotNull { parseFeature(it, FeatureSource.RACE) }
        val originFeats = readArray("5e-SRD-Feats.json")
            .mapNotNull { parseOriginFeat(it) }
        val extras = readArray("feature_catalog_extra.json")
            .mapNotNull { parseExtra(it) }
        (classFeatures + racialTraits + originFeats + extras).sortedBy { it.name }
    }

    /** Parses a fully-formed catalog entry from the supplementary PHB asset (non-SRD content). */
    private fun parseExtra(json: JSONObject): FeatureCatalogItem? {
        val id = json.optString("id").ifBlank { return null }
        val name = json.optString("name").ifBlank { return null }
        val source = runCatching { FeatureSource.valueOf(json.optString("source")) }
            .getOrDefault(FeatureSource.OTHER)
        val level = if (json.isNull("level")) null else json.optInt("level").takeIf { it > 0 }
        return FeatureCatalogItem(
            id = id,
            name = name,
            description = json.optString("description"),
            level = level,
            source = source,
            category = json.optString("category"),
            ruName = json.optString("ruName"),
            ruDescription = json.optString("ruDescription"),
            ruCategory = localizeCategory(json.optString("category"))
        )
    }

    private fun readArray(assetName: String): JSONArray {
        val rawJson = runCatching {
            context.assets.open(assetName).bufferedReader().use { it.readText() }
        }.getOrNull() ?: return JSONArray()
        return runCatching { JSONArray(rawJson) }.getOrDefault(JSONArray())
    }

    private fun JSONArray.mapNotNull(transform: (JSONObject) -> FeatureCatalogItem?): List<FeatureCatalogItem> =
        buildList(length()) {
            for (index in 0 until length()) {
                transform(getJSONObject(index))?.let(::add)
            }
        }

    private fun parseFeature(json: JSONObject, source: FeatureSource): FeatureCatalogItem? {
        val index = json.optString("index").ifBlank { return null }
        val name = json.optString("name").ifBlank { return null }
        val description = json.optString("description").ifBlank {
            json.optJSONArray("desc").joinParagraphs()
        }
        val level = json.optJSONObject("level")?.optString("index")
            ?.let { Regex("""(\d+)$""").find(it)?.groupValues?.get(1)?.toIntOrNull() }
        val category = when (source) {
            FeatureSource.CLASS -> json.optJSONObject("class")?.optString("name").orEmpty()
            FeatureSource.RACE -> json.optJSONArray("species").joinNames()
            else -> ""
        }
        val ru = ruText().optJSONObject(index)
        return FeatureCatalogItem(
            id = "${source.name}:$index",
            name = name,
            description = description,
            level = level,
            source = source,
            category = category,
            ruName = ru?.optString("name").orEmpty(),
            ruDescription = ru?.optString("description").orEmpty(),
            ruCategory = localizeCategory(category)
        )
    }

    private fun parseOriginFeat(json: JSONObject): FeatureCatalogItem? {
        if (!json.optString("type").equals("origin", ignoreCase = true)) return null
        val index = json.optString("index").ifBlank { return null }
        val name = json.optString("name").ifBlank { return null }
        val description = json.optString("description").ifBlank {
            json.optJSONArray("desc").joinParagraphs()
        }
        val ru = ruText().optJSONObject(index)
        return FeatureCatalogItem(
            id = "FEAT:$index",
            name = name,
            description = description,
            level = null,
            source = FeatureSource.BACKGROUND,
            category = "Origin Feat",
            ruName = ru?.optString("name").orEmpty(),
            ruDescription = ru?.optString("description").orEmpty(),
            ruCategory = localizeCategory("Origin Feat")
        )
    }

    private fun JSONArray?.joinParagraphs(): String {
        if (this == null) return ""
        return buildList(length()) {
            for (index in 0 until length()) {
                optString(index).takeIf { it.isNotBlank() }?.let(::add)
            }
        }.joinToString("\n\n")
    }

    private fun JSONArray?.joinNames(): String {
        if (this == null) return ""
        return buildList(length()) {
            for (index in 0 until length()) {
                optJSONObject(index)?.optString("name")?.takeIf { it.isNotBlank() }?.let(::add)
            }
        }.joinToString(", ")
    }
}
