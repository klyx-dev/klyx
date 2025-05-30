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
import kwasm.ast.module.Index
import kwasm.ast.module.Offset
import kwasm.format.ParseContext
import kwasm.format.ParseException
import kwasm.format.text.TextModuleCounts
import kwasm.format.text.Tokenizer
import org.assertj.core.api.Assertions
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@Suppress("EXPERIMENTAL_UNSIGNED_LITERALS")
@RunWith(JUnit4::class)
class ElementSegmentTest {
    private val counts = TextModuleCounts(0, 0, 0, 0, 0)
    private val tokenizer = Tokenizer()
    private val context = ParseContext("ElementSegmentTest.wast")

    @Test
    fun parse_returnsNull_ifStartToken_isNotOpenParen() {
        val result = tokenizer.tokenize("elem $0 (offset i32.const 0x44) $1)", context)
            .parseElementSegment(0, counts)
        assertThat(result).isNull()
    }

    @Test
    fun parse_returnsNull_ifTokenAfterOpeningParen_isNotElemKeyword() {
        val result = tokenizer.tokenize("(notelem $0 (offset i32.const 0x44) $1)", context)
            .parseElementSegment(0, counts)
        assertThat(result).isNull()
    }

    @Test
    fun throws_whenTableIndex_doesntFollow_dataKeyword() {
        assertThrows(ParseException::class.java) {
            tokenizer.tokenize("(elem \"test\" (offset i32.const 0x44) $1)")
                .parseElementSegment(0, counts)
        }
    }

    @Test
    fun throws_whenOffset_doesntFollow_tableIndex() {
        assertThrows(ParseException::class.java) {
            tokenizer.tokenize("(elem offset i32.const 0x44) $1)")
                .parseElementSegment(0, counts)
        }

        assertThrows(ParseException::class.java) {
            tokenizer.tokenize("(elem (notoffset i32.const 0x44) $1)")
                .parseElementSegment(0, counts)
        }

        assertThrows(ParseException::class.java) {
            tokenizer.tokenize("(elem $1)")
                .parseElementSegment(0, counts)
        }
    }

    @Test
    fun throws_whenNotClosedWithParen() {
        assertThrows(ParseException::class.java) {
            tokenizer.tokenize("(elem (offset i32.const 0x44) $1")
                .parseElementSegment(0, counts)
        }
    }

    @Test
    fun parses_withEmptyTableIndex() {
        val (result, newCounts) = tokenizer.tokenize("(elem (offset i32.const 0) $1)")
            .parseElementSegment(0, counts) ?: Assertions.fail("Expected segment")
        assertThat(result.parseLength).isEqualTo(9)
        assertThat(result.astNode.init).isEqualTo(
            astNodeListOf(
                Index.ByIdentifier(Identifier.Function("$1"))
            )
        )
        assertThat(result.astNode.tableIndex).isEqualTo(Index.ByInt(0))
        assertThat(result.astNode.offset).isEqualTo(
            Offset(
                Expression(
                    astNodeListOf(NumericConstantInstruction.I32(IntegerLiteral.S32(0)))
                )
            )
        )
        assertThat(newCounts).isEqualTo(counts)
    }

    @Test
    fun parses_withoutOffsetKeyword() {
        val (result, newCounts) = tokenizer.tokenize("(elem $0 (i32.const 0) $1)")
            .parseElementSegment(0, counts) ?: Assertions.fail("Expected segment")
        assertThat(result.parseLength).isEqualTo(9)
        assertThat(result.astNode.init).isEqualTo(
            astNodeListOf(
                Index.ByIdentifier(Identifier.Function("$1"))
            )
        )
        assertThat(result.astNode.tableIndex)
            .isEqualTo(Index.ByIdentifier(Identifier.Table("$0")))
        assertThat(result.astNode.offset).isEqualTo(
            Offset(
                Expression(
                    astNodeListOf(NumericConstantInstruction.I32(IntegerLiteral.S32(0)))
                )
            )
        )
        assertThat(newCounts).isEqualTo(counts)
    }

    @Test
    fun parses_withoutInitIndices() {
        val (result, newCounts) = tokenizer.tokenize("(elem $0 (i32.const 0))")
            .parseElementSegment(0, counts) ?: Assertions.fail("Expected segment")
        assertThat(result.parseLength).isEqualTo(8)
        assertThat(result.astNode.init).isEmpty()
        assertThat(result.astNode.tableIndex)
            .isEqualTo(Index.ByIdentifier(Identifier.Table("$0")))
        assertThat(result.astNode.offset).isEqualTo(
            Offset(
                Expression(
                    astNodeListOf(NumericConstantInstruction.I32(IntegerLiteral.S32(0)))
                )
            )
        )
        assertThat(newCounts).isEqualTo(counts)
    }

    @Test
    fun parses_minimal() {
        val (result, newCounts) = tokenizer.tokenize("(elem (i32.const 0))")
            .parseElementSegment(0, counts) ?: Assertions.fail("Expected segment")
        assertThat(result.parseLength).isEqualTo(7)
        assertThat(result.astNode.init).isEmpty()
        assertThat(result.astNode.tableIndex)
            .isEqualTo(Index.ByInt(0))
        assertThat(result.astNode.offset).isEqualTo(
            Offset(
                Expression(
                    astNodeListOf(NumericConstantInstruction.I32(IntegerLiteral.S32(0)))
                )
            )
        )
        assertThat(newCounts).isEqualTo(counts)
    }

    @Test
    fun parses_multipleFunctionIndices() {
        val (result, newCounts) = tokenizer.tokenize("(elem (i32.const 0) $1 $2 $3)")
            .parseElementSegment(0, counts) ?: Assertions.fail("Expected segment")
        assertThat(result.parseLength).isEqualTo(10)
        assertThat(result.astNode.init).isEqualTo(
            astNodeListOf(
                Index.ByIdentifier(Identifier.Function("$1")),
                Index.ByIdentifier(Identifier.Function("$2")),
                Index.ByIdentifier(Identifier.Function("$3"))
            )
        )
        assertThat(result.astNode.tableIndex)
            .isEqualTo(Index.ByInt(0))
        assertThat(result.astNode.offset).isEqualTo(
            Offset(
                Expression(
                    astNodeListOf(NumericConstantInstruction.I32(IntegerLiteral.S32(0)))
                )
            )
        )
        assertThat(newCounts).isEqualTo(counts)
    }
}
