package com.klyx.wasm.wasi

class WasiExitException(
    val exitCode: Int
) : RuntimeException("Process exit code: $exitCode") {

    // no need to capture the Stack Trace
    override fun fillInStackTrace(): Throwable {
        return this
    }
}
