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

package kwasm.format.text.instruction

import com.google.common.truth.Truth.assertThat
import kwasm.ast.instruction.MemArg
import kwasm.format.ParseContext
import kwasm.format.ParseException
import kwasm.format.text.Tokenizer
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class MemArgTest {
    private val tokenizer = Tokenizer()
    private val context = ParseContext("MemArgTest.wat")

    @Test
    fun parse_parsesDefault_whenEmpty() {
        val result = tokenizer.tokenize("", context).parseMemarg(0, 4)
        assertThat(result.parseLength).isEqualTo(0)
        assertThat(result.astNode).isEqualTo(MemArg.FOUR)
    }

    @Test
    fun parse_parsesDefault_whenFromToken_isNotKeyword() {
        val result = tokenizer.tokenize("(", context).parseMemarg(0, 4)
        assertThat(result.parseLength).isEqualTo(0)
        assertThat(result.astNode).isEqualTo(MemArg.FOUR)
    }

    @Test
    fun parse_parsesDefault_whenFromToken_doesNotStartWithAlignOffset() {
        val result = tokenizer.tokenize("call", context).parseMemarg(0, 4)
        assertThat(result.parseLength).isEqualTo(0)
        assertThat(result.astNode).isEqualTo(MemArg.FOUR)
    }

    @Test
    fun parse_offsetOnly() {
        var result = tokenizer.tokenize("offset=50", context).parseMemarg(0, 4)
        assertThat(result.parseLength).isEqualTo(1)
        assertThat(result.astNode).isEqualTo(MemArg(50, 4))

        result = tokenizer.tokenize("offset=50 call", context).parseMemarg(0, 4)
        assertThat(result.parseLength).isEqualTo(1)
        assertThat(result.astNode).isEqualTo(MemArg(50, 4))
    }

    @Test
    fun parse_alignOnly() {
        val result = tokenizer.tokenize("align=16", context).parseMemarg(0, 4)
        assertThat(result.parseLength).isEqualTo(1)
        assertThat(result.astNode).isEqualTo(MemArg(0, 16))
    }

    @Test
    fun parse_offsetAndAlign() {
        val result = tokenizer.tokenize("offset=50 align=16", context).parseMemarg(0, 4)
        assertThat(result.parseLength).isEqualTo(2)
        assertThat(result.astNode).isEqualTo(
            MemArg(
                50,
                16
            )
        )
    }

    @Test
    fun throws_whenAlignIsIllegal() {
        var e = assertThrows(ParseException::class.java) {
            tokenizer.tokenize("offset=50 align=5", context).parseMemarg(0, 4)
        }
        assertThat(e).hasMessageThat().contains("Illegal MemArg value for N=4")

        e = assertThrows(ParseException::class.java) {
            tokenizer.tokenize("align=35", context).parseMemarg(0, 4)
        }
        assertThat(e).hasMessageThat().contains("Illegal MemArg value for N=4")
    }
}
