package com.klyx.editor

import com.klyx.languages.java.TreeSitterJava
import io.github.treesitter.ktreesitter.Language
import io.github.treesitter.ktreesitter.Node
import io.github.treesitter.ktreesitter.Parser
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.runner.junit4.KotestTestRunner
import org.junit.runner.RunWith

@RunWith(KotestTestRunner::class)
class TmpTest : FunSpec({
    fun printTree(node: Node, depth: Int = 0) {
        println(
            "  ".repeat(depth) +
                    "${node.type} " +
                    "[${node.startPoint.row}:${node.startPoint.column} - " +
                    "${node.endPoint.row}:${node.endPoint.column}]"
        )

        for (i in 0u until node.childCount) {
            node.child(i)?.let {
                printTree(it, depth + 1)
            }
        }
    }

    test("test ts") {
        val parser = Parser(Language(TreeSitterJava.language()))
        val source = """
            class Main {
                public static void main(String[] args) {
                    System.out.println("Hello, World!");
                }
            }
        """.trimIndent()
        val tree = parser.parse(source)
        val rootNode = tree.rootNode
        parser.language
        printTree(rootNode)
        rootNode.type shouldBe "program"
    }
})
