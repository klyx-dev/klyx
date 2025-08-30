package com.klyx.wasm.wasi

import at.released.weh.filesystem.stdio.StdioSink
import kotlinx.io.RawSink

actual fun StdioSinkProvider(sinkProvider: () -> RawSink) = object : StdioSinkProvider {
    override fun open() = RawSinkStdioSink(sinkProvider())
}

private class RawSinkStdioSink(sink: RawSink) : StdioSink, RawSink by sink
