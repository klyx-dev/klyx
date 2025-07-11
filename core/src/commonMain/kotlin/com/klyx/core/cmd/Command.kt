package com.klyx.core.cmd

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.experimental.ExperimentalTypeInference

@DslMarker
annotation class CommandDsl

data class Command(
    val name: String,
    val description: String? = null,
    val shortcutKey: String? = null,
    val execute: Command.() -> Unit
) {
    fun run() = execute(this)
}

@CommandDsl
class CommandBuilder {
    private var name: String? = null
    private var description: String? = null
    private var shortcutKey: String? = null
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

    fun shortcutKey(shortcutKey: String?) = apply {
        this.shortcutKey = shortcutKey?.takeIf { it.isNotBlank() }
    }

    @OptIn(ExperimentalTypeInference::class)
    fun execute(@BuilderInference block: Command.() -> Unit) = apply {
        this.execute = block
    }

    fun build(): Command {
        val finalName = requireNotNull(name) { "Command must have a name." }
        return Command(finalName, description, shortcutKey, execute)
    }
}

@OptIn(ExperimentalContracts::class, ExperimentalTypeInference::class)
inline fun command(
    @BuilderInference block: CommandBuilder.() -> Unit
): Command {
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
    return CommandBuilder().apply(block).build()
}
