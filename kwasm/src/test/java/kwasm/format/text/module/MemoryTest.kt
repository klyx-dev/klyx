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

package kwasm.format.text.module

import com.google.common.truth.Truth.assertThat
import kwasm.ast.Identifier
import kwasm.ast.IntegerLiteral
import kwasm.ast.astNodeListOf
import kwasm.ast.instruction.Expression
import kwasm.ast.instruction.NumericConstantInstruction
import kwasm.ast.module.DataSegment
import kwasm.ast.module.Index
import kwasm.ast.module.Memory
import kwasm.ast.module.Offset
import kwasm.ast.type.Limits
import kwasm.ast.type.MemoryType
import kwasm.format.ParseContext
import kwasm.format.ParseException
import kwasm.format.text.TextModuleCounts
import kwasm.format.text.Tokenizer
import org.assertj.core.api.Assertions
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
@Suppress("EXPERIMENTAL_UNSIGNED_LITERALS", "EXPERIMENTAL_API_USAGE", "UNCHECKED_CAST")
class MemoryTest {
    private val counts = TextModuleCounts(0, 0, 0, 0, 0)
    private val tokenizer = Tokenizer()
    private val context = ParseContext("MemoryTest.wast")

    @Test
    fun parseMemoryBasic_returnsNull_ifOpenParenNotFound() {
        val result = tokenizer.tokenize("memory $0 0 1)", context)
            .parseMemoryBasic(0, counts)
        assertThat(result).isNull()
    }

    @Test
    fun parseMemoryBasic_returnsNull_ifMemoryKeywordNotFound() {
        val result = tokenizer.tokenize("(non-memory $0 0 1)", context)
            .parseMemoryBasic(0, counts)
        assertThat(result).isNull()
    }

    @Test
    fun parseMemoryBasic_throws_ifMemoryTypeCouldntBeFound() {
        assertThrows(ParseException::class.java) {
            tokenizer.tokenize("(memory $0)", context).parseMemoryBasic(0, counts)
        }
    }

    @Test
    fun parseMemoryBasic_throws_ifNoClosingParenFound() {
        val e = assertThrows(ParseException::class.java) {
            tokenizer.tokenize("(memory $0 0 1", context).parseMemoryBasic(0, counts)
        }
        assertThat(e).hasMessageThat().contains("Expected ')'")
    }

    @Test
    fun parseMemoryBasic() {
        val (result, newCounts) = tokenizer.tokenize("(memory $0 0 1)", context)
            .parseMemoryBasic(0, counts) ?: Assertions.fail("Expected a result")
        assertThat(result.parseLength).isEqualTo(6)
        assertThat(result.astNode.id).isEqualTo(Identifier.Memory("$0"))
        assertThat(result.astNode.memoryType).isEqualTo(
            MemoryType(
                Limits(0, 1)
            )
        )
        assertThat(newCounts.memories).isEqualTo(counts.memories + 1)
    }

    @Test
    fun parseMemoryAndDataSegment_returnsNullIf_openingParen_notFound() {
        val result = tokenizer.tokenize("memory $0 (data \"first\" \"second\"))", context)
            .parseMemoryAndDataSegment(0, counts)
        assertThat(result).isNull()
    }

    @Test
    fun parseMemoryAndDataSegment_returnsNullIf_memoryKeyword_notFound() {
        val result = tokenizer.tokenize("(non-memory $0 (data \"first\" \"second\"))", context)
            .parseMemoryAndDataSegment(0, counts)
        assertThat(result).isNull()
    }

    @Test
    fun parseMemoryAndDataSegment_throwsIf_dataOpeningParen_notFound() {
        val e = assertThrows(ParseException::class.java) {
            tokenizer.tokenize("(memory $0 data \"first\" \"second\"))", context)
                .parseMemoryAndDataSegment(0, counts)
        }
        assertThat(e).hasMessageThat().contains("Expected inline data segment")
    }

    @Test
    fun parseMemoryAndDataSegment_throwsIf_dataKeyword_notFound() {
        val e = assertThrows(ParseException::class.java) {
            tokenizer.tokenize("(memory $0 (non-data \"first\" \"second\"))", context)
                .parseMemoryAndDataSegment(0, counts)
        }
        assertThat(e).hasMessageThat().contains("Expected inline data segment")
    }

