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

package kwasm.validation.instruction

import kwasm.ParseRule
import kwasm.ast.instruction.Instruction
import kwasm.validation.ValidationContext
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.lang.IllegalStateException

@RunWith(JUnit4::class)
class InstructionValidatorTest {
    @get:Rule val parser = ParseRule()

    @Test
    fun unsupportedInstructionType_throws() {
        assertThrows(IllegalStateException::class.java) {
            DummyInstruction().validate(ValidationContext.EMPTY_FUNCTION_BODY)
        }
    }

    private class DummyInstruction : Instruction
}
