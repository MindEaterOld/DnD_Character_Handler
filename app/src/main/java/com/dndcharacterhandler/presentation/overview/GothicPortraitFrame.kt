package com.dndcharacterhandler.presentation.overview

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.dndcharacterhandler.presentation.theme.LocalDesignTokens
import com.dndcharacterhandler.presentation.theme.LocalThemeLook
import com.dndcharacterhandler.presentation.theme.ThemeLook
import kotlin.math.hypot

internal val GothicPortraitWidth = 252.dp
internal val GothicPortraitHeight = GothicPortraitWidth * (1422f / 1106f)

/**
 * Classic's octagon, drawn (it replaced the white raster, owner's choice from boards, 2026-10-07: V2): its
 * corners as fractions of the frame, from the bottom edge's left end round to its right end.
 */
private val ClassicOctagon = listOf(
    Offset(.34f, .912f), Offset(.131f, .763f), Offset(.131f, .359f), Offset(.375f, .186f),
    Offset(.625f, .186f), Offset(.869f, .359f), Offset(.869f, .763f), Offset(.66f, .912f)
)

/** The engraving's opening, measured against its 1106 × 1422 raster, inside the white moulding; in the same order. */
private val GothicOpening = listOf(
    Offset(.344f, .893f), Offset(.165f, .752f), Offset(.165f, .366f), Offset(.377f, .202f),
    Offset(.623f, .202f), Offset(.835f, .366f), Offset(.835f, .752f), Offset(.656f, .893f)
)

/** The experience's ring: as thick as the classic frame's line. */
private val PortraitRingWidth = 4.dp

/** The level's plaque in the frame's bottom edge (owner's choice, 2026-10-07): as low as its two lines allow. */
internal val PortraitPlaqueHeight = 40.dp

/** The polygon the ring follows: Classic's drawn octagon; in an engraving, its opening, the ring inside it. */
private fun ringPolygon(look: ThemeLook): List<Offset> = if (look.portraitArtwork == null) ClassicOctagon else GothicOpening

/** How far in the ring lies from its polygon: on Classic's line; inside the engraving's opening, clear of its moulding. */
private fun ringInset(look: ThemeLook): Dp = if (look.portraitArtwork == null) 0.dp else PortraitRingWidth / 2

/** The plaque's centre from the frame's top: on the ring's bottom edge. */
internal fun portraitPlaqueCenterY(look: ThemeLook): Dp =
    GothicPortraitHeight * ringPolygon(look).first().y - ringInset(look)

/** The ring's lower-left bevel, in dp: its foot (the bottom edge's left end) and its direction down to that foot. */
private fun lowerBevel(look: ThemeLook): Pair<Offset, Offset> {
    val ring = inset(scaled(ringPolygon(look), Size(GothicPortraitWidth.value, GothicPortraitHeight.value)), ringInset(look).value)
    val foot = ring[0]
    val top = ring[1]
    val length = hypot(foot.x - top.x, foot.y - top.y)
    return foot to Offset((foot.x - top.x) / length, (foot.y - top.y) / length)
}

/**
 * The cut of the plaque's top corners: square across the bevel and as long as the ring is wide — the ring's own
 * end (owner's wish, 2026-10-07). [DpSize.width] along the top edge, [DpSize.height] down the side.
 */
private fun plaqueTopCut(look: ThemeLook): DpSize {
    val (_, down) = lowerBevel(look)
    return DpSize(PortraitRingWidth * down.y, PortraitRingWidth * down.x)
}

/**
 * The plaque, centred on the ring's bottom edge, as wide as puts the cuts of its top corners square across the
 * ring's lower bevels: the ring comes down each bevel and ends in that cut.
 */
internal fun portraitPlaqueSize(look: ThemeLook): DpSize {
    val (foot, down) = lowerBevel(look)
    val cut = plaqueTopCut(look)
    // The ring's middle meets the cut's middle, half a cut below the plaque's top.
    val up = (PortraitPlaqueHeight.value / 2 - cut.height.value / 2) / down.y
    val left = foot.x - down.x * up - cut.width.value / 2
    return DpSize(((GothicPortraitWidth.value / 2 - left) * 2).dp, PortraitPlaqueHeight)
}

