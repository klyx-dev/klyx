package com.klyx.editor.compose.renderer

import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Constraints

internal data class TextCacheKey(
    val text: String,
    val fontSizeValue: Float,
    val fontFamilyName: String?
)

internal data class LineNumberCacheKey(
    val lineNum: Int,
    val fontSizeValue: Float,
    val fontFamilyName: String?
)

internal class LayoutCache {
    private val textCache = mutableMapOf<TextCacheKey, TextLayoutResult>()
    private val lineNumberCache = mutableMapOf<LineNumberCacheKey, TextLayoutResult>()

    fun getOrMeasureText(
        key: TextCacheKey,
        measurer: TextMeasurer,
        style: TextStyle
    ): TextLayoutResult {
        return textCache.getOrPut(key) {
            measurer.measure(
                text = key.text,
                softWrap = false,
                constraints = Constraints(maxWidth = Constraints.Infinity),
                style = style
            )
        }
    }

    fun getOrMeasureLineNumber(
        key: LineNumberCacheKey,
        measurer: TextMeasurer,
        style: TextStyle,
        width: Int
    ): TextLayoutResult {
        return lineNumberCache.getOrPut(key) {
            measurer.measure(
                text = key.lineNum.toString(),
                style = style,
                constraints = Constraints(minWidth = width + 5)
            )
        }
    }

    fun clear() {
        textCache.clear()
        lineNumberCache.clear()
    }

    fun clearStaleEntries(visibleRange: IntRange) {
        lineNumberCache.keys.removeAll {
            it.lineNum !in visibleRange
        }
    }
}
