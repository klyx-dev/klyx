package com.klyx.wasm.todo

class WasmException(
    override val message: String?,
    override val cause: Throwable? = null
) : RuntimeException()
