package com.dndcharacterhandler.presentation.dice

import androidx.compose.material3.SliderDefaults
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Casino
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dndcharacterhandler.domain.model.CustomDiceSkin
import com.dndcharacterhandler.domain.model.DiceFontIds
import com.dndcharacterhandler.domain.model.DicePattern
import com.dndcharacterhandler.presentation.components.OverlayCloseButton
import com.dndcharacterhandler.presentation.components.ToggleChip
import com.dndcharacterhandler.presentation.components.toggleContent
import com.dndcharacterhandler.presentation.components.toggleFill
import com.dndcharacterhandler.presentation.localization.LocalStrings
import com.dndcharacterhandler.presentation.localization.text
import com.dndcharacterhandler.presentation.theme.LocalDesignTokens
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import kotlin.random.Random

/** Which colour of the skin the colour picker is choosing. */
private enum class SkinColor { BODY, EDGE, NUMBER, NUMBER_OUTLINE, PATTERN, PATTERN_GLOW }

/** The die kinds a skin can have face pictures for, as the workshop names them. */
private val FaceArtKinds = listOf(
    DieShapeKind.D4 to "d4",
    DieShapeKind.D6 to "d6",
    DieShapeKind.D8 to "d8",
    DieShapeKind.D10 to "d10",
    DieShapeKind.D10_TENS to "d10 ×10",
    DieShapeKind.D12 to "d12",
    DieShapeKind.D20 to "d20"
)

/** The dice the preview shows, by the chips above it: the d100 is its two d10s, the tens and the units. */
private val PreviewDice = listOf(
    listOf(DieShapeKind.D4) to "d4",
    listOf(DieShapeKind.D6) to "d6",
    listOf(DieShapeKind.D8) to "d8",
    listOf(DieShapeKind.D12) to "d12",
    listOf(DieShapeKind.D20) to "d20",
    listOf(DieShapeKind.D10_TENS, DieShapeKind.D10) to "d100"
)

