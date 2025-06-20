package com.klyx.core.extension

import java.io.File

data class Extension(
    val toml: ExtensionToml,
    val path: String,
    val wasmFiles: List<File> = emptyList(),
    val themeFiles: List<File> = emptyList(),
    val isDevExtension: Boolean = false
)

@Throws(Exception::class)
fun parseExtension(dir: File, toml: ExtensionToml): Extension {
    val themes = dir.resolve("themes").listFiles { file ->
        file.extension == "json"
    }?.toList() ?: emptyList()

    val wasmFiles = dir.resolve("lib").listFiles { file ->
        file.extension == "wasm"
    }?.toList() ?: emptyList()

    return Extension(toml, dir.absolutePath, wasmFiles, themes)
}
