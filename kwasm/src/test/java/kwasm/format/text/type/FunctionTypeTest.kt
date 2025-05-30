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

package kwasm.format.text.type

import com.google.common.truth.Truth.assertThat
import kwasm.ast.Identifier
import kwasm.ast.astNodeListOf
import kwasm.ast.type.FunctionType
import kwasm.ast.type.Param
import kwasm.ast.type.Result
import kwasm.ast.type.ValueType
import kwasm.format.ParseContext
import kwasm.format.ParseException
import kwasm.format.text.ParseResult
import kwasm.format.text.Tokenizer
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class FunctionTypeTest {

    private val context = ParseContext("FunctionTypeTest.wasm", 1, 1)
    private val tokenizer = Tokenizer()

    @Test
    fun parseValidFunctionType_OneParamOneReturn() {
        val onlyParam = astNodeListOf(
            Param(
                Identifier.Local(
                    "\$val1"
                ),
                ValueType.I32
            )
        )
        val onlyReturnType = astNodeListOf(Result(ValueType.I32))
        val expected = ParseResult(
            FunctionType(
                onlyParam,
                onlyReturnType
            ),
            12
        )
        val actual = tokenizer.tokenize("(func (param \$val1 i32) (result i32))", context).parseFunctionType(0)
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun parseValidFunctionType_MultiParamMultiReturn() {
        val params = astNodeListOf(
            Param(
                Identifier.Local("\$val1"),
                ValueType.I32
            ),
            Param(
                Identifier.Local("\$val2"),
                ValueType.I64
            )
        )
        val returnTypes = astNodeListOf(
            Result(ValueType.I32),
            Result(ValueType.I64)
        )
        val expected = ParseResult(
            FunctionType(
                params,
                returnTypes
            ),
            21
        )
        val actual =
            tokenizer.tokenize("(func (param \$val1 i32) (param \$val2 i64) (result i32) (result i64))", context)
                .parseFunctionType(0)
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun parseValidFunctionType_SingleParamVoidReturn() {
        val onlyParam = astNodeListOf(
            Param(
                Identifier.Local(
                    "\$val1"
                ),
                ValueType.I32
            )
        )
        val expected = ParseResult(
            FunctionType(
                onlyParam,
                astNodeListOf()
            ),
            8
        )
        val actual = tokenizer.tokenize("(func (param \$val1 i32))", context).parseFunctionType(0)
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun parseValidFunctionType_NoParamSingleReturn() {
        val onlyReturnType = astNodeListOf(Result(ValueType.I32))
        val expected = ParseResult(
            FunctionType(
                astNodeListOf(),
                onlyReturnType
            ),
            7
        )
        val actual = tokenizer.tokenize("(func (result i32))", context).parseFunctionType(0)
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun parseValidFunctionType_NoParamNoReturn() {
        val expected = ParseResult(
            FunctionType(
                astNodeListOf(),
                astNodeListOf()
            ),
            3
        )
        val actual = tokenizer.tokenize("(func)", context).parseFunctionType(0)
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun parseInvalidFunctionType_FlippedParams() {
        val exception = assertThrows(ParseException::class.java) {
            tokenizer.tokenize("(func (result i32) (param \$val1 i32))", context).parseFunctionType(0)
        }
        assertThat(exception).hasMessageThat().contains("Expected ')'")
    }

    @Test
    fun parseValueParamList() {
        val expected = astNodeListOf(
            Param(Identifier.Local(null, null), ValueType.I32),
            Param(Identifier.Local(null, null), ValueType.I64)
        )
        val actual = tokenizer.tokenize("(param i32) (param i64)", context).parseParamList(0)
        assertThat(actual.astNode).isEqualTo(expected)
        assertThat(actual.parseLength).isEqualTo(8)
    }

    @Test
    fun parseValueParamList_OnlyOneParam() {
        val expected = astNodeListOf(
            Param(
                Identifier.Local(
                    null,
                    null
                ),
                ValueType.I32
            )
        )
        val actual = tokenizer.tokenize("(param i32) (result i64)", context).parseParamList(0)
        assertThat(actual.astNode).isEqualTo(expected)
        assertThat(actual.parseLength).isEqualTo(4)
    }

    @Test
    fun parseValueResultList() {
        val expected = astNodeListOf(
            Result(ValueType.I32),
            Result(ValueType.I64)
        )
        val actual = tokenizer.tokenize("(result i32) (result i64)", context).parseResultList(0)
        assertThat(actual.astNode).isEqualTo(expected)
        assertThat(actual.parseLength).isEqualTo(8)
    }

    @Test
    fun parseValueResultList_OnlyOneResult() {
        val expected = astNodeListOf(Result(ValueType.I32))
        val actual = tokenizer.tokenize("(result i32) (param i64)", context).parseResultList(0)
        assertThat(actual.astNode).isEqualTo(expected)
        assertThat(actual.parseLength).isEqualTo(4)
    }
}
