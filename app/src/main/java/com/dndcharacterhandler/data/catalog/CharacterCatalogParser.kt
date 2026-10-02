package com.dndcharacterhandler.data.catalog

import com.dndcharacterhandler.data.json.has
import com.dndcharacterhandler.data.json.isNull
import com.dndcharacterhandler.data.json.optArray
import com.dndcharacterhandler.data.json.optBoolean
import com.dndcharacterhandler.data.json.optDouble
import com.dndcharacterhandler.data.json.optInt
import com.dndcharacterhandler.data.json.optObject
import com.dndcharacterhandler.data.json.optString
import com.dndcharacterhandler.data.json.parseJsonObject
import com.dndcharacterhandler.domain.model.AdvancementStep
import com.dndcharacterhandler.domain.model.CatalogBackground
import com.dndcharacterhandler.domain.model.CatalogClass
import com.dndcharacterhandler.domain.model.CatalogContainer
import com.dndcharacterhandler.domain.model.CatalogContainerItem
import com.dndcharacterhandler.domain.model.CatalogEquipmentRef
import com.dndcharacterhandler.domain.model.CatalogWeaponMastery
import com.dndcharacterhandler.domain.model.EquipmentNode
import com.dndcharacterhandler.domain.model.CatalogFeature
import com.dndcharacterhandler.domain.model.CatalogFeatureKind
import com.dndcharacterhandler.domain.model.CatalogGrant
import com.dndcharacterhandler.domain.model.CatalogRecovery
import com.dndcharacterhandler.domain.model.CatalogSpecies
import com.dndcharacterhandler.domain.model.CatalogSpellRef
import com.dndcharacterhandler.domain.model.CatalogSpellcasting
import com.dndcharacterhandler.domain.model.CatalogSubclass
import com.dndcharacterhandler.domain.model.CatalogText
import com.dndcharacterhandler.domain.model.CatalogTrait
import com.dndcharacterhandler.domain.model.CatalogUses
import com.dndcharacterhandler.domain.model.CharacterCatalog
import com.dndcharacterhandler.domain.model.ChoiceRestriction
import com.dndcharacterhandler.domain.model.ClassRestriction
import com.dndcharacterhandler.domain.model.TraitChoice
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.longOrNull

