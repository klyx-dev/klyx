package com.klyx.core

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@OptIn(ExperimentalContracts::class)
inline fun <T, R> T?.ifNull(defaultValue: () -> R): R where T : R {
    contract { callsInPlace(defaultValue, InvocationKind.AT_MOST_ONCE) }
    return this ?: defaultValue()
}

val Enum<*>.spacedName: String
    get() = name.replace(Regex("(?<=[a-z])(?=[A-Z])"), " ")

private val httpClient = HttpClient(CIO)

suspend fun fetchText(url: String) = withContext(Dispatchers.IO) {
    httpClient.get(url).bodyAsText()
}

suspend fun fetchBody(url: String) = withContext(Dispatchers.IO) {
    httpClient.get(url).body<ByteArray>()
}
