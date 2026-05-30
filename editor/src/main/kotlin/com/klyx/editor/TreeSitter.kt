package com.klyx.editor

import android.content.Context
import com.klyx.editor.treesitter.TSLanguageRegistry
import com.klyx.editor.treesitter.createEditorLanguage
import com.klyx.languages.c.TreeSitterC
import com.klyx.languages.css.TreeSitterCss
import com.klyx.languages.html.TreeSitterHtml
import com.klyx.languages.java.TreeSitterJava
import com.klyx.languages.javascript.TreeSitterJavascript
import com.klyx.languages.python.TreeSitterPython
import io.github.rosemoe.sora.lang.Language

class TreeSitter(private val context: Context) : AutoCloseable {
    val languageProvider = TSLanguageRegistry(context)

    init {

    }

    fun java() = createEditorLanguage("java", TreeSitterJava.language())
    fun python() = createEditorLanguage("python", TreeSitterPython.language())
    fun html() = createEditorLanguage("html", TreeSitterHtml.language())
    fun javascript() = createEditorLanguage("javascript", TreeSitterJavascript.language())
    fun jsx() = createEditorLanguage("jsx", TreeSitterJavascript.language())
    fun c() = createEditorLanguage("c", TreeSitterC.language())
    fun css() = createEditorLanguage("css", TreeSitterCss.language())

    private fun createEditorLanguage(name: String, language: Any): Language =
        createEditorLanguage(context, name, language, languageProvider)

    override fun close() {
        languageProvider.clear()
    }
}
