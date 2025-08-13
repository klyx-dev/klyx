@file:OptIn(ExperimentalWasmApi::class)

package com.klyx.extension.modules.impl

import com.klyx.core.logging.logger
import com.klyx.extension.api.Worktree
import com.klyx.extension.api.toWasmOption
import com.klyx.extension.api.toWasmResult
import com.klyx.extension.api.worktreeFunction
import com.klyx.pointer.asPointer
import com.klyx.pointer.value
import com.klyx.wasm.ExperimentalWasmApi
import com.klyx.wasm.HostModuleScope
import com.klyx.wasm.signature
import com.klyx.wasm.toBuffer
import com.klyx.wasm.type.WasmString
import com.klyx.wasm.type.collections.toWasmList
import com.klyx.wasm.utils.i32
import com.klyx.wasm.utils.toLittleEndianBytes

private val logger = logger("Worktree")

fun HostModuleScope.worktreeFunctions() {
    worktreeFunction(
        "read-text-file",
        signature {
            // params: handle, path.ptr, path.len, retptr
            listOf(i32, i32, i32, i32) returns null
        }
    ) { args ->
        val worktreePtr = args[0].asPointer()
        val path = args.string(1)
        val retPtr = args.i32(3)

        val worktree: Worktree = try {
            worktreePtr.value()
        } catch (ex: Exception) {
            logger.error("Failed to get worktree: ${ex.message}")
            memory.write(retPtr, 0.toLittleEndianBytes())
            return@worktreeFunction null
        }

        with(memory) {
            val result = worktree.readTextFile(path).toWasmResult()
            write(retPtr, result.toBuffer())
        }
        null
    }

    worktreeFunction("id", signature { i32 returns i64 }) { args ->
        val worktree = try {
            args[0].asPointer().value<Worktree>()
        } catch (ex: Exception) {
            logger.error("Failed to get worktree: ${ex.message}")
            return@worktreeFunction null
        }
        longArrayOf(worktree.id)
    }

    worktreeFunction("root-path", signature { i32 + i32 returns null }) { args ->
        val worktreePtr = args[0].asPointer()
        val retPtr = args[1].i32

        val worktree = try {
            worktreePtr.value<Worktree>()
        } catch (ex: Exception) {
            logger.error("Failed to get worktree: ${ex.message}")
            memory.write(retPtr, 0.toLittleEndianBytes())
            return@worktreeFunction null
        }

        with(memory) {
            val rootPath = WasmString(worktree.rootPath)
            write(retPtr, rootPath.toBuffer())
        }
        null
    }

    worktreeFunction("which", signature { listOf(i32, i32, i32, i32) returns null }) { args ->
        val worktreePtr = args[0].asPointer()
        val binaryName = args.string(1)
        val retPtr = args.i32(3)

        val worktree = try {
            worktreePtr.value<Worktree>()
        } catch (ex: Exception) {
            logger.error("Failed to get worktree: ${ex.message}")
            memory.write(retPtr, 0.toLittleEndianBytes())
            return@worktreeFunction null
        }

        with(memory) {
            val which = worktree.which(binaryName).toWasmOption()
            write(retPtr, which.toBuffer())
        }
        null
    }

    worktreeFunction("shell-env", signature { listOf(i32, i32) returns null }) { args ->
        val worktreePtr = args[0].asPointer()
        val retPtr = args[1].i32

        val worktree = try {
            worktreePtr.value<Worktree>()
        } catch (ex: Exception) {
            logger.error("Failed to get worktree: ${ex.message}")
            memory.write(retPtr, 0.toLittleEndianBytes())
            return@worktreeFunction null
        }

        with(memory) {
            val envVars = worktree.shellEnv().toWasmList()
            write(retPtr, envVars.toBuffer())
        }

        null
    }
}
