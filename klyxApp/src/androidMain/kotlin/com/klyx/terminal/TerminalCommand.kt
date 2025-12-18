package com.klyx.terminal

import com.klyx.core.generateId
import java.io.Serializable

@Suppress("SerialVersionUIDInSerializableClass")
data class TerminalCommand(
    val cmd: String,
    val args: Array<String> = emptyArray(),
    val env: Array<String> = emptyArray(),
    val sandbox: Boolean = true,
    val cwd: String? = null,
    val id: String = generateId(),
    val terminatePreviousSession: Boolean = true
) : Serializable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TerminalCommand

        if (terminatePreviousSession != other.terminatePreviousSession) return false
        if (cmd != other.cmd) return false
        if (!args.contentEquals(other.args)) return false
        if (!env.contentEquals(other.env)) return false
        if (cwd != other.cwd) return false
        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        var result = terminatePreviousSession.hashCode()
        result = 31 * result + cmd.hashCode()
        result = 31 * result + args.contentHashCode()
        result = 31 * result + env.contentHashCode()
        result = 31 * result + (cwd?.hashCode() ?: 0)
        result = 31 * result + id.hashCode()
        return result
    }
}
