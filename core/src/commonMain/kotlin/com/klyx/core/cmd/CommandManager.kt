package com.klyx.core.cmd

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.type
import com.klyx.core.cmd.key.KeyShortcut
import com.klyx.core.cmd.key.KeyShortcutSequence
import com.klyx.core.cmd.key.keyShortcutOf
import com.klyx.core.cmd.key.matchesSequence
import com.klyx.core.cmd.key.sequence
import com.klyx.core.event.EventBus
import com.klyx.core.theme.ThemeManager
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

object CommandManager {
    val commands = mutableStateSetOf<Command>()
    val recentlyUsedCommands = mutableStateSetOf<Command>()

    private val activeSequences = mutableMapOf<Command, KeyShortcutSequence>()

    var showCommandPalette by mutableStateOf(false)
    val isCommandPaletteHidden get() = !showCommandPalette

    init {
        EventBus.instance.subscribe<KeyEvent> { event ->
            if (event.type == KeyEventType.KeyDown) {
                coroutineScope {
                    activeSequences.entries.removeAll { (command, sequence) ->
                        if (event.matchesSequence(sequence)) {
                            if (sequence.advance()) {
                                launch { command.run() }
                                true
                            } else false
                        } else true
                    }
                }

                for (command in commands) {
                    val sequence = command.shortcuts.sequence()
                    if (event.matchesSequence(sequence)) {
                        if (sequence.advance()) {
                            command.run()
                        } else {
                            activeSequences[command] = sequence
                        }
                    }
                }
            }
        }

        addCommand(buildCommand {
            name("theme selector: toggle")
            shortcut(keyShortcutOf(Key.K, ctrl = true) and keyShortcutOf(Key.T, ctrl = true))
            action { ThemeManager.toggleThemeSelector() }
        })
    }

    suspend fun performShortcut(shortcut: KeyShortcut) {
        coroutineScope {
            activeSequences.entries.removeAll { (command, sequence) ->
                if (shortcut.matchesSequence(sequence)) {
                    if (sequence.advance()) {
                        launch { command.run() }
                        true
                    } else false
                } else true
            }
        }

        for (command in commands) {
            val sequence = command.shortcuts.sequence()
            if (shortcut.matchesSequence(sequence)) {
                if (sequence.advance()) {
                    command.run()
                } else {
                    activeSequences[command] = sequence
                }
            }
        }
    }

    suspend fun performShortcut(shortcuts: Collection<KeyShortcut>) {
        for (shortcut in shortcuts) {
            performShortcut(shortcut)
        }
    }

    fun showCommandPalette() {
        showCommandPalette = true
    }

    fun hideCommandPalette() {
        showCommandPalette = false
    }

    fun toggleCommandPalette() {
        showCommandPalette = !showCommandPalette
    }

    fun addCommand(command: Command) {
        val isPresent = commands.any { it.shortcuts == command.shortcuts }
        if (!isPresent) commands.add(command)
    }

    fun addRecentlyUsedCommand(command: Command) {
        val isPresent = recentlyUsedCommands.any { it.shortcuts == command.shortcuts }
        if (!isPresent) recentlyUsedCommands.add(command)
    }

    fun addCommands(commands: List<Command>) {
        this.commands.addAll(commands.filter { !this.commands.any { c -> c.shortcuts == it.shortcuts } })
    }

    fun removeCommand(command: Command) = commands.remove(command)
    fun removeCommands(commands: List<Command>) = this.commands.removeAll(commands)
}
