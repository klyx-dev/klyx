package com.klyx.wasm.wasi

import at.released.weh.filesystem.stdio.StdioSink

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
expect class StdioSinkProvider {
    fun open(): StdioSink
}