/** The plaque's outline: every corner cut as the top ones are, by the ring's end ([plaqueTopCut]); the bottom mirrors it. */
internal fun portraitPlaqueShape(look: ThemeLook): Shape = PlaqueShape(plaqueTopCut(look))

private class PlaqueShape(private val cut: DpSize) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline = with(density) {
        val x = cut.width.toPx()
        val y = cut.height.toPx()
        Outline.Generic(Path().apply {
            moveTo(x, 0f)
            lineTo(size.width - x, 0f)
            lineTo(size.width, y)
            lineTo(size.width, size.height - y)
            lineTo(size.width - x, size.height)
            lineTo(x, size.height)
            lineTo(0f, size.height - y)
            lineTo(0f, y)
            close()
        })
    }
}

/**
 * The overview's portrait (owner's choices from boards, 2026-10-07: V2, the plaque): the frame is the experience
 * bar. A ring round the portrait fills with [progress] from the left side of the plaque in the bottom edge, round
 * the frame, back into its right side; [plaque] (the level and the experience) sits in that edge. Classic draws
 * its octagon — the ring on its line and a hairline inside; an engraving keeps its white artwork, tinted with the
 * palette, and the ring runs inside its opening.
 */
@Composable
internal fun GothicPortraitFrame(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    progress: Float = 0f,
    progressColor: Color = LocalDesignTokens.current.colors.progress.xpFill,
    plaque: (@Composable () -> Unit)? = null,
    portrait: @Composable BoxScope.() -> Unit
) {
    val colors = LocalDesignTokens.current.colors
    val look = LocalThemeLook.current
    val drawn = look.portraitArtwork == null
    val polygon = ringPolygon(look)
    // Classic's portrait reaches the ring's inner edge; an engraving's fills its opening.
    val opening = remember(polygon, drawn) { PolygonShape(polygon, if (drawn) PortraitRingWidth / 2 else 0.dp) }
    val track = if (drawn) colors.border.miniCard else colors.progress.xpTrack
    val hairline = colors.ornament.middle
    Box(modifier = modifier.size(GothicPortraitWidth, GothicPortraitHeight)) {
        Box(
            modifier = Modifier.fillMaxSize().clip(opening)
                .background(colors.surface.portrait).clickable(onClick = onClick)
        ) {
            // Crop/framing operates on the portrait opening, not on the roof and transparent margins.
            Box(
                modifier = Modifier.offset(x = GothicPortraitWidth * .165f, y = GothicPortraitHeight * .202f)
                    .size(GothicPortraitWidth * .67f, GothicPortraitHeight * .691f),
                content = portrait
            )
        }
        look.portraitArtwork?.let { artwork ->
            Image(
                painter = painterResource(artwork),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.FillBounds,
                colorFilter = ColorFilter.tint(colors.ornament.inner)
            )
        }
        Box(
            modifier = Modifier.fillMaxSize().drawWithCache {
                val ringWidth = PortraitRingWidth.toPx()
                val ring = inset(scaled(polygon, size), ringInset(look).toPx())
                val path = ringPath(ring)
                // Where the ring leaves the plaque and comes back to it.
                val bottom = ring.first().y
                val plaqueSize = portraitPlaqueSize(look)
                val plaqueArea = if (plaque != null) {
                    Rect(
                        left = (size.width - plaqueSize.width.toPx()) / 2,
                        top = bottom - plaqueSize.height.toPx() / 2,
                        right = (size.width + plaqueSize.width.toPx()) / 2,
                        bottom = bottom + plaqueSize.height.toPx() / 2
                    )
                } else {
                    null
                }
                val measure = PathMeasure().apply { setPath(path, false) }
                val length = measure.length
                var start = 0f
                var end = length
                if (plaqueArea != null) {
                    while (start < length && plaqueArea.contains(measure.getPosition(start))) start += 1f
                    while (end > start && plaqueArea.contains(measure.getPosition(end))) end -= 1f
                }
                // The ends reach into the plaque and its outline cuts them: the ring meets its corners squarely,
                // not with an end cut across the bevel.
                val reach = if (plaqueArea != null) ringWidth * 2 else 0f
                val trackPath = Path().also { measure.getSegment(start - reach, end + reach, it, true) }
                val filled = Path().also {
                    measure.getSegment(start - reach, start + (end - start) * progress.coerceIn(0f, 1f), it, true)
                }
                val inner = if (drawn) ringPath(inset(ring, 5.dp.toPx())) else null
                // The plaque's own outline, where it stands: what cuts the ring's ends.
                val plaqueOutline = plaqueArea?.let { area ->
                    val outline = portraitPlaqueShape(look).createOutline(area.size, layoutDirection, this)
                    Path().apply {
                        addPath((outline as Outline.Generic).path, area.topLeft)
                    }
                }
                val stroke = Stroke(ringWidth, cap = StrokeCap.Butt, join = StrokeJoin.Miter)
                onDrawBehind {
                    inner?.let { drawPath(it, hairline, style = Stroke(.75.dp.toPx())) }
                    if (plaqueOutline != null) {
                        clipPath(plaqueOutline, ClipOp.Difference) {
                            drawPath(trackPath, track, style = stroke)
                            if (progress > 0f) drawPath(filled, progressColor, style = stroke)
                        }
                    } else {
                        drawPath(trackPath, track, style = stroke)
                        if (progress > 0f) drawPath(filled, progressColor, style = stroke)
                    }
                }
            }
        )
        if (plaque != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = portraitPlaqueCenterY(look) - PortraitPlaqueHeight / 2)
                    .size(portraitPlaqueSize(look)),
                contentAlignment = Alignment.Center
            ) { plaque() }
        }
    }
}

