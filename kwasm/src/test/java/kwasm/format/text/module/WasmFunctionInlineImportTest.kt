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
import kwasm.ast.module.Import
import kwasm.ast.module.ImportDescriptor
import kwasm.ast.module.TypeUse
import kwasm.ast.type.Param
import kwasm.ast.type.Result
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

@RunWith(JUnit4::class)
class WasmFunctionInlineImportTest {
    private val counts = TextModuleCounts(0, 0, 0, 0, 0)
    private val tokenizer = Tokenizer()
    private val context =
        ParseContext("WasmFunctionInlineImportTest.wat")

    @Test
    fun parse_returnsNullIf_openingParenNotFound() {
        val result = tokenizer.tokenize("func $0 (import \"a\" \"b\") (param i32))", context)
            .parseInlineWasmFunctionImport(0, counts)
        assertThat(result).isNull()
    }

    @Test
    fun parse_returnsNullIf_funcKeywordNotFound() {
        val result = tokenizer.tokenize("(nonfunc $0 (import \"a\" \"b\") (param i32))", context)
            .parseInlineWasmFunctionImport(0, counts)
        assertThat(result).isNull()
    }

    @Test
    fun parse_returnsNullIf_importOpeningParenNotFound() {
        val result = tokenizer.tokenize("(func $0 import \"a\" \"b\") (param i32))", context)
            .parseInlineWasmFunctionImport(0, counts)
        assertThat(result).isNull()
    }

    @Test
    fun parse_returnsNullIf_importKeywordNotFound() {
        val result = tokenizer.tokenize("(func $0 (nonimport \"a\" \"b\") (param i32))", context)
            .parseInlineWasmFunctionImport(0, counts)
        assertThat(result).isNull()
    }

    @Test
    fun parse_throwsIf_moduleNameNotFound() {
        assertThrows(ParseException::class.java) {
            tokenizer.tokenize("(func $0 (import) (param i32))", context)
                .parseInlineWasmFunctionImport(0, counts)
        }
    }

    @Test
    fun parse_throwsIf_funcNameNotFound() {
        assertThrows(ParseException::class.java) {
            tokenizer.tokenize("(func $0 (import \"a\") (param i32))", context)
                .parseInlineWasmFunctionImport(0, counts)
        }
    }

    @Test
    fun parse_throwsIf_importNotClosed() {
        val e = assertThrows(ParseException::class.java) {
            tokenizer.tokenize("(func $0 (import \"a\" \"b\" (param i32))", context)
                .parseInlineWasmFunctionImport(0, counts)
        }
        assertThat(e).hasMessageThat().contains("Expected ')'")
    }

    @Test
    fun parse_throwsIf_notClosed() {
        val e = assertThrows(ParseException::class.java) {
            tokenizer.tokenize("(func $0 (import \"a\" \"b\") (param i32)", context)
                .parseInlineWasmFunctionImport(0, counts)
        }
        assertThat(e).hasMessageThat().contains("Expected ')'")
    }

    @Test
    fun parse_minimal() {
        val (result, newCounts) = tokenizer.tokenize("(func (import \"a\" \"b\"))", context)
            .parseInlineWasmFunctionImport(0, counts) ?: fail("Expected a result")

        assertThat(result.parseLength).isEqualTo(8)
        assertThat(result.astNode.moduleName).isEqualTo("a")
        assertThat(result.astNode.name).isEqualTo("b")
        val descriptor = result.astNode.descriptor as ImportDescriptor.Function
        assertThat(descriptor.id.stringRepr).isNull()
        assertThat(descriptor.id.unique).isEqualTo(0)
        assertThat(descriptor.typeUse).isEqualTo(
            TypeUse(
                null,
                astNodeListOf(),
                astNodeListOf()
            )
        )
        assertThat(newCounts.functions).isEqualTo(counts.functions + 1)
    }

    @Test
    fun parse() {
        val (result, newCounts) = tokenizer.tokenize(
            "(func $0 (import \"a\" \"b\") (param i32) (result i32))",
            context
        ).parseInlineWasmFunctionImport(0, counts) ?: fail("Expected a result")

        assertThat(result.parseLength).isEqualTo(17)
        assertThat(result.astNode).isEqualTo(
            Import(
                "a",
                "b",
                ImportDescriptor.Function(
                    Identifier.Function("$0"),
                    TypeUse(
                        null,
                        astNodeListOf(
                            Param(
                                Identifier.Local(
                                    null,
                                    null
                                ),
                                ValueType.I32
                            )
                        ),
                        astNodeListOf(Result(ValueType.I32))
                    )
                )
            )
        )
        assertThat(newCounts.functions).isEqualTo(counts.functions + 1)
    }
}
