package com.dndcharacterhandler.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.dndcharacterhandler.data.localization.LocalizationRepository
import com.dndcharacterhandler.domain.model.AppLanguage
import com.dndcharacterhandler.domain.model.AppTheme
import com.dndcharacterhandler.domain.dnd5e.model.ArmorClassMode
import com.dndcharacterhandler.domain.dnd5e.model.Character
import com.dndcharacterhandler.domain.dnd5e.model.CharacterBundle
import com.dndcharacterhandler.domain.model.GameSystem
import com.dndcharacterhandler.presentation.localization.LocalStrings
import com.dndcharacterhandler.presentation.theme.DnDTheme

/** The drawer in [theme] with the app's own Russian texts, listing [state]'s characters. */
@Composable
private fun DrawerPreview(state: CharacterManagerUiState, theme: AppTheme = AppTheme.CLASSIC, closable: Boolean = true) {
    val context = LocalContext.current
    val strings = remember { LocalizationRepository(context).getStrings(AppLanguage.RUSSIAN) }
    CompositionLocalProvider(LocalStrings provides strings) {
        DnDTheme(theme) {
            CharacterManagerDrawer(
                state = state,
                onSelectCharacter = {},
                onCreateCharacter = {},
                onExportCharacter = {},
                onDeleteCharacter = {},
                onImportCharacter = {},
                onOpenSettings = {},
                onPickGameSystem = {},
                onClose = if (closable) ({}) else null
            )
        }
    }
}

private val previewCharacters = listOf(
    previewCharacterBundle(id = 1, name = "Аларик Штормвинд", race = "Человек", characterClass = "Волшебник", subclass = "Прорицатель", level = 7),
    previewCharacterBundle(id = 2, name = "Торин Железнобородый", race = "Дварф", characterClass = "Воин", subclass = "", level = 5),
    previewCharacterBundle(id = 3, name = "Элара Лунная Тень", race = "Эльф", characterClass = "Следопыт", subclass = "", level = 8)
)

@Preview(name = "Character Manager Drawer", showBackground = true, showSystemUi = true, device = "spec:width=412dp,height=915dp")
@Composable
fun CharacterManagerDrawerPreview() = DrawerPreview(
    CharacterManagerUiState(characters = previewCharacters, selectedCharacterId = 1, isLoaded = true)
)

@Preview(name = "Character Manager Drawer — engraving", showBackground = true, showSystemUi = true, device = "spec:width=412dp,height=915dp")
@Composable
fun CharacterManagerDrawerEngravedPreview() = DrawerPreview(
    CharacterManagerUiState(characters = previewCharacters, selectedCharacterId = 1, isLoaded = true),
    theme = AppTheme.ENGRAVED
)

/** The first launch: no characters, so only "New Character" and "Import", and no close button. */
@Preview(name = "Character Manager Drawer — no characters", showBackground = true, showSystemUi = true, device = "spec:width=412dp,height=915dp")
@Composable
fun CharacterManagerDrawerEmptyPreview() = DrawerPreview(CharacterManagerUiState(isLoaded = true), closable = false)

/** A system without its sheet yet: its list says so, and no character can be made in it. */
@Preview(name = "Character Manager Drawer — a system in development", showBackground = true, showSystemUi = true, device = "spec:width=412dp,height=915dp")
@Composable
fun CharacterManagerDrawerSystemInDevelopmentPreview() = DrawerPreview(
    CharacterManagerUiState(gameSystem = GameSystem.PATHFINDER_2E, isLoaded = true),
    closable = false
)

private fun previewCharacterBundle(
    id: Long,
    name: String,
    race: String,
    characterClass: String,
    subclass: String,
    level: Int
): CharacterBundle {
    return CharacterBundle(
        character = Character(
            id = id,
            name = name,
            race = race,
            characterClass = characterClass,
            subclass = subclass,
            level = level,
            portraitUri = null,
            currentHp = 8,
            maxHp = 8,
            temporaryHp = 0,
            hitDieSides = 8,
            spentHitDice = 0,
            hasInspiration = false,
            armorClass = 10,
            baseArmorClass = 10,
            armorClassMode = ArmorClassMode.AUTOMATIC,
            speed = 30,
            initiative = 0,
            initiativeBonus = 0,
            experience = 0,
            strength = 10,
            dexterity = 10,
            constitution = 10,
            intelligence = 10,
            wisdom = 10,
            charisma = 10,
            strengthSaveProficient = false,
            dexteritySaveProficient = false,
            constitutionSaveProficient = false,
            intelligenceSaveProficient = false,
            wisdomSaveProficient = false,
            charismaSaveProficient = false,
            passivePerceptionBonus = 0,
            armorProficiencies = "",
            weaponProficiencies = "",
            toolProficiencies = "",
            languageProficiencies = "",
            alignment = "",
            background = "",
            faith = "",
            homeland = "",
            age = "",
            gender = "",
            height = "",
            weight = "",
            eyes = "",
            hair = "",
            skin = "",
            personalityTraits = "",
            ideals = "",
            bonds = "",
            flaws = "",
            biography = "",
            createdAt = 0L,
            updatedAt = 0L
        ),
        skills = emptyList(),
        attacks = emptyList(),
        combatResources = emptyList(),
        inventoryItems = emptyList(),
        spells = emptyList(),
        features = emptyList(),
        notes = emptyList()
    )
}
