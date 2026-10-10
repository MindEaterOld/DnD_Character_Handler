package com.dndcharacterhandler.data.repository

import com.dndcharacterhandler.domain.model.GameSystem
import com.dndcharacterhandler.domain.model.Condition
import com.dndcharacterhandler.data.local.entity.AttackEntity
import com.dndcharacterhandler.data.local.entity.CharacterEntity
import com.dndcharacterhandler.data.local.entity.CharacterWithDetails
import com.dndcharacterhandler.data.local.entity.CombatResourceEntity
import com.dndcharacterhandler.data.local.entity.FeatureEntity
import com.dndcharacterhandler.data.local.entity.InventoryItemEntity
import com.dndcharacterhandler.data.local.entity.NoteEntity
import com.dndcharacterhandler.data.local.entity.SkillEntity
import com.dndcharacterhandler.data.local.entity.SpellAttackEntity
import com.dndcharacterhandler.data.local.entity.SpellEntity
import com.dndcharacterhandler.domain.model.PortraitFraming
import com.dndcharacterhandler.domain.model.Attack
import com.dndcharacterhandler.domain.model.ArmorClassMode
import com.dndcharacterhandler.domain.model.Character
import com.dndcharacterhandler.domain.model.CharacterBundle
import com.dndcharacterhandler.domain.model.CombatResource
import com.dndcharacterhandler.domain.model.Feature
import com.dndcharacterhandler.domain.model.InventoryArmorDetails
import com.dndcharacterhandler.domain.model.InventoryCategory
import com.dndcharacterhandler.domain.model.InventoryContainerDetails
import com.dndcharacterhandler.domain.model.InventoryItem
import com.dndcharacterhandler.domain.model.InventoryWeaponDamage
import com.dndcharacterhandler.domain.model.InventoryWeaponDetails
import com.dndcharacterhandler.domain.model.InventoryWeaponProperty
import com.dndcharacterhandler.domain.model.Note
import com.dndcharacterhandler.domain.model.Skill
import com.dndcharacterhandler.domain.model.Spell

fun CharacterWithDetails.toDomain(): CharacterBundle =
    CharacterBundle(
        character = character.toDomain(),
        skills = skills.map { Skill(it.id, it.name, it.isProficient, it.isExpertise, it.hasJackOfAllTrades) },
        attacks = attacks.map {
            Attack(
                id = it.id,
                name = it.name,
                icon = it.icon,
                isProficient = it.isProficient,
                calculationMode = it.calculationMode,
                ability = it.ability,
                normalRange = it.normalRange,
                longRange = it.longRange,
                damageDiceCount = it.damageDiceCount,
                damageDieType = it.damageDieType,
                alternateDamageDiceCount = it.alternateDamageDiceCount,
                alternateDamageDieType = it.alternateDamageDieType,
                alternateDamageType = it.alternateDamageType,
                magicalBonus = it.magicalBonus,
                applyAbilityModifierToDamage = it.applyAbilityModifierToDamage,
                manualAttackBonusOrSaveDc = it.attackBonusOrSaveDc,
                manualDamage = it.damage,
                primaryDamageType = it.damageType,
                baseWeaponId = it.baseWeaponId
            )
        },
        combatResources = combatResources.map {
            CombatResource(
                it.id,
                it.name,
                it.currentUses,
                it.maximumUses,
                it.restoresOnShortRest,
                it.restoresOnLongRest,
                it.catalogId
            )
        },
        inventoryItems = inventoryItems.sortedBy { it.id }.map(InventoryItemEntity::toDomain),
        spells = spells.map {
            Spell(
                id = it.id,
                catalogId = it.catalogId,
                name = it.name,
                level = it.level,
                school = it.school,
                isPrepared = it.isPrepared,
                isAlwaysPrepared = it.isAlwaysPrepared,
                description = it.description,
                higherLevelDescription = it.higherLevelDescription,
                range = it.range,
                castingTime = it.castingTime,
                duration = it.duration,
                components = it.components,
                material = it.material,
                materialCost = it.materialCost,
                isRitual = it.isRitual,
                requiresConcentration = it.requiresConcentration,
                attackType = it.attackType,
                availableClasses = it.availableClasses,
                damageType = it.damageType,
                damageBase = it.damageBase,
                damageBonusValue = it.damageBonusValue,
                damageBonusIsModifier = it.damageBonusIsModifier,
                altDamageBase = it.altDamageBase,
                altDamageType = it.altDamageType,
                altDamageBonusValue = it.altDamageBonusValue,
                altDamageBonusIsModifier = it.altDamageBonusIsModifier,
                damage = it.damage,
                saveAbility = it.saveAbility,
                saveEffect = it.saveEffect,
                areaOfEffect = it.areaOfEffect,
                healBase = it.healBase,
                healBonusValue = it.healBonusValue,
                healBonusIsModifier = it.healBonusIsModifier,
                healing = it.healing
            )
        },
        spellAttacks = spellAttacks.map { it.toDomain() },
        features = features.map { Feature(it.id, it.name, it.description, it.level, it.source, it.category, it.catalogId) },
        notes = notes.map { Note(it.id, it.title, it.createdDate, it.updatedDate, it.content, it.isPinned) }
    )

