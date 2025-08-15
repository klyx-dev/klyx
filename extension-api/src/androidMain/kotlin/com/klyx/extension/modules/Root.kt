@file:OptIn(ExperimentalWasmApi::class)

package com.klyx.extension.modules

import com.klyx.core.logging.logger
import com.klyx.extension.api.Worktree
import com.klyx.extension.api.toWasmOption
import com.klyx.extension.api.toWasmResult
import com.klyx.pointer.asPointer
import com.klyx.pointer.dropPtr
import com.klyx.pointer.value
import com.klyx.wasm.ExperimentalWasmApi
import com.klyx.wasm.WasmMemory
import com.klyx.wasm.annotations.HostFunction
import com.klyx.wasm.annotations.HostModule
import com.klyx.wasm.toBuffer
import com.klyx.wasm.type.collections.toWasmList
import com.klyx.wasm.type.toWasm

@HostModule("\$root")
object Root {
    private val logger = logger("RootModule")

    @HostFunction("[resource-drop]worktree")
    fun dropWorktree(worktreePtr: Int) {
        val ptr = worktreePtr.asPointer()

        if (!dropPtr(ptr)) {
            logger.warn("Failed to drop worktree (ptr: $ptr)")
        }
    }

    @HostFunction("[method]worktree.read-text-file")
    fun WasmMemory.worktreeReadTextFile(ptr: Int, path: String, retPtr: Int) {
        val worktree = ptr.asPointer().value<Worktree>()
        val result = worktree.readTextFile(path).toWasmResult()
        write(retPtr, result.toBuffer())
    }

    @HostFunction("[method]worktree.id")
    fun worktreeId(ptr: Int) = ptr.asPointer().value<Worktree>().id

    @HostFunction("[method]worktree.root-path")
    fun WasmMemory.worktreeRootPath(ptr: Int, retPtr: Int) {
        val worktree = ptr.asPointer().value<Worktree>()
        val rootPath = worktree.rootPath.toWasm()
        write(retPtr, rootPath.toBuffer())
    }

    @HostFunction("[method]worktree.which")
    fun WasmMemory.worktreeWhich(ptr: Int, binaryName: String, retPtr: Int) {
        val worktree = ptr.asPointer().value<Worktree>()
        val result = worktree.which(binaryName).toWasmOption()
        write(retPtr, result.toBuffer())
    }

    @HostFunction("[method]worktree.shell-env")
    fun WasmMemory.worktreeShellEnv(ptr: Int, retPtr: Int) {
        val worktree = ptr.asPointer().value<Worktree>()
        val envVars = worktree.shellEnv().toWasmList()
        write(retPtr, envVars.toBuffer())
    }
}
