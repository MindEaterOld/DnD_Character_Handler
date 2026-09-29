package com.dndcharacterhandler.data.repository

import android.content.Context
import com.dndcharacterhandler.domain.model.InventoryArmorDetails
import com.dndcharacterhandler.domain.model.InventoryArmorType
import com.dndcharacterhandler.domain.model.InventoryCatalogItem
import com.dndcharacterhandler.domain.model.InventoryCatalogSource
import com.dndcharacterhandler.domain.model.InventoryCategory
import com.dndcharacterhandler.domain.model.InventoryWeaponClass
import com.dndcharacterhandler.domain.model.InventoryWeaponDamage
import com.dndcharacterhandler.domain.model.InventoryWeaponDetails
import com.dndcharacterhandler.domain.model.InventoryWeaponProperty
import com.dndcharacterhandler.domain.model.InventoryWeaponRangeType
import com.dndcharacterhandler.domain.repository.InventoryCatalogRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class AssetInventoryCatalogRepository(
    private val context: Context
) : InventoryCatalogRepository {
    @Volatile
    private var cachedItems: List<InventoryCatalogItem>? = null

    override suspend fun getItems(): List<InventoryCatalogItem> {
        cachedItems?.let { return it }
        return withContext(Dispatchers.IO) {
            cachedItems?.let { return@withContext it }

            val equipmentItems = readArray("5e-SRD-Equipment.json").mapNotNull(::parseEquipmentItem)
            val magicItems = readArray("5e-SRD-Magic-Items.json").mapNotNull(::parseMagicItem)
            val russian = readRussianText()
            val merged = (equipmentItems + magicItems)
                .map { item -> item.withRussianText(russian.optJSONObject(item.id)) }
                .sortedBy { it.name }
            cachedItems = merged
            merged
        }
    }

    /** Russian names/descriptions keyed by catalog id ("equipment:<index>", "magic:<index>"), from TTG Club. */
    private fun readRussianText(): JSONObject =
        runCatching {
            JSONObject(context.assets.open("inventory_text_ru.json").bufferedReader().use { it.readText() })
        }.getOrDefault(JSONObject())

    private fun InventoryCatalogItem.withRussianText(text: JSONObject?): InventoryCatalogItem =
        if (text == null) {
            this
        } else {
            copy(
                ruName = text.optString("name"),
                ruDescription = text.optString("description"),
                ruDetailLine = text.optString("detail")
            )
        }

    private fun readArray(assetName: String): JSONArray {
        val rawJson = context.assets.open(assetName).bufferedReader().use { it.readText() }
        return JSONArray(rawJson)
    }

    private fun JSONArray.mapNotNull(transform: (JSONObject) -> InventoryCatalogItem?): List<InventoryCatalogItem> =
        buildList(length()) {
            for (index in 0 until length()) {
                val item = transform(getJSONObject(index)) ?: continue
                add(item)
            }
        }

    private fun parseEquipmentItem(json: JSONObject): InventoryCatalogItem? {
        val id = json.optString("index").ifBlank { return null }
        val name = json.optString("name").ifBlank { return null }
        val categories = json.optJSONArray("equipment_categories") ?: JSONArray()
        val category = mapCategory(name = name, categories = categories)
        val weight = json.optDoubleOrZero("weight")
        val detailLine = buildEquipmentDetailLine(json)
        val description = json.optString("description").ifBlank { detailLine }
        val cost = json.optJSONObject("cost")
        val armorDetails = json.toArmorDetails(categories)
        val weaponDetails = json.toWeaponDetails(categories)

        return InventoryCatalogItem(
            id = "equipment:$id",
            name = name,
            category = category,
            weight = weight,
            description = description,
            isMagical = false,
            source = InventoryCatalogSource.EQUIPMENT,
            detailLine = detailLine.ifBlank { null },
            costQuantity = cost?.optInt("quantity")?.takeIf { it > 0 },
            costUnit = cost?.optString("unit")?.ifBlank { null },
            armorDetails = armorDetails,
            weaponDetails = weaponDetails
        )
    }

    private fun parseMagicItem(json: JSONObject): InventoryCatalogItem? {
        val id = json.optString("index").ifBlank { return null }
        val name = json.optString("name").ifBlank { return null }
        val equipmentCategory = json.optJSONObject("equipment_category")
        val categories = JSONArray().apply {
            if (equipmentCategory != null) {
                put(equipmentCategory)
            }
        }
        val category = mapCategory(name = name, categories = categories)
        val rarity = json.optJSONObject("rarity")?.optString("name").orEmpty()
        val attunement = json.optBoolean("attunement", false)
        val detailLine = buildMagicItemDetailLine(
            rarity = rarity,
            attunement = attunement,
            category = equipmentCategory?.optString("name").orEmpty()
        )

        return InventoryCatalogItem(
            id = "magic:$id",
            name = name,
            category = category,
            weight = 0.0,
            description = json.optString("desc"),
            isMagical = true,
            source = InventoryCatalogSource.MAGIC_ITEM,
            detailLine = detailLine.ifBlank { null }
        )
    }

    private fun mapCategory(name: String, categories: JSONArray): InventoryCategory {
        val categoryTokens = buildList {
            add(name.lowercase())
            for (index in 0 until categories.length()) {
                val value = categories.optJSONObject(index)?.optString("name").orEmpty().lowercase()
                if (value.isNotBlank()) {
                    add(value)
                }
            }
        }

        return when {
            categoryTokens.any { it.contains("armor") || it.contains("shield") } -> InventoryCategory.ARMOR
            categoryTokens.any { it.contains("weapon") || it.contains("ammunition") } -> InventoryCategory.WEAPON
            categoryTokens.any { it.contains("potion") || it.contains("poison") || it.contains("consumable") } -> InventoryCategory.CONSUMABLE
            else -> InventoryCategory.OTHER
        }
    }

    private fun JSONObject.toArmorDetails(categories: JSONArray): InventoryArmorDetails? {
        val armorClass = optJSONObject("armor_class") ?: return null
        val armorType = categories.toArmorType() ?: return null
        return InventoryArmorDetails(
            armorType = armorType,
            armorClass = armorClass.optInt("base"),
            appliesDexterityBonus = armorClass.optBoolean("dex_bonus"),
            maxDexterityBonus = armorClass.optNullableInt("max_bonus"),
            strengthMinimum = optInt("str_minimum", 0),
            hasStealthDisadvantage = optBoolean("stealth_disadvantage", false)
        )
    }

    private fun JSONArray.toArmorType(): InventoryArmorType? {
        val names = (0 until length()).mapNotNull { index ->
            optJSONObject(index)?.optString("name")?.takeIf { it.isNotBlank() }
        }
        return when {
            names.any { it == "Shields" } -> InventoryArmorType.SHIELD
            names.any { it == "Light Armor" } -> InventoryArmorType.LIGHT
            names.any { it == "Medium Armor" } -> InventoryArmorType.MEDIUM
            names.any { it == "Heavy Armor" } -> InventoryArmorType.HEAVY
            else -> null
        }
    }

    private fun JSONObject.toWeaponDetails(categories: JSONArray): InventoryWeaponDetails? {
        val weaponClass = categories.toWeaponClass() ?: return null
        val rangeType = categories.toWeaponRangeType() ?: return null
        val baseDamage = optJSONObject("damage")?.toWeaponDamage() ?: return null
        val range = preferredWeaponRange()
        return InventoryWeaponDetails(
            weaponClass = weaponClass,
            rangeType = rangeType,
            // SRD indexes use hyphens ("light-hammer"); the app's weapon/proficiency ids use underscores.
            baseWeaponId = optString("index").ifBlank { null }?.replace('-', '_'),
            normalRange = range?.first,
            longRange = range?.second,
            damages = listOf(baseDamage),
            twoHandedDamage = optJSONObject("two_handed_damage")?.toWeaponDamage(),
            properties = optJSONArray("properties").toWeaponProperties()
        )
    }

    private fun JSONArray.toWeaponClass(): InventoryWeaponClass? {
        val names = (0 until length()).mapNotNull { index ->
            optJSONObject(index)?.optString("name")?.takeIf { it.isNotBlank() }
        }
        return when {
            names.any { it == "Simple Weapons" } -> InventoryWeaponClass.SIMPLE
            names.any { it == "Martial Weapons" } -> InventoryWeaponClass.MARTIAL
            else -> null
        }
    }

    private fun JSONArray.toWeaponRangeType(): InventoryWeaponRangeType? {
        val names = (0 until length()).mapNotNull { index ->
            optJSONObject(index)?.optString("name")?.takeIf { it.isNotBlank() }
        }
        return when {
            names.any { it == "Melee Weapons" || it.contains("Melee") } -> InventoryWeaponRangeType.MELEE
            names.any { it == "Ranged Weapons" || it.contains("Ranged") } -> InventoryWeaponRangeType.RANGED
            else -> null
        }
    }

    private fun JSONObject.preferredWeaponRange(): Pair<Int?, Int?>? {
        val thrownRange = optJSONObject("throw_range")
        if (thrownRange != null) {
            return thrownRange.optNullableInt("normal") to thrownRange.optNullableInt("long")
        }
        val range = optJSONObject("range") ?: return null
        return range.optNullableInt("normal") to range.optNullableInt("long")
    }

    private fun JSONObject.toWeaponDamage(): InventoryWeaponDamage =
        InventoryWeaponDamage(
            dice = optString("damage_dice"),
            damageType = optJSONObject("damage_type")?.optString("name").orEmpty()
        )

    private fun JSONArray?.toWeaponProperties(): Set<InventoryWeaponProperty> {
        if (this == null) return emptySet()
        return (0 until length()).mapNotNull { index ->
            optJSONObject(index)?.optString("name")?.takeIf { it.isNotBlank() }?.toWeaponProperty()
        }.toSet()
    }

    private fun String.toWeaponProperty(): InventoryWeaponProperty? =
        when (this) {
            "Ammunition" -> InventoryWeaponProperty.AMMUNITION
            "Finesse" -> InventoryWeaponProperty.FINESSE
            "Heavy" -> InventoryWeaponProperty.HEAVY
            "Light" -> InventoryWeaponProperty.LIGHT
            "Loading" -> InventoryWeaponProperty.LOADING
            "Reach" -> InventoryWeaponProperty.REACH
            "Thrown" -> InventoryWeaponProperty.THROWN
            "Two-Handed" -> InventoryWeaponProperty.TWO_HANDED
            "Versatile" -> InventoryWeaponProperty.VERSATILE
            else -> null
        }

    private fun buildEquipmentDetailLine(json: JSONObject): String {
        val parts = mutableListOf<String>()
        json.optJSONObject("cost")?.let { cost ->
            val quantity = cost.optInt("quantity")
            val unit = cost.optString("unit")
            if (quantity > 0 && unit.isNotBlank()) {
                parts += "$quantity $unit"
            }
        }
        json.optJSONObject("damage")?.let { damage ->
            val damageDice = damage.optString("damage_dice")
            val damageType = damage.optJSONObject("damage_type")?.optString("name").orEmpty()
            if (damageDice.isNotBlank()) {
                parts += listOf(damageDice, damageType).filter { it.isNotBlank() }.joinToString(" ")
            }
        }
        json.optJSONObject("armor_class")?.let { armorClass ->
            val base = armorClass.optInt("base")
            if (base > 0) {
                parts += "AC $base"
            }
        }

        if (parts.isEmpty()) {
            val categoryName = json.optJSONArray("equipment_categories")
                ?.optJSONObject(0)
                ?.optString("name")
                .orEmpty()
            if (categoryName.isNotBlank()) {
                parts += categoryName
            }
        }

        return parts.joinToString(" - ")
    }

    private fun buildMagicItemDetailLine(
        rarity: String,
        attunement: Boolean,
        category: String
    ): String {
        val parts = mutableListOf<String>()
        if (category.isNotBlank()) parts += category
        if (rarity.isNotBlank()) parts += rarity
        if (attunement) parts += "Attunement"
        return parts.joinToString(" - ")
    }

    private fun JSONObject.optDoubleOrZero(name: String): Double =
        when (val value = opt(name)) {
            is Number -> value.toDouble()
            is String -> value.toDoubleOrNull() ?: 0.0
            else -> 0.0
        }

    private fun JSONObject.optNullableInt(name: String): Int? =
        if (isNull(name) || !has(name)) null else optInt(name)
}
