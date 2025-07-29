package com.klyx.wasm

import com.dylibso.chicory.runtime.Memory
import com.klyx.asIntPair
import java.nio.charset.Charset

@ExperimentalWasmApi
class WasmMemory internal constructor(
    private val memory: Memory
) {
    fun readString(addr: Int, len: Int, charset: Charset = Charsets.UTF_8): String = run {
        memory.readString(addr, len, charset)
    }
}

@ExperimentalWasmApi
fun WasmMemory.readString(args: LongArray, offset: Int = 0) = run {
    val (addr, len) = args.takePair(offset).asIntPair()
    readString(addr, len)
}
