package com.klyx.editor.compose.text.buffer

import kotlinx.io.RawSink
import kotlinx.io.buffered

fun PieceTreeTextBuffer.writeToSink(sink: RawSink) {
    readPiecesContent { content ->
        sink.buffered().use {
            it.write(content.encodeToByteArray())
        }
    }
}
