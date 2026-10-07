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
 * Classic's frame, drawn (owner's choices from boards and the screen, 2026-10-07: A2, E2, W1): straight sides, a
 * flat bottom with its corners a little rounded, and a round arch over them, 30 % taller than a half circle — its
 * edges as fractions of the frame.
 */
private const val ArchLeft = .1f
private const val ArchRight = .9f
private const val ArchBottom = .9f
private const val ArchApex = .11f
private const val ArchStretch = 1.3f
private val ArchFootRadius = 10.dp

/** The engraving's opening, measured against its 1106 × 1422 raster, inside the white moulding: from the bottom edge's left end round. */
private val GothicOpening = listOf(
    Offset(.344f, .893f), Offset(.165f, .752f), Offset(.165f, .366f), Offset(.377f, .202f),
    Offset(.623f, .202f), Offset(.835f, .366f), Offset(.835f, .752f), Offset(.656f, .893f)
)

/** The experience's ring. */
private val PortraitRingWidth = 4.dp

/** The level's plate: round its words this much air across and up and down, its corners rounded. */
private val PlateAirAcross = 10.dp
private val PlateAirDown = 5.dp
private val PlateCorner = 6.dp

/** Between the plate and the ring's ends. */
private val GapAir = 5.dp

/** The serifs the ring ends in at the gap. */
private val GapSerif = 12.dp

/** How far the level's plate hangs below the ring's line, for what follows the frame. */
private val PlateFoot = 20.dp

/** The level's words: the numeral's size (the caller sets «lvl» smaller), trimmed of their leading. */
@Composable
@ReadOnlyComposable
private fun badgeStyle() = MaterialTheme.typography.headlineMedium.copy(
    lineHeightStyle = LineHeightStyle(alignment = LineHeightStyle.Alignment.Center, trim = LineHeightStyle.Trim.Both)
)

/** The frame's shape for a [look]: Classic's drawn arch, or the engraving's opening, the ring inside its moulding. */
private class FrameGeometry(look: ThemeLook) {
    val drawn = look.portraitArtwork == null

    /** The box the picture is laid in, as fractions of the frame. */
    val box: Rect = if (drawn) {
        Rect(ArchLeft, ArchApex, ArchRight, ArchBottom)
    } else {
        Rect(GothicOpening.minOf { it.x }, GothicOpening.minOf { it.y }, GothicOpening.maxOf { it.x }, GothicOpening.maxOf { it.y })
    }

    /** Half the frame's width as it is drawn, as a fraction of the box's: where the side buttons stand off. */
    val halfWidth: Float = if (drawn) .5f - ArchLeft else .5f - GothicOpening.minOf { it.x }

    /** Where the arch's sides end, as a fraction of the frame's height, for a frame [w] × [h]. */
    fun shoulder(w: Float, h: Float): Float = ArchApex + (ArchRight - ArchLeft) * w / 2 * ArchStretch / h

    /** The middle of the straight sides, as a fraction of the height: where the side buttons' columns are centred. */
    fun sideMiddle(): Float = if (drawn) {
        (shoulder(GothicPortraitWidth.value, GothicPortraitHeight.value) + ArchBottom) / 2
    } else {
        (GothicOpening[1].y + GothicOpening[2].y) / 2
    }

    /** The ring's bottom edge, in px, for a frame of [size]. */
    fun bottom(size: Size, ringInsetPx: Float): Float =
        if (drawn) ArchBottom * size.height else GothicOpening.first().y * size.height - ringInsetPx

    /** The opening, closed, moved in by [d] px, its bottom corners rounded by [footPx]: what the picture is clipped to. */
    fun opening(size: Size, d: Float, footPx: Float): Path = Path().apply {
        if (drawn) {
            val l = ArchLeft * size.width + d
            val r = ArchRight * size.width - d
            val b = ArchBottom * size.height - d
            val foot = (footPx - d).coerceAtLeast(0f)
            moveTo(l + foot, b)
            quadraticTo(l, b, l, b - foot)
            lineTo(l, shoulder(size.width, size.height) * size.height)
            arch(size, d)
            lineTo(r, b - foot)
            quadraticTo(r, b, r - foot, b)
            close()
        } else {
            val points = inset(scaled(GothicOpening, size), d)
            moveTo(points.first().x, points.first().y)
            points.drop(1).forEach { lineTo(it.x, it.y) }
            close()
        }
    }

    /** The ring: from the gap's left end along the bottom, round the frame, back to the gap's right end. */
    fun ring(size: Size, ringInsetPx: Float, gapHalf: Float, footPx: Float): Path = Path().apply {
        val cx = size.width / 2
        val b = bottom(size, ringInsetPx)
        moveTo(cx - gapHalf, b)
        if (drawn) {
            val l = ArchLeft * size.width
            val r = ArchRight * size.width
            lineTo(l + footPx, b)
            quadraticTo(l, b, l, b - footPx)
            lineTo(l, shoulder(size.width, size.height) * size.height)
            arch(size, 0f)
            lineTo(r, b - footPx)
            quadraticTo(r, b, r - footPx, b)
        } else {
            inset(scaled(GothicOpening, size), ringInsetPx).forEach { lineTo(it.x, it.y) }
        }
        lineTo(cx + gapHalf, b)
    }

