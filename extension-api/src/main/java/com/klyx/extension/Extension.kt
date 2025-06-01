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
    val toml: ExtensionToml = File(dir, "extension.toml").inputStream().use(Toml.Default::decodeFromStream)

    val wasm = dir.listFiles { file ->
        file.extension == "wasm"
    }?.firstOrNull()?.inputStream() ?: throw ExtensionLoadException("No wasm file found in $dir")

    return Extension(wasm, toml)
}
