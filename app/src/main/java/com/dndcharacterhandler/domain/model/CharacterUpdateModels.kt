package com.dndcharacterhandler.domain.model

enum class CharacterProficiencyField {
    ARMOR,
    WEAPON,
    TOOL,
    /** The weapons the character has mastered (weapon ids, as the weapon field's). */
    WEAPON_MASTERY,
    LANGUAGE,
    /** Resistances, immunities, vulnerabilities (trait keys, see Defenses). */
    DEFENSES
}

enum class CharacterTextField {
    ALIGNMENT,
    BACKGROUND,
    FAITH,
    HOMELAND,
    PERSONALITY_TRAITS,
    IDEALS,
    BONDS,
    FLAWS,
    AGE,
    GENDER,
    HEIGHT,
    WEIGHT,
    EYES,
    HAIR,
    SKIN,
    BIOGRAPHY
}
