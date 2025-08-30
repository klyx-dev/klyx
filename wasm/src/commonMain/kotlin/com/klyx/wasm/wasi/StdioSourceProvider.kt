package com.klyx.wasm.wasi

import at.released.weh.filesystem.stdio.StdioSource
import kotlinx.io.RawSource

interface StdioSourceProvider {
    fun open(): StdioSource
}

expect fun StdioSourceProvider(sourceProvider: () -> RawSource): StdioSourceProvider

