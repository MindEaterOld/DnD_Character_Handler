package com.dndcharacterhandler.presentation.overview

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ZoomIn
import androidx.compose.material.icons.outlined.ZoomOut
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
import com.dndcharacterhandler.presentation.components.dialogLeadText
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
 * fingers (or the slider between the zoom's icons) bring it closer. Saved as a [PortraitFraming], so the drawer's small
 * portrait shows the same part.
 */
@OptIn(ExperimentalMaterial3Api::class)
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
            modifier = Modifier.dialogLeadText(),
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
            drawRect(colors.background.page, topLeft = frame.topLeft, size = frame.size)
            drawIntoCanvas { it.saveLayer(frame, Paint()) }
            clipRect(frame.left, frame.top, frame.right, frame.bottom) {
                translate(frame.left + placed.left, frame.top + placed.top) {
                    with(painter) { draw(pictureSize) }
                }
            }
            portraitVignette(frame, frame.height * footShare, colors.background.page)
            drawIntoCanvas { it.restore() }
            drawRect(colors.border.faint, topLeft = frame.topLeft, size = frame.size, style = Stroke(width = 1.dp.toPx()))
        }
        // The zoom (owner's choice from boards, 2026-10-13: В, with no reset): a thin slider in the style of the app's bars
        // between the zoom's icons; a tap on an icon steps it. No label, no reset — two fingers do it too.
        fun zoomTo(zoom: Float) {
            val picture = image
            val target = zoom.coerceIn(1f, PortraitFraming.MAX_ZOOM)
            draft = if (picture != null && window.width > 0f) {
                draft.zoomedTo(target, picture.width, picture.height, window.width, window.height)
            } else {
                draft.copy(zoom = target)
            }
        }
        val gold = MaterialTheme.colorScheme.primary
        val zoomLabel = text("overview_portrait_zoom")
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { zoomTo(draft.zoom - ZoomStep) }) {
                Icon(Icons.Outlined.ZoomOut, contentDescription = null, tint = colors.text.muted)
            }
            Slider(
                value = draft.zoom,
                onValueChange = ::zoomTo,
                valueRange = 1f..PortraitFraming.MAX_ZOOM,
                modifier = Modifier
                    .weight(1f)
                    .semantics { contentDescription = zoomLabel },
                thumb = {
                    Box(
                        modifier = Modifier
                            .size(ZoomThumb)
                            .clip(CircleShape)
                            .background(gold)
                    )
                },
                track = { state ->
                    val share = (state.value - state.valueRange.start) / (state.valueRange.endInclusive - state.valueRange.start)
                    Canvas(modifier = Modifier.fillMaxWidth().height(ZoomTrack)) {
                        val corner = CornerRadius(size.height / 2)
                        drawRoundRect(colors.border.faint, size = size, cornerRadius = corner)
                        drawRoundRect(gold, size = Size(size.width * share, size.height), cornerRadius = corner)
                    }
                }
            )
            IconButton(onClick = { zoomTo(draft.zoom + ZoomStep) }) {
                Icon(Icons.Outlined.ZoomIn, contentDescription = null, tint = colors.text.muted)
            }
        }
    }
}

/** The zoom's track as the app's bars (the hit points', the experience's): 4dp, its thumb a gold dot. */
private val ZoomTrack = 4.dp
private val ZoomThumb = 18.dp

/** What a tap on a zoom icon adds or takes. */
private const val ZoomStep = 0.25f