/**
 * The dice workshop: the player's own dice skin, seen on a die turning above its settings — the
 * faces' colour, see-through and shine; the edges; the numbers' colour, outline, font and size; a
 * pattern over the faces (a web, marble, a nebula, or a picture as their material); and pictures of
 * their own for every face of a die (a face template saved, painted, loaded back). A random look by
 * DiceSkinGenerator's rules is a tap away, and the skin as it stands can be shared as one file.
 * Nothing is kept until Save; pictures loaded meanwhile wait as drafts.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun DiceWorkshopOverlay(
    viewModel: DiceSkinsViewModel,
    initial: CustomDiceSkin,
    isNew: Boolean,
    onClose: () -> Unit,
    /** The player's skin a new one is made from: its pictures come along as drafts. */
    copiedFrom: String? = null
) {
    val colors = LocalDesignTokens.current.colors
    val strings = LocalStrings.current
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val fonts = LocalFontFamilyResolver.current
    val numberStyle = MaterialTheme.typography.headlineMedium
    val scope = rememberCoroutineScope()
    var skin by remember { mutableStateOf(initial) }
    var draftPicture by remember { mutableStateOf<File?>(null) }
    var draftFaceArt by remember { mutableStateOf(mapOf<String, File?>()) }
    var previewKind by remember { mutableStateOf(DieShapeKind.D20) }
    var picking by remember { mutableStateOf<SkinColor?>(null) }
    var templateKind by remember { mutableStateOf<DieShapeKind?>(null) }
    var faceArtKind by remember { mutableStateOf<DieShapeKind?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }
    val look = viewModel.customLook(skin, draftPicture, draftFaceArt)

    fun close() {
        viewModel.discardDraft()
        onClose()
    }
    BackHandler(onBack = ::close)
    LaunchedEffect(copiedFrom) {
        val source = copiedFrom ?: return@LaunchedEffect
        val (picture, faces) = viewModel.copyPicturesToDraft(source)
        draftPicture = picture
        draftFaceArt = faces
        // A new version, so the preview reads the pictures.
        skin = skin.copy(updatedAt = System.nanoTime())
    }
    LaunchedEffect(message) {
        if (message != null) {
            delay(2500)
            message = null
        }
    }

    val pictureLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            val file = viewModel.importPicture(uri)
            if (file != null) {
                draftPicture = file
                // A new version, so the preview reads the picture again.
                skin = skin.copy(pattern = skin.pattern as? DicePattern.Picture ?: DicePattern.Picture(), updatedAt = System.nanoTime())
            } else {
                message = strings["dice_workshop_image_failed"]
            }
        }
    }
    val faceArtLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        val kind = faceArtKind ?: return@rememberLauncherForActivityResult
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            val file = viewModel.importFaceArt(kind, uri)
            if (file != null) {
                draftFaceArt = draftFaceArt + (kind.name to file)
                previewKind = kind
                skin = skin.copy(updatedAt = System.nanoTime())
            } else {
                message = strings["dice_workshop_image_failed"]
            }
        }
    }
    // Not "application/zip": the system's file picker would add ".zip" to the name.
    val shareLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            message = strings[if (viewModel.exportSkin(uri, look)) "dice_workshop_shared" else "dice_workshop_share_failed"]
        }
    }
    val templateLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/png")) { uri: Uri? ->
        val kind = templateKind ?: return@rememberLauncherForActivityResult
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            val font = DiceFonts.font(context.assets, skin.font)
            val style = numberStyle.copy(fontFamily = font?.family ?: numberStyle.fontFamily, fontWeight = font?.weight ?: numberStyle.fontWeight)
            val saved = viewModel.exportTemplate(kind, style, fonts, uri)
            message = strings[if (saved) "dice_workshop_template_saved" else "dice_workshop_image_failed"]
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background.radialEnd)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
            .systemBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                text = text("dice_workshop_title"),
                modifier = Modifier.padding(start = 20.dp, end = 64.dp, top = 16.dp),
                style = MaterialTheme.typography.headlineMedium,
                color = colors.text.primary
            )
            WorkshopPreview(look = look, kinds = PreviewDice.first { previewKind in it.first }.first)
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)
            ) {
                PreviewDice.forEach { (kinds, label) ->
                    ToggleChip(label = label, selected = previewKind in kinds, onClick = { previewKind = kinds.last() })
                }
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = skin.name,
                    onValueChange = { skin = skin.copy(name = it.take(40)) },
                    label = { Text(text("dice_workshop_name")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                // A new look by DiceSkinGenerator's rules; the name and the face pictures stay.
                FlowRow {
                    ActionButton(text("dice_workshop_random"), Icons.Outlined.Casino) {
                        skin = DiceSkinGenerator.generate(skin)
                    }
                    // The skin as it is now, pictures and all, in a file to send a friend.
                    ActionButton(text("dice_workshop_share"), Icons.Outlined.Share) {
                        val fileName = skin.name.trim().ifBlank { strings["dice_workshop_default_name"] }.replace(Regex("[\\\\/:*?\"<>|]"), "_")
                        shareLauncher.launch("$fileName.diceskin")
                    }
                }

                WorkshopSection(text("dice_workshop_section_faces")) {
                    ColorRow(text("dice_workshop_color"), Color(skin.bodyColor)) { picking = SkinColor.BODY }
                    // See-through: 0% is a solid die.
                    SliderRow(text("dice_workshop_transparency"), 1f - skin.bodyOpacity, 0f..0.7f, percent(1f - skin.bodyOpacity)) {
                        skin = skin.copy(bodyOpacity = 1f - it)
                    }
                    SliderRow(text("dice_workshop_gloss"), skin.gloss, 0f..1f, percent(skin.gloss)) { skin = skin.copy(gloss = it) }
                }

                WorkshopSection(text("dice_workshop_section_edges")) {
                    ColorRow(text("dice_workshop_color"), Color(skin.edgeColor)) { picking = SkinColor.EDGE }
                    SliderRow(
                        text("dice_workshop_edge_width"),
                        skin.edgeWidth,
                        0f..3f,
                        if (skin.edgeWidth < 0.05f) text("common_none") else "%.1f".format(skin.edgeWidth)
                    ) { skin = skin.copy(edgeWidth = if (it < 0.05f) 0f else it) }
                }

                WorkshopSection(text("dice_workshop_section_numbers")) {
                    ColorRow(text("dice_workshop_color"), Color(skin.numberColor)) { picking = SkinColor.NUMBER }
                    SwitchRow(text("dice_workshop_number_outline"), skin.numberOutlineColor != null) { on ->
                        skin = skin.copy(numberOutlineColor = if (on) (skin.numberOutlineColor ?: skin.bodyColor) else null)
                    }
                    skin.numberOutlineColor?.let { outline ->
                        ColorRow(text("dice_workshop_number_outline_color"), Color(outline)) { picking = SkinColor.NUMBER_OUTLINE }
                    }
                    Text(text("dice_workshop_font"), style = MaterialTheme.typography.bodyMedium, color = colors.text.muted)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        DiceFontIds.all.forEach { font ->
                            FontSample(font, selected = font == skin.font) { skin = skin.copy(font = font) }
                        }
                    }
                    SliderRow(text("dice_workshop_number_size"), skin.numberScale, 0.7f..1.3f, percent(skin.numberScale)) {
                        skin = skin.copy(numberScale = it)
                    }
                }

                WorkshopSection(text("dice_workshop_section_pattern")) {
                    val pattern = skin.pattern
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            "dice_workshop_pattern_none" to DicePattern.None,
                            "dice_workshop_pattern_web" to ((pattern as? DicePattern.Web) ?: DicePattern.Web(skin.edgeColor)),
                            "dice_workshop_pattern_marble" to ((pattern as? DicePattern.Marble) ?: DicePattern.Marble(skin.numberColor, Random.nextInt())),
                            "dice_workshop_pattern_nebula" to ((pattern as? DicePattern.Nebula) ?: DicePattern.Nebula(skin.edgeColor, skin.numberColor, Random.nextInt())),
                            "dice_workshop_pattern_picture" to ((pattern as? DicePattern.Picture) ?: DicePattern.Picture())
                        ).forEach { (labelKey, option) ->
                            ToggleChip(
                                label = text(labelKey),
                                selected = option::class == pattern::class,
                                onClick = {
                                    if (option is DicePattern.Picture && draftPicture == null && look.picture == null) pictureLauncher.launch(arrayOf("image/*"))
                                    skin = skin.copy(pattern = option)
                                }
                            )
                        }
                    }
                    when (pattern) {
                        DicePattern.None -> Unit
                        is DicePattern.Web -> ColorRow(text("dice_workshop_pattern_threads"), Color(pattern.color)) { picking = SkinColor.PATTERN }
                        is DicePattern.Marble -> {
                            ColorRow(text("dice_workshop_pattern_veins"), Color(pattern.color)) { picking = SkinColor.PATTERN }
                            ActionButton(text("dice_workshop_shuffle"), Icons.Outlined.Shuffle) {
                                skin = skin.copy(pattern = pattern.copy(seed = Random.nextInt()))
                            }
                        }
                        is DicePattern.Nebula -> {
                            ColorRow(text("dice_workshop_pattern_clouds"), Color(pattern.color)) { picking = SkinColor.PATTERN }
                            ColorRow(text("dice_workshop_pattern_glow"), Color(pattern.glow)) { picking = SkinColor.PATTERN_GLOW }
                            ActionButton(text("dice_workshop_shuffle"), Icons.Outlined.Shuffle) {
                                skin = skin.copy(pattern = pattern.copy(seed = Random.nextInt()))
                            }
                        }
                        is DicePattern.Picture -> {
                            Text(text("dice_workshop_picture_hint"), style = MaterialTheme.typography.bodyMedium, color = colors.text.muted)
                            ActionButton(text("dice_workshop_picture_choose"), Icons.Outlined.Image) { pictureLauncher.launch(arrayOf("image/*")) }
                            SliderRow(text("dice_workshop_picture_scale"), pattern.scale, 0.5f..3f, "×%.1f".format(pattern.scale)) {
                                skin = skin.copy(pattern = pattern.copy(scale = it))
                            }
                            SliderRow(text("dice_workshop_picture_rotation"), pattern.rotation, 0f..360f, "${pattern.rotation.toInt()}°") {
                                skin = skin.copy(pattern = pattern.copy(rotation = it))
                            }
                            SliderRow(text("dice_workshop_picture_strength"), pattern.strength, 0f..1f, percent(pattern.strength)) {
                                skin = skin.copy(pattern = pattern.copy(strength = it))
                            }
                        }
                    }
                }

                WorkshopSection(text("dice_workshop_section_face_art")) {
                    Text(text("dice_workshop_face_art_hint"), style = MaterialTheme.typography.bodyMedium, color = colors.text.muted)
                    FaceArtKinds.forEach { (kind, label) ->
                        val loaded = look.faceArt.containsKey(kind)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { previewKind = kind }
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(label, modifier = Modifier.width(72.dp), style = MaterialTheme.typography.titleMedium, color = colors.text.primary)
                            Text(
                                text = text(if (loaded) "dice_workshop_face_art_loaded" else "dice_workshop_face_art_none"),
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (loaded) colors.accent.heal else colors.text.subtle,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            IconButton(onClick = {
                                templateKind = kind
                                previewKind = kind
                                templateLauncher.launch("dice_template_${label.replace(" ×", "x").replace(" ", "")}.png")
                            }) {
                                Icon(Icons.Outlined.Download, contentDescription = text("dice_workshop_template_save"), tint = colors.text.label)
                            }
                            IconButton(onClick = {
                                faceArtKind = kind
                                faceArtLauncher.launch(arrayOf("image/*"))
                            }) {
                                Icon(Icons.Outlined.Upload, contentDescription = text("dice_workshop_face_art_load"), tint = colors.text.label)
                            }
                            if (loaded) {
                                IconButton(onClick = {
                                    draftFaceArt = draftFaceArt + (kind.name to null)
                                    skin = skin.copy(updatedAt = System.nanoTime())
                                }) {
                                    Icon(Icons.Outlined.Delete, contentDescription = text("common_delete"), tint = colors.text.muted)
                                }
                            }
                        }
                    }
                    SwitchRow(text("dice_workshop_numbers_over_art"), skin.numbersOverArt) { skin = skin.copy(numbersOverArt = it) }
                    ActionButton(text("dice_workshop_ai_prompt"), Icons.Outlined.ContentCopy) {
                        val label = FaceArtKinds.first { it.first == previewKind }.second
                        clipboard.setText(AnnotatedString(strings.format("dice_workshop_ai_prompt_text", label)))
                        message = strings["dice_workshop_ai_prompt_copied"]
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            message?.let {
                Text(
                    text = it,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.accent.inspiration
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!isNew) {
                    FilledTonalIconButton(
                        onClick = { confirmDelete = true },
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = LocalDesignTokens.current.colors.accent.dangerHpZero.copy(alpha = LocalDesignTokens.current.alpha.faint),
                            contentColor = LocalDesignTokens.current.colors.accent.dangerHpZero
                        )
                    ) {
                        Icon(Icons.Outlined.Delete, contentDescription = text("common_delete"))
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
                Button(
                    onClick = {
                        viewModel.save(skin.copy(name = skin.name.trim().ifBlank { strings["dice_workshop_default_name"] }), draftPicture, draftFaceArt)
                        onClose()
                    }
                ) {
                    Text(text("common_save"))
                }
            }
        }
        OverlayCloseButton(onClick = ::close, modifier = Modifier.align(Alignment.TopEnd))
    }

    picking?.let { target ->
        val (title, current) = when (target) {
            SkinColor.BODY -> text("dice_workshop_section_faces") to skin.bodyColor
            SkinColor.EDGE -> text("dice_workshop_section_edges") to skin.edgeColor
            SkinColor.NUMBER -> text("dice_workshop_section_numbers") to skin.numberColor
            SkinColor.NUMBER_OUTLINE -> text("dice_workshop_number_outline_color") to (skin.numberOutlineColor ?: skin.bodyColor)
            SkinColor.PATTERN -> text("dice_workshop_section_pattern") to patternColor(skin.pattern)
            SkinColor.PATTERN_GLOW -> text("dice_workshop_pattern_glow") to ((skin.pattern as? DicePattern.Nebula)?.glow ?: skin.numberColor)
        }
        DiceColorPickerDialog(title = title, initial = Color(current), onDismiss = { picking = null }) { picked ->
            val argb = picked.toArgb()
            skin = when (target) {
                SkinColor.BODY -> skin.copy(bodyColor = argb)
                SkinColor.EDGE -> skin.copy(edgeColor = argb)
                SkinColor.NUMBER -> skin.copy(numberColor = argb)
                SkinColor.NUMBER_OUTLINE -> skin.copy(numberOutlineColor = argb)
                SkinColor.PATTERN -> skin.copy(pattern = when (val pattern = skin.pattern) {
                    is DicePattern.Web -> pattern.copy(color = argb)
                    is DicePattern.Marble -> pattern.copy(color = argb)
                    is DicePattern.Nebula -> pattern.copy(color = argb)
                    else -> pattern
                })
                SkinColor.PATTERN_GLOW -> skin.copy(pattern = (skin.pattern as? DicePattern.Nebula)?.copy(glow = argb) ?: skin.pattern)
            }
            picking = null
        }
    }

    if (confirmDelete) {
        com.dndcharacterhandler.presentation.components.EditDialog(
            title = text("dice_workshop_delete_title"),
            onDismiss = { confirmDelete = false },
            onConfirm = {
                viewModel.delete(initial)
                viewModel.discardDraft()
                confirmDelete = false
                onClose()
            },
            confirmLabel = text("common_delete"),
            confirmIsDanger = true
        ) {
            Text(strings.format("dice_workshop_delete_text", initial.name), color = colors.text.primary)
        }
    }
}

