package com.klyx.editor

import androidx.compose.runtime.Stable
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Constraints

@Stable
internal class LineLayoutCache {
    private val cache = mutableMapOf<String, TextLayoutResult>()
    private val maxSize = 200

    fun TextMeasurer.getOrMeasure(
        line: String,
        style: TextStyle = TextStyle.Default,
        constraints: Constraints = Constraints()
    ): TextLayoutResult {
        val key = "${line.hashCode()}_${style.hashCode()}_${constraints.hashCode()}"

        return cache.getOrPut(key) {
            if (cache.size >= maxSize) {
                // remove oldest entry (simple LRU-like behavior)
                cache.remove(cache.keys.first())
            }

            measure(
                text = AnnotatedString(line),
                style = style,
                constraints = constraints,
                softWrap = false
            )
        }
    }

    fun clear() {
        cache.clear()
    }
}
