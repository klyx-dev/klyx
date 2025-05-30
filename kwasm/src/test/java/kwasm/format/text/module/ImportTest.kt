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
import kwasm.ast.module.ImportDescriptor
import kwasm.ast.module.TypeUse
import kwasm.ast.type.ElementType
import kwasm.ast.type.GlobalType
import kwasm.ast.type.Limits
import kwasm.ast.type.MemoryType
import kwasm.ast.type.Param
import kwasm.ast.type.Result
import kwasm.ast.type.TableType
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

@Suppress("EXPERIMENTAL_UNSIGNED_LITERALS")
@RunWith(JUnit4::class)
class ImportTest {
    private val counts = TextModuleCounts(0, 0, 0, 0, 0)
    private val tokenizer = Tokenizer()
    private val context = ParseContext("Import.wast")

    @Test
    fun parseFuncImportDescriptor() {
        val (result, newCounts) = tokenizer.tokenize("(func $0 (param i32) (result i32))")
            .parseImportDescriptor(0, counts) ?: fail("Expected a result")

        assertThat(result.parseLength).isEqualTo(12)
        assertThat(result.astNode.id).isEqualTo(Identifier.Function("$0"))
        val descriptor = result.astNode as ImportDescriptor.Function
        assertThat(descriptor.typeUse).isEqualTo(
            TypeUse(
                null,
                astNodeListOf(
                    Param(
                        Identifier.Local(null, null),
                        ValueType.I32
                    )
                ),
                astNodeListOf(
                    Result(ValueType.I32)
                )
            )
        )
        assertThat(newCounts.functions).isEqualTo(counts.functions + 1)
    }

    @Test
    fun parseFuncImportDescriptor_returnsNull_ifOpeningParenNotFound() {
        val result = tokenizer.tokenize("func $0 (param i32) (result i32))")
            .parseFuncImportDescriptor(0, counts)
        assertThat(result).isNull()
    }

    @Test
    fun parseFuncImportDescriptor_returnsNull_ifFuncNotFound() {
        val result = tokenizer.tokenize("(nonfunc $0 (param i32) (result i32))")
            .parseFuncImportDescriptor(0, counts)
        assertThat(result).isNull()
    }

    @Test
    fun parseFuncImportDescriptor_throws_ifClosingParenNotFound() {
        assertThrows(ParseException::class.java) {
            tokenizer.tokenize("(func $0 (param i32) (result i32)")
                .parseImportDescriptor(0, counts)
        }
    }

    @Test
    fun parseTableImportDescriptor() {
        val (result, newCounts) = tokenizer.tokenize("(table $0 0 10 funcref)")
            .parseImportDescriptor(0, counts) ?: fail("Expected a result")

        assertThat(result.parseLength).isEqualTo(7)
        assertThat(result.astNode.id).isEqualTo(Identifier.Table("$0"))
        val descriptor = result.astNode as ImportDescriptor.Table
        assertThat(descriptor.tableType).isEqualTo(
            TableType(
                Limits(0, 10),
                ElementType.FunctionReference
            )
        )
        assertThat(newCounts.tables).isEqualTo(counts.tables + 1)
    }

    @Test
    fun parseTableImportDescriptor_returnsNull_ifOpeningParenNotFound() {
        val result = tokenizer.tokenize("table $0 0 10 funcref)")
            .parseTableImportDescriptor(0, counts)
        assertThat(result).isNull()
    }

    @Test
    fun parseTableImportDescriptor_returnsNull_ifTableNotFound() {
        val result = tokenizer.tokenize("(nontable $0 0 10 funcref)")
            .parseTableImportDescriptor(0, counts)
        assertThat(result).isNull()
    }

    @Test
    fun parseTableImportDescriptor_throws_ifClosingParenNotFound() {
        assertThrows(ParseException::class.java) {
            tokenizer.tokenize("(table $0 0 10 funcref")
                .parseImportDescriptor(0, counts)
        }
    }

    @Test
    fun parseMemoryImportDescriptor() {
        val (result, newCounts) = tokenizer.tokenize("(memory $0 0 10)")
            .parseImportDescriptor(0, counts) ?: fail("Expected a result")

        assertThat(result.parseLength).isEqualTo(6)
        assertThat(result.astNode.id).isEqualTo(Identifier.Memory("$0"))
        val descriptor = result.astNode as ImportDescriptor.Memory
        assertThat(descriptor.memoryType).isEqualTo(
            MemoryType(
                Limits(0, 10)
            )
        )
        assertThat(newCounts.memories).isEqualTo(counts.memories + 1)
    }

