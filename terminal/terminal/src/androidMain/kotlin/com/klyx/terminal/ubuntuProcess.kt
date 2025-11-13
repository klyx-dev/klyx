@file:Suppress("SpreadOperator")

package com.klyx.terminal

import android.content.Context
import com.klyx.core.ProcessBuilder
import com.klyx.core.process
import com.klyx.terminal.internal.buildProotArgs
import com.klyx.terminal.internal.currentUser
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@OptIn(ExperimentalContracts::class)
context(context: Context)
inline fun ubuntuProcess(
    vararg commands: String,
    loginUser: Boolean = true,
    block: ProcessBuilder.() -> Unit = {}
): ProcessBuilder {
    contract { callsInPlace(block, InvocationKind.AT_MOST_ONCE) }

    if (!sandboxDir.exists() || sandboxDir.list()?.isEmpty() == true) {
        error("Ubuntu rootfs is not initialized")
    }

    return process(
        arrayOf(
            linker,
            "${klyxBinDir.absolutePath}/proot",
            *buildProotArgs(
                currentUser,
                withInitScript = false,
                loginUser = loginUser,
                commands = commands
            )
        )
    ) {
        env(
            "PATH",
            "/bin:/sbin:/usr/bin:/usr/sbin:/usr/local/bin:/usr/local/sbin:/system/bin:/system/xbin"
        )
        env("PROOT_TMP_DIR", klyxFilesDir.resolve("usr/tmp").absolutePath)
    }.apply(block)
}
