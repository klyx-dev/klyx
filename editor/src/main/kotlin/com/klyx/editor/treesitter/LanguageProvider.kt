package com.klyx.editor.treesitter

import io.github.treesitter.ktreesitter.Language

enum class LanguagePriority(val tier: Int) {
    PLUGIN(0),
    BUILT_IN(1),
    USER(2);

    fun canOverride(other: LanguagePriority): Boolean =
        this.tier > other.tier
}

data class QuerySources(
    val highlights: String,
    val indents: String? = null,
    val folds: String? = null,
    val locals: String? = null,
    val injections: String? = null,
    val tags: String? = null,
)

data class LanguageEntry(
    val name: String,
    val language: Language,
    val querySources: QuerySources,
    val priority: LanguagePriority,
    val extensions: List<String>,
    val fileNames: List<String>,
    val themeOverrides: Map<String, Long> = emptyMap(),
)

interface LanguageProvider {
    fun getLanguage(languageName: String): Language?
    fun getQueries(languageName: String): LanguageQueries?
}

