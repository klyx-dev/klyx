@file:OptIn(ExperimentalWasmApi::class)

package com.klyx.extension.api

import com.klyx.pointer.Pointer
import com.klyx.wasm.ExperimentalWasmApi
import com.klyx.wasm.WasmMemory
import com.klyx.wasm.readLoweredString
import com.klyx.wasm.type.Option
import com.klyx.wasm.type.str

fun WasmMemory.tryReadOptionResult(ptr: Pointer) = readResult(
    pointer = ptr,
    readOk = Option.reader(str.reader)::read,
    readErr = WasmMemory::readLoweredString
)
