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
import kwasm.runtime.Memory
import kwasm.runtime.memory.ByteBufferMemory
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.lang.IllegalArgumentException

@RunWith(JUnit4::class)
class ByteBufferMemoryProviderTest {
    @Test
    fun throws_when_requestedMinPages_isNonPositive() {
        val provider = ByteBufferMemoryProvider(1024 * 1024) // 1mb

        assertThrows(IllegalArgumentException::class.java) {
            provider.buildMemory(-1, null)
        }.also {
            assertThat(it).hasMessageThat()
                .contains("requestedMinPages must be greater than or equal to 0")
        }
    }

    @Test
    fun throws_when_requestedMaxPages_cantFitInMaxAllowed() {
        val provider = ByteBufferMemoryProvider(1024 * 1024) // 1mb
        val providerMaxPages = Memory.pagesForBytes(1024 * 1024)

        assertThrows(IllegalStateException::class.java) {
            provider.buildMemory(1, providerMaxPages + 1)
        }.also {
            assertThat(it).hasMessageThat()
                .contains(
                    "Cannot fit requestedMaxPages: ${providerMaxPages + 1} into maximum allowed" +
                        " size: ${1024 * 1024} bytes"
                )
        }
    }

    @Test
    fun uses_maximumSizeBytes_whenRequestedMax_isNull() {
        val provider = ByteBufferMemoryProvider(1024 * 1024) // 1mb
        val providerMaxPages = Memory.pagesForBytes(1024 * 1024)

        val memory =
            provider.buildMemory(1, null) as ByteBufferMemory

        assertThat(memory.maximumPages).isEqualTo(providerMaxPages)
        assertThat(memory.sizePages).isEqualTo(1)
    }

    @Test
    fun validInputs() {
        val provider = ByteBufferMemoryProvider(1024 * 1024) // 1mb

        val memory =
            provider.buildMemory(2, 3) as ByteBufferMemory

        assertThat(memory.maximumPages).isEqualTo(3)
        assertThat(memory.sizePages).isEqualTo(2)
    }
}
