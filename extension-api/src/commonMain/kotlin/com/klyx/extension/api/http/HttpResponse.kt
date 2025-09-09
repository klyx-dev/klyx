package com.klyx.extension.api.http

import com.klyx.wasm.ExperimentalWasmApi
import com.klyx.wasm.WasmMemory
import com.klyx.wasm.type.WasmMemoryReader
import com.klyx.wasm.type.WasmType
import com.klyx.wasm.type.collections.Tuple2
import com.klyx.wasm.type.collections.WasmList
import com.klyx.wasm.type.collections.toWasmList
import com.klyx.wasm.type.list
import com.klyx.wasm.type.u8
import com.klyx.wasm.type.wasm

@OptIn(ExperimentalWasmApi::class)
data class HttpResponse(
    val headers: List<Pair<String, String>>,
    val body: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as HttpResponse

        if (headers != other.headers) return false
        if (!body.contentEquals(other.body)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = headers.hashCode()
        result = 31 * result + body.contentHashCode()
        return result
    }
}

@OptIn(ExperimentalWasmApi::class, ExperimentalUnsignedTypes::class)
context(memory: WasmMemory)
internal fun HttpResponse.toExtensionHttpResponse(): ExtensionHttpResponse {
    return ExtensionHttpResponse(
        headers = headers.toWasmList(),
        body = body.toUByteArray().wasm
    )
}

@OptIn(ExperimentalWasmApi::class)
internal data class ExtensionHttpResponse(
    val headers: WasmList<Tuple2<WasmType, WasmType>>,
    val body: list<u8>
) : WasmType {
    override fun writeToBuffer(buffer: ByteArray, offset: Int) {
        var currentOffset = offset
        headers.writeToBuffer(buffer, currentOffset)
        currentOffset += headers.sizeInBytes()
        body.writeToBuffer(buffer, currentOffset)
    }

    override fun sizeInBytes(): Int {
        return headers.sizeInBytes() + body.sizeInBytes()
    }

    override fun toString(memory: WasmMemory): String {
        return buildString {
            append("ExtensionHttpResponse(")
            append("headers=${headers.toString(memory)}, ")
            append("body=${body.toString(memory)}")
            append(")")
        }
    }

    override fun createReader(): WasmMemoryReader<out ExtensionHttpResponse> {
        error("ExtensionHttpResponse is not designed to be read directly from WasmMemory. It's an internal representation for writing data.")
    }
}

