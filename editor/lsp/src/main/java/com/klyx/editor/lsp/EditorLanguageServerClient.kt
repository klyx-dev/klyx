package com.klyx.editor.lsp

import android.content.Context
import android.os.Bundle
import com.github.michaelbull.result.getOrElse
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import com.github.michaelbull.result.runCatching
import com.klyx.core.file.KxFile
import com.klyx.core.language
import com.klyx.core.logging.logger
import com.klyx.core.settings.AppSettings
import com.klyx.editor.lsp.completion.LspCompletionItem
import com.klyx.editor.lsp.editor.SignatureHelpWindow
import com.klyx.editor.lsp.util.asLspPosition
import com.klyx.extension.api.Worktree
import io.github.rosemoe.sora.event.ContentChangeEvent
import io.github.rosemoe.sora.event.SelectionChangeEvent
import io.github.rosemoe.sora.lang.Language
import io.github.rosemoe.sora.lang.analysis.AnalyzeManager
import io.github.rosemoe.sora.lang.completion.CompletionPublisher
import io.github.rosemoe.sora.lang.diagnostic.DiagnosticDetail
import io.github.rosemoe.sora.lang.diagnostic.DiagnosticRegion
import io.github.rosemoe.sora.lang.diagnostic.DiagnosticsContainer
import io.github.rosemoe.sora.lang.diagnostic.Quickfix
import io.github.rosemoe.sora.lang.format.Formatter
import io.github.rosemoe.sora.lang.smartEnter.NewlineHandler
import io.github.rosemoe.sora.text.CharPosition
import io.github.rosemoe.sora.text.Content
import io.github.rosemoe.sora.text.ContentReference
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.SymbolPairMatch
import io.github.rosemoe.sora.widget.subscribeAlways
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.eclipse.lsp4j.Diagnostic
import org.eclipse.lsp4j.DiagnosticSeverity
import org.eclipse.lsp4j.Position
import org.eclipse.lsp4j.SignatureHelp
import org.eclipse.lsp4j.TextEdit
import org.eclipse.lsp4j.WorkspaceEdit
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.lang.ref.WeakReference

