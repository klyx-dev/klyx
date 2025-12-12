package com.klyx.core

import android.net.TrafficStats
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import kotlin.time.Duration.Companion.minutes

actual val httpClient by lazy {
    TrafficStats.setThreadStatsTag(1)
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

