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

package kwasm.runtime.instruction

import com.google.common.truth.Truth.assertThat
import kwasm.ParseRule
import kwasm.runtime.EmptyExecutionContext
import kwasm.runtime.toValue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class NumericConstantInstructionTest {
    @get:Rule
    val parser = ParseRule()

    @Test
    fun i32() = parser.with {
        val instruction = "i32.const 42".parseInstruction()
        val postContext = instruction.execute(EmptyExecutionContext())
        assertThat(postContext.stacks.operands.pop()).isEqualTo(42.toValue())
    }

    @Test
    fun i64() = parser.with {
        val instruction = "i64.const 42".parseInstruction()
        val postContext = instruction.execute(EmptyExecutionContext())
        assertThat(postContext.stacks.operands.pop()).isEqualTo(42L.toValue())
    }

    @Test
    fun f32() = parser.with {
        val instruction = "f32.const 93.3".parseInstruction()
        val postContext = instruction.execute(EmptyExecutionContext())
        assertThat(postContext.stacks.operands.pop()).isEqualTo(93.3f.toValue())
    }

    @Test
    fun f64() = parser.with {
        val instruction = "f64.const 93.3".parseInstruction()
        val postContext = instruction.execute(EmptyExecutionContext())
        assertThat(postContext.stacks.operands.pop()).isEqualTo(93.3.toValue())
    }
}
