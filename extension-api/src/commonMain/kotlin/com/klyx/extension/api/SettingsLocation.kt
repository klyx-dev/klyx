package com.klyx.extension.api

import arrow.core.None
import arrow.core.Some

data class SettingsLocation(
    val worktreeId: Long,
    val path: String
)

fun parseSettingsLocation(discriminant: Int, worktreeId: Long, path: String) = when (discriminant) {
    0 -> None
    1 -> Some(SettingsLocation(worktreeId, path))
    else -> error("Unknown discriminant: $discriminant")
}
