package com.klyx.extension.api.http

import com.klyx.wasm.ExperimentalWasmApi
import com.klyx.wasm.WasmMemory
import com.klyx.wasm.type.list
import com.klyx.wasm.type.str
import com.klyx.wasm.type.tuple2

@OptIn(ExperimentalUnsignedTypes::class)
data class HttpRequest(
    val method: HttpMethod,
    val url: String,
    val headers: Map<String, String>,
    val body: ByteArray?,
    val redirectPolicy: RedirectPolicy
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as HttpRequest

        if (method != other.method) return false
        if (url != other.url) return false
        if (headers != other.headers) return false
        if (!body.contentEquals(other.body)) return false
        if (redirectPolicy != other.redirectPolicy) return false

        return true
    }

    override fun hashCode(): Int {
        var result = method.hashCode()
        result = 31 * result + url.hashCode()
        result = 31 * result + headers.hashCode()
        result = 31 * result + (body?.contentHashCode() ?: 0)
        result = 31 * result + redirectPolicy.hashCode()
        return result
    }
}

@OptIn(ExperimentalWasmApi::class)
internal fun WasmMemory.parseHttpRequest(
    methodOrdinal: Int,
    url: String,
    headersPtr: Int,
    headersLen: Int,
    bodyTag: Int,
    bodyPtr: Int,
    bodyLen: Int,
    redirectPolicyTag: Int,
    redirectPolicyLimit: Int
): HttpRequest {
    val method = HttpMethod.entries[methodOrdinal]

    val headersReader = list.reader(tuple2.reader(str.reader, str.reader))
    val headers = headersReader.read(this, headersPtr, headersLen).associate { (k, v) -> k.value to v.value }

    val body = when (bodyTag) {
        0 -> null
        1 -> readBytes(bodyPtr, bodyLen)
        else -> error("Unknown body tag: $bodyTag")
    }

    val redirectPolicy = parseRedirectPolicy(redirectPolicyTag, redirectPolicyLimit)

    return HttpRequest(
        method = method,
        url = url,
        headers = headers,
        body = body,
        redirectPolicy = redirectPolicy
    )
}
