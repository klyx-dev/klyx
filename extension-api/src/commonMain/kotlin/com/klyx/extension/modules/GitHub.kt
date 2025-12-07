@file:OptIn(ExperimentalWasmApi::class)

package com.klyx.extension.modules

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
import io.itsvks.anyhow.fold
import io.itsvks.anyhow.runCatching

@HostModule("klyx:extension/github")
object GitHub {
    @HostFunction
    suspend fun latestGithubRelease(
        memory: WasmMemory,
        repo: String,
        requireAssets: Boolean,
        preRelease: Boolean,
        resultPtr: Int
    ) {
        val release = runCatching {
            com.klyx.extension.api.github.latestGithubRelease(repo, requireAssets, preRelease)
        }

        val result = release.fold(
            ok = { Ok(GithubRelease.from(it, memory)) },
            err = { with(memory) { Err(it.toString().wstr) } }
        )

        memory.write(resultPtr, result.toBuffer())
    }

    @HostFunction
    suspend fun WasmMemory.githubReleaseByTagName(repo: String, tag: String, resultPtr: Int) {
        val release = runCatching {
            getReleaseByTagName(repo, tag)
        }

        val result = release.fold(
            ok = { Ok(GithubRelease.from(it, this@githubReleaseByTagName)) },
            err = { Err((it.toString()).wstr) }
        )

        write(resultPtr, result.toBuffer())
    }
}