    @Test
    fun parseMemoryImportDescriptor_returnsNull_ifOpeningParenNotFound() {
        val result = tokenizer.tokenize("memory $0 0 10)")
            .parseMemoryImportDescriptor(0, counts)
        assertThat(result).isNull()
    }

    @Test
    fun parseMemoryImportDescriptor_returnsNull_ifMemoryNotFound() {
        val result = tokenizer.tokenize("(nonmemory $0 0)")
            .parseMemoryImportDescriptor(0, counts)
        assertThat(result).isNull()
    }

    @Test
    fun parseMemoryImportDescriptor_throws_ifClosingParenNotFound() {
        assertThrows(ParseException::class.java) {
            tokenizer.tokenize("(memory $0 0 10")
                .parseImportDescriptor(0, counts)
        }
    }

    @Test
    fun parseGlobalImportDescriptor() {
        val (result, newCounts) = tokenizer.tokenize("(global $0 (mut i32))")
            .parseImportDescriptor(0, counts) ?: fail("Expected a result")

        assertThat(result.parseLength).isEqualTo(8)
        assertThat(result.astNode.id).isEqualTo(Identifier.Global("$0"))
        val descriptor = result.astNode as ImportDescriptor.Global
        assertThat(descriptor.globalType).isEqualTo(
            GlobalType(
                ValueType.I32,
                true
            )
        )
        assertThat(newCounts.globals).isEqualTo(counts.globals + 1)
    }

    @Test
    fun parseGlobalImportDescriptor_returnsNull_ifOpeningParenNotFound() {
        val result = tokenizer.tokenize("global $0 (mut i32))")
            .parseGlobalImportDescriptor(0, counts)
        assertThat(result).isNull()
    }

    @Test
    fun parseGlobalImportDescriptor_returnsNull_ifGlobalNotFound() {
        val result = tokenizer.tokenize("(nonglobal $0 i32)")
            .parseGlobalImportDescriptor(0, counts)
        assertThat(result).isNull()
    }

    @Test
    fun parseGlobalImportDescriptor_throws_ifClosingParenNotFound() {
        assertThrows(ParseException::class.java) {
            tokenizer.tokenize("(global $0 i32")
                .parseImportDescriptor(0, counts)
        }
    }

    @Test
    fun parseImport() {
        val (result, newCounts) = tokenizer.tokenize("(import \"module\" \"name\" (global i32))")
            .parseImport(0, counts) ?: fail("Expected a result")
        assertThat(result.parseLength).isEqualTo(9)
        assertThat(result.astNode.moduleName).isEqualTo("module")
        assertThat(result.astNode.name).isEqualTo("name")
        assertThat(result.astNode.descriptor as? ImportDescriptor.Global).isNotNull()
        assertThat(newCounts.globals).isEqualTo(counts.globals + 1)
    }

    @Test
    fun parseImport_returnsNull_ifOpeningParenNotFound() {
        val result = tokenizer.tokenize("import \"module\" \"name\" (global i32))")
            .parseImport(0, counts)
        assertThat(result).isNull()
    }

    @Test
    fun parseImport_returnsNull_ifImportIsNotFound() {
        val result = tokenizer.tokenize("(nonimport \"module\" \"name\" (global i32))")
            .parseImport(0, counts)
        assertThat(result).isNull()
    }

    @Test
    fun parseImport_throwsIfModuleNameOrImportName_notFound() {
        assertThrows(ParseException::class.java) {
            tokenizer.tokenize("(import \"module\" (global i32))")
                .parseImport(0, counts)
        }
        assertThrows(ParseException::class.java) {
            tokenizer.tokenize("(import \"name\" (global i32))")
                .parseImport(0, counts)
        }
        assertThrows(ParseException::class.java) {
            tokenizer.tokenize("(import (global i32))")
                .parseImport(0, counts)
        }
    }

    @Test
    fun parseImport_throwsIfDescriptorNotFound() {
        var e = assertThrows(ParseException::class.java) {
            tokenizer.tokenize("(import \"module\" \"name\")")
                .parseImport(0, counts)
        }
        assertThat(e).hasMessageThat().contains("Expected import descriptor")
        e = assertThrows(ParseException::class.java) {
            tokenizer.tokenize("(import \"module\" \"name\" i32)")
                .parseImport(0, counts)
        }
        assertThat(e).hasMessageThat().contains("Expected import descriptor")
    }

    @Test
    fun parseImport_throwsIfClosingParen_notFound() {
        var e = assertThrows(ParseException::class.java) {
            tokenizer.tokenize("(import \"module\" \"name\" (global i32)")
                .parseImport(0, counts)
        }
        assertThat(e).hasMessageThat().contains("Expected ')'")
    }
}
