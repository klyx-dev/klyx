package com.klyx.core.extension

import com.klyx.core.file.KxFile
import com.klyx.core.file.resolve

data class Extension(
    val toml: ExtensionToml,
    val path: String,
    val wasmFiles: List<KxFile> = emptyList(),
    val themeFiles: List<KxFile> = emptyList(),
    val isDevExtension: Boolean = false
)

@Throws(Exception::class)
fun parseExtension(dir: KxFile, toml: ExtensionToml): Extension {
    val themes = dir.resolve("themes").listFiles { file ->
        file.extension == "json"
    }?.toList() ?: emptyList()

    val wasmFiles = dir.resolve("lib").listFiles { file ->
        file.extension == "wasm"
    }?.toList() ?: emptyList()

    return Extension(toml, dir.absolutePath, wasmFiles, themes)
}
