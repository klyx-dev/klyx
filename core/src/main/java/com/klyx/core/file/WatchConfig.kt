package com.klyx.core.file

import android.os.FileObserver

data class WatchConfig(
    val recursive: Boolean = true,
    val watchMask: Int = FileObserver.ALL_EVENTS,
    val debounceMs: Long = 100L,
    val includeHiddenFiles: Boolean = false,
    val fileExtensionFilter: Set<String> = emptySet()
)
