@file:OptIn(ExperimentalWasmApi::class)

package com.klyx.extension.modules

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.fold
import com.github.michaelbull.result.map
import com.github.michaelbull.result.runCatching
import com.klyx.borrow.ref
import com.klyx.extension.api.http.HttpResponseStream
import com.klyx.extension.api.http.RedirectPolicy
import com.klyx.extension.api.http.parseHttpRequest
import com.klyx.extension.api.http.toExtensionHttpResponse
import com.klyx.extension.api.http.toKtorHttpMethod
import com.klyx.pointer.asPointer
import com.klyx.pointer.dropPtr
import com.klyx.pointer.value
import com.klyx.wasm.ExperimentalWasmApi
import com.klyx.wasm.WasmMemory
import com.klyx.wasm.annotations.HostFunction
import com.klyx.wasm.annotations.HostModule
import com.klyx.wasm.type.None
import com.klyx.wasm.type.Some
import com.klyx.wasm.type.toBuffer
import com.klyx.wasm.type.wasm
import com.klyx.wasm.type.wstr
import io.ktor.client.HttpClient
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.headers
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsBytes
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking

@HostModule("klyx:extension/http-client")
object HttpClient {
    private val client = HttpClient {
        followRedirects = true
    }

    private val noFollowClient = HttpClient {
        followRedirects = false
    }

    @HostFunction
    fun WasmMemory.fetch(
        methodOrdinal: Int,
        url: String,
        headersPtr: Int,
        headersLen: Int,
        bodyTag: Int,
        bodyPtr: Int,
        bodyLen: Int,
        redirectPolicyTag: Int,
        redirectPolicyLimit: Int,
        resultPtr: Int
    ) {
        val response = try {
            runBlocking {
                val res = fetchResponse(
                    methodOrdinal = methodOrdinal,
                    url = url,
                    headersPtr = headersPtr,
                    headersLen = headersLen,
                    bodyTag = bodyTag,
                    bodyPtr = bodyPtr,
                    bodyLen = bodyLen,
                    redirectPolicyTag = redirectPolicyTag,
                    redirectPolicyLimit = redirectPolicyLimit
                )

                Ok(
                    com.klyx.extension.api.http.HttpResponse(
                        headers = res.headers.entries().map { (k, v) -> k to v.joinToString(",") },
                        body = res.bodyAsBytes()
                    )
                )
            }
        } catch (err: Exception) {
            Err(err.message ?: "Unknown error")
        }

        val wasmResponse = response.fold(
            success = { com.klyx.wasm.type.Ok(it.toExtensionHttpResponse()) },
            failure = { com.klyx.wasm.type.Err(it.wstr) }
        )

        write(resultPtr, wasmResponse.toBuffer())
    }

    @HostFunction
    fun WasmMemory.fetchStream(
        methodOrdinal: Int,
        url: String,
        headersPtr: Int,
        headersLen: Int,
        bodyTag: Int,
        bodyPtr: Int,
        bodyLen: Int,
        redirectPolicyTag: Int,
        redirectPolicyLimit: Int,
        resultPtr: Int
    ) = runBlocking {
        val response = runCatching {
            fetchResponse(
                methodOrdinal = methodOrdinal,
                url = url,
                headersPtr = headersPtr,
                headersLen = headersLen,
                bodyTag = bodyTag,
                bodyPtr = bodyPtr,
                bodyLen = bodyLen,
                redirectPolicyTag = redirectPolicyTag,
                redirectPolicyLimit = redirectPolicyLimit
            )
        }

        val stream = response.map { HttpResponseStream(it.bodyAsChannel()) }

        val result = stream.fold(
            success = { com.klyx.wasm.type.Ok(ref(it).rawPointer.wasm) },
            failure = { com.klyx.wasm.type.Err((it.message ?: "Unknown error").wstr) }
        )
        write(resultPtr, result.toBuffer())
    }

    private suspend fun WasmMemory.fetchResponse(
        methodOrdinal: Int,
        url: String,
        headersPtr: Int,
        headersLen: Int,
        bodyTag: Int,
        bodyPtr: Int,
        bodyLen: Int,
        redirectPolicyTag: Int,
        redirectPolicyLimit: Int,
    ): HttpResponse {
        val req = parseHttpRequest(
            methodOrdinal = methodOrdinal,
            url = url,
            headersPtr = headersPtr,
            headersLen = headersLen,
            bodyTag = bodyTag,
            bodyPtr = bodyPtr,
            bodyLen = bodyLen,
            redirectPolicyTag = redirectPolicyTag,
            redirectPolicyLimit = redirectPolicyLimit
        )

        println("Request: $req")

        return when (req.redirectPolicy) {
            RedirectPolicy.FollowAll -> client.request(req.url) {
                method = req.method.toKtorHttpMethod()
                headers { req.headers.forEach { (k, v) -> append(k, v) } }
                req.body?.let { setBody(it) }
            }

            is RedirectPolicy.FollowLimit -> {
                var currentUrl = req.url
                var redirectCount = 0
                var finalResponse: HttpResponse

                while (true) {
                    val resp = client.request(currentUrl) {
                        method = req.method.toKtorHttpMethod()
                        headers { req.headers.forEach { (k, v) -> append(k, v) } }
                        req.body?.let { setBody(it) }
                    }

                    if (resp.status.isRedirect()) {
                        val location = resp.headers[HttpHeaders.Location] ?: error("Redirect without Location")
                        redirectCount++
                        if (redirectCount > req.redirectPolicy.limit) error("Too many redirects")
                        currentUrl = location
                    } else {
                        finalResponse = resp
                        break
                    }
                }

                finalResponse
            }

            RedirectPolicy.NoFollow -> noFollowClient.request(req.url) {
                method = req.method.toKtorHttpMethod()
                headers { req.headers.forEach { (k, v) -> append(k, v) } }
                req.body?.let { setBody(it) }
                expectSuccess = true
            }
        }
    }

    private fun HttpStatusCode.isRedirect(): Boolean {
        return value in 300..399
    }

    @HostFunction("[resource-drop]http-response-stream")
    fun dropHttpResponseStream(ptr: Int) {
        val pointer = ptr.asPointer()
        dropPtr(pointer)
    }

    @HostFunction("[method]http-response-stream.next-chunk")
    fun WasmMemory.httpResponseStreamNextChunk(ptr: Int, resultPtr: Int) = runBlocking {
        val stream = ptr.asPointer().value<HttpResponseStream>()

        val chunk = try {
            com.klyx.wasm.type.Ok(stream.nextChunk()?.wasm?.let(::Some) ?: None)
        } catch (err: Exception) {
            com.klyx.wasm.type.Err((err.message ?: "Unknown error").wstr)
        }
        write(resultPtr, chunk.toBuffer())
    }
}
