package com.klyx.core

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import kotlin.time.Duration.Companion.minutes

actual val httpClient
    get() = HttpClient(CIO) {
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

