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

package kwasm.runtime.stack

import com.google.common.truth.Truth.assertThat
import kwasm.runtime.DoubleValue
import kwasm.runtime.FloatValue
import kwasm.runtime.IntValue
import kwasm.runtime.LongValue
import kwasm.runtime.Value
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class BaseStackTest {
    @Test
    fun constructor_tooManyInitialValues() {
        val e = assertThrows(IllegalArgumentException::class.java) {
            StackImpl(
                listOf(
                    IntValue(10),
                    LongValue(64L),
                    FloatValue(10.0f),
                    DoubleValue(42.0)
                ),
                maxCapacity = 3
            )
        }

        assertThat(e).hasMessageThat()
            .contains("Initial values for stack: StackImpl exceeds maximum capacity: 3")
    }

    @Test
    fun constructor_notTooManyInitialValues() {
        val stack = StackImpl(
            listOf(
                IntValue(10),
                LongValue(64L),
                FloatValue(10.0f),
                DoubleValue(42.0)
            )
        )

        assertThat(stack.height).isEqualTo(4)
        assertThat(stack.peek()).isEqualTo(DoubleValue(42.0))
    }

    @Test
    fun push_pushesItemOnTop() {
        val stack = StackImpl()

        stack.push(IntValue(10))
        assertThat(stack.peek()).isEqualTo(IntValue(10))

        stack.push(LongValue(10L))
        assertThat(stack.peek()).isEqualTo(LongValue(10L))
    }

    @Test
    fun push_throws_ifStackIsTooBig() {
        val stack = StackImpl(emptyList(), 1)
        stack.push(IntValue(10))

        val e = assertThrows(IllegalStateException::class.java) {
            stack.push(IntValue(42))
        }
        assertThat(e).hasMessageThat()
            .contains("Stack overflow for stack: StackImpl, exceeded max capacity: 1")
    }

    @Test
    fun pop_throws_ifStackIsEmpty() {
        val stack = StackImpl()

        assertThrows(IllegalStateException::class.java) { stack.pop() }
            .also { assertThat(it).hasMessageThat().contains("Stack: StackImpl is empty") }

        stack.push(IntValue(10))
        stack.pop()

        assertThrows(IllegalStateException::class.java) { stack.pop() }
            .also { assertThat(it).hasMessageThat().contains("Stack: StackImpl is empty") }
    }

    @Test
    fun pop_popsFromTop() {
        val stack = StackImpl()

        stack.push(IntValue(1))
        stack.push(IntValue(2))
        stack.push(IntValue(3))

        assertThat(stack.pop()).isEqualTo(IntValue(3))
        assertThat(stack.pop()).isEqualTo(IntValue(2))
        assertThat(stack.pop()).isEqualTo(IntValue(1))
    }

    @Test
    fun peek_peeksAtTop_withoutRemoving() {
        val stack = StackImpl()
        stack.push(IntValue(1))
        assertThat(stack.peek()).isEqualTo(IntValue(1))
        assertThat(stack.height).isEqualTo(1)
        stack.push(IntValue(2))
        assertThat(stack.peek()).isEqualTo(IntValue(2))
        assertThat(stack.height).isEqualTo(2)
        stack.push(IntValue(3))
        assertThat(stack.peek()).isEqualTo(IntValue(3))
        assertThat(stack.height).isEqualTo(3)
    }

    @Test
    fun peek_returnsNull_ifStackEmpty() {
        val stack = StackImpl()

        assertThat(stack.peek()).isNull()
    }

    @Test
    fun height_reflectsHeight() {
        val stack = StackImpl()

        assertThat(stack.height).isEqualTo(0)
        stack.push(IntValue(1))
        assertThat(stack.height).isEqualTo(1)
        stack.push(IntValue(1))
        assertThat(stack.height).isEqualTo(2)
        stack.push(IntValue(1))
        assertThat(stack.height).isEqualTo(3)
    }

    private class StackImpl(
        initialValues: List<Value<*>> = emptyList(),
        maxCapacity: Int = DEFAULT_MAX_CAPACITY
    ) : BaseStack<Value<*>>("StackImpl", initialValues, maxCapacity)
}