fun CharacterEntity.toDomain(): Character =
    Character(
        id = id,
        gameSystem = GameSystem.fromKey(gameSystem) ?: GameSystem.DEFAULT,
        name = name,
        race = race,
        characterClass = characterClass,
        subclass = subclass,
        level = level,
        portraitUri = portraitUri,
        portraitFraming = PortraitFraming(portraitFocusX, portraitFocusY, portraitZoom),
        currentHp = currentHp,
        maxHp = maxHp,
        temporaryHp = temporaryHp,
        deathSaveSuccesses = deathSaveSuccesses,
        deathSaveFailures = deathSaveFailures,
        conditions = Condition.parse(conditions),
        exhaustion = exhaustion,
        concentrationSpellId = concentrationSpellId,
        hitDieSides = hitDieSides,
        spentHitDice = spentHitDice,
        hasInspiration = hasInspiration,
        armorClass = armorClass,
        baseArmorClass = baseArmorClass,
        armorClassMode = armorClassMode,
        copperPieces = copperPieces,
        silverPieces = silverPieces,
        goldPieces = goldPieces,
        speed = speed,
        initiative = initiative,
        initiativeBonus = initiativeBonus,
        spellcastingAbility = spellcastingAbility,
        spellSlotMaximums = spellSlotMaximums,
        spellSlotRemaining = spellSlotRemaining,
        spellSlotsRestoreOnShortRest = spellSlotsRestoreOnShortRest,
        spellSlotsRestoreOnLongRest = spellSlotsRestoreOnLongRest,
        experience = experience,
        strength = strength,
        dexterity = dexterity,
        constitution = constitution,
        intelligence = intelligence,
        wisdom = wisdom,
        charisma = charisma,
        strengthSaveProficient = strengthSaveProficient,
        dexteritySaveProficient = dexteritySaveProficient,
        constitutionSaveProficient = constitutionSaveProficient,
        intelligenceSaveProficient = intelligenceSaveProficient,
        wisdomSaveProficient = wisdomSaveProficient,
        charismaSaveProficient = charismaSaveProficient,
        passivePerceptionBonus = passivePerceptionBonus,
        darkvisionMode = darkvisionMode,
        darkvisionManualFeet = darkvisionManualFeet,
        armorProficiencies = armorProficiencies,
        weaponProficiencies = weaponProficiencies,
        toolProficiencies = toolProficiencies,
        languageProficiencies = languageProficiencies,
        weaponMasteries = weaponMasteries,
        defenses = defenses,
        alignment = alignment,
        background = background,
        faith = faith,
        homeland = homeland,
        age = age,
        gender = gender,
        height = height,
        weight = weight,
        eyes = eyes,
        hair = hair,
        skin = skin,
        size = size,
        personalityTraits = personalityTraits,
        ideals = ideals,
        bonds = bonds,
        flaws = flaws,
        biography = biography,
        createdAt = createdAt,
        updatedAt = updatedAt,
        classes = ProgressionJson.decodeClasses(classesJson),
        advancements = ProgressionJson.decodeAdvancements(advancementsJson)
    )

