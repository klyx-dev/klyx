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

@PublishedApi
context(context: Context)
internal fun isCmdAvailableInLocalPath(cmd: String) = run {
    klyxBinDir.resolve(cmd).exists()
}

@OptIn(ExperimentalContracts::class, ExperimentalTypeInference::class)
context(context: Context)
inline fun localProcess(
    vararg commands: String,
    useProotIfCmdIsNotAvailableLocally: Boolean = true,
    @BuilderInference
    block: ProcessBuilder.() -> Unit = {}
): ProcessBuilder {
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }

    val cmd = commands.first()
    return if (isCmdAvailableInLocalPath(cmd)) {
        process(
            linker,
            "${klyxBinDir.absolutePath}/$cmd",
            *commands.drop(1).toTypedArray()
        ) {
            env("PWD", klyxFilesDir.absolutePath)
            workingDirectory(klyxFilesDir)
        }.apply(block)
    } else if (useProotIfCmdIsNotAvailableLocally) {
        process(
            linker,
            "${klyxBinDir.absolutePath}/proot",
            *buildProotArgs(currentUser, withInitScript = false),
            cmd,
            *commands.drop(1).toTypedArray()
        ) {
            env(
                "PATH", """
                /bin:/sbin:/usr/bin:/usr/sbin:/usr/local/bin:/usr/local/sbin:/system/bin:/system/xbin
            """.trimIndent()
            )
            env("PROOT_TMP_DIR", klyxFilesDir.resolve("usr/tmp").absolutePath)
        }.apply(block)
    } else {
        process(cmd, *commands.drop(1).toTypedArray()).apply(block)
    }
}
