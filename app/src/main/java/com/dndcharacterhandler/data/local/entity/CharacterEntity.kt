package com.dndcharacterhandler.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import androidx.room.Embedded
import com.dndcharacterhandler.domain.model.ArmorClassMode
import com.dndcharacterhandler.domain.model.AttackCalculationMode
import com.dndcharacterhandler.domain.model.DarkvisionMode
import com.dndcharacterhandler.domain.model.FeatureSource
import com.dndcharacterhandler.domain.model.InventoryArmorType
import com.dndcharacterhandler.domain.model.InventoryCategory
import com.dndcharacterhandler.domain.model.InventoryWeaponClass
import com.dndcharacterhandler.domain.model.InventoryWeaponRangeType
import com.dndcharacterhandler.domain.model.SpellcastingAbility

@Entity(tableName = "characters")
data class CharacterEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
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
    val copperPieces: Int,
    val silverPieces: Int,
    val goldPieces: Int,
    val speed: Int,
    val initiative: Int,
    val initiativeBonus: Int,
    val spellcastingAbility: SpellcastingAbility,
    val spellSlotMaximums: String,
    val spellSlotRemaining: String,
    val spellSlotsRestoreOnShortRest: Boolean,
    val spellSlotsRestoreOnLongRest: Boolean,
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

