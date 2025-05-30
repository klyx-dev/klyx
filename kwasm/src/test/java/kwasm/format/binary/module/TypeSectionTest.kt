/*
 * Copyright 2021 Google LLC
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

package kwasm.format.binary.module

import com.google.common.truth.Truth.assertThat
import kwasm.ast.Identifier
import kwasm.ast.type.FunctionType
import kwasm.ast.type.Param
import kwasm.ast.type.Result
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
class TypeSectionTest {
    @Test
    fun empty() {
        val bytes = listOf(0x01, 0x01, 0x00)
        val parser = BinaryParser(ByteArrayInputStream(bytes.toByteArray()))
        val section = parser.readSection()
        assertThat(section).isEqualTo(TypeSection(emptyList()))
    }

    @Test
    fun oneItem() {
        val bytes = listOf(0x01, 0x06, 0x01, 0x60, 0x01, 0x7F, 0x01, 0x7E)
        val parser = BinaryParser(ByteArrayInputStream(bytes.toByteArray()))
        val section = parser.readSection()
        assertThat(section).isEqualTo(
            TypeSection(
                listOf(
                    FunctionType(
                        listOf(
                            Param(Identifier.Local(null, null), ValueType.I32)
                        ),
                        listOf(
                            Result(ValueType.I64)
                        )
                    )
                )
            )
        )
    }

    @Test
    fun twoItems() {
        val bytes = listOf(0x01, 0x09, 0x02, 0x60, 0x01, 0x7F, 0x01, 0x7E, 0x60, 0x00, 0x00)
        val parser = BinaryParser(ByteArrayInputStream(bytes.toByteArray()))
        val section = parser.readSection()
        assertThat(section).isEqualTo(
            TypeSection(
                listOf(
                    FunctionType(
                        listOf(
                            Param(Identifier.Local(null, null), ValueType.I32)
                        ),
                        listOf(
                            Result(ValueType.I64)
                        )
                    ),
                    FunctionType(emptyList(), emptyList())
                )
            )
        )
    }

    @Test
    fun incorrectSize_throws() {
        val bytes = listOf(0x01, 0x06, 0x02, 0x60, 0x01, 0x7F, 0x01, 0x7E, 0x60, 0x00, 0x00)
        val parser = BinaryParser(ByteArrayInputStream(bytes.toByteArray()))
        assertThrows(ParseException::class.java) { parser.readSection() }
    }
}
