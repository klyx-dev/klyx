@file:OptIn(ExperimentalWasmApi::class)

package com.klyx.extension.modules

import com.klyx.core.Notifier
import com.klyx.core.logging.logger
import com.klyx.extension.api.Worktree
import com.klyx.extension.api.lsp.parseLanguageServerInstallationStatus
import com.klyx.extension.internal.toWasmOption
import com.klyx.extension.internal.toWasmResult
import com.klyx.pointer.asPointer
import com.klyx.pointer.dropPtr
import com.klyx.pointer.value
import com.klyx.wasm.ExperimentalWasmApi
import com.klyx.wasm.WasmMemory
import com.klyx.wasm.annotations.HostFunction
import com.klyx.wasm.annotations.HostModule
import com.klyx.wasm.type.Err
import com.klyx.wasm.type.Ok
import com.klyx.wasm.type.WasmUnit
import com.klyx.wasm.type.collections.toWasmList
import com.klyx.wasm.type.toBuffer
import com.klyx.wasm.type.toWasm
import com.klyx.wasm.type.wstr
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

@HostModule("\$root")
object Root : KoinComponent {
    private val logger = logger("RootModule")
    private val notifier: Notifier by inject()

    @HostFunction("[resource-drop]worktree")
    fun dropWorktree(worktreePtr: Int) {
        val ptr = worktreePtr.asPointer()

        if (!dropPtr(ptr)) {
            logger.warn("Failed to drop worktree (pointer: $ptr)")
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
        val rootPath = worktree.rootFile.absolutePath.toWasm()
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

    @HostFunction
    fun WasmMemory.makeFileExecutable(path: String, resultPtr: Int) {
        val res = com.klyx.extension.internal.makeFileExecutable(path).toWasmResult()
        write(resultPtr, res.toBuffer())
    }

    @HostFunction
    fun WasmMemory.downloadFile(url: String, path: String, resultPtr: Int) = runBlocking {
        val result = try {
            com.klyx.core.file.downloadFile(url, path)
            Ok(WasmUnit)
        } catch (e: Exception) {
            Err("$e".wstr)
        }
        write(resultPtr, result.toBuffer())
    }

    @Suppress("unused")
    @HostFunction
    fun setLanguageServerInstallationStatus(languageServerName: String, tag: Int, failedReason: String) {
        val status = parseLanguageServerInstallationStatus(tag, failedReason)
        //TODO("Set language server installation status, not yet implemented: $status")
        logger.info { "Set language server installation status: $status" }
    }
}
