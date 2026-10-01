package com.dndcharacterhandler.data.local

import androidx.room.TypeConverter
import com.dndcharacterhandler.domain.model.ArmorClassMode
import com.dndcharacterhandler.domain.model.AttackCalculationMode
import com.dndcharacterhandler.domain.model.CreatureSize
import com.dndcharacterhandler.domain.model.DarkvisionMode
import com.dndcharacterhandler.domain.model.FeatureSource
import com.dndcharacterhandler.domain.model.InventoryArmorType
import com.dndcharacterhandler.domain.model.InventoryCategory
import com.dndcharacterhandler.domain.model.InventoryWeaponClass
import com.dndcharacterhandler.domain.model.InventoryWeaponRangeType
import com.dndcharacterhandler.domain.model.SpellcastingAbility

class RoomConverters {
    @TypeConverter
    fun fromArmorClassMode(value: ArmorClassMode): String = value.name

    @TypeConverter
    fun toArmorClassMode(value: String): ArmorClassMode =
        runCatching { ArmorClassMode.valueOf(value) }.getOrDefault(ArmorClassMode.AUTOMATIC)

    @TypeConverter
    fun fromAttackCalculationMode(value: AttackCalculationMode): String = value.name

    @TypeConverter
    fun toAttackCalculationMode(value: String): AttackCalculationMode =
        runCatching { AttackCalculationMode.valueOf(value) }.getOrDefault(AttackCalculationMode.AUTOMATIC)

    @TypeConverter
    fun fromDarkvisionMode(value: DarkvisionMode): String = value.name

    @TypeConverter
    fun toDarkvisionMode(value: String): DarkvisionMode =
        runCatching { DarkvisionMode.valueOf(value) }.getOrDefault(DarkvisionMode.AUTO)

    @TypeConverter
    fun fromCreatureSize(value: CreatureSize): String = value.name

    @TypeConverter
    fun toCreatureSize(value: String): CreatureSize =
        runCatching { CreatureSize.valueOf(value) }.getOrDefault(CreatureSize.MEDIUM)

    @TypeConverter
    fun fromInventoryCategory(value: InventoryCategory): String = value.name

    @TypeConverter
    fun toInventoryCategory(value: String): InventoryCategory =
        runCatching { InventoryCategory.valueOf(value) }.getOrDefault(InventoryCategory.OTHER)

    @TypeConverter
    fun fromInventoryArmorType(value: InventoryArmorType?): String? = value?.name

    @TypeConverter
    fun toInventoryArmorType(value: String?): InventoryArmorType? =
        value?.let { runCatching { InventoryArmorType.valueOf(it) }.getOrNull() }

    @TypeConverter
    fun fromInventoryWeaponClass(value: InventoryWeaponClass?): String? = value?.name

    @TypeConverter
    fun toInventoryWeaponClass(value: String?): InventoryWeaponClass? =
        value?.let { runCatching { InventoryWeaponClass.valueOf(it) }.getOrNull() }

    @TypeConverter
    fun fromInventoryWeaponRangeType(value: InventoryWeaponRangeType?): String? = value?.name

    @TypeConverter
    fun toInventoryWeaponRangeType(value: String?): InventoryWeaponRangeType? =
        value?.let { runCatching { InventoryWeaponRangeType.valueOf(it) }.getOrNull() }

    @TypeConverter
    fun fromFeatureSource(value: FeatureSource): String = value.name

    @TypeConverter
    fun toFeatureSource(value: String): FeatureSource =
        runCatching { FeatureSource.valueOf(value) }.getOrDefault(FeatureSource.OTHER)

    @TypeConverter
    fun fromSpellcastingAbility(value: SpellcastingAbility): String = value.name

    @TypeConverter
    fun toSpellcastingAbility(value: String): SpellcastingAbility =
        runCatching { SpellcastingAbility.valueOf(value) }.getOrDefault(SpellcastingAbility.WISDOM)
}
