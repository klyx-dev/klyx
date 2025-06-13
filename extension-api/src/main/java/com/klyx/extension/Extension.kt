package com.klyx.extension

import java.io.File
import java.io.InputStream

data class Extension(
    val toml: ExtensionToml,
    val path: String,
    val wasmInput: InputStream? = null,
    val themeInput: InputStream? = null,
    val isDevExtension: Boolean = false
)

@Throws(Exception::class)
fun parseExtension(dir: File, toml: ExtensionToml): Extension {
    val themeInput = if (toml.extension?.type == "theme") {
        File(dir, "themes/themes.json").inputStream()
    } else null

    // Only look for WASM file if the extension type requires it
    val wasmInput = if (toml.extension?.type == "wasm") {
        dir.listFiles { file ->
            file.extension == "wasm"
        }?.firstOrNull()?.inputStream()
    } else null

    return Extension(
        wasmInput = wasmInput,
        themeInput = themeInput,
        toml = toml,
        path = dir.absolutePath
    )
}
