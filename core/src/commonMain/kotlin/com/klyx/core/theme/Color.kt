package com.klyx.core.theme

import androidx.compose.ui.graphics.Color

private val colorNameMap = mapOf(
    "black" to Color.Black,
    "darkgray" to Color.DarkGray,
    "gray" to Color.Gray,
    "lightgray" to Color.LightGray,
    "white" to Color.White,
    "red" to Color.Red,
    "green" to Color.Green,
    "blue" to Color.Blue,
    "yellow" to Color.Yellow,
    "cyan" to Color.Cyan,
    "magenta" to Color.Magenta,

    "aqua" to Color(0xFF00FFFF),
    "fuchsia" to Color(0xFFFF00FF),
    "darkgrey" to Color.DarkGray,
    "grey" to Color.Gray,
    "lightgrey" to Color.LightGray,
    "lime" to Color(0xFF00FF00),
    "maroon" to Color(0xFF800000),
    "navy" to Color(0xFF000080),
    "olive" to Color(0xFF808000),
    "purple" to Color(0xFF800080),
    "silver" to Color(0xFFC0C0C0),
    "teal" to Color(0xFF008080),
    "transparent" to Color.Transparent
)

fun String.toColor(): Color {
    if (this[0] == '#') {
        var color = substring(1).toLongOrNull(16) ?: throwUnknownColor()

        if (length == 7) {
            color = color or 0x00000000ff000000
        } else if (length != 9) throwUnknownColor()

        return Color(color)
    } else {
        colorNameMap[this]?.let { return it }
    }
    throwUnknownColor()
}

private fun throwUnknownColor(): Nothing {
    throw IllegalArgumentException("Unknown color")
}
