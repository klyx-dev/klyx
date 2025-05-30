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
import kwasm.format.binary.toByteArray
import kwasm.format.text.TextModuleCounts
import kwasm.format.text.Tokenizer
import org.assertj.core.api.Assertions.fail
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@ExperimentalUnsignedTypes
@Suppress("EXPERIMENTAL_UNSIGNED_LITERALS")
@RunWith(JUnit4::class)
class DataSegmentTest {
    private val counts = TextModuleCounts(0, 0, 0, 0, 0)
    private val tokenizer = Tokenizer()
    private val context = ParseContext("DataSegmentTest.wast")

    @Test
    fun parse_returnsNull_ifStartToken_isNotOpenParen() {
        val result = tokenizer.tokenize("data $0 (offset i32.const 0x44) \"test\")", context)
            .parseDataSegment(0, counts)
        assertThat(result).isNull()
    }

    @Test
    fun parse_returnsNull_ifTokenAfterOpeningParen_isNotDataKeyword() {
        val result = tokenizer.tokenize("(notdata $0 (offset i32.const 0x44) \"test\")", context)
            .parseDataSegment(0, counts)
        assertThat(result).isNull()
    }

    @Test
    fun throws_whenMemoryIndex_doesntFollow_dataKeyword() {
        assertThrows(ParseException::class.java) {
            tokenizer.tokenize("(data \"test\" (offset i32.const 0x44) \"test\")")
                .parseDataSegment(0, counts)
        }
    }

    @Test
    fun throws_whenOffset_doesntFollow_memIndex() {
        assertThrows(ParseException::class.java) {
            tokenizer.tokenize("(data offset i32.const 0x44) \"test\")")
                .parseDataSegment(0, counts)
        }

        assertThrows(ParseException::class.java) {
            tokenizer.tokenize("(data (notoffset i32.const 0x44) \"test\")")
                .parseDataSegment(0, counts)
        }

        assertThrows(ParseException::class.java) {
            tokenizer.tokenize("(data \"test\")")
                .parseDataSegment(0, counts)
        }
    }

    @Test
    fun throws_whenNotClosedWithParen() {
        assertThrows(ParseException::class.java) {
            tokenizer.tokenize("(data (offset i32.const 0x44) \"test\"")
                .parseDataSegment(0, counts)
        }
    }

    @Test
    fun parses_withEmptyMemIndex() {
        val (result, newCounts) = tokenizer.tokenize("(data (offset i32.const 0) \"test\")")
            .parseDataSegment(0, counts) ?: fail("Expected data segment")
        assertThat(result.parseLength).isEqualTo(9)
        assertThat(result.astNode.init).isEqualTo("test".toByteArray(Charsets.UTF_8))
        assertThat(result.astNode.memoryIndex).isEqualTo(Index.ByInt(0))
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
        val (result, newCounts) = tokenizer.tokenize("(data $1 (i32.const 0) \"test\")")
            .parseDataSegment(0, counts) ?: fail("Expected data segment")
        assertThat(result.parseLength).isEqualTo(9)
        assertThat(result.astNode.init).isEqualTo("test".toByteArray(Charsets.UTF_8))
        assertThat(result.astNode.memoryIndex)
            .isEqualTo(Index.ByIdentifier(Identifier.Memory("$1")))
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
    fun parses_withoutInitStrings() {
        val (result, newCounts) = tokenizer.tokenize("(data $1 (i32.const 0))")
            .parseDataSegment(0, counts) ?: fail("Expected data segment")
        assertThat(result.parseLength).isEqualTo(8)
        assertThat(result.astNode.init).isEqualTo(ByteArray(0))
        assertThat(result.astNode.memoryIndex)
            .isEqualTo(Index.ByIdentifier(Identifier.Memory("$1")))
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
        val (result, newCounts) = tokenizer.tokenize("(data (i32.const 0))")
            .parseDataSegment(0, counts) ?: fail("Expected data segment")
        assertThat(result.parseLength).isEqualTo(7)
        assertThat(result.astNode.init).isEqualTo(ByteArray(0))
        assertThat(result.astNode.memoryIndex)
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
    fun parses_multipleStrings() {
        val (result, newCounts) = tokenizer.tokenize("(data (i32.const 0) \"a\" \"b\" \"c\")")
            .parseDataSegment(0, counts) ?: fail("Expected data segment")
        assertThat(result.parseLength).isEqualTo(10)
        assertThat(result.astNode.init).isEqualTo("abc".toByteArray(Charsets.UTF_8))
        assertThat(result.astNode.memoryIndex)
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
    fun parseDataString_backslashedBytes() {
        val result = tokenizer.tokenize(""""\00\00\00\00\00\00\a0\7f\01\00\d0\7f"""")
            .parseDataString(0)

        assertThat(result.first).isEqualTo(
            ByteArray(12).also {
                it[0] = 0
                it[1] = 0
                it[2] = 0
                it[3] = 0
                it[4] = 0
                it[5] = 0
                it[6] = 0xa0.toUByte().toByte()
                it[7] = 0x7f.toUByte().toByte()
                it[8] = 0x01.toUByte().toByte()
                it[9] = 0x00.toUByte().toByte()
                it[10] = 0xd0.toUByte().toByte()
                it[11] = 0x7f.toUByte().toByte()
            }
        )
        assertThat(result.second).isEqualTo(1)
    }

    @Test
    fun parseDataString_multipleLinesOfBackslashedBytes() {
        val (parsedBytes, parsedTokens) = tokenizer.tokenize(
            """
                "\00asm" "\01\00\00\00"
                "\01\04\01\60\00\00"       ;; Type section
                "\03\02\01\00"             ;; Function section
                "\0a\0c\01"                ;; Code section

                ;; function 0
                "\0a\02"
                "\ff\ff\ff\ff\0f\7f"       ;; 0xFFFFFFFF i32
                "\02\7e"                   ;; 0x00000002 i64
                "\0b"                      ;; end
            """.trimIndent()
        ).parseDataString(0)

        assertThat(parsedBytes).isEqualTo(
            listOf(
                0x00, 'a'.toInt(), 's'.toInt(), 'm'.toInt(), 0x01, 0x00, 0x00, 0x00,
                0x01, 0x04, 0x01, 0x60, 0x00, 0x00,
                0x03, 0x02, 0x01, 0x00,
                0x0a, 0x0c, 0x01,
                0x0a, 0x02,
                0xff, 0xff, 0xff, 0xff, 0x0f, 0x7f,
                0x02, 0x7e,
                0x0b
            ).toByteArray()
        )
        assertThat(parsedTokens).isEqualTo(9)
    }
}
