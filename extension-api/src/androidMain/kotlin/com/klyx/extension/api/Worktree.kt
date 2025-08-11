package com.klyx.extension.api

import com.klyx.core.Environment
import com.klyx.wasm.ExperimentalWasmApi
import com.klyx.wasm.HostFnSync
import com.klyx.wasm.HostModuleScope
import com.klyx.wasm.WasmSignature
import com.klyx.wasm.signature
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
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
                // TODO: Consider executable permissions check
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

@OptIn(ExperimentalWasmApi::class)
fun HostModuleScope.worktreeFunction(
    name: String,
    signature: WasmSignature = signature { none },
    implementation: HostFnSync
) = function("[method]worktree.$name", signature, implementation)

fun Worktree.borrow() = WorktreeRegistry.borrow(this)
val SystemWorktree = WorktreeRegistry.new(Environment.DeviceHomeDir)

object WorktreeRegistry {
    private val instances = mutableMapOf<Long, Worktree>()
    private var nextId = 1L

    fun new(path: String): Worktree {
        val id = nextId++
        val worktree = Worktree(id, path.toPath(true))
        instances[id] = worktree
        return worktree
    }

    operator fun get(id: Long): Worktree = instances[id] ?: error("No worktree with id $id")

    fun drop(id: Long) {
        if (instances.containsKey(id)) {
            instances.remove(id)?.close()
        } else {
            error("No worktree with id $id to drop.")
        }
    }

    fun borrow(worktree: Worktree): Long {
        val id = nextId++
        instances[id] = worktree
        return id
    }
}
