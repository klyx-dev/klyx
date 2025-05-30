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
import kwasm.ast.module.ElementSegment
import kwasm.ast.module.Index
import kwasm.ast.module.Offset
import kwasm.format.binary.BinaryParser
import kwasm.format.binary.toByteArray
import kwasm.format.binary.value.toBytesAsVector
import kwasm.util.Leb128
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.io.ByteArrayInputStream

@Suppress("UNCHECKED_CAST")
@RunWith(JUnit4::class)
class ElementSectionTest {
    @Test
    fun readElementSection_empty() {
        val bytes = listOf(0x09, 0x01, 0x00).toByteArray()
        val parser = BinaryParser(ByteArrayInputStream(bytes))
        assertThat(parser.readSection()).isEqualTo(ElementSection(emptyList()))
    }

    @Test
    fun readElementSection_nonEmpty() {
        val section = ElementSection(
            listOf(
                ElementSegment(
                    Index.ByInt(0) as Index<Identifier.Table>,
                    Offset(
                        Expression(listOf(NumericConstantInstruction.I32(IntegerLiteral.S32(1))))
                    ),
                    emptyList()
                ),
                ElementSegment(
                    Index.ByInt(0) as Index<Identifier.Table>,
                    Offset(
                        Expression(listOf(NumericConstantInstruction.I32(IntegerLiteral.S32(12))))
                    ),
                    listOf(
                        Index.ByInt(1) as Index<Identifier.Function>,
                        Index.ByInt(2) as Index<Identifier.Function>,
                        Index.ByInt(3) as Index<Identifier.Function>,
                    )
                ),
            )
        )
        val bodyBytes: List<Byte> = (
            sequenceOf<Byte>(0x02) +
                section.segments[0].tableIndex.toBytes() +
                sequenceOf(0x41, 0x01, 0x0B) +
                section.segments[0].init.toBytesAsVector { it.toBytes() } +
                section.segments[1].tableIndex.toBytes() +
                sequenceOf(0x41, 0x0C, 0x0B) +
                section.segments[1].init.toBytesAsVector { it.toBytes() }
            ).toList()

        val bytes: Sequence<Byte> =
            sequenceOf<Byte>(0x09) +
                Leb128.encodeUnsigned(bodyBytes.size) +
                bodyBytes.asSequence()

        val parser = BinaryParser(ByteArrayInputStream(bytes.toList().toByteArray()))
        assertThat(parser.readSection()).isEqualTo(section)
    }
}
