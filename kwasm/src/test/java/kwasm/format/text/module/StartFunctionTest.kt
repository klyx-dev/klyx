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
import kwasm.ast.module.Index
import kwasm.ast.module.StartFunction
import kwasm.format.ParseException
import kwasm.format.text.TextModuleCounts
import kwasm.format.text.Tokenizer
import org.assertj.core.api.Assertions.fail
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class StartFunctionTest {
    private val counts = TextModuleCounts(0, 0, 0, 0, 0)
    private val tokenizer = Tokenizer()

    @Test
    fun parse_returnsNull_ifDoesntStartWithOpenParen() {
        val result = tokenizer.tokenize("start $0").parseStartFunction(0, counts)
        assertThat(result).isNull()
    }

    @Test
    fun parse_returnsNull_ifKeywordIsNotStart() {
        val result = tokenizer.tokenize("(end $0)").parseStartFunction(0, counts)
        assertThat(result).isNull()
    }

    @Test
    fun throws_ifFunctionIndex_notFound() {
        assertThrows(ParseException::class.java) {
            tokenizer.tokenize("(start )").parseStartFunction(0, counts)
        }
    }

    @Test
    fun throws_ifClosingParen_notFoundAfterFunctionIndex() {
        val e = assertThrows(ParseException::class.java) {
            tokenizer.tokenize("(start $0").parseStartFunction(0, counts)
        }

        assertThat(e).hasMessageThat().contains("Expected ')'")
    }

    @Test
    fun parses_startFunction() {
        val (result, newCounts) = tokenizer.tokenize("(start $0)")
            .parseStartFunction(0, counts) ?: fail("Expected a result")

        assertThat(result.parseLength).isEqualTo(4)
        assertThat(result.astNode)
            .isEqualTo(
                StartFunction(
                    Index.ByIdentifier(
                        Identifier.Function("$0")
                    )
                )
            )
        assertThat(newCounts).isEqualTo(counts)
    }

    @Suppress("EXPERIMENTAL_UNSIGNED_LITERALS", "UNCHECKED_CAST")
    @Test
    fun parses_startFunction_withIntIndex() {
        val (result, newCounts) = tokenizer.tokenize("(start 123)")
            .parseStartFunction(0, counts) ?: fail("Expected a result")

        assertThat(result.parseLength).isEqualTo(4)
        assertThat(result.astNode)
            .isEqualTo(StartFunction(Index.ByInt(123) as Index<Identifier.Function>))
        assertThat(newCounts).isEqualTo(counts)
    }
}
