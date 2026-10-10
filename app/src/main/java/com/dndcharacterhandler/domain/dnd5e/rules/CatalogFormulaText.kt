package com.dndcharacterhandler.domain.dnd5e.rules

import com.dndcharacterhandler.domain.dnd5e.model.AdvancementStep
import com.dndcharacterhandler.domain.dnd5e.model.Character
import com.dndcharacterhandler.domain.dnd5e.model.CharacterCatalog
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToLong

/** What a character brings to catalog formulas. */
data class FormulaContext(
    val totalLevel: Int,
    /** Class identifier ("barbarian") -> levels in that class. */
    val classLevels: Map<String, Int>,
    /** Subclass identifiers the character has ("battle-master"). */
    val subclasses: Set<String>,
    /** Ability ("str") -> modifier. */
    val abilityModifiers: Map<String, Int>,
    val proficiencyBonus: Int
) {
    companion object {
        /**
         * The context of a character whose class and subclass are free text: they are matched to the
         * catalog by name, and the whole character level counts for that class.
         */
        fun of(character: Character, catalog: CharacterCatalog): FormulaContext {
            val characterClass = catalog.classes.firstOrNull { it.name.matches(character.characterClass) || it.identifier.equals(character.characterClass.trim(), true) }
            val subclass = catalog.subclasses.firstOrNull {
                (characterClass == null || it.classIdentifier == characterClass.identifier) && it.name.matches(character.subclass)
            }
            return FormulaContext(
                totalLevel = character.level,
                classLevels = characterClass?.let { mapOf(it.identifier to character.level) }.orEmpty(),
                subclasses = setOfNotNull(subclass?.identifier),
                abilityModifiers = mapOf(
                    "str" to abilityModifier(character.strength),
                    "dex" to abilityModifier(character.dexterity),
                    "con" to abilityModifier(character.constitution),
                    "int" to abilityModifier(character.intelligence),
                    "wis" to abilityModifier(character.wisdom),
                    "cha" to abilityModifier(character.charisma)
                ),
                proficiencyBonus = proficiencyBonusForLevel(character.level)
            )
        }

        private fun com.dndcharacterhandler.domain.dnd5e.model.CatalogText.matches(value: String): Boolean {
            val needle = value.trim()
            return needle.isNotEmpty() && (en.equals(needle, ignoreCase = true) || ru.equals(needle, ignoreCase = true))
        }
    }
}

/**
 * Fills the {=FORMULA} tokens the catalog keeps in its texts (Foundry formulas such as
 * "@scale.barbarian.rages" or "1d8 + @abilities.wis.mod"). With a [FormulaContext] they become the
 * character's numbers, as Foundry shows them on a sheet; without one, or for parts the character
 * doesn't have (a scale of another class), a readable label is shown instead.
 */
class CatalogFormulaText(private val catalog: CharacterCatalog) {
    fun render(text: String, russian: Boolean, context: FormulaContext?): String {
        if (!text.contains("{=")) return text
        return TOKEN.replace(text) { match -> formula(match.groupValues[1], russian, context) }
    }

    /** The character's value of a Foundry [formula] ("@scale.barbarian.rages", "@prof"), or null if it needs something unknown. */
    fun value(formula: String, context: FormulaContext): String? {
        var resolvedAll = true
        val substituted = REFERENCE.replace(formula) { match ->
            resolve(match.value, context) ?: "0".also { resolvedAll = false }
        }
        if (!resolvedAll) return null
        if (DICE.containsMatchIn(substituted)) return tidySigns(substituted)
        return evaluate(substituted)?.let(::formatNumber)
    }

    private fun formula(formula: String, russian: Boolean, context: FormulaContext?): String {
        var resolvedAll = true
        val substituted = REFERENCE.replace(formula) { match ->
            val value = context?.let { resolve(match.value, it) }
            if (value == null) {
                resolvedAll = false
                label(match.value, russian)
            } else {
                value
            }
        }
        if (!resolvedAll) return cleanLabels(substituted)
        if (DICE.containsMatchIn(substituted)) return tidySigns(substituted)
        return evaluate(substituted)?.let(::formatNumber) ?: tidySigns(substituted)
    }

