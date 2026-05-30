package com.klyx.editor.treesitter

import android.content.Context
import com.klyx.languages.c.TreeSitterC
import com.klyx.languages.comment.TreeSitterComment
import com.klyx.languages.css.TreeSitterCss
import com.klyx.languages.html.TreeSitterHtml
import com.klyx.languages.java.TreeSitterJava
import com.klyx.languages.javascript.TreeSitterJavascript
import com.klyx.languages.python.TreeSitterPython
import io.github.treesitter.ktreesitter.Language
import java.util.concurrent.ConcurrentHashMap

class TSLanguageRegistry(private val context: Context) : LanguageProvider {

    private val languageCache = ConcurrentHashMap<String, Language>()
    private val queriesCache = ConcurrentHashMap<String, LanguageQueries>()

    private val languageSuppliers = mapOf(
        "java" to { TreeSitterJava.language() },
        "python" to { TreeSitterPython.language() },
        "html" to { TreeSitterHtml.language() },
        "c" to { TreeSitterC.language() },
        "css" to { TreeSitterCss.language() },
        "comment" to { TreeSitterComment.language() },
        "javascript" to { TreeSitterJavascript.language() },
    )

    override fun getLanguage(languageName: String): Language? {
        val normalizedName = languageName.lowercase()
        return languageCache.getOrPut(normalizedName) {
            val nativePointer = languageSuppliers[normalizedName]?.invoke() ?: return null
            Language(nativePointer)
        }
    }

    override fun getQueries(languageName: String): LanguageQueries? {
        val normalizedName = languageName.lowercase()
        val lang = getLanguage(normalizedName) ?: return null

        return queriesCache.getOrPut(normalizedName) {
            LanguageQueries(context, lang, normalizedName)
        }
    }

    fun clear() {
        queriesCache.values.forEach { it.closeSafely() }
        queriesCache.clear()
        languageCache.clear()
    }
}
