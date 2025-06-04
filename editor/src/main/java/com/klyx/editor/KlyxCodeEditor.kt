package com.klyx.editor

import android.content.Context
import android.util.AttributeSet
import android.view.KeyEvent
import com.itsaky.androidide.treesitter.TreeSitter
import com.klyx.core.event.EventBus
import com.klyx.core.event.toComposeKeyEvent
import io.github.rosemoe.sora.langs.textmate.TextMateColorScheme
import io.github.rosemoe.sora.langs.textmate.TextMateLanguage
import io.github.rosemoe.sora.langs.textmate.registry.FileProviderRegistry
import io.github.rosemoe.sora.langs.textmate.registry.GrammarRegistry
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry
import io.github.rosemoe.sora.langs.textmate.registry.dsl.LanguageDefinitionListBuilder
import io.github.rosemoe.sora.langs.textmate.registry.dsl.languages
import io.github.rosemoe.sora.langs.textmate.registry.model.ThemeModel
import io.github.rosemoe.sora.langs.textmate.registry.provider.AssetsFileResolver
import io.github.rosemoe.sora.widget.CodeEditor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.eclipse.tm4e.core.registry.IThemeSource

class KlyxCodeEditor @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : CodeEditor(context, attrs, defStyleAttr, defStyleRes) {

    private val scope = CoroutineScope(Dispatchers.Default)

    init {
        applyTextMateTheme()

        isCursorAnimationEnabled = false
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        EventBus.getInstance().postSync(event.toComposeKeyEvent())
        return super.onKeyDown(keyCode, event)
    }

    fun setTheme(name: String) {
        ThemeRegistry.getInstance().setTheme(name)
    }

    private fun applyTextMateTheme() {
        if (colorScheme !is TextMateColorScheme) {
            colorScheme = TextMateColorScheme.create(ThemeRegistry.getInstance())
        }
    }

    fun setLanguage(scope: String?) {
        val l = editorLanguage
        val language = if (l is TextMateLanguage) {
            l.updateLanguage(scope)
            l
        } else {
            TextMateLanguage.create(scope, true)
        }

        setEditorLanguage(language)
    }

    companion object {
        @JvmStatic
        fun setupFileProviders(context: Context) {
            FileProviderRegistry.getInstance().addFileProvider(AssetsFileResolver(context.applicationContext.assets))
        }

        @JvmStatic
        fun loadTSLibrary() {
            TreeSitter.loadLibrary()
        }

        fun loadTextMateTheme(
            path: String,
            name: String = path.substringAfterLast("/").substringBeforeLast("."),
            isDark: Boolean = true
        ) {
            ThemeRegistry.getInstance().loadTheme(
                ThemeModel(
                    IThemeSource.fromInputStream(
                        FileProviderRegistry.getInstance().tryGetInputStream(path), path, null
                    ),
                    name
                ).apply { this.isDark = isDark }
            )
        }

        fun loadTextMateLanguages(path: String) {
            GrammarRegistry.getInstance().loadGrammars(path)
        }

        fun registerLanguageGrammar(block: LanguageDefinitionListBuilder.() -> Unit) {
            GrammarRegistry.getInstance().loadGrammars(languages { block() })
        }

        fun registerDefaultGrammers() {
            registerLanguageGrammar {
                language("txt") {
                    grammar = "textmate/grammers/txt.json"
                    scopeName = "text.plain"
                }
            }
        }

        fun registerDefaultThemes() {
            loadTextMateTheme("textmate/themes/catppuccin-frappe.json", name = "Catppuccin Frappé")
            loadTextMateTheme("textmate/themes/catppuccin-macchiato.json", name = "Catppuccin Macchiato")
            loadTextMateTheme("textmate/themes/one-dark-pro.json", name = "One Dark Pro")
            loadTextMateTheme("textmate/themes/darcula.json", name = "Darcula")
            loadTextMateTheme("textmate/themes/one-light.json", name = "One Light", isDark = false)
        }
    }
}
