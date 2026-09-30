package com.dndcharacterhandler.data.repository

import androidx.core.net.toUri
import com.dndcharacterhandler.domain.model.Attack
import com.dndcharacterhandler.domain.model.AttackCalculationMode
import com.dndcharacterhandler.domain.model.DarkvisionMode
import com.dndcharacterhandler.domain.model.ArmorClassMode
import com.dndcharacterhandler.domain.model.Character
import com.dndcharacterhandler.domain.model.CharacterBundle
import com.dndcharacterhandler.domain.model.CombatResource
import com.dndcharacterhandler.domain.model.Feature
import com.dndcharacterhandler.domain.model.FeatureSource
import com.dndcharacterhandler.domain.model.InventoryArmorDetails
import com.dndcharacterhandler.domain.model.InventoryArmorType
import com.dndcharacterhandler.domain.model.InventoryCategory
import com.dndcharacterhandler.domain.model.InventoryItem
import com.dndcharacterhandler.domain.model.InventoryWeaponClass
import com.dndcharacterhandler.domain.model.InventoryWeaponDamage
import com.dndcharacterhandler.domain.model.InventoryWeaponDetails
import com.dndcharacterhandler.domain.model.InventoryWeaponProperty
import com.dndcharacterhandler.domain.model.InventoryWeaponRangeType
import com.dndcharacterhandler.domain.model.Note
import com.dndcharacterhandler.domain.model.Skill
import com.dndcharacterhandler.domain.model.Spell
import com.dndcharacterhandler.domain.model.SpellcastingAbility
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

// 18: armor/shield magicalBonus is meaningful (adds to AC).
private const val SCHEMA_VERSION = 19

data class ImportedArchive(
    val characterBundle: CharacterBundle,
    val characterName: String
)

