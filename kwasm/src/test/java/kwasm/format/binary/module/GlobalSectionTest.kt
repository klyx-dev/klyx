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
import kwasm.ast.module.Global
import kwasm.ast.type.GlobalType
import kwasm.ast.type.ValueType
import kwasm.format.binary.BinaryParser
import kwasm.format.binary.toByteArray
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.io.ByteArrayInputStream

@RunWith(JUnit4::class)
class GlobalSectionTest {
    @Test
    fun empty() {
        val section = GlobalSection(emptyList())
        val bytes = listOf(0x06, 0x01, 0x00).toByteArray()
        val parser = BinaryParser(ByteArrayInputStream(bytes))
        assertThat(parser.readSection()).isEqualTo(section)
    }

    @Test
    fun nonEmpty() {
        val section = GlobalSection(
            listOf(
                Global(
                    Identifier.Global(null, null),
                    GlobalType(ValueType.I32, false),
                    Expression(
                        listOf(NumericConstantInstruction.I32(IntegerLiteral.S32(1)))
                    )
                )
            )
        )
        val bytes = listOf(0x06, 0x06, 0x01, 0x7F, 0x00, 0x41, 0x01, 0x0B).toByteArray()
        val parser = BinaryParser(ByteArrayInputStream(bytes))
        assertThat(parser.readSection()).isEqualTo(section)
    }
}
