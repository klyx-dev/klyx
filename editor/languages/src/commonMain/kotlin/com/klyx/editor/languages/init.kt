package com.klyx.editor.languages

import com.klyx.core.app.App
import com.klyx.core.noderuntime.NodeRuntime
import com.klyx.editor.language.LanguageRegistry

fun initLanguages(languages: LanguageRegistry, node: NodeRuntime, cx: App) {
    val typeScriptLspAdapter = TypeScriptLspAdapter(node)
    val vtslsLspAdapter = VtslsLspAdapter(node)

    languages.registerAvailableLspAdapter("vtsls", vtslsLspAdapter)
    languages.registerAvailableLspAdapter("typescript-language-server", typeScriptLspAdapter)
}
