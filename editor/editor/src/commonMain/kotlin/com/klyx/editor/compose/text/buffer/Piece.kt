package com.klyx.editor.compose.text.buffer

import kotlinx.serialization.Serializable

@Serializable
internal data class Piece(
    val bufferIndex: Int,
    val start: BufferCursor,
    val end: BufferCursor,
    val lineFeedCnt: Int,
    val length: Int
)
