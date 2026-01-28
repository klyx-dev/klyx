package com.klyx.terminal.emulator

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.yield

/**
 * A circular byte buffer allowing one producer and one consumer thread.
 */
class ByteQueue(size: Int) {
    private val buffer = ByteArray(size)
    private var head = 0
    private var stored = 0
    private var open = true

    private val mutex = Mutex()

    suspend fun close() {
        mutex.withLock {
            open = false
        }
    }

    suspend fun read(out: ByteArray, blocking: Boolean): Int {
        while (true) {
            mutex.withLock {
                if (stored > 0) break
                if (!open) return -1
                if (!blocking) return 0
            }
            yield()
        }

        return mutex.withLock {
            var read = 0
            var remaining = out.size

            while (remaining > 0 && stored > 0) {
                val run = minOf(buffer.size - head, stored)
                val n = minOf(run, remaining)

                buffer.copyInto(out, read, head, head + n)

                head = (head + n) % buffer.size
                stored -= n
                read += n
                remaining -= n
            }
            read
        }
    }

    /**
     * Attempt to write the specified portion of the provided buffer to the queue.
     *
     * Returns whether the output was totally written, false if it was closed before.
     */
    suspend fun write(src: ByteArray, offset: Int, length: Int): Boolean {
        require(offset + length <= src.size)
        require(length > 0)

        var remaining = length
        var off = offset

        while (remaining > 0) {
            mutex.withLock {
                if (!open) return false
                if (stored == buffer.size) return@withLock

                val tail = (head + stored) % buffer.size
                val run = if (tail >= head) buffer.size - tail else head - tail
                val n = minOf(run, remaining, buffer.size - stored)

                src.copyInto(buffer, tail, off, off + n)

                stored += n
                off += n
                remaining -= n
            }
            yield()
        }
        return true
    }
}
