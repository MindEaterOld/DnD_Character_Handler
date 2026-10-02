package com.dndcharacterhandler.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.migration.Migration
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.dndcharacterhandler.data.local.dao.CharacterDao
import com.dndcharacterhandler.data.local.entity.AttackEntity
import com.dndcharacterhandler.data.local.entity.CharacterEntity
import com.dndcharacterhandler.data.local.entity.CombatResourceEntity
import com.dndcharacterhandler.data.local.entity.FeatureEntity
import com.dndcharacterhandler.data.local.entity.InventoryItemEntity
import com.dndcharacterhandler.data.local.entity.NoteEntity
import com.dndcharacterhandler.data.local.entity.SkillEntity
import com.dndcharacterhandler.data.local.entity.SpellAttackEntity
import com.dndcharacterhandler.data.local.entity.SpellEntity

@Database(
    entities = [
        CharacterEntity::class,
        SkillEntity::class,
        AttackEntity::class,
        CombatResourceEntity::class,
        InventoryItemEntity::class,
        SpellEntity::class,
        SpellAttackEntity::class,
        FeatureEntity::class,
        NoteEntity::class
    ],
    version = 50,
    exportSchema = false
)
@TypeConverters(RoomConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun characterDao(): CharacterDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "dnd_character_handler.db"
                ).addMigrations(
                    MIGRATION_1_2,
                    MIGRATION_2_3,
                    MIGRATION_3_4,
                    MIGRATION_4_5,
                    MIGRATION_5_6,
                    MIGRATION_6_7,
                    MIGRATION_7_8,
                    MIGRATION_8_9,
                    MIGRATION_9_10,
                    MIGRATION_10_11,
                    MIGRATION_11_12,
                    MIGRATION_12_13,
                    MIGRATION_13_14,
                    MIGRATION_14_15,
                    MIGRATION_15_16,
                    MIGRATION_16_17,
                    MIGRATION_17_18,
                    MIGRATION_18_19,
                    MIGRATION_19_20,
                    MIGRATION_20_21,
                    MIGRATION_21_22,
                    MIGRATION_22_23,
                    MIGRATION_23_24,
                    MIGRATION_24_25,
                    MIGRATION_25_26,
                    MIGRATION_26_27,
                    MIGRATION_27_28,
                    MIGRATION_28_29,
                    MIGRATION_29_30,
                    MIGRATION_30_31,
                    MIGRATION_31_32,
                    MIGRATION_32_33,
                    MIGRATION_33_34,
                    MIGRATION_34_35,
                    MIGRATION_35_36,
                    MIGRATION_36_37,
                    MIGRATION_37_38,
                    MIGRATION_38_39,
                    MIGRATION_39_40,
                    MIGRATION_40_41,
                    MIGRATION_41_42,
                    MIGRATION_42_43,
                    MIGRATION_43_44,
                    MIGRATION_44_45,
                    MIGRATION_45_46,
                    MIGRATION_46_47,
                    MIGRATION_47_48,
                    MIGRATION_48_49,
                    MIGRATION_49_50
                ).build().also { INSTANCE = it }
            }
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE characters ADD COLUMN temporaryHp INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE characters ADD COLUMN hitDieSides INTEGER NOT NULL DEFAULT 8")
                db.execSQL("ALTER TABLE characters ADD COLUMN spentHitDice INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE characters ADD COLUMN hasInspiration INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE characters ADD COLUMN passivePerceptionBonus INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE characters ADD COLUMN strengthSaveProficient INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE characters ADD COLUMN dexteritySaveProficient INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE characters ADD COLUMN constitutionSaveProficient INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE characters ADD COLUMN intelligenceSaveProficient INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE characters ADD COLUMN wisdomSaveProficient INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE characters ADD COLUMN charismaSaveProficient INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE skills ADD COLUMN isExpertise INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE skills ADD COLUMN hasJackOfAllTrades INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE characters ADD COLUMN armorProficiencies TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE characters ADD COLUMN weaponProficiencies TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE characters ADD COLUMN toolProficiencies TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE characters ADD COLUMN languageProficiencies TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE notes ADD COLUMN isPinned INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE inventory_items ADD COLUMN costQuantity INTEGER")
                db.execSQL("ALTER TABLE inventory_items ADD COLUMN costUnit TEXT")
                db.execSQL("ALTER TABLE inventory_items ADD COLUMN armorType TEXT")
                db.execSQL("ALTER TABLE inventory_items ADD COLUMN armorClass INTEGER")
                db.execSQL("ALTER TABLE inventory_items ADD COLUMN appliesDexterityBonus INTEGER")
                db.execSQL("ALTER TABLE inventory_items ADD COLUMN maxDexterityBonus INTEGER")
                db.execSQL("ALTER TABLE inventory_items ADD COLUMN strengthMinimum INTEGER")
                db.execSQL("ALTER TABLE inventory_items ADD COLUMN hasStealthDisadvantage INTEGER")
            }
        }

        private val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE characters ADD COLUMN baseArmorClass INTEGER NOT NULL DEFAULT 10")
                db.execSQL("ALTER TABLE characters ADD COLUMN armorClassMode TEXT NOT NULL DEFAULT 'AUTOMATIC'")
            }
        }

        private val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE inventory_items ADD COLUMN weaponClass TEXT")
                db.execSQL("ALTER TABLE inventory_items ADD COLUMN weaponRangeType TEXT")
                db.execSQL("ALTER TABLE inventory_items ADD COLUMN weaponNormalRange INTEGER")
                db.execSQL("ALTER TABLE inventory_items ADD COLUMN weaponLongRange INTEGER")
                db.execSQL("ALTER TABLE inventory_items ADD COLUMN weaponPrimaryDamageDice TEXT")
                db.execSQL("ALTER TABLE inventory_items ADD COLUMN weaponPrimaryDamageType TEXT")
                db.execSQL("ALTER TABLE inventory_items ADD COLUMN weaponTwoHandedDamageDice TEXT")
                db.execSQL("ALTER TABLE inventory_items ADD COLUMN weaponTwoHandedDamageType TEXT")
            }
        }

        private val MIGRATION_15_16 = object : Migration(15, 16) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE characters ADD COLUMN copperPieces INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE characters ADD COLUMN silverPieces INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE characters ADD COLUMN goldPieces INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_16_17 = object : Migration(16, 17) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE inventory_items ADD COLUMN description TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE inventory_items ADD COLUMN weaponProperties TEXT")
            }
        }

        private val MIGRATION_17_18 = object : Migration(17, 18) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE inventory_items ADD COLUMN isMagical INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_18_19 = object : Migration(18, 19) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE inventory_items ADD COLUMN weaponBaseId TEXT")
            }
        }

        private val MIGRATION_19_20 = object : Migration(19, 20) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE characters ADD COLUMN initiativeBonus INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_20_21 = object : Migration(20, 21) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE features ADD COLUMN source TEXT NOT NULL DEFAULT 'OTHER'")
            }
        }

        private val MIGRATION_21_22 = object : Migration(21, 22) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE features ADD COLUMN level INTEGER")
            }
        }

        private val MIGRATION_22_23 = object : Migration(22, 23) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS features_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        characterOwnerId INTEGER NOT NULL,
                        name TEXT NOT NULL,
                        description TEXT NOT NULL,
                        level INTEGER,
                        source TEXT NOT NULL,
                        FOREIGN KEY(characterOwnerId) REFERENCES characters(id) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO features_new (id, characterOwnerId, name, description, level, source)
                    SELECT id, characterOwnerId, name, description, level, source
                    FROM features
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE features")
                db.execSQL("ALTER TABLE features_new RENAME TO features")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_features_characterOwnerId ON features(characterOwnerId)")
            }
        }

        private val MIGRATION_23_24 = object : Migration(23, 24) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE combat_resources ADD COLUMN restoresOnShortRest INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE combat_resources ADD COLUMN restoresOnLongRest INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_24_25 = object : Migration(24, 25) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE characters ADD COLUMN spellcastingAbility TEXT NOT NULL DEFAULT 'WISDOM'")
            }
        }

        private val MIGRATION_25_26 = object : Migration(25, 26) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE inventory_items ADD COLUMN magicalBonus INTEGER NOT NULL DEFAULT 1")
            }
        }

        private val MIGRATION_26_27 = object : Migration(26, 27) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE attacks ADD COLUMN isProficient INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_27_28 = object : Migration(27, 28) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE attacks ADD COLUMN ability TEXT NOT NULL DEFAULT 'STRENGTH'")
            }
        }

        private val MIGRATION_28_29 = object : Migration(28, 29) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE attacks ADD COLUMN normalRange INTEGER")
                db.execSQL("ALTER TABLE attacks ADD COLUMN longRange INTEGER")
                db.execSQL("ALTER TABLE attacks ADD COLUMN damageDiceCount INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE attacks ADD COLUMN damageDieType TEXT NOT NULL DEFAULT 'd4'")
                db.execSQL("ALTER TABLE attacks ADD COLUMN alternateDamageDiceCount INTEGER")
                db.execSQL("ALTER TABLE attacks ADD COLUMN alternateDamageDieType TEXT")
                db.execSQL("ALTER TABLE attacks ADD COLUMN alternateDamageType TEXT")
                db.execSQL("ALTER TABLE attacks ADD COLUMN magicalBonus INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE attacks ADD COLUMN applyAbilityModifierToDamage INTEGER NOT NULL DEFAULT 1")
            }
        }

        private val MIGRATION_29_30 = object : Migration(29, 30) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE attacks ADD COLUMN calculationMode TEXT NOT NULL DEFAULT 'AUTOMATIC'")
            }
        }

        private val MIGRATION_30_31 = object : Migration(30, 31) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE characters ADD COLUMN spellSlotMaximums TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE characters ADD COLUMN spellSlotRemaining TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE characters ADD COLUMN spellSlotsRestoreOnShortRest INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE characters ADD COLUMN spellSlotsRestoreOnLongRest INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE spells ADD COLUMN catalogId TEXT")
                db.execSQL("ALTER TABLE spells ADD COLUMN higherLevelDescription TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE spells ADD COLUMN range TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE spells ADD COLUMN castingTime TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE spells ADD COLUMN duration TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE spells ADD COLUMN components TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE spells ADD COLUMN material TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE spells ADD COLUMN isRitual INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE spells ADD COLUMN requiresConcentration INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE spells ADD COLUMN attackType TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE spells ADD COLUMN availableClasses TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_31_32 = object : Migration(31, 32) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE spells ADD COLUMN damageType TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE spells ADD COLUMN damage TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE spells ADD COLUMN saveAbility TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE spells ADD COLUMN saveEffect TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE spells ADD COLUMN areaOfEffect TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE spells ADD COLUMN healing TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_32_33 = object : Migration(32, 33) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE spells ADD COLUMN materialCost TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_33_34 = object : Migration(33, 34) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE spells ADD COLUMN damageBase TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_34_35 = object : Migration(34, 35) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE spells ADD COLUMN healBase TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_35_36 = object : Migration(35, 36) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE spells ADD COLUMN altDamageBase TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE spells ADD COLUMN altDamageType TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_36_37 = object : Migration(36, 37) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE spells ADD COLUMN damageBonusValue INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE spells ADD COLUMN damageBonusIsModifier INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE spells ADD COLUMN altDamageBonusValue INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE spells ADD COLUMN altDamageBonusIsModifier INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE spells ADD COLUMN healBonusValue INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE spells ADD COLUMN healBonusIsModifier INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_37_38 = object : Migration(37, 38) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE attacks ADD COLUMN areaOfEffect TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_38_39 = object : Migration(38, 39) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE attacks ADD COLUMN spellLevel INTEGER")
            }
        }

        private val MIGRATION_39_40 = object : Migration(39, 40) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE attacks ADD COLUMN isRitual INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE attacks ADD COLUMN spellComponents TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE attacks ADD COLUMN materialCost TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_40_41 = object : Migration(40, 41) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS spell_attacks (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        characterOwnerId INTEGER NOT NULL,
                        catalogId TEXT,
                        name TEXT NOT NULL,
                        level INTEGER NOT NULL,
                        school TEXT NOT NULL,
                        isPrepared INTEGER NOT NULL,
                        description TEXT NOT NULL,
                        higherLevelDescription TEXT NOT NULL,
                        range TEXT NOT NULL,
                        castingTime TEXT NOT NULL,
                        duration TEXT NOT NULL,
                        components TEXT NOT NULL,
                        material TEXT NOT NULL,
                        materialCost TEXT NOT NULL,
                        isRitual INTEGER NOT NULL,
                        requiresConcentration INTEGER NOT NULL,
                        attackType TEXT NOT NULL,
                        availableClasses TEXT NOT NULL,
                        damageType TEXT NOT NULL,
                        damageBase TEXT NOT NULL,
                        damageBonusValue INTEGER NOT NULL,
                        damageBonusIsModifier INTEGER NOT NULL,
                        altDamageBase TEXT NOT NULL,
                        altDamageType TEXT NOT NULL,
                        altDamageBonusValue INTEGER NOT NULL,
                        altDamageBonusIsModifier INTEGER NOT NULL,
                        damage TEXT NOT NULL,
                        saveAbility TEXT NOT NULL,
                        saveEffect TEXT NOT NULL,
                        areaOfEffect TEXT NOT NULL,
                        healBase TEXT NOT NULL,
                        healBonusValue INTEGER NOT NULL,
                        healBonusIsModifier INTEGER NOT NULL,
                        healing TEXT NOT NULL,
                        FOREIGN KEY(characterOwnerId) REFERENCES characters(id) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_spell_attacks_characterOwnerId ON spell_attacks(characterOwnerId)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS attacks_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        characterOwnerId INTEGER NOT NULL,
                        name TEXT NOT NULL,
                        icon TEXT NOT NULL,
                        isProficient INTEGER NOT NULL,
                        calculationMode TEXT NOT NULL,
                        ability TEXT NOT NULL,
                        normalRange INTEGER,
                        longRange INTEGER,
                        damageDiceCount INTEGER NOT NULL,
                        damageDieType TEXT NOT NULL,
                        alternateDamageDiceCount INTEGER,
                        alternateDamageDieType TEXT,
                        alternateDamageType TEXT,
                        magicalBonus INTEGER NOT NULL,
                        applyAbilityModifierToDamage INTEGER NOT NULL,
                        range TEXT NOT NULL,
                        attackBonusOrSaveDc TEXT NOT NULL,
                        damage TEXT NOT NULL,
                        damageType TEXT NOT NULL,
                        FOREIGN KEY(characterOwnerId) REFERENCES characters(id) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO attacks_new (
                        id, characterOwnerId, name, icon, isProficient, calculationMode, ability,
                        normalRange, longRange, damageDiceCount, damageDieType,
                        alternateDamageDiceCount, alternateDamageDieType, alternateDamageType,
                        magicalBonus, applyAbilityModifierToDamage, range, attackBonusOrSaveDc, damage, damageType
                    )
                    SELECT
                        id, characterOwnerId, name, icon, isProficient, calculationMode, ability,
                        normalRange, longRange, damageDiceCount, damageDieType,
                        alternateDamageDiceCount, alternateDamageDieType, alternateDamageType,
                        magicalBonus, applyAbilityModifierToDamage, range, attackBonusOrSaveDc, damage, damageType
                    FROM attacks
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE attacks")
                db.execSQL("ALTER TABLE attacks_new RENAME TO attacks")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_attacks_characterOwnerId ON attacks(characterOwnerId)")
            }
        }

        private val MIGRATION_41_42 = object : Migration(41, 42) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE characters ADD COLUMN darkvisionMode TEXT NOT NULL DEFAULT 'AUTO'")
                db.execSQL("ALTER TABLE characters ADD COLUMN darkvisionManualFeet INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_42_43 = object : Migration(42, 43) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE features ADD COLUMN category TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_43_44 = object : Migration(43, 44) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE features ADD COLUMN catalogId TEXT")
            }
        }

        private val MIGRATION_44_45 = object : Migration(44, 45) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE inventory_items ADD COLUMN catalogId TEXT")
            }
        }

        private val MIGRATION_45_46 = object : Migration(45, 46) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Armor's magic bonus now adds to AC. Until now the editor always saved 1 for armor
                // without letting the user set it, so the stored value is meaningless: reset it.
                db.execSQL("UPDATE inventory_items SET magicalBonus = 0 WHERE category = 'ARMOR'")
            }
        }

        private val MIGRATION_46_47 = object : Migration(46, 47) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Level-up wizard: the character's classes and the choices made at each level.
                db.execSQL("ALTER TABLE characters ADD COLUMN classesJson TEXT NOT NULL DEFAULT '[]'")
                db.execSQL("ALTER TABLE characters ADD COLUMN advancementsJson TEXT NOT NULL DEFAULT '[]'")
                db.execSQL("ALTER TABLE combat_resources ADD COLUMN catalogId TEXT")
            }
        }

        private val MIGRATION_47_48 = object : Migration(47, 48) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Spells a feature prepares (domain, species, feat spells): they don't count toward the limit.
                db.execSQL("ALTER TABLE spells ADD COLUMN isAlwaysPrepared INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE spell_attacks ADD COLUMN isAlwaysPrepared INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_48_49 = object : Migration(48, 49) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // The character's size; containers (what they carry) and the container an item lies in.
                db.execSQL("ALTER TABLE characters ADD COLUMN size TEXT NOT NULL DEFAULT 'MEDIUM'")
                db.execSQL("ALTER TABLE inventory_items ADD COLUMN containerCapacity REAL")
                db.execSQL("ALTER TABLE inventory_items ADD COLUMN containerWeightlessContents INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE inventory_items ADD COLUMN containerId INTEGER")
            }
        }

        private val MIGRATION_49_50 = object : Migration(49, 50) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Weapon masteries of their own; the weapon an attack was made from.
                db.execSQL("ALTER TABLE characters ADD COLUMN weaponMasteries TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE attacks ADD COLUMN baseWeaponId TEXT")
            }
        }
    }
}
