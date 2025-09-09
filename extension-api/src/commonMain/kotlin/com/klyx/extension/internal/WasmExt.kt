package com.klyx.extension.internal

fun notReadable(name: String): Nothing {
    error("$name is not designed to be read directly from WasmMemory. It's an internal representation for writing data.")
}
