@file:OptIn(ExperimentalWasmApi::class)

package com.klyx.extension.modules.impl

import com.klyx.core.logging.logger
import com.klyx.extension.api.Worktree
import com.klyx.extension.api.worktreeFunction
import com.klyx.extension.wasm.WasmOption
import com.klyx.extension.wasm.WasmResult
import com.klyx.extension.wasm.toWasmString
import com.klyx.pointer.asPointer
import com.klyx.pointer.value
import com.klyx.wasm.ExperimentalWasmApi
import com.klyx.wasm.HostModuleScope
import com.klyx.wasm.alloc
import com.klyx.wasm.signature
import com.klyx.wasm.utils.i32
import com.klyx.wasm.utils.toBytesLE
import com.klyx.wasm.utils.toLittleEndianBytes
import com.klyx.wasm.utils.writeInt32LE
import com.klyx.wasm.write
import com.klyx.wasm.writeString

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

        val result = runCatching {
            val content = worktree.readTextFile(path)
            val (ptr, len) = memory.writeString(content)
            Pair(ptr, len)
        }

        val wasmResult = result.fold(
            onSuccess = { (dataPtr, dataLen) ->
                WasmResult.success(dataPtr, dataLen)
            },
            onFailure = { ex ->
                val (ptr, len) = memory.writeString(ex.message ?: "unknown error")
                WasmResult.error(ptr, len)
            }
        )
        memory.write(retPtr, wasmResult.toBuffer())
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
        val rootPath = worktree.rootPath.toString()

        val str = memory.write(rootPath.toBytesLE()).toWasmString()
        memory.write(retPtr, str.toBuffer())
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

        val result = runCatching {
            val which = worktree.which(binaryName)
            val (ptr, len) = memory.writeString(which)
            Pair(ptr, len)
        }

        val option = result.fold(
            onSuccess = { (ptr, len) ->
                WasmOption.some(ptr, len)
            },
            onFailure = { ex ->
                WasmOption.none()
            }
        )

        memory.write(retPtr, option.toBuffer())
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

        val envVars = worktree.shellEnv()

        // each tuple: 4 ints (ptr,len ptr,len) = 4 * 4 bytes
        val tupleSize = 16

        val tuplesCount = envVars.size
        val tuplesBytes = ByteArray(tupleSize * tuplesCount)

        for ((index, envVar) in envVars.withIndex()) {
            val (key, value) = envVar

            val (keyPtr, keyLen) = memory.writeString(key)
            val (valPtr, valLen) = memory.writeString(value)

            val offset = index * tupleSize
            tuplesBytes.writeInt32LE(keyPtr, offset)
            tuplesBytes.writeInt32LE(keyLen, offset + 4)
            tuplesBytes.writeInt32LE(valPtr, offset + 8)
            tuplesBytes.writeInt32LE(valLen, offset + 12)
        }

        val listPtr = instance.alloc(tuplesBytes.size)
        memory.write(listPtr, tuplesBytes)

        // According to WIT-bindgen convention for lists:
        // struct { ptr: u32, len: u32 }
        val retBuf = ByteArray(8)
        retBuf.writeInt32LE(listPtr, 0)
        retBuf.writeInt32LE(tuplesCount, 4)

        memory.write(retPtr, retBuf)
        null
    }
}
