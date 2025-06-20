package com.klyx.core

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

expect val httpClient: HttpClient

suspend fun fetchText(url: String) = withContext(Dispatchers.IO) {
    httpClient.get(url).bodyAsText()
}

suspend fun fetchBody(url: String) = withContext(Dispatchers.IO) {
    httpClient.get(url).body<ByteArray>()
}

