package com.klyx.wasm.wasi

import at.released.weh.filesystem.stdio.StdioSink
import kotlinx.io.RawSink

interface StdioSinkProvider {
    fun open(): StdioSink
}

expect fun StdioSinkProvider(sinkProvider: () -> RawSink): StdioSinkProvider
