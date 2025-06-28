package com.klyx.core.cmd

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.type
import com.klyx.core.cmd.key.ShortcutSequence
import com.klyx.core.cmd.key.matchesSequence
import com.klyx.core.cmd.key.parseShortcut
import com.klyx.core.event.EventBus
import com.klyx.core.theme.ThemeManager

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual object CommandManager {
    actual val commands = mutableStateSetOf<Command>()
    actual val recentlyUsedCommands = mutableStateSetOf<Command>()

    private val activeSequences = mutableMapOf<Command, ShortcutSequence>()

    actual var showCommandPalette by mutableStateOf(false)

    init {
        EventBus.instance.subscribe<KeyEvent> { event ->
            if (event.type == KeyEventType.KeyDown) {
                activeSequences.entries.removeIf { (command, sequence) ->
                    if (event.matchesSequence(sequence)) {
                        if (sequence.advance()) {
                            command.execute(command)
                            true
                        } else false
                    } else true
                }

                for (command in commands) {
                    command.shortcutKey?.let { key ->
                        val sequence = parseShortcut(key)
                        if (event.matchesSequence(sequence)) {
                            if (sequence.advance()) {
                                command.execute(command)
                            } else {
                                activeSequences[command] = sequence
                            }
                        }
                    }
                }
            }
        }

        addCommand(Command("Toggle theme selector", shortcutKey = "Ctrl-K Ctrl-T") {
            ThemeManager.toggleThemeSelector()
        })
    }

    actual fun showPalette() {
        showCommandPalette = true
    }

    actual fun hidePalette() {
        showCommandPalette = false
    }

    actual fun addCommand(vararg command: Command) {
        commands.addAll(command)
    }

    actual fun addRecentlyUsedCommand(command: Command) {
        recentlyUsedCommands.add(command)
    }
}
