package com.dndcharacterhandler.presentation.dice

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dndcharacterhandler.domain.model.CustomDiceSkin
import com.dndcharacterhandler.domain.model.DiceFontIds
import com.dndcharacterhandler.domain.model.DicePattern
import com.dndcharacterhandler.presentation.theme.DnDTheme
import com.dndcharacterhandler.presentation.theme.LocalDesignTokens

/** Skins the dice workshop can make, each on the dice it would be thrown as. */
@Preview(showBackground = true, widthDp = 412, heightDp = 640)
@Composable
fun DiceSkinsPreview() {
    val skins = listOf(
        CustomDiceSkin(
            id = "marble", name = "Marble", bodyColor = 0xFFF2EBDD.toInt(), gloss = 0.4f,
            edgeColor = 0xFF5A5A62.toInt(), numberColor = 0xFF141217.toInt(), font = DiceFontIds.CINZEL_DECORATIVE,
            pattern = DicePattern.Marble(0xFF2A2A30.toInt(), seed = 7)
        ),
        CustomDiceSkin(
            id = "emerald", name = "Emerald and gold", bodyColor = 0xFF14524A.toInt(), gloss = 0.6f,
            edgeColor = 0xFFE0B548.toInt(), numberColor = 0xFFE0B548.toInt(), numberOutlineColor = 0xFF141217.toInt(),
            font = DiceFontIds.NEW_ROCKER, pattern = DicePattern.Marble(0xFFE0B548.toInt(), seed = 11)
        ),
        CustomDiceSkin(
            id = "nebula", name = "Nebula", bodyColor = 0xFF0B0A14.toInt(), gloss = 0.3f, edgeWidth = 0f,
            edgeColor = 0xFF8AD1E8.toInt(), numberColor = 0xFFF2EBDD.toInt(), font = DiceFontIds.UNCIAL_ANTIQUA,
            pattern = DicePattern.Nebula(0xFF6A3FA0.toInt(), 0xFF8AD1E8.toInt(), seed = 5)
        ),
        CustomDiceSkin(
            id = "glass", name = "Ruby glass", bodyColor = 0xFFB3202A.toInt(), bodyOpacity = 0.5f, gloss = 0.8f,
            edgeColor = 0xFFE88FB4.toInt(), numberColor = 0xFFF2EBDD.toInt(), font = DiceFontIds.BUNGEE, numberScale = 0.9f
        )
    )
    DnDTheme {
        Column(
            modifier = Modifier
                .background(LocalDesignTokens.current.colors.background.radialEnd)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            skins.forEach { skin ->
                Text(skin.name, style = MaterialTheme.typography.titleMedium, color = LocalDesignTokens.current.colors.text.primary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    listOf(DieType.D4, DieType.D6, DieType.D8, DieType.D10, DieType.D12, DieType.D20).forEach { type ->
                        DieIcon(type = type, look = DiceLook.Custom(skin), modifier = Modifier.size(56.dp))
                    }
                }
            }
        }
    }
}
