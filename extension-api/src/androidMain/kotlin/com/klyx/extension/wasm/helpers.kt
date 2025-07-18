package com.klyx.extension.wasm

import kwasm.api.HostFunctionContext
import kwasm.runtime.IntValue

fun string(ptr: IntValue, len: IntValue, context: HostFunctionContext): String {
    val bytes = ByteArray(len.value)
    context.memory?.readBytes(bytes, ptr.value)
    return bytes.toString(Charsets.UTF_8)
}
