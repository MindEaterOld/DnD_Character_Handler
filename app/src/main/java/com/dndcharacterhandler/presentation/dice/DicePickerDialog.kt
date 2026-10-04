package com.dndcharacterhandler.presentation.dice

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Brush
import androidx.compose.material.icons.outlined.FileOpen
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dndcharacterhandler.domain.model.CustomDiceSkin
import com.dndcharacterhandler.domain.model.DicePattern
import com.dndcharacterhandler.presentation.components.EditDialog
import com.dndcharacterhandler.presentation.components.StepButton
import com.dndcharacterhandler.presentation.components.toggleContent
import com.dndcharacterhandler.presentation.components.toggleFill
import com.dndcharacterhandler.presentation.localization.text
import com.dndcharacterhandler.presentation.theme.LocalDesignTokens
import java.util.UUID

/** Most dice bodies on the table at once (a d100 counts as two). */
internal const val MAX_DICE_BODIES = 12

/**
 * Pick how many of each die to throw. The gear in the corner turns the dialog to the dice skins;
 * a picked skin applies right away ([onSkinChange]). The skins end with the player's own and
 * "Create your own", which opens the dice workshop on a copy of the picked look ([onCreateSkin],
 * with the id of the player's skin it copies, if it is one, so its pictures come too); the pencil on
 * one of the player's opens it on that one ([onEditSkin]); the last row loads a skin a friend
 * shared as a file ([onImportSkin]).
 */
@Composable
internal fun DicePickerDialog(
    initialSelection: Map<DieType, Int>,
    skin: DiceLook,
    customSkins: List<DiceLook.Custom>,
    onSkinChange: (DiceLook) -> Unit,
    onCreateSkin: (CustomDiceSkin, String?) -> Unit,
    onEditSkin: (CustomDiceSkin) -> Unit,
    onImportSkin: () -> Unit,
    onDismiss: () -> Unit,
    onRoll: (Map<DieType, Int>) -> Unit
) {
    var counts by remember { mutableStateOf(initialSelection) }
    var choosingSkin by remember { mutableStateOf(false) }
    val bodies = counts.entries.sumOf { (type, count) -> type.bodyCount * count }

    EditDialog(
        title = text(if (choosingSkin) "dice_skin_title" else "dice_picker_title"),
        onDismiss = onDismiss,
        onConfirm = { onRoll(counts.filterValues { it > 0 }) },
        confirmLabel = text("dice_picker_roll"),
        confirmEnabled = bodies > 0,
        // The gear turns to the skins; on the skins the arrow before the title turns back.
        titleLeading = if (choosingSkin) {
            {
                IconButton(onClick = { choosingSkin = false }) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = text("common_back"))
                }
            }
        } else {
            null
        },
        titleActions = if (choosingSkin) {
            null
        } else {
            {
                IconButton(onClick = { choosingSkin = true }) {
                    Icon(Icons.Outlined.Settings, contentDescription = text("dice_skin_title"))
                }
            }
        }
    ) {
        if (choosingSkin) {
            val start = newSkinFrom(skin, text("dice_workshop_default_name"))
            DiceSkinList(
                selected = skin,
                customSkins = customSkins,
                onSelect = onSkinChange,
                onCreate = { onCreateSkin(start, (skin as? DiceLook.Custom)?.skin?.id) },
                onEdit = onEditSkin,
                onImport = onImportSkin
            )
        } else {
            DiceCountList(counts = counts, bodies = bodies, onCountsChange = { counts = it })
        }
    }
}

@Composable
private fun DiceCountList(
    counts: Map<DieType, Int>,
    bodies: Int,
    onCountsChange: (Map<DieType, Int>) -> Unit
) {
    val colors = LocalDesignTokens.current.colors
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        DieType.entries.forEach { type ->
            val count = counts[type] ?: 0
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = type.label,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.text.primary
                )
                StepButton(
                    icon = Icons.Outlined.Remove,
                    contentDescription = text("common_decrease"),
                    onClick = { onCountsChange(counts + (type to count - 1)) },
                    enabled = count > 0,
                    size = 40.dp
                )
                Text(
                    text = count.toString(),
                    modifier = Modifier.widthIn(min = 44.dp),
                    style = MaterialTheme.typography.titleMedium,
                    color = if (count > 0) colors.accent.inspiration else colors.text.subtle,
                    textAlign = TextAlign.Center
                )
                StepButton(
                    icon = Icons.Outlined.Add,
                    contentDescription = text("common_increase"),
                    onClick = { onCountsChange(counts + (type to count + 1)) },
                    enabled = bodies + type.bodyCount <= MAX_DICE_BODIES,
                    size = 40.dp
                )
            }
        }
    }
}

/** Every skin with a d20 drawn in it; tapping one picks it. */
@Composable
private fun DiceSkinList(
    selected: DiceLook,
    customSkins: List<DiceLook.Custom>,
    onSelect: (DiceLook) -> Unit,
    onCreate: () -> Unit,
    onEdit: (CustomDiceSkin) -> Unit,
    onImport: () -> Unit
) {
    val colors = LocalDesignTokens.current.colors
    val shape = RoundedCornerShape(14.dp)
    val looks: List<DiceLook> = DiceSkin.entries.map { DiceLook.BuiltIn(it) } + customSkins
    // The list scrolls inside the dialog once the player has made a few skins.
    LazyColumn(modifier = Modifier.heightIn(max = 460.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(looks, key = { it.key }) { look ->
            val isSelected = look.key == selected.key
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .background(toggleFill(isSelected))
                    .clickable { onSelect(look) }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                DieSkinSwatch(look = look, modifier = Modifier.size(52.dp))
                Text(
                    text = when (look) {
                        is DiceLook.BuiltIn -> text(look.skin.nameKey)
                        is DiceLook.Custom -> look.skin.name
                    },
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    color = toggleContent(isSelected)
                )
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Outlined.Check,
                        contentDescription = null,
                        tint = toggleContent(true)
                    )
                }
                if (look is DiceLook.Custom) {
                    IconButton(onClick = { onEdit(look.skin) }) {
                        Icon(Icons.Outlined.Edit, contentDescription = text("common_edit"), tint = toggleContent(isSelected, colors.text.muted))
                    }
                }
            }
        }
        item(key = "create") {
            SkinListAction(Icons.Outlined.Brush, text("dice_workshop_create"), onCreate)
        }
        item(key = "import") {
            SkinListAction(Icons.Outlined.FileOpen, text("dice_skin_import"), onImport)
        }
    }
}

/** A row under the skins that does something rather than picking one. */
@Composable
private fun SkinListAction(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    val colors = LocalDesignTokens.current.colors
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surface.button)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(icon, contentDescription = null, tint = colors.text.primary, modifier = Modifier.size(28.dp))
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleMedium,
            color = colors.text.primary
        )
    }
}

/** A new skin of the player's, starting as [look]: a tweak of the gold dice is a tap away. */
@Composable
private fun newSkinFrom(look: DiceLook, name: String): CustomDiceSkin {
    val id = remember(look.key) { UUID.randomUUID().toString() }
    val style = rememberDiceSkinStyle(look)
    if (look is DiceLook.Custom) return look.skin.copy(id = id, name = name, faceArt = emptySet(), updatedAt = 0)
    val texture = style.texture
    return CustomDiceSkin(
        id = id,
        name = name,
        bodyColor = style.body.toArgb(),
        edgeColor = style.edge.toArgb(),
        numberColor = style.number.toArgb(),
        gloss = 0f,
        pattern = if (texture is DiceTexture.Web) DicePattern.Web(texture.thread.toArgb()) else DicePattern.None
    )
}
