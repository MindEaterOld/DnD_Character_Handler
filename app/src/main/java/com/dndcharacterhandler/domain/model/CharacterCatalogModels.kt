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
    val legacyIds: Map<String, String>,
    /** Proficiency and trait keys ("skills:ath", "tool:art:smith"...) with their labels and children. */
    val traits: Map<String, CatalogTrait> = emptyMap(),
    /** Labels of the trait kinds: "skills", "tool", "languages"... */
    val traitCategories: Map<String, CatalogText> = emptyMap(),
    /** Equipment the advancements, starting equipment and containers point at, by id. */
    val equipment: Map<String, CatalogEquipmentRef> = emptyMap(),
    /** Bags, cases and equipment packs, by the id of their [equipment] entry. */
    val containers: Map<String, CatalogContainer> = emptyMap()
) {
    val featuresById: Map<String, CatalogFeature> by lazy { features.associateBy { it.id } }

    /** Containers sold by themselves (not a pack's waterskin), by [equipmentNameKey] of their English name. */
    val containersByName: Map<String, CatalogContainer> by lazy {
        containers.values.filter { it.inside == null }.mapNotNull { container ->
            equipment[container.id]?.name?.en?.takeIf { it.isNotBlank() }?.let { equipmentNameKey(it) to container }
        }.toMap()
    }
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

/** A proficiency or trait: "tool:art" (Artisan's Tools) has the single tools as [children]. */
data class CatalogTrait(val key: String, val name: CatalogText, val children: List<String>)

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
    val advancement: List<AdvancementStep>,
    val startingEquipment: List<EquipmentNode> = emptyList(),
    /** Gold (GP) taken instead of the starting equipment, as Foundry's formula ("155"). */
    val wealth: String = ""
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
    val advancement: List<AdvancementStep>,
    val startingEquipment: List<EquipmentNode> = emptyList(),
    /** Gold (GP) taken instead of the starting equipment. */
    val wealth: String = ""
)

/** An item from Foundry's equipment; [weight] (lb) is known for what containers hold. */
data class CatalogEquipmentRef(val id: String, val name: CatalogText, val type: String, val weight: Double? = null)

/**
 * A container from Foundry's equipment (a backpack, a pouch, an equipment pack): what it carries
 * ([capacity] lb; nothing weighs inside a Bag of Holding), its own weight, price and text, and what
 * it comes with. [inside] is the container it comes in (a pack's waterskin), null for one sold alone.
 */
data class CatalogContainer(
    val id: String,
    val capacity: Double?,
    val weightlessContents: Boolean,
    val weight: Double,
    val price: Int?,
    val priceUnit: String?,
    val text: CatalogText,
    val inside: String?,
    val contents: List<CatalogContainerItem>
)

data class CatalogContainerItem(val itemId: String, val count: Int)

/** Starting equipment as Foundry lays it out: groups of all ([Group.any] false) or one of their children. */
sealed interface EquipmentNode {
    data class Group(val any: Boolean, val children: List<EquipmentNode>) : EquipmentNode
    data class Item(val itemId: String, val count: Int) : EquipmentNode
    /** Coins: [currency] is "gp", "sp"... */
    data class Currency(val currency: String, val count: Int) : EquipmentNode
    /** One item of a kind to pick: [category] "tool" with [key] "art" is an artisan's tool of choice. */
    data class Category(val category: String, val key: String, val count: Int) : EquipmentNode
}

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

/**
 * A spell of the catalog: the PHB's and the supplements', with the spell lists it is on
 * ("class:wizard", "subclass:light", "other:mark-healing") and what its card shows. Range, casting
 * time and duration are in the app's (SRD) wording: "60 feet", "1 bonus action", "Up to 1 minute".
 */
data class CatalogSpellRef(
    val id: String,
    val name: CatalogText,
    val level: Int?,
    /** Foundry's school key: "evo", "abj"... */
    val school: String = "",
    val book: String = "",
    val lists: List<String> = emptyList(),
    val components: String = "",
    val material: CatalogText = CatalogText(),
    val castingTime: String = "",
    val range: String = "",
    val duration: String = "",
    val ritual: Boolean = false,
    val concentration: Boolean = false,
    val text: CatalogText = CatalogText(),
    /** "Using a Higher-Level Spell Slot" / "Cantrip Upgrade", apart from [text]. */
    val higher: CatalogText = CatalogText(),
    val materialCost: String = "",
    /** The rolls, from Foundry's activities in the SRD's wording: "ranged", "DEX", "half", "8d6", "Fire". */
    val attackType: String = "",
    val saveAbility: String = "",
    val saveEffect: String = "",
    val damageBase: String = "",
    /** "MOD" for the spellcasting modifier, or a number. */
    val damageBonus: String = "",
    val damageType: String = "",
    val altDamageBase: String = "",
    val altDamageBonus: String = "",
    val altDamageType: String = "",
    /** The damage by slot (or character) level: "3: 8d6\n4: 9d6". */
    val damage: String = "",
    val healBase: String = "",
    val healBonus: String = "",
    val healing: String = "",
    /** "sphere, 20 ft". */
    val areaOfEffect: String = ""
)

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
        val restriction: ChoiceRestriction?,
        /** Spells picked here are prepared: 1 prepared, 2 always prepared (Foundry's spell.prepared); 0 not. */
        val spellPrepared: Int = 0
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
