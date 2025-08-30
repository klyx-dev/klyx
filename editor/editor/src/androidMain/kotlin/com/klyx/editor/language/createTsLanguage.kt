package com.klyx.editor.language

import com.itsaky.androidide.treesitter.TSLanguage
import com.itsaky.androidide.treesitter.TSLanguageCache

fun createTsLanguage(name: String, language: Any): TSLanguage = run {
    var cache = TSLanguageCache.get(name)
    if (cache != null) return@run cache
    require(language is Long && language > 0) { "Invalid language: $language" }

    cache = TSLanguage.create(name, language)
    TSLanguageCache.cache(name, cache)
    cache
}
