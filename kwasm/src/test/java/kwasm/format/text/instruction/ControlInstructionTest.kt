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
import kwasm.ast.astNodeListOf
import kwasm.ast.instruction.ControlInstruction
import kwasm.ast.module.Index
import kwasm.ast.module.TypeUse
import kwasm.ast.type.Param
import kwasm.ast.type.Result
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
class ControlInstructionTest {
    private val tokenizer = Tokenizer()
    private val context = ParseContext("ControlInstructionTest.wat", 1, 1)

    @Test
    fun parse_parsesBlock() {
        val parsed = tokenizer.tokenize(
            """
            block
                nop
                return
                unreachable
            end
            """.trimIndent(),
            context
        ).parseControlInstruction(0) ?: fail("Expected an instruction")
        assertThat(parsed.parseLength).isEqualTo(5)
        val block = parsed.astNode as? ControlInstruction.Block ?: fail("Expected a block")
        assertThat(block.label).isNull()
        assertThat(block.result.result).isNull()
        assertThat(block.instructions).hasSize(3)
        assertThat(block.instructions[0]).isEqualTo(ControlInstruction.NoOp)
        assertThat(block.instructions[1]).isEqualTo(ControlInstruction.Return)
        assertThat(block.instructions[2]).isEqualTo(ControlInstruction.Unreachable)
    }

    @Test
    fun parse_parsesBlock_withReturnType() {
        val parsed = tokenizer.tokenize(
            """
            block (result i32)
                nop
                return
                unreachable
            end
            """.trimIndent(),
            context
        ).parseControlInstruction(0) ?: fail("Expected an instruction")
        assertThat(parsed.parseLength).isEqualTo(9)
        val block = parsed.astNode as? ControlInstruction.Block ?: fail("Expected a block")
        assertThat(block.label).isNull()
        assertThat(block.result.result).isEqualTo(Result(ValueType.I32))
        assertThat(block.instructions).hasSize(3)
        assertThat(block.instructions[0]).isEqualTo(ControlInstruction.NoOp)
        assertThat(block.instructions[1]).isEqualTo(ControlInstruction.Return)
        assertThat(block.instructions[2]).isEqualTo(ControlInstruction.Unreachable)
    }

    @Test
    fun parse_parsesBlock_withLabel() {
        val parsed = tokenizer.tokenize(
            """
            block ${'$'}myBlock
                nop
                return
                unreachable
            end
            """.trimIndent(),
            context
        ).parseControlInstruction(0) ?: fail("Expected an instruction")
        assertThat(parsed.parseLength).isEqualTo(6)
        val block = parsed.astNode as? ControlInstruction.Block ?: fail("Expected a block")
        assertThat(block.label?.stringRepr).isEqualTo("\$myBlock")
        assertThat(block.result.result).isNull()
        assertThat(block.instructions).hasSize(3)
        assertThat(block.instructions[0]).isEqualTo(ControlInstruction.NoOp)
        assertThat(block.instructions[1]).isEqualTo(ControlInstruction.Return)
        assertThat(block.instructions[2]).isEqualTo(ControlInstruction.Unreachable)
    }

    @Test
    fun parse_parsesBlock_withLabel_andEndLabel() {
        val parsed = tokenizer.tokenize(
            """
            block ${'$'}myBlock
                nop
                return
                unreachable
            end ${'$'}myBlock
            """.trimIndent(),
            context
        ).parseControlInstruction(0) ?: fail("Expected an instruction")
        assertThat(parsed.parseLength).isEqualTo(7)
        val block = parsed.astNode as? ControlInstruction.Block ?: fail("Expected a block")
        assertThat(block.label?.stringRepr).isEqualTo("\$myBlock")
        assertThat(block.result.result).isNull()
        assertThat(block.instructions).hasSize(3)
        assertThat(block.instructions[0]).isEqualTo(ControlInstruction.NoOp)
        assertThat(block.instructions[1]).isEqualTo(ControlInstruction.Return)
        assertThat(block.instructions[2]).isEqualTo(ControlInstruction.Unreachable)
    }

    @Test
    fun parse_throws_whenBlock_hasNoEnd() {
        val exception = assertThrows(ParseException::class.java) {
            tokenizer.tokenize(
                """
                block
                    nop
                    return
                    unreachable
                """.trimIndent(),
                context
            ).parseControlInstruction(0)
        }
        assertThat(exception).hasMessageThat().contains("Expected \"end\"")
    }

    @Test
    fun parse_throws_whenBlock_endLabel_mismatchesOpenerLabel() {
        assertThrows(ParseException::class.java) {
            tokenizer.tokenize(
                """
                block
                    nop
                    return
                    unreachable
                end ${'$'}myBlock
                """.trimIndent(),
                context
            ).parseControlInstruction(0)
        }

        assertThrows(ParseException::class.java) {
            tokenizer.tokenize(
                """
                block ${'$'}myBlock 
                    nop
                    return
                    unreachable
                end ${'$'}oops
                """.trimIndent(),
                context
            ).parseControlInstruction(0)
        }
    }

