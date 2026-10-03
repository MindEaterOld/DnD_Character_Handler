package com.dndcharacterhandler.presentation.components  // пакет того файла, рядом с чьими композаблами доска

// ВРЕМЕННЫЙ файл доски вариантов (/ui-layout): отрендерить, удалить, пересобрать. Не коммитить.

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dndcharacterhandler.data.localization.LocalizationRepository
import com.dndcharacterhandler.domain.model.AppLanguage
import com.dndcharacterhandler.presentation.localization.LocalStrings
import com.dndcharacterhandler.presentation.localization.text
import com.dndcharacterhandler.presentation.theme.DnDTheme
import com.dndcharacterhandler.presentation.theme.LocalDesignTokens

@OptIn(ExperimentalLayoutApi::class)
@Preview(widthDp = 880, heightDp = 360)
@Composable
private fun VariantBoard() {
    val context = LocalContext.current
    // Настоящие тексты из localization.json, а не карта превью.
    val strings = remember { LocalizationRepository(context).getStrings(AppLanguage.RUSSIAN) }
    val variants: List<Pair<String, @Composable () -> Unit>> = listOf(
        "A  сейчас" to {
            StatCardRow { MiniStatCard(label = text("stat_card_armor_class"), value = "15", modifier = Modifier.weight(1f)) }
        },
        "B  с иконкой" to {
            StatCardRow {
                MiniStatCard(
                    label = text("stat_card_armor_class"),
                    value = "15",
                    modifier = Modifier.weight(1f),
                    icon = { MiniStatCardIcon(Icons.Outlined.Shield) }
                )
            }
        }
    )
    CompositionLocalProvider(LocalStrings provides strings) {
        DnDTheme {
            ScreenBackground {
                val colors = LocalDesignTokens.current.colors
                FlowRow(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    maxItemsInEachRow = 2
                ) {
                    variants.forEach { (caption, content) ->
                        // Плитка шириной в экран 412dp, с полями экрана 24.
                        Column(modifier = Modifier.width(412.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(caption, style = MaterialTheme.typography.labelMedium, color = colors.accent.inspiration)
                            Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)) { content() }
                        }
                    }
                }
            }
        }
    }
}
