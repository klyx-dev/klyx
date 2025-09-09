@file:OptIn(ExperimentalWasmApi::class)

package com.klyx.extension.modules

import com.github.michaelbull.result.fold
import com.github.michaelbull.result.runCatching
import com.klyx.extension.api.github.getReleaseByTagName
import com.klyx.extension.internal.GithubRelease
import com.klyx.wasm.ExperimentalWasmApi
import com.klyx.wasm.WasmMemory
import com.klyx.wasm.annotations.HostFunction
import com.klyx.wasm.annotations.HostModule
import com.klyx.wasm.type.Err
import com.klyx.wasm.type.Ok
import com.klyx.wasm.type.toBuffer
import com.klyx.wasm.type.wstr
import kotlinx.coroutines.runBlocking

@HostModule("klyx:extension/github")
object GitHub {
    @HostFunction
    fun latestGithubRelease(
        memory: WasmMemory,
        repo: String,
        requireAssets: Boolean,
        preRelease: Boolean,
        resultPtr: Int
    ) = runBlocking {
        val release = runCatching {
            com.klyx.extension.api.github.latestGithubRelease(repo, requireAssets, preRelease)
        }

        val result = release.fold(
            success = { Ok(GithubRelease.from(it, memory)) },
            failure = { with(memory) { Err((it.message ?: "Unknown error").wstr) } }
        )

        memory.write(resultPtr, result.toBuffer())
    }

    @HostFunction
    fun WasmMemory.githubReleaseByTagName(repo: String, tag: String, resultPtr: Int) = runBlocking {
        val release = runCatching {
            getReleaseByTagName(repo, tag)
        }

        val result = release.fold(
            success = { Ok(GithubRelease.from(it, this@githubReleaseByTagName)) },
            failure = { Err((it.message ?: "Unknown error").wstr) }
        )

        write(resultPtr, result.toBuffer())
    }
}
