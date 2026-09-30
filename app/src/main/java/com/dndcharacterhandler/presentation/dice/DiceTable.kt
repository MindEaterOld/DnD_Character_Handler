package com.dndcharacterhandler.presentation.dice

import android.graphics.Matrix
import android.graphics.Paint
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.resolveAsTypeface
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.dndcharacterhandler.presentation.components.OverlayCloseButton
import com.dndcharacterhandler.presentation.localization.LocalStrings
import com.dndcharacterhandler.presentation.localization.text
import com.dndcharacterhandler.presentation.theme.LocalDesignTokens
import kotlinx.coroutines.flow.collectLatest
import kotlin.math.max
import kotlin.random.Random

internal enum class DicePhase { READY, HOLDING, ROLLING, SETTLED }

/** State of the dice table: the physics world, the camera and what the player is doing. */
internal class DiceTableState(private val selection: Map<DieType, Int>, seed: Long = System.nanoTime()) {
    val world = DiceWorld(Random(seed))
    var phase by mutableStateOf(DicePhase.READY)
        private set
    var results by mutableStateOf<List<ThrownDie>>(emptyList())
        private set

    /** The dice are in hand or rolling, so the simulation needs every frame. */
    val isAnimating: Boolean get() = phase == DicePhase.HOLDING || phase == DicePhase.ROLLING

    /** Bumped every simulated frame so the canvas redraws. */
    var frame by mutableLongStateOf(0L)
        private set

    private var dice: List<ThrownDie> = emptyList()
    private var widthPx = 1f
    private var heightPx = 1f
    var focalLength = 1.0
        private set

    fun setViewport(width: Int, height: Int) {
        if (width <= 0 || height <= 0) return
        widthPx = width.toFloat()
        heightPx = height.toFloat()
        world.setAspectRatio(heightPx.toDouble() / widthPx)
        // The table's edges (at floor level) line up exactly with the screen's edges.
        focalLength = (widthPx / 2) * world.cameraHeight / world.halfWidth
        frame++
    }

    /** Screen point -> world point at height [y] under a top-down perspective camera. */
    private fun unproject(offset: Offset, y: Double): Vec3 {
        val depth = (world.cameraHeight - y) / focalLength
        return Vec3((offset.x - widthPx / 2) * depth, y, (offset.y - heightPx / 2) * depth)
    }

    fun project(point: Vec3): Offset {
        val scale = focalLength / (world.cameraHeight - point.y)
        return Offset((widthPx / 2 + point.x * scale).toFloat(), (heightPx / 2 + point.z * scale).toFloat())
    }

    fun press(offset: Offset) {
        val hand = unproject(offset, world.handHeight)
        if (dice.isEmpty()) {
            dice = world.spawnSelection(selection, hand)
        } else {
            world.grab(hand)
        }
        results = emptyList()
        phase = DicePhase.HOLDING
    }

    fun drag(offset: Offset) {
        world.moveHand(unproject(offset, world.handHeight))
    }

    fun release(velocity: Velocity) {
        if (phase != DicePhase.HOLDING) return
        val depth = (world.cameraHeight - world.handHeight) / focalLength
        world.release(Vec3(velocity.x * depth, 0.0, velocity.y * depth))
        phase = DicePhase.ROLLING
    }

    fun tick(seconds: Double) {
        if (!isAnimating) return
        world.advance(seconds)
        frame++
        if (phase == DicePhase.ROLLING && world.allSettled) {
            results = dice
            phase = DicePhase.SETTLED
        }
    }
}

/**
 * Full-screen dice table: press and hold to take the dice in hand, drag to shake them (they follow
 * the finger with some lag and knock into each other), let go to throw. The screen edges are walls.
 * The face that lands on top is the result.
 */
