package com.dndcharacterhandler.presentation.overview

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.dndcharacterhandler.presentation.theme.LocalDesignTokens
import com.dndcharacterhandler.presentation.theme.LocalThemeLook
import com.dndcharacterhandler.presentation.theme.ThemeLook
import kotlin.math.hypot

internal val GothicPortraitWidth = 252.dp
internal val GothicPortraitHeight = GothicPortraitWidth * (1422f / 1106f)

/**
 * Classic's frame, drawn (owner's choice from boards, 2026-10-08: R2): a rectangle with the stat cards' corners,
 * 202 × 256 — its edges as fractions of the frame's box. Tried and turned down before it: an arch (A2, E2, W1), an
 * octagon; and after it, the hit points joined to the portrait (a pedestal, a character card).
 */
private const val FrameLeft = .1f
private const val FrameRight = .9f
private const val FrameTop = .11f
private const val FrameBottom = .9f
private val FrameCorner = 10.dp

/** The engraving's opening, measured against its 1106 × 1422 raster, inside the white moulding: from the bottom edge's left end round. */
private val GothicOpening = listOf(
    Offset(.344f, .893f), Offset(.165f, .752f), Offset(.165f, .366f), Offset(.377f, .202f),
    Offset(.623f, .202f), Offset(.835f, .366f), Offset(.835f, .752f), Offset(.656f, .893f)
)

/** The experience's ring. */
private val PortraitRingWidth = 4.dp

/**
 * The level's plate (owner's wishes, 2026-10-07): the whole gap from serif to serif, this much air round its words
 * across; low, not as tall as the words — solid [PlateOut] outside the ring's line and [PlateIn] inside it, then
 * fading [PlateFade] over the picture to nothing; its corners rounded.
 */
private val PlateAirAcross = 14.dp
private val PlateAirDown = 5.dp
private val PlateCorner = 6.dp
private val PlateOut = 9.dp
private val PlateIn = 3.dp
private val PlateFade = 10.dp

/** The serifs the ring ends in at the gap. */
private val GapSerif = 12.dp

/** How far the level's words and plate rise over Classic's top edge: what must clear the header's mask. */
private val LevelOverhang = 12.dp

/** The level's words: the numeral's size (the caller sets «lvl» smaller), trimmed of their leading. */
@Composable
@ReadOnlyComposable
private fun badgeStyle() = MaterialTheme.typography.headlineMedium.copy(
    lineHeightStyle = LineHeightStyle(alignment = LineHeightStyle.Alignment.Center, trim = LineHeightStyle.Trim.Both)
)

/**
 * The frame's shape for a [look]: Classic's drawn rectangle, the level in its top edge; or the engraving's opening,
 * the ring inside its moulding, the level in its bottom edge.
 */
private class FrameGeometry(look: ThemeLook) {
    val drawn = look.portraitArtwork == null

    /** The box the picture is laid in, as fractions of the frame. */
    val box: Rect = if (drawn) {
        Rect(FrameLeft, FrameTop, FrameRight, FrameBottom)
    } else {
        Rect(GothicOpening.minOf { it.x }, GothicOpening.minOf { it.y }, GothicOpening.maxOf { it.x }, GothicOpening.maxOf { it.y })
    }

    /** Half the frame's width as it is drawn, as a fraction of the box's: where the side buttons stand off. */
    val halfWidth: Float = if (drawn) .5f - FrameLeft else .5f - GothicOpening.minOf { it.x }

    /** The middle of the straight sides, as a fraction of the height: where the side buttons' columns are centred. */
    fun sideMiddle(): Float = if (drawn) (FrameTop + FrameBottom) / 2 else (GothicOpening[1].y + GothicOpening[2].y) / 2

    /** The line the level is written on, in px, for a frame of [size]: Classic's top edge, the engraving's bottom one. */
    fun levelLine(size: Size, ringInsetPx: Float): Float =
        if (drawn) FrameTop * size.height else GothicOpening.first().y * size.height - ringInsetPx

