package com.klyx.core.languages

import com.klyx.core.language.LanguageRegistry
import com.klyx.core.noderuntime.NodeRuntime

object Languages {
    fun init(languages: LanguageRegistry, node: NodeRuntime) {
        val typeScriptLspAdapter = TypeScriptLspAdapter(node)
        val vtslsLspAdapter = VtslsLspAdapter(node)

        languages.registerAvailableLspAdapter("vtsls", vtslsLspAdapter)
        languages.registerAvailableLspAdapter("typescript-language-server", typeScriptLspAdapter)
    }
}
