package com.dndcharacterhandler.presentation.dice

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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.dndcharacterhandler.presentation.components.OverlayCloseButton
import com.dndcharacterhandler.presentation.localization.LocalStrings
import com.dndcharacterhandler.presentation.localization.text
import com.dndcharacterhandler.presentation.theme.LocalDesignTokens
import kotlinx.coroutines.flow.collectLatest
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
    val camera = DiceCamera(eyeHeight = world.cameraHeight)

    fun setViewport(width: Int, height: Int) {
        if (width <= 0 || height <= 0) return
        world.setAspectRatio(height.toDouble() / width)
        // The table's edges (at floor level) line up exactly with the screen's edges.
        camera.setViewport(
            width = width.toFloat(),
            height = height.toFloat(),
            focalLength = (width / 2.0) * world.cameraHeight / world.halfWidth
        )
        frame++
    }

    fun press(offset: Offset) {
        val hand = camera.unproject(offset, world.handHeight)
        if (dice.isEmpty()) {
            dice = world.spawnSelection(selection, hand)
        } else {
            world.grab(hand)
        }
        results = emptyList()
        phase = DicePhase.HOLDING
    }

    fun drag(offset: Offset) {
        world.moveHand(camera.unproject(offset, world.handHeight))
    }

    fun release(velocity: Velocity) {
        if (phase != DicePhase.HOLDING) return
        val depth = (world.cameraHeight - world.handHeight) / camera.focalLength
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
internal fun DiceTableOverlay(selection: Map<DieType, Int>, skin: DiceSkin, onClose: () -> Unit) {
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
            skin = skin,
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

@Composable
private fun DiceCanvas(state: DiceTableState, skin: DiceSkin, modifier: Modifier = Modifier) {
    val skinColors = skin.colors()
    val shadowColor = LocalDesignTokens.current.colors.background.radialEnd
    val numbers = rememberDieNumberPaint()
    val scratch = remember { DieDrawScratch() }

    Canvas(modifier = modifier) {
        // Reading the frame counter subscribes this draw to every simulation step.
        state.frame
        val numberPaint = numbers.get()
        val bodies = state.world.bodies.sortedBy { it.position.y }
        bodies.forEach { drawDieShadow(state.camera, it, shadowColor) }
        bodies.forEach { drawDie(state.camera, it, skinColors, numberPaint, scratch) }
    }
}
