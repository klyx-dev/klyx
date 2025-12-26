package com.klyx.editor.lsp.completion

import com.klyx.lsp.CompletionItem
import com.klyx.lsp.InsertTextFormat
import com.klyx.lsp.Position
import com.klyx.lsp.Range
import com.klyx.lsp.TextEdit
import com.klyx.lsp.types.fold
import io.github.rosemoe.sora.lang.completion.CompletionItemKind
import io.github.rosemoe.sora.lang.completion.SimpleCompletionIconDrawer
import io.github.rosemoe.sora.lang.completion.snippet.parser.CodeSnippetParser
import io.github.rosemoe.sora.text.CharPosition
import io.github.rosemoe.sora.text.Content
import io.github.rosemoe.sora.widget.CodeEditor

class LspCompletionItem(
    val item: CompletionItem,
    prefixLength: Int
) : io.github.rosemoe.sora.lang.completion.CompletionItem(item.label, item.detail) {

    init {
        item.textEdit
        this.prefixLength = prefixLength
        kind = item.kind?.let { CompletionItemKind.valueOf(it.name) } ?: CompletionItemKind.Text
        sortText = item.sortText
        //desc = item.labelDetails?.description
        icon = SimpleCompletionIconDrawer.draw(kind ?: CompletionItemKind.Text)
    }

    override fun performCompletion(editor: CodeEditor, text: Content, position: CharPosition) {
        val edit = item.textEdit?.fold(
            { it },
            { TextEdit(it.insert, it.newText) }
        )

        val insertText = edit?.newText ?: item.insertText ?: item.label

        val range = edit?.range ?: Range(
            Position(position.line, position.column - prefixLength),
            Position(position.line, position.column)
        )

        if (item.insertTextFormat == InsertTextFormat.Snippet) {
            val codeSnippet = CodeSnippetParser.parse(insertText)

            text.delete(
                range.start.line.toInt(), range.start.character.toInt(),
                range.end.line.toInt(), range.end.character.toInt()
            )

            editor.snippetController.startSnippet(
                text.getCharIndex(range.start.line.toInt(), range.start.character.toInt()),
                codeSnippet,
                ""
            )
        } else {
            editor.text.replace(
                range.start.line.toInt(), range.start.character.toInt(),
                range.end.line.toInt(), range.end.character.toInt(), insertText
            )
        }
    }

    override fun performCompletion(editor: CodeEditor, text: Content, line: Int, column: Int) {
        //
    }
}