    /** The opening, closed, moved in by [d] px, its corners rounded by [cornerPx]: what the picture is clipped to. */
    fun opening(size: Size, d: Float, cornerPx: Float): Path = Path().apply {
        if (drawn) {
            addRoundRect(
                RoundRect(
                    FrameLeft * size.width + d, FrameTop * size.height + d,
                    FrameRight * size.width - d, FrameBottom * size.height - d,
                    CornerRadius((cornerPx - d).coerceAtLeast(0f))
                )
            )
        } else {
            val points = inset(scaled(GothicOpening, size), d)
            moveTo(points.first().x, points.first().y)
            points.drop(1).forEach { lineTo(it.x, it.y) }
            close()
        }
    }

    /**
     * The ring, clockwise round the frame from the gap's far end to its near one: in Classic from the top edge's gap
     * to the right, down, along the bottom, up and back; in the engraving from the bottom edge's gap to the left, up,
     * over and down.
     */
    fun ring(size: Size, ringInsetPx: Float, gapHalf: Float, cornerPx: Float): Path = Path().apply {
        val cx = size.width / 2
        if (drawn) {
            val l = FrameLeft * size.width
            val r = FrameRight * size.width
            val t = FrameTop * size.height
            val b = FrameBottom * size.height
            val c = cornerPx
            moveTo(cx + gapHalf, t)
            lineTo(r - c, t)
            arcTo(Rect(r - 2 * c, t, r, t + 2 * c), -90f, 90f, false)
            lineTo(r, b - c)
            arcTo(Rect(r - 2 * c, b - 2 * c, r, b), 0f, 90f, false)
            lineTo(l + c, b)
            arcTo(Rect(l, b - 2 * c, l + 2 * c, b), 90f, 90f, false)
            lineTo(l, t + c)
            arcTo(Rect(l, t, l + 2 * c, t + 2 * c), 180f, 90f, false)
            lineTo(cx - gapHalf, t)
        } else {
            val b = levelLine(size, ringInsetPx)
            moveTo(cx - gapHalf, b)
            inset(scaled(GothicOpening, size), ringInsetPx).forEach { lineTo(it.x, it.y) }
            lineTo(cx + gapHalf, b)
        }
    }
}

/** The middle of the frame's straight sides, from its top: where the side buttons' columns are centred. */
@Composable
@ReadOnlyComposable
internal fun portraitSideMiddleY(): Dp = GothicPortraitHeight * FrameGeometry(LocalThemeLook.current).sideMiddle()

/** From the frame's middle to the side buttons' columns' middles: off its sides by [gap], half a button more. */
@Composable
@ReadOnlyComposable
internal fun portraitSideColumnX(gap: Dp): Dp =
    GothicPortraitWidth * FrameGeometry(LocalThemeLook.current).halfWidth + PortraitRingWidth / 2 + gap + PortraitSideButtonSize / 2

/**
 * From the frame's box's top to the highest it draws: Classic's level over its top edge; the engraving's artwork,
 * kept 4dp clear. Nothing above it may stand under the header's mask.
 */
@Composable
@ReadOnlyComposable
internal fun portraitFrameHead(): Dp =
    if (LocalThemeLook.current.portraitArtwork == null) GothicPortraitHeight * FrameTop - LevelOverhang else (-4).dp

/** The lowest the frame draws, from its top: Classic's bottom edge, or the engraving's artwork. */
@Composable
@ReadOnlyComposable
internal fun portraitFrameFoot(): Dp =
    if (LocalThemeLook.current.portraitArtwork == null) GothicPortraitHeight * FrameBottom + PortraitRingWidth / 2 else GothicPortraitHeight

/** From the frame's foot to the hit points' card: close under Classic's plain edge (R2), clear of the engraving's spike. */
@Composable
@ReadOnlyComposable
internal fun portraitHpGap(): Dp = if (LocalThemeLook.current.portraitArtwork == null) 12.dp else 27.dp

