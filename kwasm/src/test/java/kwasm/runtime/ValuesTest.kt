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

package kwasm.runtime

import com.google.common.truth.Truth.assertThat
import kwasm.ast.type.ValueType
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@Suppress("EXPERIMENTAL_UNSIGNED_LITERALS", "EXPERIMENTAL_API_USAGE")
@RunWith(JUnit4::class)
class ValuesTest {
    @Test
    fun intValue_unsigned() {
        val x = IntValue(-3)
        assertThat(x.unsignedValue).isEqualTo(UInt.MAX_VALUE - 2u)
    }

    @Test
    fun unsignedInt_intValue() {
        val x = UInt.MAX_VALUE.toValue()
        assertThat(x.value).isEqualTo(-1)
    }

    @Test
    fun longValue_unsigned() {
        val x = LongValue(-3)
        assertThat(x.unsignedValue).isEqualTo(ULong.MAX_VALUE - 2u)
    }

    @Test
    fun unsignedLong_longValue() {
        val x = ULong.MAX_VALUE.toValue()
        assertThat(x.value).isEqualTo(-1L)
    }

    @Test
    fun toValueType() {
        assertThat(IntValue::class.toValueType()).isEqualTo(ValueType.I32)
        assertThat(LongValue::class.toValueType()).isEqualTo(ValueType.I64)
        assertThat(FloatValue::class.toValueType()).isEqualTo(ValueType.F32)
        assertThat(DoubleValue::class.toValueType()).isEqualTo(ValueType.F64)
        assertThat(EmptyValue::class.toValueType()).isNull()
    }
}
