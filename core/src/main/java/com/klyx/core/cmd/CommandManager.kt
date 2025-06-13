package com.klyx.core.cmd

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import com.klyx.core.event.EventBus

object CommandManager {
    val commands = mutableStateListOf<Command>()
    val recentlyUsedCommands = mutableStateListOf<Command>()

    var showCommandPalette by mutableStateOf(false)
        private set

    init {
        EventBus.getInstance().subscribe<KeyEvent> { event ->
            val isCtrlPressed = event.isCtrlPressed
            val isShiftPressed = event.isShiftPressed
            val isAltPressed = event.isAltPressed

            cmd@ for (command in commands) {
                val keys = command.keybinding?.split("-")

                if (keys != null) {
                    var isCtrlRequired = false
                    var isShiftRequired = false
                    var isAltRequired = false
                    var cmdKey: Key? = null

                    for (key in keys) {
                        when (key.lowercase()) {
                            "ctrl" -> isCtrlRequired = true
                            "shift" -> isShiftRequired = true
                            "alt" -> isAltRequired = true
                            else -> cmdKey = key.toKey()
                        }
                    }

                    if (isCtrlPressed == isCtrlRequired &&
                        isShiftPressed == isShiftRequired &&
                        isAltPressed == isAltRequired &&
                        cmdKey == event.key
                    ) {
                        command.action(command)
                        break@cmd
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

    fun addCommand(vararg commands: Command) {
        this.commands.addAll(commands)
    }

    fun addRecentlyUsedCommand(command: Command) {
        recentlyUsedCommands.add(command)
    }
}
