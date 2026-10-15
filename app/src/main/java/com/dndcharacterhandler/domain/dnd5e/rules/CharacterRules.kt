package com.dndcharacterhandler.domain.dnd5e.rules

import com.dndcharacterhandler.domain.dnd5e.model.CreatureSize
import com.dndcharacterhandler.domain.dnd5e.model.Character
import com.dndcharacterhandler.domain.dnd5e.model.Feature
import com.dndcharacterhandler.domain.dnd5e.model.InventoryArmorDetails
import com.dndcharacterhandler.domain.dnd5e.model.InventoryArmorType
import com.dndcharacterhandler.domain.dnd5e.model.InventoryItem
import com.dndcharacterhandler.domain.dnd5e.model.SpellcastingAbility

fun abilityModifier(score: Int): Int = Math.floorDiv(score - 10, 2)

/** What a character can carry, in lb (2024): Strength × 15, twice that for a Large one. */
fun carryingCapacity(strength: Int, size: CreatureSize): Double =
    strength.coerceAtLeast(1) * 15.0 * if (size == CreatureSize.LARGE) 2 else 1

/**
 * Heights by size, in cm. The PHB (2024) gives a Small species about 2–4 ft and a Medium one about 4–8 ft (a goliath,
 * 7–8 ft, is Medium); beyond is Large. The sheet's height runs from 2 ft to about 10.
 */
const val SmallestHeightCm = 61.0
const val SmallMaxHeightCm = 122.0
const val MediumMaxHeightCm = 244.0
const val TallestHeightCm = 300.0

/** The size a height makes: Small under 4 ft, Medium up to 8 ft, Large beyond. */
fun sizeForHeightCm(cm: Double): CreatureSize = when {
    cm < SmallMaxHeightCm -> CreatureSize.SMALL
    cm <= MediumMaxHeightCm -> CreatureSize.MEDIUM
    else -> CreatureSize.LARGE
}

fun proficiencyBonusForLevel(level: Int): Int = 2 + ((level.coerceIn(1, 20) - 1) / 4)

fun calculateInitiative(dexterityScore: Int, initiativeBonus: Int): Int =
    abilityModifier(dexterityScore) + initiativeBonus

fun scoreForSpellcastingAbility(character: Character, ability: SpellcastingAbility): Int =
    when (ability) {
        SpellcastingAbility.STRENGTH -> character.strength
        SpellcastingAbility.DEXTERITY -> character.dexterity
        SpellcastingAbility.CONSTITUTION -> character.constitution
        SpellcastingAbility.INTELLIGENCE -> character.intelligence
        SpellcastingAbility.WISDOM -> character.wisdom
        SpellcastingAbility.CHARISMA -> character.charisma
    }

fun calculateArmorClass(
    dexterityScore: Int,
    inventoryItems: List<InventoryItem>,
    constitutionScore: Int = 10,
    wisdomScore: Int = 10,
    charismaScore: Int = 10,
    features: List<Feature> = emptyList()
): Int = armorClassBreakdown(
    dexterityScore, inventoryItems, constitutionScore, wisdomScore, charismaScore, features
).total

/** The automatic armor class of [character] with its [inventoryItems] and [features]. */
fun automaticArmorClass(
    character: Character,
    inventoryItems: List<InventoryItem>,
    features: List<Feature>
): ArmorClassBreakdown = armorClassBreakdown(
    dexterityScore = character.dexterity,
    inventoryItems = inventoryItems,
    constitutionScore = character.constitution,
    wisdomScore = character.wisdom,
    charismaScore = character.charisma,
    features = features
)

/** An ability that adds to the armor class. */
enum class ArmorClassAbility { DEXTERITY, CONSTITUTION, WISDOM, CHARISMA }

/** One thing the armor class is made of, as the armor class's pop-up lists it. */
sealed interface ArmorClassSource {
    val value: Int

    /** The base without armor: 10 (PHB 2024; the base the player could set before 2026-10-15 no longer counts). */
    data class Unarmored(override val value: Int) : ArmorClassSource