    /** The arch from the left side's top to the right one's, moved in by [d] px. */
    private fun Path.arch(size: Size, d: Float) {
        val rx = (ArchRight - ArchLeft) * size.width / 2
        val ry = rx * ArchStretch
        val cy = shoulder(size.width, size.height) * size.height
        arcTo(Rect(size.width / 2 - rx + d, cy - ry + d, size.width / 2 + rx - d, cy + ry - d), 180f, 180f, false)
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

/** The lowest the frame draws, from its top: Classic's level words on its bottom edge, or the engraving's artwork. */
@Composable
@ReadOnlyComposable
internal fun portraitFrameFoot(): Dp =
    if (LocalThemeLook.current.portraitArtwork == null) GothicPortraitHeight * ArchBottom + PlateFoot else GothicPortraitHeight

/**
 * The overview's portrait (owner's choices from boards and the screen, 2026-10-07: V2, A2, E2, W1): the frame is the
 * experience bar. A ring round the portrait fills with [progress] from the gap in the bottom edge — its left end —
 * round the frame, back to the gap's right end; the ring ends at the gap in serifs, and [badge] (the level, «I lvl»)
 * is written in it, on the ring's line, on a dark plate. Classic draws its arch, the ring on its line; an engraving
 * keeps its white artwork, tinted with the palette, the ring inside its opening. The
 * picture darkens toward the frame's edges, sunk into it. A tap on the words is [onBadgeClick], a long press
 * [onBadgeLongClick].
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
    val footPx = with(density) { ArchFootRadius.toPx() }
    // Classic's portrait reaches the ring's inner edge; an engraving's fills its opening.
    val opening = remember(geometry) { OpeningShape(geometry, if (geometry.drawn) PortraitRingWidth / 2 else 0.dp, ArchFootRadius) }
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
    val gapPx = if (badge != null) plate.width + with(density) { GapAir.toPx() } * 2 else 0f
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
                val bottom = geometry.bottom(size, ringInset.toPx())
                val ring = geometry.ring(size, ringInset.toPx(), half, footPx)
                val measure = PathMeasure().apply { setPath(ring, false) }
                val filled = Path().also { measure.getSegment(0f, measure.length * progress.coerceIn(0f, 1f), it, true) }
                val stroke = Stroke(ringWidth, cap = StrokeCap.Butt, join = StrokeJoin.Miter)
                val cx = size.width / 2
                val serif = GapSerif.toPx() / 2
                val serifWidth = 2.dp.toPx()
                // The serifs stand just outside the ring's ends, flush with them.
                val leftSerif = cx - half - serifWidth / 2
                val rightSerif = cx + half + serifWidth / 2
                val layer = Paint().apply { alpha = track.alpha }
                val corner = CornerRadius(PlateCorner.toPx())
                onDrawBehind {
                    // The empty ring and its serifs in one layer, solid, then laid down at the track's alpha: where
                    // they meet nothing doubles.
                    drawContext.canvas.saveLayer(Rect(Offset.Zero, size), layer)
                    val solid = track.copy(alpha = 1f)
                    drawPath(ring, solid, style = stroke)
                    if (badge != null) {
                        drawLine(solid, Offset(leftSerif, bottom - serif), Offset(leftSerif, bottom + serif), serifWidth)
                        drawLine(solid, Offset(rightSerif, bottom - serif), Offset(rightSerif, bottom + serif), serifWidth)
                    }
                    drawContext.canvas.restore()
                    // The experience over it; the left serif lit with it, the right one at the full ring.
                    if (progress > 0f) {
                        drawPath(filled, progressColor, style = stroke)
                        if (badge != null) {
                            drawLine(progressColor, Offset(leftSerif, bottom - serif), Offset(leftSerif, bottom + serif), serifWidth)
                        }
                    }
                    if (progress >= 1f && badge != null) {
                        drawLine(progressColor, Offset(rightSerif, bottom - serif), Offset(rightSerif, bottom + serif), serifWidth)
                    }
                    if (badge != null) {
                        // The level's plate: dark, for its words to read on the picture and the moulding.
                        drawRoundRect(
                            colors.surface.card,
                            topLeft = Offset(cx - plate.width / 2, bottom - plate.height / 2),
                            size = plate,
                            cornerRadius = corner
                        )
                    }
                }
            }
        )
        if (badge != null) {
            val bottom = with(density) { geometry.bottom(Size(GothicPortraitWidth.toPx(), GothicPortraitHeight.toPx()), ringInset.toPx()).toDp() }
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = bottom - with(density) { plate.height.toDp() } / 2)
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

/** The frame's opening, moved in by [inset], its bottom corners rounded by [foot]. */
private class OpeningShape(private val geometry: FrameGeometry, private val inset: Dp, private val foot: Dp) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline =
        Outline.Generic(with(density) { geometry.opening(size, inset.toPx(), foot.toPx()) })
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
