@file:Suppress("EmptyInitBlock")

package com.klyx.wasm.todo

import io.github.charlietap.chasm.embedding.shapes.Instance

@ExperimentalWasmApi
class WasmInstance internal constructor(
    private val instance: Instance
) {
    init {

    }
}

@OptIn(ExperimentalWasmApi::class)
internal fun Instance.asWasmInstance() = WasmInstance(this)
