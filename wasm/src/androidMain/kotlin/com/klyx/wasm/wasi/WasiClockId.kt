package com.klyx.wasm.wasi

/**
 * WASI [clockid](https://github.com/WebAssembly/WASI/blob/v0.2.1/legacy/preview1/docs.md#clockid)
 */
internal object WasiClockId {
    const val Realtime = 0
    const val Monotonic = 1
    const val ProcessCputimeId = 2
    const val ThreadCputimeId = 3
}
