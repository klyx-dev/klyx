package com.klyx.api.language

import com.klyx.api.plugin.KlyxPlugin
import com.klyx.api.plugin.PluginService

interface LanguageRegistry : PluginService {

    context(plugin: KlyxPlugin)
    fun register(
        descriptor: LanguageDescriptor,
        grammarProvider: LanguageGrammarProvider,
        queries: QueryProvider,
        theme: LanguageThemeProvider? = null,
    ): LanguageRegistration

    fun unregister(id: String)

    fun getDescriptor(name: String): LanguageDescriptor?

    fun getExtensions(): Map<String, String>

    fun getFileNames(): Map<String, String>
}
