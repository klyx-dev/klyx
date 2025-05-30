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
import kwasm.ast.module.Import
import kwasm.ast.module.ImportDescriptor
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

@Suppress("EXPERIMENTAL_API_USAGE", "EXPERIMENTAL_UNSIGNED_LITERALS")
@RunWith(JUnit4::class)
class MemoryInlineImportTest {
    private val counts = TextModuleCounts(0, 0, 0, 0, 0)
    private val tokenizer = Tokenizer()
    private val context = ParseContext("MemoryInlineImportTest.wat")

    @Test
    fun parse_returnsNullIf_openingParenNotFound() {
        val result = tokenizer.tokenize("memory $0 (import \"a\" \"b\") 0 1)", context)
            .parseInlineMemoryImport(0, counts)
        assertThat(result).isNull()
    }

    @Test
    fun parse_returnsNullIf_memoryKeywordNotFound() {
        val result = tokenizer.tokenize("(nonmemory $0 (import \"a\" \"b\") 0 1)", context)
            .parseInlineMemoryImport(0, counts)
        assertThat(result).isNull()
    }

    @Test
    fun parse_returnsNullIf_importOpeningParenNotFound() {
        val result = tokenizer.tokenize("(memory $0 import \"a\" \"b\") 0 1)", context)
            .parseInlineMemoryImport(0, counts)
        assertThat(result).isNull()
    }

    @Test
    fun parse_returnsNullIf_importKeywordNotFound() {
        val result = tokenizer.tokenize("(memory $0 (nonimport \"a\" \"b\") 0 1)", context)
            .parseInlineMemoryImport(0, counts)
        assertThat(result).isNull()
    }

    @Test
    fun parse_throwsIf_moduleNameNotFound() {
        assertThrows(ParseException::class.java) {
            tokenizer.tokenize("(memory $0 (import) 0 1)", context)
                .parseInlineMemoryImport(0, counts)
        }
    }

    @Test
    fun parse_throwsIf_memoryNameNotFound() {
        assertThrows(ParseException::class.java) {
            tokenizer.tokenize("(memory $0 (import \"a\") 0 1)", context)
                .parseInlineMemoryImport(0, counts)
        }
    }

    @Test
    fun parse_throwsIf_importNotClosed() {
        val e = assertThrows(ParseException::class.java) {
            tokenizer.tokenize("(memory $0 (import \"a\" \"b\" 0 1)", context)
                .parseInlineMemoryImport(0, counts)
        }
        assertThat(e).hasMessageThat().contains("Expected ')'")
    }

    @Test
    fun parse_throwsIf_notClosed() {
        val e = assertThrows(ParseException::class.java) {
            tokenizer.tokenize("(memory $0 (import \"a\" \"b\") 0 1", context)
                .parseInlineMemoryImport(0, counts)
        }
        assertThat(e).hasMessageThat().contains("Expected ')'")
    }

    @Test
    fun parse_minimal() {
        val (result, newCounts) = tokenizer.tokenize("(memory (import \"a\" \"b\") 1)", context)
            .parseInlineMemoryImport(0, counts) ?: Assertions.fail("Expected a result")

        assertThat(result.parseLength).isEqualTo(9)
        assertThat(result.astNode.moduleName).isEqualTo("a")
        assertThat(result.astNode.name).isEqualTo("b")
        val descriptor = result.astNode.descriptor as ImportDescriptor.Memory
        assertThat(descriptor.memoryType).isEqualTo(MemoryType(Limits(1)))
        assertThat(newCounts.memories).isEqualTo(counts.memories + 1)
    }

    @Test
    fun parse() {
        val (result, newCounts) = tokenizer.tokenize(
            "(memory $0 (import \"a\" \"b\") 0 1)",
            context
        ).parseInlineMemoryImport(0, counts) ?: Assertions.fail("Expected a result")

        assertThat(result.parseLength).isEqualTo(11)
        assertThat(result.astNode).isEqualTo(
            Import(
                "a",
                "b",
                ImportDescriptor.Memory(
                    Identifier.Memory("$0"),
                    MemoryType(
                        Limits(0, 1)
                    )
                )
            )
        )
        assertThat(newCounts.memories).isEqualTo(counts.memories + 1)
    }
}
