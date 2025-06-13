package com.klyx.core.cmd

data class Command(
    val name: String,
    val description: String? = null,
    val shortcutKey: String? = null,
    val execute: Command.() -> Unit
)


