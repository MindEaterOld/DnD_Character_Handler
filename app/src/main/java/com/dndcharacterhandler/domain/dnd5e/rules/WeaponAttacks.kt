package com.dndcharacterhandler.domain.dnd5e.rules

import com.dndcharacterhandler.domain.dnd5e.model.Attack
import com.dndcharacterhandler.domain.dnd5e.model.AttackCalculationMode
import com.dndcharacterhandler.domain.dnd5e.model.InventoryCategory
import com.dndcharacterhandler.domain.dnd5e.model.InventoryItem
import com.dndcharacterhandler.domain.dnd5e.model.InventoryWeaponClass
import com.dndcharacterhandler.domain.dnd5e.model.InventoryWeaponDetails
import com.dndcharacterhandler.domain.dnd5e.model.InventoryWeaponProperty
import com.dndcharacterhandler.domain.dnd5e.model.InventoryWeaponRangeType
import com.dndcharacterhandler.domain.dnd5e.model.SpellcastingAbility
import com.dndcharacterhandler.domain.dnd5e.model.WeaponGroupMartialId
import com.dndcharacterhandler.domain.dnd5e.model.WeaponGroupSimpleId
import com.dndcharacterhandler.domain.dnd5e.model.martialWeaponOptions
import com.dndcharacterhandler.domain.dnd5e.model.simpleWeaponOptions

/*
 * An attack made from a weapon in the inventory is that weapon's, as in Foundry: proficiency, the
 * ability it attacks with, its magic bonus, damage and range come from the weapon and from the
 * character as they are now, not as they were when the attack was added.
 */

/** The sheet's weapon id ("war_pick"); older catalog items keep the SRD's hyphens ("war-pick"). */
private val InventoryWeaponDetails.weaponId: String?
    get() = baseWeaponId?.replace('-', '_')?.takeIf(String::isNotBlank)

/** Whether the character is proficient with the weapon: with its kind or with its whole group. */
fun InventoryWeaponDetails.isProficient(weaponProficiencyIds: Set<String>): Boolean {
    val id = weaponId
    if (id != null && id in weaponProficiencyIds) return true
    val group = when {
        id != null && simpleWeaponOptions.any { it.id == id } -> InventoryWeaponClass.SIMPLE
        id != null && martialWeaponOptions.any { it.id == id } -> InventoryWeaponClass.MARTIAL
        else -> weaponClass
    }
    return when (group) {
        InventoryWeaponClass.SIMPLE -> WeaponGroupSimpleId in weaponProficiencyIds
        InventoryWeaponClass.MARTIAL -> WeaponGroupMartialId in weaponProficiencyIds
    }
}

/** Dexterity for a ranged weapon, the better of Strength and Dexterity for a finesse one, Strength otherwise. */
fun InventoryWeaponDetails.attackAbility(strength: Int, dexterity: Int): SpellcastingAbility = when {
    rangeType == InventoryWeaponRangeType.RANGED -> SpellcastingAbility.DEXTERITY
    InventoryWeaponProperty.FINESSE in properties && abilityModifier(dexterity) > abilityModifier(strength) -> SpellcastingAbility.DEXTERITY
    else -> SpellcastingAbility.STRENGTH
}

/**
 * The inventory weapon [attack] belongs to: the weapon of its name, else (renamed) the one of its
 * kind; null for an attack of no weapon carried (a custom one, a weapon given away).
 */
fun weaponOf(attack: Attack, items: List<InventoryItem>): InventoryItem? {
    val weapons = items.filter { it.category == InventoryCategory.WEAPON && it.weaponDetails != null }
    val name = attack.name.trim()
    weapons.firstOrNull { it.name.trim().equals(name, ignoreCase = true) }?.let { return it }
    val kind = attack.baseWeaponId?.replace('-', '_') ?: return null
    return weapons.firstOrNull { it.weaponDetails?.weaponId == kind }
}

/**
 * [attack] as [weapon] and the character make it now. A hand-written attack (the manual mode) stays
 * as written; so do its name and whether the ability modifier goes to damage (the off hand).
 */
fun Attack.madeWith(weapon: InventoryItem, strength: Int, dexterity: Int, weaponProficiencyIds: Set<String>): Attack {
    val details = weapon.weaponDetails ?: return this
    if (calculationMode == AttackCalculationMode.MANUAL) return this
    val primary = details.damages.firstOrNull()
    val alternate = details.twoHandedDamage
    return copy(
        isProficient = details.isProficient(weaponProficiencyIds),
        ability = details.attackAbility(strength, dexterity),
        normalRange = details.normalRange,
        longRange = details.longRange,
        damageDiceCount = primary?.dice.diceCount() ?: damageDiceCount,
        damageDieType = primary?.dice.dieType() ?: damageDieType,
        alternateDamageDiceCount = alternate?.dice.diceCount(),
        alternateDamageDieType = alternate?.dice.dieType(),
        alternateDamageType = alternate?.damageType,
        magicalBonus = if (weapon.isMagical) weapon.magicalBonus else 0,
        primaryDamageType = primary?.damageType ?: primaryDamageType,
        baseWeaponId = details.weaponId ?: baseWeaponId
    )
}

/** A new attack for [weapon], as the character makes it now. */
fun weaponAttack(weapon: InventoryItem, strength: Int, dexterity: Int, weaponProficiencyIds: Set<String>): Attack =
    Attack(name = weapon.name, icon = weapon.icon, primaryDamageType = "").madeWith(weapon, strength, dexterity, weaponProficiencyIds)

/** "2d6" -> 2. */
private fun String?.diceCount(): Int? =
    this?.replace(" ", "")?.let { Regex("""^(\d+)d\d+.*$""").matchEntire(it)?.groupValues?.get(1)?.toIntOrNull() }

/** "2d6" -> "d6". */
private fun String?.dieType(): String? =
    this?.replace(" ", "")?.let { Regex("""^\d+(d\d+).*$""").matchEntire(it)?.groupValues?.get(1) }
