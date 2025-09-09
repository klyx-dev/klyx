package com.klyx.extension.api.http

import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readAvailable

class HttpResponseStream(
    private val channel: ByteReadChannel
) {
    private var finished = false

    /**
     * Reads the next chunk from the response.
     * Returns null if the stream has ended.
     */
    suspend fun nextChunk(maxChunkSize: Int = 16_384): ByteArray? {
        if (finished) return null

        val buffer = ByteArray(maxChunkSize)
        val bytesRead = channel.readAvailable(buffer)
        return if (bytesRead == -1) {
            finished = true
            null
        } else {
            buffer.copyOf(bytesRead)
        }
    }
}
