package com.dndcharacterhandler.presentation.overview

import com.dndcharacterhandler.presentation.components.buttonFill
import androidx.compose.ui.semantics.disabled
import com.dndcharacterhandler.presentation.components.CharacterHeaderInset
import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.outlined.Crop
import androidx.compose.material.icons.Icons
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.dndcharacterhandler.presentation.components.AppImage
import com.dndcharacterhandler.presentation.components.EditDialog
import com.dndcharacterhandler.presentation.components.OverlayCloseButton
import com.dndcharacterhandler.presentation.localization.text
import com.dndcharacterhandler.presentation.theme.LocalDesignTokens
import java.io.File
import java.io.FileOutputStream

/**
 * Where a tap on the portrait opens its menu (show, change, crop): the art's middle only, clear of everything on it
 * (owner, 2026-10-10 — the whole art took the tap, and one on «+» at full hit points, disabled, fell through to it) —
 * under the header, between the side buttons' columns (24dp in and their 44dp), down to the survival block, which
 * stands under it in the same column.
 */
@Composable
internal fun PortraitTapZone(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val label = text("overview_portrait_menu_title")
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = CharacterHeaderInset, start = PortraitSideColumn, end = PortraitSideColumn)
            .clickable(onClickLabel = label, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = label }
    )
}

/** A side buttons' column with its inset from the screen's edge. */
private val PortraitSideColumn = 24.dp + PortraitSideButtonSize

@Composable
internal fun PortraitFallback(characterName: String) {
    val token = LocalDesignTokens.current.typography.portraitInitial
    val colors = LocalDesignTokens.current.colors
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        colors.surface.button,
                        colors.surface.card,
                        colors.background.page
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = characterName.take(1).ifBlank { "?" },
            style = MaterialTheme.typography.headlineMedium.copy(fontSize = token.fontSizeSp.sp),
            color = colors.text.primary
        )
    }
}

@Composable
internal fun PortraitMenuDialog(
    hasPortrait: Boolean,
    onShowPortrait: () -> Unit,
    onChangePortrait: () -> Unit,
    onFramePortrait: () -> Unit,
    onDismiss: () -> Unit
) {
    EditDialog(
        title = text("overview_portrait_menu_title"),
        onDismiss = onDismiss
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            PortraitMenuOption(
                label = text("overview_portrait_show"),
                icon = Icons.Outlined.Visibility,
                enabled = hasPortrait,
                onClick = onShowPortrait
            )
            PortraitMenuOption(
                label = text("overview_portrait_change"),
                icon = Icons.Outlined.Image,
                enabled = true,
                onClick = onChangePortrait
            )
            PortraitMenuOption(
                label = text("overview_portrait_frame"),
                icon = Icons.Outlined.Crop,
                enabled = true,
                onClick = onFramePortrait
            )
        }
    }
}

@Composable
private fun PortraitMenuOption(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val colors = LocalDesignTokens.current.colors
    val contentColor = if (enabled) colors.text.primary else colors.text.subtle
    // An action, so a button: the standard button fill, no outline (CLAUDE.md, buttons vs. stats; owner, 2026-10-13 —
    // the plain rows read as text). As the drawer's actions.
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(buttonFill())
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = contentColor
        )
    }
}

@Composable
internal fun PortraitViewer(
    portraitUri: String,
    characterName: String,
    onClose: () -> Unit
) {
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        PortraitViewerContent(
            portraitUri = portraitUri,
            characterName = characterName,
            onClose = onClose
        )
    }
}

@Composable
private fun PortraitViewerContent(
    portraitUri: String,
    characterName: String,
    onClose: () -> Unit
) {
    val colors = LocalDesignTokens.current.colors
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background.page)
    ) {
        AppImage(
            imageRef = portraitUri,
            contentDescription = characterName,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )
        OverlayCloseButton(onClick = onClose, modifier = Modifier.align(Alignment.TopEnd))
    }
}

internal fun copyPortraitToCharacterFiles(
    context: Context,
    characterId: Long,
    sourceUri: Uri
): String? {
    val extension = runCatching { guessImageExtension(context, sourceUri) }.getOrDefault("jpg")
    val directory = File(context.filesDir, "character_portraits/$characterId").apply {
        mkdirs()
    }
    val target = File(directory, "portrait.${System.currentTimeMillis()}.$extension")
    val copied = runCatching {
        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            FileOutputStream(target).use { output ->
                input.copyTo(output)
            }
        } != null
    }.getOrDefault(false)
    if (!copied) {
        target.delete()
        return null
    }
    // Only drop the previous portrait once the new one is safely on disk.
    directory.listFiles()
        ?.filter { it.name.startsWith("portrait.") && it != target }
        ?.forEach { it.delete() }
    return target.absolutePath
}

private fun guessImageExtension(
    context: Context,
    uri: Uri
): String {
    val mimeType = context.contentResolver.getType(uri)
    val mimeExtension = mimeType
        ?.let { MimeTypeMap.getSingleton().getExtensionFromMimeType(it) }
        ?.lowercase()
    if (!mimeExtension.isNullOrBlank()) return mimeExtension

    val pathExtension = uri.lastPathSegment
        ?.substringAfterLast('.', missingDelimiterValue = "")
        ?.lowercase()
        ?.takeIf { it.matches(Regex("[a-z0-9]{1,5}")) }
    return pathExtension ?: "jpg"
}
