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
import kwasm.ast.module.Export
import kwasm.ast.module.ExportDescriptor
import kwasm.ast.module.Import
import kwasm.ast.module.ImportDescriptor
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

@Suppress("EXPERIMENTAL_UNSIGNED_LITERALS", "EXPERIMENTAL_API_USAGE", "UNCHECKED_CAST")
@RunWith(JUnit4::class)
class MemoryInlineExportTest {
    private val counts = TextModuleCounts(0, 0, 0, 0, 0)
    private val tokenizer = Tokenizer()
    private val context = ParseContext("MemoryInlineExportTest.wat")

    @Test
    fun parse_returnsNullIf_openingParenNotFound() {
        val result = tokenizer.tokenize("memory $0 (export \"a\"))", context)
            .parseInlineMemoryExport(0, counts)
        assertThat(result).isNull()
    }

    @Test
    fun parse_returnsNullIf_memoryKeywordNotFound() {
        val result = tokenizer.tokenize("(nonmemory $0 (export \"a\"))", context)
            .parseInlineMemoryExport(0, counts)
        assertThat(result).isNull()
    }

    @Test
    fun parse_returnsNullIf_exportOpeningParenNotFound() {
        val result = tokenizer.tokenize("(memory $0 export \"a\"))", context)
            .parseInlineMemoryExport(0, counts)
        assertThat(result).isNull()
    }

    @Test
    fun parse_returnsNullIf_exportKeywordNotFound() {
        val result = tokenizer.tokenize("(memory $0 (nonexport \"a\"))", context)
            .parseInlineMemoryExport(0, counts)
        assertThat(result).isNull()
    }

    @Test
    fun parse_throwsIf_exportNameNotFound() {
        assertThrows(ParseException::class.java) {
            tokenizer.tokenize("(memory $0 (export))", context)
                .parseInlineMemoryExport(0, counts)
        }
    }

    @Test
    fun parse_throwsIf_exportClosureNotFound() {
        val e = assertThrows(ParseException::class.java) {
            tokenizer.tokenize("(memory $0 (export \"a\"", context)
                .parseInlineMemoryExport(0, counts)
        }
        assertThat(e).hasMessageThat().contains("Expected ')'")
    }

    @Test
    fun parse_minimal() {
        val (result, newCounts) = tokenizer.tokenize("(memory (export \"a\"))", context)
            .parseInlineMemoryExport(0, counts) ?: Assertions.fail("Expected a result")
        assertThat(result.parseLength).isEqualTo(7)
        assertThat(result.astNode.size).isEqualTo(1)
        val export = result.astNode[0] as Export
        assertThat(export.name).isEqualTo("a")
        assertThat(export.descriptor).isInstanceOf(ExportDescriptor.Memory::class.java)
        assertThat(newCounts.memories).isEqualTo(counts.memories + 1)
    }

    @Test
    fun parse_simple() {
        val (result, newCounts) = tokenizer.tokenize("(memory $0 (export \"a\"))", context)
            .parseInlineMemoryExport(0, counts) ?: Assertions.fail("Expected a result")
        assertThat(result.parseLength).isEqualTo(8)
        assertThat(result.astNode).containsExactly(
            Export(
                "a",
                ExportDescriptor.Memory(
                    Index.ByInt(0) as Index<Identifier.Memory>
                )
            )
        ).inOrder()
        assertThat(newCounts.memories).isEqualTo(counts.memories + 1)
    }

    @Test
    fun parse_multipleExports() {
        val (result, newCounts) = tokenizer.tokenize(
            "(memory $0 (export \"a\") (export \"b\") (export \"c\"))",
            context
        ).parseInlineMemoryExport(0, counts) ?: Assertions.fail("Expected a result")
        assertThat(result.parseLength).isEqualTo(16)
        assertThat(result.astNode).containsExactly(
            Export(
                "a",
                ExportDescriptor.Memory(
                    Index.ByInt(0) as Index<Identifier.Memory>,
                )
            ),
            Export(
                "b",
                ExportDescriptor.Memory(
                    Index.ByInt(0) as Index<Identifier.Memory>,
                )
            ),
            Export(
                "c",
                ExportDescriptor.Memory(
                    Index.ByInt(0) as Index<Identifier.Memory>,
                )
            )
        ).inOrder()
        assertThat(newCounts.memories).isEqualTo(counts.memories + 1)
    }

    @Test
    fun parse_exportThenImport() {
        val (result, newCounts) = tokenizer.tokenize(
            "(memory $0 (export \"a\") (import \"b\" \"c\") 1)",
            context
        ).parseInlineMemoryExport(0, counts) ?: Assertions.fail("Expected a result")
        assertThat(result.parseLength).isEqualTo(14)
        assertThat(result.astNode).containsExactly(
            Export(
                "a",
                ExportDescriptor.Memory(
                    Index.ByInt(0) as Index<Identifier.Memory>
                )
            ),
            Import(
                "b",
                "c",
                ImportDescriptor.Memory(
                    Identifier.Memory("$0"),
                    MemoryType(Limits(1))
                )
            )
        ).inOrder()
        assertThat(newCounts.memories).isEqualTo(counts.memories + 1)
    }

    @Test
    fun parse_exportThenInlineData() {
        val (result, newCounts) = tokenizer.tokenize(
            "(memory $0 (export \"a\") (data \"test\"))",
            context
        ).parseInlineMemoryExport(0, counts) ?: Assertions.fail("Expected a result")
        assertThat(result.parseLength).isEqualTo(12)
        assertThat(result.astNode).containsExactly(
            Export(
                "a",
                ExportDescriptor.Memory(
                    Index.ByInt(0) as Index<Identifier.Memory>
                )
            ),
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
                "test".toByteArray(Charsets.UTF_8)
            )
        ).inOrder()
        assertThat(newCounts.memories).isEqualTo(counts.memories + 1)
    }

    @Test
    fun parse_multipleExportsThenInlineData() {
        val (result, newCounts) = tokenizer.tokenize(
            "(memory $0 (export \"a\") (export \"b\") (data \"test\"))",
            context
        ).parseInlineMemoryExport(0, counts) ?: Assertions.fail("Expected a result")
        assertThat(result.parseLength).isEqualTo(16)
        assertThat(result.astNode).containsExactly(
            Export(
                "a",
                ExportDescriptor.Memory(
                    Index.ByInt(0) as Index<Identifier.Memory>
                )
            ),
            Export(
                "b",
                ExportDescriptor.Memory(
                    Index.ByInt(0) as Index<Identifier.Memory>
                )
            ),
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
                "test".toByteArray(Charsets.UTF_8)
            )
        ).inOrder()
        assertThat(newCounts.memories).isEqualTo(counts.memories + 1)
    }
}