    @Test
    fun parse_parsesLoop() {
        val parsed = tokenizer.tokenize(
            """
            loop
                nop
                return
                unreachable
            end
            """.trimIndent(),
            context
        ).parseControlInstruction(0) ?: fail("Expected an instruction")
        assertThat(parsed.parseLength).isEqualTo(5)
        val block = parsed.astNode as? ControlInstruction.Loop ?: fail("Expected a loop")
        assertThat(block.label).isNull()
        assertThat(block.result.result).isNull()
        assertThat(block.instructions).hasSize(3)
        assertThat(block.instructions[0]).isEqualTo(ControlInstruction.NoOp)
        assertThat(block.instructions[1]).isEqualTo(ControlInstruction.Return)
        assertThat(block.instructions[2]).isEqualTo(ControlInstruction.Unreachable)
    }

    @Test
    fun parse_parsesLoop_withLabel() {
        val parsed = tokenizer.tokenize(
            """
            loop ${'$'}myLoop
                nop
                return
                unreachable
            end
            """.trimIndent(),
            context
        ).parseControlInstruction(0) ?: fail("Expected an instruction")
        assertThat(parsed.parseLength).isEqualTo(6)
        val block = parsed.astNode as? ControlInstruction.Loop ?: fail("Expected a loop")
        assertThat(block.label?.stringRepr).isEqualTo("\$myLoop")
        assertThat(block.result.result).isNull()
        assertThat(block.instructions).hasSize(3)
        assertThat(block.instructions[0]).isEqualTo(ControlInstruction.NoOp)
        assertThat(block.instructions[1]).isEqualTo(ControlInstruction.Return)
        assertThat(block.instructions[2]).isEqualTo(ControlInstruction.Unreachable)
    }

    @Test
    fun parse_parsesLoop_withLabel_andEndLabel() {
        val parsed = tokenizer.tokenize(
            """
            loop ${'$'}myLoop
                nop
                return
                unreachable
            end ${'$'}myLoop
            """.trimIndent(),
            context
        ).parseControlInstruction(0) ?: fail("Expected an instruction")
        assertThat(parsed.parseLength).isEqualTo(7)
        val block = parsed.astNode as? ControlInstruction.Loop ?: fail("Expected a loop")
        assertThat(block.label?.stringRepr).isEqualTo("\$myLoop")
        assertThat(block.result.result).isNull()
        assertThat(block.instructions).hasSize(3)
        assertThat(block.instructions[0]).isEqualTo(ControlInstruction.NoOp)
        assertThat(block.instructions[1]).isEqualTo(ControlInstruction.Return)
        assertThat(block.instructions[2]).isEqualTo(ControlInstruction.Unreachable)
    }

    @Test
    fun parse_throws_whenLoop_hasNoEnd() {
        val exception = assertThrows(ParseException::class.java) {
            tokenizer.tokenize(
                """
                loop
                    nop
                    return
                    unreachable
                """.trimIndent(),
                context
            ).parseControlInstruction(0)
        }
        assertThat(exception).hasMessageThat().contains("Expected \"end\"")
    }

    @Test
    fun parse_throws_whenLoop_endLabel_mismatchesOpenerLabel() {
        assertThrows(ParseException::class.java) {
            tokenizer.tokenize(
                """
                loop
                    nop
                    return
                    unreachable
                end ${'$'}myBlock
                """.trimIndent(),
                context
            ).parseControlInstruction(0)
        }

        assertThrows(ParseException::class.java) {
            tokenizer.tokenize(
                """
                loop ${'$'}myLoop
                    nop
                    return
                    unreachable
                end ${'$'}oops
                """.trimIndent(),
                context
            ).parseControlInstruction(0)
        }
    }

    @Test
    fun parse_parsesIf() {
        val parsed = tokenizer.tokenize(
            """
            if
                nop
                return
                unreachable
            end
            """.trimIndent(),
            context
        ).parseControlInstruction(0) ?: fail("Expected an instruction")
        assertThat(parsed.parseLength).isEqualTo(5)
        val block = parsed.astNode as? ControlInstruction.If ?: fail("Expected an if")
        assertThat(block.label).isNull()
        assertThat(block.result.result).isNull()
        assertThat(block.positiveInstructions).hasSize(3)
        assertThat(block.positiveInstructions[0]).isEqualTo(ControlInstruction.NoOp)
        assertThat(block.positiveInstructions[1]).isEqualTo(ControlInstruction.Return)
        assertThat(block.positiveInstructions[2]).isEqualTo(ControlInstruction.Unreachable)
        assertThat(block.negativeInstructions).isEmpty()
    }

