package com.dndcharacterhandler.domain.dnd5e.rules

import com.dndcharacterhandler.domain.dnd5e.model.Attack
import com.dndcharacterhandler.domain.dnd5e.model.AttackCalculationMode
import com.dndcharacterhandler.domain.dnd5e.model.InventoryCategory
import com.dndcharacterhandler.domain.dnd5e.model.InventoryItem
import com.dndcharacterhandler.domain.dnd5e.model.InventoryWeaponClass
import com.dndcharacterhandler.domain.dnd5e.model.InventoryWeaponDamage
import com.dndcharacterhandler.domain.dnd5e.model.InventoryWeaponDetails
import com.dndcharacterhandler.domain.dnd5e.model.InventoryWeaponProperty
import com.dndcharacterhandler.domain.dnd5e.model.InventoryWeaponRangeType
import com.dndcharacterhandler.domain.dnd5e.model.SpellcastingAbility
import com.dndcharacterhandler.domain.dnd5e.model.WeaponGroupMartialId
import com.dndcharacterhandler.domain.dnd5e.model.WeaponGroupSimpleId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class WeaponAttacksTest {
    private fun weapon(
        name: String,
        id: String,
        weaponClass: InventoryWeaponClass,
        rangeType: InventoryWeaponRangeType = InventoryWeaponRangeType.MELEE,
        properties: Set<InventoryWeaponProperty> = emptySet(),
        dice: String = "1d8",
        magicalBonus: Int = 0
    ) = InventoryItem(
        id = name.length.toLong(),
        name = name,
        category = InventoryCategory.WEAPON,
        weight = 3.0,
        quantity = 1,
        isEquipped = true,
        icon = "",
        isMagical = magicalBonus > 0,
        magicalBonus = magicalBonus,
        weaponDetails = InventoryWeaponDetails(
            weaponClass = weaponClass,
            rangeType = rangeType,
            baseWeaponId = id,
            normalRange = null,
            longRange = null,
            damages = listOf(InventoryWeaponDamage(dice, "Slashing")),
            properties = properties
        )
    )

    private val greataxe = weapon("Секира", "greataxe", InventoryWeaponClass.MARTIAL, dice = "1d12")
    private val rapier = weapon("Рапира", "rapier", InventoryWeaponClass.MARTIAL, properties = setOf(InventoryWeaponProperty.FINESSE))
    private val shortbow = weapon("Короткий лук", "shortbow", InventoryWeaponClass.SIMPLE, InventoryWeaponRangeType.RANGED, dice = "1d6")

    @Test
    fun proficiencyFollowsTheCharactersProficienciesNow() {
        val attack = weaponAttack(greataxe, 16, 10, emptySet())
        assertFalse(attack.isProficient)
        assertTrue(attack.madeWith(greataxe, 16, 10, setOf(WeaponGroupMartialId)).isProficient)
        assertTrue(attack.madeWith(greataxe, 16, 10, setOf("greataxe")).isProficient)
        assertFalse(attack.madeWith(greataxe, 16, 10, setOf(WeaponGroupSimpleId)).isProficient)
    }

    @Test
    fun aFinesseWeaponTakesTheBetterOfStrengthAndDexterityAsTheyAreNow() {
        val attack = weaponAttack(rapier, 16, 10, emptySet())
        assertEquals(SpellcastingAbility.STRENGTH, attack.ability)
        assertEquals(SpellcastingAbility.DEXTERITY, attack.madeWith(rapier, 16, 18, emptySet()).ability)
        assertEquals(SpellcastingAbility.DEXTERITY, weaponAttack(shortbow, 18, 8, emptySet()).ability)
        assertEquals(SpellcastingAbility.STRENGTH, weaponAttack(greataxe, 8, 18, emptySet()).ability)
    }

    @Test
    fun theWeaponsDamageAndMagicBonusComeAlong() {
        val attack = weaponAttack(greataxe, 16, 10, emptySet())
        assertEquals(1, attack.damageDiceCount)
        assertEquals("d12", attack.damageDieType)
        assertEquals(0, attack.magicalBonus)
        val enchanted = greataxe.copy(isMagical = true, magicalBonus = 2)
        assertEquals(2, attack.madeWith(enchanted, 16, 10, emptySet()).magicalBonus)
    }

    @Test
    fun theAttacksOwnChoicesStay() {
        val offHand = weaponAttack(greataxe, 16, 10, emptySet()).copy(id = 7, name = "Секира (левая)", applyAbilityModifierToDamage = false)
        val now = offHand.madeWith(greataxe, 16, 10, setOf(WeaponGroupMartialId))
        assertEquals(7L, now.id)
        assertEquals("Секира (левая)", now.name)
        assertFalse(now.applyAbilityModifierToDamage)
        val manual = offHand.copy(calculationMode = AttackCalculationMode.MANUAL)
        assertSame(manual, manual.madeWith(greataxe, 16, 10, setOf(WeaponGroupMartialId)))
    }

    @Test
    fun anAttackFindsItsWeaponByNameThenByKind() {
        val items = listOf(rapier, greataxe)
        val attack = weaponAttack(greataxe, 16, 10, emptySet())
        assertEquals(greataxe, weaponOf(attack, items))
        assertEquals(greataxe, weaponOf(attack.copy(name = "Громовая"), items))
        assertNull(weaponOf(Attack(name = "Укус", icon = "", primaryDamageType = "Piercing"), items))
        assertNull(weaponOf(attack.copy(name = "Громовая"), listOf(rapier)))
    }
}
