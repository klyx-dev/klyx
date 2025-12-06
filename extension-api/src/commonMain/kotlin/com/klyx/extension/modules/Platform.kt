@file:OptIn(ExperimentalWasmApi::class)

package com.klyx.extension.modules

import com.klyx.core.platform.currentArchitecture
import com.klyx.core.platform.currentOs
import com.klyx.wasm.ExperimentalWasmApi
import com.klyx.wasm.WasmMemory
import com.klyx.wasm.annotations.HostFunction
import com.klyx.wasm.annotations.HostModule
import com.klyx.wasm.type.collections.Tuple2
import com.klyx.wasm.type.toBuffer
import com.klyx.wasm.type.wasm

@HostModule("klyx:extension/platform")
object Platform {
    @HostFunction
    fun currentPlatform(memory: WasmMemory, returnPtr: Int) {
        val os = currentOs()
        val architecture = currentArchitecture()

        val tuple = Tuple2(os.value.wasm, architecture.value.wasm)
        memory.write(returnPtr, tuple.toBuffer())
    }
}
