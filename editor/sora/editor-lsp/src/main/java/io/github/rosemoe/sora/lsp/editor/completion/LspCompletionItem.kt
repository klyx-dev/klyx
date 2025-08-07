/*******************************************************************************
 *    sora-editor - the awesome code editor for Android
 *    https://github.com/Rosemoe/sora-editor
 *    Copyright (C) 2020-2023  Rosemoe
 *
 *     This library is free software; you can redistribute it and/or
 *     modify it under the terms of the GNU Lesser General Public
 *     License as published by the Free Software Foundation; either
 *     version 2.1 of the License, or (at your option) any later version.
 *
 *     This library is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *     Lesser General Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser General Public
 *     License along with this library; if not, write to the Free Software
 *     Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
 *     USA
 *
 *     Please contact Rosemoe by email 2073412493@qq.com if you need
 *     additional information or have any questions
 ******************************************************************************/

package io.github.rosemoe.sora.lsp.editor.completion

import android.util.Log
import io.github.rosemoe.sora.lang.completion.CompletionItemKind
import io.github.rosemoe.sora.lang.completion.SimpleCompletionIconDrawer.draw
import io.github.rosemoe.sora.lang.completion.snippet.parser.CodeSnippetParser
import io.github.rosemoe.sora.lsp.editor.LspEventManager
import io.github.rosemoe.sora.lsp.events.EventType
import io.github.rosemoe.sora.lsp.events.document.applyEdits
import io.github.rosemoe.sora.lsp.utils.asLspPosition
import io.github.rosemoe.sora.lsp.utils.createPosition
import io.github.rosemoe.sora.lsp.utils.createRange
import io.github.rosemoe.sora.text.CharPosition
import io.github.rosemoe.sora.text.Content
import io.github.rosemoe.sora.util.Logger
import io.github.rosemoe.sora.widget.CodeEditor
import org.eclipse.lsp4j.CompletionItem
import org.eclipse.lsp4j.InsertTextFormat
import org.eclipse.lsp4j.TextEdit


class LspCompletionItem(
    private val completionItem: CompletionItem,
    private val eventManager: LspEventManager,
    prefixLength: Int
) : io.github.rosemoe.sora.lang.completion.CompletionItem(
    completionItem.label,
    completionItem.detail
) {

    init {
        this.prefixLength = prefixLength
        kind =
            if (completionItem.kind == null) CompletionItemKind.Text else CompletionItemKind.valueOf(
                completionItem.kind.name
            )
        sortText = completionItem.sortText
        val labelDetails = completionItem.labelDetails
        if (labelDetails != null && labelDetails.description != null) {
            desc = labelDetails.description
        }
        icon = draw(kind ?: CompletionItemKind.Text)
    }

    override fun performCompletion(editor: CodeEditor, text: Content, position: CharPosition) {
        var textEdit = TextEdit()

        textEdit.range = createRange(
            createPosition(
                position.line,
                position.column - prefixLength
            ), position.asLspPosition()
        )

        if (completionItem.insertText != null) {
            textEdit.newText = completionItem.insertText
        }

        if (completionItem.textEdit != null && completionItem.textEdit.isLeft) {
            //TODO: support InsertReplaceEdit
            textEdit = completionItem.textEdit.left
        }

        if (textEdit.newText == null && completionItem.label != null) {
            textEdit.newText = completionItem.label
        }

        run {
            // workaround https://github.com/Microsoft/vscode/issues/17036
            val start = textEdit.range.start
            val end = textEdit.range.end
            if (start.line > end.line || start.line == end.line && start.character > end.character) {
                textEdit.range.end = start
                textEdit.range.start = end
            }
        }

        run {
            // allow completion items to be wrong with a too wide range
            val documentEnd = createPosition(
                text.lineCount - 1,
                text.getColumnCount(0.coerceAtLeast(text.lineCount - 1)) - 1
            )
            val textEditEnd = textEdit.range.end
            if (documentEnd.line < textEditEnd.line || documentEnd.line == textEditEnd.line && documentEnd.character < textEditEnd.character
            ) {
                textEdit.range.end = documentEnd
            }
        }

        val finalTextEdit = textEdit

        if (completionItem.insertTextFormat == InsertTextFormat.Snippet) {
            val codeSnippet = CodeSnippetParser.parse(textEdit.newText)
            var startIndex =
                text.getCharIndex(
                    textEdit.range.start.line, textEdit.range.start.character)

            var endIndex = text.getCharIndex(
                textEdit.range.end.line,
                (text.getColumnCount(textEdit.range.end.line) - 1)
                    .coerceAtMost(textEdit.range.end.character)
            )

            val selectedText = text.subSequence(startIndex, endIndex).toString()

            if (endIndex < startIndex) {
                Logger.instance(this.javaClass.name)
                    .w(
                        "Invalid location information found applying edits from %s to %s",
                        textEdit.range.start,
                        textEdit.range.end
                    )
                val diff = startIndex - endIndex
                endIndex = startIndex
                startIndex = endIndex - diff
            }

            text.delete(startIndex, endIndex)

            editor.snippetController
                .startSnippet(startIndex, codeSnippet, selectedText)
        } else {
            eventManager.emit(EventType.applyEdits) {
                put("edits", listOf(finalTextEdit))
                put(text)
            }
        }

        if (completionItem.additionalTextEdits != null) {
            eventManager.emit(EventType.applyEdits) {
                put("edits", completionItem.additionalTextEdits)
                put(text)
            }
        }
    }

    override fun performCompletion(editor: CodeEditor, text: Content, line: Int, column: Int) {
        // do nothing
    }
}


