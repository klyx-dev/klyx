package com.klyx.core.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.core.graphics.toColorInt
import com.klyx.core.cmd.Command
import com.klyx.core.cmd.CommandManager
import com.klyx.core.settings.SettingsManager
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.InputStream

/**
 * Data class representing a parsed Klyx theme
 */
data class KlyxTheme(
    val name: String,
    val appearance: String,
    val style: KlyxThemeStyle
)

fun KlyxTheme.isDark() = appearance == "dark"

/**
 * Data class representing the style section of a Klyx theme
 */
data class KlyxThemeStyle(
    // Core colors
    val background: Color?,
    val foreground: Color?,
    val accent: Color?,
    val border: Color?,
    val error: Color?,
    val warning: Color?,
    val success: Color?,
    val info: Color?,

    // Element colors
    val elementBackground: Color?,
    val elementHover: Color?,
    val elementActive: Color?,
    val elementSelected: Color?,
    val elementDisabled: Color?,

    // Ghost element colors
    val ghostElementBackground: Color?,
    val ghostElementHover: Color?,
    val ghostElementActive: Color?,
    val ghostElementSelected: Color?,
    val ghostElementDisabled: Color?,

    // Surface colors
    val surfaceBackground: Color?,
    val elevatedSurfaceBackground: Color?,

    // Text colors
    val text: Color?,
    val textAccent: Color?,
    val textMuted: Color?,
    val textDisabled: Color?,
    val textPlaceholder: Color?,

    // Border variants
    val borderVariant: Color?,
    val borderFocused: Color?,
    val borderSelected: Color?,
    val borderDisabled: Color?,
    val borderTransparent: Color?,

    // Status colors with backgrounds
    val errorBackground: Color?,
    val warningBackground: Color?,
    val successBackground: Color?,
    val infoBackground: Color?,

    // Editor specific colors
    val editorBackground: Color?,
    val editorForeground: Color?,
    val editorActiveLineBackground: Color?,

    // Gutter colors
    val gutterBackground: Color?,
    val gutterText: Color?,
    val gutterDivider: Color?,

    // Syntax highlighting colors
    val syntaxKeyword: Color?,
    val syntaxFunction: Color?,
    val syntaxString: Color?,
    val syntaxComment: Color?,
    val syntaxVariable: Color?,
    val syntaxNumber: Color?,
    val syntaxBoolean: Color?,
    val syntaxType: Color?,

    // Additional utility colors
    val dropTargetBackground: Color?,
    val searchMatchBackground: Color?,
    val linkTextHover: Color?
)

object ThemeManager {
    private var currentThemeFamily: JsonObject? by mutableStateOf(null)
    private var availableThemes: List<KlyxTheme> by mutableStateOf(emptyList())

