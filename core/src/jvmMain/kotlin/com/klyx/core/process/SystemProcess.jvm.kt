package com.klyx.core.process

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@OptIn(ExperimentalContracts::class)
actual inline fun systemProcess(command: String, block: KxProcessBuilder.() -> Unit): KxProcess {
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
    return process(command, block)
}

@OptIn(ExperimentalContracts::class)
actual inline fun systemProcess(commands: Array<out String>, block: KxProcessBuilder.() -> Unit): KxProcess {
    contract { callsInPlace(block, InvocationKind.AT_MOST_ONCE) }
    return process(commands, block)
}
