package com.klyx.editor.compose.text.buffer

import kotlinx.serialization.Serializable

@Serializable
internal data class LineStarts(
    val lineStarts: MutableList<Int>,
    val cr: Int,
    val lf: Int,
    val crlf: Int,
    val isBasicASCII: Boolean
)

internal fun CharSequence.computeLineStartOffsets(): MutableList<Int> {
    val r = ArrayList<Int>(length / 16) // heuristic capacity
    r.add(0)

    var i = 0
    while (i < length) {
        val ch = this[i]
        if (ch == '\r') {
            if (i + 1 < length && this[i + 1] == '\n') {
                r.add(i + 2)
                i += 2
                continue
            }
            r.add(i + 1)
        } else if (ch == '\n') {
            r.add(i + 1)
        }
        i++
    }
    return r
}

internal fun String.analyzeLineStarts(): LineStarts {
    var cr = 0
    var lf = 0
    var crlf = 0
    var isBasicASCII = true

    val r = ArrayList<Int>(length / 16)
    r.add(0)

    var i = 0
    val len = length
    while (i < len) {
        val ch = this[i]
        when (ch) {
            '\r' -> {
                if (i + 1 < len && this[i + 1] == '\n') {
                    crlf++
                    r.add(i + 2)
                    i += 2
                    continue
                } else {
                    cr++
                    r.add(i + 1)
                }
            }

            '\n' -> {
                lf++
                r.add(i + 1)
            }

            else -> if (isBasicASCII) {
                if (ch != '\t' && (ch !in ' '..'~')) {
                    isBasicASCII = false
                }
            }
        }
        i++
    }
    return LineStarts(r, cr, lf, crlf, isBasicASCII)
}
