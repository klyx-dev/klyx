package com.klyx.wasm

import io.github.charlietap.chasm.embedding.shapes.Module
import kotlin.jvm.JvmInline

@ExperimentalWasmApi
@JvmInline
value class WasmModule internal constructor(
    internal val module: Module
)
