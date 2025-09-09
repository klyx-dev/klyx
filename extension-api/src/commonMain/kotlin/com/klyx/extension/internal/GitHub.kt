@file:OptIn(ExperimentalWasmApi::class)

package com.klyx.extension.internal

import com.klyx.wasm.ExperimentalWasmApi
import com.klyx.wasm.WasmMemory
import com.klyx.wasm.type.HasWasmReader
import com.klyx.wasm.type.WasmMemoryReader
import com.klyx.wasm.type.WasmString
import com.klyx.wasm.type.WasmType
import com.klyx.wasm.type.collections.WasmList
import com.klyx.wasm.type.collections.toWasmList
import com.klyx.wasm.type.str
import com.klyx.wasm.type.wstr

internal data class GithubRelease(
    val version: WasmString,
    val assets: WasmList<GithubReleaseAsset>
) : WasmType {

    override fun createReader(): WasmMemoryReader<out WasmType> {
        notReadable("GithubRelease")
    }

    override fun writeToBuffer(buffer: ByteArray, offset: Int) {
        var currentOffset = offset
        version.writeToBuffer(buffer, currentOffset)
        currentOffset += version.sizeInBytes()
        assets.writeToBuffer(buffer, currentOffset)
    }

    override fun sizeInBytes(): Int {
        return version.sizeInBytes() + assets.sizeInBytes()
    }

    override fun toString(memory: WasmMemory): String {
        return buildString {
            append("GithubRelease(")
            append("version=${version.toString(memory)}, ")
            append("assets=${assets.toString(memory)}")
            append(")")
        }
    }

    companion object {
        fun from(release: com.klyx.extension.api.github.GithubRelease, memory: WasmMemory): GithubRelease {
            return with(memory) {
                GithubRelease(
                    release.tagName.wstr,
                    release.assets.map { asset ->
                        GithubReleaseAsset(asset.name.wstr, asset.browserDownloadUrl.wstr)
                    }.toWasmList()
                )
            }
        }
    }
}

internal data class GithubReleaseAsset(
    val name: WasmString,
    val downloadUrl: WasmString
) : WasmType {
    override fun createReader() = reader

    override fun writeToBuffer(buffer: ByteArray, offset: Int) {
        var currentOffset = offset
        name.writeToBuffer(buffer, currentOffset)
        currentOffset += name.sizeInBytes()
        downloadUrl.writeToBuffer(buffer, currentOffset)
    }

    override fun sizeInBytes(): Int {
        return name.sizeInBytes() + downloadUrl.sizeInBytes()
    }

    override fun toString(memory: WasmMemory): String {
        return buildString {
            append("GithubReleaseAsset(")
            append("name=${name.toString(memory)}, ")
            append("downloadUrl=${downloadUrl.toString(memory)}")
            append(")")
        }
    }

    companion object : HasWasmReader<GithubReleaseAsset> {
        override val reader
            get() = object : WasmMemoryReader<GithubReleaseAsset> {
                private val strReader = str.reader

                override fun read(
                    memory: WasmMemory,
                    offset: Int
                ): GithubReleaseAsset {
                    var currentOffset = offset
                    val name = strReader.read(memory, currentOffset)
                    currentOffset += strReader.elementSize
                    val downloadUrl = strReader.read(memory, currentOffset)
                    return GithubReleaseAsset(name, downloadUrl)
                }

                override val elementSize: Int get() = strReader.elementSize * 2
            }
    }
}
