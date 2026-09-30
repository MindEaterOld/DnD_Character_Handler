package com.dndcharacterhandler.domain.model

/**
 * The character-building catalog imported from Foundry (see tools/foundry_catalog): classes and
 * subclasses with their full level progression, species, backgrounds, and every feature, option and
 * feat they grant. Ids are the Foundry document ids, stable across re-imports.
 */
data class CharacterCatalog(
    val classes: List<CatalogClass>,
    val subclasses: List<CatalogSubclass>,
    val species: List<CatalogSpecies>,
    val backgrounds: List<CatalogBackground>,
    val features: List<CatalogFeature>,
    val spells: Map<String, CatalogSpellRef>,
    val books: Map<String, CatalogText>,
    val subtypes: Map<String, CatalogText>,
    /** Ids of the catalog the app used before, mapped to the entries that replace them. */
    val legacyIds: Map<String, String>
) {
    val featuresById: Map<String, CatalogFeature> by lazy { features.associateBy { it.id } }
    val classesByIdentifier: Map<String, CatalogClass> by lazy { classes.associateBy { it.identifier } }
    val subclassesByIdentifier: Map<String, CatalogSubclass> by lazy { subclasses.associateBy { it.identifier } }

    /** Display name of whatever granted a feature: a class, subclass, species, background or feat. */
    fun ownerName(id: String): CatalogText? = owners[id]

    private val owners: Map<String, CatalogText> by lazy {
        buildMap {
            classes.forEach { put(it.id, it.name) }
            subclasses.forEach { put(it.id, it.name) }
            species.forEach { put(it.id, it.name) }
            backgrounds.forEach { put(it.id, it.name) }
            features.forEach { put(it.id, it.name) }
        }
    }

    companion object {
        val EMPTY = CharacterCatalog(
            classes = emptyList(), subclasses = emptyList(), species = emptyList(), backgrounds = emptyList(),
            features = emptyList(), spells = emptyMap(), books = emptyMap(), subtypes = emptyMap(), legacyIds = emptyMap()
        )
    }
}

/** A catalog text in English and Russian; either may be missing, and then the other one is shown. */
data class CatalogText(val en: String = "", val ru: String = "") {
    fun get(russian: Boolean): String = if (russian) ru.ifBlank { en } else en.ifBlank { ru }
    val isBlank: Boolean get() = en.isBlank() && ru.isBlank()
}

data class CatalogSpellcasting(
    /** "full", "half", "third", "pact" or "artificer", as Foundry counts caster levels. */
    val progression: String,
    val ability: String
)

data class CatalogClass(
    val id: String,
    val identifier: String,
    val name: CatalogText,
    val book: String,
    val hitDie: Int,
    /** Abilities that need 13+ to multiclass into or out of this class. */
    val primaryAbilities: List<String>,
    /** True when all [primaryAbilities] need 13+, false when any one of them is enough. */
    val primaryAbilitiesAll: Boolean,
    val spellcasting: CatalogSpellcasting?,
    val advancement: List<AdvancementStep>
)

data class CatalogSubclass(
    val id: String,
    val identifier: String,
    val classIdentifier: String,
    val name: CatalogText,
    val text: CatalogText,
    val book: String,
    val spellcasting: CatalogSpellcasting?,
    val advancement: List<AdvancementStep>
)

data class CatalogSpecies(
    val id: String,
    val identifier: String,
    val name: CatalogText,
    val text: CatalogText,
    val book: String,
    val movement: Map<String, Double>,
    val senses: Map<String, Double>,
    val advancement: List<AdvancementStep>
)

data class CatalogBackground(
    val id: String,
    val identifier: String,
    val name: CatalogText,
    val text: CatalogText,
    val book: String,
    val advancement: List<AdvancementStep>
)

enum class CatalogFeatureKind(val key: String) {
    CLASS_FEATURE("classFeature"),
    SUBCLASS_FEATURE("subclassFeature"),
    /** Picked from a list: invocations, maneuvers, metamagic, fighting styles... */
    CLASS_OPTION("classOption"),
    FEAT("feat"),
    SPECIES_TRAIT("speciesTrait"),
    BACKGROUND_FEATURE("backgroundFeature"),
    /** A choice inside another feature (e.g. an Elemental Adept element). */
    FEATURE_OPTION("featureOption"),
    /** A weapon or other item a feature grants (Psychic Blades, natural weapons...). */
    ITEM("item"),
    OTHER("other");

    companion object {
        fun of(key: String): CatalogFeatureKind = entries.firstOrNull { it.key == key } ?: OTHER
    }
}

/** Something that grants a feature: [ownerId] at [level], directly or as one of its choices. */
data class CatalogGrant(val ownerId: String, val level: Int?, val viaChoice: Boolean)

data class CatalogRecovery(
    /** "lr" long rest, "sr" short rest, "day", "dawn", "turn", "round"... */
    val period: String,
    /** "recoverAll", "loseAll" or "formula". */
    val type: String,
    val formula: String?
)

