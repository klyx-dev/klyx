package com.klyx.extension

import com.klyx.core.RwLock
import com.klyx.core.app.App
import com.klyx.core.app.Global
import com.klyx.core.lsp.LanguageServerName
import com.klyx.editor.language.BinaryStatus
import com.klyx.editor.language.LanguageMatcher
import com.klyx.editor.language.LanguageName
import com.klyx.editor.language.LoadedLanguage
import com.klyx.extension.types.SlashCommand
import com.klyx.util.Ok
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import okio.Path

@JvmInline
private value class GlobalExtensionHostProxy(
    val proxy: ExtensionHostProxy = ExtensionHostProxy(
        RwLock(null),
        RwLock(null),
        RwLock(null),
        RwLock(null),
        RwLock(null),
        RwLock(null),
        RwLock(null),
        RwLock(null)
    )
) : Global

/**
 * A proxy for interacting with the extension host.
 *
 * This object implements each of the individual proxy types so that their
 * methods can be called directly on it.
 * Registration function for language model providers.
 */
typealias LanguageModelProviderRegistration = (App) -> Unit

@MustUseReturnValues
data class ExtensionHostProxy(
    private val themeProxy: RwLock<ExtensionThemeProxy?>,
    private val grammarProxy: RwLock<ExtensionGrammarProxy?>,
    private val languageProxy: RwLock<ExtensionLanguageProxy?>,
    private val languageServerProxy: RwLock<ExtensionLanguageServerProxy?>,
    private val snippetProxy: RwLock<ExtensionSnippetProxy?>,
    private val slashCommandProxy: RwLock<ExtensionSlashCommandProxy?>,
    private val contextServerProxy: RwLock<ExtensionContextServerProxy?>,
    private val languageModelProviderProxy: RwLock<ExtensionLanguageModelProviderProxy?>
) : ExtensionThemeProxy, ExtensionLanguageProxy, ExtensionGrammarProxy, ExtensionLanguageServerProxy,
    ExtensionSnippetProxy, ExtensionSlashCommandProxy, ExtensionContextServerProxy,
    ExtensionLanguageModelProviderProxy {

    companion object {
        /**
         * Returns the global [ExtensionHostProxy].
         */
        fun global(cx: App): ExtensionHostProxy {
            val global: GlobalExtensionHostProxy = cx.global()
            return global.proxy
        }

        /**
         * Returns the global [ExtensionHostProxy].
         *
         * Inserts a default [ExtensionHostProxy] if one does not yet exist.
         */
        fun defaultGlobal(cx: App): ExtensionHostProxy {
            return cx.globalOrDefault { GlobalExtensionHostProxy() }.proxy
        }
    }

    fun registerThemeProxy(proxy: ExtensionThemeProxy) {
        themeProxy.write { proxy }
    }

    fun registerLanguageProxy(proxy: ExtensionLanguageProxy) {
        languageProxy.write { proxy }
    }

    fun registerGrammarProxy(proxy: ExtensionGrammarProxy) {
        grammarProxy.write { proxy }
    }

    fun registerLanguageServerProxy(proxy: ExtensionLanguageServerProxy) {
        languageServerProxy.write { proxy }
    }

    fun registerSnippetProxy(proxy: ExtensionSnippetProxy) {
        snippetProxy.write { proxy }
    }

    fun registerSlashCommandProxy(proxy: ExtensionSlashCommandProxy) {
        slashCommandProxy.write { proxy }
    }

    fun registerContextServerProxy(proxy: ExtensionContextServerProxy) {
        contextServerProxy.write { proxy }
    }

    fun registerLanguageModelProviderProxy(proxy: ExtensionLanguageModelProviderProxy) {
        languageModelProviderProxy.write { proxy }
    }

    override fun setExtensionsLoaded() {
        themeProxy.read()?.setExtensionsLoaded()
    }

    override fun listThemeNames(themePath: Path): Deferred<Result<List<String>>> {
        return themeProxy.read()?.listThemeNames(themePath) ?: CompletableDeferred(Ok(emptyList()))
    }

    override fun removeUserThemes(themes: List<String>) {
        themeProxy.read()?.removeUserThemes(themes)
    }

    override fun loadUserTheme(themePath: Path): Deferred<Result<Unit>> {
        return themeProxy.read()?.loadUserTheme(themePath) ?: CompletableDeferred(Ok(Unit))
    }

    override fun reloadCurrentTheme(cx: App) {
        themeProxy.read()?.reloadCurrentTheme(cx)
    }

    override fun listIconThemeNames(iconThemePath: Path): Deferred<Result<List<String>>> {
        return themeProxy.read()?.listIconThemeNames(iconThemePath) ?: CompletableDeferred(Ok(emptyList()))
    }

    override fun removeIconThemes(iconThemes: List<String>) {
        themeProxy.read()?.removeIconThemes(iconThemes)
    }

    override fun loadIconTheme(
        iconThemePath: Path,
        iconsRootDir: Path
    ): Deferred<Result<Unit>> {
        return themeProxy.read()?.loadIconTheme(iconThemePath, iconsRootDir) ?: CompletableDeferred(Ok(Unit))
    }

    override fun reloadCurrentIconTheme(cx: App) {
        themeProxy.read()?.reloadCurrentIconTheme(cx)
    }

    override fun registerGrammars(grammars: List<Pair<String, Path>>) {
        grammarProxy.read()?.registerGrammars(grammars)
    }

    override fun registerLanguageServer(
        extension: Extension,
        languageServerId: LanguageServerName,
        language: LanguageName
    ) {
        languageServerProxy.read()?.registerLanguageServer(extension, languageServerId, language)
    }

    override fun removeLanguageServer(
        language: LanguageName,
        languageServerId: LanguageServerName,
        cx: App
    ): Result<Job> {
        return languageServerProxy.read()?.removeLanguageServer(language, languageServerId, cx)
            ?: Result.success(Job().also { it.complete() })
    }

    override fun updateLanguageServerStatus(
        languageServerId: LanguageServerName,
        status: BinaryStatus
    ) {
        languageServerProxy.read()?.updateLanguageServerStatus(languageServerId, status)
    }

    override fun registerSnippet(path: Path, snippetContents: String): Result<Unit> {
        return snippetProxy.read()?.registerSnippet(path, snippetContents) ?: Result.success(Unit)
    }

    override fun registerSlashCommand(
        extension: Extension,
        command: SlashCommand
    ) {
        slashCommandProxy.read()?.registerSlashCommand(extension, command)
    }

    override fun unregisterSlashCommand(commandName: String) {
        slashCommandProxy.read()?.unregisterSlashCommand(commandName)
    }

    override fun registerContextServer(
        extension: Extension,
        serverId: String,
        cx: App
    ) {
        contextServerProxy.read()?.registerContextServer(extension, serverId, cx)
    }

    override fun unregisterContextServer(serverId: String, cx: App) {
        contextServerProxy.read()?.unregisterContextServer(serverId, cx)
    }

    override fun registerLanguageModelProvider(
        providerId: String,
        register: LanguageModelProviderRegistration,
        app: App
    ) {
        languageModelProviderProxy.read()?.registerLanguageModelProvider(providerId, register, app)
    }

    override fun unregisterLanguageModelProvider(providerId: String, app: App) {
        languageModelProviderProxy.read()?.unregisterLanguageModelProvider(providerId, app)
    }

    override fun registerLanguage(
        language: LanguageName,
        grammar: String?,
        matcher: LanguageMatcher,
        hidden: Boolean,
        load: () -> Result<LoadedLanguage>
    ) {
        languageProxy.read()?.registerLanguage(language, grammar, matcher, hidden, load)
    }

    override fun removeLanguages(
        languagesToRemove: List<LanguageName>,
        grammarsToRemove: List<String>
    ) {
        languageProxy.read()?.removeLanguages(languagesToRemove, grammarsToRemove)
    }
}

