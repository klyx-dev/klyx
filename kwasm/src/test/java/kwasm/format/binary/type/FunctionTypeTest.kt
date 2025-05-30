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
import kwasm.ast.Identifier
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
class FunctionTypeTest {
    @Test
    fun readFunctionType_throwsWhenNotPrefixedCorrectly() {
        val bytes = listOf(0x61, 0x00, 0x00).toByteArray()
        val parser = BinaryParser(ByteArrayInputStream(bytes))
        assertThrows(ParseException::class.java) { parser.readFunctionType() }
    }

    @Test
    fun readFunctionType_empty() {
        val bytes = listOf(0x60, 0x00, 0x00).toByteArray()
        val parser = BinaryParser(ByteArrayInputStream(bytes))
        val result = parser.readFunctionType()
        assertThat(result.parameters).isEmpty()
        assertThat(result.returnValueEnums).isEmpty()
    }

    @Test
    fun readFunctionType_paramsNoReturns() {
        val bytes = listOf(0x60, 0x02, 0x7F, 0x7E, 0x00).toByteArray()
        val parser = BinaryParser(ByteArrayInputStream(bytes))
        val result = parser.readFunctionType()
        assertThat(result.parameters).hasSize(2)
        assertThat(result.parameters[0].id).isEqualTo(Identifier.Local(null, null))
        assertThat(result.parameters[0].valType).isEqualTo(ValueType.I32)
        assertThat(result.parameters[1].id).isEqualTo(Identifier.Local(null, null))
        assertThat(result.parameters[1].valType).isEqualTo(ValueType.I64)
        assertThat(result.returnValueEnums).isEmpty()
    }

    @Test
    fun readFunctionType_returnsNoParams() {
        val bytes = listOf(0x60, 0x00, 0x02, 0x7F, 0x7E).toByteArray()
        val parser = BinaryParser(ByteArrayInputStream(bytes))
        val result = parser.readFunctionType()
        assertThat(result.parameters).isEmpty()
        assertThat(result.returnValueEnums).hasSize(2)
        assertThat(result.returnValueEnums[0].valType).isEqualTo(ValueType.I32)
        assertThat(result.returnValueEnums[1].valType).isEqualTo(ValueType.I64)
    }
}