/** Reads character_catalog.json, the catalog tools/foundry_catalog/convert.py builds. */
object CharacterCatalogParser {
    fun parse(json: String): CharacterCatalog {
        val root = parseJsonObject(json)
        return CharacterCatalog(
            classes = root.objects("classes").map(::parseClass),
            subclasses = root.objects("subclasses").map(::parseSubclass),
            species = root.objects("species").map(::parseSpecies),
            backgrounds = root.objects("backgrounds").map(::parseBackground),
            features = root.objects("features").map(::parseFeature),
            spells = root.optObject("spells").entries { id, value ->
                CatalogSpellRef(
                    id = id,
                    name = value.text("name"),
                    level = value.optIntOrNull("level"),
                    school = value.optString("school"),
                    book = value.optString("book"),
                    lists = value.strings("lists"),
                    components = value.optString("components"),
                    material = value.text("material"),
                    castingTime = value.optString("castingTime"),
                    range = value.optString("range"),
                    duration = value.optString("duration"),
                    ritual = value.optBoolean("ritual"),
                    concentration = value.optBoolean("concentration"),
                    text = value.text("text"),
                    higher = value.text("higher"),
                    materialCost = value.optString("materialCost"),
                    attackType = value.optString("attackType"),
                    saveAbility = value.optString("saveAbility"),
                    saveEffect = value.optString("saveEffect"),
                    damageBase = value.optString("damageBase"),
                    damageBonus = value.optString("damageBonus"),
                    damageType = value.optString("damageType"),
                    altDamageBase = value.optString("altDamageBase"),
                    altDamageBonus = value.optString("altDamageBonus"),
                    altDamageType = value.optString("altDamageType"),
                    damage = value.optString("damage"),
                    healBase = value.optString("healBase"),
                    healBonus = value.optString("healBonus"),
                    healing = value.optString("healing"),
                    areaOfEffect = value.optString("areaOfEffect")
                )
            },
            books = root.optObject("books").entries { _, value -> value.asText() },
            subtypes = root.optObject("subtypes").entries { _, value -> value.asText() },
            legacyIds = root.optObject("legacyIds").let { legacy ->
                buildMap { legacy?.keys?.forEach { key -> put(key, legacy.getString(key)) } }
            },
            traits = root.optObject("traits")?.optObject("labels").entries { key, value ->
                CatalogTrait(key, value.text("name"), value.strings("children"))
            },
            traitCategories = root.optObject("traits")?.optObject("categories").entries { _, value -> value.asText() },
            equipment = root.optObject("equipment").entries { id, value ->
                CatalogEquipmentRef(id, value.text("name"), value.optString("type"), value.optDouble("weight").takeIf { !it.isNaN() })
            },
            containers = root.optObject("containers").entries { id, value ->
                val price = value.optObject("price")
                CatalogContainer(
                    id = id,
                    capacity = value.optDouble("capacity").takeIf { !it.isNaN() },
                    weightlessContents = value.optBoolean("weightless"),
                    weight = value.optDouble("weight", 0.0),
                    price = price?.optDouble("value")?.takeIf { !it.isNaN() && it % 1.0 == 0.0 }?.toInt(),
                    priceUnit = price?.optString("unit")?.ifBlank { null },
                    text = value.text("text"),
                    inside = value.optString("inside").ifBlank { null },
                    contents = value.objects("contents").map { CatalogContainerItem(it.getString("item"), it.optInt("count", 1)) }
                )
            },
            weaponMasteries = root.optObject("weaponMasteries")?.optObject("properties").entries { id, value ->
                CatalogWeaponMastery(id, value.text("name"), value.text("text"))
            },
            weaponMasteryOf = root.optObject("weaponMasteries")?.optObject("weapons").let { weapons ->
                buildMap { weapons?.keys?.forEach { key -> put(key, weapons.getString(key)) } }
            }
        )
    }

    private fun parseClass(json: JsonObject) = CatalogClass(
        id = json.getString("id"),
        identifier = json.optString("identifier"),
        name = json.text("name"),
        book = json.optString("book"),
        hitDie = json.optInt("hitDie", 8),
        primaryAbilities = json.strings("primaryAbilities"),
        primaryAbilitiesAll = json.optBoolean("primaryAbilitiesAll"),
        spellcasting = json.spellcasting(),
        advancement = json.advancement(),
        startingEquipment = json.equipmentNodes(),
        wealth = json.optString("wealth")
    )

    private fun parseSubclass(json: JsonObject) = CatalogSubclass(
        id = json.getString("id"),
        identifier = json.optString("identifier"),
        classIdentifier = json.optString("classIdentifier"),
        name = json.text("name"),
        text = json.text("text"),
        book = json.optString("book"),
        spellcasting = json.spellcasting(),
        advancement = json.advancement()
    )

    private fun parseSpecies(json: JsonObject) = CatalogSpecies(
        id = json.getString("id"),
        identifier = json.optString("identifier"),
        name = json.text("name"),
        text = json.text("text"),
        book = json.optString("book"),
        movement = json.numbers("movement"),
        senses = json.numbers("senses"),
        advancement = json.advancement()
    )

    private fun parseBackground(json: JsonObject) = CatalogBackground(
        id = json.getString("id"),
        identifier = json.optString("identifier"),
        name = json.text("name"),
        text = json.text("text"),
        book = json.optString("book"),
        advancement = json.advancement(),
        startingEquipment = json.equipmentNodes(),
        wealth = json.optString("wealth")
    )

    private fun JsonObject.equipmentNodes(key: String = "startingEquipment"): List<EquipmentNode> =
        objects(key).mapNotNull { node ->
            when (node.optString("type")) {
                "OR" -> EquipmentNode.Group(any = true, children = node.equipmentNodes("children"))
                "AND" -> EquipmentNode.Group(any = false, children = node.equipmentNodes("children"))
                "item" -> EquipmentNode.Item(node.optString("item"), node.optInt("count", 1).coerceAtLeast(1))
                "currency" -> EquipmentNode.Currency(node.optString("currency", "gp"), node.optInt("count", 1))
                "category" -> EquipmentNode.Category(node.optString("category"), node.optString("key"), node.optInt("count", 1))
                else -> null
            }
        }

