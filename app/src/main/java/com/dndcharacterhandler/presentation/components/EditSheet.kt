package com.dndcharacterhandler.presentation.components

import androidx.core.view.WindowCompat
import androidx.compose.ui.window.DialogWindowProvider
import androidx.compose.ui.platform.LocalView
import androidx.compose.runtime.SideEffect
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
 * ([EditDialog]) staying put, the content under it scrolling with soft edges ([fadingEdges]). No buttons: what is ticked applies at once, so there is
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
        // The sheet is a window of its own, and Material lights its bars' icons by the phone's own theme: on a phone in
        // light mode they turned black over the app's dark screens. Every theme of the app is dark: they stay light, as
        // the app's own bars (MainActivity).
        val sheetView = LocalView.current
        SideEffect {
            // The content's view is the sheet layout's child: the window is its parent's.
            ((sheetView.parent as? DialogWindowProvider) ?: (sheetView as? DialogWindowProvider))?.window?.let { window ->
                WindowCompat.getInsetsController(window, sheetView).apply {
                    isAppearanceLightStatusBars = false
                    isAppearanceLightNavigationBars = false
                }
            }
        }
        // Never the whole screen: the sheet in its corner stays visible above it, to tap and close.
        val maxHeight = (LocalConfiguration.current.screenHeightDp * SheetMaxHeightShare).dp
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = maxHeight)
                .padding(start = 24.dp, end = 24.dp, bottom = 24.dp)
        ) {
            // The title stays; what is under it scrolls, its edges soft.
            Text(
                text = title,
                modifier = Modifier.padding(bottom = 4.dp),
                style = MaterialTheme.typography.titleLarge,
                color = colors.text.primary
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fadingVerticalScroll()
                    .padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                content = content
            )
        }
    }
}

/** The sheet's top corners: the pop-up's, 28. */
private val SheetCornerRadius = 28.dp

/** How much of the screen's height the sheet's content takes at most. */
private const val SheetMaxHeightShare = 0.75f
