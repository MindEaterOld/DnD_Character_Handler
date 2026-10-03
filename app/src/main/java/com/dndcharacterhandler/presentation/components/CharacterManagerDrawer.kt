package com.dndcharacterhandler.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import com.dndcharacterhandler.data.localization.LocalizedStrings
import com.dndcharacterhandler.domain.model.PortraitFraming
import com.dndcharacterhandler.domain.model.AssetReferences
import com.dndcharacterhandler.domain.model.Character
import com.dndcharacterhandler.domain.model.CharacterBundle
import com.dndcharacterhandler.presentation.localization.LocalStrings
import com.dndcharacterhandler.presentation.localization.text
import com.dndcharacterhandler.presentation.theme.DnDTheme
import com.dndcharacterhandler.presentation.theme.LocalDesignTokens

@Composable
fun CharacterManagerDrawer(
    state: CharacterManagerUiState,
    onSelectCharacter: (Long) -> Unit,
    onCreateCharacter: () -> Unit,
    onExportCharacter: () -> Unit,
    onDeleteCharacter: () -> Unit,
    onImportCharacter: () -> Unit,
    onOpenSettings: () -> Unit,
    /** Null while there are no characters: the drawer can't be closed then. */
    onClose: (() -> Unit)?
) {
    val tokens = LocalDesignTokens.current.typography
    val colors = LocalDesignTokens.current.colors

    // The whole screen wide: with no scrim to tap beside it, it closes with the cross, Back or a swipe.
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .fillMaxWidth()
            .background(
                Brush.radialGradient(
                    colors = listOf(colors.background.radialStart, colors.background.radialMiddle, colors.background.radialEnd),
                    radius = 1300f
                )
            )
            .statusBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 20.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            item {
                Text(
                    text = text("drawer_characters"),
                    // Clear of the close button.
                    modifier = Modifier.padding(end = 60.dp),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontSize = tokens.characterName.fontSizeSp.sp,
                        lineHeight = (tokens.characterName.lineHeightSp ?: tokens.characterName.fontSizeSp).sp
                    ),
                    color = colors.text.primary
                )
                DrawerOrnamentDivider(modifier = Modifier.padding(top = 4.dp, bottom = 10.dp))
            }

            items(state.characters, key = { it.character.id }) { characterBundle ->
                DrawerCharacterCard(
                    characterBundle = characterBundle,
                    selected = state.selectedCharacterId == characterBundle.character.id,
                    onClick = { onSelectCharacter(characterBundle.character.id) }
                )
            }

            item {
                DrawerActionCard(
                    label = text("drawer_new_character"),
                    icon = Icons.Outlined.AddCircleOutline,
                    onClick = onCreateCharacter,
                    dashed = true,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }

            // Export and delete act on the selected character: none without characters.
            if (state.characters.isNotEmpty()) {
                item {
                    DrawerOrnamentDivider(modifier = Modifier.padding(top = 10.dp, bottom = 6.dp))
                    DrawerActionCard(
                        label = text("drawer_export_character"),
                        icon = Icons.Outlined.FileUpload,
                        onClick = onExportCharacter
                    )
                }

                item {
                    DrawerActionCard(
                        label = text("drawer_delete_character"),
                        icon = Icons.Outlined.Delete,
                        onClick = onDeleteCharacter
                    )
                }
            }

            item {
                DrawerOrnamentDivider(modifier = Modifier.padding(top = 10.dp, bottom = 6.dp))
                DrawerActionCard(
                    label = text("drawer_import_character"),
                    icon = Icons.Outlined.FileDownload,
                    onClick = onImportCharacter
                )
            }

            item {
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
        // Pinned to the bottom, where the screens have their tab bar.
        DrawerActionCard(
            label = text("overview_settings"),
            icon = Icons.Outlined.Settings,
            onClick = onOpenSettings,
            modifier = Modifier
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        )
        }
        if (onClose != null) {
            OverlayCloseButton(onClick = onClose, modifier = Modifier.align(Alignment.TopEnd))
        }
    }
}

