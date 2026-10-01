package com.dndcharacterhandler.domain.levelup

import com.dndcharacterhandler.domain.model.AdvancementStep
import com.dndcharacterhandler.domain.model.CatalogBackground
import com.dndcharacterhandler.domain.model.CatalogClass
import com.dndcharacterhandler.domain.model.CatalogEquipmentRef
import com.dndcharacterhandler.domain.model.CatalogSpecies
import com.dndcharacterhandler.domain.model.CatalogFeature
import com.dndcharacterhandler.domain.model.CatalogSpellRef
import com.dndcharacterhandler.domain.model.CatalogSubclass
import com.dndcharacterhandler.domain.model.CatalogText
import com.dndcharacterhandler.domain.model.CharacterClassEntry
import com.dndcharacterhandler.domain.rules.MulticlassRequirement
import com.dndcharacterhandler.domain.rules.SpellSlotTable

enum class HitPointMethod { MAXIMUM, AVERAGE, ROLL }

/** How a new character's ability scores are set: the PHB's three ways, or the scores it has. */
enum class AbilityMethod { STANDARD_ARRAY, POINT_BUY, ROLL, KEEP }

/** The PHB standard array. */
val STANDARD_ARRAY: List<Int> = listOf(15, 14, 13, 12, 10, 8)

/** Point buy: 27 points, scores 8 to 15, and what each score costs. */
const val POINT_BUY_BUDGET = 27
val POINT_BUY_COSTS: Map<Int, Int> = mapOf(8 to 0, 9 to 1, 10 to 2, 11 to 3, 12 to 4, 13 to 5, 14 to 7, 15 to 9)

/** The first run for a character whose class so far is only text. */
data class LevelUpSetup(
    val classId: String,
    val subclassId: String? = null,
    /**
     * The character is already filled in up to its current level: only record the class. Otherwise
     * the wizard builds it from level 1 (a new character).
     */
    val keepExistingLevels: Boolean
)

/** What the player chose on one page. */
sealed interface LevelUpAnswer {
    data class HitPoints(val method: HitPointMethod, val value: Int) : LevelUpAnswer

    /** One set of picked trait keys per choice group of the step. */
    data class Traits(val picks: List<Set<String>>) : LevelUpAnswer

    /** Picked catalog features; [replacedId] is a known one given up in exchange, where allowed. */
    data class Items(val picks: List<String>, val replacedId: String? = null) : LevelUpAnswer

    /** Optional features the player chose not to take. */
    data class Grants(val declined: Set<String>) : LevelUpAnswer

    /** Points added per ability ("str" to 1...). */
    data class AbilityScores(val increases: Map<String, Int>) : LevelUpAnswer

    /** A feat taken instead of the ability score increase. */
    data class Feat(val featId: String) : LevelUpAnswer

    data class Subclass(val subclassId: String) : LevelUpAnswer

    /** A new character's ability scores before any bonus; [rolls] are the 4d6 totals when rolled. */
    data class BaseAbilities(val method: AbilityMethod, val scores: Map<String, Int>, val rolls: List<Int> = emptyList()) : LevelUpAnswer

    /** A species or background from the catalog, or null to keep the character's own (homebrew) one. */
    data class Origin(val id: String?) : LevelUpAnswer

    /** One of the starting equipment options, and the pick for each of its choices (by option id). */
    data class Equipment(val option: Int, val picks: List<String> = emptyList()) : LevelUpAnswer
}

/** Everything chosen so far; pages are rebuilt from it after every answer. */
data class LevelUpDraft(
    val targetLevel: Int,
    val setup: LevelUpSetup? = null,
    /** Character level -> the class that gains it. */
    val classPicks: Map<Int, String> = emptyMap(),
    /** Page key -> answer. */
    val answers: Map<String, LevelUpAnswer> = emptyMap()
)

/** A class the character already has, with its levels, as offered on the class page. */
data class ClassChoice(val characterClass: CatalogClass, val levels: Int, val subclass: CatalogSubclass?)

data class GrantedFeature(val feature: CatalogFeature, val optional: Boolean)

data class ScaleChange(val title: CatalogText, val from: String?, val to: String)

data class TraitOption(val key: String, val name: CatalogText, val alreadyHas: Boolean)

data class TraitGroup(val count: Int, val options: List<TraitOption>)

data class ItemOption(val feature: CatalogFeature, val known: Boolean)

/** One screen of the wizard. [key] identifies its answer in [LevelUpDraft.answers]. */
sealed interface LevelUpPage {
    val key: String
    /** The character level the page belongs to; 0 for the setup and the summary. */
    val characterLevel: Int

    data class Setup(
        val classes: List<CatalogClass>,
        val currentLevel: Int,
        /** The character already has features, so the wizard suggests keeping them. */
        val suggestKeep: Boolean
    ) : LevelUpPage {
        override val key = "setup"
        override val characterLevel = 0
    }

    data class ChooseClass(
        override val characterLevel: Int,
        val current: List<ClassChoice>,
        val others: List<CatalogClass>,
        val selectedClassId: String,
        /** Requirements for multiclassing into the selected class; empty when it is one of [current]. */
        val requirements: List<MulticlassRequirement>
    ) : LevelUpPage {
        override val key = "L$characterLevel:class"
    }

    data class HitPoints(
        override val key: String,
        override val characterLevel: Int,
        val characterClass: CatalogClass,
        val classLevel: Int,
        /** The very first level of a character: hit points are the die's maximum. */
        val takesMaximum: Boolean,
        val constitutionModifier: Int
    ) : LevelUpPage {
        val average: Int get() = characterClass.hitDie / 2 + 1
    }

