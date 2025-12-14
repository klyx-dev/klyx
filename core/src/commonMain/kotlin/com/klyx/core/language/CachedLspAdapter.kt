package com.klyx.core.language

import com.klyx.core.lsp.LanguageServerBinaryOptions
import com.klyx.core.lsp.LanguageServerName

class CachedLspAdapter private constructor(
    val name: LanguageServerName,
    val diskBasedDiagnosticSources: List<String>,
    val diskBasedDiagnosticsProgressToken: String?,
    private val languageIds: HashMap<LanguageName, String>,
    val adapter: LspAdapter,
    private val cachedBinary: ServerBinaryCache?
) {

    constructor(adapter: LspAdapter) : this(
        adapter.name(),
        adapter.diskBasedDiagnosticSources(),
        adapter.diskBasedDiagnosticsProgressToken(),
        adapter.languageIds(),
        adapter,
        null
    )

    fun languageId(languageName: LanguageName) = languageIds.getOrElse(languageName) { languageName.lspId() }

    override fun toString(): String {
        return "CachedLspAdapter(name='$name', languageIds=$languageIds)"
    }

    suspend fun getLanguageServerCommand(
        delegate: LspAdapterDelegate,
        binaryOptions: LanguageServerBinaryOptions
    ): LanguageServerBinaryLocations {
        return adapter.getLanguageServerCommand(delegate, binaryOptions, cachedBinary)
    }
}
