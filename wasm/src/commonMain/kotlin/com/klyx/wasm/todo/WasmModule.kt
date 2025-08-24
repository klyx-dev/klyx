package com.klyx.wasm.todo

import io.github.charlietap.chasm.embedding.shapes.Module

@ExperimentalWasmApi
class WasmModule internal constructor(
    internal val module: Module
)