/** A polygon given as [fractions] of the shape's size, moved in by [inset]. */
private class PolygonShape(private val fractions: List<Offset>, private val inset: Dp) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val points = inset(scaled(fractions, size), with(density) { inset.toPx() })
        return Outline.Generic(Path().apply {
            moveTo(points.first().x, points.first().y)
            points.drop(1).forEach { lineTo(it.x, it.y) }
            close()
        })
    }
}

/** [fractions] of a frame of [size], in px. */
private fun scaled(fractions: List<Offset>, size: Size): List<Offset> =
    fractions.map { Offset(it.x * size.width, it.y * size.height) }

/** A convex polygon moved in by [by] px: each edge shifted toward the middle, the corners where they meet again. */
private fun inset(points: List<Offset>, by: Float): List<Offset> {
    if (by == 0f) return points
    val middle = Offset(points.map { it.x }.average().toFloat(), points.map { it.y }.average().toFloat())
    val n = points.size
    // Each edge as a point on it, moved in, and its direction.
    val edges = (0 until n).map { i ->
        val a = points[i]
        val b = points[(i + 1) % n]
        val length = hypot(b.x - a.x, b.y - a.y)
        val direction = Offset((b.x - a.x) / length, (b.y - a.y) / length)
        var normal = Offset(-direction.y, direction.x)
        if ((middle.x - a.x) * normal.x + (middle.y - a.y) * normal.y < 0) normal = Offset(-normal.x, -normal.y)
        Offset(a.x + normal.x * by, a.y + normal.y * by) to direction
    }
    return (0 until n).map { i ->
        val (p1, d1) = edges[(i - 1 + n) % n]
        val (p2, d2) = edges[i]
        val cross = d1.x * d2.y - d1.y * d2.x
        val t = ((p2.x - p1.x) * d2.y - (p2.y - p1.y) * d2.x) / cross
        Offset(p1.x + d1.x * t, p1.y + d1.y * t)
    }
}

/** The ring round [points]: from the bottom edge's middle, left first, round to the middle again. */
private fun ringPath(points: List<Offset>): Path = Path().apply {
    val first = points.first()
    val last = points.last()
    moveTo((first.x + last.x) / 2, (first.y + last.y) / 2)
    points.forEach { lineTo(it.x, it.y) }
    close()
}
