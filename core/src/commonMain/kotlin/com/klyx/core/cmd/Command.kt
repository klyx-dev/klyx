package com.klyx.core.cmd

import com.klyx.core.cmd.key.KeyShortcut
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@DslMarker
private annotation class CommandDsl

data class Command(
    val name: String,
    val description: String? = null,
    val isHiddenInCommandPalette: Boolean = false,
    val dismissOnAction: Boolean = true,
    val shortcuts: List<KeyShortcut> = emptyList(),
    val action: CommandAction?
) {
    suspend fun run() {
        action?.run(this)
    }
}

fun interface CommandAction {
    suspend fun run(command: Command)
}

@CommandDsl
class CommandBuilder {
    private var name: String? = null
    private var description: String? = null
    private var shortcuts = mutableListOf<KeyShortcut>()
    private var action: CommandAction? = null
    private var hideInCmdPalette = false
    private var dismissOnAction = true

    fun name(name: String) = apply {
        require(name.isNotBlank()) { "Command name cannot be blank." }
        this.name = name
    }

    fun description(description: String?) = apply {
        this.description = description?.takeIf { it.isNotBlank() }
    }

    fun shortcut(vararg shortcut: KeyShortcut) = apply {
        shortcuts += shortcut
    }

    fun shortcut(shortcuts: Collection<KeyShortcut>) = apply {
        this.shortcuts += shortcuts
    }

    fun hideInCommandPalette() = apply { hideInCmdPalette = true }
    fun dismissOnAction(dismiss: Boolean) = apply { dismissOnAction = dismiss }

    fun action(action: CommandAction) = apply { this.action = action }

    fun build(): Command {
        if (!hideInCmdPalette) requireNotNull(name) { "Command must have a name." }

        return Command(
            name = name.orEmpty(),
            description = description,
            shortcuts = shortcuts,
            dismissOnAction = dismissOnAction,
            isHiddenInCommandPalette = hideInCmdPalette,
            action = action
        )
    }
}

@OptIn(ExperimentalContracts::class)
inline fun buildCommand(block: CommandBuilder.() -> Unit): Command {
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
    return CommandBuilder().apply(block).build()
}
