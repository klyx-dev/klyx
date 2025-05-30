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
import kwasm.ast.IntegerLiteral
import kwasm.ast.instruction.Expression
import kwasm.ast.instruction.NumericConstantInstruction
import kwasm.ast.module.DataSegment
import kwasm.ast.module.Index
import kwasm.ast.module.Offset
import kwasm.format.binary.BinaryParser
import kwasm.format.binary.toByteArray
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.io.ByteArrayInputStream

@Suppress("UNCHECKED_CAST")
@RunWith(JUnit4::class)
class DataSectionTest {
    @Test
    fun empty() {
        val bytes = listOf(0x0B, 0x01, 0x00).toByteArray()
        val parser = BinaryParser(ByteArrayInputStream(bytes))
        assertThat(parser.readSection()).isEqualTo(DataSection(emptyList()))
    }

    @Test
    fun nonEmpty() {
        val bytes = listOf(
            0x0B, 0x13,
            0x02,
            0x00, 0x41, 0x00, 0x0B, 0x04, 0xFA, 0xCE, 0xB0, 0x08,
            0x00, 0x41, 0x04, 0x0B, 0x04, 0x80, 0x08, 0x1E, 0x17,
        ).toByteArray()
        val parser = BinaryParser(ByteArrayInputStream(bytes))
        val expected = DataSection(
            listOf(
                DataSegment(
                    Index.ByInt(0) as Index<Identifier.Memory>,
                    Offset(
                        Expression(listOf(NumericConstantInstruction.I32(IntegerLiteral.S32(0))))
                    ),
                    listOf(0xFA, 0xCE, 0xB0, 0x08).toByteArray()
                ),
                DataSegment(
                    Index.ByInt(0) as Index<Identifier.Memory>,
                    Offset(
                        Expression(listOf(NumericConstantInstruction.I32(IntegerLiteral.S32(4))))
                    ),
                    listOf(0x80, 0x08, 0x1E, 0x17).toByteArray()
                ),
            )
        )
        assertThat(parser.readSection()).isEqualTo(expected)
    }
}
