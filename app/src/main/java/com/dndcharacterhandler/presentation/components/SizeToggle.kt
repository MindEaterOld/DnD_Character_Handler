package com.dndcharacterhandler.presentation.components

import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.background
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
import com.dndcharacterhandler.domain.dnd5e.model.CreatureSize
import com.dndcharacterhandler.presentation.localization.text

/**
 * The character's size: a gnome, a vampire and a rock golem, each drawn at its size, in a stat card's
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
        SizeOptions(
            selected = selected,
            onSelect = onSelect,
            sizes = sizes,
            modifier = Modifier.padding(start = 6.dp, end = 6.dp, top = 10.dp, bottom = 6.dp)
        )
    }
}

/** The toggle's cells in a row: each size's figure over its name. */
@Composable
private fun SizeOptions(
    selected: CreatureSize?,
    onSelect: (CreatureSize) -> Unit,
    sizes: List<CreatureSize>,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        sizes.forEach { size ->
            val isSelected = size == selected
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(toggleFill(isSelected))
                    .selectable(selected = isSelected, role = Role.RadioButton) { onSelect(size) }
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

/** The size's figure: the toggle's cell, the size's icon on the biography (it changes with the size), the height pop-up's. */
val CreatureSize.figure: ImageVector
    get() = when (this) {
        CreatureSize.SMALL -> GnomeFigure
        CreatureSize.MEDIUM -> VampireFigure
        CreatureSize.LARGE -> GolemFigure
    }

/** A filled figure on a 24×24 grid; the fill is a placeholder colour that `Icon` tints. */
private fun figure(name: String, pathData: String): ImageVector =
    ImageVector.Builder(name = name, defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)
        .addPath(pathData = addPathNodes(pathData), fill = SolidColor(Color.Black))
        .build()

// The figures are from Game Icons (game-icons.net), CC BY 3.0: "Bad gnome" by Cathelineau, "Vampire cape"
// and "Rock golem" by Delapouite (owner's choice from boards, 2026-10-06). Their paths are filled, scaled to
// the 24 grid, as tall as fits and standing on its bottom edge, so the three compare by their icon's size.

/** A gnome: a pointed hat, a bushy beard, a stride. */
private val GnomeFigure: ImageVector by lazy {
    figure(
        "Gnome",
        "M16.36 10.44C17.11 11.27 17.81 12.3 18.23 13.35C17.92 13.19 17.57 13.08 17.17 13.06L16.2 13" +
            "L16.84 13.73C17.52 14.49 17.9 15.19 18.12 15.93C17.91 15.87 17.69 15.85 17.46 15.85L16.86 15.86" +
            "L17.1 16.41C17.54 17.42 17.7 18.46 17.58 19.55C17.43 19.46 17.26 19.38 17.05 19.33L17.02 19.32" +
            "L16.97 19.31C16.93 19.31 16.89 19.31 16.85 19.31C16.77 19.31 16.68 19.3 16.59 19.31" +
            "C15.79 19.34 15.02 19.71 14.43 20.49C14.26 20.43 14.07 20.38 13.89 20.34L13.35 20.24L13.41 20.79" +
            "C13.45 21.23 13.28 21.95 13.04 22.57C12.92 22.32 12.78 22.04 12.63 21.75" +
            "C12.47 21.46 12.28 21.17 12.03 20.94C11.78 20.7 11.42 20.54 11.03 20.55" +
            "C10.75 20.56 10.49 20.66 10.31 20.83C10.12 20.99 10.01 21.21 9.96 21.43" +
            "C9.9 21.67 9.9 21.91 9.92 22.13C9.72 22.09 9.55 22 9.38 21.86C9.11 21.61 8.85 21.17 8.68 20.46" +
            "L8.45 19.61L7.09 21.54C6.82 20.85 6.73 20.09 6.91 19.17L7.18 17.74L5.7 19.37" +
            "C5.52 18.87 5.54 18.44 5.43 17.67L5.33 16.92L4.33 17.8C4.28 17.55 4.29 17.32 4.36 17.09" +
            "C4.44 16.66 4.66 16.19 4.87 15.62L5.15 14.82L4.35 15.1C3.99 15.22 3.66 15.46 3.36 15.68" +
            "C3.3 15.72 3.28 15.74 3.24 15.78C3.29 15.54 3.37 15.32 3.48 15.11" +
            "C3.75 14.54 4.18 13.96 4.7 13.47L5.13 13.08L4.62 12.82C4.2 12.6 3.75 12.61 3.27 12.66" +
            "C3.17 12.67 3.07 12.69 2.97 12.71C3.37 12.28 3.89 11.76 4.46 11.28" +
            "C4.49 11.25 4.52 11.23 4.54 11.22C5.4 11.78 6.34 12.12 7.36 12.3" +
            "C7.31 12.37 7.26 12.44 7.21 12.51C6.56 13.46 6.05 14.45 6.07 15.42L6.08 15.9L6.55 15.81" +
            "C7.33 15.68 8.02 15.49 8.61 15.27C8.98 15.55 9.46 15.8 10.08 15.88" +
            "C11.51 16.08 12.41 16 13.09 15.77C13.75 15.54 14.15 15.16 14.5 14.88L14.61 14.79L15.38 11.68" +
            "L14.49 12.3C13.65 12.88 12.76 13.1 11.79 13.2C12.02 12.96 12.21 12.74 12.38 12.54" +
            "C12.5 12.39 12.48 12.16 12.51 11.93C13.78 11.59 15.08 11.08 16.36 10.44ZM14.77 2.49" +
            "C14.88 2.49 14.99 2.5 15.09 2.51C15.7 2.68 15.97 2.86 16.28 3.31L16.46 3.33" +
            "C16.86 3.37 17.16 3.55 17.46 3.82C17.74 4.07 18.09 4.34 18.36 4.72" +
            "C18.76 5.29 18.93 5.81 19.18 6.36C19.78 6.48 20.34 6.88 20.66 7.27" +
            "C21.01 7.7 21.25 8.24 21.33 8.66C21.4 9.14 21.33 9.43 21.2 9.57C21.07 9.71 20.86 9.8 20.41 9.71" +
            "C19.84 9.58 19.2 9.09 18.74 8.44C18.29 7.8 18.03 7.03 18.07 6.48L18.09 6.12L17.73 6.05" +
            "C16.53 5.64 15.66 4.76 15.14 3.8C14.49 4.19 13.74 4.18 13.02 4.15" +
            "C12.89 4.15 12.75 4.14 12.62 4.14L13.32 4.84C14.32 5.83 14.86 6.43 15.03 6.81" +
            "C15.13 7.01 15.14 7.13 15.12 7.28C15.11 7.32 15.09 7.39 15.07 7.45" +
            "C13.01 8.3 10.14 8.33 7.02 8.07C7.32 7.03 8.1 6.03 9.13 5.15C10.37 4.12 11.95 3.27 13.32 2.76" +
            "C13.37 2.75 13.41 2.73 13.47 2.72C13.91 2.6 14.33 2.49 14.77 2.49ZM15.04 8.33 15.81 9.8" +
            "C14.61 10.39 13.4 10.84 12.24 11.16C12.17 11.1 12.11 11.05 12.04 11" +
            "C12.5 10.22 12.92 9.45 12.59 9.04C11.53 9.42 10.72 9.92 10.62 10.8" +
            "C10.57 10.82 10.51 10.85 10.46 10.89C10.26 11.02 10.14 11.15 9.97 11.29L10.49 11.92" +
            "C10.7 11.74 10.83 11.61 10.9 11.56C11.04 11.48 11.44 11.64 11.63 11.72" +
            "C11.65 11.73 11.67 11.74 11.69 11.74C11.73 11.76 11.73 11.92 11.66 12.12" +
            "C10.85 13.08 9.7 14.32 6.94 14.91C7.07 14.33 7.42 13.65 7.88 12.97" +
            "C8.45 12.14 9.18 11.32 9.81 10.59C9.95 10.43 10.14 10.07 10.25 9.75L8.78 9.01" +
            "C11.1 9.1 13.25 8.97 15.04 8.33ZM6.58 8.86C7.19 8.91 7.8 8.95 8.38 8.99" +
            "C8.1 9.79 8.37 10.22 8.74 10.57C8.47 10.89 8.2 11.21 7.93 11.55C6.93 11.43 6 11.14 5.18 10.65Z" +
            "M22.46 14.52C23.09 14.71 23.39 15.09 23.62 15.58C23.74 15.84 23.82 16.11 23.9 16.39L22.51 15.67" +
            "L22.61 16.44C22.66 16.88 22.53 17.28 22.25 17.66L21.46 16.34L21.09 16.65" +
            "C20.76 16.94 20.63 17.33 20.47 17.75C20.44 17.85 20.4 17.94 20.36 18.04" +
            "C20.26 17.78 20.19 17.49 20.14 17.2C20.06 16.6 20.14 16.02 20.38 15.68" +
            "C21.02 15.04 21.71 14.74 22.46 14.52ZM1.7 5.93C1.99 6.01 2.27 6.09 2.48 6.21" +
            "C2.81 6.38 3.06 6.65 3.22 7.36L2.38 9.05C1.73 9.27 0.98 9.31 0.25 9.26L1.44 8.23L1.06 7.92" +
            "C0.76 7.68 0.46 7.47 0.1 7.3C0.24 7.2 0.37 7.11 0.49 7.04C0.78 6.88 1.02 6.81 1.31 6.85" +
            "L1.83 6.91ZM3.05 21.98C3.65 22.29 4.35 22.39 5.11 22.31L4.88 23.32" +
            "C4.35 23.78 3.65 23.94 2.89 23.89C2.27 23.86 1.62 23.69 1.04 23.46" +
            "C1.24 23.2 1.39 22.96 1.58 22.76C1.85 22.48 2.22 22.22 3.05 21.98ZM19.02 11.8H19.14" +
            "C19.51 11.82 19.8 11.95 20.08 12.16C20.53 12.48 21.03 13.13 21.69 13.93" +
            "C21.11 14.14 20.53 14.45 19.99 14.92C19.82 14.76 19.63 14.62 19.42 14.5L19.36 14.25" +
            "C19.2 13.43 18.86 12.62 18.43 11.87C18.66 11.82 18.85 11.8 19.02 11.8ZM18.29 21.66" +
            "C18.63 21.98 19 22.28 19.3 22.57C19.59 22.85 19.78 23.11 19.88 23.36" +
            "C19.49 23.5 19.1 23.65 18.63 23.7C18.06 23.74 17.33 23.63 16.31 23.09L16.27 22.28Z" +
            "M14.11 13.46 13.89 14.33C13.55 14.6 13.28 14.84 12.83 15C12.31 15.17 11.54 15.25 10.19 15.08" +
            "C9.92 15.04 9.7 14.97 9.51 14.88Q10.26 14.49 10.82 14.07C11 14.06 11.19 14.06 11.37 14.05" +
            "C12.29 13.99 13.22 13.86 14.11 13.46ZM3.91 7.87C4.61 8.06 5.17 8.36 5.68 8.68L4.5 10.2" +
            "C4.45 10.23 4.41 10.27 4.38 10.3C4.05 9.92 3.66 9.57 3.16 9.31L3.78 8.04ZM4.95 19.68" +
            "C5.03 19.88 5.13 20.08 5.26 20.3L5.54 20.77L6.02 20.24C6.03 20.45 6.04 20.67 6.08 20.87" +
            "L5.46 21.42C4.65 21.6 3.99 21.54 3.44 21.27C3.48 20.89 3.77 20.49 4.17 20.17" +
            "C4.4 19.97 4.66 19.82 4.87 19.72C4.9 19.7 4.93 19.69 4.95 19.68ZM16.63 20.11" +
            "C16.7 20.11 16.79 20.13 16.88 20.13C17.19 20.21 17.32 20.34 17.45 20.53" +
            "C17.54 20.65 17.61 20.81 17.71 20.98L16.08 21.48C15.91 21.16 15.63 20.91 15.28 20.74" +
            "C15.68 20.3 16.12 20.13 16.63 20.11Z"
    )
}

/** A vampire in a high-collared cape, standing straight. */
private val VampireFigure: ImageVector by lazy {
    figure(
        "Vampire",
        "M13.5 5.21 14.26 5.6C15.17 6.05 16.21 6.26 16.97 6.54C17.36 6.68 17.66 6.85 17.84 7.02" +
            "C18.02 7.2 18.11 7.35 18.11 7.65C18.11 9.57 17.31 11.92 16.59 14.28" +
            "C15.87 16.66 15.21 19.08 15.66 21.29L15.67 21.35L15.69 21.4C16.15 22.32 16.86 22.91 17.62 23.34" +
            "C17.82 23.45 18 23.55 18.2 23.65C16.96 23.64 15.71 23.44 14.79 22.52L14.46 22.19L14.14 22.52" +
            "C13.73 22.93 13.33 23.32 12.95 23.6C12.78 23.74 12.61 23.83 12.46 23.9V11.14" +
            "C12.46 11.09 12.54 10.79 12.69 10.46C12.84 10.12 13.06 9.73 13.31 9.33" +
            "C13.82 8.53 14.43 7.71 14.79 7.36L15.24 6.9L14.67 6.62C13.91 6.23 13.57 5.87 13.41 5.64" +
            "C13.34 5.53 13.3 5.44 13.29 5.4C13.37 5.35 13.43 5.28 13.5 5.21ZM10.5 5.21" +
            "C10.57 5.28 10.63 5.35 10.71 5.4C10.7 5.44 10.66 5.53 10.59 5.64C10.43 5.87 10.09 6.23 9.33 6.62" +
            "L8.76 6.9L9.21 7.36C9.57 7.71 10.18 8.53 10.69 9.33C10.94 9.73 11.16 10.12 11.31 10.46" +
            "C11.46 10.79 11.54 11.09 11.54 11.14V23.9C11.39 23.83 11.22 23.74 11.05 23.6" +
            "C10.67 23.32 10.27 22.93 9.86 22.52L9.54 22.19L9.21 22.52C8.29 23.44 7.04 23.64 5.8 23.65" +
            "C6 23.55 6.18 23.45 6.38 23.35C7.14 22.91 7.85 22.32 8.31 21.4L8.33 21.35L8.34 21.29" +
            "C8.79 19.08 8.13 16.66 7.41 14.28C6.69 11.92 5.89 9.57 5.89 7.65C5.89 7.35 5.98 7.2 6.16 7.02" +
            "C6.34 6.85 6.64 6.68 7.03 6.54C7.79 6.26 8.83 6.05 9.74 5.6ZM12 0.1" +
            "C12.42 0.1 12.8 0.32 13.12 0.76C13.43 1.18 13.64 1.81 13.64 2.51" +
            "C13.64 3.21 13.43 3.85 13.12 4.27C12.8 4.7 12.42 4.93 12 4.93C11.58 4.93 11.2 4.7 10.88 4.27" +
            "C10.57 3.85 10.36 3.21 10.36 2.51C10.36 1.81 10.57 1.18 10.88 0.76C11.2 0.32 11.58 0.1 12 0.1Z" +
            "M16.88 1.01C16.61 1.61 16.33 2.22 16.01 2.81C15.52 3.69 14.95 4.43 14.26 4.77L13.65 5.07" +
            "C13.73 5 13.8 4.91 13.86 4.82C14.31 4.21 14.56 3.4 14.56 2.51C14.56 2.22 14.54 1.93 14.48 1.64" +
            "C15.22 1.49 16 1.28 16.88 1.01ZM7.12 1.01C8 1.28 8.78 1.49 9.52 1.64" +
            "C9.46 1.92 9.44 2.21 9.44 2.51C9.44 3.4 9.69 4.21 10.14 4.82C10.2 4.91 10.27 5 10.35 5.07" +
            "L9.74 4.77C9.05 4.43 8.48 3.69 7.99 2.81C7.67 2.22 7.39 1.61 7.12 1.01ZM11.53 5.85H11.54V8.95" +
            "C11.52 8.92 11.5 8.87 11.46 8.84C11.08 8.22 10.65 7.61 10.26 7.15" +
            "C10.82 6.8 11.16 6.46 11.35 6.16C11.42 6.06 11.47 5.96 11.53 5.85ZM12.46 5.85H12.47" +
            "C12.53 5.96 12.58 6.06 12.65 6.16C12.84 6.46 13.18 6.8 13.74 7.15" +
            "C13.35 7.61 12.92 8.22 12.54 8.84C12.5 8.87 12.48 8.92 12.46 8.95Z"
    )
}

/** A rock golem: boulders for a body, a head sunk between the shoulders. */
private val GolemFigure: ImageVector by lazy {
    figure(
        "Golem",
        "M6.64 17.47 8.74 17.77 9.53 19.7 9.85 17.93 12 18.24 17.36 17.47 18.08 23.9H16.71L16.12 22.71" +
            "L15.53 23.9H13.93L12 20.03L10.07 23.9H5.92ZM9.41 7.53 11.54 8.96V17.23L6.6 16.53L5.56 12.18" +
            "L7.85 13.32L10.49 12.44L10.21 11.56L7.91 12.33L5.33 11.03L5.8 7.82L6.14 7.77ZM10.54 0.1H13.46" +
            "L14.21 0.85L13.36 2.11L14.75 1.4L16.44 3.09L15.74 6.59H8.26L7.56 3.09Z" +
            "M14.59 7.53 17.86 7.77 18.2 7.82 18.67 11.03 16.09 12.33 13.79 11.56 13.51 12.44 15.24 13.02 " +
            "16.21 14.78 17.22 12.79 18.44 12.18 17.4 16.53 12.46 17.23" +
            "V8.96Z" +
            "M19.18 7.97 20.15 8.1 20.67 9.12 20.89 8.21 22.33 8.41 23.88 10.47 23.26 14.75 22.56 15.45 19.88 14.91" +
            "V11.96ZM4.84 7.97 4.12 11.97V14.92L0.85 15.57L0.52 13.23L1.62 12.33L0.31 11.79L0.12 10.47" +
            "L1.67 8.41ZM4.18 15.86 4.9 22.36 2.46 22.97 1.02 16.48Z" +
            "M19.82 15.86 22.98 16.48 21.58 22.77 20.67 21.32 20.26 22.65 19.1 22.36ZM17.25 4.43 21.43 6.52" +
            "V7.34L16.7 6.67ZM6.75 4.43 7.31 6.67 2.57 7.34V6.52L4.91 5.34L6.15 5.88L5.62 5Z" +
            "M15.14 3.37 14.47 3.7 12.82 4.53 13.03 5.41H15.14ZM8.86 3.37V5.41H10.97L11.18 4.53Z"
    )
}
