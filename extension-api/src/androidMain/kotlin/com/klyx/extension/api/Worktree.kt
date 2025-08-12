package com.klyx.extension.api

import com.klyx.core.Environment
import com.klyx.fs.canExecute
import com.klyx.wasm.ExperimentalWasmApi
import com.klyx.wasm.HostFnSync
import com.klyx.wasm.HostModuleScope
import com.klyx.wasm.WasmSignature
import com.klyx.wasm.signature
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.datetime.Clock
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.buffer
import java.io.File

class Worktree(
    val id: Long,
    val rootPath: Path
) : AutoCloseable {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val fs = FileSystem.SYSTEM

    fun readTextFile(path: String) = run {
        val source = fs.source(rootPath.resolve(path.toPath()))
        source.buffer().readUtf8()
    }

    fun which(binaryName: String): String {
        val paths = System.getenv("PATH")?.split(File.pathSeparatorChar) ?: emptyList()

        for (pathDir in paths) {
            val binaryPath = pathDir.toPath().resolve(binaryName)

            if (fs.exists(binaryPath) && !fs.metadata(binaryPath).isDirectory) {
                if (!binaryPath.toString().canExecute()) {
                    //throw RuntimeException("Binary '$binaryName' is not executable")
                }
                return binaryPath.toString()
            }
        }

        throw RuntimeException("Binary '$binaryName' not found in PATH")
    }

    fun shellEnv(): List<Pair<String, String>> {
        return System.getenv().map { entry ->
            entry.key to entry.value
        }
    }

    override fun close() {
        scope.cancel("Worktree $id closed")
    }
}

val SystemWorktree = Worktree(Environment.DeviceHomeDir)

fun Worktree(path: String) = run {
    val id = Clock.System.now().toEpochMilliseconds()
    Worktree(id, path.toPath(true))
}

@OptIn(ExperimentalWasmApi::class)
fun HostModuleScope.worktreeFunction(
    name: String,
    signature: WasmSignature = signature { none },
    implementation: HostFnSync
) = function("[method]worktree.$name", signature, implementation)