class EditorLanguageServerClient(
    val worktree: Worktree,
    val file: KxFile,
    val editor: CodeEditor,
    val scope: CoroutineScope,
    private val settings: AppSettings
) : KoinComponent {
    private val applicationContext: Context by inject()
    private var serverClient: LanguageServerClient? = null
    private val capabilities get() = serverClient?.serverCapabilities

    private val completionTriggers get() = capabilities?.completionProvider?.triggerCharacters.orEmpty()
    private val signatureHelpTriggers get() = capabilities?.signatureHelpProvider?.triggerCharacters.orEmpty()
    private val signatureHelpReTriggers get() = capabilities?.signatureHelpProvider?.retriggerCharacters.orEmpty()

    private val signatureHelpWindowWeakReference = WeakReference(SignatureHelpWindow(editor))
    val isShowSignatureHelp: Boolean
        get() = signatureHelpWindowWeakReference.get()?.isShowing ?: false

    fun initialize() {
        scope.launch {
            LanguageServerManager.tryConnectLspIfAvailable(worktree, file.language(), settings).onSuccess { client ->
                serverClient = client
                editor.setEditorLanguage(LspLanguage(editor.editorLanguage, scope))
                LanguageServerManager.openDocument(worktree, file)
                client.onDiagnostics = ::updateDiagnostics
                client.onApplyWorkspaceEdit = {
                    applyWorkspaceEdit(it.edit)
                }

                editor.subscribeAlways<ContentChangeEvent> { event ->
                    scope.launch {
                        LanguageServerManager.changeDocument(worktree, file, event.editor.text.toString())

                        if (hitReTrigger(event.changedText)) {
                            showSignatureHelp(null)
                            return@launch
                        }

                        tryShowSignatureHelp(event.changeStart)
                    }
                }

                editor.subscribeAlways<SelectionChangeEvent> { event ->
                    if (hitReTrigger(event.editor.text[event.left.index].toString())) {
                        showSignatureHelp(null)
                        return@subscribeAlways
                    }

                    scope.launch {
                        tryShowSignatureHelp(event.left)
                    }
                }
            }
        }
    }

    private suspend fun tryShowSignatureHelp(position: CharPosition) {
        LanguageServerManager
            .signatureHelp(worktree, file, position.asLspPosition())
            .onSuccess { signatureHelp ->
                showSignatureHelp(signatureHelp)
            }
            .onFailure {
                println("Error showing signature: $it")
            }
    }

    private fun updateDiagnostics(diagnostics: List<Diagnostic>) {
        val diagnosticsContainer = editor.diagnostics ?: DiagnosticsContainer()
        diagnosticsContainer.reset()

        scope.launch {
            val diagnosticRegions = diagnostics.mapIndexed { idx, diagnosticSource ->
                val quickfixes = fetchQuickfixes(diagnosticSource)
                DiagnosticRegion(
                    diagnosticSource.range.start.getIndex(editor),
                    diagnosticSource.range.end.getIndex(editor),
                    diagnosticSource.severity.toEditorLevel(),
                    idx.toLong(),
                    DiagnosticDetail(
                        diagnosticSource.severity.name,
                        diagnosticSource.message,
                        quickfixes,
                        null
                    )
                )
            }

            diagnosticsContainer.addDiagnostics(diagnosticRegions)

            withContext(Dispatchers.Main) {
                editor.diagnostics = diagnosticsContainer
            }
        }
    }

    fun showSignatureHelp(signatureHelp: SignatureHelp?) {
        val signatureHelpWindow = signatureHelpWindowWeakReference.get() ?: return

        if (signatureHelp == null) {
            editor.post { signatureHelpWindow.dismiss() }
            return
        }

        editor.post { signatureHelpWindow.show(signatureHelp) }
    }

    fun hitReTrigger(eventText: CharSequence): Boolean {
        for (trigger in signatureHelpReTriggers) {
            return eventText in trigger
        }

        return false
    }

    fun hitTrigger(eventText: CharSequence): Boolean {
        for (trigger in signatureHelpTriggers) {
            return eventText in trigger
        }

        return false
    }

    private fun Position.getIndex(editor: CodeEditor): Int {
        val l = if (this.line == editor.lineCount) editor.lineCount - 1 else this.line
        return runCatching {
            editor.text.getCharIndex(
                this.line,
                editor.text.getColumnCount(l).coerceAtMost(this.character)
            )
        }.getOrElse { 0 }
    }

    private fun DiagnosticSeverity.toEditorLevel(): Short {
        return when (this) {
            DiagnosticSeverity.Hint, DiagnosticSeverity.Information -> DiagnosticRegion.SEVERITY_TYPO
            DiagnosticSeverity.Error -> DiagnosticRegion.SEVERITY_ERROR
            DiagnosticSeverity.Warning -> DiagnosticRegion.SEVERITY_WARNING
        }
    }

    private suspend fun fetchQuickfixes(diagnostic: Diagnostic): List<Quickfix> {
        val result = LanguageServerManager
            .requestQuickFixes(worktree, file, diagnostic)
            .getOrElse { return emptyList() }

        return result.mapNotNull { either ->
            when {
                either.isRight -> {
                    val action = either.right
                    Quickfix(action.title ?: "Quickfix") {
                        action.edit?.let { applyWorkspaceEdit(it) }
                    }
                }

                either.isLeft -> {
                    val command = either.left
                    Quickfix(command.title) {
                        scope.launch {
                            serverClient?.executeCommand(command.command, command.arguments)
                        }
                    }
                }

                else -> null
            }
        }
    }

    private fun applyWorkspaceEdit(edit: WorkspaceEdit) {
        println("APPLY: $edit")
        val changes = mutableListOf<TextEdit>()

        edit.changes?.forEach { (_, edits) ->
            changes.addAll(edits)
        }

        edit.documentChanges?.forEach { docChange ->
            if (docChange.isLeft) {
                changes.addAll(docChange.left.edits)
            }
        }

        if (changes.isEmpty()) return

        val sortedEdits = changes.sortedWith(
            compareByDescending<TextEdit> { it.range.start.line }
                .thenByDescending { it.range.start.character }
        )

        scope.launch(Dispatchers.Main) {
            val text = editor.text
            for (te in sortedEdits) {
                val startIdx = te.range.start.getIndex(editor)
                val endIdx = te.range.end.getIndex(editor)

                text.replace(startIdx, endIdx, te.newText)
            }
        }
    }

    fun applyTextEdits(edits: List<TextEdit>, content: Content) {
        runCatching {
            edits.forEach { textEdit ->
                val range = textEdit.range
                val text = textEdit.newText
                var startIndex = content.getCharIndex(range.start.line, range.start.character)
                val endLine = range.end.line.coerceAtMost(content.lineCount - 1)
                var endIndex = content.getCharIndex(endLine, range.end.character)

                if (endIndex < startIndex) {
                    logger().warn { "Invalid edit: start=$startIndex end=$endIndex" }
                    val diff = startIndex - endIndex
                    endIndex = startIndex
                    startIndex = endIndex - diff
                }
                content.replace(startIndex, endIndex, text)
            }
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
            return LspFormatter(this@EditorLanguageServerClient)
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

