package com.klyx.lsp

import android.os.Bundle
import com.klyx.lsp.types.fold
import io.github.rosemoe.sora.lang.Language
import io.github.rosemoe.sora.lang.analysis.AnalyzeManager
import io.github.rosemoe.sora.lang.completion.CompletionCancelledException
import io.github.rosemoe.sora.lang.completion.CompletionHelper
import io.github.rosemoe.sora.lang.completion.CompletionPublisher
import io.github.rosemoe.sora.lang.format.Formatter
import io.github.rosemoe.sora.lang.smartEnter.NewlineHandler
import io.github.rosemoe.sora.text.CharPosition
import io.github.rosemoe.sora.text.ContentReference
import io.github.rosemoe.sora.util.MyCharacter
import io.github.rosemoe.sora.widget.SymbolPairMatch
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.runBlocking
import io.github.rosemoe.sora.lang.completion.CompletionItemKind as EditorItemKind

class LspLanguage(
    private val lspManager: LspManager,
    private val base: Language,
    private val tabId: String,
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

    override fun getQuickQuoteHandler(): io.github.rosemoe.sora.lang.QuickQuoteHandler? {
        return base.quickQuoteHandler
    }

    override fun getIndentAdvance(
        content: ContentReference,
        line: Int,
        column: Int,
        spaceCountOnLine: Int,
        tabCountOnLine: Int
    ): Int {
        return base.getIndentAdvance(content, line, column, spaceCountOnLine, tabCountOnLine)
    }

    override fun requireAutoComplete(
        content: ContentReference,
        position: CharPosition,
        publisher: CompletionPublisher,
        extraArguments: Bundle
    ) {
        val prefix = CompletionHelper.computePrefix(content, position) { MyCharacter.isJavaIdentifierPart(it) }

        try {
            runBlocking {
                val server = lspManager.getLanguageServer(tabId) ?: return@runBlocking

                // Fetch LSP completions
                val params = CompletionParams(
                    textDocument = TextDocumentIdentifier(uri),
                    position = Position(position.line, position.column)
                )

                checkCancelled()

                val response = try {
                    server.textDocument.completion(params)
                } catch (_: Exception) {
                    null
                }

                checkCancelled()

                val lspItems = response?.fold(
                    { it },
                    { it.items }
                ) ?: emptyList()

                val prefixLower = prefix.lowercase()
                val completionItems = lspItems.filter { item ->
                    val filterText = (item.filterText ?: item.label).lowercase()
                    if (prefixLower.isNotEmpty()) {
                        filterText.contains(prefixLower)
                    } else {
                        true
                    }
                }.map { item ->
                    LspCompletionItem(
                        item.label,
                        item.detail ?: "",
                        prefix.length,
                        item.insertText ?: item.label
                    ).apply {
                        sortText = item.sortText ?: item.label
                        filterText = item.filterText ?: item.label
                        kind = mapKind(item.kind)
                    }
                }

                checkCancelled()

                publisher.addItems(completionItems)

                // Also call base language for local completions
                try {
                    base.requireAutoComplete(content, position, publisher, extraArguments)
                } catch (_: Exception) {
                    // Ignore base completion errors
                }

                checkCancelled()
                publisher.updateList()
            }
        } catch (e: CompletionCancelledException) {
            throw e
        } catch (_: Exception) {
            // ignore other errors
        }
    }

    private suspend fun checkCancelled() {
        if (CompletionHelper.checkCancelled()) {
            currentCoroutineContext().cancel()
        }
    }

    private fun mapKind(lspKind: CompletionItemKind?): EditorItemKind {
        return when (lspKind) {
            CompletionItemKind.Text -> EditorItemKind.Text
            CompletionItemKind.Method -> EditorItemKind.Method
            CompletionItemKind.Function -> EditorItemKind.Function
            CompletionItemKind.Constructor -> EditorItemKind.Constructor
            CompletionItemKind.Field -> EditorItemKind.Field
            CompletionItemKind.Variable -> EditorItemKind.Variable
            CompletionItemKind.Class -> EditorItemKind.Class
            CompletionItemKind.Interface -> EditorItemKind.Interface
            CompletionItemKind.Module -> EditorItemKind.Module
            CompletionItemKind.Property -> EditorItemKind.Property
            CompletionItemKind.Unit -> EditorItemKind.Unit
            CompletionItemKind.Value -> EditorItemKind.Value
            CompletionItemKind.Enum -> EditorItemKind.Enum
            CompletionItemKind.Keyword -> EditorItemKind.Keyword
            CompletionItemKind.Snippet -> EditorItemKind.Snippet
            CompletionItemKind.Color -> EditorItemKind.Color
            CompletionItemKind.File -> EditorItemKind.File
            CompletionItemKind.Reference -> EditorItemKind.Reference
            CompletionItemKind.Folder -> EditorItemKind.Folder
            CompletionItemKind.EnumMember -> EditorItemKind.EnumMember
            CompletionItemKind.Constant -> EditorItemKind.Constant
            CompletionItemKind.Struct -> EditorItemKind.Struct
            CompletionItemKind.Event -> EditorItemKind.Event
            CompletionItemKind.Operator -> EditorItemKind.Operator
            CompletionItemKind.TypeParameter -> EditorItemKind.TypeParameter
            else -> EditorItemKind.Identifier
        }
    }

    override fun destroy() {
        base.destroy()
    }
}
