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
import kwasm.format.binary.toByteArray
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.io.ByteArrayInputStream

@Suppress("EXPERIMENTAL_UNSIGNED_LITERALS")
@RunWith(JUnit4::class)
class IntegerValueTest {
    @Test
    fun readUInt_throwsOnUnusedOnes() {
        val bytes = listOf(0x82, 0x80, 0x80, 0x80, 0x10).toByteArray()
        val parser = BinaryParser(ByteArrayInputStream(bytes))

        val e = assertThrows(ParseException::class.java) {
            parser.readUInt()
        }
    }

    @Test
    fun readULong_throwsOnUnusedOnes() {
        val bytes = listOf(0x82, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x80, 0x7F).toByteArray()
        val parser = BinaryParser(ByteArrayInputStream(bytes))

        val e = assertThrows(ParseException::class.java) {
            parser.readUInt()
        }
    }

    @Test
    fun readUInt_readsZero() {
        val bytes = ByteArray(1) { 0x00 }
        val parser = BinaryParser(ByteArrayInputStream(bytes))

        assertThat(parser.readUInt()).isEqualTo(0)
    }

    @Test
    fun readUInt_reads127() {
        val bytes = ByteArray(1) { 0b1111111 }
        val parser = BinaryParser(ByteArrayInputStream(bytes))

        assertThat(parser.readUInt()).isEqualTo(127)
    }

    @Test
    fun readUInt_reads3OneByte() {
        val bytes = ByteArray(1) { 0x03 }
        val parser = BinaryParser(ByteArrayInputStream(bytes))

        assertThat(parser.readUInt()).isEqualTo(3)
    }

    @Test
    fun readUInt_reads3TwoBytes() {
        val bytes = listOf(0x83, 0x00).toByteArray()
        val parser = BinaryParser(ByteArrayInputStream(bytes))

        assertThat(parser.readUInt()).isEqualTo(3)
    }

    @Test
    fun readInt_readsZero() {
        val bytes = ByteArray(1) { 0b0 }
        val parser = BinaryParser(ByteArrayInputStream(bytes))

        assertThat(parser.readInt()).isEqualTo(0)
    }

    @Test
    fun readInt_reads127AsNeg1() {
        val bytes = ByteArray(1) { 0b1111111 }
        val parser = BinaryParser(ByteArrayInputStream(bytes))

        assertThat(parser.readInt()).isEqualTo(-1)
    }

    @Test
    fun readInt_readsNegativeTwo_oneByte() {
        val bytes = listOf(0x7e).toByteArray()
        val parser = BinaryParser(ByteArrayInputStream(bytes))

        assertThat(parser.readInt()).isEqualTo(-2)
    }

    @Test
    fun readInt_readsNegativeTwo_twoBytes() {
        val bytes = listOf(0xFE, 0x7F).toByteArray()
        val parser = BinaryParser(ByteArrayInputStream(bytes))

        assertThat(parser.readInt()).isEqualTo(-2)
    }

    @Test
    fun readInt_readsNegativeTwo_threeBytes() {
        val bytes = listOf(0xFE, 0xFF, 0x7F).toByteArray()
        val parser = BinaryParser(ByteArrayInputStream(bytes))

        assertThat(parser.readInt()).isEqualTo(-2)
    }

    @Test
    fun readULong_readsZero() {
        val bytes = ByteArray(1) { 0x00 }
        val parser = BinaryParser(ByteArrayInputStream(bytes))

        assertThat(parser.readULong()).isEqualTo(0)
    }

    @Test
    fun readULong_reads127() {
        val bytes = ByteArray(1) { 0b1111111 }
        val parser = BinaryParser(ByteArrayInputStream(bytes))

        assertThat(parser.readULong()).isEqualTo(127)
    }

    @Test
    fun readULong_reads3OneByte() {
        val bytes = ByteArray(1) { 0x03 }
        val parser = BinaryParser(ByteArrayInputStream(bytes))

        assertThat(parser.readULong()).isEqualTo(3)
    }

    @Test
    fun readULong_reads3TwoBytes() {
        val bytes = listOf(0x83, 0x00).toByteArray()
        val parser = BinaryParser(ByteArrayInputStream(bytes))

        assertThat(parser.readULong()).isEqualTo(3)
    }

    @Test
    fun readLong_readsZero() {
        val bytes = ByteArray(1) { 0b0 }
        val parser = BinaryParser(ByteArrayInputStream(bytes))

        assertThat(parser.readLong()).isEqualTo(0)
    }

    @Test
    fun readLong_reads127AsNeg1() {
        val bytes = ByteArray(1) { 0b1111111 }
        val parser = BinaryParser(ByteArrayInputStream(bytes))

        assertThat(parser.readLong()).isEqualTo(-1)
    }

    @Test
    fun readLong_readsNegativeTwo_oneByte() {
        val bytes = listOf(0x7e).toByteArray()
        val parser = BinaryParser(ByteArrayInputStream(bytes))

        assertThat(parser.readLong()).isEqualTo(-2)
    }

    @Test
    fun readLong_readsNegativeTwo_twoBytes() {
        val bytes = listOf(0xFE, 0x7F).toByteArray()
        val parser = BinaryParser(ByteArrayInputStream(bytes))

        assertThat(parser.readLong()).isEqualTo(-2)
    }

    @Test
    fun readLong_readsNegativeTwo_threeBytes() {
        val bytes = listOf(0xFE, 0xFF, 0x7F).toByteArray()
        val parser = BinaryParser(ByteArrayInputStream(bytes))

        assertThat(parser.readLong()).isEqualTo(-2)
    }
}
