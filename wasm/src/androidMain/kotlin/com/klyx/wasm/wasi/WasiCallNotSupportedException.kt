package com.klyx.wasm.wasi

class WasiCallNotSupportedException(
    override val message: String?
) : Exception()

fun wasiCallNotSupported(callName: String): Nothing {
    throw WasiCallNotSupportedException("We don't yet support this WASI call: $callName")
}
