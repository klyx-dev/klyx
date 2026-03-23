@file:OptIn(ExperimentalContracts::class)

package com.klyx.core.process

import android.os.Process
import com.klyx.core.io.Paths
import com.klyx.core.io.fs
import com.klyx.core.io.root
import com.klyx.core.terminal.TerminalManager
import com.klyx.core.terminal.internal.buildProotArgs
import com.klyx.core.util.join
import com.klyx.core.withAndroidContext
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

val linker = if (Process.is64Bit()) "/system/bin/linker64" else "/system/bin/linker"

@PublishedApi
internal fun isCmdAvailableInLocalPath(cmd: String) = fs.exists(Paths.dataDir.join("files/usr/bin").join(cmd))

actual inline fun systemProcess(command: String, block: KxProcessBuilder.() -> Unit): KxProcess {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }

    val klyxBinDir = Paths.dataDir.join("files/usr/bin")
    val sandboxDir = Paths.root

    return withAndroidContext {
        if (!fs.exists(sandboxDir) || fs.list(sandboxDir).isEmpty()) {
            systemProcessLogger.warn { "Sandbox environment not found or empty; falling back to system binaries." }

            if (isCmdAvailableInLocalPath(command)) {
                systemProcessLogger.debug { "Command found in local path. Executing via linker: $linker" }
                process(command = linker) {
                    args("${klyxBinDir}/$command")
                    block()
                }
            } else {
                systemProcessLogger.debug { "Command not found in local path. Attempting direct execution: $command" }
                process(command, block)
            }
        } else {
            process(command = linker) {
                args("${klyxBinDir}/proot")
                args(buildProotArgs(user = TerminalManager.currentUser, commands = listOf(command)))
                environment { putAll(prootEnvVars()) }
                block()
            }
        }
    }
}

actual inline fun systemProcess(commands: Array<out String>, block: KxProcessBuilder.() -> Unit): KxProcess {
    contract { callsInPlace(block, InvocationKind.AT_MOST_ONCE) }
    require(commands.isNotEmpty()) { "commands cannot be empty" }

    val klyxBinDir = Paths.dataDir.join("files/usr/bin")
    val sandboxDir = Paths.root

    return withAndroidContext {
        if (!fs.exists(sandboxDir) || fs.list(sandboxDir).isEmpty()) {
            systemProcessLogger.warn { "Sandbox environment not found or empty; falling back to system binaries." }

            if (isCmdAvailableInLocalPath(commands.first())) {
                systemProcessLogger.debug { "Command found in local path. Executing via linker: $linker" }
                process(command = linker) {
                    args("${klyxBinDir}/${commands.first()}")
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
                args("${klyxBinDir}/proot")
                args(buildProotArgs(user = TerminalManager.currentUser, commands = commands.asList()))
                environment { putAll(prootEnvVars()) }
                block()
            }
        }
    }
}

fun prootEnvVars() = buildMap {
    put("PATH", "/bin:/sbin:/usr/bin:/usr/sbin:/usr/local/bin:/usr/local/sbin:/system/bin:/system/xbin")
    val prootTmpDir = Paths.dataDir.join("files", "usr/tmp")
    fs.createDirectories(prootTmpDir)
    put("PROOT_TMP_DIR", "$prootTmpDir")
}
