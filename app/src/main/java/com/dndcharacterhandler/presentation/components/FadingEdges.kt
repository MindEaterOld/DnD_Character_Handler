package com.dndcharacterhandler.presentation.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Soft edges for what scrolls in a pop-up or a sheet (owner's wish, 2026-10-07): what goes past the top or the
 * bottom fades out over [FadingEdgeLength] instead of being cut. An edge fades only while there is more to scroll
 * that way, as much as there is up to the fade's length, so a list at rest is never faded. The colours here are
 * the mask's alpha only.
 */
private fun Modifier.fadingEdges(
    /** How much of a fade's length, in px, there is hidden above: 0 none, 1 a whole fade or more. */
    top: (fadePx: Float) -> Float,
    bottom: (fadePx: Float) -> Float,
    length: Dp
): Modifier = this
    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithContent {
        drawContent()
        val px = length.toPx().coerceAtMost(size.height / 2)
        val hiddenAbove = top(px).coerceIn(0f, 1f)
        val hiddenBelow = bottom(px).coerceIn(0f, 1f)
        if (hiddenAbove > 0f) {
            drawRect(
                brush = Brush.verticalGradient(0f to Color.Black.copy(alpha = 1f - hiddenAbove), 1f to Color.Black, endY = px),
                size = Size(size.width, px),
                blendMode = BlendMode.DstIn
            )
        }
        if (hiddenBelow > 0f) {
            drawRect(
                brush = Brush.verticalGradient(
                    0f to Color.Black,
                    1f to Color.Black.copy(alpha = 1f - hiddenBelow),
                    startY = size.height - px,
                    endY = size.height
                ),
                topLeft = Offset(0f, size.height - px),
                size = Size(size.width, px),
                blendMode = BlendMode.DstIn
            )
        }
    }

/** Soft edges for a column scrolled by [state]; put it before the column's `verticalScroll`. */
fun Modifier.fadingEdges(state: ScrollState, length: Dp = FadingEdgeLength): Modifier = fadingEdges(
    top = { fadePx -> state.value / fadePx },
    bottom = { fadePx -> (state.maxValue - state.value) / fadePx },
    length = length
)

/** Soft edges for a lazy list scrolled by [state]: the bottom fades while there is more below. */
fun Modifier.fadingEdges(state: LazyListState, length: Dp = FadingEdgeLength): Modifier = fadingEdges(
    top = { fadePx -> if (state.firstVisibleItemIndex > 0) 1f else state.firstVisibleItemScrollOffset / fadePx },
    bottom = { if (state.canScrollForward) 1f else 0f },
    length = length
)

/** A column that scrolls with soft edges: `verticalScroll` and [fadingEdges] on one state. */
@Composable
fun Modifier.fadingVerticalScroll(state: ScrollState = rememberScrollState()): Modifier =
    fadingEdges(state).verticalScroll(state)

/** A lazy list with soft edges, for lists inside pop-ups and sheets. */
@Composable
fun FadingLazyColumn(
    modifier: Modifier = Modifier,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: LazyListScope.() -> Unit
) {
    val state = rememberLazyListState()
    LazyColumn(
        state = state,
        modifier = modifier.fadingEdges(state),
        verticalArrangement = verticalArrangement,
        content = content
    )
}

/** How far the edges fade. */
val FadingEdgeLength = 24.dp
