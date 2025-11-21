@file:OptIn(ExperimentalWasmApi::class)

package com.klyx.extension.modules

import com.klyx.wasm.ExperimentalWasmApi
import com.klyx.wasm.WasmMemory
import com.klyx.wasm.annotations.HostFunction
import com.klyx.wasm.annotations.HostModule
import com.klyx.wasm.type.collections.Tuple2
import com.klyx.wasm.type.toBuffer
import com.klyx.wasm.type.wasm
import kotlin.jvm.JvmInline

internal expect fun currentOs(): Platform.Os
internal expect fun currentArchitecture(): Platform.Architecture

@HostModule("klyx:extension/platform")
object Platform {

    @JvmInline
    value class Os private constructor(val value: Int) {
        companion object {
            val Mac = Os(0)
            val Linux = Os(1)
            val Windows = Os(2)
            val Android = Os(3)
            val iOS = Os(4)
        }
    }

    @JvmInline
    value class Architecture private constructor(val value: Int) {
        companion object {
            val Aarch64 = Architecture(0)
            val X86 = Architecture(1)
            val X8664 = Architecture(2)
        }
    }

    @HostFunction
    fun currentPlatform(memory: WasmMemory, returnPtr: Int) {
        val os = currentOs()
        val architecture = currentArchitecture()

        val tuple = Tuple2(os.value.wasm, architecture.value.wasm)
        memory.write(returnPtr, tuple.toBuffer())
    }
}