fun CharacterBundle.toArchiveManifest(
    exportedAt: Long,
    mapAssetReference: (String?, String) -> String?
): JSONObject {
    val characterObject = JSONObject().apply {
        put("id", character.id)
        put("name", character.name)
        put("race", character.race)
        put("characterClass", character.characterClass)
        put("subclass", character.subclass)
        put("level", character.level)
        put("portraitUri", mapAssetReference(character.portraitUri, "portrait"))
        put("currentHp", character.currentHp)
        put("maxHp", character.maxHp)
        put("temporaryHp", character.temporaryHp)
        put("hitDieSides", character.hitDieSides)
        put("spentHitDice", character.spentHitDice)
        put("hasInspiration", character.hasInspiration)
        put("armorClass", character.armorClass)
        put("baseArmorClass", character.baseArmorClass)
        put("armorClassMode", character.armorClassMode.name)
        put("copperPieces", character.copperPieces)
        put("silverPieces", character.silverPieces)
        put("goldPieces", character.goldPieces)
        put("speed", character.speed)
        put("initiative", character.initiative)
        put("initiativeBonus", character.initiativeBonus)
        put("spellcastingAbility", character.spellcastingAbility.name)
        put("spellSlotMaximums", character.spellSlotMaximums)
        put("spellSlotRemaining", character.spellSlotRemaining)
        put("spellSlotsRestoreOnShortRest", character.spellSlotsRestoreOnShortRest)
        put("spellSlotsRestoreOnLongRest", character.spellSlotsRestoreOnLongRest)
        put("experience", character.experience)
        put("strength", character.strength)
        put("dexterity", character.dexterity)
        put("constitution", character.constitution)
        put("intelligence", character.intelligence)
        put("wisdom", character.wisdom)
        put("charisma", character.charisma)
        put("strengthSaveProficient", character.strengthSaveProficient)
        put("dexteritySaveProficient", character.dexteritySaveProficient)
        put("constitutionSaveProficient", character.constitutionSaveProficient)
        put("intelligenceSaveProficient", character.intelligenceSaveProficient)
        put("wisdomSaveProficient", character.wisdomSaveProficient)
        put("charismaSaveProficient", character.charismaSaveProficient)
        put("passivePerceptionBonus", character.passivePerceptionBonus)
        put("darkvisionMode", character.darkvisionMode.name)
        put("darkvisionManualFeet", character.darkvisionManualFeet)
        put("armorProficiencies", character.armorProficiencies)
        put("weaponProficiencies", character.weaponProficiencies)
        put("toolProficiencies", character.toolProficiencies)
        put("languageProficiencies", character.languageProficiencies)
        put("alignment", character.alignment)
        put("background", character.background)
        put("faith", character.faith)
        put("homeland", character.homeland)
        put("age", character.age)
        put("gender", character.gender)
        put("height", character.height)
        put("weight", character.weight)
        put("eyes", character.eyes)
        put("hair", character.hair)
        put("skin", character.skin)
        put("personalityTraits", character.personalityTraits)
        put("ideals", character.ideals)
        put("bonds", character.bonds)
        put("flaws", character.flaws)
        put("biography", character.biography)
        put("createdAt", character.createdAt)
        put("updatedAt", character.updatedAt)
        put("classes", ProgressionJson.classesToJson(character.classes))
        put("advancements", ProgressionJson.advancementsToJson(character.advancements))
    }

    return JSONObject().apply {
        put("schemaVersion", SCHEMA_VERSION)
        put("exportedAt", exportedAt)
        put("character", characterObject)
        put("skills", JSONArray(skills.map { skill ->
            JSONObject().apply {
                put("name", skill.name)
                put("isProficient", skill.isProficient)
                put("isExpertise", skill.isExpertise)
                put("hasJackOfAllTrades", skill.hasJackOfAllTrades)
            }
        }))
        put("attacks", JSONArray(attacks.mapIndexed { index, attack ->
            JSONObject().apply {
                put("name", attack.name)
                put("icon", mapAssetReference(attack.icon, "attack_${index}_${slugify(attack.name)}"))
                put("isProficient", attack.isProficient)
                put("calculationMode", attack.calculationMode.name)
                put("ability", attack.ability.name)
                put("normalRange", attack.normalRange)
                put("longRange", attack.longRange)
                put("damageDiceCount", attack.damageDiceCount)
                put("damageDieType", attack.damageDieType)
                put("alternateDamageDiceCount", attack.alternateDamageDiceCount)
                put("alternateDamageDieType", attack.alternateDamageDieType)
                put("alternateDamageType", attack.alternateDamageType)
                put("magicalBonus", attack.magicalBonus)
                put("applyAbilityModifierToDamage", attack.applyAbilityModifierToDamage)
                put("attackBonusOrSaveDc", attack.manualAttackBonusOrSaveDc)
                put("damage", attack.manualDamage)
                put("damageType", attack.primaryDamageType)
            }
        }))
        put("combatResources", JSONArray(combatResources.map { resource ->
            JSONObject().apply {
                put("name", resource.name)
                put("currentUses", resource.currentUses)
                put("maximumUses", resource.maximumUses)
                put("restoresOnShortRest", resource.restoresOnShortRest)
                put("restoresOnLongRest", resource.restoresOnLongRest)
                resource.catalogId?.let { put("catalogId", it) }
            }
        }))
        put("inventoryItems", JSONArray(inventoryItems.mapIndexed { index, item ->
            JSONObject().apply {
                put("name", item.name)
                put("description", item.description)
                put("isMagical", item.isMagical)
                put("magicalBonus", item.magicalBonus)
                put("category", item.category.name)
                put("weight", item.weight)
                put("quantity", item.quantity)
                put("isEquipped", item.isEquipped)
                put("icon", mapAssetReference(item.icon, "inventory_${index}_${slugify(item.name)}"))
                put("costQuantity", item.costQuantity)
                put("costUnit", item.costUnit)
                put("armorDetails", item.armorDetails?.toJson())
                put("weaponDetails", item.weaponDetails?.toJson())
                // Optional and additive: older app versions just ignore it.
                item.catalogId?.let { put("catalogId", it) }
            }
        }))
        put("spells", JSONArray(spells.map { spell ->
            JSONObject().apply {
                put("catalogId", spell.catalogId)
                put("name", spell.name)
                put("level", spell.level)
                put("school", spell.school)
                put("isPrepared", spell.isPrepared)
                put("description", spell.description)
                put("higherLevelDescription", spell.higherLevelDescription)
                put("range", spell.range)
                put("castingTime", spell.castingTime)
                put("duration", spell.duration)
                put("components", spell.components)
                put("material", spell.material)
                put("materialCost", spell.materialCost)
                put("isRitual", spell.isRitual)
                put("requiresConcentration", spell.requiresConcentration)
                put("attackType", spell.attackType)
                put("availableClasses", spell.availableClasses)
                put("damageType", spell.damageType)
                put("damageBase", spell.damageBase)
                put("damageBonusValue", spell.damageBonusValue)
                put("damageBonusIsModifier", spell.damageBonusIsModifier)
                put("altDamageBase", spell.altDamageBase)
                put("altDamageType", spell.altDamageType)
                put("altDamageBonusValue", spell.altDamageBonusValue)
                put("altDamageBonusIsModifier", spell.altDamageBonusIsModifier)
                put("damage", spell.damage)
                put("saveAbility", spell.saveAbility)
                put("saveEffect", spell.saveEffect)
                put("areaOfEffect", spell.areaOfEffect)
                put("healBase", spell.healBase)
                put("healBonusValue", spell.healBonusValue)
                put("healBonusIsModifier", spell.healBonusIsModifier)
                put("healing", spell.healing)
            }
        }))
        put("spellAttacks", JSONArray(spellAttacks.map { spell ->
            JSONObject().apply {
                put("catalogId", spell.catalogId)
                put("name", spell.name)
                put("level", spell.level)
                put("school", spell.school)
                put("isPrepared", spell.isPrepared)
                put("description", spell.description)
                put("higherLevelDescription", spell.higherLevelDescription)
                put("range", spell.range)
                put("castingTime", spell.castingTime)
                put("duration", spell.duration)
                put("components", spell.components)
                put("material", spell.material)
                put("materialCost", spell.materialCost)
                put("isRitual", spell.isRitual)
                put("requiresConcentration", spell.requiresConcentration)
                put("attackType", spell.attackType)
                put("availableClasses", spell.availableClasses)
                put("damageType", spell.damageType)
                put("damageBase", spell.damageBase)
                put("damageBonusValue", spell.damageBonusValue)
                put("damageBonusIsModifier", spell.damageBonusIsModifier)
                put("altDamageBase", spell.altDamageBase)
                put("altDamageType", spell.altDamageType)
                put("altDamageBonusValue", spell.altDamageBonusValue)
                put("altDamageBonusIsModifier", spell.altDamageBonusIsModifier)
                put("damage", spell.damage)
                put("saveAbility", spell.saveAbility)
                put("saveEffect", spell.saveEffect)
                put("areaOfEffect", spell.areaOfEffect)
                put("healBase", spell.healBase)
                put("healBonusValue", spell.healBonusValue)
                put("healBonusIsModifier", spell.healBonusIsModifier)
                put("healing", spell.healing)
            }
        }))
        put("features", JSONArray(features.map { feature ->
            JSONObject().apply {
                put("name", feature.name)
                put("description", feature.description)
                put("level", feature.level)
                put("source", feature.source.name)
                put("category", feature.category)
                // Optional and additive: older app versions just ignore it.
                feature.catalogId?.let { put("catalogId", it) }
            }
        }))
        put("notes", JSONArray(notes.map { note ->
            JSONObject().apply {
                put("title", note.title)
                put("createdDate", note.createdDate)
                put("updatedDate", note.updatedDate)
                put("content", note.content)
                put("isPinned", note.isPinned)
            }
        }))
    }
}

