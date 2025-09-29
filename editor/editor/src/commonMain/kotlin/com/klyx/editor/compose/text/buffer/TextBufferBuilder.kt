package com.klyx.editor.compose.text.buffer

import com.klyx.editor.compose.text.LineBreak
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@OptIn(ExperimentalContracts::class)
inline fun buildTextBuffer(
    lineBreak: LineBreak = LineBreak.LF,
    normalizeLineBreaks: Boolean = true,
    builderAction: TextBufferBuilder.() -> Unit
): PieceTreeTextBuffer {
    contract { callsInPlace(builderAction, InvocationKind.EXACTLY_ONCE) }
    return TextBufferBuilder().apply(builderAction).build(lineBreak, normalizeLineBreaks)
}

@Suppress("NOTHING_TO_INLINE")
inline fun buildTextBuffer(
    sequence: CharSequence,
    lineBreak: LineBreak = LineBreak.LF,
    normalizeLineBreaks: Boolean = true
) = PieceTreeTextBufferBuilder(sequence).build(lineBreak, normalizeLineBreaks)

fun CharSequence.toTextBuffer(
    lineBreak: LineBreak = LineBreak.LF,
    normalizeLineBreaks: Boolean = true
) = buildTextBuffer(this, lineBreak, normalizeLineBreaks)

val EmptyTextBuffer = PieceTreeTextBufferBuilder().build()

@DslMarker
private annotation class TextBufferDsl

@TextBufferDsl
class TextBufferBuilder {
    private val chunks = mutableListOf<String>()

    fun append(chunk: String) {
        chunks += chunk
    }

    @PublishedApi
    internal fun build(
        lineBreak: LineBreak = LineBreak.LF,
        normalizeLineBreaks: Boolean = true
    ): PieceTreeTextBuffer {
        val pieceBuilder = PieceTreeTextBufferBuilder()
        for (chunk in chunks) {
            pieceBuilder.acceptChunk(chunk)
        }
        return pieceBuilder.build(lineBreak, normalizeLineBreaks)
    }
}
