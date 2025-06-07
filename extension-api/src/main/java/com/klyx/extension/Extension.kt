package com.klyx.extension

import com.akuleshov7.ktoml.Toml
import com.akuleshov7.ktoml.source.decodeFromStream
import java.io.File
import java.io.InputStream

data class Extension(
    val wasmInput: InputStream? = null,
    val themeInput: InputStream? = null,
    val toml: ExtensionToml,
    val path: String
)

@Throws(Exception::class)
fun parseExtension(dir: File): Extension {
    val toml: ExtensionToml = File(dir, "extension.toml").inputStream().use(Toml.Default::decodeFromStream)

    val themeInput = if (toml.extension?.type == "theme") {
        File(dir, "themes/themes.json").inputStream()
    } else null

    // Only look for WASM file if the extension type requires it
    val wasmInput = if (toml.extension?.type == "wasm") {
        dir.listFiles { file ->
            file.extension == "wasm"
        }?.firstOrNull()?.inputStream()
    } else null

    return Extension(wasmInput, themeInput, toml, dir.absolutePath)
}