data class CatalogUses(
    /** Foundry formula for the maximum, e.g. "@scale.barbarian.rages" or "@prof". */
    val max: String,
    val recovery: List<CatalogRecovery>
)

data class CatalogFeature(
    val id: String,
    val identifier: String,
    val name: CatalogText,
    val text: CatalogText,
    val book: String,
    val kind: CatalogFeatureKind,
    /** Foundry item type: "feat" for features and feats, "weapon" etc. for granted items. */
    val type: String,
    /** Foundry feature type: "class", "feat", "race"... */
    val featureType: String,
    /** Foundry feature subtype: "eldritchInvocation", "maneuver", "general", "origin"... */
    val subtype: String,
    val level: Int?,
    /** Requirement text as the book prints it, e.g. "Жрец 3 (Домен войны)". */
    val requirements: String,
    val prerequisiteLevel: Int?,
    val repeatable: Boolean,
    val uses: CatalogUses?,
    /** How it is used: "action", "bonus", "reaction", "minute"... */
    val activation: String?,
    val grantedBy: List<CatalogGrant>,
    val advancement: List<AdvancementStep>
)

data class CatalogSpellRef(val id: String, val name: CatalogText, val level: Int?)

enum class ClassRestriction {
    /** Only for the character's first class. */
    PRIMARY,
    /** Only when the class is taken as a multiclass. */
    SECONDARY
}

data class TraitChoice(val count: Int, val pool: List<String>)

data class ChoiceRestriction(val type: String, val subtype: String, val list: List<String>, val level: String)

/** One step of a class, subclass, species, background or feat progression (Foundry "advancement"). */
sealed interface AdvancementStep {
    val id: String
    /** The level the step happens at; null for steps spread over levels (scale values, choices). */
    val level: Int?
    val title: CatalogText
    val classRestriction: ClassRestriction?

    data class HitPoints(
        override val id: String,
        override val level: Int?,
        override val title: CatalogText,
        override val classRestriction: ClassRestriction?
    ) : AdvancementStep

    /** Proficiencies and similar traits: [grants] outright plus [choices] ("skills:ath", "saves:str"...). */
    data class Trait(
        override val id: String,
        override val level: Int?,
        override val title: CatalogText,
        override val classRestriction: ClassRestriction?,
        val mode: String,
        val grants: List<String>,
        val choices: List<TraitChoice>,
        val allowReplacements: Boolean,
        val hint: CatalogText
    ) : AdvancementStep

    data class ItemGrant(
        override val id: String,
        override val level: Int?,
        override val title: CatalogText,
        override val classRestriction: ClassRestriction?,
        val items: List<String>,
        val spells: List<String>,
        val optional: Boolean
    ) : AdvancementStep

    /**
     * Picks from a list: [counts] maps a level to how many are picked there. With an empty [items]
     * pool the options are every catalog feature matching [restriction] (e.g. all maneuvers).
     */
    data class ItemChoice(
        override val id: String,
        override val level: Int?,
        override val title: CatalogText,
        override val classRestriction: ClassRestriction?,
        val counts: Map<Int, Int>,
        val replacementLevels: Set<Int>,
        val items: List<String>,
        val spells: List<String>,
        val itemType: String,
        val restriction: ChoiceRestriction?
    ) : AdvancementStep

    /** A value that grows with level; [values] maps the level it changes at to the value ("2", "1d10"). */
    data class ScaleValue(
        override val id: String,
        override val level: Int?,
        override val title: CatalogText,
        override val classRestriction: ClassRestriction?,
        val identifier: String,
        val scaleType: String,
        val values: Map<Int, String>,
        val units: String
    ) : AdvancementStep {
        /** The value at [level]: the one from the highest listed level not above it. */
        fun valueAt(level: Int): String? =
            values.filterKeys { it <= level }.maxByOrNull { it.key }?.value
    }

    data class AbilityScoreImprovement(
        override val id: String,
        override val level: Int?,
        override val title: CatalogText,
        override val classRestriction: ClassRestriction?,
        val points: Int,
        val cap: Int?,
        val fixed: Map<String, Int>,
        val locked: Set<String>,
        /** A feat suggested in place of the improvement (the Epic Boon at level 19). */
        val recommendation: String?
    ) : AdvancementStep

    data class Subclass(
        override val id: String,
        override val level: Int?,
        override val title: CatalogText,
        override val classRestriction: ClassRestriction?
    ) : AdvancementStep

    data class Size(
        override val id: String,
        override val level: Int?,
        override val title: CatalogText,
        override val classRestriction: ClassRestriction?,
        val sizes: List<String>
    ) : AdvancementStep

    /** A step type the app doesn't know yet; kept so nothing silently disappears. */
    data class Other(
        override val id: String,
        override val level: Int?,
        override val title: CatalogText,
        override val classRestriction: ClassRestriction?,
        val type: String
    ) : AdvancementStep
}