    /** The character's value for one @reference, or null when it can't be known. */
    private fun resolve(reference: String, context: FormulaContext): String? {
        val parts = reference.removePrefix("@").split('.')
        return when (parts.firstOrNull()) {
            "prof" -> context.proficiencyBonus.toString()
            "attributes" -> if (parts.getOrNull(1) == "prof") context.proficiencyBonus.toString() else null
            "abilities" -> if (parts.getOrNull(2) == "mod") context.abilityModifiers[parts.getOrNull(1)]?.toString() else null
            "details" -> if (parts.getOrNull(1) == "level") context.totalLevel.toString() else null
            "classes" -> if (parts.getOrNull(2) == "levels") context.classLevels[parts.getOrNull(1)]?.toString() else null
            "scale" -> scale(parts.getOrNull(1) ?: return null, parts.getOrNull(2) ?: return null, parts.getOrNull(3), context)
            else -> null
        }
    }

    private fun scale(owner: String, identifier: String, property: String?, context: FormulaContext): String? {
        val (step, classIdentifier) = scaleStep(owner, identifier) ?: return null
        if (catalog.subclassesByIdentifier.containsKey(owner) && owner !in context.subclasses) return null
        val level = context.classLevels[classIdentifier] ?: return null
        val value = step.valueAt(level) ?: return null
        return scaleProperty(value, property)
    }

    private fun scaleStep(owner: String, identifier: String): Pair<AdvancementStep.ScaleValue, String>? {
        catalog.classesByIdentifier[owner]?.let { characterClass ->
            characterClass.advancement.scale(identifier)?.let { return it to characterClass.identifier }
        }
        catalog.subclassesByIdentifier[owner]?.let { subclass ->
            subclass.advancement.scale(identifier)?.let { return it to subclass.classIdentifier }
        }
        return null
    }

    private fun List<AdvancementStep>.scale(identifier: String): AdvancementStep.ScaleValue? =
        filterIsInstance<AdvancementStep.ScaleValue>().firstOrNull { it.identifier == identifier }

    /** Foundry scale properties: "4d8".number = 4, .faces = 8, .die = "d8"; anything else is the value. */
    private fun scaleProperty(value: String, property: String?): String {
        val dice = DICE_VALUE.matchEntire(value) ?: return value
        val number = dice.groupValues[1].ifBlank { "1" }
        val faces = dice.groupValues[2]
        return when (property) {
            "number" -> number
            "faces" -> faces
            "die", "denom" -> "d$faces"
            else -> "${dice.groupValues[1]}d$faces"
        }
    }

    private fun label(reference: String, russian: Boolean): String {
        val parts = reference.removePrefix("@").split('.')
        return when (parts.firstOrNull()) {
            "prof" -> if (russian) "бонус мастерства" else "Proficiency Bonus"
            "attributes" -> if (russian) "бонус мастерства" else "Proficiency Bonus"
            "abilities" -> abilityLabel(parts.getOrNull(1), russian)
            "details" -> if (russian) "уровень персонажа" else "character level"
            "classes" -> {
                val characterClass = catalog.classesByIdentifier[parts.getOrNull(1)]
                val name = characterClass?.name?.get(russian) ?: parts.getOrNull(1).orEmpty()
                if (russian) "уровень ${genitive(name)}" else "$name level"
            }
            "scale" -> scaleLabel(parts.getOrNull(1), parts.getOrNull(2), parts.getOrNull(3), russian)
            else -> reference
        }
    }

    /** A scale without a character: its values over the levels, e.g. "2/3/4/5/6". */
    private fun scaleLabel(owner: String?, identifier: String?, property: String?, russian: Boolean): String {
        val (step, _) = scaleStep(owner ?: return "", identifier ?: return "") ?: return identifier.orEmpty()
        val values = step.values.toSortedMap().values.map { scaleProperty(it, property) }.distinct()
        return values.joinToString("/").ifBlank { step.title.get(russian) }
    }

    private fun abilityLabel(ability: String?, russian: Boolean): String {
        val names = ABILITIES[ability] ?: return ability.orEmpty()
        return if (russian) "модификатор ${names.second}" else "${names.first} modifier"
    }

