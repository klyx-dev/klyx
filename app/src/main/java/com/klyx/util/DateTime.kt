package com.klyx.util

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.time.Duration

fun Duration.asLocalDateTime(): LocalDateTime = Instant
    .ofEpochMilli(inWholeMilliseconds)
    .atZone(ZoneId.systemDefault())
    .toLocalDateTime()

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
