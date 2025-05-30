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
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class StoreTest {
    @Test
    fun allocateGlobal() {
        var store = Store()

        store = store.allocateGlobal(Global.Int(0, true))
            .let { (newStore, address) ->
                assertThat(newStore).isNotSameInstanceAs(store)
                assertThat(newStore.globals).hasSize(1)
                assertThat(address.value).isEqualTo(0)
                newStore
            }
        store = store.allocateGlobal(Global.Float(0f, true))
            .let { (newStore, address) ->
                assertThat(newStore).isNotSameInstanceAs(store)
                assertThat(newStore.globals).hasSize(2)
                assertThat(address.value).isEqualTo(1)
                newStore
            }
    }

    @Test
    fun allocateTable() {
        val store = Store()
        store.allocateTable(Table(mutableMapOf(), 1))
            .also { (newStore, address) ->
                assertThat(newStore).isNotSameInstanceAs(store)
                assertThat(newStore.tables).hasSize(1)
                assertThat(address.value).isEqualTo(0)
            }
    }
}
