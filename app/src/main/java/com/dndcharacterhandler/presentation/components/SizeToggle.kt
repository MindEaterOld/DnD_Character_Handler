package com.dndcharacterhandler.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dndcharacterhandler.domain.model.CreatureSize
import com.dndcharacterhandler.presentation.localization.text

/**
 * The character's size: a gnome, a human and a giant, each drawn at its size, in a stat card's
 * frame. [sizes] limits the cells (a species that is Small or Medium); a tap picks one.
 */
@Composable
fun SizeToggle(
    selected: CreatureSize?,
    onSelect: (CreatureSize) -> Unit,
    modifier: Modifier = Modifier,
    sizes: List<CreatureSize> = CreatureSize.entries
) {
    BorderLabelCard(label = text("size_label"), modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 6.dp, end = 6.dp, top = 10.dp, bottom = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            sizes.forEach { size ->
                val isSelected = size == selected
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(toggleFill(isSelected))
                        .clickable { onSelect(size) }
                        .padding(horizontal = 4.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // The figures stand on one line, so their heights compare.
                    Box(modifier = Modifier.height(LargestFigure), contentAlignment = Alignment.BottomCenter) {
                        Icon(
                            imageVector = size.figure,
                            contentDescription = null,
                            tint = toggleContent(isSelected),
                            modifier = Modifier.size(size.figureSize)
                        )
                    }
                    Text(
                        text = text(size.labelKey),
                        style = MaterialTheme.typography.bodyMedium,
                        color = toggleContent(isSelected),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/** "Маленький", "Средний", "Большой". */
val CreatureSize.labelKey: String
    get() = when (this) {
        CreatureSize.SMALL -> "size_small"
        CreatureSize.MEDIUM -> "size_medium"
        CreatureSize.LARGE -> "size_large"
    }

private val LargestFigure = 50.dp

private val CreatureSize.figureSize: Dp
    get() = when (this) {
        CreatureSize.SMALL -> 30.dp
        CreatureSize.MEDIUM -> 40.dp
        CreatureSize.LARGE -> LargestFigure
    }

private val CreatureSize.figure: ImageVector
    get() = when (this) {
        CreatureSize.SMALL -> GnomeFigure
        CreatureSize.MEDIUM -> HumanFigure
        CreatureSize.LARGE -> GiantFigure
    }

/** A filled figure on a 24×24 grid; the fill is a placeholder colour that `Icon` tints. */
private fun figure(name: String, pathData: String): ImageVector =
    ImageVector.Builder(name = name, defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)
        .addPath(pathData = addPathNodes(pathData), fill = SolidColor(Color.Black))
        .build()

/** A gnome: a tall pointed hat, a beard down to the belly, short legs. */
private val GnomeFigure: ImageVector by lazy {
    figure(
        "Gnome",
        "M13.6 0.8 C12.6 3.2 10.4 6 7.6 8.4 H16.6 C15.4 6 14.2 3.4 13.6 0.8 Z " +
            "M7.1 8.8 H16.9 A0.7 0.7 0 0 1 16.9 10.2 H7.1 A0.7 0.7 0 0 1 7.1 8.8 Z " +
            "M8.8 10.8 H15.2 V12.2 C15.2 13.2 16.1 14.4 15.8 16.2 C15.5 18 13.8 19.2 12 20 " +
            "C10.2 19.2 8.5 18 8.2 16.2 C7.9 14.4 8.8 13.2 8.8 12.2 Z " +
            "M6.6 14.2 C6.7 16.2 7.4 17.8 8.8 19 C9.7 19.8 10.8 20.6 12 21.2 C13.2 20.6 14.3 19.8 15.2 19 " +
            "C16.6 17.8 17.3 16.2 17.4 14.2 C18.6 14.8 19.2 16 19.2 17.6 V21.4 H4.8 V17.6 C4.8 16 5.4 14.8 6.6 14.2 Z " +
            "M6.9 22 H11.2 V23.4 H6.2 C6.2 22.7 6.5 22.2 6.9 22 Z " +
            "M12.8 22 H17.1 C17.5 22.2 17.8 22.7 17.8 23.4 H12.8 Z"
    )
}

/** A human, arms down. */
private val HumanFigure: ImageVector by lazy {
    figure(
        "Human",
        "M12 1.5 A3 3 0 1 1 12 7.5 A3 3 0 1 1 12 1.5 Z " +
            "M8.6 8.6 H15.4 C17 8.6 18.2 9.8 18.2 11.4 V15.6 C18.2 16.2 17.7 16.6 17.2 16.6 C16.7 16.6 16.2 16.2 16.2 15.6 " +
            "V12.2 H15.6 V22.2 C15.6 22.8 15.1 23.2 14.5 23.2 C13.9 23.2 13.4 22.8 13.4 22.2 V16.8 H10.6 V22.2 " +
            "C10.6 22.8 10.1 23.2 9.5 23.2 C8.9 23.2 8.4 22.8 8.4 22.2 V12.2 H7.8 V15.6 C7.8 16.2 7.3 16.6 6.8 16.6 " +
            "C6.3 16.6 5.8 16.2 5.8 15.6 V11.4 C5.8 9.8 7 8.6 8.6 8.6 Z"
    )
}

/** A giant: a small head on huge shoulders, long thick arms, heavy legs. */
private val GiantFigure: ImageVector by lazy {
    figure(
        "Giant",
        "M12 1 A2.4 2.4 0 1 1 12 5.8 A2.4 2.4 0 1 1 12 1 Z " +
            "M3.4 6.3 H20.6 C21.9 6.3 22.8 7.2 22.8 8.5 V17.4 C22.8 18.2 22.2 18.8 21.4 18.8 C20.6 18.8 20 18.2 20 17.4 " +
            "V10.4 H19.2 L18.2 15.8 H5.8 L4.8 10.4 H4 V17.4 C4 18.2 3.4 18.8 2.6 18.8 C1.8 18.8 1.2 18.2 1.2 17.4 " +
            "V8.5 C1.2 7.2 2.1 6.3 3.4 6.3 Z " +
            "M5.9 15.4 H11.3 V22.3 C11.3 22.9 10.8 23.4 10.2 23.4 H7.7 C7.1 23.4 6.6 22.9 6.5 22.3 Z " +
            "M12.7 15.4 H18.1 L17.5 22.3 C17.4 22.9 16.9 23.4 16.3 23.4 H13.8 C13.2 23.4 12.7 22.9 12.7 22.3 Z"
    )
}
