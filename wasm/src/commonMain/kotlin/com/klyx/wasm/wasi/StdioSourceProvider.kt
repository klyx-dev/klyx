package com.klyx.wasm.wasi

import at.released.weh.filesystem.stdio.StdioSource

expect class StdioSourceProvider {
    fun open(): StdioSource
}

