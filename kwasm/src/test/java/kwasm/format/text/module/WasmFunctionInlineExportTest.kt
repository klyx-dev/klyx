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
import kwasm.ast.astNodeListOf
import kwasm.ast.instruction.NumericInstruction
import kwasm.ast.module.Export
import kwasm.ast.module.ExportDescriptor
import kwasm.ast.module.Import
import kwasm.ast.module.ImportDescriptor
import kwasm.ast.module.Index
import kwasm.ast.module.Local
import kwasm.ast.module.TypeUse
import kwasm.ast.module.WasmFunction
import kwasm.ast.type.Param
import kwasm.ast.type.ValueType
import kwasm.format.ParseContext
import kwasm.format.ParseException
import kwasm.format.text.TextModuleCounts
import kwasm.format.text.Tokenizer
import org.assertj.core.api.Assertions.fail
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@Suppress("UNCHECKED_CAST")
@RunWith(JUnit4::class)
class WasmFunctionInlineExportTest {
    private val counts = TextModuleCounts(0, 0, 0, 0, 0)
    private val tokenizer = Tokenizer()
    private val context = ParseContext("WasmFunctionInlineExportTest.wat")

    @Test
    fun parse_returnsNullIf_openingParenNotFound() {
        val result = tokenizer.tokenize("func $0 (export \"a\"))", context)
            .parseInlineWasmFunctionExport(0, counts)
        assertThat(result).isNull()
    }

    @Test
    fun parse_returnsNullIf_funcKeywordNotFound() {
        val result = tokenizer.tokenize("(nonfunc $0 (export \"a\"))", context)
            .parseInlineWasmFunctionExport(0, counts)
        assertThat(result).isNull()
    }

    @Test
    fun parse_returnsNullIf_exportOpeningParenNotFound() {
        val result = tokenizer.tokenize("(func $0 export \"a\"))", context)
            .parseInlineWasmFunctionExport(0, counts)
        assertThat(result).isNull()
    }

    @Test
    fun parse_returnsNullIf_exportKeywordNotFound() {
        val result = tokenizer.tokenize("(func $0 (nonexport \"a\"))", context)
            .parseInlineWasmFunctionExport(0, counts)
        assertThat(result).isNull()
    }

    @Test
    fun parse_throwsIf_exportNameNotFound() {
        assertThrows(ParseException::class.java) {
            tokenizer.tokenize("(func $0 (export))", context)
                .parseInlineWasmFunctionExport(0, counts)
        }
    }

    @Test
    fun parse_throwsIf_exportClosureNotFound() {
        val e = assertThrows(ParseException::class.java) {
            tokenizer.tokenize("(func $0 (export \"a\"", context)
                .parseInlineWasmFunctionExport(0, counts)
        }
        assertThat(e).hasMessageThat().contains("Expected ')'")
    }

    @Test
    fun parse_minimal() {
        val (result, newCounts) = tokenizer.tokenize("(func (export \"a\"))", context)
            .parseInlineWasmFunctionExport(0, counts) ?: fail("Expected a result")
        assertThat(result.parseLength).isEqualTo(7)
        val func = result.astNode[0] as WasmFunction
        val export = result.astNode[1] as Export

        assertThat(func.typeUse).isEqualTo(
            TypeUse(
                null,
                astNodeListOf(),
                astNodeListOf()
            )
        )
        assertThat(func.locals).isEmpty()
        assertThat(func.instructions).isEmpty()
        assertThat(export.name).isEqualTo("a")
        assertThat(export.descriptor.index)
            .isEqualTo(Index.ByInt(0) as Index<Identifier.Function>)
        assertThat(newCounts.functions).isEqualTo(counts.functions + 1)
    }

    @Test
    fun parse_simple() {
        val (result, newCounts) = tokenizer.tokenize("(func $0 (export \"a\"))", context)
            .parseInlineWasmFunctionExport(0, counts) ?: fail("Expected a result")
        assertThat(result.parseLength).isEqualTo(8)
        assertThat(result.astNode).containsExactly(
            WasmFunction(
                Identifier.Function("$0"),
                TypeUse(
                    null,
                    astNodeListOf(),
                    astNodeListOf()
                ),
                astNodeListOf(),
                astNodeListOf()
            ),
            Export(
                "a",
                ExportDescriptor.Function(
                    Index.ByInt(0) as Index<Identifier.Function>
                )
            )
        ).inOrder()
        assertThat(newCounts.functions).isEqualTo(counts.functions + 1)
    }

