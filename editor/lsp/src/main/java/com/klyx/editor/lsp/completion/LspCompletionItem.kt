package com.klyx.editor.lsp.completion

import com.klyx.core.logging.logger
import io.github.rosemoe.sora.lang.completion.CompletionItemKind
import io.github.rosemoe.sora.lang.completion.SimpleCompletionIconDrawer
import io.github.rosemoe.sora.text.CharPosition
import io.github.rosemoe.sora.text.Content
import io.github.rosemoe.sora.widget.CodeEditor
import org.eclipse.lsp4j.CompletionItem
import org.eclipse.lsp4j.Position
import org.eclipse.lsp4j.Range
import org.eclipse.lsp4j.TextEdit

class LspCompletionItem(
    private val item: CompletionItem,
    prefixLength: Int
) : io.github.rosemoe.sora.lang.completion.CompletionItem(item.label, item.detail) {

    init {
        this.prefixLength = prefixLength
        kind = item.kind?.let { CompletionItemKind.valueOf(it.name) } ?: CompletionItemKind.Text
        sortText = item.sortText
        desc = item.labelDetails?.description
        icon = SimpleCompletionIconDrawer.draw(kind ?: CompletionItemKind.Text)
    }

    override fun performCompletion(editor: CodeEditor, text: Content, position: CharPosition) {
        logger().info { "performCompletion: $item" }
        println("performCompletion: $item")

        val edit = when {
            item.textEdit?.isLeft == true -> item.textEdit.left
            item.textEdit?.isRight == true -> {
                val ire = item.textEdit.right
                TextEdit(ire.insert, ire.newText)
            }

            else -> null
        }

        val insertText = edit?.newText ?: item.insertText ?: item.label

        val range = edit?.range ?: Range(
            Position(position.line, position.column - prefixLength),
            Position(position.line, position.column)
        )

//        text.delete(
//            range.start.line, range.start.character,
//            range.end.line, range.end.character
//        )
        editor.text.replace(
            range.start.line, range.start.character,
            range.end.line, range.end.character, insertText
        )
    }

    override fun performCompletion(editor: CodeEditor, text: Content, line: Int, column: Int) {
        //
    }
}
