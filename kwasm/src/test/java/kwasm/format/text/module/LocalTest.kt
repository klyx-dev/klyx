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
import kwasm.ast.module.Local
import kwasm.ast.type.ValueType
import kwasm.format.ParseContext
import kwasm.format.ParseException
import kwasm.format.text.Tokenizer
import org.assertj.core.api.Assertions.fail
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class LocalTest {
    private val tokenizer = Tokenizer()
    private val context = ParseContext("LocalTest.wast")

    @Test
    fun parse_returnsNull_ifOpenParen_notFound() {
        val result = tokenizer.tokenize("local i64)", context).parseLocal(0)
        assertThat(result).isNull()
    }

    @Test
    fun parse_returnsNull_ifLocalKeyword_notFound() {
        val result = tokenizer.tokenize("(i64)", context).parseLocal(0)
        assertThat(result).isNull()
    }

    @Test
    fun throws_ifIdProvided_andMoreThanOne_valueType() {
        assertThrows(ParseException::class.java) {
            tokenizer.tokenize("(local $0 i32 i64)", context).parseLocal(0)
        }
    }

    @Test
    fun throws_ifNoValueTypes() {
        assertThrows(ParseException::class.java) {
            tokenizer.tokenize("(local $0)", context).parseLocal(0)
        }
    }

    @Test
    fun throws_ifNoClosingParen() {
        assertThrows(ParseException::class.java) {
            tokenizer.tokenize("(local $0 i32", context).parseLocal(0)
        }
    }

    @Test
    fun parse_returnsEmptyListOfLocals_ifAnonymous() {
        val result = tokenizer.tokenize("(local)", context).parseLocal(0)
            ?: fail("Expected a result")
        assertThat(result.parseLength).isEqualTo(3)
        assertThat(result.astNode).isEmpty()
    }

    @Test
    fun parse_returnsMultipleLocals_ifAvailable() {
        val result = tokenizer.tokenize("(local i32 i64)", context).parseLocal(0)
            ?: fail("Expected a result")
        assertThat(result.parseLength).isEqualTo(5)
        assertThat(result.astNode)
            .containsExactly(
                Local(null, ValueType.I32),
                Local(null, ValueType.I64)
            )
            .inOrder()
    }

    @Test
    fun parse_returnsLocalWithId() {
        val result = tokenizer.tokenize("(local $0 i32)", context).parseLocal(0)
            ?: fail("Expected a result")
        assertThat(result.parseLength).isEqualTo(5)
        assertThat(result.astNode)
            .containsExactly(
                Local(Identifier.Local("$0"), ValueType.I32)
            )
            .inOrder()
    }

    @Test
    fun parsePlural_parsesMultipleLocals() {
        val result = tokenizer.tokenize(
            "(local i32 i64) (local $1 f32) (local $2 f64)",
            context
        ).parseLocals(0)
        assertThat(result.parseLength).isEqualTo(15)
        assertThat(result.astNode).containsExactly(
            Local(null, ValueType.I32),
            Local(null, ValueType.I64),
            Local(Identifier.Local("$1"), ValueType.F32),
            Local(Identifier.Local("$2"), ValueType.F64)
        ).inOrder()
    }
}
