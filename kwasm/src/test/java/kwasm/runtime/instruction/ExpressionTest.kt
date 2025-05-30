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
import kwasm.KWasmRuntimeException
import kwasm.ParseRule
import kwasm.runtime.Address
import kwasm.runtime.EmptyExecutionContext
import kwasm.runtime.Global
import kwasm.runtime.Store
import kwasm.runtime.toValue
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ExpressionTest {
    @get:Rule
    val parser = ParseRule()

    @Test
    fun throws_ifExpressionValue_isEmpty() = parser.with {
        val expression =
            """
            (global.get 0)
            (global.set 0)
        """.parseExpression()

        val context = EmptyExecutionContext().let {
            it.moduleInstance.globalAddresses.add(Address.Global(0))
            it.copy(
                store = Store(globals = listOf(Global.Int(10, true)))
            )
        }

        val e = assertThrows(KWasmRuntimeException::class.java) {
            expression.execute(context)
        }
        assertThat(e).hasMessageThat()
            .contains("Expression expected to produce a value, but stack is empty")
    }

    @Test
    fun valid() = parser.with {
        val expression =
            """
            (global.get 0)
        """.parseExpression()

        val context = EmptyExecutionContext().let {
            it.moduleInstance.globalAddresses.add(Address.Global(0))
            it.copy(
                store = Store(globals = listOf(Global.Int(10, true)))
            )
        }

        val postExecutionContext = expression.execute(context)
        assertThat(postExecutionContext.stacks.operands.height).isEqualTo(1)
        assertThat(postExecutionContext.stacks.operands.pop()).isEqualTo(10.toValue())
    }
}
