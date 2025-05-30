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
import kwasm.ast.module.Index
import kwasm.ast.type.FunctionType
import kwasm.ast.type.Param
import kwasm.ast.type.Result
import kwasm.ast.type.ValueType
import kwasm.format.ParseContext
import kwasm.format.ParseException
import kwasm.format.text.Tokenizer
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
@Suppress("EXPERIMENTAL_UNSIGNED_LITERALS")
class IndexTest {
    private val tokenizer = Tokenizer()
    private val context = ParseContext("IndexTest.wat", 1, 1)

    @Test
    fun parseIndex_throwsWithoutIdentifierOrUint() {
        val exception = assertThrows(ParseException::class.java) {
            tokenizer.tokenize("()").parseIndex<Identifier.Type>(0)
        }
        assertThat(exception).hasMessageThat().contains("Expected an index")
    }

    @Test
    fun parseIndex_forType_withUInt_returns_index() {
        val parsed = tokenizer.tokenize("1234", context)
            .parseIndex<Identifier.Type>(0)
        assertThat(parsed.astNode).isInstanceOf(Index.ByInt::class.java)
        assertThat((parsed.astNode as Index.ByInt).indexVal).isEqualTo(1234)
        assertThat(parsed.parseLength).isEqualTo(1)
    }

    @Test
    fun parseIndex_forType_withIdentifier_returns_index() {
        val parsed = tokenizer.tokenize("\$myType", context)
            .parseIndex<Identifier.Type>(0)
        assertThat(parsed.astNode).isInstanceOf(Index.ByIdentifier::class.java)
        assertThat((parsed.astNode as Index.ByIdentifier<Identifier.Type>).indexVal)
            .isEqualTo(Identifier.Type("\$myType"))
    }

    @Test
    fun parseIndex_forFunction_withUInt_returns_index() {
        val parsed = tokenizer.tokenize("1234", context)
            .parseIndex<Identifier.Function>(0)
        assertThat(parsed.astNode).isInstanceOf(Index.ByInt::class.java)
        assertThat((parsed.astNode as Index.ByInt).indexVal).isEqualTo(1234)
        assertThat(parsed.parseLength).isEqualTo(1)
    }

    @Test
    fun parseIndex_forFunction_withIdentifier_returns_index() {
        val parsed = tokenizer.tokenize("\$myFunction", context)
            .parseIndex<Identifier.Function>(0)
        assertThat(parsed.astNode).isInstanceOf(Index.ByIdentifier::class.java)
        assertThat((parsed.astNode as Index.ByIdentifier<Identifier.Function>).indexVal)
            .isEqualTo(Identifier.Function("\$myFunction"))
    }

    @Test
    fun parseIndex_forTable_withUInt_returns_index() {
        val parsed = tokenizer.tokenize("1234", context)
            .parseIndex<Identifier.Table>(0)
        assertThat(parsed.astNode).isInstanceOf(Index.ByInt::class.java)
        assertThat((parsed.astNode as Index.ByInt).indexVal).isEqualTo(1234)
        assertThat(parsed.parseLength).isEqualTo(1)
    }

    @Test
    fun parseIndex_forTable_withIdentifier_returns_index() {
        val parsed = tokenizer.tokenize("\$myTable", context)
            .parseIndex<Identifier.Table>(0)
        assertThat(parsed.astNode).isInstanceOf(Index.ByIdentifier::class.java)
        assertThat((parsed.astNode as Index.ByIdentifier<Identifier.Table>).indexVal)
            .isEqualTo(Identifier.Table("\$myTable"))
    }

    @Test
    fun parseIndex_forMemory_withUInt_returns_index() {
        val parsed = tokenizer.tokenize("1234", context)
            .parseIndex<Identifier.Memory>(0)
        assertThat(parsed.astNode).isInstanceOf(Index.ByInt::class.java)
        assertThat((parsed.astNode as Index.ByInt).indexVal).isEqualTo(1234)
        assertThat(parsed.parseLength).isEqualTo(1)
    }

    @Test
    fun parseIndex_forMemory_withIdentifier_returns_index() {
        val parsed = tokenizer.tokenize("\$myMemory", context)
            .parseIndex<Identifier.Memory>(0)
        assertThat(parsed.astNode).isInstanceOf(Index.ByIdentifier::class.java)
        assertThat((parsed.astNode as Index.ByIdentifier<Identifier.Memory>).indexVal)
            .isEqualTo(Identifier.Memory("\$myMemory"))
    }

    @Test
    fun parseIndex_forGlobal_withUInt_returns_index() {
        val parsed = tokenizer.tokenize("1234", context)
            .parseIndex<Identifier.Global>(0)
        assertThat(parsed.astNode).isInstanceOf(Index.ByInt::class.java)
        assertThat((parsed.astNode as Index.ByInt).indexVal).isEqualTo(1234)
        assertThat(parsed.parseLength).isEqualTo(1)
    }

