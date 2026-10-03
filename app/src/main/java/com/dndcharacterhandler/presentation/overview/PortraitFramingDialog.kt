package com.dndcharacterhandler.presentation.overview

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import com.dndcharacterhandler.domain.model.PortraitFraming
import com.dndcharacterhandler.presentation.components.EditDialog
import com.dndcharacterhandler.presentation.components.rememberAppImagePainter
import com.dndcharacterhandler.presentation.localization.text
import com.dndcharacterhandler.presentation.theme.LocalDesignTokens
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** A regular octagon round [center], [radius] to its corners, its sides flat at the top, bottom and sides. */
internal fun octagonPath(center: Offset, radius: Float): Path = Path().apply {
    for (i in 0 until 8) {
        val angle = (PI / 8 + i * PI / 4).toFloat()
        val x = center.x + radius * cos(angle)
        val y = center.y + radius * sin(angle)
        if (i == 0) moveTo(x, y) else lineTo(x, y)
    }
    close()
}

/** The portrait's octagon, filling whatever it clips. */
internal val OctagonShape = GenericShape { size, _ ->
    addPath(octagonPath(Offset(size.width / 2f, size.height / 2f), size.minDimension / 2f))
}

/** How much of the editor the octagon's window takes: the rest shows what stays outside, dimmed. */
private const val WindowShare = 0.72f

/** The picture outside the window: its own colours at a third of their light. */
private val Dimmed = ColorFilter.colorMatrix(ColorMatrix().apply { setToScale(0.35f, 0.35f, 0.35f, 1f) })

/**
 * Choosing which part of the portrait shows in its octagon, as photo apps let you: the whole
 * picture, the part outside the frame dimmed; a drag moves it, two fingers (or the slider) bring it
 * closer. Saved as a [PortraitFraming], so the drawer's small portrait shows the same part.
 */
@Composable
internal fun PortraitFramingDialog(
    portraitReference: String,
    initial: PortraitFraming,
    onSave: (PortraitFraming) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalDesignTokens.current.colors
    var draft by remember { mutableStateOf(initial) }
    val painter = rememberAppImagePainter(portraitReference)
    val image = painter?.intrinsicSize?.takeIf { it.isSpecified && it.width > 0f && it.height > 0f }
    var window by remember { mutableStateOf(0f) }
    EditDialog(
        title = text("overview_portrait_frame"),
        onDismiss = onDismiss,
        onConfirm = { onSave(draft) },
        scrollable = false
    ) {
        Text(
            text = text("overview_portrait_frame_hint"),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.text.muted
        )
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clipToBounds()
                .onSizeChanged { window = it.width * WindowShare }
                .pointerInput(image, window) {
                    if (image == null) return@pointerInput
                    detectTransformGestures { _, pan, zoomChange, _ ->
                        draft = draft
                            .zoomedTo(draft.zoom * zoomChange, image.width, image.height, window, window)
                            .panned(pan.x, pan.y, image.width, image.height, window, window)
                    }
                }
        ) {
            if (painter == null || image == null) return@Canvas
            val origin = Offset((size.width - window) / 2f, (size.height - window) / 2f)
            val placed = draft.placement(image.width, image.height, window, window)
            val pictureSize = Size(placed.width, placed.height)
            // The whole picture dimmed, then what the frame shows at full light.
            translate(origin.x + placed.left, origin.y + placed.top) {
                with(painter) { draw(pictureSize, colorFilter = Dimmed) }
            }
            val frame = octagonPath(center, window / 2f)
            clipPath(frame) {
                translate(origin.x + placed.left, origin.y + placed.top) {
                    with(painter) { draw(pictureSize) }
                }
            }
            // The portrait frame's own contours round the window.
            drawPath(octagonPath(center, window / 2f * 1.05f), colors.ornament.inner, style = Stroke(width = 1.dp.toPx()))
            drawPath(octagonPath(center, window / 2f * 1.12f), colors.ornament.middle, style = Stroke(width = 3.dp.toPx()))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = text("overview_portrait_zoom"),
                style = MaterialTheme.typography.labelMedium,
                color = colors.text.label,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = { draft = PortraitFraming() }) {
                Text(text("overview_portrait_frame_reset"))
            }
        }
        Slider(
            value = draft.zoom,
            onValueChange = { zoom ->
                draft = if (image != null && window > 0f) draft.zoomedTo(zoom, image.width, image.height, window, window) else draft.copy(zoom = zoom)
            },
            valueRange = 1f..PortraitFraming.MAX_ZOOM,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = colors.border.muted
            )
        )
    }
}
