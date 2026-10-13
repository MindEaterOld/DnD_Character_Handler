package com.dndcharacterhandler.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.unit.dp
import com.dndcharacterhandler.presentation.theme.LocalDesignTokens

/**
 * Where the app's notices show (owner, 2026-10-13 — quick taps on inspiration made Material's host jump, its old notice
 * shrinking out as the new one grew in): one pill that takes a new notice's words in place, its width easing to them,
 * fading in when the first comes and out when the last goes — no scale, nothing moves. Runs the notices' time as
 * Material's host does: 4 s, 10 s with an action, from the newest.
 */
@Composable
fun AppNoticeHost(
    state: SnackbarHostState,
    backdrop: GraphicsLayer?,
    backdropOrigin: Offset,
    modifier: Modifier = Modifier
) {
    val current = state.currentSnackbarData
    var shown by remember { mutableStateOf<SnackbarData?>(null) }
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(current) {
        if (current != null) {
            shown = current
            visible = true
            delay(if (current.visuals.actionLabel != null) NoticeLongMillis else NoticeShortMillis)
            current.dismiss()
        } else {
            // A notice replaced by a newer one is gone for a moment: the pill waits for it before it fades.
            delay(NoticeSwapGraceMillis)
            visible = false
        }
    }
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(tween(NoticeFadeMillis)),
        exit = fadeOut(tween(NoticeFadeMillis))
    ) {
        shown?.let { AppNotice(it, backdrop, backdropOrigin) }
    }
}

/**
 * A notice (owner's choices from the device, 2026-10-13): a pill as wide as its words, on the tab bar's plate — its
 * outline `border.faint`, the screens behind it blurred and tinted ([GlassPlateBack]) — its words `bodyMedium`, its
 * action in gold after them. Stands in the middle of the height it is given.
 */
@Composable
fun AppNotice(data: SnackbarData, backdrop: GraphicsLayer?, backdropOrigin: Offset) {
    val colors = LocalDesignTokens.current.colors
    val shape = RoundedCornerShape(50)
    Box(modifier = Modifier.fillMaxHeight(), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .clip(shape)
                .border(1.dp, colors.border.faint, shape)
        ) {
            GlassPlateBack(backdrop, backdropOrigin, Modifier.matchParentSize())
            Row(
                modifier = Modifier
                    .animateContentSize()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = data.visuals.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.text.primary,
                    maxLines = 1
                )
                data.visuals.actionLabel?.let { label ->
                    Text(
                        text = label,
                        modifier = Modifier
                            .padding(start = 16.dp)
                            .clickable { data.performAction() },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

/** How long a notice stays: as Material's short and long. */
private const val NoticeShortMillis = 4_000L
private const val NoticeLongMillis = 10_000L

/** How long the pill waits for a notice's replacement before it fades. */
private const val NoticeSwapGraceMillis = 120L

/** The pill's fade in and out. */
private const val NoticeFadeMillis = 200