    @Test
    fun parseIndex_forGlobal_withIdentifier_returns_index() {
        val parsed = tokenizer.tokenize("\$myGlobal", context)
            .parseIndex<Identifier.Global>(0)
        assertThat(parsed.astNode).isInstanceOf(Index.ByIdentifier::class.java)
        assertThat((parsed.astNode as Index.ByIdentifier<Identifier.Global>).indexVal)
            .isEqualTo(Identifier.Global("\$myGlobal"))
    }

    @Test
    fun parseIndex_forLocal_withUInt_returns_index() {
        val parsed = tokenizer.tokenize("1234", context)
            .parseIndex<Identifier.Local>(0)
        assertThat(parsed.astNode).isInstanceOf(Index.ByInt::class.java)
        assertThat((parsed.astNode as Index.ByInt).indexVal).isEqualTo(1234)
        assertThat(parsed.parseLength).isEqualTo(1)
    }

    @Test
    fun parseIndex_forLocal_withIdentifier_returns_index() {
        val parsed = tokenizer.tokenize("\$myLocal", context)
            .parseIndex<Identifier.Local>(0)
        assertThat(parsed.astNode).isInstanceOf(Index.ByIdentifier::class.java)
        assertThat((parsed.astNode as Index.ByIdentifier<Identifier.Local>).indexVal)
            .isEqualTo(Identifier.Local("\$myLocal"))
    }

    @Test
    fun parseIndex_forLabel_withUInt_returns_index() {
        val parsed = tokenizer.tokenize("1234", context)
            .parseIndex<Identifier.Label>(0)
        assertThat(parsed.astNode).isInstanceOf(Index.ByInt::class.java)
        assertThat((parsed.astNode as Index.ByInt).indexVal).isEqualTo(1234)
        assertThat(parsed.parseLength).isEqualTo(1)
    }

    @Test
    fun parseIndex_forLabel_withIdentifier_returns_index() {
        val parsed = tokenizer.tokenize("\$myLabel", context)
            .parseIndex<Identifier.Label>(0)
        assertThat(parsed.astNode).isInstanceOf(Index.ByIdentifier::class.java)
        assertThat((parsed.astNode as Index.ByIdentifier<Identifier.Label>).indexVal)
            .isEqualTo(Identifier.Label("\$myLabel"))
    }

    @Test
    fun parseIndex_forTypeDef_withUInt_returns_index() {
        val parsed = tokenizer.tokenize("1234", context)
            .parseIndex<Identifier.TypeDef>(0)
        assertThat(parsed.astNode).isInstanceOf(Index.ByInt::class.java)
        assertThat((parsed.astNode as Index.ByInt).indexVal).isEqualTo(1234)
        assertThat(parsed.parseLength).isEqualTo(1)
    }

    @Test
    fun parseIndex_forTypeDef_withIdentifier_returns_index() {
        val parsed = tokenizer.tokenize("(func (param i32 i64) (result i32))", context)
            .parseIndex<Identifier.TypeDef>(0)
        assertThat(parsed.astNode).isInstanceOf(Index.ByIdentifier::class.java)
        assertThat((parsed.astNode as Index.ByIdentifier<Identifier.TypeDef>).indexVal)
            .isEqualTo(
                Identifier.TypeDef(
                    FunctionType(
                        astNodeListOf(
                            Param(
                                Identifier.Local(null, null),
                                ValueType.I32
                            ),
                            Param(
                                Identifier.Local(null, null),
                                ValueType.I64
                            )
                        ),
                        astNodeListOf(Result(ValueType.I32))
                    )
                )
            )
    }

    @Test
    fun parseIndices_parsesListOfIndices() {
        val parsed = tokenizer.tokenize("$0 $1 $2 $3")
            .parseIndices<Identifier.Type>(0)
        assertThat(parsed.astNode).hasSize(4)
        repeat(4) {
            assertThat(parsed.astNode[it]).isInstanceOf(Index.ByIdentifier::class.java)
            assertThat((parsed.astNode[it] as Index.ByIdentifier<Identifier.Type>).indexVal)
                .isEqualTo(Identifier.Type("\$$it"))
        }
    }

    @Test
    fun parseIndices_withMax_onlyReturns_sizeOfMax() {
        val parsed = tokenizer.tokenize("$0 $1 $2 $3")
            .parseIndices<Identifier.Type>(0, max = 2)
        assertThat(parsed.astNode).hasSize(2)
        repeat(2) {
            assertThat(parsed.astNode[it]).isInstanceOf(Index.ByIdentifier::class.java)
            assertThat((parsed.astNode[it] as Index.ByIdentifier<Identifier.Type>).indexVal)
                .isEqualTo(Identifier.Type("\$$it"))
        }
    }

    @Test
    fun parseIndices_throwsIf_minIndices_notReached() {
        val exception = assertThrows(ParseException::class.java) {
            tokenizer.tokenize("$0 $1").parseIndices<Identifier.Type>(0, min = 3)
        }
        assertThat(exception).hasMessageThat().contains("Expected at least 3 indices, found 2")
    }
}
