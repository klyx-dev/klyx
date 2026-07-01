package com.klyx.api.util

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.time.Duration

/**
 * Converts a [Duration] into a [LocalDateTime] representing the duration since epoch.
 */
fun Duration.asLocalDateTime(): LocalDateTime = Instant
    .ofEpochMilli(inWholeMilliseconds)
    .atZone(ZoneId.systemDefault())
    .toLocalDateTime()

/**
 * Adds an ordinal suffix to a day of the month (e.g., 1st, 2nd, 11th).
 */
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

/**
 * Formats a [LocalDate] as "Month DaySuffix, Year" (e.g., "July 1st, 2026").
 */
fun LocalDate.formatDate(): String {
    val monthName = month.name.lowercase().replaceFirstChar { it.uppercaseChar() }
    val daySuffix = dayWithSuffix(dayOfMonth)
    return "$monthName $daySuffix, $year"
}

/**
 * Formats a [LocalDateTime] as "Month DaySuffix, Year at Hour:Minute" (e.g., "July 1st, 2026 at 10:30").
 */
fun LocalDateTime.formatDateTime(): String {
    val daySuffix = dayWithSuffix(dayOfMonth)
    val monthName = month.name.lowercase().replaceFirstChar { it.uppercaseChar() }
    val hour = hour.toString().padStart(2, '0')
    val minute = minute.toString().padStart(2, '0')

    return "$monthName $daySuffix, $year at $hour:$minute"
}