    private fun parseFeature(json: JsonObject) = CatalogFeature(
        id = json.getString("id"),
        identifier = json.optString("identifier"),
        name = json.text("name"),
        text = json.text("text"),
        book = json.optString("book"),
        kind = CatalogFeatureKind.of(json.optString("kind")),
        type = json.optString("type"),
        featureType = json.optString("featureType"),
        subtype = json.optString("subtype"),
        level = json.optIntOrNull("level"),
        requirements = json.optString("requirements"),
        prerequisiteLevel = json.optIntOrNull("prerequisiteLevel"),
        repeatable = json.optBoolean("repeatable"),
        uses = json.optObject("uses")?.let { uses ->
            CatalogUses(
                max = uses.optString("max"),
                recovery = uses.objects("recovery").map { recovery ->
                    CatalogRecovery(
                        period = recovery.optString("period"),
                        type = recovery.optString("type"),
                        formula = recovery.optString("formula").ifBlank { null }
                    )
                }
            )
        },
        activation = json.optString("activation").ifBlank { null },
        grantedBy = json.objects("grantedBy").map { grant ->
            CatalogGrant(
                ownerId = grant.getString("owner"),
                level = grant.optIntOrNull("level"),
                viaChoice = grant.optString("via") == "choice"
            )
        },
        advancement = json.advancement()
    )

    private fun parseStep(json: JsonObject): AdvancementStep {
        val id = json.optString("id")
        val level = json.optIntOrNull("level")
        val title = json.text("title")
        val restriction = when (json.optString("classRestriction")) {
            "primary" -> ClassRestriction.PRIMARY
            "secondary" -> ClassRestriction.SECONDARY
            else -> null
        }
        return when (val type = json.optString("type")) {
            "HitPoints" -> AdvancementStep.HitPoints(id, level, title, restriction)
            "Trait" -> AdvancementStep.Trait(
                id, level, title, restriction,
                mode = json.optString("mode", "default"),
                grants = json.strings("grants"),
                choices = json.objects("choices").map { TraitChoice(it.optInt("count", 1), it.strings("pool")) },
                allowReplacements = json.optBoolean("allowReplacements"),
                hint = json.text("hint")
            )
            "ItemGrant" -> AdvancementStep.ItemGrant(
                id, level, title, restriction,
                items = json.strings("items"),
                spells = json.strings("spells"),
                optional = json.optBoolean("optional")
            )
            "ItemChoice" -> AdvancementStep.ItemChoice(
                id, level, title, restriction,
                counts = json.optObject("counts").let { counts ->
                    buildMap {
                        counts?.keys?.forEach { key -> key.toIntOrNull()?.let { put(it, counts.getInt(key)) } }
                    }
                },
                replacementLevels = json.optArray("replacementLevels").ints().toSet(),
                items = json.strings("items"),
                spells = json.strings("spells"),
                itemType = json.optString("itemType"),
                spellPrepared = json.optObject("spell")?.optInt("prepared") ?: 0,
                restriction = json.optObject("restriction")?.let {
                    ChoiceRestriction(
                        type = it.optString("type"),
                        subtype = it.optString("subtype"),
                        list = it.strings("list"),
                        level = it.optString("level")
                    )
                }
            )
            "ScaleValue" -> AdvancementStep.ScaleValue(
                id, level, title, restriction,
                identifier = json.optString("identifier"),
                scaleType = json.optString("scaleType"),
                values = json.optObject("values").let { values ->
                    buildMap {
                        values?.keys?.forEach { key ->
                            key.toIntOrNull()?.let { put(it, values.getValue(key).let(::scalarText)) }
                        }
                    }
                },
                units = json.optString("units")
            )
            "AbilityScoreImprovement" -> AdvancementStep.AbilityScoreImprovement(
                id, level, title, restriction,
                points = json.optInt("points"),
                cap = json.optIntOrNull("cap"),
                fixed = json.optObject("fixed").let { fixed ->
                    buildMap { fixed?.keys?.forEach { key -> put(key, fixed.getInt(key)) } }
                },
                locked = json.strings("locked").toSet(),
                recommendation = json.optString("recommendation").ifBlank { null }
            )
            "Subclass" -> AdvancementStep.Subclass(id, level, title, restriction)
            "Size" -> AdvancementStep.Size(id, level, title, restriction, json.strings("sizes"))
            else -> AdvancementStep.Other(id, level, title, restriction, type)
        }
    }