fun archiveManifestToCharacterBundle(
    manifest: JSONObject,
    resolveAssetReference: (String?) -> String?
): ImportedArchive {
    val schemaVersion = manifest.optInt("schemaVersion", SCHEMA_VERSION)
    require(schemaVersion in 1..SCHEMA_VERSION) {
        "Unsupported character archive schema version."
    }

    val characterJson = manifest.getJSONObject("character")
    // Guard against corrupt/hand-edited archives: optInt coerces non-numeric values to 0, which
    // would import as level 0 / 0 max HP and let spentHitDice exceed the level.
    val importedLevel = characterJson.optInt("level").coerceIn(1, 20)
    val character = Character(
        id = 0,
        name = characterJson.optString("name"),
        race = characterJson.optString("race"),
        characterClass = characterJson.optString("characterClass"),
        subclass = characterJson.optString("subclass"),
        level = importedLevel,
        portraitUri = resolveAssetReference(characterJson.optNullableString("portraitUri")),
        currentHp = characterJson.optInt("currentHp"),
        maxHp = characterJson.optInt("maxHp").coerceAtLeast(1),
        temporaryHp = characterJson.optInt("temporaryHp").coerceAtLeast(0),
        hitDieSides = characterJson.optInt("hitDieSides", 8).coerceInHitDieSides(),
        spentHitDice = characterJson.optInt("spentHitDice").coerceIn(0, importedLevel),
        hasInspiration = characterJson.optBoolean("hasInspiration"),
        armorClass = characterJson.optInt("armorClass"),
        baseArmorClass = characterJson.optInt("baseArmorClass", 10).coerceAtLeast(1),
        armorClassMode = characterJson.optString("armorClassMode")
            .toEnumOrDefault(ArmorClassMode.AUTOMATIC),
        copperPieces = characterJson.optInt("copperPieces").coerceAtLeast(0),
        silverPieces = characterJson.optInt("silverPieces").coerceAtLeast(0),
        goldPieces = characterJson.optInt("goldPieces").coerceAtLeast(0),
        speed = characterJson.optInt("speed"),
        initiative = characterJson.optInt("initiative"),
        initiativeBonus = characterJson.optInt("initiativeBonus"),
        spellcastingAbility = characterJson.optString("spellcastingAbility")
            .takeIf { it.isNotBlank() }
            ?.let { runCatching { com.dndcharacterhandler.domain.model.SpellcastingAbility.valueOf(it) }.getOrDefault(com.dndcharacterhandler.domain.model.SpellcastingAbility.WISDOM) }
            ?: com.dndcharacterhandler.domain.model.SpellcastingAbility.WISDOM,
        spellSlotMaximums = characterJson.optString("spellSlotMaximums"),
        spellSlotRemaining = characterJson.optString("spellSlotRemaining"),
        spellSlotsRestoreOnShortRest = characterJson.optBoolean("spellSlotsRestoreOnShortRest", false),
        spellSlotsRestoreOnLongRest = characterJson.optBoolean("spellSlotsRestoreOnLongRest", true),
        experience = characterJson.optInt("experience"),
        strength = characterJson.optInt("strength"),
        dexterity = characterJson.optInt("dexterity"),
        constitution = characterJson.optInt("constitution"),
        intelligence = characterJson.optInt("intelligence"),
        wisdom = characterJson.optInt("wisdom"),
        charisma = characterJson.optInt("charisma"),
        strengthSaveProficient = characterJson.optBoolean("strengthSaveProficient"),
        dexteritySaveProficient = characterJson.optBoolean("dexteritySaveProficient"),
        constitutionSaveProficient = characterJson.optBoolean("constitutionSaveProficient"),
        intelligenceSaveProficient = characterJson.optBoolean("intelligenceSaveProficient"),
        wisdomSaveProficient = characterJson.optBoolean("wisdomSaveProficient"),
        charismaSaveProficient = characterJson.optBoolean("charismaSaveProficient"),
        passivePerceptionBonus = characterJson.optInt("passivePerceptionBonus"),
        darkvisionMode = characterJson.optString("darkvisionMode")
            .takeIf(String::isNotBlank)
            ?.let { runCatching { DarkvisionMode.valueOf(it) }.getOrDefault(DarkvisionMode.AUTO) }
            ?: DarkvisionMode.AUTO,
        darkvisionManualFeet = characterJson.optInt("darkvisionManualFeet"),
        armorProficiencies = characterJson.optString("armorProficiencies"),
        weaponProficiencies = characterJson.optString("weaponProficiencies"),
        toolProficiencies = characterJson.optString("toolProficiencies"),
        languageProficiencies = characterJson.optString("languageProficiencies"),
        alignment = characterJson.optString("alignment"),
        background = characterJson.optString("background"),
        faith = characterJson.optString("faith"),
        homeland = characterJson.optString("homeland"),
        age = characterJson.optString("age"),
        gender = characterJson.optString("gender"),
        height = characterJson.optString("height"),
        weight = characterJson.optString("weight"),
        eyes = characterJson.optString("eyes"),
        hair = characterJson.optString("hair"),
        skin = characterJson.optString("skin"),
        personalityTraits = characterJson.optString("personalityTraits"),
        ideals = characterJson.optString("ideals"),
        bonds = characterJson.optString("bonds"),
        flaws = characterJson.optString("flaws"),
        biography = characterJson.optString("biography"),
        createdAt = characterJson.optLong("createdAt"),
        updatedAt = characterJson.optLong("updatedAt"),
        classes = ProgressionJson.classesFromJson(characterJson.optJSONArray("classes")),
        advancements = ProgressionJson.advancementsFromJson(characterJson.optJSONArray("advancements"))
    )

    return ImportedArchive(
        characterName = character.name,
        characterBundle = CharacterBundle(
            character = character,
            skills = manifest.optJSONArray("skills")?.toSkillList().orEmpty(),
            attacks = manifest.optJSONArray("attacks")?.toAttackList(resolveAssetReference).orEmpty(),
            combatResources = manifest.optJSONArray("combatResources")?.toCombatResourceList().orEmpty(),
            inventoryItems = manifest.optJSONArray("inventoryItems")
                ?.toInventoryItemList(resolveAssetReference, schemaVersion)
                .orEmpty(),
            spells = manifest.optJSONArray("spells")?.toSpellList().orEmpty(),
            spellAttacks = manifest.optJSONArray("spellAttacks")?.toSpellList().orEmpty(),
            features = manifest.optJSONArray("features")?.toFeatureList().orEmpty(),
            notes = manifest.optJSONArray("notes")?.toNoteList().orEmpty()
        )
    )
}

