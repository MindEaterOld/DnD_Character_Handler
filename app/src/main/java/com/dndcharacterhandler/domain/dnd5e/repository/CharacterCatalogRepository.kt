package com.dndcharacterhandler.domain.dnd5e.repository

import com.dndcharacterhandler.domain.dnd5e.model.CharacterCatalog

/** The imported character-building catalog: classes, subclasses, species, backgrounds and features. */
interface CharacterCatalogRepository {
    suspend fun getCatalog(): CharacterCatalog
}
