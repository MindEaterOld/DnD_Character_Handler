package com.dndcharacterhandler.domain.rules

import com.dndcharacterhandler.domain.model.Character
import com.dndcharacterhandler.domain.model.InventoryArmorDetails
import com.dndcharacterhandler.domain.model.InventoryArmorType
import com.dndcharacterhandler.domain.model.InventoryItem
import com.dndcharacterhandler.domain.model.SpellcastingAbility

fun abilityModifier(score: Int): Int = Math.floorDiv(score - 10, 2)

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
    val equippedArmor = inventoryItems.firstOrNull {
        it.isEquipped && it.armorDetails?.armorType != null && it.armorDetails.armorType != InventoryArmorType.SHIELD
    }?.armorDetails
    val equippedShield = inventoryItems.firstOrNull {
        it.isEquipped && it.armorDetails?.armorType == InventoryArmorType.SHIELD
    }?.armorDetails

    val effectiveArmorClass = if (equippedArmor != null) {
        equippedArmor.armorClass + equippedArmor.appliedDexterityModifier(dexterityModifier)
    } else {
        baseArmorClass + dexterityModifier
    }

    return (effectiveArmorClass + (equippedShield?.armorClass ?: 0)).coerceAtLeast(1)
}

fun InventoryArmorDetails.appliedDexterityModifier(dexterityModifier: Int): Int {
    if (!appliesDexterityBonus) return 0
    return maxDexterityBonus?.let { dexterityModifier.coerceAtMost(it) } ?: dexterityModifier
}
