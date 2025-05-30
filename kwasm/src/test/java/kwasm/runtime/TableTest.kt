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
import kwasm.ast.type.ElementType
import kwasm.ast.type.Limits
import kwasm.ast.type.TableType
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class TableTest {
    @Test
    fun constructor_throws_ifElementsSize_greaterThanMaxSize() {
        assertThrows(IllegalArgumentException::class.java) {
            Table(
                mutableMapOf(
                    0 to Address.Function(0),
                    1 to Address.Function(1)
                ),
                maxSize = 1
            )
        }.also {
            assertThat(it).hasMessageThat()
                .contains("Elements in table cannot exceed the maximum size: 1")
        }
    }

    @Test
    fun pseudoConstructor() {
        val table = Table(TableType(Limits(0L, 10L), ElementType.FunctionReference)) {
            it += 0 to Address.Function(1)
        }
        assertThat(table.elements).containsExactly(0, Address.Function(1))
        assertThat(table.maxSize).isEqualTo(10)
    }

    @Test
    fun allocate() {
        val store = Store()

        store.allocate(TableType(Limits(0, 25L), ElementType.FunctionReference))
            .also { (newStore, addr) ->
                assertThat(addr.value).isEqualTo(0)
                assertThat(newStore.tables).hasSize(1)
                assertThat(newStore.tables[0])
                    .isEqualTo(Table(TableType(Limits(0, 25L), ElementType.FunctionReference)))
            }
    }
}
