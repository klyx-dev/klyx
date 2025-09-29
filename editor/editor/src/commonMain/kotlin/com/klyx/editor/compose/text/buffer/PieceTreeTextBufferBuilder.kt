package com.klyx.editor.compose.text.buffer

import com.klyx.editor.compose.text.CharCode
import com.klyx.editor.compose.text.LineBreak
import com.klyx.editor.compose.text.Strings
import com.klyx.editor.compose.text.codePointAt

class PieceTreeTextBufferBuilder(sequence: CharSequence? = null) {
    private val chunks = mutableListOf<TextBuffer>()

    private var byteOrderMark = ""
    private var hasRetainedChar = false
    private var retainedChar = 0

    private var cr = 0
    private var lf = 0
    private var crlf = 0
    private var containsRTL = false
    private var containsUnusualLineTerminators = false
    private var containsNonBasicASCII = true

    init {
        sequence?.let {
            acceptChunk(it.toString())
        }
    }

    // append text
    fun acceptChunk(text: String) {
        if (text.isEmpty()) {
            return // Nothing to do
        }

        var chunk = text
        if (chunks.isEmpty()) {
            if (Strings.startsWithUTF8BOM(chunk)) {
                byteOrderMark = Strings.UTF8_BOM_CHARACTER
                chunk = chunk.substring(1)
            }
        }

        val lastChar = chunk.codePointAt(chunk.length - 1)
        if (lastChar == CharCode.CarriageReturn || (lastChar in 0xD800..0xDBFF)) {
            // last character is \r or a high surrogate => keep it back
            acceptChunk(chunk.dropLast(1), false)
            hasRetainedChar = true
            retainedChar = lastChar
        } else {
            acceptChunk(chunk, false)
            hasRetainedChar = false
            retainedChar = lastChar
        }
    }

    private fun acceptChunk(chunk: String, allowEmptyStrings: Boolean) {
        if (!allowEmptyStrings && chunk.isEmpty()) {
            return // Nothing to do
        }

        if (hasRetainedChar) {
            appendChunk(Strings.toChars(retainedChar) + chunk)
        } else {
            appendChunk(chunk)
        }
    }

    private fun appendChunk(chunk: String) {
        val lineStarts = chunk.analyzeLineStarts()

        chunks.add(TextBuffer(StringBuilder(chunk), lineStarts.lineStarts))
        cr += lineStarts.cr
        lf += lineStarts.lf
        crlf += lineStarts.crlf

        if (!lineStarts.isBasicASCII) {
            // this chunk contains non basic ASCII characters
            containsNonBasicASCII = false;
            if (!containsRTL) {
                containsRTL = Strings.containsRTL(chunk);
            }
            if (!containsUnusualLineTerminators) {
                containsUnusualLineTerminators = Strings.containsUnusualLineTerminators(chunk);
            }
        }
    }

    private fun getFirstLineText(lengthLimit: Int): String {
        return chunks[0].buffer.substring(0, lengthLimit).lines()[0]
    }

    private fun getLineBreak(lineBreak: LineBreak): String {
        val totalEOLCount = cr + lf + crlf
        val totalCRCount = cr + crlf
        if (totalEOLCount == 0) {
            // This is an empty file or a file with precisely one line
            return if (lineBreak == LineBreak.LF) "\n" else "\r\n"
        }
        if (totalCRCount > totalEOLCount / 2) {
            // More than half of the file contains \r\n ending lines
            return "\r\n"
        }
        // At least one line more ends in \n
        return "\n"
    }

    // finish accept the all text chunks
    fun build(
        lineBreak: LineBreak = LineBreak.LF,
        normalizeLineBreaks: Boolean = true
    ): PieceTreeTextBuffer {
        if (chunks.isEmpty()) {
            acceptChunk("", true)
        }

        if (hasRetainedChar) {
            hasRetainedChar = false
            // recreate last chunk
            val lastChunk = chunks.last()
            lastChunk.buffer.append(Strings.toChars(retainedChar))
            val newLineStarts = lastChunk.buffer.computeLineStartOffsets()
            lastChunk.lineStarts = newLineStarts

            if (retainedChar == CharCode.CarriageReturn) {
                cr++
            }
        }

        val lineSeparator = getLineBreak(lineBreak)

        if (normalizeLineBreaks &&
            ((lineSeparator == "\r\n" && (cr > 0 || lf > 0)) ||
                    (lineSeparator == "\n" && (cr > 0 || crlf > 0)))
        ) {
            // Normalize pieces
            for ((index, chunk) in chunks.withIndex()) {
                val str = chunk.buffer.toString().replace(Strings.newLine, lineSeparator)
                val newLineStart = str.computeLineStartOffsets()
                chunks[index] = TextBuffer(StringBuilder(str), newLineStart)
            }
        }

        // create the piece tree buffer
        return PieceTreeTextBuffer(
            chunks = chunks,
            lineBreak = lineSeparator,
            lineBreakNormalized = normalizeLineBreaks,
            bom = byteOrderMark,
            isRtl = containsRTL,
            isLineTerminators = containsUnusualLineTerminators,
            isBasicASCII = !containsNonBasicASCII
        ) // mightContainNonBasicASCII = !_isBasicASCII
    }
}