    data class Features(
        override val key: String,
        override val characterLevel: Int,
        val characterClass: CatalogClass,
        val classLevel: Int,
        val grants: List<GrantedFeature>,
        val scaleChanges: List<ScaleChange>,
        val spells: List<CatalogSpellRef>
    ) : LevelUpPage

    data class Traits(
        override val key: String,
        override val characterLevel: Int,
        val source: CatalogText,
        val step: AdvancementStep.Trait,
        /** Traits the step grants outright. */
        val granted: List<TraitOption>,
        val groups: List<TraitGroup>
    ) : LevelUpPage {
        /** How many to pick in each group: fewer when the group hasn't enough options left. */
        val required: List<Int> get() = groups.map { group ->
            minOf(group.count, group.options.count { !it.alreadyHas || step.allowReplacements })
        }
    }

    data class Items(
        override val key: String,
        override val characterLevel: Int,
        val source: CatalogText,
        val step: AdvancementStep.ItemChoice,
        val count: Int,
        val options: List<ItemOption>,
        /** Features picked earlier that may be swapped for another now. */
        val replaceable: List<CatalogFeature>
    ) : LevelUpPage {
        val required: Int get() = minOf(count, options.count { !it.known || it.feature.repeatable })
    }

    data class AbilityScores(
        override val key: String,
        override val characterLevel: Int,
        val source: CatalogText,
        val step: AdvancementStep.AbilityScoreImprovement,
        /** Scores before this step. */
        val scores: Map<String, Int>,
        val allowFeat: Boolean,
        val feats: List<CatalogFeature>
    ) : LevelUpPage {
        val maxScore: Int get() = 20
        val cap: Int get() = step.cap ?: step.points
        val abilities: List<String> get() = ABILITIES.filter { it !in step.locked }

        /** Points to spend: fewer when the scores are already near the maximum. */
        val required: Int get() = minOf(
            step.points,
            abilities.sumOf { ability -> minOf(cap, (maxScore - (scores[ability] ?: 10)).coerceAtLeast(0)) }
        )

        companion object {
            val ABILITIES = listOf("str", "dex", "con", "int", "wis", "cha")
        }
    }

    data class Subclass(
        override val key: String,
        override val characterLevel: Int,
        val characterClass: CatalogClass,
        val options: List<CatalogSubclass>
    ) : LevelUpPage

    /** A new character's ability scores: standard array, point buy, 4d6 rolls, or keep the current ones. */
    data class BaseAbilities(val currentScores: Map<String, Int>) : LevelUpPage {
        override val key = "abilities"
        override val characterLevel = 1
    }

    data class Species(val options: List<CatalogSpecies>, val currentName: String) : LevelUpPage {
        override val key = "species"
        override val characterLevel = 1
    }

    data class Background(val options: List<CatalogBackground>, val currentName: String) : LevelUpPage {
        override val key = "background"
        override val characterLevel = 1
    }

    /** Starting equipment of the first class or of the background. */
    data class Equipment(
        override val key: String,
        val source: CatalogText,
        val options: List<EquipmentOption>
    ) : LevelUpPage {
        override val characterLevel = 1
    }

    data class Summary(val summary: LevelUpSummary) : LevelUpPage {
        override val key = "summary"
        override val characterLevel = 0
    }
}

/** One thing a starting equipment choice offers: an item, or a tool of a kind ("tool:art:smith"). */
data class EquipmentPick(val id: String, val item: CatalogEquipmentRef, val count: Int)

/** "One of these" inside an equipment option (an artisan's tool or a musical instrument...). */
data class EquipmentChoice(val options: List<EquipmentPick>, val suggested: String?)

/** One starting equipment option: fixed items, coins (by currency) and choices; or just gold. */
data class EquipmentOption(
    val items: List<EquipmentPick>,
    val coins: Map<String, Int>,
    val choices: List<EquipmentChoice>,
    /** The "take the gold instead" option. */
    val isWealth: Boolean = false
)

/** A spell pick the wizard leaves to the Spells screen for now. */
data class SpellChoiceNote(val source: CatalogText, val title: CatalogText, val count: Int)

/** What applying the draft will change. */
data class LevelUpSummary(
    val fromLevel: Int,
    val toLevel: Int,
    val classes: List<CharacterClassEntry>,
    /** Maximum hit points gained, Constitution included. */
    val hitPointGain: Int,
    val addedFeatures: List<CatalogFeature>,
    val removedFeatures: List<CatalogFeature>,
    val abilityIncreases: Map<String, Int>,
    val proficiencies: List<TraitOption>,
    val expertise: List<TraitOption>,
    val masteries: List<TraitOption>,
    /** Damage resistances and immunities the features give; the app has no field for them. */
    val defenses: List<TraitOption>,
    val spellsGranted: List<CatalogSpellRef>,
    val spellChoices: List<SpellChoiceNote>,
    val spellSlots: SpellSlotTable,
    val unmetRequirements: List<MulticlassRequirement>,
    val species: CatalogSpecies? = null,
    val background: CatalogBackground? = null,
    /** Ability scores the new character starts with, before bonuses; null when they stay. */
    val baseScores: Map<String, Int>? = null,
    val equipment: List<EquipmentPick> = emptyList(),
    val coins: Map<String, Int> = emptyMap()
)

/** The pages for a draft, and whether every one of them is answered. */
data class LevelUpRun(val pages: List<LevelUpPage>, val isComplete: Boolean)
