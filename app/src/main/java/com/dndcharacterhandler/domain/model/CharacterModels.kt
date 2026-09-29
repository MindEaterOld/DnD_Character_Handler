package com.dndcharacterhandler.domain.model

enum class ArmorClassMode { AUTOMATIC, MANUAL }
enum class AttackCalculationMode { AUTOMATIC, MANUAL }
enum class DarkvisionMode { AUTO, MANUAL }
enum class SpellcastingAbility { STRENGTH, DEXTERITY, CONSTITUTION, INTELLIGENCE, WISDOM, CHARISMA }

data class Character(
    val id: Long = 0,
    val name: String,
    val race: String,
    val characterClass: String,
    val subclass: String,
    val level: Int,
    val portraitUri: String?,
    val currentHp: Int,
    val maxHp: Int,
    val temporaryHp: Int,
    val hitDieSides: Int,
    val spentHitDice: Int,
    val hasInspiration: Boolean,
    val armorClass: Int,
    val baseArmorClass: Int,
    val armorClassMode: ArmorClassMode,
    val copperPieces: Int = 0,
    val silverPieces: Int = 0,
    val goldPieces: Int = 0,
    val speed: Int,
    val initiative: Int,
    val initiativeBonus: Int = 0,
    val spellcastingAbility: SpellcastingAbility = SpellcastingAbility.WISDOM,
    val spellSlotMaximums: String = "",
    val spellSlotRemaining: String = "",
    val spellSlotsRestoreOnShortRest: Boolean = false,
    val spellSlotsRestoreOnLongRest: Boolean = true,
    val experience: Int,
    val strength: Int,
    val dexterity: Int,
    val constitution: Int,
    val intelligence: Int,
    val wisdom: Int,
    val charisma: Int,
    val strengthSaveProficient: Boolean,
    val dexteritySaveProficient: Boolean,
    val constitutionSaveProficient: Boolean,
    val intelligenceSaveProficient: Boolean,
    val wisdomSaveProficient: Boolean,
    val charismaSaveProficient: Boolean,
    val passivePerceptionBonus: Int,
    val darkvisionMode: DarkvisionMode = DarkvisionMode.AUTO,
    val darkvisionManualFeet: Int = 0,
    val armorProficiencies: String,
    val weaponProficiencies: String,
    val toolProficiencies: String,
    val languageProficiencies: String,
    val alignment: String,
    val background: String,
    val faith: String,
    val homeland: String,
    val age: String,
    val gender: String,
    val height: String,
    val weight: String,
    val eyes: String,
    val hair: String,
    val skin: String,
    val personalityTraits: String,
    val ideals: String,
    val bonds: String,
    val flaws: String,
    val biography: String,
    val createdAt: Long,
    val updatedAt: Long
)

data class CharacterBundle(
    val character: Character,
    val skills: List<Skill>,
    val attacks: List<Attack>,
    val combatResources: List<CombatResource>,
    val inventoryItems: List<InventoryItem>,
    val spells: List<Spell>,
    val spellAttacks: List<Spell> = emptyList(),
    val features: List<Feature>,
    val notes: List<Note>
)

data class Skill(
    val id: Long = 0,
    val name: String,
    val isProficient: Boolean,
    val isExpertise: Boolean = false,
    val hasJackOfAllTrades: Boolean = false
)
data class Attack(
    val id: Long = 0,
    val name: String,
    val icon: String,
    val isProficient: Boolean = false,
    val calculationMode: AttackCalculationMode = AttackCalculationMode.AUTOMATIC,
    val ability: SpellcastingAbility = SpellcastingAbility.STRENGTH,
    val normalRange: Int? = null,
    val longRange: Int? = null,
    val damageDiceCount: Int = 1,
    val damageDieType: String = "d4",
    val alternateDamageDiceCount: Int? = null,
    val alternateDamageDieType: String? = null,
    val alternateDamageType: String? = null,
    val magicalBonus: Int = 0,
    val applyAbilityModifierToDamage: Boolean = true,
    val manualAttackBonusOrSaveDc: String = "",
    val manualDamage: String = "",
    val primaryDamageType: String
)
data class CombatResource(
    val id: Long = 0,
    val name: String,
    val currentUses: Int,
    val maximumUses: Int,
    val restoresOnShortRest: Boolean = false,
    val restoresOnLongRest: Boolean = false
)
enum class InventoryCategory { WEAPON, ARMOR, CONSUMABLE, OTHER }
enum class InventoryArmorType { LIGHT, MEDIUM, HEAVY, SHIELD }
enum class InventoryWeaponClass { SIMPLE, MARTIAL }
enum class InventoryWeaponRangeType { MELEE, RANGED }
enum class InventoryWeaponProperty {
    AMMUNITION,
    FINESSE,
    HEAVY,
    LIGHT,
    LOADING,
    REACH,
    THROWN,
    TWO_HANDED,
    VERSATILE
}

