package com.klyx.editor.compose.text.buffer

import kotlinx.io.RawSink
import kotlinx.io.buffered

internal fun PieceTreeTextBuffer.writeToSink(sink: RawSink) {
    sink.buffered().use {
        readPiecesContent { text ->
            it.write(text.encodeToByteArray())
        }
    }
}
