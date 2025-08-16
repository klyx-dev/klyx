package com.klyx.wasm.wasi

import at.released.weh.filesystem.stdio.StdioSink
import kotlinx.io.RawSink
import kotlinx.io.asSink
import java.io.OutputStream

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
internal actual class StdioSinkProvider(
    private val streamProvider: () -> OutputStream,
) {
    actual fun open(): StdioSink = OutputStreamStdioSink(streamProvider())
}

private class OutputStreamStdioSink(
    private val outputStream: OutputStream,
    sink: RawSink = outputStream.asSink(),
) : StdioSink, RawSink by sink
