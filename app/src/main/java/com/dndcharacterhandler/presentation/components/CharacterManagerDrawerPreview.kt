package com.dndcharacterhandler.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.tooling.preview.Preview
import com.dndcharacterhandler.data.localization.LocalizedStrings
import com.dndcharacterhandler.domain.model.AppLanguage
import com.dndcharacterhandler.domain.model.ArmorClassMode
import com.dndcharacterhandler.domain.model.Character
import com.dndcharacterhandler.domain.model.CharacterBundle
import com.dndcharacterhandler.presentation.localization.LocalStrings
import com.dndcharacterhandler.presentation.theme.DnDTheme

@Preview(
    name = "Character Manager Drawer",
    showBackground = true,
    showSystemUi = true,
    device = "spec:width=412dp,height=915dp"
)
@Composable
fun CharacterManagerDrawerPreview() {
    val previewStrings = LocalizedStrings(
        language = AppLanguage.ENGLISH,
        values = mapOf(
            "drawer_characters" to "Characters",
            "drawer_new_character" to "New Character",
            "drawer_export_character" to "Export Character",
            "drawer_delete_character" to "Delete Character",
            "drawer_import_character" to "Import Character",
            "drawer_level" to "Level %1\$s",
            "placeholder_loading_character" to "Unnamed Adventurer",
            "placeholder_race" to "Human",
            "placeholder_class" to "Wizard",
            "placeholder_subclass" to "Subclass",
            "language_en" to "English",
            "language_ru" to "Russian",
            "language_de" to "German",
            "language_fr" to "French",
            "language_es" to "Spanish"
        )
    )

    CompositionLocalProvider(LocalStrings provides previewStrings) {
        DnDTheme {
            CharacterManagerDrawer(
                state = CharacterManagerUiState(
                    characters = listOf(
                        previewCharacterBundle(
                            id = 1,
                            name = "Alaric Stormwind",
                            race = "Human",
                            characterClass = "Wizard",
                            subclass = "Divination",
                            level = 7
                        ),
                        previewCharacterBundle(
                            id = 2,
                            name = "Thorin Ironbeard",
                            race = "Dwarf",
                            characterClass = "Fighter",
                            subclass = "",
                            level = 5
                        ),
                        previewCharacterBundle(
                            id = 3,
                            name = "Elara Moonshadow",
                            race = "Elf",
                            characterClass = "Ranger",
                            subclass = "",
                            level = 8
                        )
                    ),
                    selectedCharacterId = 1,
                    language = AppLanguage.ENGLISH
                ),
                onSelectCharacter = {},
                onCreateCharacter = {},
                onExportCharacter = {},
                onDeleteCharacter = {},
                onImportCharacter = {},
                onClose = {}
            )
        }
    }
}

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
