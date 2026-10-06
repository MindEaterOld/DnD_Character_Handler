package com.dndcharacterhandler.domain.rules

import com.dndcharacterhandler.domain.model.CreatureSize
import com.dndcharacterhandler.domain.model.Character
import com.dndcharacterhandler.domain.model.InventoryArmorDetails
import com.dndcharacterhandler.domain.model.InventoryArmorType
import com.dndcharacterhandler.domain.model.InventoryItem
import com.dndcharacterhandler.domain.model.SpellcastingAbility

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
    baseArmorClass: Int,
    dexterityScore: Int,
    inventoryItems: List<InventoryItem>
): Int {
    val dexterityModifier = abilityModifier(dexterityScore)
    val equippedArmorItem = inventoryItems.firstOrNull {
        it.isEquipped && it.armorDetails?.armorType != null && it.armorDetails.armorType != InventoryArmorType.SHIELD
    }
    val equippedShieldItem = inventoryItems.firstOrNull {
        it.isEquipped && it.armorDetails?.armorType == InventoryArmorType.SHIELD
    }
    val equippedArmor = equippedArmorItem?.armorDetails
    val equippedShield = equippedShieldItem?.armorDetails

    val effectiveArmorClass = if (equippedArmor != null) {
        equippedArmor.armorClass + equippedArmor.appliedDexterityModifier(dexterityModifier) +
            equippedArmorItem.armorMagicBonus()
    } else {
        baseArmorClass + dexterityModifier
    }
    val shieldArmorClass = equippedShield?.let { it.armorClass + equippedShieldItem.armorMagicBonus() } ?: 0

    return (effectiveArmorClass + shieldArmorClass).coerceAtLeast(1)
}

/** "+N" of magic armor or a magic shield (e.g. Plate Armor +1, Shield +2); 0 for mundane items. */
fun InventoryItem.armorMagicBonus(): Int = if (isMagical) magicalBonus.coerceAtLeast(0) else 0

fun InventoryArmorDetails.appliedDexterityModifier(dexterityModifier: Int): Int {
    if (!appliesDexterityBonus) return 0
    return maxDexterityBonus?.let { dexterityModifier.coerceAtMost(it) } ?: dexterityModifier
}
