package com.dndcharacterhandler.presentation.attributes

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.dndcharacterhandler.presentation.theme.LocalDesignTokens
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random
import kotlinx.coroutines.launch

/**
 * The spider hanging under Charisma's frame into the skills' spare room (owner's choice from boards,
 * 2026-10-04): a decoration in the colour of the frames' outline, 22dp in from the column's left edge. Its
 * feet are at the room's bottom, level with the left column's, and its thread runs up to the frame, as long
 * as the room makes it. A [Spacer]: it draws in the height it is given and asks for none, so the columns
 * stay level.
 *
 * A tap on it sends up a red heart from a random point around it, rising and fading out; every tap adds one,
 * so quick taps make a flurry (owner's wish, 2026-10-07).
 */
@Composable
internal fun SkillsSpider(modifier: Modifier = Modifier) {
    val painter = rememberVectorPainter(SkillsSpiderBody)
    val heartPainter = rememberVectorPainter(Icons.Filled.Favorite)
    val colors = LocalDesignTokens.current.colors
    val tint = colors.border.miniCard
    val heartTint = ColorFilter.tint(colors.accent.dangerHpZero)
    val hearts = remember { mutableStateListOf<SpiderHeart>() }
    val scope = rememberCoroutineScope()
    Spacer(
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures { tap ->
                    // Only a tap on the spider, not on the empty room around it.
                    if ((tap - spiderCenter(size.height.toFloat())).getDistance() > SpiderTapRadius.toPx()) {
                        return@detectTapGestures
                    }
                    val heart = SpiderHeart(
                        across = Random.nextFloat() * 2f - 1f,
                        up = Random.nextFloat(),
                        sway = Random.nextFloat() * 2f - 1f
                    )
                    hearts += heart
                    scope.launch {
                        heart.flight.animateTo(1f, tween(HeartFlightMillis, easing = LinearOutSlowInEasing))
                        hearts.remove(heart)
                    }
                }
            }
            .drawBehind {
                val unit = SpiderUnit.toPx()
                val bodyWidth = SkillsSpiderBody.viewportWidth * unit
                val bodyHeight = SkillsSpiderBody.viewportHeight * unit
                val left = SpiderLeft.toPx()
                val bodyTop = size.height - bodyHeight
                // The thread is two units wide over the body's middle and stops short of the legs, as in the icon.
                drawRect(
                    color = tint,
                    topLeft = Offset(left + 9.55f * unit, 0f),
                    size = Size(2f * unit, (bodyTop - 1.84f * unit).coerceAtLeast(0f))
                )
                translate(left, bodyTop) {
                    with(painter) { draw(Size(bodyWidth, bodyHeight), colorFilter = ColorFilter.tint(tint)) }
                }
                // The hearts: each pops up where it was born, rises with a little sway and fades out.
                val center = spiderCenter(size.height)
                hearts.forEach { heart ->
                    val flight = heart.flight.value
                    val x = center.x + heart.across * HeartSpreadAcross.toPx() +
                        heart.sway * HeartSway.toPx() * sin(flight * PI).toFloat()
                    val y = center.y + HeartBirthBelow.toPx() - heart.up * HeartSpreadUp.toPx() - HeartRise.toPx() * flight
                    val side = HeartSize.toPx() * (0.6f + 0.4f * (flight / 0.15f).coerceAtMost(1f))
                    val alpha = if (flight < 0.4f) 1f else 1f - (flight - 0.4f) / 0.6f
                    translate(x - side / 2, y - side / 2) {
                        with(heartPainter) { draw(Size(side, side), alpha = alpha, colorFilter = heartTint) }
                    }
                }
            }
    )
}

/** A heart in flight: where around the spider it was born (from −1 to 1 across, 0 to 1 up), its sway, its flight. */
private class SpiderHeart(val across: Float, val up: Float, val sway: Float) {
    val flight = Animatable(0f)
}

/** The middle of the spider's body in a room [roomHeight] tall. */
private fun androidx.compose.ui.unit.Density.spiderCenter(roomHeight: Float): Offset {
    val unit = SpiderUnit.toPx()
    return Offset(
        SpiderLeft.toPx() + SkillsSpiderBody.viewportWidth * unit / 2,
        roomHeight - SkillsSpiderBody.viewportHeight * unit / 2
    )
}

// The icon's own units at 1.3dp each: 26dp from the thread's top to the feet, as the icon draws it.
private val SpiderUnit = 1.3.dp
private val SpiderLeft = 22.dp
private val SpiderTapRadius = 32.dp
private val HeartSize = 18.dp
private val HeartSpreadAcross = 26.dp
private val HeartSpreadUp = 22.dp
private val HeartBirthBelow = 4.dp
private val HeartRise = 80.dp
private val HeartSway = 10.dp
private const val HeartFlightMillis = 1400

/**
 * The spider's body: the "spider-thread" of Material Design Icons by Pictogrammers (Apache 2.0) without its
 * thread, the viewport cut to the figure; [SkillsSpider] draws the thread to the length it needs.
 */
private val SkillsSpiderBody: ImageVector by lazy {
    ImageVector.Builder(
        name = "SkillsSpiderBody",
        defaultWidth = 21.1.dp,
        defaultHeight = 13.08.dp,
        viewportWidth = 21.1f,
        viewportHeight = 13.08f
    )
        .addGroup(translationX = -1.45f, translationY = -8.92f)
        .addPath(
            pathData = addPathNodes(
                "M16.9 15a5 5 0 0 1-.17.55L20 17.42V22h-2v-3.42l-2.26-1.29a4.94 4.94 0 0 1-7.48 0L6 18.58V22H4v-4.58l3.27-1.87A5 5 0 0 1 7.1 15H5.3" +
                    "l-2.75 1.83l-1.1-1.66L4.7 13h2.4a5 5 0 0 1 .27-.88l-1.56-1l-3.57.88l-.48-2l4.43-1.08l2.31 1.53a5 5 0 0 1 7 0l2.27-1.53L22.24 10" +
                    "l-.48 2l-3.57-.89l-1.56 1a5 5 0 0 1 .27.89h2.4l3.25 2.16l-1.1 1.66L18.7 15" +
                    "M11 14a1 1 0 1 0-1 1a1 1 0 0 0 1-1m4 0a1 1 0 1 0-1 1a1 1 0 0 0 1-1"
            ),
            fill = SolidColor(Color.Black)
        )
        .clearGroup()
        .build()
}
