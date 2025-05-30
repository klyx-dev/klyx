/*
 * Copyright 2020 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kwasm

import kwasm.ast.AstNodeList
import kwasm.ast.instruction.Expression
import kwasm.ast.instruction.Instruction
import kwasm.ast.module.Global
import kwasm.ast.module.Local
import kwasm.ast.module.WasmModule
import kwasm.format.ParseContext
import kwasm.format.text.TextModuleCounts
import kwasm.format.text.Tokenizer
import kwasm.format.text.instruction.parseExpression
import kwasm.format.text.instruction.parseInstruction
import kwasm.format.text.instruction.parseInstructions
import kwasm.format.text.module.parseGlobal
import kwasm.format.text.module.parseLocals
import kwasm.format.text.module.parseModule
import kwasm.format.text.token.Token
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * A JUnit [TestRule] which provides parsing functionality without needing to define a [Tokenizer]
 * and [ParseContext].
 */
class ParseRule : TestRule {
    val tokenizer = Tokenizer()
    private val counts = TextModuleCounts(0, 0, 0, 0, 0)
    lateinit var context: ParseContext

    override fun apply(base: Statement, description: Description): Statement {
        return object : Statement() {
            override fun evaluate() {
                context = ParseContext("${description.testClass.simpleName}.wat")
                base.evaluate()
            }
        }
    }

    /**
     * Runs the given [block] within the scope of this [ParseRule], allowing for several helpful
     * extension functions to be used.
     *
     * For example:
     *
     * ```kotlin
     * class MyTest {
     *     @get:Rule
     *     val parser = ParseRule()
     *
     *     @Test
     *     fun testSomething() = parser.with {
     *         val tokens = "(module)".tokenize()
     *         assertThat(tokens[0]).isInstanceOf(Paren.Open::class.java)
     *     }
     * }
     * ```
     */
    fun with(block: ParseRule.() -> Unit) = with(this, block)

    /** Tokenizes the given wasm source. */
    fun String.tokenize(): List<Token> = tokenizer.tokenize(trimIndent(), context)

    /** Parse a [WasmModule] from the given wasm source. */
    fun String.parseModule(): WasmModule =
        requireNotNull(tokenize().parseModule(0)?.astNode) { "No module found in source:\n$this" }

    /** Parse an [Instruction] from the given wasm source. */
    fun String.parseInstruction(): Instruction =
        requireNotNull(tokenize().parseInstruction(0)?.astNode) {
            "No instruction found in source:\n$this"
        }

    /** Parse a [Local] from the given wasm source. */
    fun String.parseLocals(): List<Local> =
        requireNotNull(tokenize().parseLocals(0).astNode) {
            "No locals found in source:\n$this"
        }

    /** Parse a [Global] from the given wasm source. */
    fun String.parseGlobal(): Global =
        requireNotNull(tokenize().parseGlobal(0, counts)?.first?.astNode) {
            "No global found in source:\n$this"
        }

    /** Parse a sequence of [Instruction]s from the given wasm source. */
    fun String.parseInstructions(): AstNodeList<out Instruction> =
        tokenize().parseInstructions(0).astNode

    /** Parse an [Expression] from the given wasm source. */
    fun String.parseExpression(): Expression = tokenize().parseExpression(0).astNode
}
