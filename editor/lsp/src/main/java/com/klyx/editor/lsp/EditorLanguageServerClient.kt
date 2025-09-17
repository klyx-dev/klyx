package com.klyx.editor.lsp

import android.content.Context
import android.os.Bundle
import android.util.LruCache
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
import kotlinx.coroutines.channels.Channel
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

    private val prefixCache = LruCache<String, String>(100)
    private val positionIndexCache = LruCache<String, Int>(200)
    private val quickfixCache = LruCache<String, List<Quickfix>>(50)

    private val cacheMutex = Mutex()

    private var signatureHelpWindow: SignatureHelpWindow? = null

    private var documentChangeJob: Job? = null
    private var signatureHelpJob: Job? = null
    private val diagnosticsFlow = MutableSharedFlow<List<Diagnostic>>(replay = 1)
    private val completionChannel = Channel<CompletionRequest>(Channel.UNLIMITED)

    private var lastSignaturePosition: CharPosition? = null
    private var lastDiagnosticsHash = AtomicInteger(0)

    private val textEditComparator = compareByDescending<TextEdit> {
        it.range.start.line
    }.thenByDescending { it.range.start.character }

    private data class CompletionRequest(
        val content: ContentReference,
        val position: CharPosition,
        val publisher: CompletionPublisher,
        val extraArguments: Bundle
    )

    fun initialize() {
        signatureHelpWindow = SignatureHelpWindow(editor)
        setupFlows()

        scope.launch {
            LanguageServerManager.tryConnectLspIfAvailable(worktree, file.language(), settings).onSuccess { client ->
                serverClient = client
                editor.setEditorLanguage(LspLanguage(editor.editorLanguage, scope))
                LanguageServerManager.openDocument(worktree, file)

                client.onDiagnostics = { diagnostics ->
                    diagnosticsFlow.tryEmit(diagnostics)
                }

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
                old.hashCode() == new.hashCode()
            }
            .debounce(100)
            .flowOn(Dispatchers.Default)
            .onEach { diagnostics -> updateDiagnosticsOptimized(diagnostics) }
            .launchIn(scope)

        scope.launch {
            for (request in completionChannel) {
                processCompletionRequest(request)
            }
        }
    }

    private fun setupEventSubscriptions() {
        editor.subscribeAlways<ContentChangeEvent> { event ->
            documentChangeJob?.cancel()
            documentChangeJob = scope.launch {
                delay(200)

                LanguageServerManager.changeDocument(
                    worktree,
                    file,
                    event.editor.text.toString()
                )

                val changeText = event.changedText
                if (hitReTrigger(changeText)) {
                    showSignatureHelp(null)
                    return@launch
                }

                tryShowSignatureHelpDebounced(event.changeStart)
            }
        }

        editor.subscribeAlways<SelectionChangeEvent> { event ->
            val position = event.left

            if (position == lastSignaturePosition) return@subscribeAlways
            lastSignaturePosition = position

            if (hitReTrigger(event.editor.text[position.index].toString())) {
                showSignatureHelp(null)
                return@subscribeAlways
            }

            tryShowSignatureHelpDebounced(position)
        }
    }

    private fun tryShowSignatureHelpDebounced(position: CharPosition) {
        signatureHelpJob?.cancel()
        signatureHelpJob = scope.launch {
            delay(150)
            tryShowSignatureHelp(position)
        }
    }

    private suspend fun tryShowSignatureHelp(position: CharPosition) {
        LanguageServerManager
            .signatureHelp(worktree, file, position.asLspPosition())
            .onSuccess { signatureHelp ->
                showSignatureHelp(signatureHelp)
            }
            .onFailure {
                logger().debug { "Signature help failed: $it" }
            }
    }

    private suspend fun updateDiagnosticsOptimized(diagnostics: List<Diagnostic>) {
        val diagnosticsHash = diagnostics.hashCode()
        if (diagnosticsHash == lastDiagnosticsHash.get()) return
        lastDiagnosticsHash.set(diagnosticsHash)

        if (diagnostics.isEmpty()) {
            withContext(Dispatchers.Main) {
                editor.diagnostics?.reset()
            }
            return
        }

        val diagnosticRegions = diagnostics.mapIndexedAsync { idx, diagnostic ->
            coroutineScope {
                async(Dispatchers.Default) {
                    createDiagnosticRegion(idx, diagnostic)
                }
            }
        }.awaitAll()

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
            diagnostic.range.start.getIndexCached(editor),
            diagnostic.range.end.getIndexCached(editor),
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
        val window = signatureHelpWindow ?: return

        if (signatureHelp == null) {
            if (window.isShowing) {
                editor.post { window.dismiss() }
            }
            return
        }

        editor.post { window.show(signatureHelp) }
    }

    fun hitReTrigger(eventText: CharSequence): Boolean {
        return signatureHelpReTriggers.any { trigger ->
            eventText.contains(trigger)
        }
    }

    fun hitTrigger(eventText: CharSequence): Boolean {
        return signatureHelpTriggers.any { trigger ->
            eventText.contains(trigger)
        }
    }

    private suspend fun Position.getIndexCached(editor: CodeEditor): Int {
        val key = "${this.line}:${this.character}"
        return cacheMutex.withLock {
            positionIndexCache.get(key) ?: run {
                val index = calculatePositionIndex(editor)
                positionIndexCache.put(key, index)
                index
            }
        }
    }

    private fun Position.calculatePositionIndex(editor: CodeEditor): Int {
        val safeLine = if (this.line >= editor.lineCount) editor.lineCount - 1 else this.line
        return runCatching {
            val columnCount = editor.text.getColumnCount(safeLine)
            val safeColumn = columnCount.coerceAtMost(this.character)
            editor.text.getCharIndex(this.line, safeColumn)
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
        val cacheKey = "${diagnostic.range}:${diagnostic.message}"
        return cacheMutex.withLock {
            quickfixCache.get(cacheKey) ?: run {
                val quickfixes = fetchQuickfixes(diagnostic)
                quickfixCache.put(cacheKey, quickfixes)
                quickfixes
            }
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
                        action.edit?.let {
                            scope.launch(Dispatchers.Default) {
                                applyWorkspaceEdit(it)
                            }
                        }
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

    private fun collectAllTextEdits(edit: WorkspaceEdit): List<TextEdit> {
        val changes = mutableListOf<TextEdit>()

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
                val startIdx = textEdit.range.start.getIndexCached(editor)
                val endIdx = textEdit.range.end.getIndexCached(editor)

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
                val items = completionItems.map { item ->
                    LspCompletionItem(item, prefixLength)
                }

                val sortedItems = items.sortedWith { a, b ->
                    val startsWithA = a.label.toString().startsWith(prefix, ignoreCase = true)
                    val startsWithB = b.label.toString().startsWith(prefix, ignoreCase = true)

                    when {
                        startsWithA && !startsWithB -> -1
                        !startsWithA && startsWithB -> 1
                        else -> {
                            val sortA = (a.sortText ?: a.label.toString()).lowercase()
                            val sortB = (b.sortText ?: b.label.toString()).lowercase()
                            val cmp = sortA.compareTo(sortB)
                            if (cmp != 0) cmp
                            else {
                                val labelCmp = a.label.toString().compareTo(b.label.toString(), ignoreCase = true)
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
            completionChannel.trySend(request)
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
        val cacheKey = "${position.line}:${position.column}"
        return cacheMutex.withLock {
            prefixCache.get(cacheKey) ?: run {
                val prefix = calculatePrefix(content, position)
                prefixCache.put(cacheKey, prefix)
                prefix
            }
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

    fun dispose() {
        documentChangeJob?.cancel()
        signatureHelpJob?.cancel()
        signatureHelpWindow?.dismiss()
        signatureHelpWindow = null

        completionChannel.cancel()

        scope.cancel()

        runBlocking {
            cacheMutex.withLock {
                prefixCache.evictAll()
                positionIndexCache.evictAll()
                quickfixCache.evictAll()
            }
        }
    }
}

private suspend inline fun <T, R> List<T>.mapIndexedAsync(
    crossinline transform: suspend (index: Int, T) -> Deferred<R>
): List<Deferred<R>> = mapIndexed { index, item -> transform(index, item) }
