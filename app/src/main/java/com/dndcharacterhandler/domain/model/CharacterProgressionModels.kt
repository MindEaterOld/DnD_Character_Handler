package com.dndcharacterhandler.domain.model

/**
 * One class of a character; a multiclass character has several. [classId] and [subclassId] are
 * [CharacterCatalog] ids.
 */
data class CharacterClassEntry(
    val classId: String,
    val subclassId: String? = null,
    val levels: Int,
    /**
     * The class taken at character level 1: its first-level proficiencies and maximum hit points
     * apply, while classes added later get the reduced multiclass set.
     */
    val isOriginal: Boolean = false,
    /** Hit dice of this class spent since the last long rest. */
    val spentHitDice: Int = 0
)

/**
 * One choice made while levelling up — hit points rolled, skills or invocations picked, an ability
 * increase or feat, a subclass — kept the way Foundry's advancement keeps it, so the level can be
 * reviewed or taken back later.
 */
data class AdvancementRecord(
    /** Character level the step was applied at. */
    val characterLevel: Int,
    /** The class that gained the level, and its level in that class afterwards. */
    val classId: String,
    val classLevel: Int,
    /** Catalog entry the step belongs to: the class, its subclass, or a feat or feature with steps of its own. */
    val sourceId: String,
    val stepId: String,
    /** The step type, as in [AdvancementStep] ("HitPoints", "Trait", "ItemChoice"...). */
    val type: String,
    /** What was chosen, as JSON (see LevelUpEngine). */
    val value: String
)
