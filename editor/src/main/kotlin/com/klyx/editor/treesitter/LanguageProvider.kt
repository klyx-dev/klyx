package com.klyx.editor.treesitter

import io.github.treesitter.ktreesitter.Language

interface LanguageProvider {
    fun getLanguage(languageName: String): Language?
    fun getQueries(languageName: String): LanguageQueries?
}

