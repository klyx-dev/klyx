@file:OptIn(ExperimentalWasmApi::class)

package com.klyx.extension.api

import com.klyx.wasm.ExperimentalWasmApi
import com.klyx.wasm.WasmMemory
import com.klyx.wasm.type.Option
import com.klyx.wasm.type.str

fun WasmMemory.tryReadOptionResult(ptr: Int) = readResult(
    pointer = ptr,
    readOk = Option.reader(str.reader)::read,
    readErr = WasmMemory::readCString
)
