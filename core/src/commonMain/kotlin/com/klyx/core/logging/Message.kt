package com.klyx.core.logging

import kotlinx.datetime.Clock

data class Message(
    val level: Level,
    val tag: String,
    val message: String,
    val timestamp: Long = Clock.System.now().toEpochMilliseconds(),
    val throwable: Throwable? = null
)
