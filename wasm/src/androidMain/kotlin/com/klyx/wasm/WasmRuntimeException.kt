package com.klyx.wasm

class WasmRuntimeException(
    override val message: String? = null,
    override val cause: Throwable? = null
) : RuntimeException()