private fun patternColor(pattern: DicePattern): Int = when (pattern) {
    is DicePattern.Web -> pattern.color
    is DicePattern.Marble -> pattern.color
    is DicePattern.Nebula -> pattern.color
    else -> 0xFFFFFFFF.toInt()
}

private fun percent(value: Float): String = "${(value * 100).toInt()}%"

/** The skin on [kinds] turning slowly side by side (the d100's two dice), so every side of them shows. */
@Composable
private fun WorkshopPreview(look: DiceLook, kinds: List<DieShapeKind>) {
    var angle by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        var previous = 0L
        while (true) {
            withFrameNanos { now ->
                if (previous != 0L) angle += ((now - previous) / 1_000_000_000.0 * 0.6).toFloat()
                previous = now
            }
        }
    }
    val turn = Quat.axisAngle(Vec3(0.35, 1.0, 0.2).normalized(), angle.toDouble())
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
    ) {
        kinds.forEach { kind ->
            DieIcon(
                type = DieType.D20,
                look = look,
                kind = kind,
                turn = turn,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
        }
    }
}

@Composable
private fun WorkshopSection(title: String, content: @Composable () -> Unit) {
    val colors = LocalDesignTokens.current.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surface.card.copy(alpha = LocalDesignTokens.current.alpha.veil))
            .border(1.dp, colors.border.muted, RoundedCornerShape(12.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = colors.text.primary)
        content()
    }
}

