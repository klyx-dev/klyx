package com.klyx.core

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout

actual val httpClient get() = HttpClient(CIO) {
    engine {
        requestTimeout = 20_000
    }

    install(HttpTimeout) {
        requestTimeoutMillis = 20_000
        connectTimeoutMillis = 20_000
        socketTimeoutMillis = 20_000
    }
}

