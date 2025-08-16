package com.klyx.wasm.wasi

import at.released.weh.filesystem.stdio.StdioSource

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
expect class StdioSourceProvider {
    fun open(): StdioSource
}