@Composable
internal fun DiceTableOverlay(selection: Map<DieType, Int>, onClose: () -> Unit) {
    val state = remember(selection) { DiceTableState(selection) }
    val colors = LocalDesignTokens.current.colors
    BackHandler(onBack = onClose)

    LaunchedEffect(state) {
        // Frames are only requested while the dice move; between throws the table sits idle.
        snapshotFlow { state.isAnimating }.collectLatest { animating ->
            if (!animating) return@collectLatest
            var previous = 0L
            while (true) {
                withFrameNanos { now ->
                    if (previous != 0L) state.tick((now - previous) / 1_000_000_000.0)
                    previous = now
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background.radialEnd.copy(alpha = 0.6f))
    ) {
        DiceCanvas(
            state = state,
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { state.setViewport(it.width, it.height) }
                .pointerInput(state) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        down.consume()
                        val tracker = VelocityTracker()
                        tracker.addPosition(down.uptimeMillis, down.position)
                        state.press(down.position)
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) break
                            tracker.addPosition(change.uptimeMillis, change.position)
                            state.drag(change.position)
                            change.consume()
                        }
                        state.release(tracker.calculateVelocity())
                    }
                }
        )

        DiceStatusPanel(
            state = state,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .systemBarsPadding()
                .padding(start = 72.dp, end = 72.dp, top = 16.dp)
        )

        OverlayCloseButton(onClick = onClose, modifier = Modifier.align(Alignment.TopEnd))
    }
}

@Composable
private fun DiceStatusPanel(state: DiceTableState, modifier: Modifier = Modifier) {
    val colors = LocalDesignTokens.current.colors
    val strings = LocalStrings.current
    val phase = state.phase
    if (phase == DicePhase.HOLDING || phase == DicePhase.ROLLING) return
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(colors.surface.card.copy(alpha = 0.88f))
            .border(1.dp, colors.border.muted, RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (phase == DicePhase.SETTLED) {
            val results = state.results
            Text(
                text = strings.format("dice_result_total", results.sumOf { it.value() }),
                style = MaterialTheme.typography.headlineMedium,
                color = colors.text.primary,
                textAlign = TextAlign.Center
            )
            results.groupBy { it.type }.forEach { (type, thrown) ->
                Text(
                    text = "${thrown.size}${type.label}: ${thrown.joinToString(" + ") { it.value().toString() }}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.text.muted,
                    textAlign = TextAlign.Center
                )
            }
        }
        Text(
            text = text(if (phase == DicePhase.READY) "dice_hint_grab" else "dice_hint_regrab"),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.text.label,
            textAlign = TextAlign.Center
        )
    }
}

/** Light direction for the flat shading: from the upper left of the screen. */
private val LightDirection = Vec3(-0.35, 1.0, -0.45).normalized()

@Composable
private fun DiceCanvas(state: DiceTableState, modifier: Modifier = Modifier) {
    val colors = LocalDesignTokens.current.colors
    val bodyColor = colors.accent.inspiration
    val numberColor = colors.surface.card
    val shadowColor = colors.background.radialEnd
    // Numbers in the app's headline face (the serif of the type scale).
    val numberStyle = MaterialTheme.typography.headlineMedium
    val fontFamilyResolver = LocalFontFamilyResolver.current
    val typeface by remember(numberStyle, fontFamilyResolver) {
        fontFamilyResolver.resolveAsTypeface(numberStyle.fontFamily, numberStyle.fontWeight ?: FontWeight.Normal)
    }
    val paint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }
    }
    val scratch = remember { DieDrawScratch() }

    Canvas(modifier = modifier) {
        // Reading the frame counter subscribes this draw to every simulation step.
        state.frame
        paint.color = numberColor.toArgb()
        paint.typeface = typeface
        val bodies = state.world.bodies.sortedBy { it.position.y }
        bodies.forEach { drawShadow(state, it, shadowColor) }
        bodies.forEach { drawDie(state, it, bodyColor, paint, scratch) }
    }
}

