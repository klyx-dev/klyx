package com.klyx.core.process

import com.klyx.core.logging.logger
import kotlin.jvm.JvmName

@PublishedApi
internal val systemProcessLogger = logger("SystemProcess")

/**
 * **Note**: If the process runs inside `sandbox` (on Android), passing arguments via [block] is not valid.
 */
expect inline fun systemProcess(command: String, block: KxProcessBuilder.() -> Unit = {}): KxProcess

/**
 * **Note**: If the process runs inside `sandbox` (on Android), passing arguments via [block] is not valid.
 * You must provide all arguments within the [commands] array instead.
 */
expect inline fun systemProcess(commands: Array<out String>, block: KxProcessBuilder.() -> Unit = {}): KxProcess

/**
 * **Note**: If the process runs inside `sandbox` (on Android), passing arguments via [block] is not valid.
 * You must provide all arguments within the [commands] array instead.
 */
@JvmName("systemProcess0")
inline fun systemProcess(vararg commands: String, block: KxProcessBuilder.() -> Unit = {}): KxProcess {
    return if (commands.size == 1) {
        val command = commands[0]
        systemProcess(command, block)
    } else {
        systemProcess(commands, block)
    }
}
