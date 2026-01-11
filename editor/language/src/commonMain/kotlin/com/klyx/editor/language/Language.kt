package com.klyx.editor.language

import com.klyx.editor.language.manifest.ManifestName
import com.klyx.editor.language.toolchain.ToolchainLister

data class Language(
    val id: LanguageId,
    val config: LanguageConfig,
    val toolchain: ToolchainLister?,
    val manifestName: ManifestName?
)
