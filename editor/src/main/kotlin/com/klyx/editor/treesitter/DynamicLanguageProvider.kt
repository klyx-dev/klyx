package com.klyx.editor.treesitter

import com.klyx.editor.TSLanguageRegistry
import io.github.treesitter.ktreesitter.Language
import java.util.concurrent.ConcurrentHashMap

class DynamicLanguageProvider(
    private val builtIn: TSLanguageRegistry,
) : LanguageProvider {

    private val dynamicLanguages = ConcurrentHashMap<String, Language>()
    private val dynamicQueries = ConcurrentHashMap<String, LanguageQueries>()

    fun registerLanguage(name: String, language: Language, queries: LanguageQueries) {
        val normalized = name.lowercase()
        dynamicLanguages[normalized] = language
        dynamicQueries[normalized] = queries
    }

    fun unregisterLanguage(name: String) {
        val normalized = name.lowercase()
        dynamicQueries.remove(normalized)?.closeSafely()
        dynamicLanguages.remove(normalized)
    }

    override fun getLanguage(languageName: String): Language? {
        val normalized = languageName.lowercase()
        return dynamicLanguages[normalized] ?: builtIn.getLanguage(normalized)
    }

    override fun getQueries(languageName: String): LanguageQueries? {
        val normalized = languageName.lowercase()
        return dynamicQueries[normalized] ?: builtIn.getQueries(normalized)
    }

    fun clear() {
        builtIn.clear()
        dynamicQueries.values.forEach { it.closeSafely() }
        dynamicQueries.clear()
        dynamicLanguages.clear()
    }
}