@Entity(
    tableName = "skills",
    foreignKeys = [
        ForeignKey(
            entity = CharacterEntity::class,
            parentColumns = ["id"],
            childColumns = ["characterOwnerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("characterOwnerId")]
)
data class SkillEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val characterOwnerId: Long,
    val name: String,
    val isProficient: Boolean,
    val isExpertise: Boolean,
    val hasJackOfAllTrades: Boolean
)

@Entity(
    tableName = "attacks",
    foreignKeys = [
        ForeignKey(
            entity = CharacterEntity::class,
            parentColumns = ["id"],
            childColumns = ["characterOwnerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("characterOwnerId")]
)
data class AttackEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val characterOwnerId: Long,
    val name: String,
    val icon: String,
    val isProficient: Boolean,
    val calculationMode: AttackCalculationMode,
    val ability: SpellcastingAbility,
    val normalRange: Int?,
    val longRange: Int?,
    val damageDiceCount: Int,
    val damageDieType: String,
    val alternateDamageDiceCount: Int?,
    val alternateDamageDieType: String?,
    val alternateDamageType: String?,
    val magicalBonus: Int,
    val applyAbilityModifierToDamage: Boolean,
    val range: String,
    val attackBonusOrSaveDc: String,
    val damage: String,
    val damageType: String
)

@Entity(
    tableName = "combat_resources",
    foreignKeys = [
        ForeignKey(
            entity = CharacterEntity::class,
            parentColumns = ["id"],
            childColumns = ["characterOwnerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("characterOwnerId")]
)
data class CombatResourceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val characterOwnerId: Long,
    val name: String,
    val currentUses: Int,
    val maximumUses: Int,
    val restoresOnShortRest: Boolean,
    val restoresOnLongRest: Boolean
)

@Entity(
    tableName = "inventory_items",
    foreignKeys = [
        ForeignKey(
            entity = CharacterEntity::class,
            parentColumns = ["id"],
            childColumns = ["characterOwnerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("characterOwnerId")]
)
data class InventoryItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val characterOwnerId: Long,
    val name: String,
    val description: String,
    val isMagical: Boolean,
    val magicalBonus: Int,
    val category: InventoryCategory,
    val weight: Double,
    val quantity: Int,
    val isEquipped: Boolean,
    val icon: String,
    val costQuantity: Int?,
    val costUnit: String?,
    val armorType: InventoryArmorType?,
    val armorClass: Int?,
    val appliesDexterityBonus: Boolean?,
    val maxDexterityBonus: Int?,
    val strengthMinimum: Int?,
    val hasStealthDisadvantage: Boolean?,
    val weaponClass: InventoryWeaponClass?,
    val weaponRangeType: InventoryWeaponRangeType?,
    val weaponBaseId: String?,
    val weaponNormalRange: Int?,
    val weaponLongRange: Int?,
    val weaponPrimaryDamageDice: String?,
    val weaponPrimaryDamageType: String?,
    val weaponTwoHandedDamageDice: String?,
    val weaponTwoHandedDamageType: String?,
    val weaponProperties: String?,
    val catalogId: String? = null
)

@Entity(
    tableName = "spells",
    foreignKeys = [
        ForeignKey(
            entity = CharacterEntity::class,
            parentColumns = ["id"],
            childColumns = ["characterOwnerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("characterOwnerId")]
)
data class SpellEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val characterOwnerId: Long,
    val catalogId: String?,
    val name: String,
    val level: Int,
    val school: String,
    val isPrepared: Boolean,
    val description: String,
    val higherLevelDescription: String,
    val range: String,
    val castingTime: String,
    val duration: String,
    val components: String,
    val material: String,
    val materialCost: String,
    val isRitual: Boolean,
    val requiresConcentration: Boolean,
    val attackType: String,
    val availableClasses: String,
    val damageType: String,
    val damageBase: String,
    val damageBonusValue: Int,
    val damageBonusIsModifier: Boolean,
    val altDamageBase: String,
    val altDamageType: String,
    val altDamageBonusValue: Int,
    val altDamageBonusIsModifier: Boolean,
    val damage: String,
    val saveAbility: String,
    val saveEffect: String,
    val areaOfEffect: String,
    val healBase: String,
    val healBonusValue: Int,
    val healBonusIsModifier: Boolean,
    val healing: String
)

@Entity(
    tableName = "spell_attacks",
    foreignKeys = [
        ForeignKey(
            entity = CharacterEntity::class,
            parentColumns = ["id"],
            childColumns = ["characterOwnerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("characterOwnerId")]
)
data class SpellAttackEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val characterOwnerId: Long,
    val catalogId: String?,
    val name: String,
    val level: Int,
    val school: String,
    val isPrepared: Boolean,
    val description: String,
    val higherLevelDescription: String,
    val range: String,
    val castingTime: String,
    val duration: String,
    val components: String,
    val material: String,
    val materialCost: String,
    val isRitual: Boolean,
    val requiresConcentration: Boolean,
    val attackType: String,
    val availableClasses: String,
    val damageType: String,
    val damageBase: String,
    val damageBonusValue: Int,
    val damageBonusIsModifier: Boolean,
    val altDamageBase: String,
    val altDamageType: String,
    val altDamageBonusValue: Int,
    val altDamageBonusIsModifier: Boolean,
    val damage: String,
    val saveAbility: String,
    val saveEffect: String,
    val areaOfEffect: String,
    val healBase: String,
    val healBonusValue: Int,
    val healBonusIsModifier: Boolean,
    val healing: String
)

@Entity(
    tableName = "features",
    foreignKeys = [
        ForeignKey(
            entity = CharacterEntity::class,
            parentColumns = ["id"],
            childColumns = ["characterOwnerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("characterOwnerId")]
)
data class FeatureEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val characterOwnerId: Long,
    val name: String,
    val description: String,
    val level: Int?,
    val source: FeatureSource,
    val category: String = "",
    val catalogId: String? = null
)

@Entity(
    tableName = "notes",
    foreignKeys = [
        ForeignKey(
            entity = CharacterEntity::class,
            parentColumns = ["id"],
            childColumns = ["characterOwnerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("characterOwnerId")]
)
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val characterOwnerId: Long,
    val title: String,
    val createdDate: Long,
    val updatedDate: Long,
    val content: String,
    val isPinned: Boolean = false
)

data class CharacterWithDetails(
    @Embedded val character: CharacterEntity,
    @Relation(parentColumn = "id", entityColumn = "characterOwnerId")
    val skills: List<SkillEntity>,
    @Relation(parentColumn = "id", entityColumn = "characterOwnerId")
    val attacks: List<AttackEntity>,
    @Relation(parentColumn = "id", entityColumn = "characterOwnerId")
    val combatResources: List<CombatResourceEntity>,
    @Relation(parentColumn = "id", entityColumn = "characterOwnerId")
    val inventoryItems: List<InventoryItemEntity>,
    @Relation(parentColumn = "id", entityColumn = "characterOwnerId")
    val spells: List<SpellEntity>,
    @Relation(parentColumn = "id", entityColumn = "characterOwnerId")
    val spellAttacks: List<SpellAttackEntity>,
    @Relation(parentColumn = "id", entityColumn = "characterOwnerId")
    val features: List<FeatureEntity>,
    @Relation(parentColumn = "id", entityColumn = "characterOwnerId")
    val notes: List<NoteEntity>
)