private fun JSONArray.toSkillList(): List<Skill> =
    (0 until length()).map { index ->
        getJSONObject(index).let { json ->
            Skill(
                name = json.optString("name"),
                isProficient = json.optBoolean("isProficient"),
                isExpertise = json.optBoolean("isExpertise"),
                hasJackOfAllTrades = json.optBoolean("hasJackOfAllTrades")
            )
        }
    }

private fun JSONArray.toAttackList(resolveAssetReference: (String?) -> String?): List<Attack> =
    (0 until length()).map { index ->
        getJSONObject(index).let { json ->
            Attack(
                name = json.optString("name"),
                icon = resolveAssetReference(json.optNullableString("icon")).orEmpty(),
                isProficient = json.optBoolean("isProficient"),
                calculationMode = json.optString("calculationMode")
                    .takeIf(String::isNotBlank)
                    ?.let { runCatching { AttackCalculationMode.valueOf(it) }.getOrDefault(AttackCalculationMode.AUTOMATIC) }
                    ?: AttackCalculationMode.AUTOMATIC,
                ability = json.optString("ability")
                    .takeIf(String::isNotBlank)
                    ?.let { runCatching { SpellcastingAbility.valueOf(it) }.getOrDefault(SpellcastingAbility.STRENGTH) }
                    ?: SpellcastingAbility.STRENGTH,
                normalRange = json.optNullableInt("normalRange"),
                longRange = json.optNullableInt("longRange"),
                damageDiceCount = json.optInt("damageDiceCount", 1).coerceAtLeast(0),
                damageDieType = json.optString("damageDieType").ifBlank { "d4" },
                alternateDamageDiceCount = json.optNullableInt("alternateDamageDiceCount"),
                alternateDamageDieType = json.optNullableString("alternateDamageDieType"),
                alternateDamageType = json.optNullableString("alternateDamageType"),
                magicalBonus = json.optInt("magicalBonus", 0),
                applyAbilityModifierToDamage = json.optBoolean("applyAbilityModifierToDamage", true),
                manualAttackBonusOrSaveDc = json.optString("attackBonusOrSaveDc"),
                manualDamage = json.optString("damage"),
                primaryDamageType = json.optString("damageType")
            )
        }
    }

