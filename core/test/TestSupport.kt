package com.klyx.core

import com.klyx.core.undo.HistoryConfig
import com.klyx.core.undo.UndoManager
import kotlin.time.Duration.Companion.milliseconds

fun newManager(maxEntries: Int = 1000, coalescingWindowMs: Long = 2000) =
    UndoManager("test", HistoryConfig(maxEntries = maxEntries, coalescingWindow = coalescingWindowMs.milliseconds))

class FakeOwner(override val ownerId: String, override val isActive: Boolean = true) : Owner
