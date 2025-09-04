package com.klyx.core

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
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

fun dayWithSuffix(day: Int): String {
    return when {
        day in 11..13 -> "${day}th"
        else -> when (day % 10) {
            1 -> "${day}st"
            2 -> "${day}nd"
            3 -> "${day}rd"
            else -> "${day}th"
        }
    }
}

fun LocalDate.formatDate(): String {
    val monthName = month.name.lowercase().replaceFirstChar { it.uppercaseChar() }
    val daySuffix = dayWithSuffix(dayOfMonth)
    return "$monthName $daySuffix, $year"
}

fun LocalDateTime.formatDateTime(): String {
    val daySuffix = dayWithSuffix(dayOfMonth)
    val monthName = month.name.lowercase().replaceFirstChar { it.uppercaseChar() }
    val hour = hour.toString().padStart(2, '0')
    val minute = minute.toString().padStart(2, '0')

    return "$monthName $daySuffix, $year at $hour:$minute"
}