private fun JSONArray.toCombatResourceList(): List<CombatResource> =
    (0 until length()).map { index ->
        getJSONObject(index).let { json ->
            CombatResource(
                name = json.optString("name"),
                currentUses = json.optInt("currentUses"),
                maximumUses = json.optInt("maximumUses"),
                restoresOnShortRest = json.optBoolean("restoresOnShortRest", false),
                restoresOnLongRest = json.optBoolean("restoresOnLongRest", false),
                catalogId = json.optNullableString("catalogId")
            )
        }
    }

private fun JSONArray.toInventoryItemList(
    resolveAssetReference: (String?) -> String?,
    schemaVersion: Int
): List<InventoryItem> =
    (0 until length()).map { index ->
        getJSONObject(index).let { json ->
            val category = json.optString("category").toEnumOrDefault(InventoryCategory.OTHER)
            InventoryItem(
                name = json.optString("name"),
                description = json.optString("description"),
                isMagical = json.optBoolean("isMagical", false),
                // Before v18 armor's bonus was always saved as 1 without affecting AC; it counts now.
                magicalBonus = if (schemaVersion < 18 && category == InventoryCategory.ARMOR) {
                    0
                } else {
                    json.optInt("magicalBonus", 1)
                },
                category = category,
                weight = json.optDouble("weight"),
                quantity = json.optInt("quantity"),
                isEquipped = json.optBoolean("isEquipped"),
                icon = resolveAssetReference(json.optNullableString("icon")).orEmpty(),
                costQuantity = json.optNullableInt("costQuantity"),
                costUnit = json.optNullableString("costUnit"),
                armorDetails = json.optJSONObject("armorDetails")?.toArmorDetails(),
                weaponDetails = json.optJSONObject("weaponDetails")?.toWeaponDetails(),
                catalogId = json.optNullableString("catalogId")
            )
        }
    }

