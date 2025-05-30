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

package kwasm.format.binary.value

import com.google.common.truth.Truth.assertThat
import kwasm.format.binary.BinaryParser
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.io.ByteArrayInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

@RunWith(JUnit4::class)
class FloatingPointValueTest {
    @Test
    fun readFloat() {
        val bytes = ByteArray(4)
        val buf = ByteBuffer.wrap(bytes)
        buf.order(ByteOrder.LITTLE_ENDIAN)
        buf.putFloat(-0.5f)
        val parser = BinaryParser(ByteArrayInputStream(bytes))
        assertThat(parser.readFloat()).isEqualTo(-0.5f)
    }

    @Test
    fun readDouble() {
        val bytes = ByteArray(8)
        val buf = ByteBuffer.wrap(bytes)
        buf.order(ByteOrder.LITTLE_ENDIAN)
        buf.putDouble(-0.5)
        val parser = BinaryParser(ByteArrayInputStream(bytes))
        assertThat(parser.readDouble()).isEqualTo(-0.5)
    }
}
