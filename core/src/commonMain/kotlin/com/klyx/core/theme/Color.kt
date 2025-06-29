package com.klyx.core.theme

import androidx.annotation.FloatRange
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
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

fun Color.blend(
    color: Color,
    @FloatRange(from = 0.0, to = 1.0) fraction: Float = 0.2f
): Color {
    val inverseFraction = 1 - fraction

    val r = this.red * inverseFraction + color.red * fraction
    val g = this.green * inverseFraction + color.green * fraction
    val b = this.blue * inverseFraction + color.blue * fraction
    val a = this.alpha * inverseFraction + color.alpha * fraction

    return Color(r, g, b, a)
}

@Composable
fun Color.harmonizeWithPrimary(
    @FloatRange(
        from = 0.0,
        to = 1.0
    ) fraction: Float = 0.2f
): Color = blend(MaterialTheme.colorScheme.primary, fraction)

