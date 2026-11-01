package com.dndcharacterhandler.presentation.components

import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.foundation.layout.width
import com.dndcharacterhandler.domain.dnd5e.rules.experienceProgress
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.border
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import com.dndcharacterhandler.domain.model.GameSystem
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
import com.dndcharacterhandler.data.localization.LocalizedStrings
import com.dndcharacterhandler.domain.model.PortraitFraming
import com.dndcharacterhandler.domain.model.AssetReferences
import com.dndcharacterhandler.domain.dnd5e.model.CharacterBundle
import com.dndcharacterhandler.presentation.localization.LocalStrings
import com.dndcharacterhandler.presentation.localization.text
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
    /** Turns the drawer to another game system: its characters, and new ones made in it. */
    onPickGameSystem: (GameSystem) -> Unit,
    /** Null while there are no characters: the drawer can't be closed then. */
    onClose: (() -> Unit)?
) {
    val tokens = LocalDesignTokens.current.typography
    val colors = LocalDesignTokens.current.colors
    // The system card unfolds in place (A); Back folds it before it closes the drawer.
    var isPickingSystem by remember { mutableStateOf(false) }
    BackHandler(enabled = isPickingSystem) { isPickingSystem = false }

    // The whole screen wide: with no scrim to tap beside it, it closes with the cross, Back or a swipe.
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .fillMaxWidth()
            .background(colors.background.page)
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
            }

            // The game system heads the list: the characters under it are its own (owner's wish, 2026-10-10).
            item {
                GameSystemCard(
                    system = state.gameSystem,
                    expanded = isPickingSystem,
                    onToggle = { isPickingSystem = !isPickingSystem },
                    onPick = { system ->
                        onPickGameSystem(system)
                        isPickingSystem = false
                    },
                    modifier = Modifier.padding(top = 12.dp)
                )
                // 20dp of air over the rule and under it: the rule is the middle of its 18dp, the list's 6dp gap under the item.
                DrawerOrnamentDivider(modifier = Modifier.padding(top = 11.dp, bottom = 5.dp))
            }

            if (state.isLoaded && state.characters.isEmpty()) {
                item {
                    Text(
                        text = text("drawer_no_characters"),
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.bodyLarge,
                        color = colors.text.muted
                    )
                }
            }

            // Export and delete act on the picked character: they stand on its card (owner's choice from boards, 2026-11-01: В).
            items(state.characters, key = { it.character.id }) { characterBundle ->
                val selected = state.selectedCharacterId == characterBundle.character.id
                DrawerCharacterCard(
                    characterBundle = characterBundle,
                    selected = selected,
                    onClick = { onSelectCharacter(characterBundle.character.id) },
                    onExport = onExportCharacter.takeIf { selected },
                    onDelete = onDeleteCharacter.takeIf { selected }
                )
            }

            // A character comes in: made anew (in a system with its rules and sheet only) or imported, side by side.
            item {
                Row(
                    // 10dp under the cards (with the list's 6dp gap): they go with the list, not with the rule below.
                    modifier = Modifier.padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(DrawerActionGap)
                ) {
                    if (state.gameSystem.available) {
                        DrawerActionCard(
                            label = text("drawer_new_short"),
                            icon = Icons.Outlined.AddCircleOutline,
                            onClick = onCreateCharacter,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    DrawerActionCard(
                        label = text("drawer_import_short"),
                        icon = Icons.Outlined.FileDownload,
                        onClick = onImportCharacter,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // The app's own right under them, past a rule (owner, 2026-11-01 — pinned to the foot, a hole stood above them).
            item {
                // 20dp of air over the rule and under it, as under the system's card (the list's 6dp gap over the item).
                DrawerOrnamentDivider(modifier = Modifier.padding(top = 5.dp, bottom = 11.dp))
                val snackbar = LocalAppSnackbar.current
                val uriHandler = LocalUriHandler.current
                val strings = LocalStrings.current
                Row(horizontalArrangement = Arrangement.spacedBy(DrawerActionGap)) {
                    DrawerActionCard(
                        label = text("drawer_settings_short"),
                        icon = Icons.Outlined.Settings,
                        onClick = onOpenSettings,
                        modifier = Modifier.weight(1f)
                    )
                    DrawerActionCard(
                        label = text("drawer_contact"),
                        icon = Icons.Outlined.MailOutline,
                        onClick = {
                            val url = ContactPageUrl
                            if (url != null) uriHandler.openUri(url) else snackbar.show(strings["drawer_contact_soon"], null) {}
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.navigationBarsPadding().height(12.dp))
            }
        }
        }
        if (onClose != null) {
            OverlayCloseButton(onClick = onClose, modifier = Modifier.align(Alignment.TopEnd))
        }
    }
}

/**
 * A character in the drawer (owner's choice from boards, 2026-11-01: А — before, a round portrait in rings and a radio
 * dot): the portrait a window on the card's left, the whole height, melting into the card toward its middle as the
 * overview's does (an alpha mask, no colour laid over it); beside it the header's line — the Roman numeral and «LVL»,
 * the name — the experience's rule under it as the header's, and the species and class muted. The picked one is told by
 * its gold outline alone.
 */
@Composable
private fun DrawerCharacterCard(
    characterBundle: CharacterBundle,
    selected: Boolean,
    onClick: () -> Unit,
    /** The picked card's own actions: null on the others. */
    onExport: (() -> Unit)?,
    onDelete: (() -> Unit)?
) {
    val character = characterBundle.character
    val strings = LocalStrings.current
    val colors = LocalDesignTokens.current.colors
    val gold = MaterialTheme.colorScheme.primary
    val characterName = character.name.ifBlank { text("overview_name_placeholder") }
    val classLabel = buildDrawerClassLabel(characterBundle, strings)
    val shape = RoundedCornerShape(8.dp)
    val portraitReference = character.portraitUri ?: AssetReferences.portraitPlaceholderPath("portrait_placeholder.webp")
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(DrawerCardHeight)
            .clip(shape)
            .background(colors.surface.card.copy(alpha = LocalDesignTokens.current.alpha.veil))
            .border(1.dp, if (selected) gold else colors.border.default, shape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
    ) {
        AppImage(
            imageRef = portraitReference,
            contentDescription = characterName,
            modifier = Modifier
                .width(DrawerPortraitWidth)
                .fillMaxHeight()
                .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                .drawWithContent {
                    drawContent()
                    drawRect(
                        Brush.horizontalGradient(0.45f to Color.Black, 1f to Color.Transparent),
                        blendMode = BlendMode.DstIn
                    )
                },
            contentScale = ContentScale.Crop,
            framing = character.portraitFraming,
            fallback = {}
        )
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(start = DrawerTextStart, end = if (onExport != null) DrawerToolsRoom else 14.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Row {
                Text(
                    text = romanNumeral(character.level),
                    modifier = Modifier.alignByBaseline(),
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.text.primary
                )
                Text(
                    text = " " + text("overview_level_short").uppercase() + "  ",
                    modifier = Modifier.alignByBaseline(),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp),
                    color = colors.text.muted
                )
                Text(
                    text = characterName,
                    modifier = Modifier.alignByBaseline(),
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.text.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            // The experience's rule as the header's: 3dp on the faint track, gold as far as it goes.
            val progress = experienceProgress(character.level, character.experience)
            Canvas(modifier = Modifier.padding(top = 6.dp).width(DrawerRuleWidth).height(3.dp)) {
                val corner = CornerRadius(size.height / 2)
                drawRoundRect(colors.border.faint, size = size, cornerRadius = corner)
                if (progress > 0f) drawRoundRect(gold, size = Size(size.width * progress, size.height), cornerRadius = corner)
            }
            Text(
                text = classLabel,
                modifier = Modifier.padding(top = 8.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.text.muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        // Export over delete at the card's right end, round buttons as a pop-up's trash (CLAUDE.md, buttons).
        if (onExport != null && onDelete != null) {
            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DrawerCardTool(Icons.Outlined.FileUpload, text("drawer_export_character"), colors.text.primary, buttonFill(), onExport)
                DrawerCardTool(
                    Icons.Outlined.Delete,
                    text("drawer_delete_character"),
                    colors.accent.danger,
                    colors.accent.danger.copy(alpha = LocalDesignTokens.current.alpha.faint),
                    onDelete
                )
            }
        }
    }
}

private val DrawerCardHeight = 96.dp
private val DrawerPortraitWidth = 120.dp
private val DrawerTextStart = 104.dp
private val DrawerRuleWidth = 140.dp
/** The picked card's text stops short of its two buttons. */
private val DrawerToolsRoom = 60.dp
private val DrawerToolSize = 38.dp
/** Between two actions side by side. */
private val DrawerActionGap = 8.dp

/**
 * Where «Связаться с нами» leads: the app's page on itch.io (owner, 2026-11-01). Null until the page is up — the button
 * says so in a notice meanwhile.
 */
private val ContactPageUrl: String? = null

/** One of the picked card's actions: a round button, its fill and its icon's colour by its role. */
@Composable
private fun DrawerCardTool(icon: ImageVector, description: String, tint: Color, fill: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(DrawerToolSize)
            .clip(CircleShape)
            .background(fill)
            .clickable(role = Role.Button, onClickLabel = description, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(imageVector = icon, contentDescription = description, tint = tint, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun DrawerActionCard(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalDesignTokens.current.typography
    val colors = LocalDesignTokens.current.colors

    // An action, so a button: the standard button fill, no outline (CLAUDE.md, buttons vs. stats).
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = buttonFill()
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
                tint = colors.text.primary,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = label,
                modifier = Modifier.padding(start = 14.dp),
                style = MaterialTheme.typography.titleMedium.copy(fontSize = tokens.titleMedium.fontSizeSp.sp),
                color = colors.text.primary,
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
            color = colors.border.rule,
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