    // Default fallback colors
    private val defaultLightColors = KlyxThemeStyle(
        background = Color(0xFFFFFFFF),
        foreground = Color(0xFF000000),
        accent = Color(0xFF007ACC),
        border = Color(0xFFE1E4E8),
        error = Color(0xFFD73A49),
        warning = Color(0xFFE36209),
        success = Color(0xFF28A745),
        info = Color(0xFF0366D6),
        elementBackground = Color(0xFFF6F8FA),
        elementHover = Color(0xFFF3F4F6),
        elementActive = Color(0xFFE1E4E8),
        elementSelected = Color(0xFFDDF4FF),
        elementDisabled = Color(0xFFF6F8FA),
        ghostElementBackground = Color.Transparent,
        ghostElementHover = Color(0x0F000000),
        ghostElementActive = Color(0x1A000000),
        ghostElementSelected = Color(0x1A007ACC),
        ghostElementDisabled = Color.Transparent,
        surfaceBackground = Color(0xFFFFFFFF),
        elevatedSurfaceBackground = Color(0xFFFFFFFF),
        text = Color(0xFF24292E),
        textAccent = Color(0xFF007ACC),
        textMuted = Color(0xFF6A737D),
        textDisabled = Color(0xFFBDC3C7),
        textPlaceholder = Color(0xFF959DA5),
        borderVariant = Color(0xFFF1F3F4),
        borderFocused = Color(0xFF007ACC),
        borderSelected = Color(0xFF007ACC),
        borderDisabled = Color(0xFFE1E4E8),
        borderTransparent = Color.Transparent,
        errorBackground = Color(0xFFFDF2F2),
        warningBackground = Color(0xFFFFFBF0),
        successBackground = Color(0xFFF0F9FF),
        infoBackground = Color(0xFFF6F8FA),
        editorBackground = Color(0xFFFFFFFF),
        editorForeground = Color(0xFF24292E),
        editorActiveLineBackground = Color(0xFFF6F8FA),
        gutterBackground = Color(0xFFF6F8FA),
        gutterText = Color(0xFF6A737D),
        gutterDivider = Color(0xFFE1E4E8),
        dropTargetBackground = Color(0x1A007ACC),
        searchMatchBackground = Color(0xFFFFDF5D),
        linkTextHover = Color(0xFF0366D6),
        syntaxKeyword = Color(0xFF0000FF),
        syntaxFunction = Color(0xFF795E26),
        syntaxString = Color(0xFFA31515),
        syntaxComment = Color(0xFF008000),
        syntaxVariable = Color(0xFF001080),
        syntaxNumber = Color(0xFF098658),
        syntaxBoolean = Color(0xFF0000FF),
        syntaxType = Color(0xFF267F99)
    )

    private val defaultDarkColors = KlyxThemeStyle(
        background = Color(0xFF0D1117),
        foreground = Color(0xFFC9D1D9),
        accent = Color(0xFF58A6FF),
        border = Color(0xFF30363D),
        error = Color(0xFFF85149),
        warning = Color(0xFFD29922),
        success = Color(0xFF3FB950),
        info = Color(0xFF58A6FF),
        elementBackground = Color(0xFF161B22),
        elementHover = Color(0xFF21262D),
        elementActive = Color(0xFF30363D),
        elementSelected = Color(0xFF0D419D),
        elementDisabled = Color(0xFF161B22),
        ghostElementBackground = Color.Transparent,
        ghostElementHover = Color(0x1AFFFFFF),
        ghostElementActive = Color(0x33FFFFFF),
        ghostElementSelected = Color(0x1A58A6FF),
        ghostElementDisabled = Color.Transparent,
        surfaceBackground = Color(0xFF0D1117),
        elevatedSurfaceBackground = Color(0xFF161B22),
        text = Color(0xFFC9D1D9),
        textAccent = Color(0xFF58A6FF),
        textMuted = Color(0xFF8B949E),
        textDisabled = Color(0xFF484F58),
        textPlaceholder = Color(0xFF6E7681),
        borderVariant = Color(0xFF21262D),
        borderFocused = Color(0xFF58A6FF),
        borderSelected = Color(0xFF58A6FF),
        borderDisabled = Color(0xFF30363D),
        borderTransparent = Color.Transparent,
        errorBackground = Color(0xFF2C1A1A),
        warningBackground = Color(0xFF2C2419),
        successBackground = Color(0xFF1A2C1A),
        infoBackground = Color(0xFF1A1F2C),
        editorBackground = Color(0xFF0D1117),
        editorForeground = Color(0xFFC9D1D9),
        editorActiveLineBackground = Color(0xFF161B22),
        gutterBackground = Color(0xFF161B22),
        gutterText = Color(0xFF8B949E),
        gutterDivider = Color(0xFF30363D),
        dropTargetBackground = Color(0x1A58A6FF),
        searchMatchBackground = Color(0xFF3C2F00),
        linkTextHover = Color(0xFF79C0FF),
        syntaxKeyword = Color(0xFF569CD6),
        syntaxFunction = Color(0xFFDCDCAA),
        syntaxString = Color(0xFFCE9178),
        syntaxComment = Color(0xFF6A9955),
        syntaxVariable = Color(0xFF9CDCFE),
        syntaxNumber = Color(0xFFB5CEA8),
        syntaxBoolean = Color(0xFF569CD6),
        syntaxType = Color(0xFF4EC9B0)
    )

