package com.klyx.wasm.wasi

import at.released.weh.filesystem.stdio.StdioSink

expect class StdioSinkProvider {
    fun open(): StdioSink
}
