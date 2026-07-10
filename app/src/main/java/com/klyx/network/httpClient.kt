package com.klyx.network

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
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

        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
            })
        }
    }
}

suspend fun fetchText(url: String) = withContext(Dispatchers.IO) {
    httpClient.get(url).bodyAsText()
}

suspend inline fun <reified T> fetchBody(url: String): T = withContext(Dispatchers.IO) {
    httpClient.get(url).body()
}

fun closeHttpClient() {
    httpClient.close()
}

