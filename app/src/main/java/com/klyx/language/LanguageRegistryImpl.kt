package com.klyx.language

import android.content.Context
import android.util.Log
import com.klyx.api.language.CaptureStyle
import com.klyx.api.language.ColorKey
import com.klyx.api.language.LanguageDescriptor
import com.klyx.api.language.LanguageGrammarProvider
import com.klyx.api.language.LanguageRegistration
import com.klyx.api.language.LanguageRegistry
import com.klyx.api.language.LanguageThemeProvider
import com.klyx.api.language.QueryProvider
import com.klyx.api.plugin.KlyxPlugin
import com.klyx.api.plugin.info
import com.klyx.editor.TreeSitter
import com.klyx.editor.treesitter.EditorLanguage
import com.klyx.editor.treesitter.LanguageQueries
import com.klyx.editor.treesitter.editorTheme
import io.github.rosemoe.sora.lang.styling.textStyle
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme
import java.util.concurrent.ConcurrentHashMap

class LanguageRegistryImpl(
    private val context: Context,
) : LanguageRegistry {

    private data class PendingRegistration(
        val descriptor: LanguageDescriptor,
        val grammarProvider: LanguageGrammarProvider,
        val queries: QueryProvider,
        val theme: LanguageThemeProvider?,
        val pluginId: String,
    )

    private val pending = ConcurrentHashMap<String, PendingRegistration>()
    private val activeExtensions = ConcurrentHashMap<String, String>()
    private val activeFileNames = ConcurrentHashMap<String, String>()
    private val activeDescriptors = ConcurrentHashMap<String, LanguageDescriptor>()
    private val activeEditorLanguages = ConcurrentHashMap<String, EditorLanguage>()

    @Volatile
    private var treeSitter: TreeSitter? = null
    private val boundLanguages = ConcurrentHashMap<String, String>()

    fun bind(treeSitter: TreeSitter) {
        this.treeSitter = treeSitter
        for ((id, reg) in pending) {
            materialize(id, reg, treeSitter)
        }
    }

    fun unbind() {
        treeSitter = null
    }

    context(plugin: KlyxPlugin)
    override fun register(
        descriptor: LanguageDescriptor,
        grammarProvider: LanguageGrammarProvider,
        queries: QueryProvider,
        theme: LanguageThemeProvider?,
    ): LanguageRegistration {
        val id = "${plugin.info.id}:${descriptor.name}"
        val reg = PendingRegistration(descriptor, grammarProvider, queries, theme, plugin.info.id)

        val ts = treeSitter
        if (ts != null) {
            materialize(id, reg, ts)
        } else {
            pending[id] = reg
        }

        return object : LanguageRegistration {
            override fun unregister() {
                removeLanguage(id)
            }
        }
    }

    override fun unregister(id: String) {
        removeLanguage(id)
    }

    override fun getDescriptor(name: String): LanguageDescriptor? = activeDescriptors[name]

    override fun getExtensions(): Map<String, String> = activeExtensions.toMap()

    override fun getFileNames(): Map<String, String> = activeFileNames.toMap()

    private fun materialize(id: String, reg: PendingRegistration, ts: TreeSitter) {
        if (boundLanguages.containsKey(id)) return
        try {
            val pointer = reg.grammarProvider.provide()
            val langName = reg.descriptor.name.lowercase()

            val tsLanguage = io.github.treesitter.ktreesitter.Language(pointer)
            val queries = LanguageQueries.fromSource(
                language = tsLanguage,
                languageName = langName,
                highlights = reg.queries.highlights(),
                indents = reg.queries.indents(),
                folds = reg.queries.folds(),
                locals = reg.queries.locals(),
                injections = reg.queries.injections(),
                tags = reg.queries.tags(),
            )

            val language = EditorLanguage(
                tsLanguage = tsLanguage,
                queries = { queries },
                languageProvider = ts.languageProvider,
                themeDescription = { editorTheme() }
            )

            if (reg.theme != null) {
                applyThemeOverrides(language, reg.theme)
            }

            ts.registerDynamicLanguage(
                name = langName,
                extensions = reg.descriptor.extensions,
                fileNames = reg.descriptor.fileNames,
                editorLanguage = language,
                queries = queries,
            )

            activeEditorLanguages[langName] = language
            activeDescriptors[langName] = reg.descriptor
            reg.descriptor.extensions.forEach { ext ->
                activeExtensions[ext.lowercase()] = langName
            }
            reg.descriptor.fileNames.forEach { fn ->
                activeFileNames[fn.lowercase()] = langName
            }
            boundLanguages[id] = langName

            Log.i("LanguageRegistry", "Registered dynamic language: ${reg.descriptor.name}")
        } catch (e: Exception) {
            Log.e(
                "LanguageRegistry",
                "Failed to register language '${reg.descriptor.name}' from plugin '${reg.pluginId}'",
                e
            )
        }
    }

    private fun removeLanguage(id: String) {
        val langName = boundLanguages.remove(id)
            ?: pending.remove(id)?.descriptor?.name?.lowercase()
            ?: return

        val ts = treeSitter
        ts?.unregisterDynamicLanguage(langName)
        activeEditorLanguages.remove(langName)
        activeExtensions.values.removeAll { it == langName }
        activeFileNames.values.removeAll { it == langName }
        activeDescriptors.remove(langName)
    }

    private fun applyThemeOverrides(language: EditorLanguage, theme: LanguageThemeProvider) {
        val captureNames = language.queries.highlights.captureNames
        val overrides = mutableMapOf<String, Long>()

        for (captureName in captureNames) {
            val style = theme.getStyleForCapture(captureName) ?: continue
            val styleLong = buildStyle(style)
            if (styleLong != 0L) {
                overrides[captureName] = styleLong
            }
        }

        if (overrides.isNotEmpty()) {
            language.applyThemeOverrides(overrides)
        }
    }

    private fun buildStyle(style: CaptureStyle): Long {
        return textStyle(
            foreground = resolveColorKey(style),
            bold = style.bold,
            italic = style.italic,
            strikethrough = style.strikethrough,
        )
    }

    private fun resolveColorKey(style: CaptureStyle): Int {
        return when (style.editorColorKey) {
            ColorKey.KEYWORD -> EditorColorScheme.KEYWORD
            ColorKey.IDENTIFIER_NAME -> EditorColorScheme.IDENTIFIER_NAME
            ColorKey.FUNCTION_NAME -> EditorColorScheme.FUNCTION_NAME
            ColorKey.IDENTIFIER_VAR -> EditorColorScheme.IDENTIFIER_VAR
            ColorKey.LITERAL -> EditorColorScheme.LITERAL
            ColorKey.COMMENT -> EditorColorScheme.COMMENT
            ColorKey.ANNOTATION -> EditorColorScheme.ANNOTATION
            ColorKey.OPERATOR -> EditorColorScheme.OPERATOR
            ColorKey.HTML_TAG -> EditorColorScheme.HTML_TAG
            ColorKey.ATTRIBUTE_NAME -> EditorColorScheme.ATTRIBUTE_NAME
            ColorKey.ATTRIBUTE_VALUE -> EditorColorScheme.ATTRIBUTE_VALUE
            ColorKey.PROBLEM_ERROR -> EditorColorScheme.PROBLEM_ERROR
            ColorKey.PROBLEM_WARNING -> EditorColorScheme.PROBLEM_WARNING
            ColorKey.TEXT_NORMAL -> EditorColorScheme.TEXT_NORMAL
        }
    }
}
