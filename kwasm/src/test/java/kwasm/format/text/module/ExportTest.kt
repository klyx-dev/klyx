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
import kwasm.ast.module.Export
import kwasm.ast.module.ExportDescriptor
import kwasm.ast.module.Index
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
class ExportTest {
    private val counts = TextModuleCounts(0, 0, 0, 0, 0)
    private val tokenizer = Tokenizer()
    private val context = ParseContext("ExportTest.wast")

    @Test
    fun parseExportDescriptor_throws_whenOpenParenNotPresent() {
        val e = assertThrows(ParseException::class.java) {
            tokenizer.tokenize("global $0))", context)
                .parseExportDescriptor(0)
        }
        assertThat(e).hasMessageThat().contains("Expected '('")
    }

    @Test
    fun parseExportDescriptor_throws_whenElementAfterOpenParen_isNotKeyword() {
        val e = assertThrows(ParseException::class.java) {
            tokenizer.tokenize("(\"name\" $0)", context)
                .parseExportDescriptor(0)
        }
        assertThat(e).hasMessageThat().contains("Expected 'func', 'table', 'memory', or 'global'")
    }

    @Test
    fun parseExportDescriptor_throws_whenKeyword_isInvalid() {
        val e = assertThrows(ParseException::class.java) {
            tokenizer.tokenize("(type $0)", context)
                .parseExportDescriptor(0)
        }
        assertThat(e).hasMessageThat().contains("Expected 'func', 'table', 'memory', or 'global'")
    }

    @Test
    fun parseExportDescriptor_throws_whenClosingParenIsNotPresent() {
        val e = assertThrows(ParseException::class.java) {
            tokenizer.tokenize("(global $0", context)
                .parseExportDescriptor(0)
        }
        assertThat(e).hasMessageThat().contains("Expected ')'")
    }

    @Test
    fun parseExportDescriptor_parsesFunc() {
        val result = tokenizer.tokenize("(func $0)").parseExportDescriptor(0)
        assertThat(result.parseLength).isEqualTo(4)
        assertThat(result.astNode).isEqualTo(
            ExportDescriptor.Function(Index.ByIdentifier(Identifier.Function("$0")))
        )
    }

    @Test
    fun parseExportDescriptor_parsesTable() {
        val result = tokenizer.tokenize("(table $0)").parseExportDescriptor(0)
        assertThat(result.parseLength).isEqualTo(4)
        assertThat(result.astNode).isEqualTo(
            ExportDescriptor.Table(Index.ByIdentifier(Identifier.Table("$0")))
        )
    }

    @Test
    fun parseExportDescriptor_parsesMemory() {
        val result = tokenizer.tokenize("(memory $0)").parseExportDescriptor(0)
        assertThat(result.parseLength).isEqualTo(4)
        assertThat(result.astNode).isEqualTo(
            ExportDescriptor.Memory(Index.ByIdentifier(Identifier.Memory("$0")))
        )
    }

    @Test
    fun parseExportDescriptor_parsesGlobal() {
        val result = tokenizer.tokenize("(global $0)").parseExportDescriptor(0)
        assertThat(result.parseLength).isEqualTo(4)
        assertThat(result.astNode).isEqualTo(
            ExportDescriptor.Global(Index.ByIdentifier(Identifier.Global("$0")))
        )
    }

    @Test
    fun parseExport_returnsNullIfOpeningParen_isMissing() {
        val result = tokenizer.tokenize("export \"name\" (global $0))", context)
            .parseExport(0, counts)
        assertThat(result).isNull()
    }

    @Test
    fun parseExport_returnsNullIf_exportNotPresent() {
        val result = tokenizer.tokenize("(\"name\" (global $0))", context)
            .parseExport(0, counts)
        assertThat(result).isNull()
    }

    @Test
    fun parseExport_throws_ifNameIsNotPresent() {
        assertThrows(ParseException::class.java) {
            tokenizer.tokenize("(export (global $0))", context)
                .parseExport(0, counts)
        }
    }

    @Test
    fun parseExport_throws_ifDescriptorIsNotPresent() {
        assertThrows(ParseException::class.java) {
            tokenizer.tokenize("(export \"name\")", context)
                .parseExport(0, counts)
        }
    }

    @Test
    fun parseExport_throws_ifClosingParen_isNotPresent() {
        val e = assertThrows(ParseException::class.java) {
            tokenizer.tokenize("(export \"name\" (global $0)", context)
                .parseExport(0, counts)
        }
        assertThat(e).hasMessageThat().contains("Expected ')'")
    }

    @Test
    fun parseExport() {
        val (result, newCounts) = tokenizer.tokenize("(export \"name\" (global $0))", context)
            .parseExport(0, counts) ?: fail("Expected a result")
        assertThat(result.parseLength).isEqualTo(8)
        assertThat(result.astNode).isEqualTo(
            Export(
                "name",
                ExportDescriptor.Global(
                    Index.ByIdentifier(Identifier.Global("$0"))
                )
            )
        )
        assertThat(newCounts).isEqualTo(counts)
    }
}
