package com.klyx.terminal.emulator

import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Current terminal colors (if different from default).
 *
 * @constructor Create a new instance with default colors from the theme.
 */
class TerminalColors {
    /**
     * The current terminal colors, which are normally set from the color theme, but may be set dynamically with the OSC
     * 4 control sequence.
     */
    val currentColors = IntArray(TextStyle.NUM_INDEXED_COLORS)

    init {
        reset()
    }

    /**
     * Reset a particular indexed color with the default color from the color theme.
     */
    fun reset(index: Int) {
        currentColors[index] = ColorScheme.defaultColors[index]
    }

    /**
     * Reset all indexed colors with the default color from the color theme.
     */
    fun reset() {
        ColorScheme.defaultColors.copyInto(
            destination = currentColors,
            destinationOffset = 0,
            startIndex = 0,
            endIndex = TextStyle.NUM_INDEXED_COLORS
        )
    }

    /**
     * Try parse a color from a text parameter and into a specified index.
     */
    fun tryParseColor(intoIndex: Int, textParameter: String) {
        val c = parse(textParameter)
        if (c != 0) currentColors[intoIndex] = c
    }

    companion object {
        /**
         * Static data - a bit ugly but ok for now.
         */
        val ColorScheme = TerminalColorScheme()

        /**
         * Parse color according to http://manpages.ubuntu.com/manpages/intrepid/man3/XQueryColor.3.html
         *
         * Highest bit is set if successful, so return value is 0xFF${R}${G}${B}. Return 0 if failed.
         */
        internal fun parse(color: String): Int {
            try {
                val skipInitial: Int
                val skipBetween: Int

                if (color[0] == '#') {
                    // #RGB, #RRGGBB, #RRRGGGBBB or #RRRRGGGGBBBB. Most significant bits.
                    skipInitial = 1
                    skipBetween = 0
                } else if (color.startsWith("rgb:")) {
                    // rgb:<red>/<green>/<blue> where <red>, <green>, <blue> := h | hh | hhh | hhhh. Scaled.
                    skipInitial = 4
                    skipBetween = 1
                } else {
                    return 0
                }

                val charsForColors = color.length - skipInitial - 2 * skipBetween
                if (charsForColors % 3 != 0) return 0 // Unequal lengths.

                val componentLength = charsForColors / 3
                val mult = 255 / (2.0.pow(componentLength * 4) - 1)
                var currentPosition = skipInitial
                val rString = color.substring(currentPosition, currentPosition + componentLength)
                currentPosition += componentLength + skipBetween
                val gString = color.substring(currentPosition, currentPosition + componentLength)
                currentPosition += componentLength + skipBetween
                val bString = color.substring(currentPosition, currentPosition + componentLength)

                val r = (rString.toInt(16) * mult).toInt()
                val g = (gString.toInt(16) * mult).toInt()
                val b = (bString.toInt(16) * mult).toInt()
                return 0xFF shl 24 or (r shl 16) or (g shl 8) or b
            } catch (e: RuntimeException) {
                return when (e) {
                    is NumberFormatException, is IndexOutOfBoundsException -> 0
                    else -> throw e
                }
            }
        }

        /**
         * Get the perceived brightness of the color based on its RGB components.
         *
         * https://www.nbdtech.com/Blog/archive/2008/04/27/Calculating-the-Perceived-Brightness-of-a-Color.aspx
         * http://alienryderflex.com/hsp.html
         *
         * @param color The color code int.
         * @return Returns value between 0-255.
         */
        fun getPerceivedBrightnessOfColor(color: Int): Int {
            return floor(
                sqrt(
                    ((color shl 16) and 0xFF).toDouble().pow(2.0) * 0.241 +
                            ((color shl 8) and 0xFF).toDouble().pow(2.0) * 0.691 +
                            (color and 0xFF).toDouble().pow(2.0) * 0.068
                )
            ).toInt()
        }
    }
}