/**
 * The overview's portrait (owner's choices from boards and the screen, 2026-10-07 and 2026-10-08: V2, R2): the frame
 * is the experience bar. A ring round the portrait fills with [progress] from one end of a gap in an edge, round the
 * frame, back to its other end; the ring ends at the gap in serifs, and [badge] (the level, «I lvl») is written in it,
 * on the ring's line, on a low dark plate filling the gap and fading over the picture. Classic draws a rectangle with
 * the stat cards' corners, the gap in its top edge, the ring on its line; an engraving keeps its white artwork,
 * tinted with the palette, the ring inside its opening, the gap in its bottom edge. The picture darkens toward the
 * frame's edges, sunk into it. A tap on the words is [onBadgeClick], a long press [onBadgeLongClick].
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun GothicPortraitFrame(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    progress: Float = 0f,
    progressColor: Color = LocalDesignTokens.current.colors.progress.xpFill,
    badge: AnnotatedString? = null,
    badgeDescription: String = "",
    onBadgeClick: () -> Unit = {},
    onBadgeLongClick: () -> Unit = {},
    portrait: @Composable BoxScope.() -> Unit
) {
    val colors = LocalDesignTokens.current.colors
    val look = LocalThemeLook.current
    val geometry = remember(look) { FrameGeometry(look) }
    val density = LocalDensity.current
    val cornerPx = with(density) { FrameCorner.toPx() }
    // Classic's portrait reaches the ring's inner edge; an engraving's fills its opening.
    val opening = remember(geometry) { OpeningShape(geometry, if (geometry.drawn) PortraitRingWidth / 2 else 0.dp, FrameCorner) }
    val ringInset = if (geometry.drawn) 0.dp else PortraitRingWidth / 2
    val track = if (geometry.drawn) colors.border.miniCard else colors.progress.xpTrack
    val dark = colors.ornament.dropShadow
    val box = geometry.box
    val style = badgeStyle()
    val measurer = rememberTextMeasurer()
    val badgeSize = badge?.let { measurer.measure(it, style).size }
    val plate = with(density) {
        badgeSize?.let { Size(it.width + PlateAirAcross.toPx() * 2, it.height + PlateAirDown.toPx() * 2) } ?: Size.Zero
    }
    // The ring ends where the plate begins.
    val gapPx = plate.width
    Box(modifier = modifier.size(GothicPortraitWidth, GothicPortraitHeight)) {
        Box(
            modifier = Modifier.fillMaxSize().clip(opening)
                .background(colors.surface.portrait).clickable(onClick = onClick)
                .drawWithContent {
                    drawContent()
                    // The picture sunk into the frame (owner's choice from boards, 2026-10-07: D1): an oval over
                    // the opening's box, clear in the middle, darkening to its edges.
                    val center = Offset(size.width * box.center.x, size.height * box.center.y)
                    val radius = size.width * box.width / 2 * 1.04f
                    scale(1f, size.height * box.height / (size.width * box.width), pivot = center) {
                        drawCircle(
                            brush = Brush.radialGradient(.55f to Color.Transparent, 1f to dark, center = center, radius = radius),
                            radius = radius * 2f,
                            center = center
                        )
                    }
                }
        ) {
            // Crop/framing operates on the opening's own box, not on the roof and transparent margins: the
            // picture covers the whole opening, edge to edge.
            Box(
                modifier = Modifier.offset(x = GothicPortraitWidth * box.left, y = GothicPortraitHeight * box.top)
                    .size(GothicPortraitWidth * box.width, GothicPortraitHeight * box.height),
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
                val half = gapPx / 2
                val line = geometry.levelLine(size, ringInset.toPx())
                val ring = geometry.ring(size, ringInset.toPx(), half, cornerPx)
                val measure = PathMeasure().apply { setPath(ring, false) }
                val filled = Path().also { measure.getSegment(0f, measure.length * progress.coerceIn(0f, 1f), it, true) }
                val stroke = Stroke(ringWidth, cap = StrokeCap.Butt, join = StrokeJoin.Miter)
                val cx = size.width / 2
                val serif = GapSerif.toPx() / 2
                val serifWidth = 2.dp.toPx()
                // The serifs on the ring's last stretch, flush with its ends: on the plate's edges. The ring starts
                // at the gap's far end — the right one in Classic's top edge, the left one in the engraving's bottom.
                val startSerif = if (geometry.drawn) cx + half + serifWidth / 2 else cx - half - serifWidth / 2
                val endSerif = if (geometry.drawn) cx - half - serifWidth / 2 else cx + half + serifWidth / 2
                val layer = Paint().apply { alpha = track.alpha }
                val corner = CornerRadius(PlateCorner.toPx())
                // The plate: solid outside the frame, fading over the picture to nothing — down from Classic's top
                // edge, up from the engraving's bottom one.
                val inward = if (geometry.drawn) 1f else -1f
                val solidEnd = line + inward * PlateIn.toPx()
                val fadeEnd = solidEnd + inward * PlateFade.toPx()
                val outer = line - inward * PlateOut.toPx()
                val plateTop = minOf(outer, fadeEnd)
                val plateBottom = maxOf(outer, fadeEnd)
                val solidStop = (if (geometry.drawn) solidEnd - plateTop else plateBottom - solidEnd) / (plateBottom - plateTop)
                val plateBrush = if (geometry.drawn) {
                    Brush.verticalGradient(
                        0f to colors.surface.card,
                        solidStop to colors.surface.card,
                        1f to colors.surface.card.copy(alpha = 0f),
                        startY = plateTop,
                        endY = plateBottom
                    )
                } else {
                    Brush.verticalGradient(
                        0f to colors.surface.card.copy(alpha = 0f),
                        1f - solidStop to colors.surface.card,
                        1f to colors.surface.card,
                        startY = plateTop,
                        endY = plateBottom
                    )
                }
                onDrawBehind {
                    if (badge != null) {
                        // Under the ring and its serifs, which end on its edges.
                        drawRoundRect(
                            plateBrush,
                            topLeft = Offset(cx - half, plateTop),
                            size = Size(half * 2, plateBottom - plateTop),
                            cornerRadius = corner
                        )
                    }
                    // The empty ring and its serifs in one layer, solid, then laid down at the track's alpha: where
                    // they meet nothing doubles.
                    drawContext.canvas.saveLayer(Rect(Offset.Zero, size), layer)
                    val solid = track.copy(alpha = 1f)
                    drawPath(ring, solid, style = stroke)
                    if (badge != null) {
                        drawLine(solid, Offset(startSerif, line - serif), Offset(startSerif, line + serif), serifWidth)
                        drawLine(solid, Offset(endSerif, line - serif), Offset(endSerif, line + serif), serifWidth)
                    }
                    drawContext.canvas.restore()
                    // The experience over it; the starting serif lit with it, the closing one at the full ring.
                    if (progress > 0f) {
                        drawPath(filled, progressColor, style = stroke)
                        if (badge != null) {
                            drawLine(progressColor, Offset(startSerif, line - serif), Offset(startSerif, line + serif), serifWidth)
                        }
                    }
                    if (progress >= 1f && badge != null) {
                        drawLine(progressColor, Offset(endSerif, line - serif), Offset(endSerif, line + serif), serifWidth)
                    }
                }
            }
        )
        if (badge != null) {
            val line = with(density) { geometry.levelLine(Size(GothicPortraitWidth.toPx(), GothicPortraitHeight.toPx()), ringInset.toPx()).toDp() }
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = line - with(density) { plate.height.toDp() } / 2)
                    .size(with(density) { plate.width.toDp() }, with(density) { plate.height.toDp() })
                    .combinedClickable(onClick = onBadgeClick, onLongClick = onBadgeLongClick)
                    .semantics(mergeDescendants = true) { contentDescription = badgeDescription },
                contentAlignment = Alignment.Center
            ) {
                Text(text = badge, modifier = Modifier.wrapContentSize(unbounded = true), style = style, maxLines = 1)
            }
        }
    }
}

/** The frame's opening, moved in by [inset], its corners rounded by [corner]. */
private class OpeningShape(private val geometry: FrameGeometry, private val inset: Dp, private val corner: Dp) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline =
        Outline.Generic(with(density) { geometry.opening(size, inset.toPx(), corner.toPx()) })
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
