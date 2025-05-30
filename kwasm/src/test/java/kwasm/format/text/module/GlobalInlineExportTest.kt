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
import kwasm.ast.module.Export
import kwasm.ast.module.ExportDescriptor
import kwasm.ast.module.Global
import kwasm.ast.module.Import
import kwasm.ast.module.ImportDescriptor
import kwasm.ast.module.Index
import kwasm.ast.type.GlobalType
import kwasm.ast.type.ValueType
import kwasm.format.ParseContext
import kwasm.format.ParseException
import kwasm.format.text.TextModuleCounts
import kwasm.format.text.Tokenizer
import org.assertj.core.api.Assertions
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@Suppress("UNCHECKED_CAST")
@RunWith(JUnit4::class)
class GlobalInlineExportTest {
    private val counts = TextModuleCounts(0, 0, 0, 0, 0)
    private val tokenizer = Tokenizer()
    private val context = ParseContext("GlobalInlineExportTest.wat")

    @Test
    fun parse_returnsNullIf_openingParenNotFound() {
        val result = tokenizer.tokenize("global $0 (export \"a\"))", context)
            .parseInlineGlobalExport(0, counts)
        assertThat(result).isNull()
    }

    @Test
    fun parse_returnsNullIf_globalKeywordNotFound() {
        val result = tokenizer.tokenize("(nonglobal $0 (export \"a\"))", context)
            .parseInlineGlobalExport(0, counts)
        assertThat(result).isNull()
    }

    @Test
    fun parse_returnsNullIf_exportOpeningParenNotFound() {
        val result = tokenizer.tokenize("(global $0 export \"a\"))", context)
            .parseInlineGlobalExport(0, counts)
        assertThat(result).isNull()
    }

    @Test
    fun parse_returnsNullIf_exportKeywordNotFound() {
        val result = tokenizer.tokenize("(global $0 (nonexport \"a\"))", context)
            .parseInlineGlobalExport(0, counts)
        assertThat(result).isNull()
    }

    @Test
    fun parse_throwsIf_exportNameNotFound() {
        assertThrows(ParseException::class.java) {
            tokenizer.tokenize("(global $0 (export))", context)
                .parseInlineGlobalExport(0, counts)
        }
    }

    @Test
    fun parse_throwsIf_exportClosureNotFound() {
        val e = assertThrows(ParseException::class.java) {
            tokenizer.tokenize("(global $0 (export \"a\"", context)
                .parseInlineGlobalExport(0, counts)
        }
        assertThat(e).hasMessageThat().contains("Expected ')'")
    }

    @Test
    fun parse_minimal() {
        val (result, newCounts) = tokenizer.tokenize("(global (export \"a\"))", context)
            .parseInlineGlobalExport(0, counts) ?: Assertions.fail("Expected a result")
        assertThat(result.parseLength).isEqualTo(7)
        assertThat(result.astNode.size).isEqualTo(1)
        val node = result.astNode[0] as Export
        assertThat(node.name).isEqualTo("a")
        assertThat(newCounts.globals).isEqualTo(counts.globals + 1)
    }

    @Test
    fun parse_simple() {
        val (result, newCounts) = tokenizer.tokenize("(global $0 (export \"a\"))", context)
            .parseInlineGlobalExport(0, counts) ?: Assertions.fail("Expected a result")
        assertThat(result.parseLength).isEqualTo(8)
        assertThat(result.astNode).containsExactly(
            Export(
                "a",
                ExportDescriptor.Global(
                    Index.ByInt(0) as Index<Identifier.Global>
                )
            )
        ).inOrder()
        assertThat(newCounts.globals).isEqualTo(counts.globals + 1)
    }

    @Test
    fun parse_multipleExports() {
        val (result, newCounts) = tokenizer.tokenize(
            "(global $0 (export \"a\") (export \"b\") (export \"c\"))",
            context
        ).parseInlineGlobalExport(0, counts) ?: Assertions.fail("Expected a result")
        assertThat(result.parseLength).isEqualTo(16)
        assertThat(result.astNode).containsExactly(
            Export(
                "a",
                ExportDescriptor.Global(
                    Index.ByInt(0) as Index<Identifier.Global>
                )
            ),
            Export(
                "b",
                ExportDescriptor.Global(
                    Index.ByInt(0) as Index<Identifier.Global>
                )
            ),
            Export(
                "c",
                ExportDescriptor.Global(
                    Index.ByInt(0) as Index<Identifier.Global>
                )
            )
        ).inOrder()
        assertThat(newCounts.globals).isEqualTo(counts.globals + 1)
    }

    @Test
    fun parse_exportThenImport() {
        val (result, newCounts) = tokenizer.tokenize(
            "(global $0 (export \"a\") (import \"b\" \"c\") i32)",
            context
        ).parseInlineGlobalExport(0, counts) ?: Assertions.fail("Expected a result")
        assertThat(result.parseLength).isEqualTo(14)
        assertThat(result.astNode).containsExactly(
            Export(
                "a",
                ExportDescriptor.Global(
                    Index.ByInt(0) as Index<Identifier.Global>
                )
            ),
            Import(
                "b",
                "c",
                ImportDescriptor.Global(
                    Identifier.Global("$0"),
                    GlobalType(ValueType.I32, false)
                )
            )
        ).inOrder()
        assertThat(newCounts.globals).isEqualTo(counts.globals + 1)
    }

    @Test
    fun parse_exportThenDescriptor() {
        val (result, newCounts) = tokenizer.tokenize(
            "(global $0 (export \"a\") i32 (i32.const 0))",
            context
        ).parseInlineGlobalExport(0, counts) ?: Assertions.fail("Expected a result")
        assertThat(result.parseLength).isEqualTo(13)
        assertThat(result.astNode).containsExactly(
            Export(
                "a",
                ExportDescriptor.Global(
                    Index.ByInt(0) as Index<Identifier.Global>
                )
            ),
            Global(
                Identifier.Global("$0"),
                GlobalType(ValueType.I32, false),
                Expression(
                    astNodeListOf(
                        NumericConstantInstruction.I32(IntegerLiteral.S32(0))
                    )
                )
            )
        ).inOrder()
        assertThat(newCounts.globals).isEqualTo(counts.globals + 1)
    }

    @Test
    fun parse_multipleExportsThenDescriptor() {
        val (result, newCounts) = tokenizer.tokenize(
            "(global $0 (export \"a\") (export \"b\") (mut i32) (i32.const 0))",
            context
        ).parseInlineGlobalExport(0, counts) ?: Assertions.fail("Expected a result")
        assertThat(result.parseLength).isEqualTo(20)
        assertThat(result.astNode).containsExactly(
            Export(
                "a",
                ExportDescriptor.Global(
                    Index.ByInt(0) as Index<Identifier.Global>
                )
            ),
            Export(
                "b",
                ExportDescriptor.Global(
                    Index.ByInt(0) as Index<Identifier.Global>
                )
            ),
            Global(
                Identifier.Global("$0"),
                GlobalType(ValueType.I32, true),
                Expression(
                    astNodeListOf(
                        NumericConstantInstruction.I32(IntegerLiteral.S32(0))
                    )
                )
            )
        ).inOrder()
        assertThat(newCounts.globals).isEqualTo(counts.globals + 1)
    }
}
