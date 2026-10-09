package com.dndcharacterhandler.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.sp
import com.dndcharacterhandler.presentation.theme.LocalDesignTokens

/** A field's label on the sheet's panels: gold capitals, spaced out, as a printed character sheet's (S2, 2026-10-06). */
@Composable
fun SheetLabel(label: String, modifier: Modifier = Modifier, fontSize: TextUnit = TextUnit.Unspecified) {
    Text(
        text = label.uppercase(),
        modifier = modifier,
        style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.sp).let { if (fontSize.isSpecified) it.copy(fontSize = fontSize) else it },
        color = MaterialTheme.colorScheme.primary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

/** A rule with a small diamond in its middle: what parts the parts of a sheet's panel, [air] over and under it. */
@Composable
fun SheetOrnament(modifier: Modifier = Modifier, airAbove: Dp = 10.dp, airBelow: Dp = airAbove) {
    val colors = LocalDesignTokens.current.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = airAbove, bottom = airBelow),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.weight(1f).height(1.dp).background(colors.ornament.stroke))
        Canvas(modifier = Modifier.padding(horizontal = 8.dp).size(9.dp)) {
            val diamond = Path().apply {
                moveTo(size.width / 2, 0f)
                lineTo(size.width, size.height / 2)
                lineTo(size.width / 2, size.height)
                lineTo(0f, size.height / 2)
                close()
            }
            drawPath(diamond, colors.ornament.middle)
        }
        Box(modifier = Modifier.weight(1f).height(1.dp).background(colors.ornament.stroke))
    }
}