    /** "2", "1d10", "30": whole numbers without the ".0" of a double ("2.0" reads "2"). */
    private fun scalarText(value: JsonElement): String = when (val number = value.number()) {
        null -> value.stringValue()
        else -> number.let { if (it % 1.0 == 0.0) it.toLong().toString() else it.toString() }
    }

    private fun JsonObject.advancement(): List<AdvancementStep> = objects("advancement").map(::parseStep)

    private fun JsonObject.spellcasting(): CatalogSpellcasting? =
        optObject("spellcasting")?.let { CatalogSpellcasting(it.optString("progression"), it.optString("ability")) }

    private fun JsonObject.text(key: String): CatalogText = optObject(key)?.asText() ?: CatalogText()

    private fun JsonObject.asText() = CatalogText(en = optString("en"), ru = optString("ru"))

    /** Only a JSON number counts: text "12" gives null here (unlike the shared optIntOrNull). */
    private fun JsonObject.optIntOrNull(key: String): Int? =
        if (has(key) && !isNull(key)) optInt(key).takeIf { this[key]?.number() != null } else null

    /** Every element must be an object (an error otherwise), as org.json's getJSONObject. */
    private fun JsonObject.objects(key: String): List<JsonObject> {
        val array = optArray(key) ?: return emptyList()
        return List(array.size) { array[it].jsonObject }
    }

    private fun JsonObject.strings(key: String): List<String> {
        val array = optArray(key) ?: return emptyList()
        return List(array.size) { array[it].stringValue() }
    }

    private fun JsonArray?.ints(): List<Int> = if (this == null) emptyList() else List(size) { getInt(it) }

    private fun JsonObject.numbers(key: String): Map<String, Double> {
        val obj = optObject(key) ?: return emptyMap()
        return buildMap {
            obj.keys.forEach { name -> obj[name]?.number()?.let { put(name, it) } }
        }
    }

    private fun <T> JsonObject?.entries(transform: (String, JsonObject) -> T): Map<String, T> {
        val json = this ?: return emptyMap()
        // json.keys: a bare `keys` inside buildMap would be the keys of the map being built.
        return buildMap { json.keys.forEach { key -> json.optObject(key)?.let { put(key, transform(key, it)) } } }
    }

    /** A JSON number's value; null for text (even "12"), a flag, null, an object or an array. */
    private fun JsonElement.number(): Double? = (this as? JsonPrimitive)?.takeIf { !it.isString }?.doubleOrNull

    /** The value as text, as org.json's getString gave it: a number's digits, "null" for a null, an object's JSON. */
    private fun JsonElement.stringValue(): String = (this as? JsonPrimitive)?.content ?: toString()

    /** A whole number as org.json's getInt read it: a number (truncated) or a number given as text. */
    private fun JsonElement.intValue(): Int? =
        (this as? JsonPrimitive)?.let { it.longOrNull?.toInt() ?: it.doubleOrNull?.toInt() }

    /** Like org.json's getString: an error when [key] is missing. */
    private fun JsonObject.getString(key: String): String =
        (this[key] ?: error("No value for $key")).stringValue()

    /** Like org.json's getInt: an error when [key] is missing or isn't a number. */
    private fun JsonObject.getInt(key: String): Int =
        (this[key] ?: error("No value for $key")).intValue() ?: error("Value at $key is not an int")

    private fun JsonArray.getInt(index: Int): Int =
        this[index].intValue() ?: error("Value at $index is not an int")
}
