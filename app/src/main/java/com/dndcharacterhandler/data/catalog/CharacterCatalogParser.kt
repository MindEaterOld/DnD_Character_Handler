package com.dndcharacterhandler.data.catalog

import com.dndcharacterhandler.domain.model.AdvancementStep
import com.dndcharacterhandler.domain.model.CatalogBackground
import com.dndcharacterhandler.domain.model.CatalogClass
import com.dndcharacterhandler.domain.model.CatalogEquipmentRef
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
import org.json.JSONArray
import org.json.JSONObject

/** Reads character_catalog.json, the catalog tools/foundry_catalog/convert.py builds. */
object CharacterCatalogParser {
    fun parse(json: String): CharacterCatalog {
        val root = JSONObject(json)
        return CharacterCatalog(
            classes = root.objects("classes").map(::parseClass),
            subclasses = root.objects("subclasses").map(::parseSubclass),
            species = root.objects("species").map(::parseSpecies),
            backgrounds = root.objects("backgrounds").map(::parseBackground),
            features = root.objects("features").map(::parseFeature),
            spells = root.optJSONObject("spells").entries { id, value ->
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
            spellLegacyIds = root.optJSONObject("spellLegacyIds").let { legacy ->
                buildMap { legacy?.keys()?.forEach { key -> put(key, legacy.getString(key)) } }
            },
            books = root.optJSONObject("books").entries { _, value -> value.asText() },
            subtypes = root.optJSONObject("subtypes").entries { _, value -> value.asText() },
            legacyIds = root.optJSONObject("legacyIds").let { legacy ->
                buildMap { legacy?.keys()?.forEach { key -> put(key, legacy.getString(key)) } }
            },
            traits = root.optJSONObject("traits")?.optJSONObject("labels").entries { key, value ->
                CatalogTrait(key, value.text("name"), value.strings("children"))
            },
            traitCategories = root.optJSONObject("traits")?.optJSONObject("categories").entries { _, value -> value.asText() },
            equipment = root.optJSONObject("equipment").entries { id, value ->
                CatalogEquipmentRef(id, value.text("name"), value.optString("type"))
            }
        )
    }

    private fun parseClass(json: JSONObject) = CatalogClass(
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

    private fun parseSubclass(json: JSONObject) = CatalogSubclass(
        id = json.getString("id"),
        identifier = json.optString("identifier"),
        classIdentifier = json.optString("classIdentifier"),
        name = json.text("name"),
        text = json.text("text"),
        book = json.optString("book"),
        spellcasting = json.spellcasting(),
        advancement = json.advancement()
    )

    private fun parseSpecies(json: JSONObject) = CatalogSpecies(
        id = json.getString("id"),
        identifier = json.optString("identifier"),
        name = json.text("name"),
        text = json.text("text"),
        book = json.optString("book"),
        movement = json.numbers("movement"),
        senses = json.numbers("senses"),
        advancement = json.advancement()
    )

    private fun parseBackground(json: JSONObject) = CatalogBackground(
        id = json.getString("id"),
        identifier = json.optString("identifier"),
        name = json.text("name"),
        text = json.text("text"),
        book = json.optString("book"),
        advancement = json.advancement(),
        startingEquipment = json.equipmentNodes(),
        wealth = json.optString("wealth")
    )

    private fun JSONObject.equipmentNodes(key: String = "startingEquipment"): List<EquipmentNode> =
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

    private fun parseFeature(json: JSONObject) = CatalogFeature(
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
        uses = json.optJSONObject("uses")?.let { uses ->
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

    private fun parseStep(json: JSONObject): AdvancementStep {
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
                counts = json.optJSONObject("counts").let { counts ->
                    buildMap {
                        counts?.keys()?.forEach { key -> key.toIntOrNull()?.let { put(it, counts.getInt(key)) } }
                    }
                },
                replacementLevels = json.optJSONArray("replacementLevels").ints().toSet(),
                items = json.strings("items"),
                spells = json.strings("spells"),
                itemType = json.optString("itemType"),
                spellPrepared = json.optJSONObject("spell")?.optInt("prepared") ?: 0,
                restriction = json.optJSONObject("restriction")?.let {
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
                values = json.optJSONObject("values").let { values ->
                    buildMap {
                        values?.keys()?.forEach { key ->
                            key.toIntOrNull()?.let { put(it, values.get(key).let(::scalarText)) }
                        }
                    }
                },
                units = json.optString("units")
            )
            "AbilityScoreImprovement" -> AdvancementStep.AbilityScoreImprovement(
                id, level, title, restriction,
                points = json.optInt("points"),
                cap = json.optIntOrNull("cap"),
                fixed = json.optJSONObject("fixed").let { fixed ->
                    buildMap { fixed?.keys()?.forEach { key -> put(key, fixed.getInt(key)) } }
                },
                locked = json.strings("locked").toSet(),
                recommendation = json.optString("recommendation").ifBlank { null }
            )
            "Subclass" -> AdvancementStep.Subclass(id, level, title, restriction)
            "Size" -> AdvancementStep.Size(id, level, title, restriction, json.strings("sizes"))
            else -> AdvancementStep.Other(id, level, title, restriction, type)
        }
    }

    /** "2", "1d10", "30": whole numbers without the ".0" org.json gives doubles. */
    private fun scalarText(value: Any): String = when (value) {
        is Number -> value.toDouble().let { if (it % 1.0 == 0.0) it.toLong().toString() else it.toString() }
        else -> value.toString()
    }

    private fun JSONObject.advancement(): List<AdvancementStep> = objects("advancement").map(::parseStep)

    private fun JSONObject.spellcasting(): CatalogSpellcasting? =
        optJSONObject("spellcasting")?.let { CatalogSpellcasting(it.optString("progression"), it.optString("ability")) }

    private fun JSONObject.text(key: String): CatalogText = optJSONObject(key)?.asText() ?: CatalogText()

    private fun JSONObject.asText() = CatalogText(en = optString("en"), ru = optString("ru"))

    private fun JSONObject.optIntOrNull(key: String): Int? =
        if (has(key) && !isNull(key)) optInt(key).takeIf { opt(key) is Number } else null

    private fun JSONObject.objects(key: String): List<JSONObject> {
        val array = optJSONArray(key) ?: return emptyList()
        return List(array.length()) { array.getJSONObject(it) }
    }

    private fun JSONObject.strings(key: String): List<String> {
        val array = optJSONArray(key) ?: return emptyList()
        return List(array.length()) { array.getString(it) }
    }

    private fun JSONArray?.ints(): List<Int> = if (this == null) emptyList() else List(length()) { getInt(it) }

    private fun JSONObject.numbers(key: String): Map<String, Double> {
        val obj = optJSONObject(key) ?: return emptyMap()
        return buildMap {
            obj.keys().forEach { name -> (obj.opt(name) as? Number)?.let { put(name, it.toDouble()) } }
        }
    }

    private fun <T> JSONObject?.entries(transform: (String, JSONObject) -> T): Map<String, T> {
        if (this == null) return emptyMap()
        return buildMap { keys().forEach { key -> optJSONObject(key)?.let { put(key, transform(key, it)) } } }
    }
}
