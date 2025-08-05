package com.klyx.terminal

import android.content.Context
import com.klyx.core.ProcessBuilder
import com.klyx.core.process
import com.klyx.terminal.internal.buildProotArgs
import com.klyx.terminal.internal.currentUser
import com.klyx.terminal.internal.linker
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.experimental.ExperimentalTypeInference

@OptIn(ExperimentalTypeInference::class, ExperimentalContracts::class)
context(context: Context)
fun ubuntuProcess(
    vararg commands: String,
    @BuilderInference
    block: ProcessBuilder.() -> Unit = {}
): ProcessBuilder {
    contract { callsInPlace(block, InvocationKind.AT_MOST_ONCE) }

    if (!ubuntuDir.exists() || ubuntuDir.list()?.isEmpty() == true) {
        throw RuntimeException("Ubuntu rootfs is not initialized")
    }

    return process(
        linker,
        "${klyxBinDir.absolutePath}/proot",
        *buildProotArgs(currentUser, withInitScript = false),
        commands.first(),
        *commands.drop(1).toTypedArray()
    ) {
        env(
            "PATH", """
                /bin:/sbin:/usr/bin:/usr/sbin:/usr/local/bin:/usr/local/sbin:/system/bin:/system/xbin
            """.trimIndent()
        )
        env("PROOT_TMP_DIR", klyxFilesDir.resolve("usr/tmp").absolutePath)
    }.apply(block)
}
