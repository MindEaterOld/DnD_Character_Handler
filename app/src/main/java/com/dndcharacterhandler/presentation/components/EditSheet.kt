package com.dndcharacterhandler.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.dndcharacterhandler.presentation.theme.FrameStyle
import com.dndcharacterhandler.presentation.theme.LocalDesignTokens
import com.dndcharacterhandler.presentation.theme.LocalThemeLook

/** The cut of an etched panel's corners: the pop-up's and the sheet's in Engraving, as its frame draws them. */
internal val EtchedCornerCut = 6.dp

/**
 * The app's sheet from the bottom (owner's choice from boards, 2026-10-07: S3): for what is browsed and ticked — a
 * list of options, a description to read. A handle on top, the title in the app's serif as the pop-up's
 * ([EditDialog]), the content under it scrolling as a whole. No buttons: what is ticked applies at once, so there is
 * nothing to save, and no cross — a swipe down, a tap above it or Back closes it (the handle says so to TalkBack);
 * it never takes the whole screen, so there is always room above it to tap.
 * The pop-up's colour and corners; in Engraving an etched panel, its top corners cut, its sides running off the
 * screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditSheet(
    title: String,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = LocalDesignTokens.current.colors
    val etched = LocalThemeLook.current.frames == FrameStyle.ETCHED
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        modifier = if (etched) {
            Modifier.drawWithContent {
                drawContent()
                drawEngravedFrame(colors.border.panel, openBottom = true)
            }
        } else {
            Modifier
        },
        shape = if (etched) {
            CutCornerShape(topStart = EtchedCornerCut, topEnd = EtchedCornerCut)
        } else {
            RoundedCornerShape(topStart = SheetCornerRadius, topEnd = SheetCornerRadius)
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        dragHandle = { BottomSheetDefaults.DragHandle(color = colors.border.default) }
    ) {
        // Never the whole screen: the sheet in its corner stays visible above it, to tap and close.
        val maxHeight = (LocalConfiguration.current.screenHeightDp * SheetMaxHeightShare).dp
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = maxHeight)
                .verticalScroll(rememberScrollState())
                .padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(text = title, style = MaterialTheme.typography.titleLarge, color = colors.text.primary)
            content()
        }
    }
}

/** The sheet's top corners: the pop-up's, 28. */
private val SheetCornerRadius = 28.dp

/** How much of the screen's height the sheet's content takes at most. */
private const val SheetMaxHeightShare = 0.75f
