package com.klyx.wasm.wasi

class WasiRuntimeException(
    override val message: String? = null,
    override val cause: Throwable? = null
) : RuntimeException()
