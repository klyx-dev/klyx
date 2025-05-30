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

package kwasm.ast.util

import com.google.common.truth.Truth.assertThat
import kwasm.ast.Identifier
import kwasm.ast.module.Index
import kwasm.ast.type.GlobalType
import kwasm.ast.type.Result
import kwasm.ast.type.ResultType
import kwasm.ast.type.ValueType
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class AstNodeIndexTest {
    @Test
    fun startsEmpty() {
        val index = AstNodeIndex<GlobalType>()
        assertThat(index.size).isEqualTo(0)
    }

    @Test
    fun mutableStartsEmpty() {
        val index = MutableAstNodeIndex<GlobalType>()
        assertThat(index.size).isEqualTo(0)
    }

    @Test
    fun size() {
        val original = MutableAstNodeIndex<GlobalType>()
        original += GlobalType(ValueType.I32, true)
        original += GlobalType(ValueType.I64, false)
        original += GlobalType(ValueType.F32, true)
        original += GlobalType(ValueType.F64, false)

        assertThat(original.size).isEqualTo(4)
    }

    @Test
    fun toMutableIndex_returnsANewCopy() {
        val original = MutableAstNodeIndex<GlobalType>()
        original += GlobalType(ValueType.I32, true)
        original += GlobalType(ValueType.I64, false)

        val copy = original.toMutableIndex()
        assertThat(original !== copy).isTrue()
    }

    @Test
    fun toImmutableIndex_returnsANewCopy() {
        val original = MutableAstNodeIndex<GlobalType>()
        original += GlobalType(ValueType.I32, true)
        original += GlobalType(ValueType.I64, false)

        val copy = original.toImmutableIndex()
        assertThat(original !== copy).isTrue()
    }

    @Test
    fun byIdentifier() {
        val index = MutableAstNodeIndex<GlobalType>()
        index[Identifier.Global("myGlobal1")] =
            GlobalType(ValueType.I32, false)
        index[Identifier.Global("myGlobal2")] =
            GlobalType(ValueType.I64, false)

        assertThat(index[0])
            .isEqualTo(GlobalType(ValueType.I32, false))
        assertThat(index[1])
            .isEqualTo(GlobalType(ValueType.I64, false))
        assertThat(index[Identifier.Global("myGlobal1")])
            .isEqualTo(GlobalType(ValueType.I32, false))
        assertThat(index[Identifier.Global("myGlobal2")])
            .isEqualTo(GlobalType(ValueType.I64, false))

        assertThat(index[Identifier.Global("myGlobal3")])
            .isNull()
    }

    @Test
    fun setByIdentifier_throwsWhenIdentifier_usedAlready() {
        val index = MutableAstNodeIndex<GlobalType>()
        index[Identifier.Global("myGlobal1")] =
            GlobalType(ValueType.I32, false)

        assertThrows(IllegalStateException::class.java) {
            index[Identifier.Global("myGlobal1")] =
                GlobalType(ValueType.I64, false)
        }
    }

    @Test
    fun byIndex_usingIdentifier() {
        val index = MutableAstNodeIndex<GlobalType>()
        index[Identifier.Global("myGlobal1")] =
            GlobalType(ValueType.I32, false)

        assertThat(index[Index.ByIdentifier(Identifier.Global("myGlobal1"))])
            .isEqualTo(GlobalType(ValueType.I32, false))
        assertThat(index[Index.ByIdentifier(Identifier.Global("myGlobal2"))])
            .isNull()
    }

    @Test
    fun byIndex_usingInt() {
        val index = MutableAstNodeIndex<GlobalType>()
        index[Identifier.Global("myGlobal1")] =
            GlobalType(ValueType.I32, false)

        assertThat(index[Index.ByInt(0)])
            .isEqualTo(GlobalType(ValueType.I32, false))
        assertThat(index[Index.ByInt(1)])
            .isNull()
    }

    @Test
    fun byInt() {
        val index = MutableAstNodeIndex<GlobalType>()
        index[Identifier.Global("myGlobal1")] =
            GlobalType(ValueType.I32, false)

        assertThat(index[0]).isEqualTo(
            GlobalType(
                ValueType.I32,
                false
            )
        )
        assertThat(index[1]).isNull()
    }

    @Test
    fun setWithNullIdentifier_addsToIntBasedLookup() {
        val index = MutableAstNodeIndex<GlobalType>()
        index[null] = GlobalType(ValueType.I32, false)
        index[null] = GlobalType(ValueType.I64, false)

        assertThat(index.size).isEqualTo(2)
        assertThat(index[0]).isEqualTo(
            GlobalType(
                ValueType.I32,
                false
            )
        )
        assertThat(index[1]).isEqualTo(
            GlobalType(
                ValueType.I64,
                false
            )
        )
    }

    @Test
    fun prepend_withReusedIdentifier_throws() {
        val index = MutableAstNodeIndex<ResultType>()
        index[Identifier.Label("\$foo")] = ResultType(Result(ValueType.I32))
        assertThrows(IllegalStateException::class.java) {
            index.prepend(Identifier.Label("\$foo"), ResultType(Result(ValueType.I64)))
        }
    }

    @Test
    fun prepend_withIdentiifier() {
        val index = MutableAstNodeIndex<ResultType>()
        index[null] = ResultType(Result(ValueType.I64))
        index.prepend(Identifier.Label("\$foo"), ResultType(Result(ValueType.I32)))

        assertThat(index[0]).isEqualTo(ResultType(Result(ValueType.I32)))
    }

    @Test
    fun prepend_withoutIdentiifier() {
        val index = MutableAstNodeIndex<ResultType>()
        index[null] = ResultType(Result(ValueType.I64))
        index.prepend(ResultType(Result(ValueType.I32)))

        assertThat(index[0]).isEqualTo(ResultType(Result(ValueType.I32)))
    }
}
