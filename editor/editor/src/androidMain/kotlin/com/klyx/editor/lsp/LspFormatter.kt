package com.klyx.editor.lsp

import com.github.michaelbull.result.onSuccess
import com.klyx.editor.lsp.util.asLspRange
import io.github.rosemoe.sora.lang.format.AsyncFormatter
import io.github.rosemoe.sora.text.Content
import io.github.rosemoe.sora.text.TextRange
import kotlinx.coroutines.launch

internal class LspFormatter(private val client: EditorLanguageServerClient) : AsyncFormatter() {
    private val coroutineScope = client.coroutineScope

    override fun formatAsync(
        text: Content,
        cursorRange: TextRange
    ): TextRange? {
        coroutineScope.launch {
            LanguageServerManager
                .formatDocument(client.worktree, client.file)
                .onSuccess { edits ->
                    client.applyTextEdits(edits, text)
                }
        }

        return cursorRange
    }

    override fun formatRegionAsync(
        text: Content,
        rangeToFormat: TextRange,
        cursorRange: TextRange
    ): TextRange? {
        coroutineScope.launch {
            LanguageServerManager
                .formatDocumentRange(client.worktree, client.file, rangeToFormat.asLspRange())
                .onSuccess { edits ->
                    client.applyTextEdits(edits, text)
                }
        }

        return cursorRange
    }
}
