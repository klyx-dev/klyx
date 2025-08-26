package com.klyx.wasm.wasi

import at.released.weh.filesystem.stdio.StdioSink
import kotlinx.io.RawSink
import kotlinx.io.asSink
import java.io.OutputStream

actual class StdioSinkProvider(
    private val stream: OutputStream,
) {
    actual fun open(): StdioSink = OutputStreamStdioSink(stream)
}

fun StdioSinkProvider(streamProvider: () -> OutputStream) = StdioSinkProvider(streamProvider())

private class OutputStreamStdioSink(
    private val outputStream: OutputStream,
    sink: RawSink = outputStream.asSink(),
) : StdioSink, RawSink by sink
