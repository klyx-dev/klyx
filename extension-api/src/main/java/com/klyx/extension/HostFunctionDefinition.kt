package com.klyx.extension

import kwasm.api.HostFunction

data class HostFunctionDefinition(
    val name: String,
    val function: HostFunction<*>
)

