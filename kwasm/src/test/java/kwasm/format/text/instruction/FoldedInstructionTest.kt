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

package kwasm.format.text.instruction

import com.google.common.truth.Truth.assertThat
import kwasm.ast.Identifier
import kwasm.ast.instruction.ControlInstruction
import kwasm.ast.module.Index
import kwasm.ast.type.Result
import kwasm.ast.type.ResultType
import kwasm.ast.type.ValueType
import kwasm.format.ParseContext
import kwasm.format.ParseException
import kwasm.format.text.Tokenizer
import org.assertj.core.api.Assertions.fail
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class FoldedInstructionTest {
    private val tokenizer = Tokenizer()
    private val context = ParseContext("FoldedInstructionsTest.wat")

    @Test
    fun parse_returnsNull_ifDoesntStartWithOpenParen() {
        assertThat(tokenizer.tokenize("return", context).parseFoldedInstruction(0))
            .isNull()
    }

    @Test
    fun parse_throws_ifKeywordAfterOpen_isNotAKeyword() {
        val e = assertThrows(ParseException::class.java) {
            tokenizer.tokenize("($0)").parseFoldedInstruction(0)
        }
        assertThat(e).hasMessageThat()
            .contains("Expected `if`, `block`, `loop`, or a \"plain\" instruction")
    }

    @Test
    fun parse_throws_ifKeywordAfterOpen_isNotExpected() {
        val e = assertThrows(ParseException::class.java) {
            tokenizer.tokenize("(something_invalid)").parseFoldedInstruction(0)
        }
        assertThat(e).hasMessageThat()
            .contains("Expected `if`, `block`, `loop`, or a \"plain\" instruction")
    }

    @Test
    fun parse_throws_ifFoldedExpression_isNotClosed() {
        val e = assertThrows(ParseException::class.java) {
            tokenizer.tokenize("(br $0").parseFoldedInstruction(0)
        }
        assertThat(e).hasMessageThat()
            .contains("Expected ')' after folded instruction")
    }

    @Test
    fun parse_parsesPlainInstructions() {
        val result = tokenizer.tokenize(
            """(br_if $3 (br_if $1 (br_if $0)) (br_if $2))""",
            context
        ).parseFoldedInstruction(0)
        assertThat(result).isNotNull()
        assertThat(result?.astNode).hasSize(4)
        repeat(4) {
            assertThat(
                result?.astNode?.get(it)
            ).isEqualTo(ControlInstruction.BreakIf(Index.ByIdentifier(Identifier.Label("\$$it"))))
        }
        assertThat(result?.parseLength).isEqualTo(16)
    }

    @Test
    fun parse_parsesFoldedBlock() {
        val result = tokenizer.tokenize(
            """
            (block $0 (result i32)  
                return
            )
            """.trimIndent(),
            context
        ).parseFoldedInstruction(0) ?: fail("Expected to find something")
        assertThat(result.parseLength).isEqualTo(9)
        assertThat(result.astNode).hasSize(1)
        val block = result.astNode[0] as? ControlInstruction.Block ?: fail("Expected a block")
        assertThat(block.label).isEqualTo(Identifier.Label("$0"))
        assertThat(block.result).isEqualTo(
            ResultType(
                Result(
                    ValueType.I32
                )
            )
        )
        assertThat(block.instructions).hasSize(1)
        assertThat(block.instructions[0]).isEqualTo(ControlInstruction.Return)
    }

    @Test
    fun parse_parsesFoldedLoop() {
        val result = tokenizer.tokenize(
            """
            (loop $0 (result i32)  
                return
            )
            """.trimIndent(),
            context
        ).parseFoldedInstruction(0) ?: fail("Expected to find something")
        assertThat(result.parseLength).isEqualTo(9)
        assertThat(result.astNode).hasSize(1)
        val block = result.astNode[0] as? ControlInstruction.Loop ?: fail("Expected a loop")
        assertThat(block.label).isEqualTo(Identifier.Label("$0"))
        assertThat(block.result).isEqualTo(
            ResultType(
                Result(
                    ValueType.I32
                )
            )
        )
        assertThat(block.instructions).hasSize(1)
        assertThat(block.instructions[0]).isEqualTo(ControlInstruction.Return)
    }

    @Test
    fun parse_parsesFoldedIf() {
        val result = tokenizer.tokenize(
            """
            (if $0 (result i32) (call $1) 
                (then  
                    return
                )
            )
            """.trimIndent(),
            context
        ).parseFoldedInstruction(0) ?: fail("Expected to find something")
        assertThat(result.parseLength).isEqualTo(16)
        assertThat(result.astNode).hasSize(2)

        assertThat(result.astNode[0])
            .isEqualTo(
                ControlInstruction.Call(Index.ByIdentifier(Identifier.Function("$1")))
            )

        val block = result.astNode[1] as? ControlInstruction.If ?: fail("Expected an if")
        assertThat(block.label).isEqualTo(Identifier.Label("$0"))
        assertThat(block.result).isEqualTo(
            ResultType(
                Result(
                    ValueType.I32
                )
            )
        )
        assertThat(block.positiveInstructions).hasSize(1)
        assertThat(block.positiveInstructions[0]).isEqualTo(ControlInstruction.Return)
        assertThat(block.negativeInstructions).hasSize(0)
    }

    @Test
    fun parse_parsesFoldedIfElse() {
        val result = tokenizer.tokenize(
            """
            (if $0 (result i32) (call $1) 
                (then return)
                (else call $2)
            )
            """.trimIndent(),
            context
        ).parseFoldedInstruction(0) ?: fail("Expected to find something")
        assertThat(result.parseLength).isEqualTo(21)
        assertThat(result.astNode).hasSize(2)

        assertThat(result.astNode[0])
            .isEqualTo(
                ControlInstruction.Call(Index.ByIdentifier(Identifier.Function("$1")))
            )

        val block = result.astNode[1] as? ControlInstruction.If ?: fail("Expected an if")
        assertThat(block.label).isEqualTo(Identifier.Label("$0"))
        assertThat(block.result).isEqualTo(
            ResultType(
                Result(
                    ValueType.I32
                )
            )
        )
        assertThat(block.positiveInstructions).hasSize(1)
        assertThat(block.positiveInstructions[0]).isEqualTo(ControlInstruction.Return)
        assertThat(block.negativeInstructions).hasSize(1)
        assertThat(block.negativeInstructions[0])
            .isEqualTo(ControlInstruction.Call(Index.ByIdentifier(Identifier.Function("$2"))))
    }

    @Test
    fun parse_throwsWhen_foldedIf_doesNotInclude_then() {
        var e = assertThrows(ParseException::class.java) {
            tokenizer.tokenize(
                """
                (if (call $1))
                """.trimIndent(),
                context
            ).parseFoldedInstruction(0)
        }
        assertThat(e).hasMessageThat().contains("Expected 'then'")

        e = assertThrows(ParseException::class.java) {
            tokenizer.tokenize(
                """
                (if (call $1) (else))
                """.trimIndent(),
                context
            ).parseFoldedInstruction(0)
        }
        assertThat(e).hasMessageThat().contains("Expected 'then'")
    }

    @Test
    fun parse_throwsWhen_foldedIf_hasThenMissingClosingParen() {
        val e = assertThrows(ParseException::class.java) {
            tokenizer.tokenize(
                """
                (if (call $1) (then return)
                """.trimIndent(),
                context
            ).parseFoldedInstruction(0)
        }
        assertThat(e).hasMessageThat().contains("Expected ')'")
    }

    @Test
    fun parse_throwsWhen_foldedIf_hasAnother_foldedOp_afterThenBlock() {
        var e = assertThrows(ParseException::class.java) {
            tokenizer.tokenize(
                """
                (if (call $1) (then return) (call $2))
                """.trimIndent(),
                context
            ).parseFoldedInstruction(0)
        }
        assertThat(e).hasMessageThat().contains("Expecting 'else'")

        e = assertThrows(ParseException::class.java) {
            tokenizer.tokenize(
                """
                (if (call $1) (then return) ($2))
                """.trimIndent(),
                context
            ).parseFoldedInstruction(0)
        }
        assertThat(e).hasMessageThat().contains("Expecting 'else'")
    }

    @Test
    fun parse_throwsWhen_foldedIf_hasElseMissingClosingParen() {
        val e = assertThrows(ParseException::class.java) {
            tokenizer.tokenize(
                """
                (if (call $1) 
                    (then return) (else return
                )
                """.trimIndent(),
                context
            ).parseFoldedInstruction(0)
        }
        assertThat(e).hasMessageThat().contains("Expected ')'")
    }
}
