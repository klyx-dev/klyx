package com.klyx.terminal.emulator

/**
 * Color scheme for a terminal with default colors, which may be overridden (and then reset) from the shell using
 * Operating System Control (OSC) sequences.
 *
 * @see TerminalColors
 */
class TerminalColorScheme {
    companion object {
        /**
         * http://upload.wikimedia.org/wikipedia/en/1/15/Xterm_256color_chart.svg, but with blue color brighter.
         */
        private val DEFAULT_COLORSCHEME: IntArray = intArrayOf(
            // 16 original colors. First 8 are dim.
            0xff000000u.toInt(), // black
            0xffcd0000u.toInt(), // dim red
            0xff00cd00u.toInt(), // dim green
            0xffcdcd00u.toInt(), // dim yellow
            0xff6495edu.toInt(), // dim blue
            0xffcd00cdu.toInt(), // dim magenta
            0xff00cdcdu.toInt(), // dim cyan
            0xffe5e5e5u.toInt(), // dim white
            // Second 8 are bright:
            0xff7f7f7fu.toInt(), // medium grey
            0xffff0000u.toInt(), // bright red
            0xff00ff00u.toInt(), // bright green
            0xffffff00u.toInt(), // bright yellow
            0xff5c5cffu.toInt(), // light blue
            0xffff00ffu.toInt(), // bright magenta
            0xff00ffffu.toInt(), // bright cyan
            0xffffffffu.toInt(), // bright white

            // 216 color cube, six shades of each color:
            0xff000000u.toInt(),
            0xff00005fu.toInt(),
            0xff000087u.toInt(),
            0xff0000afu.toInt(),
            0xff0000d7u.toInt(),
            0xff0000ffu.toInt(),
            0xff005f00u.toInt(),
            0xff005f5fu.toInt(),
            0xff005f87u.toInt(),
            0xff005fafu.toInt(),
            0xff005fd7u.toInt(),
            0xff005fffu.toInt(),
            0xff008700u.toInt(),
            0xff00875fu.toInt(),
            0xff008787u.toInt(),
            0xff0087afu.toInt(),
            0xff0087d7u.toInt(),
            0xff0087ffu.toInt(),
            0xff00af00u.toInt(),
            0xff00af5fu.toInt(),
            0xff00af87u.toInt(),
            0xff00afafu.toInt(),
            0xff00afd7u.toInt(),
            0xff00afffu.toInt(),
            0xff00d700u.toInt(),
            0xff00d75fu.toInt(),
            0xff00d787u.toInt(),
            0xff00d7afu.toInt(),
            0xff00d7d7u.toInt(),
            0xff00d7ffu.toInt(),
            0xff00ff00u.toInt(),
            0xff00ff5fu.toInt(),
            0xff00ff87u.toInt(),
            0xff00ffafu.toInt(),
            0xff00ffd7u.toInt(),
            0xff00ffffu.toInt(),
            0xff5f0000u.toInt(),
            0xff5f005fu.toInt(),
            0xff5f0087u.toInt(),
            0xff5f00afu.toInt(),
            0xff5f00d7u.toInt(),
            0xff5f00ffu.toInt(),
            0xff5f5f00u.toInt(),
            0xff5f5f5fu.toInt(),
            0xff5f5f87u.toInt(),
            0xff5f5fafu.toInt(),
            0xff5f5fd7u.toInt(),
            0xff5f5fffu.toInt(),
            0xff5f8700u.toInt(),
            0xff5f875fu.toInt(),
            0xff5f8787u.toInt(),
            0xff5f87afu.toInt(),
            0xff5f87d7u.toInt(),
            0xff5f87ffu.toInt(),
            0xff5faf00u.toInt(),
            0xff5faf5fu.toInt(),
            0xff5faf87u.toInt(),
            0xff5fafafu.toInt(),
            0xff5fafd7u.toInt(),
            0xff5fafffu.toInt(),
            0xff5fd700u.toInt(),
            0xff5fd75fu.toInt(),
            0xff5fd787u.toInt(),
            0xff5fd7afu.toInt(),
            0xff5fd7d7u.toInt(),
            0xff5fd7ffu.toInt(),
            0xff5fff00u.toInt(),
            0xff5fff5fu.toInt(),
            0xff5fff87u.toInt(),
            0xff5fffafu.toInt(),
            0xff5fffd7u.toInt(),
            0xff5fffffu.toInt(),
            0xff870000u.toInt(),
            0xff87005fu.toInt(),
            0xff870087u.toInt(),
            0xff8700afu.toInt(),
            0xff8700d7u.toInt(),
            0xff8700ffu.toInt(),
            0xff875f00u.toInt(),
            0xff875f5fu.toInt(),
            0xff875f87u.toInt(),
            0xff875fafu.toInt(),
            0xff875fd7u.toInt(),
            0xff875fffu.toInt(),
            0xff878700u.toInt(),
            0xff87875fu.toInt(),
            0xff878787u.toInt(),
            0xff8787afu.toInt(),
            0xff8787d7u.toInt(),
            0xff8787ffu.toInt(),
            0xff87af00u.toInt(),
            0xff87af5fu.toInt(),
            0xff87af87u.toInt(),
            0xff87afafu.toInt(),
            0xff87afd7u.toInt(),
            0xff87afffu.toInt(),
            0xff87d700u.toInt(),
            0xff87d75fu.toInt(),
            0xff87d787u.toInt(),
            0xff87d7afu.toInt(),
            0xff87d7d7u.toInt(),
            0xff87d7ffu.toInt(),
            0xff87ff00u.toInt(),
            0xff87ff5fu.toInt(),
            0xff87ff87u.toInt(),
            0xff87ffafu.toInt(),
            0xff87ffd7u.toInt(),
            0xff87ffffu.toInt(),
            0xffaf0000u.toInt(),
            0xffaf005fu.toInt(),
            0xffaf0087u.toInt(),
            0xffaf00afu.toInt(),
            0xffaf00d7u.toInt(),
            0xffaf00ffu.toInt(),
            0xffaf5f00u.toInt(),
            0xffaf5f5fu.toInt(),
            0xffaf5f87u.toInt(),
            0xffaf5fafu.toInt(),
            0xffaf5fd7u.toInt(),
            0xffaf5fffu.toInt(),
            0xffaf8700u.toInt(),
            0xffaf875fu.toInt(),
            0xffaf8787u.toInt(),
            0xffaf87afu.toInt(),
            0xffaf87d7u.toInt(),
            0xffaf87ffu.toInt(),
            0xffafaf00u.toInt(),
            0xffafaf5fu.toInt(),
            0xffafaf87u.toInt(),
            0xffafafafu.toInt(),
            0xffafafd7u.toInt(),
            0xffafafffu.toInt(),
            0xffafd700u.toInt(),
            0xffafd75fu.toInt(),
            0xffafd787u.toInt(),
            0xffafd7afu.toInt(),
            0xffafd7d7u.toInt(),
            0xffafd7ffu.toInt(),
            0xffafff00u.toInt(),
            0xffafff5fu.toInt(),
            0xffafff87u.toInt(),
            0xffafffafu.toInt(),
            0xffafffd7u.toInt(),
            0xffafffffu.toInt(),
            0xffd70000u.toInt(),
            0xffd7005fu.toInt(),
            0xffd70087u.toInt(),
            0xffd700afu.toInt(),
            0xffd700d7u.toInt(),
            0xffd700ffu.toInt(),
            0xffd75f00u.toInt(),
            0xffd75f5fu.toInt(),
            0xffd75f87u.toInt(),
            0xffd75fafu.toInt(),
            0xffd75fd7u.toInt(),
            0xffd75fffu.toInt(),
            0xffd78700u.toInt(),
            0xffd7875fu.toInt(),
            0xffd78787u.toInt(),
            0xffd787afu.toInt(),
            0xffd787d7u.toInt(),
            0xffd787ffu.toInt(),
            0xffd7af00u.toInt(),
            0xffd7af5fu.toInt(),
            0xffd7af87u.toInt(),
            0xffd7afafu.toInt(),
            0xffd7afd7u.toInt(),
            0xffd7afffu.toInt(),
            0xffd7d700u.toInt(),
            0xffd7d75fu.toInt(),
            0xffd7d787u.toInt(),
            0xffd7d7afu.toInt(),
            0xffd7d7d7u.toInt(),
            0xffd7d7ffu.toInt(),
            0xffd7ff00u.toInt(),
            0xffd7ff5fu.toInt(),
            0xffd7ff87u.toInt(),
            0xffd7ffafu.toInt(),
            0xffd7ffd7u.toInt(),
            0xffd7ffffu.toInt(),
            0xffff0000u.toInt(),
            0xffff005fu.toInt(),
            0xffff0087u.toInt(),
            0xffff00afu.toInt(),
            0xffff00d7u.toInt(),
            0xffff00ffu.toInt(),
            0xffff5f00u.toInt(),
            0xffff5f5fu.toInt(),
            0xffff5f87u.toInt(),
            0xffff5fafu.toInt(),
            0xffff5fd7u.toInt(),
            0xffff5fffu.toInt(),
            0xffff8700u.toInt(),
            0xffff875fu.toInt(),
            0xffff8787u.toInt(),
            0xffff87afu.toInt(),
            0xffff87d7u.toInt(),
            0xffff87ffu.toInt(),
            0xffffaf00u.toInt(),
            0xffffaf5fu.toInt(),
            0xffffaf87u.toInt(),
            0xffffafafu.toInt(),
            0xffffafd7u.toInt(),
            0xffffafffu.toInt(),
            0xffffd700u.toInt(),
            0xffffd75fu.toInt(),
            0xffffd787u.toInt(),
            0xffffd7afu.toInt(),
            0xffffd7d7u.toInt(),
            0xffffd7ffu.toInt(),
            0xffffff00u.toInt(),
            0xffffff5fu.toInt(),
            0xffffff87u.toInt(),
            0xffffffafu.toInt(),
            0xffffffd7u.toInt(),
            0xffffffffu.toInt(),

            // 24 grey scale ramp:
            0xff080808u.toInt(),
            0xff121212u.toInt(),
            0xff1c1c1cu.toInt(),
            0xff262626u.toInt(),
            0xff303030u.toInt(),
            0xff3a3a3au.toInt(),
            0xff444444u.toInt(),
            0xff4e4e4eu.toInt(),
            0xff585858u.toInt(),
            0xff626262u.toInt(),
            0xff6c6c6cu.toInt(),
            0xff767676u.toInt(),
            0xff808080u.toInt(),
            0xff8a8a8au.toInt(),
            0xff949494u.toInt(),
            0xff9e9e9eu.toInt(),
            0xffa8a8a8u.toInt(),
            0xffb2b2b2u.toInt(),
            0xffbcbcbcu.toInt(),
            0xffc6c6c6u.toInt(),
            0xffd0d0d0u.toInt(),
            0xffdadadau.toInt(),
            0xffe4e4e4u.toInt(),
            0xffeeeeeeu.toInt(),

            // COLOR_INDEX_DEFAULT_FOREGROUND, COLOR_INDEX_DEFAULT_BACKGROUND and COLOR_INDEX_DEFAULT_CURSOR:
            0xffffffffu.toInt(),
            0xff000000u.toInt(),
            0xffffffffu.toInt()
        )
    }

