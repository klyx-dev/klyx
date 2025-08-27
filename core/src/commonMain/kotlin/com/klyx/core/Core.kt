package com.klyx.core

import kotlinx.serialization.json.Json
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
fun generateId() = Uuid.random().toHexString()

expect fun Any?.identityHashCode(): Int

inline fun <reified T> T.toJson() = run {
    val json = Json { prettyPrint = true }
    json.encodeToString(this)
}

expect val currentThreadName: String
