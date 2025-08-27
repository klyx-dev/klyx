package com.klyx.core.logging

import com.klyx.core.currentThreadName
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

data class Message(
    val level: Level,
    val tag: String,
    val message: String,
    val throwable: Throwable? = null,
    val timestamp: Instant = Clock.System.now(),
    val threadName: String = currentThreadName,
    val metadata: Map<String, Any> = emptyMap()
)

fun Instant.toLogString(
    showMillis: Boolean = false,
    zone: TimeZone = TimeZone.currentSystemDefault()
): String {
    val dt = toLocalDateTime(zone)
    val time = if (showMillis) dt.time.toString() else dt.time.toString().substringBefore('.')
    return time
}
