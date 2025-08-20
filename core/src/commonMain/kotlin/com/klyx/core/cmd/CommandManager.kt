package com.klyx.core.cmd

import androidx.compose.runtime.snapshots.SnapshotStateSet

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
expect object CommandManager {
    val commands: SnapshotStateSet<Command>
    val recentlyUsedCommands: SnapshotStateSet<Command>

    var showCommandPalette: Boolean
        private set

    fun showPalette()

    fun hidePalette()

    fun addCommand(command: Command)
    fun addCommand(commands: Array<Command>)

    fun addRecentlyUsedCommand(command: Command)
}