@Composable
private fun ColorRow(label: String, color: Color, onClick: () -> Unit) {
    val colors = LocalDesignTokens.current.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, color = colors.text.muted)
        Box(
            modifier = Modifier
                .size(width = 56.dp, height = 30.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(color)
                .border(1.dp, colors.border.selected, RoundedCornerShape(8.dp))
        )
    }
}

@Composable
private fun SliderRow(label: String, value: Float, range: ClosedFloatingPointRange<Float>, shown: String, onChange: (Float) -> Unit) {
    val colors = LocalDesignTokens.current.colors
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, color = colors.text.muted)
            Text(shown, style = MaterialTheme.typography.bodyMedium, color = colors.text.primary)
        }
        Slider(
            value = value.coerceIn(range),
            onValueChange = onChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = colors.border.muted
            )
        )
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    val colors = LocalDesignTokens.current.colors
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, color = colors.text.muted)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun ActionButton(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    TextButton(onClick = onClick) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(label)
    }
}

/** A font as a chip showing "20" printed in it. */
@Composable
private fun FontSample(font: String, selected: Boolean, onClick: () -> Unit) {
    val colors = LocalDesignTokens.current.colors
    val context = LocalContext.current
    val diceFont = remember(font) { DiceFonts.font(context.assets, font) }
    val style = MaterialTheme.typography.headlineMedium
    val shape = RoundedCornerShape(10.dp)
    Column(
        modifier = Modifier
            .clip(shape)
            .background(toggleFill(selected))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "20",
            modifier = Modifier.width(64.dp),
            style = style.copy(fontFamily = diceFont?.family ?: style.fontFamily, fontWeight = diceFont?.weight ?: style.fontWeight),
            color = toggleContent(selected),
            textAlign = TextAlign.Center,
            maxLines = 1
        )
        Text(
            text = DiceFonts.label(font) ?: text("dice_font_app"),
            style = MaterialTheme.typography.labelMedium,
            color = toggleContent(selected, colors.text.muted),
            maxLines = 1
        )
    }
}
