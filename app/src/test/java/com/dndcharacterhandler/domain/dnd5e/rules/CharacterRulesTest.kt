package com.dndcharacterhandler.domain.dnd5e.rules

import com.dndcharacterhandler.domain.dnd5e.model.Character
import com.dndcharacterhandler.domain.dnd5e.model.CreatureSize
import com.dndcharacterhandler.domain.dnd5e.model.InventoryArmorDetails
import com.dndcharacterhandler.domain.dnd5e.model.InventoryArmorType
import com.dndcharacterhandler.domain.dnd5e.model.InventoryCategory
import com.dndcharacterhandler.domain.dnd5e.model.InventoryItem
import com.dndcharacterhandler.domain.dnd5e.model.SpellcastingAbility
import com.dndcharacterhandler.domain.dnd5e.model.defaultCharacterBundle
import org.junit.Assert.assertEquals
import org.junit.Test

class CharacterRulesTest {

    @Test
    fun abilityModifier_followsDndRounding() {
        assertEquals(0, abilityModifier(10))
        assertEquals(0, abilityModifier(11))
        assertEquals(1, abilityModifier(12))
        assertEquals(-1, abilityModifier(8))
        assertEquals(-1, abilityModifier(9))
        assertEquals(5, abilityModifier(20))
        assertEquals(-5, abilityModifier(1))
    }

    @Test
    fun sizeForHeight_followsThePhbRanges() {
        assertEquals(CreatureSize.SMALL, sizeForHeightCm(61.0))
        assertEquals(CreatureSize.SMALL, sizeForHeightCm(121.9))
        assertEquals(CreatureSize.MEDIUM, sizeForHeightCm(122.0))
        assertEquals(CreatureSize.MEDIUM, sizeForHeightCm(180.0))
        assertEquals(CreatureSize.MEDIUM, sizeForHeightCm(244.0))
        assertEquals(CreatureSize.LARGE, sizeForHeightCm(244.1))
        assertEquals(CreatureSize.LARGE, sizeForHeightCm(300.0))
    }

    @Test
    fun proficiencyBonus_scalesEveryFourLevels() {
        assertEquals(2, proficiencyBonusForLevel(1))
        assertEquals(2, proficiencyBonusForLevel(4))
        assertEquals(3, proficiencyBonusForLevel(5))
        assertEquals(4, proficiencyBonusForLevel(9))
        assertEquals(5, proficiencyBonusForLevel(13))
        assertEquals(6, proficiencyBonusForLevel(17))
        assertEquals(6, proficiencyBonusForLevel(20))
    }

    @Test
    fun proficiencyBonus_clampsNonPositiveLevelToOne() {
        assertEquals(2, proficiencyBonusForLevel(0))
        assertEquals(2, proficiencyBonusForLevel(-3))
    }

    @Test
    fun initiative_combinesDexModifierAndBonus() {
        assertEquals(5, calculateInitiative(dexterityScore = 14, initiativeBonus = 3))
        assertEquals(-1, calculateInitiative(dexterityScore = 8, initiativeBonus = 0))
    }

    @Test
    fun scoreForSpellcastingAbility_selectsMatchingStat() {
        val character = baseCharacter().copy(
            strength = 8, dexterity = 10, constitution = 12,
            intelligence = 14, wisdom = 16, charisma = 18
        )
        assertEquals(8, scoreForSpellcastingAbility(character, SpellcastingAbility.STRENGTH))
        assertEquals(10, scoreForSpellcastingAbility(character, SpellcastingAbility.DEXTERITY))
        assertEquals(12, scoreForSpellcastingAbility(character, SpellcastingAbility.CONSTITUTION))
        assertEquals(14, scoreForSpellcastingAbility(character, SpellcastingAbility.INTELLIGENCE))
        assertEquals(16, scoreForSpellcastingAbility(character, SpellcastingAbility.WISDOM))
        assertEquals(18, scoreForSpellcastingAbility(character, SpellcastingAbility.CHARISMA))
    }

    @Test
    fun armorClass_withoutArmorUsesBasePlusDexterity() {
        val ac = calculateArmorClass(baseArmorClass = 10, dexterityScore = 16, inventoryItems = emptyList())
        assertEquals(13, ac)
    }

    @Test
    fun armorClass_lightArmorAddsFullDexterity() {
        val armor = armorItem(InventoryArmorType.LIGHT, armorClass = 11, appliesDex = true, maxDex = null)
        val ac = calculateArmorClass(baseArmorClass = 10, dexterityScore = 16, inventoryItems = listOf(armor))
        assertEquals(14, ac)
    }

