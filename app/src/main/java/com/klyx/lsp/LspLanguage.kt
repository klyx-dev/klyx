package com.klyx.lsp

import android.os.Bundle
import com.klyx.lsp.types.fold
import io.github.rosemoe.sora.lang.Language
import io.github.rosemoe.sora.lang.analysis.AnalyzeManager
import io.github.rosemoe.sora.lang.completion.CompletionCancelledException
import io.github.rosemoe.sora.lang.completion.CompletionHelper
import io.github.rosemoe.sora.lang.completion.CompletionPublisher
import io.github.rosemoe.sora.lang.completion.createCompletionItemComparator
import io.github.rosemoe.sora.lang.completion.filterCompletionItems
import io.github.rosemoe.sora.lang.completion.snippet.CodeSnippet
import io.github.rosemoe.sora.lang.completion.snippet.parser.CodeSnippetParser
import io.github.rosemoe.sora.lang.format.Formatter
import io.github.rosemoe.sora.lang.smartEnter.NewlineHandler
import io.github.rosemoe.sora.text.CharPosition
import io.github.rosemoe.sora.text.ContentReference
import io.github.rosemoe.sora.util.MyCharacter
import io.github.rosemoe.sora.widget.SymbolPairMatch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.milliseconds
import io.github.rosemoe.sora.lang.completion.CompletionItem as SoraCompletionItem
import io.github.rosemoe.sora.lang.completion.CompletionItemKind as EditorItemKind

class LspLanguage(
    private val lspManager: LspManager,
    private val base: Language,
    private val tabId: String,
    private val uri: String,
) : Language {

    internal val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    @get:JvmName("formatter")
    private val formatter: Formatter by lazy {
        if (lspManager.getServersSupporting(tabId, LspFeature.Formatting).isNotEmpty()) {
            LspFormatter(lspManager, tabId, uri)
        } else {
            base.formatter
        }
    }

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

    override fun getFormatter(): Formatter = formatter

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
                val servers = lspManager.getServersSupporting(tabId, LspFeature.Completion)
                val params = CompletionParams(
                    textDocument = TextDocumentIdentifier(uri),
                    position = Position(position.line, position.column)
                )

                checkCancelled()

                val lspItems = if (servers.isEmpty()) {
                    emptyList()
                } else {
                    supervisorScope {
                        servers.map { server ->
                            async {
                                withTimeoutOrNull(1500.milliseconds) {
                                    try {
                                        server.textDocument.completion(params)
                                    } catch (_: Exception) {
                                        null
                                    }
                                }
                            }
                        }.awaitAll()
                    }.filterNotNull()
                        .flatMap { response -> response.fold({ it }, { it.items }) }
                        .distinctBy { it.label to it.insertText }
                }

                checkCancelled()

                val prefixLower = prefix.lowercase()
                val completionItems = lspItems.mapNotNull { item -> buildCompletionItem(item, prefix, prefixLower) }

                // Rank by relevance to the typed prefix, with LSP
                // `preselect` items pinned first, then LSP `sortText`, then label. Items that
                // don't fuzzy-match the prefix are dropped by the scorer.
                try {
                    val filtered = filterCompletionItems(content, position, completionItems)
                    publisher.setComparator(lspCompletionComparator(filtered))
                    publisher.addItems(filtered)
                } catch (_: Exception) {
                    publisher.addItems(completionItems)
                }

                checkCancelled()

                try {
                    base.requireAutoComplete(content, position, publisher, extraArguments)
                } catch (_: Exception) {
                }

                checkCancelled()
                publisher.updateList()
            }
        } catch (e: CompletionCancelledException) {
            throw e
        } catch (_: Exception) {
        }
    }

    private fun buildCompletionItem(
        item: CompletionItem,
        prefix: String,
        prefixLower: String
    ): LspCompletionItem? {
        val filterText = item.filterText ?: item.label
        if (prefix.isNotEmpty() && !filterText.contains(prefixLower, ignoreCase = true)) {
            return null
        }

        val snippetFormat = item.insertTextFormat == InsertTextFormat.Snippet

        var newText = item.insertText ?: item.label
        var replaceStartLine: Int? = null
        var replaceStartColumn: Int? = null
        var replaceEndLine: Int? = null
        var replaceEndColumn: Int? = null

        item.textEdit?.fold(
            leftFn = { edit ->
                newText = edit.newText
                replaceStartLine = edit.range.start.line.toInt()
                replaceStartColumn = edit.range.start.character.toInt()
                replaceEndLine = edit.range.end.line.toInt()
                replaceEndColumn = edit.range.end.character.toInt()
            },
            rightFn = { edit ->
                // The client replaces the typed prefix, so the replace range applies.
                newText = edit.newText
                replaceStartLine = edit.replace.start.line.toInt()
                replaceStartColumn = edit.replace.start.character.toInt()
                replaceEndLine = edit.replace.end.line.toInt()
                replaceEndColumn = edit.replace.end.character.toInt()
            }
        )

        val parsedSnippet: CodeSnippet? = if (snippetFormat) {
            try {
                CodeSnippetParser.parse(newText).takeIf { it.checkContent() }
            } catch (_: Exception) {
                null
            }
        } else null

        val commitText = when {
            parsedSnippet != null -> parsedSnippet.toInsertTextForLsp()
            snippetFormat -> stripSnippet(newText)
            else -> newText
        }

        return LspCompletionItem(
            label = item.label,
            desc = item.detail ?: "",
            prefixLength = prefix.length,
            commitText = commitText,
            replaceStartLine = replaceStartLine,
            replaceStartColumn = replaceStartColumn,
            replaceEndLine = replaceEndLine,
            replaceEndColumn = replaceEndColumn,
            snippet = parsedSnippet,
        ).apply {
            this.sortText = item.sortText ?: item.label
            this.filterText = filterText
            this.kind = mapKind(item.kind)
            this.preselect = item.preselect == true
        }
    }

    private fun lspCompletionComparator(
        filtered: List<SoraCompletionItem>
    ): Comparator<SoraCompletionItem> {
        val baseComparator = createCompletionItemComparator(filtered)
        return Comparator { a, b ->
            val aPreselect = (a as? LspCompletionItem)?.preselect == true
            val bPreselect = (b as? LspCompletionItem)?.preselect == true
            when {
                aPreselect && !bPreselect -> -1
                !aPreselect && bPreselect -> 1
                else -> baseComparator.compare(a, b)
            }
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
        scope.cancel()
        base.destroy()
    }
}

