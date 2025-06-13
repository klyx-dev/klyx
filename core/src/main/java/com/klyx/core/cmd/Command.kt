package com.klyx.core.cmd

data class Command(
    val name: String,
    val description: String? = null,
    val keybinding: String? = null,
    val action: Command.() -> Unit
)


