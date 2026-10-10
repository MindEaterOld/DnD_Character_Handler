package com.dndcharacterhandler.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dndcharacterhandler.domain.dnd5e.model.CharacterBundle
import com.dndcharacterhandler.domain.usecase.GetCharacterBundleUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class CharacterSectionUiState(
    val character: CharacterBundle? = null
)

abstract class BaseCharacterViewModel(
    getCharacterBundleUseCase: GetCharacterBundleUseCase,
    selectedCharacterHolder: SelectedCharacterHolder
) : ViewModel() {
    private val _uiState = MutableStateFlow(CharacterSectionUiState())
    val uiState: StateFlow<CharacterSectionUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            selectedCharacterHolder.selectedCharacterId.collectLatest { characterId ->
                if (characterId == null) {
                    _uiState.value = CharacterSectionUiState(character = null)
                } else {
                    getCharacterBundleUseCase(characterId).collectLatest { character ->
                        _uiState.value = CharacterSectionUiState(character = character)
                    }
                }
            }
        }
    }
}