@Composable
private fun DrawerCharacterCard(
    characterBundle: CharacterBundle,
    selected: Boolean,
    onClick: () -> Unit
) {
    val character = characterBundle.character
    val strings = LocalStrings.current
    val tokens = LocalDesignTokens.current.typography
    val colors = LocalDesignTokens.current.colors
    val characterName = character.name.ifBlank { text("overview_name_placeholder") }
    val classLabel = buildDrawerClassLabel(characterBundle, strings)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(96.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = colors.surface.card.copy(alpha = 0.72f),
        border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary else colors.border.miniCard)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DrawerSelectionDot(selected = selected)
            DrawerPortrait(
                portraitUri = character.portraitUri,
                framing = character.portraitFraming,
                characterName = characterName,
                modifier = Modifier.padding(start = 8.dp)
            )
            Column(
                modifier = Modifier
                    .padding(start = 12.dp)
                    .weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = characterName,
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.text.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = classLabel,
                    modifier = Modifier.padding(top = 3.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.text.muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = strings.format("drawer_level", character.level),
                    modifier = Modifier.padding(top = 2.dp),
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = tokens.subtitleToken.fontSizeSp.sp),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun DrawerSelectionDot(selected: Boolean) {
    val colors = LocalDesignTokens.current.colors
    Box(
        modifier = Modifier
            .size(20.dp)
            .clip(CircleShape)
            .background(Color.Transparent),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(18.dp)) {
            drawCircle(
                color = if (selected) colors.accent.inspiration else Color.Transparent,
                radius = 6.dp.toPx()
            )
            drawCircle(
                color = if (selected) colors.accent.inspiration else colors.text.label,
                radius = 7.dp.toPx(),
                style = Stroke(width = 1.5.dp.toPx())
            )
        }
    }
}

@Composable
private fun DrawerPortrait(
    portraitUri: String?,
    framing: PortraitFraming,
    characterName: String,
    modifier: Modifier = Modifier
) {
    val portraitReference = portraitUri ?: AssetReferences.portraitPlaceholderPath("portrait_placeholder.png")
    val colors = LocalDesignTokens.current.colors

    Box(
        modifier = modifier.size(70.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = colors.ornament.stroke,
                radius = size.minDimension / 2f - 4.dp.toPx(),
                style = Stroke(width = 1.dp.toPx())
            )
            drawCircle(
                color = colors.border.miniCard,
                radius = size.minDimension / 2f - 9.dp.toPx(),
                style = Stroke(width = 1.dp.toPx())
            )
        }
        Surface(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape),
            shape = CircleShape,
            color = colors.surface.portrait
        ) {
            AppImage(
                imageRef = portraitReference,
                contentDescription = characterName,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                framing = framing,
                fallback = {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(colors.ornament.dot),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = characterName.take(1).ifBlank { "?" },
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontSize = LocalDesignTokens.current.typography.portraitInitial.fontSizeSp.sp
                            ),
                            color = colors.text.primary
                        )
                    }
                }
            )
        }
    }
}

@Composable
private fun DrawerActionCard(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconTint: Color = LocalDesignTokens.current.colors.text.action,
    dashed: Boolean = false
) {
    val tokens = LocalDesignTokens.current.typography
    val colors = LocalDesignTokens.current.colors

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = colors.surface.card.copy(alpha = 0.48f),
        border = BorderStroke(1.dp, if (dashed) MaterialTheme.colorScheme.outline else colors.border.miniCard)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = label,
                modifier = Modifier.padding(start = 14.dp),
                style = MaterialTheme.typography.titleMedium.copy(fontSize = tokens.titleMedium.fontSizeSp.sp),
                color = colors.text.muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
/** A plain hairline between the drawer's groups. */
private fun DrawerOrnamentDivider(modifier: Modifier = Modifier) {
    val colors = LocalDesignTokens.current.colors
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(18.dp)
    ) {
        val centerY = size.height / 2f
        drawLine(
            color = colors.ornament.stroke,
            start = Offset(0f, centerY),
            end = Offset(size.width, centerY),
            strokeWidth = 1.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}

private fun buildDrawerClassLabel(
    characterBundle: CharacterBundle,
    strings: com.dndcharacterhandler.data.localization.LocalizedStrings
): String {
    val character = characterBundle.character
    val race = character.race.ifBlank { strings["placeholder_race"] }
    val characterClass = character.characterClass.ifBlank { strings["placeholder_class"] }
    val subclass = character.subclass.ifBlank { null }
    val classLabel = if (subclass != null) "$subclass $characterClass" else characterClass
    return "$race • $classLabel"
}