fun InventoryItemEntity.toDomain(): InventoryItem =
    InventoryItem(
        id = id,
        name = name,
        description = description,
        isMagical = isMagical,
        magicalBonus = magicalBonus,
        category = category,
        weight = weight,
        quantity = quantity,
        isEquipped = isEquipped,
        icon = icon,
        costQuantity = costQuantity,
        costUnit = costUnit,
        armorDetails = if (armorType != null && armorClass != null && appliesDexterityBonus != null && strengthMinimum != null && hasStealthDisadvantage != null) {
            InventoryArmorDetails(
                armorType = armorType,
                armorClass = armorClass,
                appliesDexterityBonus = appliesDexterityBonus,
                maxDexterityBonus = maxDexterityBonus,
                strengthMinimum = strengthMinimum,
                hasStealthDisadvantage = hasStealthDisadvantage
            )
        } else {
            null
        },
        weaponDetails = if (
            weaponClass != null &&
            weaponRangeType != null &&
            weaponPrimaryDamageDice != null &&
            weaponPrimaryDamageType != null
        ) {
            InventoryWeaponDetails(
                weaponClass = weaponClass,
                rangeType = weaponRangeType,
                baseWeaponId = weaponBaseId,
                normalRange = weaponNormalRange,
                longRange = weaponLongRange,
                damages = listOf(
                    InventoryWeaponDamage(
                        dice = weaponPrimaryDamageDice,
                        damageType = weaponPrimaryDamageType
                    )
                ),
                twoHandedDamage = if (weaponTwoHandedDamageDice != null && weaponTwoHandedDamageType != null) {
                    InventoryWeaponDamage(
                        dice = weaponTwoHandedDamageDice,
                        damageType = weaponTwoHandedDamageType
                    )
                } else {
                    null
                },
                properties = weaponProperties
                    .orEmpty()
                    .split(',')
                    .mapNotNull { value ->
                        value.takeIf { it.isNotBlank() }
                            ?.let { runCatching { InventoryWeaponProperty.valueOf(it) }.getOrNull() }
                    }
                    .toSet()
            )
        } else {
            null
        },
        containerDetails = if (category == InventoryCategory.CONTAINER) {
            InventoryContainerDetails(capacity = containerCapacity, weightlessContents = containerWeightlessContents)
        } else {
            null
        },
        containerId = containerId,
        catalogId = catalogId
    )

fun Character.toEntity(): CharacterEntity =
    CharacterEntity(
        id = id,
        gameSystem = gameSystem.key,
        name = name,
        race = race,
        characterClass = characterClass,
        subclass = subclass,
        level = level,
        portraitUri = portraitUri,
        portraitFocusX = portraitFraming.focusX,
        portraitFocusY = portraitFraming.focusY,
        portraitZoom = portraitFraming.zoom,
        currentHp = currentHp,
        maxHp = maxHp,
        temporaryHp = temporaryHp,
        deathSaveSuccesses = deathSaveSuccesses,
        deathSaveFailures = deathSaveFailures,
        conditions = Condition.join(conditions),
        exhaustion = exhaustion,
        concentrationSpellId = concentrationSpellId,
        hitDieSides = hitDieSides,
        spentHitDice = spentHitDice,
        hasInspiration = hasInspiration,
        armorClass = armorClass,
        baseArmorClass = baseArmorClass,
        armorClassMode = armorClassMode,
        copperPieces = copperPieces,
        silverPieces = silverPieces,
        goldPieces = goldPieces,
        speed = speed,
        initiative = initiative,
        initiativeBonus = initiativeBonus,
        spellcastingAbility = spellcastingAbility,
        spellSlotMaximums = spellSlotMaximums,
        spellSlotRemaining = spellSlotRemaining,
        spellSlotsRestoreOnShortRest = spellSlotsRestoreOnShortRest,
        spellSlotsRestoreOnLongRest = spellSlotsRestoreOnLongRest,
        experience = experience,
        strength = strength,
        dexterity = dexterity,
        constitution = constitution,
        intelligence = intelligence,
        wisdom = wisdom,
        charisma = charisma,
        strengthSaveProficient = strengthSaveProficient,
        dexteritySaveProficient = dexteritySaveProficient,
        constitutionSaveProficient = constitutionSaveProficient,
        intelligenceSaveProficient = intelligenceSaveProficient,
        wisdomSaveProficient = wisdomSaveProficient,
        charismaSaveProficient = charismaSaveProficient,
        passivePerceptionBonus = passivePerceptionBonus,
        darkvisionMode = darkvisionMode,
        darkvisionManualFeet = darkvisionManualFeet,
        armorProficiencies = armorProficiencies,
        weaponProficiencies = weaponProficiencies,
        toolProficiencies = toolProficiencies,
        languageProficiencies = languageProficiencies,
        weaponMasteries = weaponMasteries,
        defenses = defenses,
        alignment = alignment,
        background = background,
        faith = faith,
        homeland = homeland,
        age = age,
        gender = gender,
        height = height,
        weight = weight,
        eyes = eyes,
        hair = hair,
        skin = skin,
        size = size,
        personalityTraits = personalityTraits,
        ideals = ideals,
        bonds = bonds,
        flaws = flaws,
        biography = biography,
        createdAt = createdAt,
        updatedAt = updatedAt,
        classesJson = ProgressionJson.encodeClasses(classes),
        advancementsJson = ProgressionJson.encodeAdvancements(advancements)
    )

