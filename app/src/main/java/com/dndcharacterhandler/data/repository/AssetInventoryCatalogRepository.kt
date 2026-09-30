package com.dndcharacterhandler.data.repository

import android.content.Context
import com.dndcharacterhandler.domain.model.InventoryArmorDetails
import com.dndcharacterhandler.domain.model.InventoryArmorType
import com.dndcharacterhandler.domain.model.InventoryCatalogBonusVariant
import com.dndcharacterhandler.domain.model.InventoryCatalogItem
import com.dndcharacterhandler.domain.model.InventoryCatalogKind
import com.dndcharacterhandler.domain.model.InventoryCatalogSource
import com.dndcharacterhandler.domain.model.InventoryCategory
import com.dndcharacterhandler.domain.model.InventoryWeaponClass
import com.dndcharacterhandler.domain.model.InventoryWeaponDamage
import com.dndcharacterhandler.domain.model.InventoryWeaponDetails
import com.dndcharacterhandler.domain.model.InventoryWeaponProperty
import com.dndcharacterhandler.domain.model.InventoryWeaponRangeType
import com.dndcharacterhandler.domain.repository.InventoryCatalogRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class AssetInventoryCatalogRepository(
    private val context: Context
) : InventoryCatalogRepository {
    @Volatile
    private var cachedItems: List<InventoryCatalogItem>? = null
    private val loadMutex = Mutex()

    override suspend fun getItems(): List<InventoryCatalogItem> {
        cachedItems?.let { return it }
        // Several ViewModels request the catalog at startup; parse the assets only once.
        return loadMutex.withLock { cachedItems ?: loadItems().also { cachedItems = it } }
    }

    private suspend fun loadItems(): List<InventoryCatalogItem> = withContext(Dispatchers.IO) {
        val equipmentJson = readArray("5e-SRD-Equipment.json")
        val equipmentItems = equipmentJson.mapNotNull(::parseEquipmentItem)
        val ammunitionIds = equipmentJson.objects()
            .filter { json -> json.optJSONArray("equipment_categories").categoryNames().contains("Ammunition") }
            .map { json -> "equipment:${json.optString("index")}" }
            .toSet()
        val russian = readRussianText()
        val magicItems = parseMagicItems(
            entries = readArray("5e-SRD-Magic-Items.json").objects(),
            equipment = equipmentItems,
            ammunitionIds = ammunitionIds,
            russian = russian
        )
        (equipmentItems + magicItems)
            .map { item -> item.withRussianText(russian.optJSONObject(item.id)) }
            .sortedBy { it.name }
    }

    /**
     * Sorts SRD magic items into things you can own ([InventoryCatalogKind.ITEM]) and properties
     * applied to a mundane base ([InventoryCatalogKind.ENCHANTMENT]: "Armor (Any Light, Medium, or
     * Heavy)", "Weapon (Any Melee Weapon)", ...). "+1/+2/+3" variants of an enchantment are folded into
     * it, and grouping entries that only list variants ("Shield", "Horn of Valhalla") are dropped.
     * Magic weapons/armor with a named base ("Weapon (Longsword)") keep that base's stats.
     */
    private fun parseMagicItems(
        entries: List<JSONObject>,
        equipment: List<InventoryCatalogItem>,
        ammunitionIds: Set<String>,
        russian: JSONObject
    ): List<InventoryCatalogItem> {
        val byIndex = entries.associateBy { it.optString("index") }
        val parentOfVariant = buildMap<String, JSONObject> {
            entries.forEach { parent ->
                parent.optJSONArray("variants").objects().forEach { variant -> put(variant.optString("index"), parent) }
            }
        }
        return entries.mapNotNull { entry ->
            val index = entry.optString("index")
            val item = parseMagicItem(entry) ?: return@mapNotNull null
            val (baseType, baseRule) = entry.baseTypeAndRule() ?: (null to null)
            val variants = entry.optJSONArray("variants").objects()
            when {
                parentOfVariant[index]?.isEnchantmentTemplate() == true -> null
                entry.isEnchantmentTemplate() -> item.copy(
                    kind = InventoryCatalogKind.ENCHANTMENT,
                    name = if (variants.isEmpty()) item.name else "${item.name} +1, +2, or +3",
                    baseItemIds = anyBaseIds(baseType.orEmpty(), baseRule.orEmpty(), equipment, ammunitionIds),
                    magicBonus = entry.fixedMagicBonus(),
                    bonusVariants = variants.mapNotNull { variant ->
                        val variantIndex = variant.optString("index")
                        val bonus = Regex("""-(\d)$""").find(variantIndex)?.groupValues?.get(1)?.toIntOrNull()
                            ?: return@mapNotNull null
                        InventoryCatalogBonusVariant(
                            id = "magic:$variantIndex",
                            bonus = bonus,
                            name = byIndex[variantIndex]?.optString("name").orEmpty(),
                            ruName = russian.optJSONObject("magic:$variantIndex")?.optString("name").orEmpty()
                        )
                    }.sortedBy { it.bonus }
                )
                variants.isNotEmpty() -> null
                else -> item.copy(
                    baseItemIds = baseRule?.let { specificBaseIds(it, equipment) }.orEmpty(),
                    magicBonus = entry.fixedMagicBonus()
                )
            }
        }
    }

    /** ("Weapon", "Any Melee Weapon") from a description starting "Weapon (Any Melee Weapon)". */
    private fun JSONObject.baseTypeAndRule(): Pair<String, String>? {
        val typeLine = optString("desc").lineSequence().firstOrNull()?.trim().orEmpty()
        val match = Regex("""^(Weapon|Armor) \((.+)\)$""").find(typeLine) ?: return null
        return match.groupValues[1] to match.groupValues[2]
    }

    private fun JSONObject.isEnchantmentTemplate(): Boolean =
        baseTypeAndRule()?.second?.startsWith("Any", ignoreCase = true) == true

    /** "+2 bonus to attack rolls and damage rolls" / "+1 bonus to Armor Class" stated in the description. */
    private fun JSONObject.fixedMagicBonus(): Int {
        val desc = optString("desc")
        val pattern = Regex("""\+(\d) bonus to (?:attack (?:rolls )?and damage rolls|Armor Class|AC)""", RegexOption.IGNORE_CASE)
        return pattern.find(desc)?.groupValues?.get(1)?.toIntOrNull() ?: 0
    }

    /** Mundane items allowed by rules like "Any Light, Medium, or Heavy" or "Any Medium or Heavy, Except Hide Armor". */
    private fun anyBaseIds(
        type: String,
        rule: String,
        equipment: List<InventoryCatalogItem>,
        ammunitionIds: Set<String>
    ): List<String> {
        val lower = rule.lowercase()
        val excludedName = Regex("""except ([a-z ]+)""").find(lower)?.groupValues?.get(1)?.trim()
        val candidates = when {
            "ammunition" in lower -> equipment.filter { it.id in ammunitionIds }
            type == "Weapon" -> equipment.filter { item ->
                val weapon = item.weaponDetails ?: return@filter false
                "melee" !in lower || weapon.rangeType == InventoryWeaponRangeType.MELEE
            }
            type == "Armor" -> equipment.filter { item ->
                when (item.armorDetails?.armorType) {
                    InventoryArmorType.LIGHT -> "light" in lower
                    InventoryArmorType.MEDIUM -> "medium" in lower
                    InventoryArmorType.HEAVY -> "heavy" in lower
                    InventoryArmorType.SHIELD, null -> false
                }
            }
            else -> emptyList()
        }
        return candidates.filterNot { it.name.lowercase() == excludedName }.map { it.id }
    }

    /** Mundane items named in rules like "Glaive, Greatsword, or Longsword" / "Half Plate Armor or Plate Armor". */
    private fun specificBaseIds(rule: String, equipment: List<InventoryCatalogItem>): List<String> {
        val normalize = { value: String -> value.lowercase().filter(Char::isLetter) }
        val byName = equipment.associateBy { normalize(it.name) }
        return rule.split(Regex(""",\s*(?:or\s+)?|\s+or\s+"""))
            .mapNotNull { name -> byName[normalize(name)]?.id }
            .distinct()
    }

    private fun JSONArray?.objects(): List<JSONObject> {
        val array = this ?: return emptyList()
        return (0 until array.length()).mapNotNull { index -> array.optJSONObject(index) }
    }

    private fun JSONArray?.categoryNames(): List<String> =
        objects().map { it.optString("name") }

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
