package com.klyx.extension

import com.akuleshov7.ktoml.Toml
import com.akuleshov7.ktoml.source.decodeFromStream
import java.io.File
import java.io.InputStream

data class Extension(
    val input: InputStream,
    val toml: ExtensionToml
)

@Throws(Exception::class)
fun parseExtension(dir: File): Extension {
    val toml = File(dir, "extension.toml").inputStream().use {
        Toml.decodeFromStream<ExtensionToml>(it)
    }
    val wasm = File(dir, "extension.wasm").inputStream()
    return Extension(wasm, toml)
}