fun Skill.toEntity(characterId: Long): SkillEntity =
    SkillEntity(
        id = id,
        characterOwnerId = characterId,
        name = name,
        isProficient = isProficient,
        isExpertise = isExpertise,
        hasJackOfAllTrades = hasJackOfAllTrades
    )

fun Attack.toEntity(characterId: Long): AttackEntity =
    AttackEntity(
        id = id,
        characterOwnerId = characterId,
        name = name,
        icon = icon,
        isProficient = isProficient,
        calculationMode = calculationMode,
        ability = ability,
        normalRange = normalRange,
        longRange = longRange,
        damageDiceCount = damageDiceCount,
        damageDieType = damageDieType,
        alternateDamageDiceCount = alternateDamageDiceCount,
        alternateDamageDieType = alternateDamageDieType,
        alternateDamageType = alternateDamageType,
        magicalBonus = magicalBonus,
        applyAbilityModifierToDamage = applyAbilityModifierToDamage,
        range = "",
        attackBonusOrSaveDc = manualAttackBonusOrSaveDc,
        damage = manualDamage,
        damageType = primaryDamageType,
        baseWeaponId = baseWeaponId
    )

fun CombatResource.toEntity(characterId: Long): CombatResourceEntity =
    CombatResourceEntity(
        id = id,
        characterOwnerId = characterId,
        name = name,
        currentUses = currentUses,
        maximumUses = maximumUses,
        restoresOnShortRest = restoresOnShortRest,
        restoresOnLongRest = restoresOnLongRest,
        catalogId = catalogId
    )

fun InventoryItem.toEntity(characterId: Long): InventoryItemEntity =
    InventoryItemEntity(
        id = id,
        characterOwnerId = characterId,
        name = name,
        description = description,
        isMagical = isMagical,
        magicalBonus = magicalBonus,
        category = category,
        weight = weight,
        quantity = quantity,
        isEquipped = isEquipped,
        icon = icon,
        costQuantity = costQuantity,
        costUnit = costUnit,
        armorType = armorDetails?.armorType,
        armorClass = armorDetails?.armorClass,
        appliesDexterityBonus = armorDetails?.appliesDexterityBonus,
        maxDexterityBonus = armorDetails?.maxDexterityBonus,
        strengthMinimum = armorDetails?.strengthMinimum,
        hasStealthDisadvantage = armorDetails?.hasStealthDisadvantage,
        weaponClass = weaponDetails?.weaponClass,
        weaponRangeType = weaponDetails?.rangeType,
        weaponBaseId = weaponDetails?.baseWeaponId,
        weaponNormalRange = weaponDetails?.normalRange,
        weaponLongRange = weaponDetails?.longRange,
        weaponPrimaryDamageDice = weaponDetails?.damages?.firstOrNull()?.dice,
        weaponPrimaryDamageType = weaponDetails?.damages?.firstOrNull()?.damageType,
        weaponTwoHandedDamageDice = weaponDetails?.twoHandedDamage?.dice,
        weaponTwoHandedDamageType = weaponDetails?.twoHandedDamage?.damageType,
        weaponProperties = weaponDetails?.properties
            ?.map(InventoryWeaponProperty::name)
            ?.sorted()
            ?.joinToString(","),
        containerCapacity = containerDetails?.capacity,
        containerWeightlessContents = containerDetails?.weightlessContents ?: false,
        containerId = containerId,
        catalogId = catalogId
    )