private fun InventoryArmorDetails.toJson(): JSONObject =
    JSONObject().apply {
        put("armorType", armorType.name)
        put("armorClass", armorClass)
        put("appliesDexterityBonus", appliesDexterityBonus)
        put("maxDexterityBonus", maxDexterityBonus)
        put("strengthMinimum", strengthMinimum)
        put("hasStealthDisadvantage", hasStealthDisadvantage)
    }

private fun JSONObject.toArmorDetails(): InventoryArmorDetails =
    InventoryArmorDetails(
        armorType = optString("armorType").toEnumOrDefault(InventoryArmorType.LIGHT),
        armorClass = optInt("armorClass"),
        appliesDexterityBonus = optBoolean("appliesDexterityBonus"),
        maxDexterityBonus = optNullableInt("maxDexterityBonus"),
        strengthMinimum = optInt("strengthMinimum"),
        hasStealthDisadvantage = optBoolean("hasStealthDisadvantage")
    )

private fun InventoryWeaponDetails.toJson(): JSONObject =
    JSONObject().apply {
        put("weaponClass", weaponClass.name)
        put("rangeType", rangeType.name)
        put("baseWeaponId", baseWeaponId)
        put("normalRange", normalRange)
        put("longRange", longRange)
        put("damages", JSONArray(damages.map { damage ->
            JSONObject().apply {
                put("dice", damage.dice)
                put("damageType", damage.damageType)
            }
        }))
        put("twoHandedDamage", twoHandedDamage?.let { damage ->
            JSONObject().apply {
                put("dice", damage.dice)
                put("damageType", damage.damageType)
            }
        })
        put("properties", JSONArray(properties.map(InventoryWeaponProperty::name)))
    }

private fun JSONObject.toWeaponDetails(): InventoryWeaponDetails =
    InventoryWeaponDetails(
        weaponClass = optString("weaponClass").toEnumOrDefault(InventoryWeaponClass.SIMPLE),
        rangeType = optString("rangeType").toEnumOrDefault(InventoryWeaponRangeType.MELEE),
        baseWeaponId = optNullableString("baseWeaponId"),
        normalRange = optNullableInt("normalRange"),
        longRange = optNullableInt("longRange"),
        damages = optJSONArray("damages")?.toWeaponDamageList().orEmpty(),
        twoHandedDamage = optJSONObject("twoHandedDamage")?.toWeaponDamage(),
        properties = optJSONArray("properties")
            ?.let { array ->
                (0 until array.length()).mapNotNull { index ->
                    array.optString(index)
                        .takeIf { it.isNotBlank() }
                        ?.let { runCatching { InventoryWeaponProperty.valueOf(it) }.getOrNull() }
                }.toSet()
            }
            ?: emptySet()
    )

private fun JSONArray.toWeaponDamageList(): List<InventoryWeaponDamage> =
    (0 until length()).map { index ->
        getJSONObject(index).toWeaponDamage()
    }