private fun DrawScope.drawShadow(state: DiceTableState, body: DieBody, shadowColor: Color) {
    val center = state.project(Vec3(body.position.x, 0.0, body.position.z))
    val height = body.position.y
    val radius = (body.shape.circumradius * state.focalLength / state.world.cameraHeight * (0.85 + 0.05 * height)).toFloat()
    drawOval(
        color = shadowColor.copy(alpha = (0.5f - 0.04f * height.toFloat()).coerceIn(0.12f, 0.5f)),
        topLeft = Offset(center.x - radius, center.y - radius * 0.8f),
        size = Size(radius * 2, radius * 1.6f)
    )
}

/** Objects reused for every die drawn, so a frame doesn't allocate them per face. */
private class DieDrawScratch {
    val matrix = Matrix()
    val path = Path()
    /** Projected corners of the die being drawn. */
    var projected = FloatArray(0)
    /** Projected corners of one face, in the order [DieFace.canonical] lists them. */
    val destination = FloatArray(8)
}

private fun DrawScope.drawDie(
    state: DiceTableState,
    body: DieBody,
    bodyColor: Color,
    paint: Paint,
    scratch: DieDrawScratch
) {
    val shape = body.shape
    val eye = Vec3(0.0, state.world.cameraHeight, 0.0)
    if (scratch.projected.size < shape.vertices.size * 2) scratch.projected = FloatArray(shape.vertices.size * 2)
    val projected = scratch.projected
    for (index in shape.vertices.indices) {
        val point = state.project(body.worldVertex(index))
        projected[2 * index] = point.x
        projected[2 * index + 1] = point.y
    }
    val path = scratch.path
    val matrix = scratch.matrix
    val destination = scratch.destination
    paint.textSize = shape.labelSize

    for (face in shape.faces) {
        val normal = body.orientation.rotate(face.normal)
        val center = body.position + body.orientation.rotate(face.center)
        val toEye = eye - center
        val facing = toEye.normalized() dot normal
        if (facing <= 0) continue

        val brightness = (0.38 + 0.62 * max(0.0, normal dot LightDirection)).toFloat()
        path.reset()
        val first = face.vertexIndices[0]
        path.moveTo(projected[2 * first], projected[2 * first + 1])
        for (i in 1 until face.vertexIndices.size) {
            val vertex = face.vertexIndices[i]
            path.lineTo(projected[2 * vertex], projected[2 * vertex + 1])
        }
        path.close()
        drawPath(path, color = bodyColor.shaded(brightness))
        drawPath(path, color = bodyColor.shaded(brightness * 0.55f), style = Stroke(width = 1.5f))

        // Numbers at grazing angles would be squashed into slivers; skip them.
        if (facing < 0.2) continue
        val pointCount = face.canonical.size / 2
        for (i in 0 until pointCount) {
            val vertex = face.vertexIndices[i]
            destination[2 * i] = projected[2 * vertex]
            destination[2 * i + 1] = projected[2 * vertex + 1]
        }
        if (!matrix.setPolyToPoly(face.canonical, 0, destination, 0, pointCount)) continue
        drawIntoCanvas { canvas ->
            val native = canvas.nativeCanvas
            native.save()
            native.concat(matrix)
            for (label in face.labels) {
                native.save()
                native.translate(label.offsetX, label.offsetY)
                native.rotate(label.rotationDegrees)
                paint.isUnderlineText = label.underline
                native.drawText(label.text, 0f, paint.textSize * 0.36f, paint)
                native.restore()
            }
            native.restore()
        }
    }
}

/** The same colour lit at [brightness] (1 = as defined); used for flat shading of die faces. */
private fun Color.shaded(brightness: Float): Color =
    Color(
        red = (red * brightness).coerceIn(0f, 1f),
        green = (green * brightness).coerceIn(0f, 1f),
        blue = (blue * brightness).coerceIn(0f, 1f),
        alpha = alpha
    )
