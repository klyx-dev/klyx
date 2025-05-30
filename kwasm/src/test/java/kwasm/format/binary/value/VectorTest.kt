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
import kwasm.format.binary.toByteArray
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.io.ByteArrayInputStream
import kotlin.random.Random

@RunWith(JUnit4::class)
class VectorTest {
    @Test
    fun readVector_empty() {
        val bytes = listOf(0x00, 0x10).toByteArray()
        val parser = BinaryParser(ByteArrayInputStream(bytes))
        assertThat(parser.readVector()).isEmpty()
    }

    @Test
    fun readVector_singleItem() {
        val bytes = listOf(0x01, 0x10)
        val parser = BinaryParser(ByteArrayInputStream(bytes.toByteArray()))
        assertThat(parser.readVector()).containsExactly(0x10.toByte()).inOrder()
    }

    @Test
    fun readVector_255Items() {
        val random = Random(System.currentTimeMillis())
        val randomBytes = random.nextBytes(255).map { it.toInt() }.toList()
        val bytes = listOf(0xFF, 0x01) + randomBytes
        val parser = BinaryParser(ByteArrayInputStream(bytes.toByteArray()))
        assertThat(parser.readVector())
            .containsExactlyElementsIn(randomBytes.map { it.toByte() }).inOrder()
    }
}
