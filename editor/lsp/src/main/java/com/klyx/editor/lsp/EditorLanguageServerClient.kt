package com.klyx.editor.lsp

import android.content.Context
import android.os.Bundle
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import com.klyx.core.file.KxFile
import com.klyx.core.language
import com.klyx.core.logging.logger
import com.klyx.editor.lsp.completion.LspCompletionItem
import com.klyx.extension.api.Worktree
import io.github.rosemoe.sora.event.ContentChangeEvent
import io.github.rosemoe.sora.lang.Language
import io.github.rosemoe.sora.lang.analysis.AnalyzeManager
import io.github.rosemoe.sora.lang.completion.CompletionPublisher
import io.github.rosemoe.sora.lang.diagnostic.DiagnosticDetail
import io.github.rosemoe.sora.lang.diagnostic.DiagnosticRegion
import io.github.rosemoe.sora.lang.diagnostic.DiagnosticsContainer
import io.github.rosemoe.sora.lang.format.Formatter
import io.github.rosemoe.sora.lang.smartEnter.NewlineHandler
import io.github.rosemoe.sora.text.CharPosition
import io.github.rosemoe.sora.text.ContentReference
import io.github.rosemoe.sora.util.ArrayList
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.SymbolPairMatch
import io.github.rosemoe.sora.widget.subscribeAlways
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.eclipse.lsp4j.Diagnostic
import org.eclipse.lsp4j.DiagnosticSeverity
import org.eclipse.lsp4j.Position
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class EditorLanguageServerClient(
    private val worktree: Worktree,
    private val file: KxFile,
    private val editor: CodeEditor,
    private val scope: CoroutineScope
) : KoinComponent {
    private val applicationContext: Context by inject()
    private var serverClient: LanguageServerClient? = null
    private val capabilities get() = serverClient?.serverCapabilities

    private val logger = logger()

    fun initialize() {
        scope.launch {
            LanguageServerManager.tryConnectLspIfAvailable(worktree, file.language()).onSuccess { client ->
                serverClient = client
                editor.setEditorLanguage(LspLanguage(editor.editorLanguage, scope))
                LanguageServerManager.openDocument(worktree, file)
                client.onDiagnostics = ::updateDiagnostics

                editor.subscribeAlways<ContentChangeEvent> { event ->
                    scope.launch {
                        LanguageServerManager.changeDocument(worktree, file, event.editor.text.toString())
                    }
                }
            }.onFailure {
                logger.warn { it }
            }
        }
    }

    private fun updateDiagnostics(diagnostics: List<Diagnostic>) {
        val diagnosticsContainer = editor.diagnostics ?: DiagnosticsContainer()
        diagnosticsContainer.reset()

        diagnosticsContainer.addDiagnostics(
            diagnostics.transformToEditorDiagnostics(editor)
        )

        scope.launch(Dispatchers.Main) {
            editor.diagnostics = diagnosticsContainer
        }
    }

    private fun Position.getIndex(editor: CodeEditor): Int {
        return editor.text.getCharIndex(
            this.line,
            editor.text.getColumnCount(this.line).coerceAtMost(this.character)
        )
    }

    private fun List<Diagnostic>.transformToEditorDiagnostics(editor: CodeEditor): List<DiagnosticRegion> {
        val result = ArrayList<DiagnosticRegion>()
        var id = 0L
        for (diagnosticSource in this) {
            val diagnostic = DiagnosticRegion(
                diagnosticSource.range.start.getIndex(editor),
                diagnosticSource.range.end.getIndex(editor),
                diagnosticSource.severity.toEditorLevel(),
                id++,
                DiagnosticDetail(
                    diagnosticSource.severity.name, diagnosticSource.message, null, null
                )
            )
            result.add(diagnostic)
        }
        return result
    }

    private fun DiagnosticSeverity.toEditorLevel(): Short {
        return when (this) {
            DiagnosticSeverity.Hint, DiagnosticSeverity.Information -> DiagnosticRegion.SEVERITY_TYPO
            DiagnosticSeverity.Error -> DiagnosticRegion.SEVERITY_ERROR
            DiagnosticSeverity.Warning -> DiagnosticRegion.SEVERITY_WARNING
        }
    }

    private inner class LspLanguage(
        private val wrapperLanguage: Language,
        private val scope: CoroutineScope
    ) : Language {
        override fun getAnalyzeManager(): AnalyzeManager = wrapperLanguage.analyzeManager
        override fun getInterruptionLevel(): Int = Language.INTERRUPTION_LEVEL_STRONG

        override fun requireAutoComplete(
            content: ContentReference,
            position: CharPosition,
            publisher: CompletionPublisher,
            extraArguments: Bundle
        ) {
            runBlocking {
                val prefix = calculatePrefix(content, position)
                val prefixLength = prefix.length

                LanguageServerManager
                    .completion(worktree, file, position.line, position.column)
                    .onSuccess { completionItems ->
                        val items = completionItems.map { item ->
                            LspCompletionItem(item, prefixLength)
                        }

                        publisher.setComparator { a, b ->
                            val startsWithA = a.label.toString().startsWith(prefix, ignoreCase = true)
                            val startsWithB = b.label.toString().startsWith(prefix, ignoreCase = true)

                            if (startsWithA && !startsWithB) return@setComparator -1
                            if (!startsWithA && startsWithB) return@setComparator 1

                            val sortA = (a.sortText ?: a.label.toString()).lowercase()
                            val sortB = (b.sortText ?: b.label.toString()).lowercase()
                            val cmp = sortA.compareTo(sortB)
                            if (cmp != 0) return@setComparator cmp

                            val labelCmp = a.label.toString().compareTo(b.label.toString(), ignoreCase = true)
                            if (labelCmp != 0) return@setComparator labelCmp

                            val kindA = a.kind?.ordinal ?: Int.MAX_VALUE
                            val kindB = b.kind?.ordinal ?: Int.MAX_VALUE
                            kindA - kindB
                        }
                        publisher.addItems(items)
                    }.onFailure {
                        println(it)
                        publisher.cancel()
                    }

                publisher.updateList()
            }
        }

        override fun getIndentAdvance(
            content: ContentReference,
            line: Int,
            column: Int
        ): Int {
            return wrapperLanguage.getIndentAdvance(content, line, column)
        }

        override fun useTab(): Boolean {
            return wrapperLanguage.useTab()
        }

        override fun getFormatter(): Formatter {
            return wrapperLanguage.formatter
        }

        override fun getSymbolPairs(): SymbolPairMatch? {
            return wrapperLanguage.symbolPairs
        }

        override fun getNewlineHandlers(): Array<out NewlineHandler?>? {
            return wrapperLanguage.newlineHandlers
        }

        override fun destroy() {
            scope.cancel()
        }
    }

    private fun calculatePrefix(content: ContentReference, position: CharPosition): String {
        val sb = StringBuilder()
        var col = position.column - 1

        while (col >= 0) {
            val ch = content.charAt(position.line, col)
            if (!Character.isJavaIdentifierPart(ch)) break
            sb.append(ch)
            col--
        }

        return sb.reverse().toString()
    }
}

