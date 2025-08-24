package com.klyx.wasm.todo

import com.github.michaelbull.result.map
import io.github.charlietap.chasm.embedding.module
import kotlinx.io.RawSource
import kotlinx.io.Source
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readByteArray

@OptIn(ExperimentalWasmApi::class)
@WasmDsl
class WasmModuleBuilder {
    fun bytes(wasmBytes: ByteArray) = module(wasmBytes).asResult().map(::WasmModule)
    fun source(wasmSource: RawSource) = bytes(wasmSource.buffered().use(Source::readByteArray))
    fun path(wasmPath: Path) = source(SystemFileSystem.source(wasmPath))
    fun path(wasmPath: String) = path(Path(wasmPath))
}
