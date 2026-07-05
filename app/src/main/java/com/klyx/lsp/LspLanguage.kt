package com.klyx.lsp

import android.os.Bundle
import com.klyx.lsp.server.LanguageServer
import com.klyx.lsp.types.fold
import io.github.rosemoe.sora.lang.Language
import io.github.rosemoe.sora.lang.analysis.AnalyzeManager
import io.github.rosemoe.sora.lang.completion.CompletionHelper
import io.github.rosemoe.sora.lang.completion.CompletionPublisher
import io.github.rosemoe.sora.lang.completion.SimpleCompletionItem
import io.github.rosemoe.sora.lang.format.Formatter
import io.github.rosemoe.sora.lang.smartEnter.NewlineHandler
import io.github.rosemoe.sora.text.CharPosition
import io.github.rosemoe.sora.text.ContentReference
import io.github.rosemoe.sora.util.MyCharacter
import io.github.rosemoe.sora.widget.SymbolPairMatch
import kotlinx.coroutines.runBlocking

class LspLanguage(
    private val base: Language,
    private val server: LanguageServer,
    private val uri: String,
) : Language {

    override fun getAnalyzeManager(): AnalyzeManager {
        return base.analyzeManager
    }

    override fun getInterruptionLevel(): Int {
        return base.interruptionLevel
    }

    override fun getIndentAdvance(content: ContentReference, line: Int, column: Int): Int {
        return base.getIndentAdvance(content, line, column)
    }

    override fun useTab(): Boolean {
        return base.useTab()
    }

    override fun getFormatter(): Formatter {
        return base.formatter
    }

    override fun getSymbolPairs(): SymbolPairMatch {
        return base.symbolPairs
    }

    override fun getNewlineHandlers(): Array<NewlineHandler>? {
        return base.newlineHandlers
    }

    override fun requireAutoComplete(
        content: ContentReference,
        position: CharPosition,
        publisher: CompletionPublisher,
        extraArguments: Bundle
    ) {
        val prefix = CompletionHelper.computePrefix(content, position) { MyCharacter.isJavaIdentifierPart(it) }

        runBlocking {
            try {
                // Fetch LSP completions
                val params = CompletionParams(
                    textDocument = TextDocumentIdentifier(uri),
                    position = Position(position.line, position.column)
                )
                val response = server.textDocument.completion(params)
                val lspItems = response?.fold(
                    { it },
                    { it.items }
                ) ?: emptyList()

                val completionItems = lspItems.map { item ->
                    SimpleCompletionItem(
                        item.label,
                        item.detail ?: "",
                        prefix.length,
                        item.insertText ?: item.label
                    )
                }

                publisher.addItems(completionItems)

                // Also call base language for local completions
                try {
                    base.requireAutoComplete(content, position, publisher, extraArguments)
                } catch (_: Exception) {
                    // Ignore base completion errors
                }

                publisher.updateList()
            } catch (_: Exception) {
                // Ignore
            }
        }
    }

    override fun destroy() {
        base.destroy()
    }
}