    @Test
    fun parse_multipleExports() {
        val (result, newCounts) = tokenizer.tokenize(
            "(func $0 (export \"a\") (export \"b\") (export \"c\"))",
            context
        ).parseInlineWasmFunctionExport(0, counts) ?: fail("Expected a result")
        assertThat(result.parseLength).isEqualTo(16)
        assertThat(result.astNode).containsExactly(
            WasmFunction(
                Identifier.Function("$0"),
                TypeUse(
                    null,
                    astNodeListOf(),
                    astNodeListOf()
                ),
                astNodeListOf(),
                astNodeListOf()
            ),
            Export(
                "a",
                ExportDescriptor.Function(
                    Index.ByInt(0) as Index<Identifier.Function>
                )
            ),
            Export(
                "b",
                ExportDescriptor.Function(
                    Index.ByInt(0) as Index<Identifier.Function>
                )
            ),
            Export(
                "c",
                ExportDescriptor.Function(
                    Index.ByInt(0) as Index<Identifier.Function>
                )
            )
        )
        assertThat(newCounts.functions).isEqualTo(counts.functions + 1)
    }

    @Test
    fun parse_exportThenImport() {
        val (result, newCounts) = tokenizer.tokenize(
            "(func $0 (export \"a\") (import \"b\" \"c\") (param i32))",
            context
        ).parseInlineWasmFunctionExport(0, counts) ?: fail("Expected a result")
        assertThat(result.parseLength).isEqualTo(17)
        assertThat(result.astNode).containsExactly(
            Export(
                "a",
                ExportDescriptor.Function(
                    Index.ByInt(0) as Index<Identifier.Function>
                )
            ),
            Import(
                "b",
                "c",
                ImportDescriptor.Function(
                    Identifier.Function("$0"),
                    TypeUse(
                        null,
                        astNodeListOf(
                            Param(
                                Identifier.Local(null, null),
                                ValueType.I32
                            )
                        ),
                        astNodeListOf()
                    )
                )
            )
        ).inOrder()
        assertThat(newCounts.functions).isEqualTo(counts.functions + 1)
    }

    @Test
    fun parse_exportThenBody() {
        val (result, newCounts) = tokenizer.tokenize(
            "(func $0 (export \"a\") (local i32) (i32.add))",
            context
        ).parseInlineWasmFunctionExport(0, counts) ?: fail("Expected a result")
        assertThat(result.parseLength).isEqualTo(15)
        assertThat(result.astNode).containsExactly(
            Export(
                "a",
                ExportDescriptor.Function(
                    Index.ByInt(0) as Index<Identifier.Function>
                )
            ),
            WasmFunction(
                Identifier.Function("$0"),
                TypeUse(null, astNodeListOf(), astNodeListOf()),
                astNodeListOf(
                    Local(null, ValueType.I32)
                ),
                astNodeListOf(
                    NumericInstruction.I32Add
                )
            )
        ).inOrder()
        assertThat(newCounts.functions).isEqualTo(counts.functions + 1)
    }

    @Test
    fun parse_multipleExportsThenBody() {
        val (result, newCounts) = tokenizer.tokenize(
            "(func $0 (export \"a\") (export \"b\") (local i32) (i32.add))",
            context
        ).parseInlineWasmFunctionExport(0, counts) ?: fail("Expected a result")
        assertThat(result.parseLength).isEqualTo(19)
        assertThat(result.astNode).containsExactly(
            Export(
                "a",
                ExportDescriptor.Function(
                    Index.ByInt(0) as Index<Identifier.Function>
                )
            ),
            Export(
                "b",
                ExportDescriptor.Function(
                    Index.ByInt(0) as Index<Identifier.Function>
                )
            ),
            WasmFunction(
                Identifier.Function("$0"),
                TypeUse(null, astNodeListOf(), astNodeListOf()),
                astNodeListOf(
                    Local(null, ValueType.I32)
                ),
                astNodeListOf(
                    NumericInstruction.I32Add
                )
            )
        ).inOrder()
        assertThat(newCounts.functions).isEqualTo(counts.functions + 1)
    }
}