    @Test
    fun armorClass_mediumArmorCapsDexterityBonus() {
        val armor = armorItem(InventoryArmorType.MEDIUM, armorClass = 14, appliesDex = true, maxDex = 2)
        val ac = calculateArmorClass(baseArmorClass = 10, dexterityScore = 18, inventoryItems = listOf(armor))
        assertEquals(16, ac)
    }

    @Test
    fun armorClass_heavyArmorIgnoresDexterity() {
        val armor = armorItem(InventoryArmorType.HEAVY, armorClass = 18, appliesDex = false, maxDex = null)
        val ac = calculateArmorClass(baseArmorClass = 10, dexterityScore = 16, inventoryItems = listOf(armor))
        assertEquals(18, ac)
    }

    @Test
    fun armorClass_shieldStacksOnTopOfArmor() {
        val armor = armorItem(InventoryArmorType.HEAVY, armorClass = 18, appliesDex = false, maxDex = null)
        val shield = armorItem(InventoryArmorType.SHIELD, armorClass = 2, appliesDex = false, maxDex = null)
        val ac = calculateArmorClass(baseArmorClass = 10, dexterityScore = 16, inventoryItems = listOf(armor, shield))
        assertEquals(20, ac)
    }

    @Test
    fun armorClass_addsMagicBonusOfArmorAndShield() {
        val armor = armorItem(InventoryArmorType.HEAVY, armorClass = 18, appliesDex = false, maxDex = null)
            .copy(isMagical = true, magicalBonus = 1)
        val shield = armorItem(InventoryArmorType.SHIELD, armorClass = 2, appliesDex = false, maxDex = null)
            .copy(isMagical = true, magicalBonus = 2)
        val ac = calculateArmorClass(baseArmorClass = 10, dexterityScore = 10, inventoryItems = listOf(armor, shield))
        assertEquals(23, ac)
    }

    @Test
    fun armorClass_ignoresBonusOfMundaneArmor() {
        val armor = armorItem(InventoryArmorType.HEAVY, armorClass = 18, appliesDex = false, maxDex = null)
            .copy(isMagical = false, magicalBonus = 1)
        val ac = calculateArmorClass(baseArmorClass = 10, dexterityScore = 10, inventoryItems = listOf(armor))
        assertEquals(18, ac)
    }

    @Test
    fun armorClass_onlyCountsEquippedItems() {
        val unequipped = armorItem(InventoryArmorType.HEAVY, armorClass = 18, appliesDex = false, maxDex = null)
            .copy(isEquipped = false)
        val ac = calculateArmorClass(baseArmorClass = 10, dexterityScore = 14, inventoryItems = listOf(unequipped))
        assertEquals(12, ac)
    }

    @Test
    fun armorClass_neverDropsBelowOne() {
        val ac = calculateArmorClass(baseArmorClass = 1, dexterityScore = 1, inventoryItems = emptyList())
        assertEquals(1, ac)
    }

    @Test
    fun appliedDexterityModifier_respectsToggleAndCap() {
        val noBonus = InventoryArmorDetails(
            armorType = InventoryArmorType.HEAVY,
            armorClass = 18, appliesDexterityBonus = false, maxDexterityBonus = null,
            strengthMinimum = 0, hasStealthDisadvantage = false
        )
        assertEquals(0, noBonus.appliedDexterityModifier(5))

        val capped = noBonus.copy(appliesDexterityBonus = true, maxDexterityBonus = 2)
        assertEquals(2, capped.appliedDexterityModifier(5))
        assertEquals(1, capped.appliedDexterityModifier(1))
    }

    private fun baseCharacter(): Character = defaultCharacterBundle(now = 0L).character

    private fun armorItem(
        type: InventoryArmorType,
        armorClass: Int,
        appliesDex: Boolean,
        maxDex: Int?
    ): InventoryItem = InventoryItem(
        name = type.name,
        category = InventoryCategory.ARMOR,
        weight = 0.0,
        quantity = 1,
        isEquipped = true,
        icon = "",
        armorDetails = InventoryArmorDetails(
            armorType = type,
            armorClass = armorClass,
            appliesDexterityBonus = appliesDex,
            maxDexterityBonus = maxDex,
            strengthMinimum = 0,
            hasStealthDisadvantage = false
        )
    )

    @Test
    fun aLargeCharacterCarriesTwiceAsMuch() {
        org.junit.Assert.assertEquals(240.0, carryingCapacity(16, com.dndcharacterhandler.domain.dnd5e.model.CreatureSize.MEDIUM), 0.0)
        org.junit.Assert.assertEquals(240.0, carryingCapacity(16, com.dndcharacterhandler.domain.dnd5e.model.CreatureSize.SMALL), 0.0)
        org.junit.Assert.assertEquals(480.0, carryingCapacity(16, com.dndcharacterhandler.domain.dnd5e.model.CreatureSize.LARGE), 0.0)
    }
}
