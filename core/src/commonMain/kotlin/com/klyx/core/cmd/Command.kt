package com.klyx.core.cmd

import com.klyx.core.cmd.key.KeyShortcut
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.experimental.ExperimentalTypeInference

@DslMarker
private annotation class CommandDsl

data class Command(
    val name: String,
    val description: String? = null,
    @Deprecated(
        message = "Use `shortcuts` instead.",
        replaceWith = ReplaceWith("shortcuts")
    )
    val shortcutKey: String? = null,
    val shortcuts: List<KeyShortcut> = emptyList(),
    val execute: Command.() -> Unit
) {
    fun run() = execute(this)
}

@CommandDsl
class CommandBuilder {
    private var name: String? = null
    private var description: String? = null
    private var shortcuts = mutableListOf<KeyShortcut>()
    private var execute: Command.() -> Unit = {
        error("No execution logic provided for command '${this.name}'")
    }

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

    fun shortcut(shortcuts: Collection<KeyShortcut>) = shortcut(*shortcuts.toTypedArray())

    @OptIn(ExperimentalTypeInference::class)
    fun execute(@BuilderInference block: Command.() -> Unit) = apply {
        this.execute = block
    }

    fun build(): Command {
        val finalName = requireNotNull(name) { "Command must have a name." }
        return Command(
            name = finalName,
            description = description,
            shortcutKey = shortcuts.joinToString(" ") { it.toString() },
            shortcuts = shortcuts,
            execute = execute
        )
    }
}

@OptIn(ExperimentalContracts::class, ExperimentalTypeInference::class)
inline fun command(
    @BuilderInference block: CommandBuilder.() -> Unit
): Command {
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
    return CommandBuilder().apply(block).build()
}
