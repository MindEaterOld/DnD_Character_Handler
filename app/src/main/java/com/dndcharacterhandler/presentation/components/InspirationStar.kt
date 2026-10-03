package com.dndcharacterhandler.presentation.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dndcharacterhandler.presentation.theme.LocalDesignTokens
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Inspiration on the portrait's frame: an eight-pointed compass rose — long points up, down and to
 * the sides, shorter ones between — with a dark outline and a dark heart. Off, its band is grey
 * ([band]); on, gold ([bandLit]), the heart turns white and a [glow] breathes round it (null: none).
 * The owner chose the form from a board (2026-10-03).
 */
@Composable
fun InspirationStar(
    inspired: Boolean,
    onToggle: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    size: Dp = 96.dp,
    band: Color = LocalDesignTokens.current.colors.text.subtle,
    bandLit: Color = LocalDesignTokens.current.colors.accent.inspiration,
    glow: Color? = LocalDesignTokens.current.colors.accent.inspiration
) {
    val colors = LocalDesignTokens.current.colors
    val pulse by rememberInfiniteTransition(label = "inspiration").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 1500, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "glow"
    )
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .toggleable(value = inspired, role = Role.Switch, onValueChange = { onToggle() })
            .semantics { this.contentDescription = contentDescription }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCompassRose(
                lit = inspired,
                band = if (inspired) bandLit else band,
                outline = colors.background.radialEnd,
                heart = if (inspired) colors.text.warmPrimary else colors.surface.card,
                shadow = colors.ornament.shadow,
                glow = if (inspired) glow else null,
                pulse = pulse
            )
        }
    }
}

/** The tips of a star round [center], one per radius, the first straight up. */
private fun starPath(center: Offset, radii: List<Float>): Path = Path().apply {
    val step = 2 * PI.toFloat() / radii.size
    radii.forEachIndexed { i, radius ->
        val angle = -PI.toFloat() / 2 + i * step
        val x = center.x + radius * cos(angle)
        val y = center.y + radius * sin(angle)
        if (i == 0) moveTo(x, y) else lineTo(x, y)
    }
    close()
}

/** The rose's sixteen corners: long points, short points and the valleys between them. */
private fun roseRadii(long: Float, short: Float, valley: Float) =
    List(16) { i -> if (i % 2 == 1) valley else if (i % 4 == 0) long else short }

private fun DrawScope.drawCompassRose(
    lit: Boolean,
    band: Color,
    outline: Color,
    heart: Color,
    shadow: Color,
    glow: Color?,
    pulse: Float
) {
    val center = Offset(size.width / 2f, size.height / 2f)
    val r = size.minDimension * 0.46f
    val body = starPath(center, roseRadii(r, r * 0.66f, r * 0.34f))
    // A soft shadow under it, then the band and its outline.
    translate(top = size.minDimension * 0.05f) { drawPath(body, shadow) }
    drawPath(body, band)
    drawPath(body, outline, style = Stroke(width = size.minDimension * 0.05f, join = StrokeJoin.Round))
    if (lit && glow != null) {
        val radius = r * (0.55f + 0.12f * pulse)
        drawCircle(Brush.radialGradient(listOf(glow, Color.Transparent), center = center, radius = radius), radius = radius, center = center)
    }
    drawPath(starPath(center, roseRadii(r * 0.46f, r * 0.30f, r * 0.16f)), heart)
}
