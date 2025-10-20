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

object CommandManager {
    val commands = mutableStateSetOf<Command>()
    val recentlyUsedCommands = mutableStateSetOf<Command>()

    private val activeSequences = mutableMapOf<Command, KeyShortcutSequence>()

    var showCommandPalette by mutableStateOf(false)

    init {
        EventBus.instance.subscribe<KeyEvent> { event ->
            if (event.type == KeyEventType.KeyDown) {
                activeSequences.entries.removeAll { (command, sequence) ->
                    if (event.matchesSequence(sequence)) {
                        if (sequence.advance()) {
                            command.run()
                            true
                        } else false
                    } else true
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
            name("Toggle Theme Selector")
            shortcut(keyShortcutOf(Key.K, ctrl = true) and keyShortcutOf(Key.T, ctrl = true))
            execute { ThemeManager.toggleThemeSelector() }
        })
    }

    fun performShortcut(shortcut: KeyShortcut) {
        activeSequences.entries.removeAll { (command, sequence) ->
            if (shortcut.matchesSequence(sequence)) {
                if (sequence.advance()) {
                    command.run()
                    true
                } else false
            } else true
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

    fun performShortcut(shortcuts: Collection<KeyShortcut>) {
        shortcuts.forEach { performShortcut(it) }
    }

    fun showPalette() {
        showCommandPalette = true
    }

    fun hidePalette() {
        showCommandPalette = false
    }

    fun addCommand(command: Command) {
        val isPresent = commands.any { it.shortcuts == command.shortcuts }
        if (!isPresent) commands.add(command)
    }

    fun addRecentlyUsedCommand(command: Command) {
        val isPresent = recentlyUsedCommands.any { it.shortcuts == command.shortcuts }
        if (!isPresent) recentlyUsedCommands.add(command)
    }

    fun addCommand(commands: Array<Command>) {
        this.commands.addAll(commands.filter { !this.commands.any { c -> c.shortcuts == it.shortcuts } })
    }
}
