package com.klyx.extension.capabilities

import kotlinx.serialization.Serializable

/**
 * @property command The command to execute.
 * @property args The arguments to pass to the command. Use `*` for a single wildcard argument.
 *                If the last element is `**`, then any trailing arguments are allowed.
 */
@Serializable
data class ProcessExecCapability(
    val command: String,
    val args: List<String>
) {
    /**
     * Returns whether the capability allows the given command and arguments.
     */
    fun allows(desiredCommand: String, desiredArgs: List<String>): Boolean {
        if (command != desiredCommand && command != "*") return false

        for ((ix, arg) in args.withIndex()) {
            if (arg == "**") return true
            if (ix >= desiredArgs.size) return false
            if (arg != "*" && arg != desiredArgs[ix]) return false
        }

        return args.size >= desiredArgs.size
    }
}