private fun JSONObject.toWeaponDamage(): InventoryWeaponDamage =
    InventoryWeaponDamage(
        dice = optString("dice"),
        damageType = optString("damageType")
    )

private fun JSONArray.toSpellList(): List<Spell> =
    (0 until length()).map { index ->
        getJSONObject(index).let { json ->
            Spell(
                catalogId = json.optNullableString("catalogId"),
                name = json.optString("name"),
                level = json.optInt("level"),
                school = json.optString("school"),
                isPrepared = json.optBoolean("isPrepared"),
                description = json.optString("description"),
                higherLevelDescription = json.optString("higherLevelDescription"),
                range = json.optString("range"),
                castingTime = json.optString("castingTime"),
                duration = json.optString("duration"),
                components = json.optString("components"),
                material = json.optString("material"),
                materialCost = json.optString("materialCost"),
                isRitual = json.optBoolean("isRitual", false),
                requiresConcentration = json.optBoolean("requiresConcentration", false),
                attackType = json.optString("attackType"),
                availableClasses = json.optString("availableClasses"),
                damageType = json.optString("damageType"),
                damageBase = json.optString("damageBase"),
                damageBonusValue = json.optInt("damageBonusValue", 0),
                damageBonusIsModifier = json.optBoolean("damageBonusIsModifier", false),
                altDamageBase = json.optString("altDamageBase"),
                altDamageType = json.optString("altDamageType"),
                altDamageBonusValue = json.optInt("altDamageBonusValue", 0),
                altDamageBonusIsModifier = json.optBoolean("altDamageBonusIsModifier", false),
                damage = json.optString("damage"),
                saveAbility = json.optString("saveAbility"),
                saveEffect = json.optString("saveEffect"),
                areaOfEffect = json.optString("areaOfEffect"),
                healBase = json.optString("healBase"),
                healBonusValue = json.optInt("healBonusValue", 0),
                healBonusIsModifier = json.optBoolean("healBonusIsModifier", false),
                healing = json.optString("healing")
            )
        }
    }

private fun JSONArray.toFeatureList(): List<Feature> =
    (0 until length()).map { index ->
        getJSONObject(index).let { json ->
            Feature(
                name = json.optString("name"),
                description = json.optString("description"),
                level = json.optNullableInt("level")
                    ?: json.optNullableString("resourceTracking")
                        ?.substringBefore('/')
                        ?.trim()
                        ?.toIntOrNull(),
                source = json.optString("source")
                    .takeIf { it.isNotBlank() }
                    ?.let { runCatching { FeatureSource.valueOf(it) }.getOrDefault(FeatureSource.OTHER) }
                    ?: FeatureSource.OTHER,
                category = json.optString("category"),
                catalogId = json.optNullableString("catalogId")
            )
        }
    }

private fun JSONArray.toNoteList(): List<Note> =
    (0 until length()).map { index ->
        getJSONObject(index).let { json ->
            Note(
                title = json.optString("title"),
                createdDate = json.optLong("createdDate"),
                updatedDate = json.optLong("updatedDate"),
                content = json.optString("content"),
                isPinned = json.optBoolean("isPinned", false)
            )
        }
    }

private fun JSONObject.optNullableString(key: String): String? {
    return if (isNull(key)) null else optString(key).ifBlank { null }
}

private fun JSONObject.optNullableInt(key: String): Int? {
    return if (isNull(key) || !has(key)) null else optInt(key)
}

private fun Int.coerceInHitDieSides(): Int {
    return if (this in listOf(6, 8, 10, 12)) this else 8
}

private inline fun <reified T : Enum<T>> String.toEnumOrDefault(default: T): T =
    takeIf { it.isNotBlank() }
        ?.let { runCatching { enumValueOf<T>(it) }.getOrDefault(default) }
        ?: default

fun resolveImportedAssetReference(rawValue: String?, extractedAssets: Map<String, File>): String? {
    if (rawValue.isNullOrBlank()) return null
    return if (rawValue.startsWith("assets/")) {
        extractedAssets[rawValue]?.toUri()?.toString()
    } else {
        rawValue
    }
}

fun slugify(input: String): String {
    val slug = input
        .lowercase()
        .replace(Regex("[^a-z0-9]+"), "_")
        .trim('_')
    return if (slug.isBlank()) "asset" else slug
}
