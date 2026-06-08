package com.klyx.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsBytes
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.minutes

val httpClient by lazy {
    HttpClient(CIO) {
        val timeout = 10.minutes.inWholeMilliseconds

        engine {
            requestTimeout = timeout
        }

        install(HttpTimeout) {
            requestTimeoutMillis = timeout
            connectTimeoutMillis = 1.minutes.inWholeMilliseconds
            socketTimeoutMillis = timeout
        }
    }
}

suspend fun fetchText(url: String) = withContext(Dispatchers.IO) {
    httpClient.get(url).bodyAsText()
}

suspend fun fetchBody(url: String) = withContext(Dispatchers.IO) {
    httpClient.get(url).bodyAsBytes()
}

fun closeHttpClient() {
    httpClient.close()
}

