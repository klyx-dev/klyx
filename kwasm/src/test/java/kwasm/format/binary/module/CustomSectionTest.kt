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

package kwasm.format.binary.module

import com.google.common.truth.Truth.assertThat
import kwasm.format.ParseException
import kwasm.format.binary.BinaryParser
import kwasm.format.binary.toByteArray
import kwasm.util.Leb128
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.io.ByteArrayInputStream
import kotlin.random.Random

@Suppress("EXPERIMENTAL_API_USAGE")
@RunWith(JUnit4::class)
class CustomSectionTest {
    @Test
    fun nonExistent() {
        val bytes = ByteArray(0)
        val parser = BinaryParser(ByteArrayInputStream(bytes))
        assertThat(parser.readSection()).isNull()
    }

    @Test
    fun emptyCustomSection_fails() {
        val bytes = listOf(0x00, 0x00).toByteArray()
        val parser = BinaryParser(ByteArrayInputStream(bytes))
        val e = assertThrows(ParseException::class.java) {
            parser.readSection()
        }
        assertThat(e).hasMessageThat().contains("Expected byte")
    }

    @Test
    fun invalidSize_fails() {
        val bytes = listOf(0x00, 0x03, 0x01, 0x42).toByteArray()
        val parser = BinaryParser(ByteArrayInputStream(bytes))
        val e = assertThrows(ParseException::class.java) {
            parser.readSection()
        }
        assertThat(e).hasMessageThat().contains("EOF")
    }

    @Test
    fun readCustomSection_random() {
        val random = Random(System.currentTimeMillis())
        val name = "Random Custom Section"
        val randomContents = random.nextBytes(random.nextInt(8193))
        val expected = CustomSection(name, randomContents)

        val nameLengthBytes = Leb128.encodeUnsigned(name.length).map { it.toInt() }
        val nameBytes = nameLengthBytes + name.toByteArray(Charsets.UTF_8).map { it.toInt() }
        val byteSequence =
            nameBytes + randomContents.map { it.toInt() }

        val contentBytes = byteSequence.toList()
        val contentLengthBytes = Leb128.encodeUnsigned(contentBytes.size).map { it.toInt() }

        val sectionBytes = listOf(0x00) + contentLengthBytes + contentBytes
        val parser = BinaryParser(ByteArrayInputStream(sectionBytes.toByteArray()))
        val actual = parser.readSection()
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun equalsHashcodeToString() {
        val random = Random(System.currentTimeMillis())
        val bytes = random.nextBytes(100)
        val bytesCopy = bytes.copyOf()
        val section = CustomSection("Foo", bytes)
        val section2 = CustomSection("Foo", bytesCopy)

        assertThat(section2).isEqualTo(section)
        assertThat(section2.hashCode()).isEqualTo(section.hashCode())
        assertThat(section.toString()).isEqualTo("CustomSection(name=\"Foo\", data=100 bytes)")
    }
}
