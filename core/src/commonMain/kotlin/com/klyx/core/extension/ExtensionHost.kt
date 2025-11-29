@file:OptIn(ExperimentalContracts::class)

package com.klyx.core.extension

import arrow.core.Option
import arrow.core.none
import arrow.core.some
import com.klyx.core.file.fs
import com.klyx.core.io.Paths
import com.klyx.core.io.extensionsDir
import com.klyx.core.io.join
import com.klyx.core.noderuntime.NodeRuntime
import kotlinx.atomicfu.atomic
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

interface WasmHost {
    val workDir
        get() = Paths.extensionsDir.join("work")
            .also { path ->
                if (!fs.exists(path)) {
                    fs.createDirectories(path, mustCreate = true)
                }
            }
}

val WasmHost.nodeRuntime
    get() = ExtensionHost.nodeRuntime.getOrNull()
        ?: error("Node.js runtime not initialized. Did you forget call ExtensionHost.init() before using this function?")

object ExtensionHost {
    var nodeRuntime: Option<NodeRuntime> by atomic(none())

    fun init(nodeRuntime: NodeRuntime) {
        this.nodeRuntime = nodeRuntime.some()
    }
}

inline fun <R> WasmHost.withNodeRuntime(block: NodeRuntime.() -> R): R {
    contract {
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }
    return nodeRuntime.block()
}
