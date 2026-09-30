package com.dndcharacterhandler.domain.repository

import com.dndcharacterhandler.domain.model.CharacterCatalog

/** The imported character-building catalog: classes, subclasses, species, backgrounds and features. */
interface CharacterCatalogRepository {
    suspend fun getCatalog(): CharacterCatalog
}
