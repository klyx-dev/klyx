package com.klyx.lsp

import io.github.rosemoe.sora.lang.completion.SimpleCompletionItem
import io.github.rosemoe.sora.text.CharPosition
import io.github.rosemoe.sora.text.Content
import io.github.rosemoe.sora.widget.CodeEditorDelegate

class LspCompletionItem(
    label: CharSequence,
    desc: CharSequence,
    prefixLength: Int,
    commitText: String
) : SimpleCompletionItem(label, desc, prefixLength, commitText) {

    override fun performCompletion(
        editor: CodeEditorDelegate,
        text: Content,
        position: CharPosition
    ) {
        performCompletion(editor, text, position.line, position.column)
    }

    override fun performCompletion(
        editor: CodeEditorDelegate,
        text: Content,
        line: Int,
        column: Int
    ) {
        val commitText = this.commitText ?: return

        val actualPrefixLength = prefixLength.coerceAtMost(column)

        if (actualPrefixLength == 0) {
            text.insert(line, column, commitText)
            return
        }

        text.replace(line, column - actualPrefixLength, line, column, commitText)
    }
}