fun Spell.toEntity(characterId: Long): SpellEntity =
    SpellEntity(
        id = id,
        characterOwnerId = characterId,
        catalogId = catalogId,
        name = name,
        level = level,
        school = school,
        isPrepared = isPrepared,
        isAlwaysPrepared = isAlwaysPrepared,
        description = description,
        higherLevelDescription = higherLevelDescription,
        range = range,
        castingTime = castingTime,
        duration = duration,
        components = components,
        material = material,
        materialCost = materialCost,
        isRitual = isRitual,
        requiresConcentration = requiresConcentration,
        attackType = attackType,
        availableClasses = availableClasses,
        damageType = damageType,
        damageBase = damageBase,
        damageBonusValue = damageBonusValue,
        damageBonusIsModifier = damageBonusIsModifier,
        altDamageBase = altDamageBase,
        altDamageType = altDamageType,
        altDamageBonusValue = altDamageBonusValue,
        altDamageBonusIsModifier = altDamageBonusIsModifier,
        damage = damage,
        saveAbility = saveAbility,
        saveEffect = saveEffect,
        areaOfEffect = areaOfEffect,
        healBase = healBase,
        healBonusValue = healBonusValue,
        healBonusIsModifier = healBonusIsModifier,
        healing = healing
    )

fun SpellAttackEntity.toDomain(): Spell =
    Spell(
        id = id,
        catalogId = catalogId,
        name = name,
        level = level,
        school = school,
        isPrepared = isPrepared,
        isAlwaysPrepared = isAlwaysPrepared,
        description = description,
        higherLevelDescription = higherLevelDescription,
        range = range,
        castingTime = castingTime,
        duration = duration,
        components = components,
        material = material,
        materialCost = materialCost,
        isRitual = isRitual,
        requiresConcentration = requiresConcentration,
        attackType = attackType,
        availableClasses = availableClasses,
        damageType = damageType,
        damageBase = damageBase,
        damageBonusValue = damageBonusValue,
        damageBonusIsModifier = damageBonusIsModifier,
        altDamageBase = altDamageBase,
        altDamageType = altDamageType,
        altDamageBonusValue = altDamageBonusValue,
        altDamageBonusIsModifier = altDamageBonusIsModifier,
        damage = damage,
        saveAbility = saveAbility,
        saveEffect = saveEffect,
        areaOfEffect = areaOfEffect,
        healBase = healBase,
        healBonusValue = healBonusValue,
        healBonusIsModifier = healBonusIsModifier,
        healing = healing
    )

fun Spell.toSpellAttackEntity(characterId: Long): SpellAttackEntity =
    SpellAttackEntity(
        id = id,
        characterOwnerId = characterId,
        catalogId = catalogId,
        name = name,
        level = level,
        school = school,
        isPrepared = isPrepared,
        isAlwaysPrepared = isAlwaysPrepared,
        description = description,
        higherLevelDescription = higherLevelDescription,
        range = range,
        castingTime = castingTime,
        duration = duration,
        components = components,
        material = material,
        materialCost = materialCost,
        isRitual = isRitual,
        requiresConcentration = requiresConcentration,
        attackType = attackType,
        availableClasses = availableClasses,
        damageType = damageType,
        damageBase = damageBase,
        damageBonusValue = damageBonusValue,
        damageBonusIsModifier = damageBonusIsModifier,
        altDamageBase = altDamageBase,
        altDamageType = altDamageType,
        altDamageBonusValue = altDamageBonusValue,
        altDamageBonusIsModifier = altDamageBonusIsModifier,
        damage = damage,
        saveAbility = saveAbility,
        saveEffect = saveEffect,
        areaOfEffect = areaOfEffect,
        healBase = healBase,
        healBonusValue = healBonusValue,
        healBonusIsModifier = healBonusIsModifier,
        healing = healing
    )

fun Feature.toEntity(characterId: Long): FeatureEntity =
    FeatureEntity(
        id = id,
        characterOwnerId = characterId,
        name = name,
        description = description,
        level = level,
        source = source,
        category = category,
        catalogId = catalogId
    )

fun Note.toEntity(characterId: Long): NoteEntity =
    NoteEntity(id = id, characterOwnerId = characterId, title = title, createdDate = createdDate, updatedDate = updatedDate, content = content, isPinned = isPinned)
