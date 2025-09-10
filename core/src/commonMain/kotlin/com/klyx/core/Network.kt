package com.klyx.core

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsBytes
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

expect val httpClient: HttpClient

suspend fun fetchText(url: String) = withContext(Dispatchers.IO) {
    httpClient.get(url).bodyAsText()
}

suspend fun fetchBody(url: String) = withContext(Dispatchers.IO) {
    httpClient.get(url).bodyAsBytes()
}

