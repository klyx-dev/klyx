package com.klyx.wasm

class NoMemoryException(
    override val message: String,
    override val cause: Throwable? = null
) : WasmException(message)
