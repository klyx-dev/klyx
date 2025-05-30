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
import kwasm.api.ByteBufferMemoryProvider
import kwasm.ast.type.Limits
import kwasm.ast.type.MemoryType
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class MemoryTest {
    @Test
    fun allocate() {
        val store = Store()
        val provider = ByteBufferMemoryProvider(1024 * 64)

        store.allocate(provider, MemoryType(Limits(1, null))).also { (newStore, addr) ->
            assertThat(addr).isEqualTo(Address.Memory(0))
            assertThat(newStore.memories).hasSize(1)
            assertThat(newStore.memories[0].sizePages).isEqualTo(1)
        }
    }
}
