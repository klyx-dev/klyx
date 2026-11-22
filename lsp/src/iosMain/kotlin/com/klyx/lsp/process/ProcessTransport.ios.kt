package com.klyx.lsp.process

import io.matthewnelson.kmp.process.Process

actual fun Process.asRawStreams(): ProcessStreams {
    error("Direct process stdio transport is not supported on iOS")
}

