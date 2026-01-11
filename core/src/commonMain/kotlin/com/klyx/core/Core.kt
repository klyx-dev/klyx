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
    val json = Json { prettyPrint = true; encodeDefaults = true; explicitNulls = false }
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
    val daySuffix = dayWithSuffix(day)
    return "$monthName $daySuffix, $year"
}

fun LocalDateTime.formatDateTime(): String {
    val daySuffix = dayWithSuffix(day)
    val monthName = month.name.lowercase().replaceFirstChar { it.uppercaseChar() }
    val hour = hour.toString().padStart(2, '0')
    val minute = minute.toString().padStart(2, '0')

    return "$monthName $daySuffix, $year at $hour:$minute"
}

object GitHub {
    const val KLYX_ORG_URL = "https://github.com/klyx-dev"
    const val KLYX_REPO_URL = "$KLYX_ORG_URL/klyx"

    const val RELEASE_URL = "$KLYX_REPO_URL/releases"

    const val BUG_REPORT_URL = "$KLYX_REPO_URL/issues/new?template=bug_report.yml"
    const val FEATURE_REQUEST_URL = "$KLYX_REPO_URL/issues/new?template=feature_request.yml"
}