    val defaultColors = IntArray(TextStyle.NUM_INDEXED_COLORS)

    init {
        reset()
    }

    private fun reset() {
        DEFAULT_COLORSCHEME.copyInto(
            destination = defaultColors,
            destinationOffset = 0,
            startIndex = 0,
            endIndex = TextStyle.NUM_INDEXED_COLORS
        )
    }

    fun updateWith(props: Map<String, String>) {
        reset()
        var cursorPropExists = false
        for ((key, value) in props) {
            var colorIndex: Int

            when (key) {
                "foreground" -> colorIndex = TextStyle.COLOR_INDEX_FOREGROUND
                "background" -> colorIndex = TextStyle.COLOR_INDEX_BACKGROUND
                "cursor" -> {
                    colorIndex = TextStyle.COLOR_INDEX_CURSOR
                    cursorPropExists = true
                }

                else -> {
                    if (key.startsWith("color")) {
                        try {
                            colorIndex = key.substring(5).toInt()
                        } catch (e: NumberFormatException) {
                            throw IllegalArgumentException("Invalid property: '$key'", e)
                        }
                    } else {
                        throw IllegalArgumentException("Invalid property: '$key'")
                    }
                }
            }

            val color = TerminalColors.parse(value)
            if (color == 0) throw IllegalArgumentException("Property '$key' has invalid color: '$value'")

            defaultColors[colorIndex] = color
        }

        if (!cursorPropExists) {
            setCursorColorForBackground()
        }
    }

    /**
     * If the "cursor" color is not set by user, we need to decide on the appropriate color that will
     * be visible on the current terminal background. White will not be visible on light backgrounds
     * and black won't be visible on dark backgrounds. So we find the perceived brightness of the
     * background color and if its below the threshold (too dark), we use white cursor and if its
     * above (too bright), we use black cursor.
     */
    fun setCursorColorForBackground() {
        val backgroundColor = defaultColors[TextStyle.COLOR_INDEX_BACKGROUND]
        val brightness = TerminalColors.getPerceivedBrightnessOfColor(backgroundColor)
        if (brightness > 0) {
            if (brightness < 130) {
                defaultColors[TextStyle.COLOR_INDEX_CURSOR] = 0xffffffffu.toInt()
            } else {
                defaultColors[TextStyle.COLOR_INDEX_CURSOR] = 0xff000000u.toInt()
            }
        }
    }
}
