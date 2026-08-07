package com.klyx.lsp

import io.github.rosemoe.sora.lang.format.AsyncFormatter
import io.github.rosemoe.sora.text.Content
import io.github.rosemoe.sora.text.TextRange
import kotlinx.coroutines.runBlocking

internal class LspFormatter(
    private val manager: LspManager,
    private val tabId: String,
    private val uri: String,
) : AsyncFormatter() {

    override fun formatAsync(text: Content, cursorRange: TextRange): TextRange? {
        apply(text, null)
        return cursorRange
    }

    override fun formatRegionAsync(text: Content, rangeToFormat: TextRange, cursorRange: TextRange): TextRange? {
        apply(text, rangeToFormat)
        return cursorRange
    }

    private fun apply(text: Content, range: TextRange?): Boolean = runBlocking {
        val edits = manager.format(tabId, uri, range) ?: return@runBlocking false
        // Apply backwards so every LSP range still refers to the original document.
        edits.sortedWith(
            compareByDescending<TextEdit> { it.range.start.line }
                .thenByDescending { it.range.start.character }
        ).forEach { edit ->
            text.replace(
                edit.range.start.line.toInt(), edit.range.start.character.toInt(),
                edit.range.end.line.toInt(), edit.range.end.character.toInt(),
                edit.newText
            )
        }
        true
    }
}
