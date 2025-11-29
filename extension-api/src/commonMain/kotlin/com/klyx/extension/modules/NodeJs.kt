@file:OptIn(ExperimentalWasmApi::class)

package com.klyx.extension.modules

import com.klyx.core.extension.WasmHost
import com.klyx.core.extension.nodeRuntime
import com.klyx.core.map
import com.klyx.extension.internal.toWasmOption
import com.klyx.extension.internal.toWasmResult
import com.klyx.wasm.ExperimentalWasmApi
import com.klyx.wasm.WasmMemory
import com.klyx.wasm.annotations.HostFunction
import com.klyx.wasm.annotations.HostModule
import com.klyx.wasm.type.toBuffer

@HostModule(name = "klyx:extension/nodejs")
object NodeJs : WasmHost {
    @HostFunction
    suspend fun WasmMemory.nodeBinaryPath(returnPtr: Int) {
        val result = nodeRuntime
            .binaryPath()
            .map { it.toString() }
            .toWasmResult()
        write(returnPtr, result.toBuffer())
    }

    @HostFunction
    suspend fun WasmMemory.npmPackageLatestVersion(packageName: String, returnPtr: Int) {
        val result = nodeRuntime
            .npmPackageLatestVersion(packageName)
            .toWasmResult()
        write(returnPtr, result.toBuffer())
    }

    @HostFunction
    suspend fun WasmMemory.npmPackageInstalledVersion(packageName: String, returnPtr: Int) {
        val result = nodeRuntime
            .npmPackageInstalledVersion(workDir, packageName)
            .map { it.toWasmOption() }
            .toWasmResult()
        write(returnPtr, result.toBuffer())
    }

    @HostFunction
    suspend fun WasmMemory.npmInstallPackage(packageName: String, version: String, returnPtr: Int) {
        val result = nodeRuntime
            .npmInstallPackages(workDir, mapOf(packageName to version))
            .toWasmResult()
        write(returnPtr, result.toBuffer())
    }
}
