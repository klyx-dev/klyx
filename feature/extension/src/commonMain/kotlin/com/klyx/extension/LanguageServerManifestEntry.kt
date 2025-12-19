package com.klyx.extension

import com.klyx.core.language.LanguageName
import com.klyx.lsp.language.CodeActionKind
import kotlinx.serialization.Serializable

@Serializable
data class LanguageServerManifestEntry(
    /**
     * Deprecate in favor of `languages`.
     */
    val language: LanguageName? = null,
    /**
     * The list of languages this language server should work with.
     */
    val languages: List<LanguageName> = emptyList(),
    val languageIds: HashMap<LanguageName, String> = hashMapOf(),
    val codeActionKinds: List<CodeActionKind>? = null
) {

    /** Returns the list of languages for the language server.
     *
     * Prefer this over accessing the `language` or `languages` fields directly,
     * as we currently support both.
     *
     * We can replace this with just field access for the `languages` field once
     * we have removed `language`.
     */
    fun languages(): List<LanguageName> {
        val language = if (languages.isEmpty()) language else null
        return languages + (language?.let { listOf(it) } ?: emptyList())
    }
}
