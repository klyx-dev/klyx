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

package kwasm.format.binary.type

import com.google.common.truth.Truth.assertThat
import kwasm.ast.type.ValueType
import kwasm.format.ParseException
import kwasm.format.binary.BinaryParser
import kwasm.format.binary.toByteArray
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.io.ByteArrayInputStream

@RunWith(JUnit4::class)
class ValueTypeTest {
    @Test
    fun intAsValueType() {
        assertThat(0x7F.asValueType()).isEqualTo(ValueType.I32)
        assertThat(0x7E.asValueType()).isEqualTo(ValueType.I64)
        assertThat(0x7D.asValueType()).isEqualTo(ValueType.F32)
        assertThat(0x7C.asValueType()).isEqualTo(ValueType.F64)
        assertThat(0x7B.asValueType()).isNull()
        assertThat(0.asValueType()).isNull()
        assertThat(1337.asValueType()).isNull()
    }

    @Test
    fun readValueType_throwsWhenNoByte() {
        val bytes = ByteArray(0)
        val parser = BinaryParser(ByteArrayInputStream(bytes))
        assertThrows(ParseException::class.java) { parser.readValueType() }
    }

    @Test
    fun readValueType_throwsWhenInvalid() {
        val bytes = listOf(0x11).toByteArray()
        val parser = BinaryParser(ByteArrayInputStream(bytes))
        assertThrows(ParseException::class.java) { parser.readValueType() }
    }

    @Test
    fun readValueType() {
        val bytes = listOf(0x7F, 0x7E, 0x7D, 0x7C).toByteArray()
        val parser = BinaryParser(ByteArrayInputStream(bytes))
        assertThat(parser.readValueType()).isEqualTo(ValueType.I32)
        assertThat(parser.readValueType()).isEqualTo(ValueType.I64)
        assertThat(parser.readValueType()).isEqualTo(ValueType.F32)
        assertThat(parser.readValueType()).isEqualTo(ValueType.F64)
    }
}
