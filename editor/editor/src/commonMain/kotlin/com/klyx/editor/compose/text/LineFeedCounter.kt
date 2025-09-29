package com.klyx.editor.compose.text

class LineFeedCounter(text: String) {
    // line feeds
    private val lineFeedsOffsets = mutableListOf<Int>()

    init {
        for (i in 0..<text.length) {
            if (text.codePointAt(i) == CharCode.LineFeed) {
                lineFeedsOffsets.add(i)
            }
        }
    }

    fun findLineFeedCountBeforeOffset(offset: Int): Int {
        var min = 0
        var max = lineFeedsOffsets.size - 1

        if (max == -1) {
            // no line feeds
            return 0
        }

        if (offset <= lineFeedsOffsets[0]) {
            // before first line feed
            return 0
        }

        while (min < max) {
            val mid = (min + max) shr 1

            if (lineFeedsOffsets[mid] >= offset) {
                max = mid - 1
            } else {
                if (lineFeedsOffsets[mid + 1] >= offset) {
                    // bingo!
                    min = mid
                    max = mid
                } else {
                    min = mid + 1
                }
            }
        }
        return min + 1
    }
}

