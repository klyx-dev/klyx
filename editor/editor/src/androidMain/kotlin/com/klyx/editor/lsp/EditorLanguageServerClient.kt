package com.klyx.editor.lsp

import android.content.Context
import android.os.Bundle
import android.util.LruCache
import android.view.View
import androidx.compose.runtime.CompositionContext
import com.github.michaelbull.result.getOrElse
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import com.github.michaelbull.result.runCatching
import com.klyx.core.file.KxFile
import com.klyx.core.language
import com.klyx.core.logging.logger
import com.klyx.core.settings.AppSettings
import com.klyx.editor.KlyxEditor
import com.klyx.editor.lsp.completion.LspCompletionItem
import com.klyx.editor.lsp.util.asLspPosition
import com.klyx.editor.signature.SignatureHelpWindow
import com.klyx.extension.api.Worktree
import io.github.rosemoe.sora.event.ContentChangeEvent
import io.github.rosemoe.sora.event.EditorReleaseEvent
import io.github.rosemoe.sora.event.SelectionChangeEvent
import io.github.rosemoe.sora.lang.Language
import io.github.rosemoe.sora.lang.analysis.AnalyzeManager
import io.github.rosemoe.sora.lang.completion.CompletionPublisher
import io.github.rosemoe.sora.lang.diagnostic.DiagnosticDetail
import io.github.rosemoe.sora.lang.diagnostic.DiagnosticRegion
import io.github.rosemoe.sora.lang.diagnostic.DiagnosticsContainer
import io.github.rosemoe.sora.lang.diagnostic.Quickfix
import io.github.rosemoe.sora.lang.smartEnter.NewlineHandler
import io.github.rosemoe.sora.text.CharPosition
import io.github.rosemoe.sora.text.Content
import io.github.rosemoe.sora.text.ContentReference
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.SymbolPairMatch
import io.github.rosemoe.sora.widget.subscribeAlways
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.eclipse.lsp4j.Diagnostic
import org.eclipse.lsp4j.DiagnosticSeverity
import org.eclipse.lsp4j.Position
import org.eclipse.lsp4j.SignatureHelp
import org.eclipse.lsp4j.TextEdit
import org.eclipse.lsp4j.WorkspaceEdit
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

