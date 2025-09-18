package com.klyx.core.logging

import com.klyx.core.currentThreadName
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

data class Message @OptIn(ExperimentalTime::class) constructor(
    val level: Level,
    val tag: String,
    val message: String,
    val type: MessageType = MessageType.Standard,
    val throwable: Throwable? = null,
    val timestamp: kotlin.time.Instant = kotlin.time.Clock.System.now(),
    val threadName: String = currentThreadName,
    val metadata: Map<String, Any> = emptyMap()
)

enum class MessageType {
    Standard, Progress
}

@OptIn(ExperimentalTime::class)
fun Instant.toLogString(
    showMillis: Boolean = false,
    zone: TimeZone = TimeZone.currentSystemDefault()
): String {
    val dt = toLocalDateTime(zone)
    val time = if (showMillis) dt.time.toString() else dt.time.toString().substringBefore('.')
    return time
}