    @Test
    fun parseMemoryAndDataSegment_throwsIf_dataClosingParen_notFound() {
        val e = assertThrows(ParseException::class.java) {
            tokenizer.tokenize("(memory $0 (data \"first\" \"second\")", context)
                .parseMemoryAndDataSegment(0, counts)
        }
        assertThat(e).hasMessageThat().contains("Expected ')'")
    }

    @Test
    fun parseMemoryAndDataSegment_throwsIf_closingParen_notFound() {
        val e = assertThrows(ParseException::class.java) {
            tokenizer.tokenize("(memory $0 (data \"first\" \"second\"", context)
                .parseMemoryAndDataSegment(0, counts)
        }
        assertThat(e).hasMessageThat().contains("Expected ')'")
    }

    @Test
    fun parseMemoryAndDataSegment_minimal() {
        val (result, newCounts) = tokenizer.tokenize("(memory (data))", context)
            .parseMemoryAndDataSegment(0, counts) ?: Assertions.fail("Expected a result")
        assertThat(result.parseLength).isEqualTo(6)
        assertThat(result.astNode.size).isEqualTo(2)
        val memory = result.astNode[0] as Memory
        val dataSegment = result.astNode[1] as DataSegment
        assertThat(memory.memoryType).isEqualTo(MemoryType(Limits(0, 0)))
        assertThat(dataSegment.offset).isEqualTo(
            Offset(
                Expression(
                    astNodeListOf(NumericConstantInstruction.I32(IntegerLiteral.S32(0)))
                )
            )
        )
        assertThat(dataSegment.init).isEqualTo("".toByteArray(Charsets.UTF_8))
        assertThat(newCounts.memories).isEqualTo(counts.memories + 1)
    }

    @Test
    fun parseMemoryAndDataSegment() {
        val (result, newCounts) =
            tokenizer.tokenize("(memory $0 (data \"first\" \"second\"))", context)
                .parseMemoryAndDataSegment(0, counts) ?: Assertions.fail("Expected a result")
        assertThat(result.parseLength).isEqualTo(9)
        assertThat(result.astNode).containsExactly(
            Memory(
                Identifier.Memory("$0"),
                MemoryType(
                    Limits(1, 1)
                )
            ),
            DataSegment(
                Index.ByInt(0) as Index<Identifier.Memory>,
                Offset(
                    Expression(
                        astNodeListOf(NumericConstantInstruction.I32(IntegerLiteral.S32(0)))
                    )
                ),
                "firstsecond".toByteArray(Charsets.UTF_8)
            )
        ).inOrder()
        assertThat(newCounts.memories).isEqualTo(counts.memories + 1)
    }

    @Test
    fun parseMemory_parsesPlain() {
        val (result, newCounts) = tokenizer.tokenize("(memory $0 0 1)", context)
            .parseMemory(0, counts) ?: Assertions.fail("Expected a result")
        assertThat(result.parseLength).isEqualTo(6)
        assertThat(result.astNode.size).isEqualTo(1)
        assertThat((result.astNode.first() as Memory).id).isEqualTo(Identifier.Memory("$0"))
        assertThat((result.astNode.first() as Memory).memoryType).isEqualTo(
            MemoryType(
                Limits(0, 1)
            )
        )
        assertThat(newCounts.memories).isEqualTo(counts.memories + 1)
    }

    @Test
    fun parseTable_parsesWithElementSegment() {
        val (result, newCounts) =
            tokenizer.tokenize("(memory $0 (data \"first\" \"second\"))", context)
                .parseMemory(0, counts) ?: Assertions.fail("Expected a result")
        assertThat(result.parseLength).isEqualTo(9)
        assertThat(result.astNode).containsExactly(
            Memory(
                Identifier.Memory("$0"),
                MemoryType(
                    Limits(1, 1)
                )
            ),
            DataSegment(
                Index.ByInt(0) as Index<Identifier.Memory>,
                Offset(
                    Expression(
                        astNodeListOf(NumericConstantInstruction.I32(IntegerLiteral.S32(0)))
                    )
                ),
                "firstsecond".toByteArray(Charsets.UTF_8)
            )
        ).inOrder()
        assertThat(newCounts.memories).isEqualTo(counts.memories + 1)
    }
}
