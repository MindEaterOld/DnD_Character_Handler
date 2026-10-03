package com.dndcharacterhandler.domain.rules

import com.dndcharacterhandler.domain.model.Condition
import com.dndcharacterhandler.domain.model.SpellcastingAbility

/** Exhaustion's last level: the character dies. */
const val MAX_EXHAUSTION = 6

/** What a d20 is rolled for, as the conditions see it (the rules' "D20 Tests"). */
sealed interface D20Test {
    data object Attack : D20Test
    data class AbilityCheck(val ability: SpellcastingAbility) : D20Test
    data class SavingThrow(val ability: SpellcastingAbility) : D20Test

    /** A Dexterity check, and more: being incapacitated or invisible when it's rolled counts too. */
    data object Initiative : D20Test
    data object DeathSave : D20Test
}

enum class RollMode { NORMAL, ADVANTAGE, DISADVANTAGE }

/**
 * What the conditions and exhaustion do to one roll: [modifier] added to it (exhaustion's −2 a
 * level), the conditions that give advantage or disadvantage, and those that make it fail outright.
 */
data class RollEffects(
    val modifier: Int = 0,
    val advantage: List<Condition> = emptyList(),
    val disadvantage: List<Condition> = emptyList(),
    val autoFail: List<Condition> = emptyList()
) {
    /** Advantage and disadvantage together cancel out: one d20. */
    val mode: RollMode
        get() = when {
            advantage.isNotEmpty() && disadvantage.isEmpty() -> RollMode.ADVANTAGE
            disadvantage.isNotEmpty() && advantage.isEmpty() -> RollMode.DISADVANTAGE
            else -> RollMode.NORMAL
        }

    /** Anything at all for the sheet to mark. */
    val changesRoll: Boolean get() = modifier != 0 || advantage.isNotEmpty() || disadvantage.isNotEmpty() || autoFail.isNotEmpty()
}

/** The conditions that come with others: whoever is paralyzed is incapacitated too; unconscious, prone as well. */
fun effectiveConditions(conditions: Set<Condition>): Set<Condition> = buildSet {
    addAll(conditions)
    if (conditions.any { it in Incapacitating }) add(Condition.INCAPACITATED)
    if (Condition.UNCONSCIOUS in conditions) add(Condition.PRONE)
}

private val Incapacitating = setOf(Condition.PARALYZED, Condition.PETRIFIED, Condition.STUNNED, Condition.UNCONSCIOUS)

/** Speed 0 whatever else: held, bound, frozen, turned to stone, out cold. */
private val Immobilizing = setOf(Condition.GRAPPLED, Condition.RESTRAINED, Condition.PARALYZED, Condition.PETRIFIED, Condition.UNCONSCIOUS)

/** Strength and Dexterity saving throws fail outright. */
private val FailingStrengthAndDexteritySaves = listOf(Condition.PARALYZED, Condition.PETRIFIED, Condition.STUNNED, Condition.UNCONSCIOUS)

private val DisadvantageOnAttacks = listOf(Condition.BLINDED, Condition.FRIGHTENED, Condition.POISONED, Condition.PRONE, Condition.RESTRAINED)
private val DisadvantageOnChecks = listOf(Condition.FRIGHTENED, Condition.POISONED)

/**
 * What [conditions] and [exhaustion] do to [test], by SRD 5.2. Only what always applies: effects that
 * hang on the situation (the grappler as the target, a check that needs sight, the source of fear in
 * view) are the table's call. Frightened counts as if its source were in view.
 */
fun rollEffects(test: D20Test, conditions: Set<Condition>, exhaustion: Int): RollEffects {
    val active = effectiveConditions(conditions)
    fun List<Condition>.present() = filter { it in active }
    val modifier = -2 * exhaustion.coerceIn(0, MAX_EXHAUSTION)
    return when (test) {
        D20Test.Attack -> RollEffects(
            modifier = modifier,
            advantage = listOf(Condition.INVISIBLE).present(),
            disadvantage = DisadvantageOnAttacks.present()
        )
        is D20Test.AbilityCheck -> RollEffects(modifier = modifier, disadvantage = DisadvantageOnChecks.present())
        is D20Test.SavingThrow -> RollEffects(
            modifier = modifier,
            disadvantage = if (test.ability == SpellcastingAbility.DEXTERITY) listOf(Condition.RESTRAINED).present() else emptyList(),
            autoFail = if (test.ability == SpellcastingAbility.STRENGTH || test.ability == SpellcastingAbility.DEXTERITY) {
                FailingStrengthAndDexteritySaves.present()
            } else {
                emptyList()
            }
        )
        D20Test.Initiative -> RollEffects(
            modifier = modifier,
            advantage = listOf(Condition.INVISIBLE).present(),
            disadvantage = (DisadvantageOnChecks + Condition.INCAPACITATED).present()
        )
        D20Test.DeathSave -> RollEffects(modifier = modifier)
    }
}

/**
 * How attacks against the character are rolled: with advantage while it can't see them coming or
 * can't dodge (blinded, paralyzed, petrified, restrained, stunned, unconscious), with disadvantage
 * while it's unseen. Prone hangs on the attacker's distance and isn't counted.
 */
fun attacksAgainst(conditions: Set<Condition>): RollMode {
    val active = effectiveConditions(conditions)
    val advantage = active.any { it in ExposedToAttacks }
    val disadvantage = Condition.INVISIBLE in active
    return when {
        advantage && !disadvantage -> RollMode.ADVANTAGE
        disadvantage && !advantage -> RollMode.DISADVANTAGE
        else -> RollMode.NORMAL
    }
}

private val ExposedToAttacks = setOf(
    Condition.BLINDED, Condition.PARALYZED, Condition.PETRIFIED, Condition.RESTRAINED, Condition.STUNNED, Condition.UNCONSCIOUS
)

/** The damage that gets through: petrified, the character resists all of it (half, rounded down). */
fun damageTaken(damage: Int, conditions: Set<Condition>): Int =
    if (Condition.PETRIFIED in effectiveConditions(conditions)) damage.coerceAtLeast(0) / 2 else damage.coerceAtLeast(0)

/** The speed left: 0 while held or bound, otherwise 5 feet less for each level of exhaustion. */
fun effectiveSpeed(speed: Int, conditions: Set<Condition>, exhaustion: Int): Int =
    if (effectiveConditions(conditions).any { it in Immobilizing }) 0
    else (speed - 5 * exhaustion.coerceIn(0, MAX_EXHAUSTION)).coerceAtLeast(0)

/** The conditions in play: those put on by hand, and unconscious at 0 hit points. */
fun activeConditions(conditions: Set<Condition>, currentHp: Int): Set<Condition> =
    if (currentHp <= 0) conditions + Condition.UNCONSCIOUS else conditions

/** Dead: three failed death saves, or exhaustion's last level. */
fun isDead(saves: DeathSaves, exhaustion: Int): Boolean = saves.isDead || exhaustion >= MAX_EXHAUSTION

/** Whatever incapacitates ends concentration. */
fun breaksConcentration(conditions: Set<Condition>): Boolean = Condition.INCAPACITATED in effectiveConditions(conditions)

/** The Constitution save to keep concentrating after [damage]: half of it, at least 10, at most 30. */
fun concentrationSaveDc(damage: Int): Int = (damage / 2).coerceIn(10, 30)
