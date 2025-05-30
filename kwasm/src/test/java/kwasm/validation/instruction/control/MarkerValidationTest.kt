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

package kwasm.validation.instruction.control

import kwasm.ast.instruction.ControlInstruction
import kwasm.ast.type.ResultType
import kwasm.validation.ValidationContext.Companion.EMPTY_FUNCTION_BODY
import kwasm.validation.ValidationException
import kwasm.validation.instruction.validate
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class MarkerValidationTest {
    @Test
    fun startBlock_throws() {
        val instruction = ControlInstruction.StartBlock(
            identifier = null,
            original = ControlInstruction.Block(null, ResultType(null), emptyList()),
            endPosition = 0
        )

        assertThrows(ValidationException::class.java) { instruction.validate(EMPTY_FUNCTION_BODY) }
    }

    @Test
    fun startIf_throws() {
        val instruction = ControlInstruction.StartIf(
            identifier = null,
            original = ControlInstruction.If(null, ResultType(null), emptyList(), emptyList()),
            positiveStartPosition = 0,
            negativeStartPosition = 0,
            endPosition = 0
        )

        assertThrows(ValidationException::class.java) { instruction.validate(EMPTY_FUNCTION_BODY) }
    }

    @Test
    fun endBlock_throws() {
        val instruction = ControlInstruction.EndBlock(
            identifier = null,
            original = ControlInstruction.Block(null, ResultType(null), emptyList()),
            startPosition = 0
        )

        assertThrows(ValidationException::class.java) { instruction.validate(EMPTY_FUNCTION_BODY) }
    }
}