    /** The armor worn, [itemId] in the inventory: its own class and its magic bonus. */
    data class Armor(val name: String, override val value: Int, val itemId: Long = 0) : ArmorClassSource

    /**
     * A feature that sets the base without armor in its place, [featureId] among the character's features: the
     * barbarian's or the monk's Unarmored Defense (10), the Sahuagin's Natural Armor (12).
     */
    data class FeatureBase(val name: String, override val value: Int, val featureId: Long = 0) : ArmorClassSource

    /**
     * A feature's bonus on top of the armor class, [featureId] among the character's features: the Warforged's
     * Integrated Protection (+1, always). [counted] false: it holds only in a state the sheet doesn't keep (the
     * Beasthide shifter's +1 while shifted) — shown, not added.
     */
    data class FeatureBonus(
        val name: String,
        val bonus: Int,
        val featureId: Long = 0,
        val counted: Boolean = true
    ) : ArmorClassSource {
        override val value: Int get() = if (counted) bonus else 0
    }

    /** An ability's modifier — Dexterity as far as the armor lets it in ([cap] when it held it back). */
    data class Ability(val ability: ArmorClassAbility, override val value: Int, val cap: Int? = null) : ArmorClassSource

    /** The shield held, [itemId] in the inventory: its class and its magic bonus. */
    data class Shield(val name: String, override val value: Int, val itemId: Long = 0) : ArmorClassSource
}

/** The armor class and what it is made of. */
data class ArmorClassBreakdown(val total: Int, val sources: List<ArmorClassSource>)

/** The armor class without armor (PHB 2024): 10 + Dexterity. */
const val UnarmoredBase = 10

/**
 * A feature from the catalog that sets the armor class without armor: [base] + Dexterity + [ability] (none for the
 * Sahuagin's), with a shield or not as [shieldAllowed] says.
 */
private class UnarmoredDefense(
    val catalogId: String,
    val base: Int,
    val ability: ArmorClassAbility?,
    val shieldAllowed: Boolean
)

private val UnarmoredDefenses = listOf(
    // Barbarian's Unarmored Defense: 10 + Dexterity + Constitution, a shield allowed.
    UnarmoredDefense("phbbrbUnarmoredD", 10, ArmorClassAbility.CONSTITUTION, shieldAllowed = true),
    // Monk's Unarmored Defense: 10 + Dexterity + Wisdom, no shield.
    UnarmoredDefense("phbmnkUnarmoredD", 10, ArmorClassAbility.WISDOM, shieldAllowed = false),
    // Draconic Sorcerer's Draconic Resilience: 10 + Dexterity + Charisma, only armor rules it out.
    UnarmoredDefense("phbscrDraconicRe", 10, ArmorClassAbility.CHARISMA, shieldAllowed = true),
    // College of Dance's Dazzling Footwork: 10 + Dexterity + Charisma, no shield.
    UnarmoredDefense("phbbrdDazzlingFo", 10, ArmorClassAbility.CHARISMA, shieldAllowed = false),
    // Sahuagin's Natural Armor (Exploring Eberron): 12 + Dexterity, a shield allowed.
    UnarmoredDefense("p7T9qFfyjhT3zQ9T", 12, null, shieldAllowed = true)
)

/** A species' bonus to the armor class: the Warforged's always; the Beasthide shifter's only while shifted. */
private class ArmorClassBonus(val catalogId: String, val bonus: Int, val always: Boolean)

private val ArmorClassBonuses = listOf(
    // Warforged's Integrated Protection (Eberron: Forge of the Artificer): +1, armor or not.
    ArmorClassBonus("1qDTlwdYDCWXqr7W", 1, always = true),
    // Shifter's Beasthide: +1 while shifted, a state the sheet doesn't keep yet.
    ArmorClassBonus("cR3keUbFlBqxxPPD", 1, always = false)
)

/**
 * What the armor class is made of (owner, 2026-10-15), each line knowing its feature or item. The base: the armor
 * worn, or without it the plain 10 or a feature's own base (the highest way counts, the rules letting only one);
 * then a species' bonuses; then Dexterity as far as the armor lets it in (none for heavy armor; medium armor's cap
 * noted) and a feature's ability; then the shield. Magic bonuses are in their items. Never under 1.
 */