data class InventoryArmorDetails(
    val armorType: InventoryArmorType,
    val armorClass: Int,
    val appliesDexterityBonus: Boolean,
    val maxDexterityBonus: Int?,
    val strengthMinimum: Int,
    val hasStealthDisadvantage: Boolean
)

data class InventoryWeaponDamage(
    val dice: String,
    val damageType: String
)

data class InventoryWeaponDetails(
    val weaponClass: InventoryWeaponClass,
    val rangeType: InventoryWeaponRangeType,
    val baseWeaponId: String? = null,
    val normalRange: Int?,
    val longRange: Int?,
    val damages: List<InventoryWeaponDamage>,
    val twoHandedDamage: InventoryWeaponDamage? = null,
    val properties: Set<InventoryWeaponProperty> = emptySet()
)

data class InventoryItem(
    val id: Long = 0,
    val name: String,
    val description: String = "",
    val isMagical: Boolean = false,
    val magicalBonus: Int = 1,
    val category: InventoryCategory,
    val weight: Double,
    val quantity: Int,
    val isEquipped: Boolean,
    val icon: String,
    val costQuantity: Int? = null,
    val costUnit: String? = null,
    val armorDetails: InventoryArmorDetails? = null,
    val weaponDetails: InventoryWeaponDetails? = null
)
data class Spell(
    val id: Long = 0,
    val catalogId: String? = null,
    val name: String,
    val level: Int,
    val school: String,
    val isPrepared: Boolean,
    val description: String,
    val higherLevelDescription: String = "",
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
    val healing: String = ""
)
enum class FeatureSource { RACE, BACKGROUND, CLASS, OTHER }
data class Feature(
    val id: Long = 0,
    val name: String,
    val description: String,
    val level: Int? = null,
    val source: FeatureSource = FeatureSource.OTHER,
    val category: String = "",
    /** [FeatureCatalogItem.id] this feature was added from, or null for hand-made features. */
    val catalogId: String? = null
)
data class Note(
    val id: Long = 0,
    val title: String,
    val createdDate: Long,
    val updatedDate: Long,
    val content: String,
    val isPinned: Boolean = false
)

fun defaultCharacterBundle(now: Long = System.currentTimeMillis()): CharacterBundle {
    val skills = listOf(
        "skill_acrobatics", "skill_animal_handling", "skill_arcana", "skill_athletics", "skill_deception",
        "skill_history", "skill_insight", "skill_intimidation", "skill_investigation", "skill_medicine",
        "skill_nature", "skill_perception", "skill_performance", "skill_persuasion", "skill_religion",
        "skill_sleight_of_hand", "skill_stealth", "skill_survival"
    ).map { Skill(name = it, isProficient = false, isExpertise = false, hasJackOfAllTrades = false) }

    return CharacterBundle(
        character = Character(
            name = "",
            race = "",
            characterClass = "",
            subclass = "",
            level = 1,
            portraitUri = null,
            currentHp = 8,
            maxHp = 8,
            temporaryHp = 0,
            hitDieSides = 8,
            spentHitDice = 0,
            hasInspiration = false,
            armorClass = 10,
            baseArmorClass = 10,
            armorClassMode = ArmorClassMode.AUTOMATIC,
            copperPieces = 0,
            silverPieces = 0,
            goldPieces = 0,
            speed = 30,
            initiative = 0,
            initiativeBonus = 0,
            spellcastingAbility = SpellcastingAbility.WISDOM,
            experience = 0,
            strength = 10,
            dexterity = 10,
            constitution = 10,
            intelligence = 10,
            wisdom = 10,
            charisma = 10,
            strengthSaveProficient = false,
            dexteritySaveProficient = false,
            constitutionSaveProficient = false,
            intelligenceSaveProficient = false,
            wisdomSaveProficient = false,
            charismaSaveProficient = false,
            passivePerceptionBonus = 0,
            armorProficiencies = "",
            weaponProficiencies = "",
            toolProficiencies = "",
            languageProficiencies = "",
            alignment = "",
            background = "",
            faith = "",
            homeland = "",
            age = "",
            gender = "",
            height = "",
            weight = "",
            eyes = "",
            hair = "",
            skin = "",
            personalityTraits = "",
            ideals = "",
            bonds = "",
            flaws = "",
            biography = "",
            createdAt = now,
            updatedAt = now
        ),
        skills = skills,
        attacks = emptyList(),
        combatResources = emptyList(),
        inventoryItems = emptyList(),
        spells = emptyList(),
        features = emptyList(),
        notes = emptyList()
    )
}
