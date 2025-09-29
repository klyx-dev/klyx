package com.klyx.editor.compose.text.buffer

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@OptIn(ExperimentalContracts::class)
inline fun buildTextBuffer(
    lineBreak: Boolean = true,
    builderAction: TextBufferBuilder.() -> Unit
): PieceTreeTextBuffer {
    contract { callsInPlace(builderAction, InvocationKind.EXACTLY_ONCE) }
    return TextBufferBuilder().apply(builderAction).build(lineBreak)
}

val EmptyTextBuffer = buildTextBuffer { }

@DslMarker
private annotation class TextBufferDsl

@TextBufferDsl
class TextBufferBuilder {
    private val chunks = mutableListOf<String>()

    fun append(chunk: String) {
        chunks += chunk
    }

    @PublishedApi
    internal fun build(lineBreak: Boolean): PieceTreeTextBuffer {
        val pieceBuilder = PieceTreeTextBufferBuilder()
        for (chunk in chunks) {
            pieceBuilder.acceptChunk(chunk)
        }
        return pieceBuilder.build(normalizeLineBreaks = lineBreak)
    }
}
