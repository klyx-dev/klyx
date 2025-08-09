package com.klyx.terminal

import android.content.Context
import com.klyx.core.ProcessBuilder
import com.klyx.core.process
import com.klyx.terminal.internal.linker
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@PublishedApi
context(context: Context)
internal fun isCmdAvailableInLocalPath(cmd: String) = run {
    klyxBinDir.resolve(cmd).exists()
}

@OptIn(ExperimentalContracts::class)
context(context: Context)
inline fun localProcess(
    vararg commands: String,
    useProotIfCmdIsNotAvailableLocally: Boolean = true,
    useLinker: Boolean = true,
    block: ProcessBuilder.() -> Unit = {}
): ProcessBuilder {
    contract { callsInPlace(block, InvocationKind.AT_MOST_ONCE) }

    val cmd = commands.first()
    return if (isCmdAvailableInLocalPath(cmd)) {
        process(
            *listOfNotNull(
                if (useLinker) linker else null,
                "${klyxBinDir.absolutePath}/$cmd",
                *commands.drop(1).toTypedArray()
            ).toTypedArray()
        ) {
            //env("PWD", klyxFilesDir.absolutePath)
            //workingDirectory(klyxFilesDir)
        }.apply(block)
    } else if (useProotIfCmdIsNotAvailableLocally) {
        ubuntuProcess(*commands).apply(block)
    } else {
        process(cmd, *commands.drop(1).toTypedArray()).apply(block)
    }
}