    /**
     * Load and parse a theme family from extension
     */
    fun loadThemeFamily(themeInput: InputStream?): Result<Unit> {
        return try {
            val themeJson = themeInput?.use { it.readBytes() } ?: return Result.failure(Exception("Theme file not found"))

            val json = Json.parseToJsonElement(String(themeJson)).jsonObject
            currentThemeFamily = json

            val parsedThemes = parseThemes(json)
            CommandManager.addCommand(*parsedThemes.map {
                Command("Change Theme: ${it.name} (${it.appearance})") {
                    SettingsManager.updateSettings(SettingsManager.settings.value.copy(theme = it.name))
                }
            }.toTypedArray())
            availableThemes += parsedThemes

            println("Theme family loaded successfully: ${json["name"]?.jsonPrimitive?.content}")
            println("Available themes: ${availableThemes.map { "${it.name} (${it.appearance})" }}")

            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            currentThemeFamily = null
            availableThemes = emptyList()
            Result.failure(e)
        }
    }

    /**
     * Parse themes from the JSON theme family
     */
    private fun parseThemes(json: JsonObject): List<KlyxTheme> {
        return try {
            val themesArray = json["themes"]?.jsonArray ?: return emptyList()

            themesArray.mapNotNull { themeElement ->
                val themeObj = themeElement.jsonObject
                val name = themeObj["name"]?.jsonPrimitive?.content ?: return@mapNotNull null
                val appearance = themeObj["appearance"]?.jsonPrimitive?.content ?: return@mapNotNull null
                val styleObj = themeObj["style"]?.jsonObject ?: return@mapNotNull null

                KlyxTheme(
                    name = name,
                    appearance = appearance,
                    style = parseThemeStyle(styleObj)
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private fun parseThemeStyle(styleObj: JsonObject): KlyxThemeStyle {
        val syntaxObj = styleObj["syntax"]?.jsonObject

        return KlyxThemeStyle(
            // Core colors
            background = parseColor(styleObj["background"]),
            foreground = parseColor(styleObj["foreground"]) ?: parseColor(styleObj["text"]),
            accent = parseColor(styleObj["accent"]) ?: parseColor(styleObj["text.accent"]),
            border = parseColor(styleObj["border"]),
            error = parseColor(styleObj["error"]),
            warning = parseColor(styleObj["warning"]),
            success = parseColor(styleObj["success"]),
            info = parseColor(styleObj["info"]),

            // Element colors
            elementBackground = parseColor(styleObj["element.background"]),
            elementHover = parseColor(styleObj["element.hover"]),
            elementActive = parseColor(styleObj["element.active"]),
            elementSelected = parseColor(styleObj["element.selected"]),
            elementDisabled = parseColor(styleObj["element.disabled"]),

            // Ghost element colors
            ghostElementBackground = parseColor(styleObj["ghost_element.background"]),
            ghostElementHover = parseColor(styleObj["ghost_element.hover"]),
            ghostElementActive = parseColor(styleObj["ghost_element.active"]),
            ghostElementSelected = parseColor(styleObj["ghost_element.selected"]),
            ghostElementDisabled = parseColor(styleObj["ghost_element.disabled"]),

            // Surface colors
            surfaceBackground = parseColor(styleObj["surface.background"]),
            elevatedSurfaceBackground = parseColor(styleObj["elevated_surface.background"]),

            // Text colors
            text = parseColor(styleObj["text"]),
            textAccent = parseColor(styleObj["text.accent"]),
            textMuted = parseColor(styleObj["text.muted"]),
            textDisabled = parseColor(styleObj["text.disabled"]),
            textPlaceholder = parseColor(styleObj["text.placeholder"]),

            // Border variants
            borderVariant = parseColor(styleObj["border.variant"]),
            borderFocused = parseColor(styleObj["border.focused"]),
            borderSelected = parseColor(styleObj["border.selected"]),
            borderDisabled = parseColor(styleObj["border.disabled"]),
            borderTransparent = parseColor(styleObj["border.transparent"]),

            // Status backgrounds
            errorBackground = parseColor(styleObj["error.background"]),
            warningBackground = parseColor(styleObj["warning.background"]),
            successBackground = parseColor(styleObj["success.background"]),
            infoBackground = parseColor(styleObj["info.background"]),

            // Editor colors
            editorBackground = parseColor(styleObj["editor.background"]),
            editorForeground = parseColor(styleObj["editor.foreground"]),
            editorActiveLineBackground = parseColor(styleObj["editor.active_line.background"]),

            // Gutter colors
            gutterBackground = parseColor(styleObj["editor.gutter.background"]),
            gutterText = parseColor(styleObj["editor.gutter.text"]),
            gutterDivider = parseColor(styleObj["editor.gutter.divider"]),

            // Syntax highlighting colors
            syntaxKeyword = parseColor(syntaxObj?.get("keyword")?.jsonObject?.get("color")),
            syntaxFunction = parseColor(syntaxObj?.get("function")?.jsonObject?.get("color")),
            syntaxString = parseColor(syntaxObj?.get("string")?.jsonObject?.get("color")),
            syntaxComment = parseColor(syntaxObj?.get("comment")?.jsonObject?.get("color")),
            syntaxVariable = parseColor(syntaxObj?.get("variable")?.jsonObject?.get("color")),
            syntaxNumber = parseColor(syntaxObj?.get("number")?.jsonObject?.get("color")),
            syntaxBoolean = parseColor(syntaxObj?.get("boolean")?.jsonObject?.get("color")),
            syntaxType = parseColor(syntaxObj?.get("type")?.jsonObject?.get("color")),

            // Utility colors
            dropTargetBackground = parseColor(styleObj["drop_target.background"]),
            searchMatchBackground = parseColor(styleObj["search.match_background"]),
            linkTextHover = parseColor(styleObj["link_text.hover"])
        )
    }

    private fun parseColor(colorElement: JsonElement?): Color? {
        return try {
            val colorStr = colorElement?.jsonPrimitive?.content ?: return null
            if (colorStr.isBlank()) return null

            // different color formats
            when {
                colorStr.startsWith("#") -> Color(colorStr.toColorInt())
                colorStr.startsWith("rgb(") -> parseRgbColor(colorStr)
                colorStr.startsWith("rgba(") -> parseRgbaColor(colorStr)
                colorStr.startsWith("hsl(") -> parseHslColor(colorStr)
                else -> Color(if (colorStr == "transparent") 0x00000000 else colorStr.toColorInt())
            }
        } catch (e: Exception) {
            println("Failed to parse color: ${colorElement?.jsonPrimitive?.content}, error: ${e.message}")
            null
        }
    }

    private fun parseRgbColor(rgb: String): Color {
        val values = rgb.removePrefix("rgb(").removeSuffix(")").split(",").map { it.trim().toInt() }
        return Color(values[0], values[1], values[2])
    }

    private fun parseRgbaColor(rgba: String): Color {
        val values = rgba.removePrefix("rgba(").removeSuffix(")").split(",")
        val r = values[0].trim().toInt()
        val g = values[1].trim().toInt()
        val b = values[2].trim().toInt()
        val a = values[3].trim().toFloat()
        return Color(r, g, b, (a * 255).toInt())
    }

    private fun parseHslColor(hsl: String): Color {
        // Basic HSL parsing
        val values = hsl.removePrefix("hsl(").removeSuffix(")").split(",")
        val h = values[0].trim().toFloat()
        val s = values[1].trim().removeSuffix("%").toFloat() / 100f
        val l = values[2].trim().removeSuffix("%").toFloat() / 100f
        return hslToRgb(h, s, l)
    }

    private fun hslToRgb(h: Float, s: Float, l: Float): Color {
        val c = (1f - kotlin.math.abs(2f * l - 1f)) * s
        val x = c * (1f - kotlin.math.abs((h / 60f) % 2f - 1f))
        val m = l - c / 2f

        val (r1, g1, b1) = when {
            h < 60f -> Triple(c, x, 0f)
            h < 120f -> Triple(x, c, 0f)
            h < 180f -> Triple(0f, c, x)
            h < 240f -> Triple(0f, x, c)
            h < 300f -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }

        return Color(
            ((r1 + m) * 255).toInt(),
            ((g1 + m) * 255).toInt(),
            ((b1 + m) * 255).toInt()
        )
    }

    /**
     * Get available theme names for the given appearance
     */
    fun getAvailableThemes(appearance: String): List<KlyxTheme> {
        return availableThemes.filter { it.appearance == appearance }
    }

    /**
     * Get all available themes
     */
    fun getAllAvailableThemes(): List<KlyxTheme> {
        return availableThemes
    }

    /**
     * Get theme by name and appearance
     */
    fun getTheme(name: String, appearance: String): KlyxTheme? {
        return availableThemes.find { it.name == name && it.appearance == appearance }
    }

    /**
     * Get theme by name
     */
    fun getThemeByName(name: String): KlyxTheme? {
        return availableThemes.find { it.name == name }
    }

    /**
     * Convert KlyxTheme to Material3 ColorScheme
     */
    fun getColorScheme(darkTheme: Boolean, themeName: String? = null): ColorScheme {
        val appearance = if (darkTheme) "dark" else "light"

        val theme = if (themeName != null) {
            getThemeByName(themeName)
        } else {
            availableThemes.find { it.appearance == appearance }
        }

        if (theme == null) {
            println("No theme found, using defaults for $appearance theme")
            return createDefaultColorScheme(darkTheme)
        }

        println("Using theme: ${theme.name} (${theme.appearance})")
        return createColorSchemeFromKlyxTheme(theme, darkTheme)
    }

    private fun createDefaultColorScheme(darkTheme: Boolean): ColorScheme {
        val defaults = if (darkTheme) defaultDarkColors else defaultLightColors
        return createColorSchemeFromStyle(defaults, darkTheme)
    }

    private fun createColorSchemeFromKlyxTheme(theme: KlyxTheme, darkTheme: Boolean): ColorScheme {
        val fallback = if (darkTheme) defaultDarkColors else defaultLightColors
        return createColorSchemeFromStyle(theme.style, darkTheme, fallback)
    }

    private fun createColorSchemeFromStyle(
        style: KlyxThemeStyle,
        darkTheme: Boolean,
        fallback: KlyxThemeStyle? = null
    ): ColorScheme {

        fun Color?.orFallback(fallbackColor: Color?): Color {
            return this ?: fallbackColor ?: if (darkTheme) Color.White else Color.Black
        }

        val primary = style.accent.orFallback(fallback?.accent)
        val background = style.background.orFallback(fallback?.background)
        val surface = style.surfaceBackground.orFallback(fallback?.surfaceBackground)
        val onBackground = style.foreground.orFallback(fallback?.foreground)
        val onSurface = style.text.orFallback(fallback?.text)
        val error = style.error.orFallback(fallback?.error)
        val outline = style.border.orFallback(fallback?.border)

        return if (darkTheme) {
            darkColorScheme(
                primary = primary,
                onPrimary = if (isLightColor(primary)) Color.Black else Color.White,
                primaryContainer = primary.copy(alpha = 0.24f),
                onPrimaryContainer = primary,

                secondary = style.textAccent.orFallback(primary),
                onSecondary = Color.White,
                secondaryContainer = style.elementSelected.orFallback(primary.copy(alpha = 0.24f)),
                onSecondaryContainer = style.textAccent.orFallback(primary),

                tertiary = style.info.orFallback(fallback?.info),
                onTertiary = Color.White,
                tertiaryContainer = style.infoBackground.orFallback(style.info?.copy(alpha = 0.24f)),
                onTertiaryContainer = style.info.orFallback(fallback?.info),

                error = error,
                onError = if (isLightColor(error)) Color.Black else Color.White,
                errorContainer = style.errorBackground.orFallback(error.copy(alpha = 0.24f)),
                onErrorContainer = error,

                background = background,
                onBackground = onBackground,

                surface = surface,
                onSurface = onSurface,
                surfaceVariant = style.elementBackground.orFallback(surface.copy(alpha = 0.8f)),
                onSurfaceVariant = onSurface.copy(alpha = 0.7f),

                outline = outline,
                outlineVariant = style.borderVariant.orFallback(outline.copy(alpha = 0.5f)),

                scrim = Color.Black.copy(alpha = 0.32f),
                inverseSurface = onSurface,
                inverseOnSurface = surface,
                inversePrimary = primary,

                surfaceDim = surface.copy(alpha = 0.87f),
                surfaceBright = style.elevatedSurfaceBackground.orFallback(surface),
                surfaceContainerLowest = surface.copy(alpha = 0.4f),
                surfaceContainerLow = surface.copy(alpha = 0.6f),
                surfaceContainer = style.elementBackground.orFallback(surface),
                surfaceContainerHigh = style.elementHover.orFallback(surface.copy(alpha = 1.2f)),
                surfaceContainerHighest = style.elementActive.orFallback(surface.copy(alpha = 1.5f))
            )
        } else {
            lightColorScheme(
                primary = primary,
                onPrimary = if (isLightColor(primary)) Color.Black else Color.White,
                primaryContainer = primary.copy(alpha = 0.12f),
                onPrimaryContainer = primary,

                secondary = style.textAccent.orFallback(primary),
                onSecondary = Color.White,
                secondaryContainer = style.elementSelected.orFallback(primary.copy(alpha = 0.12f)),
                onSecondaryContainer = style.textAccent.orFallback(primary),

                tertiary = style.info.orFallback(fallback?.info),
                onTertiary = Color.White,
                tertiaryContainer = style.infoBackground.orFallback(style.info?.copy(alpha = 0.12f)),
                onTertiaryContainer = style.info.orFallback(fallback?.info),

                error = error,
                onError = if (isLightColor(error)) Color.Black else Color.White,
                errorContainer = style.errorBackground.orFallback(error.copy(alpha = 0.12f)),
                onErrorContainer = error,

                background = background,
                onBackground = onBackground,

                surface = surface,
                onSurface = onSurface,
                surfaceVariant = style.elementBackground.orFallback(surface.copy(alpha = 0.5f)),
                onSurfaceVariant = onSurface.copy(alpha = 0.7f),

                outline = outline,
                outlineVariant = style.borderVariant.orFallback(outline.copy(alpha = 0.5f)),

                scrim = Color.Black.copy(alpha = 0.32f),
                inverseSurface = onSurface,
                inverseOnSurface = surface,
                inversePrimary = primary,

                surfaceDim = surface.copy(alpha = 0.87f),
                surfaceBright = style.elevatedSurfaceBackground.orFallback(surface),
                surfaceContainerLowest = surface.copy(alpha = 0.5f),
                surfaceContainerLow = surface.copy(alpha = 0.75f),
                surfaceContainer = style.elementBackground.orFallback(surface),
                surfaceContainerHigh = style.elementHover.orFallback(surface.copy(alpha = 1.25f)),
                surfaceContainerHighest = style.elementActive.orFallback(surface.copy(alpha = 1.5f))
            )
        }
    }

    /**
     * Determine if a color is light or dark for contrast calculations
     */
    private fun isLightColor(color: Color): Boolean {
        val luminance = (0.299 * color.red + 0.587 * color.green + 0.114 * color.blue)
        return luminance > 0.5
    }

    /**
     * Get theme author and name
     */
    fun getThemeInfo(): Pair<String?, String?> {
        val family = currentThemeFamily ?: return null to null
        val author = family["author"]?.jsonPrimitive?.content
        val name = family["name"]?.jsonPrimitive?.content
        return author to name
    }
}
