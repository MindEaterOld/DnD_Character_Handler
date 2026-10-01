package com.dndcharacterhandler.presentation.spells

import com.dndcharacterhandler.data.localization.LocalizedStrings
import com.dndcharacterhandler.domain.model.AppLanguage
import com.dndcharacterhandler.domain.model.Spell
import com.dndcharacterhandler.domain.model.SpellCatalogItem
import com.dndcharacterhandler.presentation.localization.catalogFieldText

/** Catalog spell name in the current language: the catalog's Russian, or its English for the others. */
internal fun SpellCatalogItem.localizedName(strings: LocalizedStrings): String =
    if (strings.language == AppLanguage.RUSSIAN) ruName.ifBlank { name } else name

/** A character's copy of a catalog spell, with its text in the current language. */
internal fun SpellCatalogItem.toLocalizedSpell(strings: LocalizedStrings): Spell =
    toSpell().localizedWith(this, strings)

/**
 * A character's catalog spell with its name, description, higher-level text and material in the
 * current language; fields the user edited are kept (see [catalogFieldText]).
 */
internal fun Spell.localizedWith(catalogItem: SpellCatalogItem?, strings: LocalizedStrings): Spell {
    if (catalogItem == null) return this
    val russian = strings.language == AppLanguage.RUSSIAN
    return copy(
        name = catalogFieldText(
            stored = name,
            english = catalogItem.name,
            russian = catalogItem.ruName,
            current = catalogItem.localizedName(strings)
        ),
        description = catalogText(description, catalogItem.description, catalogItem.ruDescription, russian),
        higherLevelDescription = catalogText(
            higherLevelDescription,
            catalogItem.higherLevelDescription,
            catalogItem.ruHigherLevel,
            russian
        ),
        material = catalogText(material, catalogItem.material, catalogItem.ruMaterial, russian)
    )
}

/** Localizes every catalog-backed spell in [spells]; spells without a catalog entry are left as they are. */
internal fun List<Spell>.localizedWith(
    catalogById: Map<String, SpellCatalogItem>,
    strings: LocalizedStrings
): List<Spell> = map { spell -> spell.localizedWith(spell.catalogId?.let(catalogById::get), strings) }

private fun catalogText(stored: String, english: String, russian: String, useRussian: Boolean): String =
    catalogFieldText(
        stored = stored,
        english = english,
        russian = russian,
        // Some catalog spells have no Russian text for a field; fall back to English instead of blank.
        current = if (useRussian && russian.isNotBlank()) russian else english
    )
