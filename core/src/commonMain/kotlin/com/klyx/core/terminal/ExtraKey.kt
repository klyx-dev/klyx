package com.klyx.core.terminal

import kotlinx.serialization.Serializable

val ExtraKeys = listOf(
    listOf(
        ExtraKey(key = "ESC"),
        ExtraKey(key = "/", popup = ExtraKey(key = "\\\\")),
        ExtraKey(key = "-", popup = ExtraKey(key = "|")),
        ExtraKey(key = "HOME"),
        ExtraKey(key = "UP"),
        ExtraKey(key = "END"),
        ExtraKey(key = "PGUP")
    ),
    listOf(
        ExtraKey(key = "TAB"),
        ExtraKey(key = "CTRL"),
        ExtraKey(key = "ALT"),
        ExtraKey(key = "LEFT"),
        ExtraKey(key = "DOWN"),
        ExtraKey(key = "RIGHT"),
        ExtraKey(key = "PGDN")
    )
)

@Serializable
data class ExtraKey(
    val key: String? = null,           // simple key
    val macro: String? = null,         // macro sequence
    val popup: ExtraKey? = null,       // nested popup
    val display: String? = null        // optional display name
)
