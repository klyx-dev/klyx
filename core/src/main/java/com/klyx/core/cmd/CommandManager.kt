package com.klyx.core.cmd

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.type
import com.klyx.core.cmd.key.matches
import com.klyx.core.cmd.key.parseShortcut
import com.klyx.core.event.EventBus

object CommandManager {
    val commands = mutableStateListOf<Command>()
    val recentlyUsedCommands = mutableStateListOf<Command>()

    var showCommandPalette by mutableStateOf(false)
        private set

    init {
        EventBus.getInstance().subscribe<KeyEvent> { event ->
            for (command in commands) {
                command.shortcutKey?.let { key ->
                    if (event.type == KeyEventType.KeyDown) {
                        if (event.matches(parseShortcut(key).first())) {
                            command.execute(command)
                        }
                    }
                }
            }
        }
    }

    fun showPalette() {
        showCommandPalette = true
    }

    fun hidePalette() {
        showCommandPalette = false
    }

    fun addCommand(vararg command: Command) {
        commands.addAll(command)
    }

    fun addRecentlyUsedCommand(command: Command) {
        recentlyUsedCommands.add(command)
    }
}
