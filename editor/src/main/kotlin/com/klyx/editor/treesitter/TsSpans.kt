package com.klyx.editor.treesitter

import io.github.rosemoe.sora.lang.styling.Span
import io.github.rosemoe.sora.lang.styling.SpanFactory
import io.github.rosemoe.sora.lang.styling.Spans
import io.github.rosemoe.sora.text.CharPosition

class TsSpans(
    private val precomputedStructure: Array<List<Span>>,
    private var linesTotal: Int,
    @Volatile private var theme: TsTheme
) : Spans {

    fun updateTheme(newTheme: TsTheme) {
        this.theme = newTheme
    }

    override fun getLineCount(): Int = linesTotal
    override fun supportsModify(): Boolean = false
    override fun modify(): Spans.Modifier = throw UnsupportedOperationException()
    override fun adjustOnInsert(start: CharPosition?, end: CharPosition?) {}
    override fun adjustOnDelete(start: CharPosition?, end: CharPosition?) {}

    override fun read() = object : Spans.Reader {
        private var lineSpansReference = emptyList<Span>()

        override fun moveToLine(line: Int) {
            lineSpansReference = if (line in precomputedStructure.indices) {
                precomputedStructure[line]
            } else {
                listOf(SpanFactory.obtain(0, theme.normalTextStyle))
            }
        }

        override fun getSpanCount(): Int = lineSpansReference.size
        override fun getSpanAt(index: Int): Span = lineSpansReference[index]
        override fun getSpansOnLine(line: Int): MutableList<Span> {
            return if (line in precomputedStructure.indices) {
                precomputedStructure[line].toMutableList()
            } else {
                mutableListOf(SpanFactory.obtain(0, theme.normalTextStyle))
            }
        }
    }
}
