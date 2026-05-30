package com.klyx.editor.treesitter

import android.os.Bundle
import io.github.rosemoe.sora.lang.EmptyLanguage
import io.github.rosemoe.sora.lang.completion.CompletionPublisher
import io.github.rosemoe.sora.lang.smartEnter.NewlineHandler
import io.github.rosemoe.sora.text.CharPosition
import io.github.rosemoe.sora.text.ContentReference
import io.github.treesitter.ktreesitter.Language
import io.github.treesitter.ktreesitter.Node

class EditorLanguage(
    private val tsLanguage: Language,
    queries: (Language) -> LanguageQueries,
    private val languageProvider: LanguageProvider,
    themeDescription: TsThemeBuilder.() -> Unit
) : io.github.rosemoe.sora.lang.Language {

    private val queries by lazy { queries(tsLanguage) }
    private val theme by lazy {
        TsThemeBuilder(this.queries.highlights).apply { themeDescription() }.theme
    }

    private val analyzer by lazy {
        TsAnalyzeManager(tsLanguage, this.queries, this.theme, languageProvider)
    }

    override fun getAnalyzeManager() = analyzer
    override fun getInterruptionLevel() = 0

    override fun requireAutoComplete(
        content: ContentReference,
        position: CharPosition,
        publisher: CompletionPublisher,
        extraArguments: Bundle
    ) {
    }

    override fun useTab() = false
    override fun getFormatter() = EmptyLanguage.EmptyFormatter.INSTANCE!!
    override fun getSymbolPairs() = EmptyLanguage.EMPTY_SYMBOL_PAIRS!!
    override fun getNewlineHandlers() = emptyArray<NewlineHandler>()

    override fun getIndentAdvance(content: ContentReference, line: Int, column: Int): Int {
        val indentQuery = queries.indents ?: return 0

        val currentLineText = content.getLine(line).toString()
        if (currentLineText.isBlank()) {
            return 0
        }

        var absoluteCharIndex = 0
        for (i in 0 until line) {
            absoluteCharIndex += content.getLine(i).length + 1
        }
        absoluteCharIndex += column

        val targetByte = analyzer.charToByte(absoluteCharIndex)
        if (targetByte <= 0) return 0

        val activeTreeRef = analyzer.activeTree ?: return 0
        var leafNode = findLeafNodeAt(activeTreeRef.rootNode, targetByte.toUInt())

        if (column == 0 && absoluteCharIndex > 0) {
            val leftLeaf = findLeafNodeAt(activeTreeRef.rootNode, (targetByte - 1).toUInt())
            leafNode = leftLeaf
        }

        var ancestor: Node? = leafNode
        try {
            val cursor = indentQuery(activeTreeRef.rootNode)
            val matches = cursor.matches()

            while (ancestor != null) {
                val nodeRow = ancestor.startPoint.row.toInt()

                for (match in matches) {
                    for (capture in match.captures) {
                        if (capture.node == ancestor) {
                            when (capture.name) {
                                "indent.begin" -> {
                                    // Only advance indentation if the block opened on this line
                                    if (nodeRow == line) {
                                        return 4
                                    }
                                }

                                "indent.end", "indent.dedent", "indent.branch" -> {
                                    if (nodeRow == line) {
                                        return -4
                                    }
                                }
                            }
                        }
                    }
                }
                ancestor = ancestor.parent
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return 0
    }

    private fun findLeafNodeAt(root: Node, byteOffset: UInt): Node {
        var current: Node = root
        while (current.childCount > 0u) {
            var shiftedDown = false
            for (i in 0u until current.childCount) {
                val child = current.child(i) ?: continue
                if (child.startByte <= byteOffset && child.endByte > byteOffset) {
                    current = child
                    shiftedDown = true
                    break
                }
            }
            if (!shiftedDown) break
        }
        return current
    }

    override fun destroy() {
        queries.closeSafely()
    }
}
