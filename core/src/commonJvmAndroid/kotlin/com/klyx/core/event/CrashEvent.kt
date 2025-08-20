package com.klyx.core.event

import com.klyx.core.file.KxFile

data class CrashEvent(
    val thread: Thread,
    val throwable: Throwable,
    val logFile: KxFile?
)
