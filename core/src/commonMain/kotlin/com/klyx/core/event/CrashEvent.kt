package com.klyx.core.event

import com.klyx.core.file.KxFile
import com.klyx.core.process.Thread

data class CrashEvent(
    val thread: Thread,
    val throwable: Throwable,
    val logFile: KxFile?
)
