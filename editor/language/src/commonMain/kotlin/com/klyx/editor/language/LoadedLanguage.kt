package com.klyx.editor.language

import com.klyx.editor.language.manifest.ManifestName
import com.klyx.editor.language.toolchain.ToolchainLister

data class LoadedLanguage(
    val config: LanguageConfig,
    val queries: LanguageQueries,
    val toolchainProvider: ToolchainLister?,
    val manifestName: ManifestName?
)
