package com.klyx.editor.treesitter

import io.github.treesitter.ktreesitter.Node
import io.github.treesitter.ktreesitter.Tree
import java.util.Stack

class TsScopedVariables(
    tree: Tree,
    text: StringBuilder,
    queries: LanguageQueries,
    private val mapper: OffsetMapper,
    cancellationToken: CancellationToken = CancellationToken.NoCancellation
) {

    fun interface CancellationToken {
        companion object {
            val NoCancellation = CancellationToken { false }
        }

        fun isCanceled(): Boolean
    }

    class AnalysisCanceledException : RuntimeException()

    private val rootScope: Scope = Scope(0u, mapper.byteToChar(tree.rootNode.endByte.toInt()).toUInt())

    init {
        if (queries.locals != null) {
            val captures = queries.locals(tree.rootNode)
                .captures()
                .map { (captureIdx, match) -> match.captures[captureIdx.toInt()] }
                .sortedBy { it.node.startByte }
                .toList()

            val scopeStack = Stack<Scope>()
            var lastAddedVariableNode: Node? = null
            scopeStack.push(rootScope)

            for (capture in captures) {
                if (cancellationToken.isCanceled()) break

                val captureName = capture.name
                // Tree-sitter uses UTF-8 byte offsets, while Sora editor uses UTF-16 character indexes.
                // Conversion is required because the analysis logic below operates on character indexes.
                val startIndex = mapper.byteToChar(capture.node.startByte.toInt()).toUInt()
                val endIndex = mapper.byteToChar(capture.node.endByte.toInt()).toUInt()

                while (scopeStack.size > 1 && startIndex >= scopeStack.peek().endIndex) {
                    scopeStack.pop()
                }

                when {
                    captureName in queries.localsScopeNames -> {
                        val newScope = Scope(startIndex, endIndex)
                        scopeStack.peek().childScopes.add(newScope)
                        scopeStack.push(newScope)
                    }

                    captureName in queries.localsMembersScopeNames -> {
                        val newScope = Scope(startIndex, endIndex, forMembers = true)
                        scopeStack.peek().childScopes.add(newScope)
                        scopeStack.push(newScope)
                    }

                    captureName in queries.localsDefinitionNames -> {
                        val scope = scopeStack.peek()
                        val name = text.substring(startIndex.toInt(), endIndex.toInt())
                        val scopedVar = ScopedVariable(
                            name,
                            if (scope.forMembers) scope.startIndex else startIndex,
                            scope.endIndex
                        )
                        scope.variables.add(scopedVar)
                        lastAddedVariableNode = capture.node
                    }

                    captureName !in queries.localsDefinitionValueNames
                            && captureName !in queries.localsReferenceNames
                            && lastAddedVariableNode != null -> {
                        // This capture is co-located with the last definition node
                        // it carries the highlight name for that variable
                        val topVariables = scopeStack.peek().variables
                        if (topVariables.isNotEmpty()) {
                            val topVariable = topVariables.last()
                            val lastStart = mapper.byteToChar(lastAddedVariableNode.startByte.toInt()).toUInt()
                            val lastEnd = mapper.byteToChar(lastAddedVariableNode.endByte.toInt()).toUInt()
                            if (lastStart == startIndex && lastEnd == endIndex
                                && topVariable.highlightCaptureName == null
                            ) {
                                topVariable.highlightCaptureName = captureName
                            }
                        }
                    }
                }
            }

            if (cancellationToken.isCanceled()) throw AnalysisCanceledException()
        }
    }

    data class Scope(
        val startIndex: UInt,
        val endIndex: UInt,
        val forMembers: Boolean = false,
        val variables: MutableList<ScopedVariable> = mutableListOf(),
        val childScopes: MutableList<Scope> = mutableListOf()
    )

    data class ScopedVariable(
        var name: String,
        var scopeStartIndex: UInt,
        var scopeEndIndex: UInt,
        var highlightCaptureName: String? = null
    )

    fun findDefinition(startIndex: UInt, endIndex: UInt, name: String): ScopedVariable? {
        var definition: ScopedVariable? = null
        var currentScope: Scope? = rootScope
        while (currentScope != null) {
            for (variable in currentScope.variables) {
                if (variable.scopeStartIndex > startIndex) break
                if (variable.scopeStartIndex <= startIndex
                    && variable.scopeEndIndex >= endIndex
                    && variable.name == name
                ) {
                    definition = variable
                    // Don't break. allow shadowing
                }
            }
            currentScope = currentScope.childScopes
                .firstOrNull { it.startIndex <= startIndex && it.endIndex >= endIndex }
        }
        return definition
    }
}
