/*
 * Copyright 2021 Google LLC
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

package kwasm.util

import kotlin.experimental.or

/**
 * Encapsulates LEB-128 integer encoding logic.
 *
 * For more details, see [the wikipedia page](https://en.wikipedia.org/wiki/LEB128).
 */
object Leb128 {
    /**
     * Encodes the provided [value] as an unsigned integer using LEB128 into a sequence of [Byte]s.
     */
    fun encodeUnsigned(value: Int): Sequence<Byte> = sequence {
        var valueLeft = value
        do {
            val byte = valueLeft and 0x7F
            valueLeft = valueLeft ushr 7
            yield(if (valueLeft != 0) (byte or 0x80).toByte() else byte.toByte())
        } while (valueLeft != 0)
    }
}