    @Test
    fun parse_parsesIfElse() {
        val parsed = tokenizer.tokenize(
            """
            if
                nop
                return
                unreachable
            else
                nop
            end
            """.trimIndent(),
            context
        ).parseControlInstruction(0) ?: fail("Expected an instruction")
        assertThat(parsed.parseLength).isEqualTo(7)
        val block = parsed.astNode as? ControlInstruction.If ?: fail("Expected an if")
        assertThat(block.label).isNull()
        assertThat(block.result.result).isNull()
        assertThat(block.positiveInstructions).hasSize(3)
        assertThat(block.positiveInstructions[0]).isEqualTo(ControlInstruction.NoOp)
        assertThat(block.positiveInstructions[1]).isEqualTo(ControlInstruction.Return)
        assertThat(block.positiveInstructions[2]).isEqualTo(ControlInstruction.Unreachable)
        assertThat(block.negativeInstructions).hasSize(1)
        assertThat(block.negativeInstructions[0]).isEqualTo(ControlInstruction.NoOp)
    }

    @Test
    fun throws_whenIfElse_isMalformed() {
        assertThrows(ParseException::class.java) {
            tokenizer.tokenize(
                """
                if
                    nop
                    return
                else
                    unreachable
                else ;; hmm, extra else here
                    nop
                end
                """.trimIndent(),
                context
            ).parseControlInstruction(0)
        }

        assertThrows(ParseException::class.java) {
            tokenizer.tokenize(
                """
                if
                    nop
                    return
                    nop
                ;; missing 'end'
                """.trimIndent(),
                context
            ).parseControlInstruction(0)
        }

        assertThrows(ParseException::class.java) {
            tokenizer.tokenize(
                """
                if
                    nop
                    return
                else
                    nop
                ;; missing 'end'
                """.trimIndent(),
                context
            ).parseControlInstruction(0)
        }

        assertThrows(ParseException::class.java) {
            tokenizer.tokenize(
                """
                if
                    nop
                    return
                    nop
                end ${'$'}badId
                """.trimIndent(),
                context
            ).parseControlInstruction(0)
        }

        assertThrows(ParseException::class.java) {
            tokenizer.tokenize(
                """
                if
                    nop
                    return
                else ${'$'}badId
                    nop
                end
                """.trimIndent(),
                context
            ).parseControlInstruction(0)
        }
    }

    @Test
    fun parse_parsesUnreachable() {
        val parsed = tokenizer.tokenize("unreachable", context)
            .parseControlInstruction(0) ?: fail("Expected an instruction")
        assertThat(parsed.astNode).isInstanceOf(ControlInstruction.Unreachable::class.java)
        assertThat(parsed.parseLength).isEqualTo(1)
    }

    @Test
    fun parse_parsesNoOp() {
        val parsed = tokenizer.tokenize("nop", context)
            .parseControlInstruction(0) ?: fail("Expected an instruction")
        assertThat(parsed.astNode).isInstanceOf(ControlInstruction.NoOp::class.java)
        assertThat(parsed.parseLength).isEqualTo(1)
    }

    @Test
    fun parse_parsesReturn() {
        val parsed = tokenizer.tokenize("return", context)
            .parseControlInstruction(0) ?: fail("Expected an instruction")
        assertThat(parsed.astNode).isInstanceOf(ControlInstruction.Return::class.java)
        assertThat(parsed.parseLength).isEqualTo(1)
    }

    @Test
    fun parse_parsesBreak() {
        val parsed = tokenizer.tokenize("br $1", context)
            .parseControlInstruction(0) ?: fail("Expected an instruction")
        assertThat(parsed.parseLength).isEqualTo(2)
        assertThat(parsed.astNode).isInstanceOf(ControlInstruction.Break::class.java)
        val instruction = parsed.astNode as ControlInstruction.Break
        assertThat(instruction)
            .isEqualTo(
                ControlInstruction.Break(Index.ByIdentifier(Identifier.Label("$1")))
            )
    }

    @Test
    fun parse_throwsOn_break_withNoIndex() {
        val exception1 = assertThrows(ParseException::class.java) {
            tokenizer.tokenize("br ;; \$id").parseControlInstruction(0)
        }
        assertThat(exception1).hasMessageThat().contains("Expected an index")

        val exception2 = assertThrows(ParseException::class.java) {
            tokenizer.tokenize("br oops-not-an-id").parseControlInstruction(0)
        }
        assertThat(exception2).hasMessageThat().contains("Expected an index")
    }

