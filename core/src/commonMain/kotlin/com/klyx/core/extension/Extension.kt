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
    val themeFiles = dir.resolve("themes")
        .listFiles { it.extension == "json" }
        ?.toList() ?: emptyList()

    val wasmFiles = listOf("src", "lib")
        .flatMap { subDir ->
            dir.resolve(subDir)
                .listFiles { it.extension == "wasm" }
                ?.toList() ?: emptyList()
        }

    return Extension(info, dir.absolutePath, wasmFiles, themeFiles)
}
