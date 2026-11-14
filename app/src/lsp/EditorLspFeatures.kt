package com.klyx.lsp

import com.klyx.lsp.types.fold
import com.klyx.runtime.workspace.Document
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray

internal suspend fun EditorLsp.requestHover(document: Document, line: Int, column: Int): HoverState? {
    val session = sessionForDocument(document)
    val uri = LspDocumentUtils.uriOf(document, workspace)
    if (session != null && session.state.value == LspSessionState.Running) {
        val hover = session.hover(uri, line, column)
        if (hover != null) {
            val text = hover.contents.fold(
                firstFn = { marked ->
                    marked.fold(left = { it }, right = { (language, value) -> "```$language\n$value\n```" })
                },
                secondFn = { list ->
                    list.joinToString("\n\n") { marked ->
                        marked.fold(left = { it }, right = { (language, value) -> "```$language\n$value\n```" })
                    }
                },
                thirdFn = { it.value }
            )
            if (text.isNotBlank()) {
                val state = HoverState(line, column, text.trim())
                _activeHover.value = state
                return state
            }
        }
    }

    // Smart fallback hover without LSP
    val controller = editorStore.controllersFor(document.id).firstOrNull() ?: return null
    val docText = withContext(Dispatchers.Main) { controller.getDocument().text }
    val lines = docText.lines()
    val currentLine = lines.getOrNull(line) ?: ""
    val word = SmartLanguageFeatures.getWordAt(currentLine, column)
    if (word.length >= 2 && (word[0].isLetter() || word[0] == '_')) {
        val count = Regex("""\b${Regex.escape(word)}\b""").findAll(docText).count()
        val declRegex =
            Regex("""(?m)^[ \t]*(?:fun|def|fn|val|var|let|const|class|struct|interface|enum)\s+${Regex.escape(word)}\b.*$""")
        val declMatch = declRegex.find(docText)?.value?.trim()
        val langId = LspDocumentUtils.languageIdOf(document)
        val info = buildString {
            append("**`$word`** ($count occurrences in file)")
            if (declMatch != null) {
                append("\n\n```$langId\n$declMatch\n```")
            }
        }
        val state = HoverState(line, column, info)
        _activeHover.value = state
        return state
    }
    return null
}

internal suspend fun EditorLsp.getCodeActions(
    document: Document,
    range: Range,
    diagnostics: List<Diagnostic> = emptyList(),
): List<CodeAction> {
    val session = sessionForDocument(document) ?: return emptyList()
    if (session.state.value != LspSessionState.Running) return emptyList()
    val uri = LspDocumentUtils.uriOf(document, workspace)
    return session.codeAction(uri, range, diagnostics) ?: emptyList()
}

internal suspend fun EditorLsp.applyCodeAction(action: CodeAction) {
    action.edit?.let { edit ->
        client.applyWorkspaceEdit(edit)
    }
    action.command?.let { (val command, val arguments) ->
        val langId = editorStore.activeDocumentId.value?.let { workspace.document(it) }
            ?.let { LspDocumentUtils.languageIdOf(it) }
        val session = langId?.let { sessionFor(it) }
        val args = (arguments as? JsonArray)?.toList()
        val _ = session?.executeCommand(command, args)
    }
    editorStore.activeDocumentId.value?.let { docId ->
        documents.saveDocument(docId)
    }
}

