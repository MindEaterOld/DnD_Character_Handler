package com.dndcharacterhandler.domain.usecase

import com.dndcharacterhandler.domain.dnd5e.model.CharacterBundle
import com.dndcharacterhandler.domain.repository.CharacterRepository
import kotlinx.coroutines.flow.Flow

class GetCharacterBundleUseCase(
    private val characterRepository: CharacterRepository
) {
    operator fun invoke(characterId: Long): Flow<CharacterBundle?> =
        characterRepository.observeCharacter(characterId)
}