fun armorClassBreakdown(
    dexterityScore: Int,
    inventoryItems: List<InventoryItem>,
    constitutionScore: Int = 10,
    wisdomScore: Int = 10,
    charismaScore: Int = 10,
    features: List<Feature> = emptyList()
): ArmorClassBreakdown {
    val dexterityModifier = abilityModifier(dexterityScore)
    val armorItem = inventoryItems.firstOrNull {
        it.isEquipped && it.armorDetails?.armorType != null && it.armorDetails.armorType != InventoryArmorType.SHIELD
    }
    val shieldItem = inventoryItems.firstOrNull {
        it.isEquipped && it.armorDetails?.armorType == InventoryArmorType.SHIELD
    }
    val shield = shieldItem?.let { item ->
        ArmorClassSource.Shield(item.name, (item.armorDetails?.armorClass ?: 0) + item.armorMagicBonus(), item.id)
    }
    val byCatalogId = features.filter { it.catalogId != null }.associateBy { it.catalogId!! }
    val bonuses = ArmorClassBonuses.mapNotNull { bonus ->
        byCatalogId[bonus.catalogId]?.let { ArmorClassSource.FeatureBonus(it.name, bonus.bonus, it.id, counted = bonus.always) }
    }
    val worn = armorItem?.armorDetails
    val sources = if (armorItem != null && worn != null) {
        buildList {
            add(ArmorClassSource.Armor(armorItem.name, worn.armorClass + armorItem.armorMagicBonus(), armorItem.id))
            addAll(bonuses)
            // Dexterity as far as the armor lets it in; none at all, no line (heavy armor).
            if (worn.appliesDexterityBonus) {
                val applied = worn.appliedDexterityModifier(dexterityModifier)
                val cap = worn.maxDexterityBonus?.takeIf { dexterityModifier > it }
                add(ArmorClassSource.Ability(ArmorClassAbility.DEXTERITY, applied, cap))
            }
            shield?.let(::add)
        }
    } else {
        val modifiers = mapOf(
            ArmorClassAbility.CONSTITUTION to abilityModifier(constitutionScore),
            ArmorClassAbility.WISDOM to abilityModifier(wisdomScore),
            ArmorClassAbility.CHARISMA to abilityModifier(charismaScore)
        )
        val dexterity = ArmorClassSource.Ability(ArmorClassAbility.DEXTERITY, dexterityModifier)
        val ways = buildList {
            add(listOfNotNull(ArmorClassSource.Unarmored(UnarmoredBase), *bonuses.toTypedArray(), dexterity, shield))
            UnarmoredDefenses.forEach { defense ->
                val feature = byCatalogId[defense.catalogId] ?: return@forEach
                if (shield != null && !defense.shieldAllowed) return@forEach
                add(
                    listOfNotNull(
                        ArmorClassSource.FeatureBase(feature.name, defense.base, feature.id),
                        *bonuses.toTypedArray(),
                        dexterity,
                        defense.ability?.let { ArmorClassSource.Ability(it, modifiers.getValue(it)) },
                        shield
                    )
                )
            }
        }
        // The first way wins a tie: the plain base stays unless a feature does better.
        ways.maxWith(compareBy<List<ArmorClassSource>> { way -> way.sumOf { it.value } }.thenByDescending { ways.indexOf(it) })
    }
    return ArmorClassBreakdown(sources.sumOf { it.value }.coerceAtLeast(1), sources)
}

/** "+N" of magic armor or a magic shield (e.g. Plate Armor +1, Shield +2); 0 for mundane items. */
fun InventoryItem.armorMagicBonus(): Int = if (isMagical) magicalBonus.coerceAtLeast(0) else 0

fun InventoryArmorDetails.appliedDexterityModifier(dexterityModifier: Int): Int {
    if (!appliesDexterityBonus) return 0
    return maxDexterityBonus?.let { dexterityModifier.coerceAtMost(it) } ?: dexterityModifier
}
