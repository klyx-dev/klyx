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

package kwasm.runtime

import com.google.common.truth.Truth.assertThat
import kwasm.KWasmRuntimeException
import kwasm.ParseRule
import kwasm.api.UnitHostFunction
import kwasm.api.functionType
import kwasm.format.text.TextModuleCounts
import kwasm.format.text.module.parseWasmFunction
import kwasm.runtime.FunctionInstance.Companion.allocate
import kwasm.runtime.stack.RuntimeStacks
import kwasm.runtime.util.AddressIndex
import kwasm.runtime.util.TypeIndex
import kwasm.runtime.utils.functionCases
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class FunctionInstanceTest {
    @get:Rule
    val parser = ParseRule()
    private val counts = TextModuleCounts(0, 0, 0, 0, 0)

    private val emptyContext: ExecutionContext
        get() = ExecutionContext(
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

    @Test
    fun allocateModuleFunction() = parser.with {
        val fn =
            """
            (func)
            """.trimIndent().tokenize().parseWasmFunction(0, counts)!!.first.astNode

        val store = Store()
        val moduleInstance = ModuleInstance(
            TypeIndex(),
            AddressIndex(),
            AddressIndex(),
            AddressIndex(),
            AddressIndex(),
            emptyList()
        )

        store.allocate(moduleInstance, fn).also { (newStore, addr) ->
            assertThat(addr.value).isEqualTo(0)
            assertThat(newStore.functions).hasSize(1)
            assertThat(newStore.functions[0]).isEqualTo(
                FunctionInstance.Module(moduleInstance, fn)
            )
        }
    }

    @Test
    fun allocateHostFunction() {
        val store = Store()
        val function = UnitHostFunction { println("Hello World!") }

        store.allocate(function)
            .also { (newStore, addr) ->
                assertThat(addr.value).isEqualTo(0)
                assertThat(newStore.functions).hasSize(1)
                assertThat(newStore.functions[0]).isEqualTo(
                    FunctionInstance.Host(
                        function.functionType,
                        function
                    )
                )
            }
    }

    @Test
    fun executeModuleFunction_empty() = functionCases(
        parser,
        """
           (func) 
        """
    ) {
        context = emptyContext

        validVoidCase()
        validCase(1, 1)
        validCase(listOf(1, 2, 3), 1, 2, 3)
    }

    @Test
    fun executeModuleFunction_withParamsAndReturnVal() = functionCases(
        parser,
        """
            (func ${'$'}addIt (param i32 i32) (result i32)
                (i32.add (local.get 0) (local.get 1))
            )
        """
    ) {
        context = emptyContext

        errorCase(
            KWasmRuntimeException::class,
            "Not enough data on the stack to call function with type: [i32, i32] => [i32]"
        )
        errorCase(
            KWasmRuntimeException::class,
            "Not enough data on the stack to call function with type: [i32, i32] => [i32]",
            1
        )
        errorCase(
            KWasmRuntimeException::class,
            "Parameter on stack does not match required parameter type for function with " +
                "type: [i32, i32] => [i32]",
            1f,
            1
        )
        errorCase(
            KWasmRuntimeException::class,
            "Parameter on stack does not match required parameter type for function with " +
                "type: [i32, i32] => [i32]",
            1,
            1f
        )
        errorCase(
            KWasmRuntimeException::class,
            "Parameter on stack does not match required parameter type for function with " +
                "type: [i32, i32] => [i32]",
            1,
            1,
            1f
        )

        validCase(2, 1, 1)
        assertThat(context.stacks.activations.height).isEqualTo(0)

        validCase(listOf(42, 2), 42, 1, 1)
        assertThat(context.stacks.activations.height).isEqualTo(0)

        validCase(listOf(1337, 42, 2), 1337, 42, 1, 1)
        assertThat(context.stacks.activations.height).isEqualTo(0)
    }

    @Test
    fun executeModuleFunction_withIdentifiedParamsAndReturnVal() = functionCases(
        parser,
        """
            (func ${'$'}addIt (param ${'$'}x i32) (param ${'$'}y i32) (result i32)
                (i32.add (local.get ${'$'}x) (local.get ${'$'}y))
            )
        """
    ) {
        context = emptyContext

        validCase(2, 1, 1)
        assertThat(context.stacks.activations.height).isEqualTo(0)

        validCase(listOf(42, 2), 42, 1, 1)
        assertThat(context.stacks.activations.height).isEqualTo(0)

        validCase(listOf(1337, 42, 2), 1337, 42, 1, 1)
        assertThat(context.stacks.activations.height).isEqualTo(0)
    }

    @Test
    fun executeModuleFunction_withIdentifiedParamsAndReturnVal_usingIndexNum() = functionCases(
        parser,
        """
            (func ${'$'}addIt (param ${'$'}x i32) (param ${'$'}y i32) (result i32)
                (i32.add (local.get 0) (local.get 1))
            )
        """
    ) {
        context = emptyContext

        validCase(2, 1, 1)
        assertThat(context.stacks.activations.height).isEqualTo(0)

        validCase(listOf(42, 2), 42, 1, 1)
        assertThat(context.stacks.activations.height).isEqualTo(0)

        validCase(listOf(1337, 42, 2), 1337, 42, 1, 1)
        assertThat(context.stacks.activations.height).isEqualTo(0)
    }

    @Test
    fun executeModuleFunction_withInvalidReturnValue() = functionCases(
        parser,
        """
            (func (param i32 i32) (result i32)
                (i32.add (local.get 0) (local.get 1))
                f32.convert_i32_s
            )
        """
    ) {
        context = emptyContext
        errorCase(KWasmRuntimeException::class, "Expected type: i32, but found f32", 1, 1)
    }

    @Test
    fun executeModuleFunction_doesntPassStackIn() = functionCases(
        parser,
        """
            (func
                i32.eqz
            )
        """
    ) {
        context = emptyContext
        errorCase(IllegalStateException::class, "Stack: Op is empty", 1, 2, 3, 4)
    }

    @Test
    fun executeModuleFunction_withInnerReturn() = functionCases(
        parser,
        """
            (func (param i32) (result i32)
                (if (result i32) 
                    (i32.eqz (i32.rem_u (local.get 0) (i32.const 2)))
                    (then 
                        (i32.const 1)
                    )
                    (else 
                        (return (i32.const 0))
                    )
                )
                i32.const 20
                i32.add 
            )
        """
    ) {
        context = emptyContext

        // Just enough stack to execute.

        validCase(21, 0)
        assertThat(context.stacks.activations.height).isEqualTo(0)

        validCase(0, 1)
        assertThat(context.stacks.activations.height).isEqualTo(0)

        validCase(21, 2)
        assertThat(context.stacks.activations.height).isEqualTo(0)

        validCase(0, 3)
        assertThat(context.stacks.activations.height).isEqualTo(0)

        validCase(21, 4)
        assertThat(context.stacks.activations.height).isEqualTo(0)

        validCase(0, 5)
        assertThat(context.stacks.activations.height).isEqualTo(0)
    }

    @Test
    fun executeModuleFunction_withLocals() = functionCases(
        parser,
        """
            (func (result i32) (local $0 i32) (local $1 i32) 
                (local.set $0 (i32.const 1)) ;; Checks that we can access by identifier
                (local.set $1 (i32.const 2)) ;; also checks
                (i32.add (local.get 0) (local.get 1)) ;; checks that we can access by index
            )
        """
    ) {
        context = emptyContext
        validCase(3)
    }

    @Test
    fun executeModuleFunction_withLocalsAndParams() = functionCases(
        parser,
        """
            ;; adds the first two params, then subtracts the third from the result
            (func (param i32 i32 i32) (result i32) (local i32 i32) 
                (local.set 3 (i32.add (local.get 0) (local.get 1)))
                (local.set 4 (i32.sub (local.get 3) (local.get 2)))
                (local.get 4)
            )
        """
    ) {
        context = emptyContext
        validCase(2, 1, 2, 1) // because (1 + 2) - 1 == 2
    }
}
