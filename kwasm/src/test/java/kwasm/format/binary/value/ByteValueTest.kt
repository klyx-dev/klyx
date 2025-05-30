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
import kwasm.format.ParseException
import kwasm.format.binary.BinaryParser
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.io.ByteArrayInputStream

@RunWith(JUnit4::class)
class ByteValueTest {
    @Test
    fun readsByte() {
        val bytes = ByteArray(1) { 255.toByte() }
        val parser = BinaryParser(ByteArrayInputStream(bytes))

        assertThat(parser.readByte()).isEqualTo(bytes[0])
    }

    @Test
    fun readsBytes() {
        val values = (0..9).map { it.toByte() }
        val bytes = ByteArray(values.size) { values[it] }
        val parser = BinaryParser(ByteArrayInputStream(bytes))

        values.forEach {
            assertThat(parser.readByte()).isEqualTo(it)
        }
    }

    @Test
    fun throwsWhenNoByteAvailable() {
        val bytes = ByteArray(1) { 1.toByte() }
        val parser = BinaryParser(ByteArrayInputStream(bytes))

        parser.readByte()

        assertThrows(ParseException::class.java) { parser.readByte() }
    }
}