private fun stripSnippet(snippet: String): String {
    val out = StringBuilder(snippet.length)
    var i = 0
    while (i < snippet.length) {
        when (val c = snippet[i]) {
            '\\' if i + 1 < snippet.length -> {
                out.append(snippet[i + 1]); i += 2
            }

            '$' if i + 1 < snippet.length -> {
                val next = snippet[i + 1]
                when {
                    next == '$' -> {
                        out.append('$'); i += 2
                    }

                    next == '{' -> {
                        val close = snippet.findMatchingBrace(i + 1)
                        if (close != -1) {
                            val inner = snippet.substring(i + 2, close)
                            val colon = inner.indexOf(':')
                            val pipe = inner.indexOf('|')
                            when {
                                colon != -1 -> out.append(stripSnippet(inner.substring(colon + 1)))
                                pipe != -1 -> out.append(inner.substring(pipe + 1).substringBefore('|'))
                            }
                            i = close + 1
                        } else {
                            out.append(c); i++
                        }
                    }

                    next.isDigit() -> {
                        var j = i + 1
                        while (j < snippet.length && snippet[j].isDigit()) j++
                        i = j
                    }

                    else -> {
                        out.append(c); i++
                    }
                }
            }

            else -> {
                out.append(c); i++
            }
        }
    }
    return out.toString()
}

private fun String.findMatchingBrace(openIndex: Int): Int {
    var depth = 1
    var i = openIndex + 1
    while (i < length) {
        when (this[i]) {
            '{' -> depth++
            '}' -> {
                depth--
                if (depth == 0) return i
            }

            '\\' -> i++
        }
        i++
    }
    return -1
}
