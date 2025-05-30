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
import kwasm.ast.module.ElementSegment
import kwasm.ast.module.Export
import kwasm.ast.module.ExportDescriptor
import kwasm.ast.module.Import
import kwasm.ast.module.ImportDescriptor
import kwasm.ast.module.Index
import kwasm.ast.module.Offset
import kwasm.ast.module.Table
import kwasm.ast.type.ElementType
import kwasm.ast.type.Limits
import kwasm.ast.type.TableType
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
class TableInlineExportTest {
    private val counts = TextModuleCounts(0, 0, 0, 0, 0)
    private val tokenizer = Tokenizer()
    private val context = ParseContext("TableInlineExportTest.wat")

    @Test
    fun parse_returnsNullIf_openingParenNotFound() {
        val result = tokenizer.tokenize("table $0 (export \"a\"))", context)
            .parseInlineTableExport(0, counts)
        assertThat(result).isNull()
    }

    @Test
    fun parse_returnsNullIf_tableKeywordNotFound() {
        val result = tokenizer.tokenize("(nontable $0 (export \"a\"))", context)
            .parseInlineTableExport(0, counts)
        assertThat(result).isNull()
    }

    @Test
    fun parse_returnsNullIf_exportOpeningParenNotFound() {
        val result = tokenizer.tokenize("(table $0 export \"a\"))", context)
            .parseInlineTableExport(0, counts)
        assertThat(result).isNull()
    }

    @Test
    fun parse_returnsNullIf_exportKeywordNotFound() {
        val result = tokenizer.tokenize("(table $0 (nonexport \"a\"))", context)
            .parseInlineTableExport(0, counts)
        assertThat(result).isNull()
    }

    @Test
    fun parse_throwsIf_exportNameNotFound() {
        assertThrows(ParseException::class.java) {
            tokenizer.tokenize("(table $0 (export))", context)
                .parseInlineTableExport(0, counts)
        }
    }

    @Test
    fun parse_throwsIf_exportClosureNotFound() {
        val e = assertThrows(ParseException::class.java) {
            tokenizer.tokenize("(table $0 (export \"a\"", context)
                .parseInlineTableExport(0, counts)
        }
        assertThat(e).hasMessageThat().contains("Expected ')'")
    }

    @Test
    fun parse_minimal() {
        val (result, newCounts) = tokenizer.tokenize("(table (export \"a\"))", context)
            .parseInlineTableExport(0, counts) ?: Assertions.fail("Expected a result")
        assertThat(result.parseLength).isEqualTo(7)
        val export = result.astNode[0] as Export
        assertThat(export.descriptor as? ExportDescriptor.Table).isNotNull()
        assertThat(export.name).isEqualTo("a")
        assertThat(newCounts.tables).isEqualTo(counts.tables + 1)
    }

    @Test
    fun parse_simple() {
        val (result, newCounts) = tokenizer.tokenize("(table $0 (export \"a\"))", context)
            .parseInlineTableExport(0, counts) ?: Assertions.fail("Expected a result")
        assertThat(result.parseLength).isEqualTo(8)
        assertThat(result.astNode).containsExactly(
            Export(
                "a",
                ExportDescriptor.Table(
                    Index.ByInt(0) as Index<Identifier.Table>
                )
            )
        ).inOrder()
        assertThat(newCounts.tables).isEqualTo(counts.tables + 1)
    }

    @Test
    fun parse_multipleExports() {
        val (result, newCounts) = tokenizer.tokenize(
            "(table $0 (export \"a\") (export \"b\") (export \"c\"))",
            context
        ).parseInlineTableExport(0, counts) ?: Assertions.fail("Expected a result")
        assertThat(result.parseLength).isEqualTo(16)
        assertThat(result.astNode).containsExactly(
            Export(
                "a",
                ExportDescriptor.Table(
                    Index.ByInt(0) as Index<Identifier.Table>
                )
            ),
            Export(
                "b",
                ExportDescriptor.Table(
                    Index.ByInt(0) as Index<Identifier.Table>
                )
            ),
            Export(
                "c",
                ExportDescriptor.Table(
                    Index.ByInt(0) as Index<Identifier.Table>
                )
            )
        ).inOrder()
        assertThat(newCounts.tables).isEqualTo(counts.tables + 1)
    }

    @Test
    fun parse_exportThenImport() {
        val (result, newCounts) = tokenizer.tokenize(
            "(table $0 (export \"a\") (import \"b\" \"c\") 1 funcref)",
            context
        ).parseInlineTableExport(0, counts) ?: Assertions.fail("Expected a result")
        assertThat(result.parseLength).isEqualTo(15)
        assertThat(result.astNode).containsExactly(
            Export(
                "a",
                ExportDescriptor.Table(
                    Index.ByInt(0) as Index<Identifier.Table>
                )
            ),
            Import(
                "b",
                "c",
                ImportDescriptor.Table(
                    Identifier.Table("$0"),
                    TableType(
                        Limits(1),
                        ElementType.FunctionReference
                    )
                )
            )
        ).inOrder()
        assertThat(newCounts.tables).isEqualTo(counts.tables + 1)
    }

    @Test
    fun parse_exportThenInlineElements() {
        val (result, newCounts) = tokenizer.tokenize(
            "(table $0 (export \"a\") funcref (elem $1 $2))",
            context
        ).parseInlineTableExport(0, counts) ?: Assertions.fail("Expected a result")
        assertThat(result.parseLength).isEqualTo(14)
        assertThat(result.astNode).containsExactly(
            Export(
                "a",
                ExportDescriptor.Table(
                    Index.ByInt(0) as Index<Identifier.Table>
                )
            ),
            Table(
                Identifier.Table("$0"),
                TableType(
                    Limits(2, 2),
                    ElementType.FunctionReference
                )
            ),
            ElementSegment(
                Index.ByInt(0) as Index<Identifier.Table>,
                Offset(
                    Expression(
                        astNodeListOf(NumericConstantInstruction.I32(IntegerLiteral.S32(0)))
                    )
                ),
                astNodeListOf(
                    Index.ByIdentifier(Identifier.Function("$1")),
                    Index.ByIdentifier(Identifier.Function("$2"))
                )
            )
        ).inOrder()
        assertThat(newCounts.tables).isEqualTo(counts.tables + 1)
    }

    @Test
    fun parse_multipleExportsThenInlineElements() {
        val (result, newCounts) = tokenizer.tokenize(
            "(table $0 (export \"a\") (export \"b\") funcref (elem $1 $2))",
            context
        ).parseInlineTableExport(0, counts) ?: Assertions.fail("Expected a result")
        assertThat(result.parseLength).isEqualTo(18)
        assertThat(result.astNode).containsExactly(
            Export(
                "a",
                ExportDescriptor.Table(
                    Index.ByInt(0) as Index<Identifier.Table>
                )
            ),
            Export(
                "b",
                ExportDescriptor.Table(
                    Index.ByInt(0) as Index<Identifier.Table>
                )
            ),
            Table(
                Identifier.Table("$0"),
                TableType(
                    Limits(2, 2),
                    ElementType.FunctionReference
                )
            ),
            ElementSegment(
                Index.ByInt(0) as Index<Identifier.Table>,
                Offset(
                    Expression(
                        astNodeListOf(NumericConstantInstruction.I32(IntegerLiteral.S32(0)))
                    )
                ),
                astNodeListOf(
                    Index.ByIdentifier(Identifier.Function("$1")),
                    Index.ByIdentifier(Identifier.Function("$2"))
                )
            )
        ).inOrder()
        assertThat(newCounts.tables).isEqualTo(counts.tables + 1)
    }
}
