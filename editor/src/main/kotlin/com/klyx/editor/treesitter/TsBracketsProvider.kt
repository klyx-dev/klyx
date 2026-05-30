package com.klyx.editor.treesitter

import io.github.rosemoe.sora.lang.brackets.BracketsProvider
import io.github.rosemoe.sora.lang.brackets.PairedBracket
import io.github.rosemoe.sora.text.Content
import io.github.treesitter.ktreesitter.Node
import io.github.treesitter.ktreesitter.Tree

class TsBracketsProvider(
    private val hostTree: Tree,
    private val mapper: OffsetMapper,
    private val hostProfile: BracketProfile,
    private val injections: List<ActiveInjection>
) : BracketsProvider {

    override fun getPairedBracketAt(text: Content, index: Int): PairedBracket? {
        val length = text.length
        val liveChar = if (index in 0 until length) text[index] else ' '
        val prevChar = if (index > 0 && index - 1 in 0 until length) text[index - 1] else ' '

        if (!isInterestingBracketChar(liveChar) && !isInterestingBracketChar(prevChar)) {
            return null
        }

        // Redirect lookup if the cursor sits inside an active embedded code block
        val injection = injections.firstOrNull { index >= it.startChar && index < it.endChar }
        if (injection != null) {
            return getPairedBracketInternal(
                targetTree = injection.tree,
                globalCharIndex = index,
                profile = injection.profile,
                injectionStartByte = injection.startByte
            )
        }

        return getPairedBracketInternal(hostTree, index, hostProfile, injectionStartByte = 0)
    }

    private fun isInterestingBracketChar(c: Char): Boolean {
        return c == '{' || c == '}' || c == '[' || c == ']' || c == '(' || c == ')' ||
                c == '<' || c == '>' || c == '/' || c == '=' || c == '"' || c == '\''
    }

    private fun getPairedBracketInternal(
        targetTree: Tree,
        globalCharIndex: Int,
        profile: BracketProfile,
        injectionStartByte: Int
    ): PairedBracket? {
        val rootNode = targetTree.rootNode

        val globalByteRight = mapper.charToByte(globalCharIndex)
        val localByteRight = (globalByteRight - injectionStartByte).coerceAtLeast(0).toUInt()
        var leafNode = findLeafNodeAt(rootNode, localByteRight)

        if (leafNode == null || !isInterestingNode(leafNode, profile)) {
            if (globalCharIndex > 0) {
                val globalByteLeft = mapper.charToByte(globalCharIndex - 1)
                val localByteLeft = (globalByteLeft - injectionStartByte).coerceAtLeast(0).toUInt()
                val leftLeaf = findLeafNodeAt(rootNode, localByteLeft)
                if (leftLeaf != null && isInterestingNode(leftLeaf, profile)) {
                    leafNode = leftLeaf
                }
            }
        }

        if (leafNode == null) return null

        val structuralResult = handleStructuralMatch(leafNode, profile, injectionStartByte)
        if (structuralResult != null) return structuralResult

        val type = leafNode.type
        val parent = leafNode.parent ?: return null

        return when {
            profile.tokenPairs.containsKey(type) -> {
                val counterpartType = profile.tokenPairs[type] ?: return null
                findForwardMatch(parent, leafNode, type, counterpartType, injectionStartByte)
            }

            profile.reverseTokenPairs.containsKey(type) -> {
                val counterpartType = profile.reverseTokenPairs[type] ?: return null
                findBackwardMatch(parent, leafNode, type, counterpartType, injectionStartByte)
            }

            else -> null
        }
    }

    private fun handleStructuralMatch(
        leafNode: Node,
        profile: BracketProfile,
        injectionStartByte: Int
    ): PairedBracket? {
        var current: Node? = leafNode
        while (current != null) {
            val type = current.type
            if (type == "start_tag" || type == "end_tag" || type == "self_closing_tag") {
                val openBracket = current.child(0u) ?: return null
                val closeBracket = current.child(current.childCount - 1u) ?: return null
                return createPairedBracket(openBracket, closeBracket, injectionStartByte)
            }
            current = current.parent
        }
        return null
    }

    private fun isInterestingNode(node: Node, profile: BracketProfile): Boolean {
        val type = node.type
        return profile.tokenPairs.containsKey(type) ||
                profile.reverseTokenPairs.containsKey(type) ||
                type == "<" || type == ">" || type == "</" || type == "/>"
    }

    private fun findForwardMatch(
        parent: Node,
        startNode: Node,
        openType: String,
        closeType: String,
        injectionStartByte: Int
    ): PairedBracket? {
        var nestingDepth = 0
        var foundCounterpart: Node? = null
        val totalChildren = parent.childCount

        var selfIndex = -1
        for (i in 0u until totalChildren) {
            if (parent.child(i) == startNode) {
                selfIndex = i.toInt()
                break
            }
        }
        if (selfIndex == -1) return null

        for (i in (selfIndex + 1) until totalChildren.toInt()) {
            val sibling = parent.child(i.toUInt()) ?: continue
            when (sibling.type) {
                openType -> nestingDepth++
                closeType -> {
                    if (nestingDepth == 0) {
                        foundCounterpart = sibling
                        break
                    }
                    nestingDepth--
                }
            }
        }
        return foundCounterpart?.let { closeNode ->
            createPairedBracket(
                startNode,
                closeNode,
                injectionStartByte
            )
        }
    }

    private fun findBackwardMatch(
        parent: Node,
        startNode: Node,
        closeType: String,
        openType: String,
        injectionStartByte: Int
    ): PairedBracket? {
        var nestingDepth = 0
        var foundCounterpart: Node? = null
        val totalChildren = parent.childCount

        var selfIndex = -1
        for (i in 0u until totalChildren) {
            if (parent.child(i) == startNode) {
                selfIndex = i.toInt()
                break
            }
        }
        if (selfIndex == -1) return null

        for (i in (selfIndex - 1) downTo 0) {
            val sibling = parent.child(i.toUInt()) ?: continue
            when (sibling.type) {
                closeType -> nestingDepth++
                openType -> {
                    if (nestingDepth == 0) {
                        foundCounterpart = sibling
                        break
                    }
                    nestingDepth--
                }
            }
        }
        return foundCounterpart?.let { openNode ->
            createPairedBracket(
                openNode,
                startNode,
                injectionStartByte
            )
        }
    }

    private fun findLeafNodeAt(root: Node, byteOffset: UInt): Node? {
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

    private fun createPairedBracket(
        openNode: Node,
        closeNode: Node,
        injectionStartByte: Int
    ): PairedBracket {
        val openStartChar = mapper.byteToChar(injectionStartByte + openNode.startByte.toInt())
        val openLength =
            mapper.byteToChar(injectionStartByte + openNode.endByte.toInt()) - openStartChar

        val closeStartChar = mapper.byteToChar(injectionStartByte + closeNode.startByte.toInt())
        val closeLength =
            mapper.byteToChar(injectionStartByte + closeNode.endByte.toInt()) - closeStartChar

        return PairedBracket(openStartChar, openLength, closeStartChar, closeLength)
    }
}
