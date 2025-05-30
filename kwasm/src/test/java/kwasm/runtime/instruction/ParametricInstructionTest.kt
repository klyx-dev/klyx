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

import kwasm.KWasmRuntimeException
import kwasm.ParseRule
import kwasm.runtime.ExecutionContext
import kwasm.runtime.ModuleInstance
import kwasm.runtime.Store
import kwasm.runtime.stack.RuntimeStacks
import kwasm.runtime.util.AddressIndex
import kwasm.runtime.util.TypeIndex
import kwasm.runtime.utils.instructionCases
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ParametricInstructionTest {
    @get:Rule
    val parser = ParseRule()

    @Test
    fun drop() = instructionCases(parser, "drop") {
        context = buildContext()

        // Empty stack should fail.
        errorCase(IllegalStateException::class, "Stack: Op is empty")
        // Stack should be empty after dropping 3
        validVoidCase(3)
        // Stack should only contain 42.0 after dropping 3
        validCase(42.0, 42.0, 3)
    }

    @Test
    fun select() = instructionCases(parser, "select") {
        context = buildContext()

        errorCase(
            KWasmRuntimeException::class,
            "Select expects i32 at the top of the stack.",
            10.0,
            11.0,
            1L // Incorrect value.
        )

        errorCase(
            IllegalStateException::class,
            "Stack: Op is empty",
            // should be two more values
            1
        )

        errorCase(
            IllegalStateException::class,
            "Stack: Op is empty",
            // should be one more value
            11.0,
            1
        )

        errorCase(
            KWasmRuntimeException::class,
            "Select expects two values of equal type on the stack.",
            // Mismatched, a float and a double
            10f,
            11.0,
            1
        )

        validCase(42.0, 42.0, 1337.0, 0)
        validCase(1337.0, 42.0, 1337.0, -1)
        validCase(1337.0, 42.0, 1337.0, 1)
    }

    private fun buildContext(): ExecutionContext {
        return ExecutionContext(
            Store(),
            ModuleInstance(
                TypeIndex(),
                AddressIndex(),
                AddressIndex(),
                AddressIndex(),
                AddressIndex(),
                emptyList()
            ),
            RuntimeStacks()
        )
    }
}
