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

import kwasm.runtime.Memory
import kwasm.runtime.memory.ByteBufferMemory

/**
 * KWasm users can implement [MemoryProvider] to customize the instances of [Memory] which are
 * provided to WebAssembly modules running in KWasm.
 */
interface MemoryProvider {
    fun buildMemory(requestedMinPages: Int, requestedMaxPages: Int?): Memory
}

/** Implementation of [MemoryProvider] which builds blank [ByteBufferMemory] memories. */
open class ByteBufferMemoryProvider(val maximumSizeBytes: Long) : MemoryProvider {
    override fun buildMemory(requestedMinPages: Int, requestedMaxPages: Int?): Memory {
        require(requestedMinPages >= 0) {
            "requestedMinPages must be greater than or equal to 0"
        }

        val maxPages = requestedMaxPages?.let {
            check(Memory.pagesForBytes(maximumSizeBytes) >= it) {
                "Cannot fit requestedMaxPages: $it into maximum allowed size: " +
                    "$maximumSizeBytes bytes"
            }
            it
        } ?: Memory.pagesForBytes(maximumSizeBytes)

        return ByteBufferMemory(maxPages, initialPages = requestedMinPages)
    }
}
