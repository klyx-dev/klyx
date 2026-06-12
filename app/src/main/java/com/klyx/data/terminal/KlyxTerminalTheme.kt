package com.klyx.data.terminal

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.klyx.terminal.emulator.TerminalColors

object KlyxTerminalTheme {
    private fun darkProps(backgroundHex: String): Map<String, String> = mapOf(
        "background" to backgroundHex,
        "foreground" to "#ABB2BF",
        "cursor" to "#61AFEF",

        "color0" to "#282C34", // Ansi Black
        "color1" to "#E06C75", // Ansi Red
        "color2" to "#98C379", // Ansi Green
        "color3" to "#E5C07B", // Ansi Yellow
        "color4" to "#61AFEF", // Ansi Blue
        "color5" to "#C678DD", // Ansi Magenta
        "color6" to "#56B6C2", // Ansi Cyan
        "color7" to "#ABB2BF", // Ansi White

        "color8" to "#636D83", // Ansi Bright Black
        "color9" to "#EA858B", // Ansi Bright Red
        "color10" to "#AAD581", // Ansi Bright Green
        "color11" to "#FFD885", // Ansi Bright Yellow
        "color12" to "#85C1FF", // Ansi Bright Blue
        "color13" to "#D398EB", // Ansi Bright Magenta
        "color14" to "#6ED5DE", // Ansi Bright Cyan
        "color15" to "#FAFAFF", // Ansi Bright White
    )

    private fun lightProps(backgroundHex: String): Map<String, String> = mapOf(
        "background" to backgroundHex,
        "foreground" to "#2A2C33",
        "cursor" to "#2F5AF3",

        "color0" to "#555555", // Ansi Black
        "color1" to "#DE3E35", // Ansi Red
        "color2" to "#3F953A", // Ansi Green
        "color3" to "#D2B67C", // Ansi Yellow
        "color4" to "#2F5AF3", // Ansi Blue
        "color5" to "#950095", // Ansi Magenta
        "color6" to "#2B6927", // Ansi Cyan
        "color7" to "#BBBBBB", // Ansi White

        "color8" to "#000000", // Ansi Bright Black
        "color9" to "#DE3E35", // Ansi Bright Red
        "color10" to "#3F953A", // Ansi Bright Green
        "color11" to "#D2B67C", // Ansi Bright Yellow
        "color12" to "#2F5AF3", // Ansi Bright Blue
        "color13" to "#A00095", // Ansi Bright Magenta
        "color14" to "#3F953A", // Ansi Bright Cyan
        "color15" to "#FFFFFF", // Ansi Bright White
    )

    fun apply(isDark: Boolean, background: Color) {
        val hex = background.toArgb().let { "#%06X".format(it and 0x00FFFFFF) }
        val props = if (isDark) darkProps(hex) else lightProps(hex)
        TerminalColors.ColorScheme.updateWith(props)
    }
}
