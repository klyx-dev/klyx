package com.klyx.wasm.wasi

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import at.released.weh.filesystem.error.Again
import at.released.weh.filesystem.error.NonblockingPollError
import at.released.weh.filesystem.model.FileSystemErrno.SUCCESS
import at.released.weh.filesystem.stdio.StdioPollEvent
import at.released.weh.filesystem.stdio.StdioSource
import kotlinx.io.RawSource
import kotlinx.io.asSource
import java.io.IOException
import java.io.InputStream

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
internal actual class StdioSourceProvider(
    private val streamProvider: () -> InputStream,
) {
    actual fun open(): StdioSource = InputStreamStdioSource(streamProvider())
}

private class InputStreamStdioSource(
    private val inputStream: InputStream,
    source: RawSource = inputStream.asSource(),
) : StdioSource, RawSource by source {
    override fun pollNonblocking(): Either<NonblockingPollError, StdioPollEvent> {
        return try {
            val bytesAvailable = inputStream.available()
            if (bytesAvailable != 0) {
                StdioPollEvent(
                    errno = SUCCESS,
                    bytesAvailable = bytesAvailable.toLong(),
                    isHangup = true,
                ).right()
            } else {
                AGAIN_ERROR
            }
        } catch (_: IOException) {
            return StdioPollEvent(
                errno = SUCCESS,
                bytesAvailable = 0,
                isHangup = true,
            ).right()
        }
    }

    private companion object {
        private val AGAIN_ERROR = Again("No data available").left()
    }
}

