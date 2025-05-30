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

package kwasm.format.binary.instruction

import com.google.common.truth.Truth.assertThat
import kwasm.ast.instruction.ParametricInstruction
import kwasm.format.binary.BinaryParser
import kwasm.format.binary.toByteArray
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.io.ByteArrayInputStream

@RunWith(JUnit4::class)
class ParametricInstructionTest {
    @Test
    fun drop() {
        val parser = BinaryParser(ByteArrayInputStream(listOf(0x1A).toByteArray()))
        val actual = parser.readInstruction()
        assertThat(actual).isEqualTo(ParametricInstruction.Drop)
    }

    @Test
    fun select() {
        val parser = BinaryParser(ByteArrayInputStream(listOf(0x1B).toByteArray()))
        val actual = parser.readInstruction()
        assertThat(actual).isEqualTo(ParametricInstruction.Select)
    }
}