class EditorLanguageServerClient(
    val worktree: Worktree,
    val file: KxFile,
    val editor: KlyxEditor,
    val scope: CoroutineScope,
    private val settings: AppSettings
) : KoinComponent {
    private val applicationContext: Context by inject()
    private var serverClient: LanguageServerClient? = null
    private val capabilities get() = serverClient?.serverCapabilities

    private val completionTriggersSet by lazy { capabilities?.completionProvider?.triggerCharacters?.toSet().orEmpty() }
    private val signatureHelpTriggersSet by lazy {
        capabilities?.signatureHelpProvider?.triggerCharacters?.toSet().orEmpty()
    }
    private val signatureHelpReTriggersSet by lazy {
        capabilities?.signatureHelpProvider?.retriggerCharacters?.toSet().orEmpty()
    }

    private val prefixCache = LruCache<String, String>(300)
    private val quickfixCache = LruCache<String, List<Quickfix>>(100)

    private val cacheMutex = Mutex()

    private val signatureHelpWindow = AtomicReference<SignatureHelpWindow>()

    private val diagnosticsFlow = MutableSharedFlow<List<Diagnostic>>(replay = 1, extraBufferCapacity = 1)

    private val lastDiagnosticsHash = AtomicInteger(0)

    private var contentChangeJob: Job? = null
    private var signatureHelpJob: Job? = null

    private val textEditComparator = compareByDescending<TextEdit> {
        it.range.start.line
    }.thenByDescending { it.range.start.character }

    private data class CompletionRequest(
        val content: ContentReference,
        val position: CharPosition,
        val publisher: CompletionPublisher,
        val extraArguments: Bundle
    )

    fun initialize(localView: View, compositionContext: CompositionContext) {
        signatureHelpWindow.set(SignatureHelpWindow(editor, localView, compositionContext))
        setupFlows()

        scope.launch(Dispatchers.IO) {
            LanguageServerManager
                .tryConnectLspIfAvailable(worktree, file.language(), settings)
                .onSuccess { client ->
                    serverClient = client
                    withContext(Dispatchers.Main) {
                        editor.setEditorLanguage(LspLanguage(editor.editorLanguage, scope))
                    }
                    LanguageServerManager.openDocument(worktree, file)

                    client.onDiagnostics = diagnosticsFlow::tryEmit

                    client.onApplyWorkspaceEdit = { workspaceEditRequest ->
                        scope.launch(Dispatchers.Default) {
                            applyWorkspaceEdit(workspaceEditRequest.edit)
                        }
                    }

                    setupEventSubscriptions()
                }
        }
    }

    @OptIn(FlowPreview::class)
    private fun setupFlows() {
        diagnosticsFlow
            .distinctUntilChanged { old, new ->
                old.size == new.size && old.hashCode() == new.hashCode()
            }
            .debounce(50)
            .flowOn(Dispatchers.Default)
            .onEach { diagnostics -> updateDiagnostics(diagnostics) }
            .launchIn(scope)
    }

    private fun setupEventSubscriptions() {
        editor.subscribeAlways<ContentChangeEvent> { event ->
            contentChangeJob?.cancel()

            contentChangeJob = scope.launch(Dispatchers.Default) {
                LanguageServerManager.changeDocument(
                    worktree,
                    file,
                    event.editor.text.toString()
                )

                delay(150)

                val text = event.editor.text.getOrNull(event.changeStart.index - 1)?.toString().orEmpty()
                if (hitReTrigger(text)) {
                    showSignatureHelp(null)
                    return@launch
                }

                if (hitTrigger(text)) {
                    tryShowSignatureHelp(event.changeStart)
                }
            }
        }

        editor.subscribeAlways<SelectionChangeEvent> { event ->
            val position = event.left

            val text = event.editor.text.getOrNull(position.index - 1)?.toString()
            println("Text: $text")
            if (text != null && hitReTrigger(text)) {
                showSignatureHelp(null)
                return@subscribeAlways
            }

            if (text != null && hitTrigger(text)) {
                signatureHelpJob?.cancel()
                signatureHelpJob = scope.launch(Dispatchers.Default) {
                    tryShowSignatureHelp(position)
                }
            } else {
                showSignatureHelp(null)
            }
        }

        editor.subscribeAlways<EditorReleaseEvent> { dispose() }
    }

    private suspend fun tryShowSignatureHelp(position: CharPosition) {
        LanguageServerManager
            .signatureHelp(worktree, file, position.asLspPosition())
            .onSuccess { signatureHelp ->
                println(signatureHelp)
                showSignatureHelp(signatureHelp)
            }
            .onFailure {
                logger().debug { "Signature help failed: $it" }
            }
    }

    private suspend fun updateDiagnostics(diagnostics: List<Diagnostic>) {
        val diagnosticsHash = diagnostics.hashCode()
        if (diagnosticsHash == lastDiagnosticsHash.getAndSet(diagnosticsHash)) return

        if (diagnostics.isEmpty()) {
            withContext(Dispatchers.Main) {
                editor.diagnostics?.reset()
            }
            return
        }

        val diagnosticRegions = coroutineScope {
            diagnostics.mapIndexed { idx, diagnostic ->
                async(Dispatchers.Default) {
                    createDiagnosticRegion(idx, diagnostic)
                }
            }.awaitAll()
        }

        val container = DiagnosticsContainer().apply {
            addDiagnostics(diagnosticRegions)
        }

        withContext(Dispatchers.Main) {
            editor.diagnostics = container
        }
    }

    private suspend fun createDiagnosticRegion(idx: Int, diagnostic: Diagnostic): DiagnosticRegion {
        val quickfixes = fetchQuickfixesCached(diagnostic)

        return DiagnosticRegion(
            diagnostic.range.start.calculatePositionIndex(editor),
            diagnostic.range.end.calculatePositionIndex(editor),
            diagnostic.severity.toEditorLevel(),
            idx.toLong(),
            DiagnosticDetail(
                diagnostic.severity.name,
                diagnostic.message,
                quickfixes,
                null
            )
        )
    }

    fun showSignatureHelp(signatureHelp: SignatureHelp?) {
        println(signatureHelp == null)
        println(signatureHelp)

        val helpWindow = signatureHelpWindow.get() ?: return

        if (signatureHelp == null) {
            editor.post { helpWindow.dismiss() }
            return
        }

        editor.post { helpWindow.show(signatureHelp) }
    }

    fun hitReTrigger(eventText: CharSequence): Boolean {
        val triggers = signatureHelpReTriggersSet
        return if (triggers.isEmpty()) false
        else eventText.any { it.toString() in triggers }
    }

    fun hitTrigger(eventText: CharSequence): Boolean {
        val triggers = signatureHelpTriggersSet
        return if (triggers.isEmpty()) false
        else eventText.any { it.toString() in triggers }
    }

    private fun Position.calculatePositionIndex(editor: CodeEditor): Int {
        val safeLine = (this.line).coerceAtMost(editor.lineCount - 1)
        return runCatching {
            val columnCount = editor.text.getColumnCount(safeLine)
            val safeColumn = this.character.coerceAtMost(columnCount)
            editor.text.getCharIndex(safeLine, safeColumn)
        }.getOrElse { 0 }
    }

    private fun DiagnosticSeverity.toEditorLevel(): Short {
        return when (this) {
            DiagnosticSeverity.Hint, DiagnosticSeverity.Information -> DiagnosticRegion.SEVERITY_TYPO
            DiagnosticSeverity.Error -> DiagnosticRegion.SEVERITY_ERROR
            DiagnosticSeverity.Warning -> DiagnosticRegion.SEVERITY_WARNING
        }
    }

    private suspend fun fetchQuickfixesCached(diagnostic: Diagnostic): List<Quickfix> {
        val cacheKey = "${diagnostic.range}:${diagnostic.message.hashCode()}"
        return cacheMutex.withLock {
            quickfixCache.get(cacheKey)
        } ?: run {
            val quickfixes = fetchQuickfixes(diagnostic)
            cacheMutex.withLock {
                quickfixCache.put(cacheKey, quickfixes)
            }
            quickfixes
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
                        action.edit?.let { edit ->
                            scope.launch(Dispatchers.Default) {
                                applyWorkspaceEdit(edit)
                            }
                        }
                    }
                }

                either.isLeft -> {
                    val command = either.left
                    Quickfix(command.title) {
                        scope.launch(Dispatchers.Default) {
                            serverClient?.executeCommand(command.command, command.arguments)
                        }
                    }
                }

                else -> null
            }
        }
    }

    private fun collectAllTextEdits(edit: WorkspaceEdit): List<TextEdit> {
        val estimatedSize = (edit.changes?.values?.sumOf { it.size } ?: 0) +
                (edit.documentChanges?.size ?: 0)

        val changes = ArrayList<TextEdit>(estimatedSize)

        edit.changes?.forEach { (_, edits) ->
            changes.addAll(edits)
        }

        edit.documentChanges?.forEach { docChange ->
            if (docChange.isLeft) {
                changes.addAll(docChange.left.edits)
            }
        }

        return changes
    }

    private suspend fun applyWorkspaceEdit(edit: WorkspaceEdit) {
        val allChanges = collectAllTextEdits(edit)
        if (allChanges.isEmpty()) return

        val sortedEdits = allChanges.sortedWith(textEditComparator)

        withContext(Dispatchers.Main) {
            val text = editor.text

            sortedEdits.forEach { textEdit ->
                val startIdx = textEdit.range.start.calculatePositionIndex(editor)
                val endIdx = textEdit.range.end.calculatePositionIndex(editor)

                if (startIdx <= endIdx && startIdx >= 0 && endIdx <= text.length) {
                    text.replace(startIdx, endIdx, textEdit.newText)
                }
            }
        }
    }

    fun applyTextEdits(edits: List<TextEdit>, content: Content) {
        if (edits.isEmpty()) return

        runCatching {
            val sortedEdits = edits.sortedWith(textEditComparator)

            sortedEdits.forEach { textEdit ->
                val range = textEdit.range
                val text = textEdit.newText

                val startIndex = content.getCharIndex(range.start.line, range.start.character)
                val endLine = range.end.line.coerceAtMost(content.lineCount - 1)
                val endIndex = content.getCharIndex(endLine, range.end.character)

                if (startIndex <= endIndex && startIndex >= 0) {
                    content.replace(startIndex, endIndex, text)
                } else {
                    logger().warn { "Invalid edit range: start=$startIndex end=$endIndex" }
                }
            }
        }.onFailure {
            logger().error(it) { "Failed to apply text edits" }
        }
    }

    private suspend fun processCompletionRequest(request: CompletionRequest) {
        val prefix = calculatePrefixCached(request.content, request.position)
        val prefixLength = prefix.length

        LanguageServerManager
            .completion(worktree, file, request.position.line, request.position.column)
            .onSuccess { completionItems ->
                if (completionItems.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        request.publisher.cancel()
                    }
                    return@onSuccess
                }

                val items = if (completionItems.size > 50) {
                    coroutineScope {
                        completionItems.chunked(25).map { chunk ->
                            async(Dispatchers.Default) {
                                chunk.map { item -> LspCompletionItem(item, prefixLength) }
                            }
                        }.awaitAll().flatten()
                    }
                } else {
                    completionItems.map { item -> LspCompletionItem(item, prefixLength) }
                }

                val sortedItems = items.sortedWith { a, b ->
                    val labelA = a.label.toString()
                    val labelB = b.label.toString()

                    val startsWithA = labelA.startsWith(prefix)
                    val startsWithB = labelB.startsWith(prefix)

                    when {
                        startsWithA && !startsWithB -> -1
                        !startsWithA && startsWithB -> 1
                        else -> {
                            val sortA = (a.sortText ?: labelA).lowercase()
                            val sortB = (b.sortText ?: labelB).lowercase()
                            val cmp = sortA.compareTo(sortB)
                            if (cmp != 0) cmp
                            else {
                                val labelCmp = labelA.compareTo(labelB, ignoreCase = true)
                                if (labelCmp != 0) labelCmp
                                else {
                                    val kindA = a.kind?.ordinal ?: Int.MAX_VALUE
                                    val kindB = b.kind?.ordinal ?: Int.MAX_VALUE
                                    kindA - kindB
                                }
                            }
                        }
                    }
                }

                withContext(Dispatchers.Main) {
                    request.publisher.addItems(sortedItems)
                    request.publisher.updateList()
                }
            }.onFailure {
                logger().debug { "Completion failed: $it" }
                withContext(Dispatchers.Main) {
                    request.publisher.cancel()
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
            val request = CompletionRequest(content, position, publisher, extraArguments)
            runBlocking { processCompletionRequest(request) }
        }

        override fun getIndentAdvance(
            content: ContentReference,
            line: Int,
            column: Int
        ): Int = wrapperLanguage.getIndentAdvance(content, line, column)

        override fun useTab() = wrapperLanguage.useTab()

        override fun getFormatter() = LspFormatter(this@EditorLanguageServerClient)

        override fun getSymbolPairs(): SymbolPairMatch? = wrapperLanguage.symbolPairs

        override fun getNewlineHandlers(): Array<out NewlineHandler?>? = wrapperLanguage.newlineHandlers

        override fun destroy() {
            scope.cancel()
        }
    }

    private suspend fun calculatePrefixCached(content: ContentReference, position: CharPosition): String {
        val cacheKey = "${position.line}:${position.column}:${content.hashCode()}"
        return cacheMutex.withLock {
            prefixCache.get(cacheKey)
        } ?: run {
            val prefix = calculatePrefix(content, position)
            cacheMutex.withLock {
                prefixCache.put(cacheKey, prefix)
            }
            prefix
        }
    }

    private fun calculatePrefix(content: ContentReference, position: CharPosition): String {
        if (position.column == 0) return ""

        val line = content.getLine(position.line)
        val col = (position.column - 1).coerceAtMost(line.length - 1)
        var start = col

        while (start >= 0 && line[start].isJavaIdentifierPart()) {
            start--
        }
        start++

        return if (start <= col) line.substring(start, col + 1) else ""
    }

    fun dispose() {
        contentChangeJob?.cancel()
        signatureHelpJob?.cancel()

        signatureHelpWindow.get()?.dismiss()
        signatureHelpWindow.set(null)

        scope.cancel()

        scope.launch(Dispatchers.Default) {
            cacheMutex.withLock {
                prefixCache.evictAll()
                quickfixCache.evictAll()
            }
        }
    }
}

private suspend inline fun <T, R> List<T>.mapIndexedAsync(
    crossinline transform: suspend (index: Int, T) -> Deferred<R>
) = mapIndexed { index, item -> transform(index, item) }
