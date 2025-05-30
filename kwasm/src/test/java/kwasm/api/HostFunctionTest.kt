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

package kwasm.api

import com.google.common.truth.Truth.assertThat
import kwasm.ast.Identifier
import kwasm.ast.type.FunctionType
import kwasm.ast.type.Param
import kwasm.ast.type.Result
import kwasm.ast.type.ValueType
import kwasm.ast.util.toFunctionIndex
import kwasm.runtime.Address
import kwasm.runtime.DoubleValue
import kwasm.runtime.EmptyValue
import kwasm.runtime.ExecutionContext
import kwasm.runtime.FloatValue
import kwasm.runtime.IntValue
import kwasm.runtime.Memory
import kwasm.runtime.ModuleInstance
import kwasm.runtime.Store
import kwasm.runtime.Value
import kwasm.runtime.memory.ByteBufferMemory
import kwasm.runtime.stack.Activation
import kwasm.runtime.stack.ActivationStack
import kwasm.runtime.stack.RuntimeStacks
import kwasm.runtime.toFunctionInstance
import kwasm.runtime.toValue
import kwasm.runtime.util.AddressIndex
import kwasm.runtime.util.LocalIndex
import kwasm.runtime.util.TypeIndex
import org.junit.Assert.assertThrows
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class HostFunctionTest {
    private lateinit var context: HostFunctionContext

    @Before
    fun setUp() {
        context = Context(ByteBufferMemory())
    }

    @Test
    fun unitHostFunction_noParams() {
        var called = false
        val function = UnitHostFunction {
            assertThat(it).isSameInstanceAs(this@HostFunctionTest.context)
            called = true
        }

        assertThat(function(emptyList(), context)).isEqualTo(EmptyValue)
        assertThat(called).isTrue()

        assertThat(function.functionType).isEqualTo(
            FunctionType(emptyList(), emptyList())
        )
    }

    @Test
    fun unitHostFunction_oneParam() {
        val argumentValues = mutableListOf<Value<*>>()
        val function = UnitHostFunction { a: IntValue, context ->
            assertThat(context).isSameInstanceAs(this@HostFunctionTest.context)
            argumentValues.add(a)
        }

        assertThat(function(listOf(42.toValue()), context)).isEqualTo(EmptyValue)
        assertThat(argumentValues).containsExactly(42.toValue())

        assertThat(function.functionType).isEqualTo(
            FunctionType(
                listOf(Param(Identifier.Local(null, 0), ValueType.I32)),
                emptyList()
            )
        )
    }

    @Test
    fun unitHostFunction_oneParam_throwsOnBadValue() {
        val function = UnitHostFunction { _: IntValue, _ ->
            fail("Should not be called")
        }

        assertThrows(IllegalArgumentException::class.java) {
            function(listOf(), context)
        }.also {
            assertThat(it).hasMessageThat().contains(
                "Parameter at position 1 is null or is not of expected type: ${IntValue::class}"
            )
        }

        assertThrows(IllegalArgumentException::class.java) {
            function(listOf(42L.toValue()), context)
        }.also {
            assertThat(it).hasMessageThat().contains(
                "Parameter at position 1 is null or is not of expected type: ${IntValue::class}"
            )
        }
    }

    @Test
    fun unitHostFunction_twoParams() {
        val argumentValues = mutableListOf<Value<*>>()
        val function = UnitHostFunction { a: IntValue, b: FloatValue, context ->
            assertThat(context).isSameInstanceAs(this@HostFunctionTest.context)
            argumentValues.add(a)
            argumentValues.add(b)
        }

        assertThat(
            function(listOf(42.toValue(), 1337.0f.toValue()), context)
        ).isEqualTo(EmptyValue)
        assertThat(argumentValues).containsExactly(42.toValue(), 1337.0f.toValue())

        assertThat(function.functionType).isEqualTo(
            FunctionType(
                listOf(
                    Param(Identifier.Local(null, 0), ValueType.I32),
                    Param(Identifier.Local(null, 1), ValueType.F32)
                ),
                emptyList()
            )
        )
    }

    @Test
    fun unitHostFunction_twoParams_throwsOnBadValue() {
        val function = UnitHostFunction { _: IntValue, _: FloatValue, _ ->
            fail("Should not be called")
        }

        assertThrows(IllegalArgumentException::class.java) {
            function(listOf(), context)
        }.also {
            assertThat(it).hasMessageThat().contains(
                "Parameter at position 1 is null or is not of expected type: ${IntValue::class}"
            )
        }

        assertThrows(IllegalArgumentException::class.java) {
            function(listOf(42L.toValue(), 1337f.toValue()), context)
        }.also {
            assertThat(it).hasMessageThat().contains(
                "Parameter at position 1 is null or is not of expected type: ${IntValue::class}"
            )
        }

        assertThrows(IllegalArgumentException::class.java) {
            function(listOf(42.toValue()), context)
        }.also {
            assertThat(it).hasMessageThat().contains(
                "Parameter at position 2 is null or is not of expected type: ${FloatValue::class}"
            )
        }

        assertThrows(IllegalArgumentException::class.java) {
            function(listOf(42.toValue(), 1337.0.toValue()), context)
        }.also {
            assertThat(it).hasMessageThat().contains(
                "Parameter at position 2 is null or is not of expected type: ${FloatValue::class}"
            )
        }
    }

    @Test
    fun unitHostFunction_threeParams() {
        val argumentValues = mutableListOf<Value<*>>()
        val function = UnitHostFunction { a: IntValue, b: FloatValue, c: DoubleValue, context ->
            assertThat(context).isSameInstanceAs(this@HostFunctionTest.context)
            argumentValues.add(a)
            argumentValues.add(b)
            argumentValues.add(c)
        }

        assertThat(
            function(listOf(42.toValue(), 1337.0f.toValue(), 80081335.0.toValue()), context)
        ).isEqualTo(EmptyValue)
        assertThat(argumentValues)
            .containsExactly(42.toValue(), 1337.0f.toValue(), 80081335.0.toValue())

        assertThat(function.functionType).isEqualTo(
            FunctionType(
                listOf(
                    Param(Identifier.Local(null, 0), ValueType.I32),
                    Param(Identifier.Local(null, 1), ValueType.F32),
                    Param(Identifier.Local(null, 1), ValueType.F64)
                ),
                emptyList()
            )
        )
    }

    @Test
    fun unitHostFunction_threeParams_throwsOnBadValue() {
        val function = UnitHostFunction { _: IntValue, _: FloatValue, _: DoubleValue, _ ->
            fail("Should not be called")
        }

        assertThrows(IllegalArgumentException::class.java) {
            function(listOf(), context)
        }.also {
            assertThat(it).hasMessageThat().contains(
                "Parameter at position 1 is null or is not of expected type: ${IntValue::class}"
            )
        }

        assertThrows(IllegalArgumentException::class.java) {
            function(listOf(42L.toValue(), 1337f.toValue(), 80081335.0.toValue()), context)
        }.also {
            assertThat(it).hasMessageThat().contains(
                "Parameter at position 1 is null or is not of expected type: ${IntValue::class}"
            )
        }

        assertThrows(IllegalArgumentException::class.java) {
            function(listOf(42.toValue()), context)
        }.also {
            assertThat(it).hasMessageThat().contains(
                "Parameter at position 2 is null or is not of expected type: ${FloatValue::class}"
            )
        }

        assertThrows(IllegalArgumentException::class.java) {
            function(listOf(42.toValue(), 1337.0.toValue(), 80081335.0.toValue()), context)
        }.also {
            assertThat(it).hasMessageThat().contains(
                "Parameter at position 2 is null or is not of expected type: ${FloatValue::class}"
            )
        }

        assertThrows(IllegalArgumentException::class.java) {
            function(listOf(42.toValue(), 1337.0f.toValue()), context)
        }.also {
            assertThat(it).hasMessageThat().contains(
                "Parameter at position 3 is null or is not of expected type: ${DoubleValue::class}"
            )
        }

        assertThrows(IllegalArgumentException::class.java) {
            function(listOf(42.toValue(), 1337.0f.toValue(), 1.toValue()), context)
        }.also {
            assertThat(it).hasMessageThat().contains(
                "Parameter at position 3 is null or is not of expected type: ${DoubleValue::class}"
            )
        }
    }

    @Test
    fun hostFunction_noParams() {
        val function = HostFunction { context: HostFunctionContext ->
            assertThat(context).isSameInstanceAs(this@HostFunctionTest.context)
            return@HostFunction 35.toValue()
        }

        assertThat(function(emptyList(), context)).isEqualTo(35.toValue())

        assertThat(function.functionType).isEqualTo(
            FunctionType(
                emptyList(),
                listOf(Result(ValueType.I32))
            )
        )
    }

    @Test
    fun hostFunction_oneParam() {
        val function = HostFunction { a: IntValue, context ->
            assertThat(context).isSameInstanceAs(this@HostFunctionTest.context)
            (a.value * 3).toValue()
        }

        assertThat(function(listOf(42.toValue()), context)).isEqualTo((42 * 3).toValue())

        assertThat(function.functionType).isEqualTo(
            FunctionType(
                listOf(Param(Identifier.Local(null, 0), ValueType.I32)),
                listOf(Result(ValueType.I32))
            )
        )
    }

    @Test
    fun hostFunction_oneParam_throwsOnBadValue() {
        val function = HostFunction { _: IntValue, _ ->
            fail("Should not be called")
            3.toValue()
        }

        assertThrows(IllegalArgumentException::class.java) {
            function(listOf(), context)
        }.also {
            assertThat(it).hasMessageThat().contains(
                "Parameter at position 1 is null or is not of expected type: ${IntValue::class}"
            )
        }

        assertThrows(IllegalArgumentException::class.java) {
            function(listOf(42L.toValue()), context)
        }.also {
            assertThat(it).hasMessageThat().contains(
                "Parameter at position 1 is null or is not of expected type: ${IntValue::class}"
            )
        }
    }

    @Test
    fun hostFunction_twoParams() {
        val function = HostFunction { a: IntValue, b: FloatValue, context ->
            assertThat(context).isSameInstanceAs(this@HostFunctionTest.context)
            (a.value * b.value).toValue()
        }

        assertThat(
            function(listOf(42.toValue(), 1337.0f.toValue()), context)
        ).isEqualTo((42 * 1337.0f).toValue())

        assertThat(function.functionType).isEqualTo(
            FunctionType(
                listOf(
                    Param(Identifier.Local(null, 0), ValueType.I32),
                    Param(Identifier.Local(null, 1), ValueType.F32)
                ),
                listOf(
                    Result(ValueType.F32)
                )
            )
        )
    }

    @Test
    fun hostFunction_twoParams_throwsOnBadValue() {
        val function = HostFunction { _: IntValue, _: FloatValue, _ ->
            fail("Should not be called")
            3L.toValue()
        }

        assertThrows(IllegalArgumentException::class.java) {
            function(listOf(), context)
        }.also {
            assertThat(it).hasMessageThat().contains(
                "Parameter at position 1 is null or is not of expected type: ${IntValue::class}"
            )
        }

        assertThrows(IllegalArgumentException::class.java) {
            function(listOf(42L.toValue(), 1337f.toValue()), context)
        }.also {
            assertThat(it).hasMessageThat().contains(
                "Parameter at position 1 is null or is not of expected type: ${IntValue::class}"
            )
        }

        assertThrows(IllegalArgumentException::class.java) {
            function(listOf(42.toValue()), context)
        }.also {
            assertThat(it).hasMessageThat().contains(
                "Parameter at position 2 is null or is not of expected type: ${FloatValue::class}"
            )
        }

        assertThrows(IllegalArgumentException::class.java) {
            function(listOf(42.toValue(), 1337.0.toValue()), context)
        }.also {
            assertThat(it).hasMessageThat().contains(
                "Parameter at position 2 is null or is not of expected type: ${FloatValue::class}"
            )
        }
    }

    @Test
    fun hostFunction_threeParams() {
        val function = HostFunction { a: IntValue, b: FloatValue, c: DoubleValue, context ->
            assertThat(context).isSameInstanceAs(context)
            (a.value * b.value / c.value).toValue()
        }

        assertThat(
            function(listOf(42.toValue(), 1337.0f.toValue(), 80081335.0.toValue()), context)
        ).isEqualTo((42 * 1337.0f / 80081335.0).toValue())

        assertThat(function.functionType).isEqualTo(
            FunctionType(
                listOf(
                    Param(Identifier.Local(null, 0), ValueType.I32),
                    Param(Identifier.Local(null, 1), ValueType.F32),
                    Param(Identifier.Local(null, 1), ValueType.F64)
                ),
                listOf(
                    Result(ValueType.F64)
                )
            )
        )
    }

    @Test
    fun hostFunction_threeParams_throwsOnBadValue() {
        val function = HostFunction { _: IntValue, _: FloatValue, _: DoubleValue, _ ->
            fail("Should not be called")
            1L.toValue()
        }

        assertThrows(IllegalArgumentException::class.java) {
            function(listOf(), context)
        }.also {
            assertThat(it).hasMessageThat().contains(
                "Parameter at position 1 is null or is not of expected type: ${IntValue::class}"
            )
        }

        assertThrows(IllegalArgumentException::class.java) {
            function(listOf(42L.toValue(), 1337f.toValue(), 80081335.0.toValue()), context)
        }.also {
            assertThat(it).hasMessageThat().contains(
                "Parameter at position 1 is null or is not of expected type: ${IntValue::class}"
            )
        }

        assertThrows(IllegalArgumentException::class.java) {
            function(listOf(42.toValue()), context)
        }.also {
            assertThat(it).hasMessageThat().contains(
                "Parameter at position 2 is null or is not of expected type: ${FloatValue::class}"
            )
        }

        assertThrows(IllegalArgumentException::class.java) {
            function(listOf(42.toValue(), 1337.0.toValue(), 80081335.0.toValue()), context)
        }.also {
            assertThat(it).hasMessageThat().contains(
                "Parameter at position 2 is null or is not of expected type: ${FloatValue::class}"
            )
        }

        assertThrows(IllegalArgumentException::class.java) {
            function(listOf(42.toValue(), 1337.0f.toValue()), context)
        }.also {
            assertThat(it).hasMessageThat().contains(
                "Parameter at position 3 is null or is not of expected type: ${DoubleValue::class}"
            )
        }

        assertThrows(IllegalArgumentException::class.java) {
            function(listOf(42.toValue(), 1337.0f.toValue(), 1.toValue()), context)
        }.also {
            assertThat(it).hasMessageThat().contains(
                "Parameter at position 3 is null or is not of expected type: ${DoubleValue::class}"
            )
        }
    }

    @Test
    fun executing_unitHostFunction_executes_doesntPushToStack() {
        var called = false
        var result = UnitHostFunction { hostContext ->
            called = true
            assertThat(hostContext.memory).isNotNull()
            assertThat(hostContext.memory?.readInt(0)).isEqualTo(42)
        }.toFunctionInstance().execute(executionContext)

        assertThat(result.stacks.operands.height).isEqualTo(0)
        assertThat(called).isTrue()

        result = UnitHostFunction { x: IntValue, hostContext ->
            called = true
            assertThat(x.value).isEqualTo(1)
            assertThat(hostContext.memory).isNotNull()
            assertThat(hostContext.memory?.readInt(0)).isEqualTo(42)
        }.toFunctionInstance()
            .execute(executionContext.also { it.stacks.operands.push(1.toValue()) })
        // Should've popped the value from the stack
        assertThat(result.stacks.operands.height).isEqualTo(0)
        assertThat(called).isTrue()
    }

    @Test
    fun executing_hostFunction_executes_pushesResultToStack() {
        var called = false
        var result = HostFunction { hostContext ->
            called = true
            assertThat(hostContext.memory).isNotNull()
            assertThat(hostContext.memory?.readInt(0)).isEqualTo(42)
            1337.toValue()
        }.toFunctionInstance().execute(executionContext)
        assertThat(result.stacks.operands.height).isEqualTo(1)
        assertThat(result.stacks.operands.peek()).isEqualTo(1337.toValue())
        assertThat(called).isTrue()

        result = HostFunction { x: IntValue, y: IntValue, hostContext ->
            called = true
            assertThat(x.value).isEqualTo(2)
            assertThat(y.value).isEqualTo(3)
            assertThat(hostContext.memory).isNotNull()
            assertThat(hostContext.memory?.readInt(0)).isEqualTo(42)
            (x.value * y.value).toValue()
        }.toFunctionInstance()
            .execute(
                executionContext.also {
                    it.stacks.operands.push(2.toValue())
                    it.stacks.operands.push(3.toValue())
                }
            )
        // Should've returned
        assertThat(result.stacks.operands.height).isEqualTo(1)
        assertThat(result.stacks.operands.peek()).isEqualTo(6.toValue())
        assertThat(called).isTrue()
    }

    private val executionContext: ExecutionContext
        get() {
            val moduleInstance = ModuleInstance(
                TypeIndex(),
                AddressIndex(),
                AddressIndex(),
                AddressIndex(listOf(Address.Memory(0))),
                AddressIndex(),
                emptyList()
            )
            return ExecutionContext(
                Store(memories = listOf(ByteBufferMemory().also { it.writeInt(42, 0) })),
                moduleInstance,
                RuntimeStacks(
                    activations = ActivationStack().also {
                        it.push(
                            Activation(
                                "foo".toFunctionIndex(),
                                LocalIndex(),
                                moduleInstance
                            )
                        )
                    }
                )
            )
        }

    private class Context(override val memory: Memory?) : HostFunctionContext
}
