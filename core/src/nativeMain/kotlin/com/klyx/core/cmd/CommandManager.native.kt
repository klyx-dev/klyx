package com.klyx.core.cmd

import androidx.compose.runtime.snapshots.SnapshotStateSet

@Suppress(names = ["EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING"])
actual object CommandManager {
    actual val commands: SnapshotStateSet<Command>
        get() = TODO("Not yet implemented")
    actual val recentlyUsedCommands: SnapshotStateSet<Command>
        get() = TODO("Not yet implemented")
    actual var showCommandPalette: Boolean
        get() = TODO("Not yet implemented")
        set(value) {}

    actual fun showPalette() {
    }

    actual fun hidePalette() {
    }

    actual fun addCommand(command: Command) {
    }

    actual fun addRecentlyUsedCommand(command: Command) {
    }

    actual fun addCommand(commands: Array<Command>) {
    }
}
