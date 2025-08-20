package com.klyx.extension.api

import com.klyx.core.Environment
import com.klyx.core.io.isExecutable
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
import okio.Path.Companion.toPath
import okio.buffer
import java.io.File

class Worktree(
    val id: Long,
    val rootPath: String
) : AutoCloseable {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val fs = FileSystem.SYSTEM

    private val worktreePath = rootPath.toPath(true)

    fun readTextFile(path: String): Result<String, String> {
        return try {
            val source = fs.source(worktreePath.resolve(path.toPath()))
            Ok(source.buffer().readUtf8())
        } catch (err: Exception) {
            Err(err.message ?: "Failed to read text file: $path in worktree $id")
        }
    }

    fun which(binaryName: String): Option<String> {
        val paths = System.getenv("PATH")?.split(File.pathSeparatorChar).orEmpty()

        for (pathDir in paths) {
            val binaryPath = pathDir.toPath().resolve(binaryName)

            if (fs.exists(binaryPath) && !fs.metadata(binaryPath).isDirectory) {
                if (!binaryPath.isExecutable()) {
                    //throw RuntimeException("Binary '$binaryName' is not executable")
                }
                return Some(binaryPath.toString())
            }
        }

        return None
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
    Worktree(id, path)
}

@OptIn(ExperimentalWasmApi::class)
internal fun HostModuleScope.worktreeFunction(
    name: String,
    signature: WasmSignature = signature { none },
    implementation: HostFnSync
) = function("[method]worktree.$name", signature, implementation)