    @Test
    fun parse_parsesBreakIf() {
        val parsed = tokenizer.tokenize("br_if $1", context)
            .parseControlInstruction(0) ?: fail("Expected an instruction")
        assertThat(parsed.parseLength).isEqualTo(2)
        assertThat(parsed.astNode).isInstanceOf(ControlInstruction.BreakIf::class.java)
        val instruction = parsed.astNode as ControlInstruction.BreakIf
        assertThat(instruction)
            .isEqualTo(
                ControlInstruction.BreakIf(Index.ByIdentifier(Identifier.Label("$1")))
            )
    }

    @Test
    fun parse_throwsOn_breakIf_withNoIndex() {
        val exception1 = assertThrows(ParseException::class.java) {
            tokenizer.tokenize("br_if;; \$id").parseControlInstruction(0)
        }
        assertThat(exception1).hasMessageThat().contains("Expected an index")

        val exception2 = assertThrows(ParseException::class.java) {
            tokenizer.tokenize("br_if oops-not-an-id").parseControlInstruction(0)
        }
        assertThat(exception2).hasMessageThat().contains("Expected an index")
    }

    @Test
    fun parse_parsesBreakTable_onlyWithDefault() {
        val parsed = tokenizer.tokenize("br_table $1", context)
            .parseControlInstruction(0) ?: fail("Expected an instruction")
        assertThat(parsed.parseLength).isEqualTo(2)
        assertThat(parsed.astNode).isInstanceOf(ControlInstruction.BreakTable::class.java)
        val instruction = parsed.astNode as ControlInstruction.BreakTable
        assertThat(instruction)
            .isEqualTo(
                ControlInstruction.BreakTable(
                    emptyList(),
                    Index.ByIdentifier(Identifier.Label("$1"))
                )
            )
    }

    @Test
    fun parse_throwsOn_breakTable_withNoIndex() {
        val exception1 = assertThrows(ParseException::class.java) {
            tokenizer.tokenize("br_table ;; \$id").parseControlInstruction(0)
        }
        assertThat(exception1).hasMessageThat().contains("Expected at least 1 index")

        val exception2 = assertThrows(ParseException::class.java) {
            tokenizer.tokenize("br_table oops-not-an-id").parseControlInstruction(0)
        }
        assertThat(exception2).hasMessageThat().contains("Expected at least 1 index")
    }

    @Test
    fun parse_parsesBreakTable_withTargets() {
        val parsed = tokenizer.tokenize("br_table $0 $1 $2", context)
            .parseControlInstruction(0) ?: fail("Expected an instruction")
        assertThat(parsed.parseLength).isEqualTo(4)
        assertThat(parsed.astNode).isInstanceOf(ControlInstruction.BreakTable::class.java)
        val instruction = parsed.astNode as ControlInstruction.BreakTable
        assertThat(instruction)
            .isEqualTo(
                ControlInstruction.BreakTable(
                    listOf(
                        Index.ByIdentifier(Identifier.Label("$0")),
                        Index.ByIdentifier(Identifier.Label("$1"))
                    ),
                    Index.ByIdentifier(Identifier.Label("$2"))
                )
            )
    }

    @Test
    fun parse_parsesCall() {
        val parsed = tokenizer.tokenize("call \$myFun", context)
            .parseControlInstruction(0) ?: fail("Expected an instruction")
        assertThat(parsed.astNode).isInstanceOf(ControlInstruction.Call::class.java)
        assertThat(parsed.parseLength).isEqualTo(2)

        val call = parsed.astNode as ControlInstruction.Call
        assertThat(call.functionIndex)
            .isEqualTo(Index.ByIdentifier(Identifier.Function("\$myFun")))
    }

    @Test
    fun parse_parsesCallIndirect() {
        val parsed = tokenizer
            .tokenize("call_indirect (type \$myType) (param i32) (result f32)", context)
            .parseControlInstruction(0) ?: fail("Expected an instruction")
        assertThat(parsed.astNode).isInstanceOf(ControlInstruction.CallIndirect::class.java)
        assertThat(parsed.parseLength).isEqualTo(13)

        val call = parsed.astNode as ControlInstruction.CallIndirect
        assertThat(call.typeUse)
            .isEqualTo(
                TypeUse(
                    Index.ByIdentifier(Identifier.Type("\$myType")),
                    astNodeListOf(
                        Param(
                            Identifier.Local(
                                null,
                                null
                            ),
                            ValueType.I32
                        )
                    ),
                    astNodeListOf(Result(ValueType.F32))
                )
            )
    }

    @Test
    fun parse_returnsNullIfNotAnInstruction() {
        val parsed = tokenizer.tokenize("0x10", context)
            .parseControlInstruction(0)
        assertThat(parsed).isNull()
    }

    @Test
    fun controlInstruction_usedBy_parseInstruction() {
        val parsed = tokenizer.tokenize("unreachable", context).parseInstruction(0)
            ?: fail("Expected instruction")
        assertThat(parsed.parseLength).isEqualTo(1)
        assertThat(parsed.astNode).isEqualTo(ControlInstruction.Unreachable)
    }
}
