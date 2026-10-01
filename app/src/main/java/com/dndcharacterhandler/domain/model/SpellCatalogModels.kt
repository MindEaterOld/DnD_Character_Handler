package com.dndcharacterhandler.domain.model

/** Where catalog spells come from: the Foundry PHB 2024 catalog (see tools/foundry_catalog). */
enum class SpellCatalogSource { SRD_2014, FOUNDRY_2024 }

data class SpellCatalogItem(
    val id: String,
    val name: String,
    val level: Int,
    val school: String,
    val description: String,
    val higherLevelDescription: String = "",
    val ruName: String = "",
    val ruDescription: String = "",
    val ruHigherLevel: String = "",
    val ruMaterial: String = "",
    val range: String = "",
    val castingTime: String = "",
    val duration: String = "",
    val components: String = "",
    val material: String = "",
    val materialCost: String = "",
    val isRitual: Boolean = false,
    val requiresConcentration: Boolean = false,
    val attackType: String = "",
    val availableClasses: String = "",
    val damageType: String = "",
    val damageBase: String = "",
    val damageBonusValue: Int = 0,
    val damageBonusIsModifier: Boolean = false,
    val altDamageBase: String = "",
    val altDamageType: String = "",
    val altDamageBonusValue: Int = 0,
    val altDamageBonusIsModifier: Boolean = false,
    val damage: String = "",
    val saveAbility: String = "",
    val saveEffect: String = "",
    val areaOfEffect: String = "",
    val healBase: String = "",
    val healBonusValue: Int = 0,
    val healBonusIsModifier: Boolean = false,
    val healing: String = "",
    val source: SpellCatalogSource = SpellCatalogSource.SRD_2014,
    /** Older ids characters' spells may carry for this spell ("spell:fireball" of the SRD 2014). */
    val legacyIds: List<String> = emptyList()
) {
    /**
     * The character's copy, prepared as a feature or the player decides: [alwaysPrepared] spells
     * (domain, species, feat spells) are prepared and don't count toward the limit; cantrips are at hand.
     */
    fun toCharacterSpell(alwaysPrepared: Boolean): Spell =
        toSpell().copy(isPrepared = alwaysPrepared || level == 0, isAlwaysPrepared = alwaysPrepared && level > 0)

    fun toSpell(): Spell =
        Spell(
            catalogId = id,
            name = name,
            level = level,
            school = school,
            isPrepared = level == 0,
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
}

/** Foundry's school keys -> the app's school names. */
private val SPELL_SCHOOLS = mapOf(
    "abj" to "Abjuration", "con" to "Conjuration", "div" to "Divination", "enc" to "Enchantment",
    "evo" to "Evocation", "ill" to "Illusion", "nec" to "Necromancy", "trs" to "Transmutation"
)

/**
 * A catalog spell (Foundry, PHB 2024) as the app's spell card: English falls back to Russian for
 * the supplements' spells that only come in Russian; the classes are the class lists it is on.
 */
fun CatalogSpellRef.toSpellCatalogItem(catalog: CharacterCatalog, legacyIds: List<String> = emptyList()): SpellCatalogItem {
    fun bonusValue(bonus: String) = bonus.toIntOrNull() ?: 0
    return SpellCatalogItem(
        id = id,
        name = name.en.ifBlank { name.ru },
        level = level ?: 0,
        school = SPELL_SCHOOLS[school] ?: school,
        description = text.en.ifBlank { text.ru },
        higherLevelDescription = higher.en.ifBlank { higher.ru },
        ruName = name.ru,
        ruDescription = text.ru,
        ruHigherLevel = higher.ru,
        ruMaterial = material.ru,
        range = range,
        castingTime = castingTime,
        duration = duration,
        components = components,
        material = material.en.ifBlank { material.ru },
        materialCost = materialCost,
        isRitual = ritual,
        requiresConcentration = concentration,
        attackType = attackType,
        availableClasses = lists.filter { it.startsWith("class:") }.mapNotNull { list ->
            catalog.classesByIdentifier[list.removePrefix("class:")]?.name?.let { it.en.ifBlank { it.ru } }
        }.joinToString(", "),
        damageType = damageType,
        damageBase = damageBase,
        damageBonusValue = bonusValue(damageBonus),
        damageBonusIsModifier = damageBonus == "MOD",
        altDamageBase = altDamageBase,
        altDamageType = altDamageType,
        altDamageBonusValue = bonusValue(altDamageBonus),
        altDamageBonusIsModifier = altDamageBonus == "MOD",
        damage = damage,
        saveAbility = saveAbility,
        saveEffect = saveEffect,
        areaOfEffect = areaOfEffect,
        healBase = healBase,
        healBonusValue = bonusValue(healBonus),
        healBonusIsModifier = healBonus == "MOD",
        healing = healing,
        source = SpellCatalogSource.FOUNDRY_2024,
        legacyIds = legacyIds
    )
}

/** Catalog spells by id, and by the older ids characters' spells may still carry. */
fun List<SpellCatalogItem>.byCatalogId(): Map<String, SpellCatalogItem> = buildMap {
    this@byCatalogId.forEach { item -> item.legacyIds.forEach { put(it, item) } }
    this@byCatalogId.forEach { item -> put(item.id, item) }
}

