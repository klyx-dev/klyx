package com.klyx.editor.treesitter

import android.util.Log
import io.github.treesitter.ktreesitter.Language
import java.util.concurrent.ConcurrentHashMap

class DynamicLanguageProvider : LanguageProvider {

    private val languages = ConcurrentHashMap<String, LanguageEntry>()
    private val queriesCache = ConcurrentHashMap<String, LanguageQueries>()

    fun register(
        name: String,
        language: Language,
        querySources: QuerySources,
        extensions: List<String>,
        fileNames: List<String>,
        priority: LanguagePriority,
        themeOverrides: Map<String, Long> = emptyMap(),
    ): Boolean {
        val normalized = name.lowercase()
        val existing = languages[normalized]
        if (existing != null && !priority.canOverride(existing.priority)) {
            Log.w(
                "DynamicLanguageProvider", "Cannot register language '$name': " +
                        "existing ${existing.priority} registration has higher priority than $priority"
            )
            return false
        }
        languages[normalized] = LanguageEntry(
            name = normalized,
            language = language,
            querySources = querySources,
            priority = priority,
            extensions = extensions.map { it.lowercase() },
            fileNames = fileNames.map { it.lowercase() },
            themeOverrides = themeOverrides,
        )
        queriesCache.remove(normalized)
        return true
    }

    fun unregister(name: String) {
        val normalized = name.lowercase()
        languages.remove(normalized)
        queriesCache.remove(normalized)?.closeSafely()
    }

    fun getEntryForExtension(extension: String): LanguageEntry? {
        val ext = extension.lowercase()
        return languages.values
            .filter { ext in it.extensions }
            .maxByOrNull { it.priority.tier }
    }

    fun getEntryForFileName(fileName: String): LanguageEntry? {
        val name = fileName.lowercase()
        return languages.values
            .filter { name in it.fileNames }
            .maxByOrNull { it.priority.tier }
    }

    override fun getLanguage(languageName: String): Language? {
        return languages[languageName.lowercase()]?.language
    }

    override fun getQueries(languageName: String): LanguageQueries? {
        val name = languageName.lowercase()
        return queriesCache.getOrPut(name) {
            val entry = languages[name] ?: return@getOrPut null
            LanguageQueries.fromSource(
                language = entry.language,
                languageName = entry.name,
                highlights = entry.querySources.highlights,
                indents = entry.querySources.indents,
                folds = entry.querySources.folds,
                locals = entry.querySources.locals,
                injections = entry.querySources.injections,
                tags = entry.querySources.tags,
            )
        }
    }

    fun clear() {
        queriesCache.values.forEach { it.closeSafely() }
        queriesCache.clear()
        languages.clear()
    }
}
