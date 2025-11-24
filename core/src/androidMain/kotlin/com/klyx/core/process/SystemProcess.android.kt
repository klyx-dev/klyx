@file:OptIn(ExperimentalContracts::class)

package com.klyx.core.process

import android.content.Context
import android.os.Process
import com.klyx.core.file.createDirIfNotExist
import com.klyx.core.terminal.currentUser
import com.klyx.core.terminal.internal.buildProotArgs
import com.klyx.core.terminal.klyxBinDir
import com.klyx.core.terminal.klyxFilesDir
import com.klyx.core.terminal.sandboxDir
import com.klyx.core.withAndroidContext
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

val linker = if (Process.is64Bit()) "/system/bin/linker64" else "/system/bin/linker"

@PublishedApi
context(context: Context)
internal fun isCmdAvailableInLocalPath(cmd: String) = run {
    klyxBinDir.resolve(cmd).exists()
}

actual inline fun systemProcess(command: String, block: KxProcessBuilder.() -> Unit): KxProcess {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }

    return withAndroidContext {
        if (!sandboxDir.exists() || sandboxDir.list()?.isEmpty() == true) {
            systemProcessLogger.warn { "Sandbox environment not found or empty; falling back to system binaries." }

            if (isCmdAvailableInLocalPath(command)) {
                systemProcessLogger.debug { "Command found in local path. Executing via linker: $linker" }
                process(command = linker) {
                    args("${klyxBinDir.absolutePath}/$command")
                    block()
                }
            } else {
                systemProcessLogger.debug { "Command not found in local path. Attempting direct execution: $command" }
                process(command, block)
            }
        } else {
            process(command = linker) {
                args("${klyxBinDir.absolutePath}/proot")
                args(buildProotArgs(user = currentUser, commands = listOf(command)))
                environment { putAll(prootEnvVars()) }
                block()
            }
        }
    }
}

actual inline fun systemProcess(commands: Array<out String>, block: KxProcessBuilder.() -> Unit): KxProcess {
    contract { callsInPlace(block, InvocationKind.AT_MOST_ONCE) }
    require(commands.isNotEmpty()) { "commands cannot be empty" }

    return withAndroidContext {
        if (!sandboxDir.exists() || sandboxDir.list()?.isEmpty() == true) {
            systemProcessLogger.warn { "Sandbox environment not found or empty; falling back to system binaries." }

            if (isCmdAvailableInLocalPath(commands.first())) {
                systemProcessLogger.debug { "Command found in local path. Executing via linker: $linker" }
                process(command = linker) {
                    args("${klyxBinDir.absolutePath}/${commands.first()}")
                    args(commands.drop(1))
                    block()
                }
            } else {
                systemProcessLogger.debug {
                    "Command not found in local path. Attempting direct " +
                            "execution: ${commands.joinToString(" ")}"
                }
                process(commands, block)
            }
        } else {
            process(command = linker) {
                args("${klyxBinDir.absolutePath}/proot")
                args(buildProotArgs(user = currentUser, commands = commands.asList()))
                environment { putAll(prootEnvVars()) }
                block()
            }
        }
    }
}

context(ctx: Context)
fun prootEnvVars() = buildMap {
    put("PATH", "/bin:/sbin:/usr/bin:/usr/sbin:/usr/local/bin:/usr/local/sbin:/system/bin:/system/xbin")
    put("PROOT_TMP_DIR", "${klyxFilesDir.resolve("usr/tmp").createDirIfNotExist().absolutePath}")
}

fun String.splitEnvVar(): Pair<String, String> {
    val s = split('=', limit = 2)
    return s[0] to s[1]
}
