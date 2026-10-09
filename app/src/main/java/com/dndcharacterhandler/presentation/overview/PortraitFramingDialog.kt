package com.dndcharacterhandler.presentation.overview

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import com.dndcharacterhandler.domain.model.PortraitFraming
import com.dndcharacterhandler.presentation.components.EditDialog
import com.dndcharacterhandler.presentation.components.rememberAppImagePainter
import com.dndcharacterhandler.presentation.localization.text
import com.dndcharacterhandler.presentation.theme.LocalDesignTokens

/** How much of the editor's width the window takes: the rest shows what stays outside it, dimmed. */
private const val WindowShare = 0.78f

/** The picture outside the window: its own colours at a third of their light. */
private val Dimmed = ColorFilter.colorMatrix(ColorMatrix().apply { setToScale(0.35f, 0.35f, 0.35f, 1f) })

/**
 * Choosing which part of the portrait shows on the overview, as photo apps let you: the window is the overview's
 * portrait itself — as wide to as tall ([heroAspect], width over height), its edges melting into the background as
 * there ([footShare] of its height at the foot) — and the picture outside it is dimmed; a drag moves the picture, two
 * fingers (or the slider) bring it closer. Saved as a [PortraitFraming], so the drawer's small portrait shows the same
 * part.
 */
@Composable
internal fun PortraitFramingDialog(
    portraitReference: String,
    initial: PortraitFraming,
    heroAspect: Float,
    footShare: Float,
    onSave: (PortraitFraming) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalDesignTokens.current.colors
    var draft by remember { mutableStateOf(initial) }
    val painter = rememberAppImagePainter(portraitReference)
    val image = painter?.intrinsicSize?.takeIf { it.isSpecified && it.width > 0f && it.height > 0f }
    var window by remember { mutableStateOf(Size.Zero) }
    // The gesture's loop runs once; it reads the picture's size and the window's as they are now.
    val currentImage by rememberUpdatedState(image)
    val currentWindow by rememberUpdatedState(window)
    // The canvas: the window with an even margin round it, the margin the same as at its sides.
    val canvasAspect = 1f / (WindowShare / heroAspect + (1f - WindowShare))
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
                .aspectRatio(canvasAspect)
                .clipToBounds()
                .onSizeChanged { canvas ->
                    val width = canvas.width * WindowShare
                    window = Size(width, width / heroAspect)
                }
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoomChange, _ ->
                        val picture = currentImage ?: return@detectTransformGestures
                        val frame = currentWindow
                        if (frame.width <= 0f) return@detectTransformGestures
                        draft = draft
                            .zoomedTo(draft.zoom * zoomChange, picture.width, picture.height, frame.width, frame.height)
                            .panned(pan.x, pan.y, picture.width, picture.height, frame.width, frame.height)
                    }
                }
        ) {
            val frameSize = window
            if (painter == null || image == null || frameSize.width <= 0f) return@Canvas
            val frame = Rect(Offset((size.width - frameSize.width) / 2f, (size.height - frameSize.height) / 2f), frameSize)
            val placed = draft.placement(image.width, image.height, frameSize.width, frameSize.height)
            val pictureSize = Size(placed.width, placed.height)
            // The whole picture dimmed.
            translate(frame.left + placed.left, frame.top + placed.top) {
                with(painter) { draw(pictureSize, colorFilter = Dimmed) }
            }
            // The window as the overview shows it: the background, the picture over it melting at its edges.
            drawRect(colors.background.radialEnd, topLeft = frame.topLeft, size = frame.size)
            drawIntoCanvas { it.saveLayer(frame, Paint()) }
            clipRect(frame.left, frame.top, frame.right, frame.bottom) {
                translate(frame.left + placed.left, frame.top + placed.top) {
                    with(painter) { draw(pictureSize) }
                }
            }
            portraitVignette(frame, frame.height * footShare, colors.surface.portrait)
            drawIntoCanvas { it.restore() }
            drawRect(colors.border.muted, topLeft = frame.topLeft, size = frame.size, style = Stroke(width = 1.dp.toPx()))
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
                val picture = image
                draft = if (picture != null && window.width > 0f) {
                    draft.zoomedTo(zoom, picture.width, picture.height, window.width, window.height)
                } else {
                    draft.copy(zoom = zoom)
                }
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
