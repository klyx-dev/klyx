package com.klyx.terminal

import android.content.Context
import com.klyx.core.ProcessBuilder
import com.klyx.core.process
import com.klyx.terminal.internal.linker
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.experimental.ExperimentalTypeInference

@OptIn(ExperimentalContracts::class, ExperimentalTypeInference::class)
context(context: Context)
inline fun localProcess(
    vararg commands: String,
    @BuilderInference
    block: ProcessBuilder.() -> Unit = {}
): ProcessBuilder {
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
    return process(
        //linker,
        "${klyxBinDir.absolutePath}/${commands.first()}",
        *commands.drop(1).toTypedArray()
    ) {
        env("PWD", klyxFilesDir.absolutePath)
        workingDirectory(klyxFilesDir)
    }.apply(block)
}
