package com.dndcharacterhandler.presentation.theme

import androidx.compose.ui.graphics.Color
import com.dndcharacterhandler.data.json.optObject
import com.dndcharacterhandler.data.json.parseJsonObject
import com.dndcharacterhandler.domain.model.AppTheme
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.math.roundToInt

/**
 * Every theme in design_tokens.json is a whole palette (see CLAUDE.md, "Themes"): the same roles as the
 * classic one, every value a hex colour, and the app reads each theme's own values.
 */
class DesignTokenThemesTest {
    private val text = listOf("src/main/assets/design_tokens.json", "app/src/main/assets/design_tokens.json")
        .map(::File).first { it.exists() }.readText()
    private val themes = parseJsonObject(text).optObject("themes")!!
    private val hex = Regex("^#([0-9A-Fa-f]{6}|[0-9A-Fa-f]{8})$")

    private fun colors(theme: AppTheme): JsonObject =
        themes.optObject(theme.key)?.optObject("colors") ?: error("design_tokens.json has no theme \"${theme.key}\"")

    @Test
    fun everyThemeHasTheClassicRoles() {
        val classic = colors(AppTheme.CLASSIC)
        val classicApp = classic.optObject("app")!!
        val problems = mutableListOf<String>()
        AppTheme.entries.forEach { theme ->
            val app = colors(theme).optObject("app")!!
            if (app.keys != classicApp.keys) problems += "${theme.key}: groups ${app.keys} ≠ ${classicApp.keys}"
            classicApp.keys.forEach { group ->
                val roles = app.optObject(group)?.keys.orEmpty()
                val expected = classicApp.optObject(group)!!.keys
                if (roles != expected) problems += "${theme.key}.$group: ${roles - expected} extra, ${expected - roles} missing"
            }
            val material = colors(theme).optObject("materialTheme")!!.keys
            val classicMaterial = classic.optObject("materialTheme")!!.keys
            if (!material.containsAll(classicMaterial)) problems += "${theme.key}.materialTheme misses ${classicMaterial - material}"
        }
        assertTrue(problems.joinToString("\n"), problems.isEmpty())
    }

    @Test
    fun everyColourIsHex() {
        val problems = mutableListOf<String>()
        AppTheme.entries.forEach { theme ->
            val colors = colors(theme)
            colors.optObject("materialTheme")!!.forEach { (role, value) ->
                if (!hex.matches(value.string())) problems += "${theme.key}.materialTheme.$role = $value"
            }
            colors.optObject("app")!!.forEach { (group, roles) ->
                roles.jsonObject.forEach { (role, value) ->
                    if (!hex.matches(value.string())) problems += "${theme.key}.$group.$role = $value"
                }
            }
        }
        assertTrue(problems.joinToString("\n"), problems.isEmpty())
    }

    @Test
    fun theAppReadsEveryThemesOwnPalette() {
        val set = parseDesignTokenSet(text)
        AppTheme.entries.forEach { theme ->
            val app = colors(theme).optObject("app")!!
            val palette = set.palette(theme)
            assertEquals(theme.key, app.colour("accent", "heal"), palette.colors.accent.heal)
            assertEquals(theme.key, app.colour("text", "primary"), palette.colors.text.primary)
            assertEquals(theme.key, app.colour("border", "muted"), palette.colors.border.muted)
            val material = colors(theme).optObject("materialTheme")!!
            assertEquals(theme.key, material.colour("primary"), palette.material.primary)
        }
    }

    @Test
    fun everyTransparencyIsAStepOfTheScale() {
        val alpha = parseDesignTokenSet(text).alpha
        assertEquals(AlphaTokens(faint = 0.15f, line = 0.3f, half = 0.5f, veil = 0.7f), alpha)
        val steps = listOf(alpha.faint, alpha.line, alpha.half, alpha.veil).map { (it * 255).roundToInt() }
        val problems = mutableListOf<String>()
        AppTheme.entries.forEach { theme ->
            val colors = colors(theme)
            val roles = colors.optObject("materialTheme")!!.map { (role, value) -> "materialTheme.$role" to value } +
                colors.optObject("app")!!.flatMap { (group, roles) -> roles.jsonObject.map { (role, value) -> "$group.$role" to value } }
            roles.forEach { (role, value) ->
                val raw = value.string().removePrefix("#")
                if (raw.length == 8 && raw.substring(0, 2).toInt(16) !in steps) problems += "${theme.key}.$role = $value"
            }
        }
        assertTrue("Off the scale (15 · 30 · 50 · 70 %):\n" + problems.joinToString("\n"), problems.isEmpty())
    }

    private fun JsonObject.colour(group: String, role: String): Color = optObject(group)!!.colour(role)

    private fun JsonObject.colour(role: String): Color {
        val raw = getValue(role).string().removePrefix("#")
        val parsed = raw.toLong(16)
        return Color(if (raw.length == 6) 0xFF000000L or parsed else parsed)
    }

    private fun kotlinx.serialization.json.JsonElement.string(): String = (this as JsonPrimitive).content
}
