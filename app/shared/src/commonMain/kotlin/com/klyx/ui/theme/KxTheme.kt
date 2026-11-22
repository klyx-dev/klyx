package com.klyx.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

data class KxTheme(
    val name: String,
    val background: Color,
    val foreground: Color,
    val cursorColor: Color,
    val cursorText: Color,
    val selectionBackground: Color,
    val selectionForeground: Color,
    val palette: List<Color>,
) {
    companion object {
        fun parse(name: String, content: String): KxTheme {
            val props = mutableMapOf<String, String>()
            val paletteColors = arrayOfNulls<Color>(16)

            content.lineSequence()
                .map { it.trim() }
                .filter { it.isNotEmpty() && !it.startsWith("#") }
                .forEach { line ->
                    val eqIndex = line.indexOf('=')
                    if (eqIndex < 0) return@forEach
                    val key = line.substring(0, eqIndex).trim()
                    val value = line.substring(eqIndex + 1).trim()

                    if (key == "palette") {
                        val sepIndex = value.indexOf('=')
                        if (sepIndex >= 0) {
                            val idx = value.substring(0, sepIndex).toIntOrNull() ?: return@forEach
                            if (idx in 0..15) {
                                paletteColors[idx] = parseHexColor(value.substring(sepIndex + 1))
                            }
                        }
                    } else {
                        props[key] = value
                    }
                }

            val bg = parseHexColor(props["background"] ?: "#000000")
            val fg = parseHexColor(props["foreground"] ?: "#ffffff")

            return KxTheme(
                name = name,
                background = bg,
                foreground = fg,
                cursorColor = parseHexColor(props["cursor-color"] ?: props["foreground"] ?: "#ffffff"),
                cursorText = parseHexColor(props["cursor-text"] ?: props["background"] ?: "#000000"),
                selectionBackground = parseHexColor(props["selection-background"] ?: props["foreground"] ?: "#ffffff"),
                selectionForeground = parseHexColor(props["selection-foreground"] ?: props["background"] ?: "#000000"),
                palette = List(16) { paletteColors[it] ?: defaultTerminalPaletteColor(it) },
            )
        }

        private fun parseHexColor(hex: String): Color {
            val h = hex.trimStart('#')
            val argb = when (h.length) {
                6 -> h.toLongOrNull(16)?.let { 0xFF000000 or it }
                8 -> h.toLongOrNull(16)
                else -> null
            }
            return Color((argb ?: 0xFF000000).toInt())
        }
    }
}

private fun Color.mix(other: Color, fraction: Float): Color {
    val inv = 1f - fraction
    return Color(
        red = this.red * inv + other.red * fraction,
        green = this.green * inv + other.green * fraction,
        blue = this.blue * inv + other.blue * fraction,
        alpha = this.alpha * inv + other.alpha * fraction,
    )
}

fun KxTheme.toColorPalette(): KxColorPalette {
    val isDark = background.luminance() < 0.5f
    val white = Color(0xFFFFFFFF)
    val black = Color(0xFF000000)
    val contrast = if (isDark) white else black

    // Panels: lighter touch on light themes so pale sage/cream backgrounds
    // don't get muddied; keep the existing weight on dark themes.
    val surface = background.mix(contrast, if (isDark) 0.15f else 0.06f)
    val surfaceVariant = background.mix(contrast, if (isDark) 0.04f else 0.02f)
    // Borders need more punch on light themes, the eye is less forgiving
    // of low-contrast outlines on bright backgrounds.
    val border = background.mix(contrast, if (isDark) 0.22f else 0.28f)

    val textPrimary = foreground
    val textSecondary = foreground.mix(background, 0.28f)
    val textMuted = foreground.mix(background, 0.5f)

    val accent = foreground.mix(background, if (isDark) 0.15f else 0.08f)
    val accentSecondary = palette[4].mix(background, if (isDark) 0.15f else 0.35f)
    val onAccent = background

    val disabledSurface = surface.mix(background, 0.5f)
    val disabledText = textMuted

    return KxColorPalette(
        name = this.name,
        background = background,
        surface = surface,
        surfaceVariant = surfaceVariant,
        border = border,
        textPrimary = textPrimary,
        textSecondary = textSecondary,
        textMuted = textMuted,
        accent = accent,
        accentSecondary = accentSecondary,
        error = palette[1],
        success = palette[2],
        warning = palette[3],
        onAccent = onAccent,
        disabledSurface = disabledSurface,
        disabledText = disabledText,
    )
}

private fun defaultTerminalPaletteColor(index: Int): Color {
    if (index in DEFAULT_ANSI_COLORS.indices) {
        return DEFAULT_ANSI_COLORS[index]
    }

    return when (index) {
        in 16..231 -> {
            val cubeIndex = index - 16
            val red = XTERM_CUBE_LEVELS[cubeIndex / 36]
            val green = XTERM_CUBE_LEVELS[(cubeIndex / 6) % 6]
            val blue = XTERM_CUBE_LEVELS[cubeIndex % 6]
            Color(red = red / 255f, green = green / 255f, blue = blue / 255f)
        }

        in 232..255 -> {
            val level = 8 + (index - 232) * 10
            Color(red = level / 255f, green = level / 255f, blue = level / 255f)
        }

        else -> Color(0xFF000000)
    }
}

private val DEFAULT_ANSI_COLORS = listOf(
    Color(0xFF000000),
    Color(0xFF800000),
    Color(0xFF008000),
    Color(0xFF808000),
    Color(0xFF000080),
    Color(0xFF800080),
    Color(0xFF008080),
    Color(0xFFC0C0C0),
    Color(0xFF808080),
    Color(0xFFFF0000),
    Color(0xFF00FF00),
    Color(0xFFFFFF00),
    Color(0xFF0000FF),
    Color(0xFFFF00FF),
    Color(0xFF00FFFF),
    Color(0xFFFFFFFF),
)

private val XTERM_CUBE_LEVELS = intArrayOf(0, 95, 135, 175, 215, 255)

