package com.klyx.core.extension

import com.klyx.core.file.KxFile
import com.klyx.core.file.resolve

data class Extension(
    val info: ExtensionInfo,
    val path: String,
    val wasmFiles: List<KxFile> = emptyList(),
    val themeFiles: List<KxFile> = emptyList(),
    val isDevExtension: Boolean = false
)

fun parseExtension(dir: KxFile, info: ExtensionInfo): Extension {
    val themes = dir.resolve("themes").listFiles { file ->
        file.extension == "json"
    }?.toList() ?: emptyList()

    val wasmFiles = dir.resolve("src").listFiles { file ->
        file.extension == "wasm"
    }?.toList() ?: dir.resolve("lib").listFiles { file ->
        file.extension == "wasm"
    }?.toList() ?: emptyList()

    return Extension(info, dir.absolutePath, wasmFiles, themes)
}