interface ExtensionThemeProxy {
    fun setExtensionsLoaded()

    fun listThemeNames(themePath: Path): Deferred<Result<List<String>>>
    fun removeUserThemes(themes: List<String>)
    fun loadUserTheme(themePath: Path): Deferred<Result<Unit>>
    fun reloadCurrentTheme(cx: App)
    fun listIconThemeNames(iconThemePath: Path): Deferred<Result<List<String>>>
    fun removeIconThemes(iconThemes: List<String>)
    fun loadIconTheme(iconThemePath: Path, iconsRootDir: Path): Deferred<Result<Unit>>
    fun reloadCurrentIconTheme(cx: App)
}

interface ExtensionLanguageProxy {
    fun registerLanguage(
        language: LanguageName,
        grammar: String?,
        matcher: LanguageMatcher,
        hidden: Boolean,
        load: () -> Result<LoadedLanguage>
    )

    fun removeLanguages(languagesToRemove: List<LanguageName>, grammarsToRemove: List<String>)
}

interface ExtensionGrammarProxy {
    fun registerGrammars(grammars: List<Pair<String, Path>>)
}

interface ExtensionLanguageServerProxy {
    fun registerLanguageServer(extension: Extension, languageServerId: LanguageServerName, language: LanguageName)
    fun removeLanguageServer(language: LanguageName, languageServerId: LanguageServerName, cx: App): Result<Job>
    fun updateLanguageServerStatus(languageServerId: LanguageServerName, status: BinaryStatus)
}

interface ExtensionSnippetProxy {
    fun registerSnippet(path: Path, snippetContents: String): Result<Unit>
}

interface ExtensionSlashCommandProxy {
    fun registerSlashCommand(extension: Extension, command: SlashCommand)
    fun unregisterSlashCommand(commandName: String)
}

interface ExtensionContextServerProxy {
    fun registerContextServer(extension: Extension, serverId: String, cx: App)
    fun unregisterContextServer(serverId: String, cx: App)
}

interface ExtensionLanguageModelProviderProxy {
    fun registerLanguageModelProvider(providerId: String, register: LanguageModelProviderRegistration, app: App)
    fun unregisterLanguageModelProvider(providerId: String, app: App)
}
