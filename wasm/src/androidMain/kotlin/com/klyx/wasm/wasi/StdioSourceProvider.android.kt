package com.klyx.wasm.wasi

import at.released.weh.filesystem.stdio.StdioSource
import kotlinx.io.RawSource

actual fun StdioSourceProvider(sourceProvider: () -> RawSource) = object : StdioSourceProvider {
    override fun open() = RawSourceStdioSource(sourceProvider())
}

private class RawSourceStdioSource(source: RawSource) : StdioSource, RawSource by source