    /** "уровень Друида", "уровень Чародея": Russian class names take the genitive. */
    private fun genitive(name: String): String = when {
        name.endsWith("й") -> name.dropLast(1) + "я"
        name.lastOrNull()?.let { it in "бвгджзклмнпрстфхцчшщ" } == true -> name + "а"
        else -> name
    }

    private fun cleanLabels(text: String): String =
        tidySigns(text.replace(Regex("""\b(floor|ceil|round)\("""), "("))

    private fun tidySigns(text: String): String =
        text.replace(Regex("""\+\s*-\s*"""), "− ").replace(Regex("""\s+"""), " ").trim()

    private fun formatNumber(value: Double): String =
        if (value % 1.0 == 0.0) value.roundToLong().toString() else "%.1f".format(value)

    /** Arithmetic with + - * /, parentheses and floor/ceil/round/min/max; null when it isn't one. */
    private fun evaluate(expression: String): Double? = runCatching { Evaluator(expression).parse() }.getOrNull()

    private class Evaluator(private val input: String) {
        private var position = 0

        fun parse(): Double {
            val value = sum()
            skipSpaces()
            require(position == input.length)
            return value
        }

        private fun sum(): Double {
            var value = product()
            while (true) {
                skipSpaces()
                value = when (peek()) {
                    '+' -> { position++; value + product() }
                    '-' -> { position++; value - product() }
                    else -> return value
                }
            }
        }

        private fun product(): Double {
            var value = unary()
            while (true) {
                skipSpaces()
                value = when (peek()) {
                    '*' -> { position++; value * unary() }
                    '/' -> { position++; value / unary() }
                    else -> return value
                }
            }
        }

        private fun unary(): Double {
            skipSpaces()
            if (peek() == '-') { position++; return -unary() }
            if (peek() == '+') { position++; return unary() }
            return atom()
        }

        private fun atom(): Double {
            skipSpaces()
            val c = peek() ?: error("end")
            if (c == '(') {
                position++
                val value = sum()
                expect(')')
                return value
            }
            if (c.isDigit() || c == '.') {
                val start = position
                while (peek()?.let { it.isDigit() || it == '.' } == true) position++
                return input.substring(start, position).toDouble()
            }
            if (c.isLetter()) {
                val start = position
                while (peek()?.isLetter() == true) position++
                val name = input.substring(start, position)
                expect('(')
                val args = mutableListOf(sum())
                while (true) {
                    skipSpaces()
                    if (peek() == ',') { position++; args += sum() } else break
                }
                expect(')')
                return when (name) {
                    "floor" -> floor(args[0])
                    "ceil" -> ceil(args[0])
                    "round" -> Math.round(args[0]).toDouble()
                    "max" -> args.max()
                    "min" -> args.min()
                    "abs" -> kotlin.math.abs(args[0])
                    else -> error("function $name")
                }
            }
            error("unexpected $c")
        }

        private fun expect(c: Char) {
            skipSpaces()
            require(peek() == c)
            position++
        }

        private fun peek(): Char? = input.getOrNull(position)

        private fun skipSpaces() {
            while (peek()?.isWhitespace() == true) position++
        }
    }

    private companion object {
        val TOKEN = Regex("""\{=([^{}]*)\}""")
        /** @scale.battle-master.superiority.die, @abilities.wis.mod, @prof ... (hyphens belong to ids). */
        val REFERENCE = Regex("""@[A-Za-z][A-Za-z0-9_]*(?:\.[A-Za-z0-9_]+(?:-[A-Za-z0-9_]+)*)*""")
        val DICE = Regex("""\d*d\d+""")
        val DICE_VALUE = Regex("""(\d*)d(\d+)""")
        val ABILITIES = mapOf(
            "str" to ("Strength" to "Силы"), "dex" to ("Dexterity" to "Ловкости"),
            "con" to ("Constitution" to "Телосложения"), "int" to ("Intelligence" to "Интеллекта"),
            "wis" to ("Wisdom" to "Мудрости"), "cha" to ("Charisma" to "Харизмы")
        )
    }
}
