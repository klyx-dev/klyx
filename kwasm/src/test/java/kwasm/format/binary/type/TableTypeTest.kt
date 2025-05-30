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
import kwasm.ast.type.ElementType
import kwasm.ast.type.Limits
import kwasm.format.ParseException
import kwasm.format.binary.BinaryParser
import kwasm.format.binary.toByteArray
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.io.ByteArrayInputStream

@RunWith(JUnit4::class)
class TableTypeTest {
    @Test
    fun readTableType_invalidElemType_throws() {
        val bytes = listOf(0x71, 0x00, 0xFF, 0x01).toByteArray()
        val parser = BinaryParser(ByteArrayInputStream(bytes))
        val e = assertThrows(ParseException::class.java) { parser.readTableType() }
        assertThat(e).hasMessageThat().contains("Illegal element type")
    }

    @Test
    fun readTableType() {
        val bytes = listOf(0x70, 0x00, 0xFF, 0x01).toByteArray()
        val parser = BinaryParser(ByteArrayInputStream(bytes))
        val tableType = parser.readTableType()
        assertThat(tableType.elemType).isEqualTo(ElementType.FunctionReference)
        assertThat(tableType.limits).isEqualTo(Limits(255))
    }
}
