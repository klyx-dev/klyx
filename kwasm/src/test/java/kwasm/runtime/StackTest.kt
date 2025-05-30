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
import kwasm.runtime.stack.BaseStack
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class StackTest {
    @Test
    fun popUntil_returnsNull_ifStackEmpty() {
        val stack = StackImpl()

        val evenValue = stack.popUntil { (it as IntValue).value % 2 == 0 }
        assertThat(evenValue).isNull()
    }

    @Test
    fun popUntil_returnsNull_ifPredicateIsNeverTrue() {
        val stack = StackImpl()
        stack.push(IntValue(3))
        stack.push(IntValue(9))
        stack.push(IntValue(5))
        stack.push(IntValue(7))
        stack.push(IntValue(1))

        val evenValue = stack.popUntil { (it as IntValue).value % 2 == 0 }
        assertThat(evenValue).isNull()
        assertThat(stack.height).isEqualTo(0)
    }

    @Test
    fun popUntil_popsUntilPredicateIsTrue() {
        val stack = StackImpl()
        stack.push(IntValue(3))
        stack.push(IntValue(2))
        stack.push(IntValue(5))
        stack.push(IntValue(7))
        stack.push(IntValue(1))

        val evenValue = stack.popUntil { (it as IntValue).value % 2 == 0 }
        assertThat(evenValue?.value).isEqualTo(2)
        assertThat(stack.height).isEqualTo(1)
    }

    private class StackImpl(
        initialValues: List<Value<*>> = emptyList(),
        maxCapacity: Int = DEFAULT_MAX_CAPACITY
    ) : BaseStack<Value<*>>("StackImpl", initialValues, maxCapacity)
}
