package com.klyx.language.extension

import arrow.core.raise.result
import com.klyx.core.app.App
import com.klyx.core.logging.logger
import com.klyx.core.lsp.LanguageServerName
import com.klyx.editor.language.BinaryStatus
import com.klyx.editor.language.LanguageName
import com.klyx.editor.language.LanguageRegistry
import com.klyx.extension.Extension
import com.klyx.extension.ExtensionGrammarProxy
import com.klyx.extension.ExtensionLanguageServerProxy
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import okio.Path

class LanguageServerRegistryProxy(
    val languageRegistry: LanguageRegistry
) : ExtensionGrammarProxy, ExtensionLanguageServerProxy {
    override fun registerGrammars(grammars: List<Pair<String, Path>>) {
        // not yet implemented
    }

    override fun registerLanguageServer(
        extension: Extension,
        languageServerId: LanguageServerName,
        language: LanguageName
    ) {
        languageRegistry.registerLspAdapter(
            languageName = language,
            adapter = ExtensionLspAdapter(extension, languageServerId)
        )
    }

    override fun removeLanguageServer(
        language: LanguageName,
        languageServerId: LanguageServerName,
        cx: App
    ): Result<Job> {
        languageRegistry.removeLspAdapter(language, languageServerId)
        // todo: stop running language servers
        return result { CompletableDeferred(Unit) }
    }

    override fun updateLanguageServerStatus(
        languageServerId: LanguageServerName,
        status: BinaryStatus
    ) {
        val logger = logger(languageServerId)

        when (status) {
            BinaryStatus.CheckingForUpdate -> logger.progress { "checking for updates for $languageServerId" }
            BinaryStatus.Downloading -> logger.progress { "downloading language server '$languageServerId'" }
            is BinaryStatus.Failed -> logger.error { "[$languageServerId] failed: ${status.error}" }
            BinaryStatus.None -> logger.info { "" }
            BinaryStatus.Starting -> logger.progress { "starting" }
            BinaryStatus.Stopped -> logger.info { "" }
            BinaryStatus.Stopping -> logger.progress { "stopping" }
        }
    }
}
