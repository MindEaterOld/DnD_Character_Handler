package com.dndcharacterhandler.data.repository

import com.dndcharacterhandler.domain.model.Attack
import com.dndcharacterhandler.domain.model.AttackCalculationMode
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
import com.dndcharacterhandler.domain.model.defaultCharacterBundle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CharacterArchiveCodecTest {

    @Test
    fun fullBundle_roundTripsLosslessly() {
        val original = richBundle()

        val restored = roundTrip(original)

        assertEquals(original, restored)
    }

    @Test
    fun missingCollections_importAsEmptyInsteadOfThrowing() {
        val manifest = richBundle().toArchiveManifest(exportedAt = 0L, mapAssetReference = identityMap)
        manifest.remove("skills")
        manifest.remove("attacks")
        manifest.remove("inventoryItems")
        manifest.remove("spells")
        manifest.remove("features")
        manifest.remove("notes")
        manifest.remove("combatResources")

        val imported = archiveManifestToCharacterBundle(manifest, resolveAssetReference = identityResolve)

        assertTrue(imported.characterBundle.skills.isEmpty())
        assertTrue(imported.characterBundle.attacks.isEmpty())
        assertTrue(imported.characterBundle.inventoryItems.isEmpty())
        assertTrue(imported.characterBundle.spells.isEmpty())
        assertTrue(imported.characterBundle.features.isEmpty())
        assertTrue(imported.characterBundle.notes.isEmpty())
        assertTrue(imported.characterBundle.combatResources.isEmpty())
        assertEquals("Aluen", imported.characterBundle.character.name)
    }

    @Test
    fun unknownEnumValues_fallBackToDefaults() {
        val manifest = richBundle().toArchiveManifest(exportedAt = 0L, mapAssetReference = identityMap)
        manifest.getJSONObject("character").put("armorClassMode", "TOTALLY_BOGUS")
        manifest.getJSONArray("inventoryItems").getJSONObject(0).put("category", "NOPE")

        val imported = archiveManifestToCharacterBundle(manifest, resolveAssetReference = identityResolve)

        assertEquals(
            com.dndcharacterhandler.domain.model.ArmorClassMode.AUTOMATIC,
            imported.characterBundle.character.armorClassMode
        )
        assertEquals(InventoryCategory.OTHER, imported.characterBundle.inventoryItems[0].category)
    }

    @Test
    fun importedCharacterIdIsReset() {
        val original = richBundle().let { it.copy(character = it.character.copy(id = 999)) }

        val restored = roundTrip(original)

        assertEquals(0L, restored.character.id)
    }

    private fun roundTrip(bundle: CharacterBundle): CharacterBundle {
        val manifest = bundle.toArchiveManifest(exportedAt = 0L, mapAssetReference = identityMap)
        return archiveManifestToCharacterBundle(manifest, resolveAssetReference = identityResolve).characterBundle
    }

    private val identityMap: (String?, String) -> String? = { source, _ -> source }
    private val identityResolve: (String?) -> String? = { it }

    private fun richBundle(): CharacterBundle {
        val base = defaultCharacterBundle(now = 1_000L)
        return base.copy(
            character = base.character.copy(
                id = 0,
                name = "Aluen",
                race = "Elf",
                characterClass = "Ranger",
                subclass = "Hunter",
                level = 7,
                portraitUri = "res:drawable/portrait_aluen",
                currentHp = 41,
                maxHp = 52,
                temporaryHp = 6,
                hitDieSides = 10,
                spentHitDice = 2,
                hasInspiration = true,
                armorClass = 16,
                baseArmorClass = 12,
                copperPieces = 9,
                silverPieces = 4,
                goldPieces = 120,
                speed = 35,
                initiative = 3,
                initiativeBonus = 1,
                spellcastingAbility = SpellcastingAbility.WISDOM,
                spellSlotMaximums = "4,3,2",
                spellSlotRemaining = "4,2,1",
                spellSlotsRestoreOnShortRest = false,
                spellSlotsRestoreOnLongRest = true,
                experience = 23000,
                strength = 12,
                dexterity = 18,
                constitution = 14,
                intelligence = 10,
                wisdom = 16,
                charisma = 8,
                dexteritySaveProficient = true,
                wisdomSaveProficient = true,
                passivePerceptionBonus = 2,
                armorProficiencies = "Light, Medium",
                weaponProficiencies = "Simple, Martial",
                alignment = "Chaotic Good",
                background = "Outlander",
                biography = "Raised in the Quivering Forest."
            ),
            skills = listOf(
                Skill(name = "skill_perception", isProficient = true, isExpertise = true, hasJackOfAllTrades = false),
                Skill(name = "skill_stealth", isProficient = true, isExpertise = false, hasJackOfAllTrades = true)
            ),
            attacks = listOf(
                Attack(
                    name = "Longbow",
                    icon = "res:drawable/longbow",
                    isProficient = true,
                    calculationMode = AttackCalculationMode.AUTOMATIC,
                    ability = SpellcastingAbility.DEXTERITY,
                    normalRange = 150,
                    longRange = 600,
                    damageDiceCount = 1,
                    damageDieType = "d8",
                    alternateDamageDiceCount = 1,
                    alternateDamageDieType = "d6",
                    alternateDamageType = "fire",
                    magicalBonus = 1,
                    applyAbilityModifierToDamage = true,
                    manualAttackBonusOrSaveDc = "+9",
                    manualDamage = "1d8+5",
                    primaryDamageType = "piercing"
                )
            ),
            combatResources = listOf(
                CombatResource(
                    name = "Superiority Dice",
                    currentUses = 2,
                    maximumUses = 4,
                    restoresOnShortRest = true,
                    restoresOnLongRest = true
                )
            ),
            inventoryItems = listOf(
                InventoryItem(
                    name = "Half Plate",
                    description = "Polished steel.",
                    isMagical = false,
                    magicalBonus = 1,
                    category = InventoryCategory.ARMOR,
                    weight = 40.0,
                    quantity = 1,
                    isEquipped = true,
                    icon = "res:drawable/half_plate",
                    costQuantity = 750,
                    costUnit = "gp",
                    armorDetails = InventoryArmorDetails(
                        armorType = InventoryArmorType.MEDIUM,
                        armorClass = 15,
                        appliesDexterityBonus = true,
                        maxDexterityBonus = 2,
                        strengthMinimum = 0,
                        hasStealthDisadvantage = true
                    )
                ),
                InventoryItem(
                    name = "Flame Tongue",
                    description = "A magic longsword.",
                    isMagical = true,
                    magicalBonus = 2,
                    category = InventoryCategory.WEAPON,
                    weight = 3.0,
                    quantity = 1,
                    isEquipped = false,
                    icon = "res:drawable/flame_tongue",
                    costQuantity = null,
                    costUnit = null,
                    weaponDetails = InventoryWeaponDetails(
                        weaponClass = InventoryWeaponClass.MARTIAL,
                        rangeType = InventoryWeaponRangeType.MELEE,
                        baseWeaponId = "longsword",
                        normalRange = null,
                        longRange = null,
                        damages = listOf(
                            InventoryWeaponDamage(dice = "1d8", damageType = "slashing"),
                            InventoryWeaponDamage(dice = "2d6", damageType = "fire")
                        ),
                        twoHandedDamage = InventoryWeaponDamage(dice = "1d10", damageType = "slashing"),
                        properties = setOf(InventoryWeaponProperty.VERSATILE, InventoryWeaponProperty.FINESSE)
                    )
                )
            ),
            spells = listOf(
                Spell(
                    catalogId = "hunters-mark",
                    name = "Hunter's Mark",
                    level = 1,
                    school = "Divination",
                    isPrepared = true,
                    description = "Mark a target.",
                    higherLevelDescription = "More concentration slots.",
                    range = "90 feet",
                    castingTime = "1 bonus action",
                    duration = "Concentration, up to 1 hour",
                    components = "V",
                    material = "",
                    isRitual = false,
                    requiresConcentration = true,
                    attackType = "",
                    availableClasses = "Ranger"
                )
            ),
            spellAttacks = listOf(
                Spell(
                    catalogId = "cure-wounds",
                    name = "Cure Wounds",
                    level = 1,
                    school = "Evocation",
                    isPrepared = true,
                    description = "Heal a creature.",
                    range = "Touch",
                    castingTime = "1 action",
                    duration = "Instantaneous",
                    components = "V, S, M",
                    material = "a diamond",
                    materialCost = "50",
                    isRitual = true,
                    availableClasses = "Cleric",
                    areaOfEffect = "cube, 15 ft",
                    healBase = "2d8",
                    healBonusValue = 3
                )
            ),
            features = listOf(
                Feature(
                    name = "Favored Enemy",
                    description = "Advantage on tracking.",
                    level = 1,
                    source = FeatureSource.CLASS,
                    catalogId = "CLASS:favored-enemy"
                )
            ),
            notes = listOf(
                Note(
                    title = "Quest log",
                    createdDate = 10L,
                    updatedDate = 20L,
                    content = "Find the stag.",
                    isPinned = true
                )
            )
        )
    }
}
