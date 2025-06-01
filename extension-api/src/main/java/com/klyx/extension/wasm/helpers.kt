package com.klyx.extension.wasm

import kwasm.api.HostFunctionContext

fun string(ptr: Int, len: Int, context: HostFunctionContext): String {
    val bytes = ByteArray(len)
    context.memory?.readBytes(bytes, ptr)
    return bytes.toString(Charsets.UTF_8)
}
