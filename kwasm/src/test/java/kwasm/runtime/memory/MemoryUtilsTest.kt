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

package kwasm.runtime.memory

import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@Suppress("EXPERIMENTAL_API_USAGE", "EXPERIMENTAL_UNSIGNED_LITERALS")
@RunWith(JUnit4::class)
class MemoryUtilsTest {
    @Test
    fun assertValidByteWidth_int() {
        1.assertValidByteWidth<Int>()
        2.assertValidByteWidth<Int>()
        4.assertValidByteWidth<Int>()
    }

    @Test
    fun assertValidByteWidth_int_invalid() {
        assertThrows(IllegalArgumentException::class.java) {
            0.assertValidByteWidth<Int>()
        }
        assertThrows(IllegalArgumentException::class.java) {
            (-1).assertValidByteWidth<Int>()
        }
        assertThrows(IllegalArgumentException::class.java) {
            3.assertValidByteWidth<Int>()
        }
        assertThrows(IllegalArgumentException::class.java) {
            8.assertValidByteWidth<Int>()
        }
    }

    @Test
    fun assertValidByteWidth_float() {
        1.assertValidByteWidth<Float>()
        2.assertValidByteWidth<Float>()
        4.assertValidByteWidth<Float>()
    }

    @Test
    fun assertValidByteWidth_float_invalid() {
        assertThrows(IllegalArgumentException::class.java) {
            0.assertValidByteWidth<Float>()
        }
        assertThrows(IllegalArgumentException::class.java) {
            (-1).assertValidByteWidth<Float>()
        }
        assertThrows(IllegalArgumentException::class.java) {
            3.assertValidByteWidth<Float>()
        }
        assertThrows(IllegalArgumentException::class.java) {
            8.assertValidByteWidth<Float>()
        }
    }

    @Test
    fun assertValidByteWidth_long() {
        1.assertValidByteWidth<Long>()
        2.assertValidByteWidth<Long>()
        4.assertValidByteWidth<Long>()
        8.assertValidByteWidth<Long>()
    }

    @Test
    fun assertValidByteWidth_long_invalid() {
        assertThrows(IllegalArgumentException::class.java) {
            0.assertValidByteWidth<Long>()
        }
        assertThrows(IllegalArgumentException::class.java) {
            (-1).assertValidByteWidth<Long>()
        }
        assertThrows(IllegalArgumentException::class.java) {
            3.assertValidByteWidth<Long>()
        }
        assertThrows(IllegalArgumentException::class.java) {
            16.assertValidByteWidth<Long>()
        }
    }

    @Test
    fun assertValidByteWidth_double() {
        1.assertValidByteWidth<Double>()
        2.assertValidByteWidth<Double>()
        4.assertValidByteWidth<Double>()
        8.assertValidByteWidth<Double>()
    }

    @Test
    fun assertValidByteWidth_double_invalid() {
        assertThrows(IllegalArgumentException::class.java) {
            0.assertValidByteWidth<Double>()
        }
        assertThrows(IllegalArgumentException::class.java) {
            (-1).assertValidByteWidth<Double>()
        }
        assertThrows(IllegalArgumentException::class.java) {
            3.assertValidByteWidth<Double>()
        }
        assertThrows(IllegalArgumentException::class.java) {
            16.assertValidByteWidth<Double>()
        }
    }

    @Test
    fun assertValidByteWidth_illegalValue() {
        assertThrows(Exception::class.java) {
            1.assertValidByteWidth<Byte>()
        }
        assertThrows(Exception::class.java) {
            1.assertValidByteWidth<Short>()
        }
    }

    @Test
    fun intWrap() {
        assertThat(256.wrap(1)).isEqualTo(0)
        assertThat(257.wrap(1)).isEqualTo(1)
        assertThat(65536.wrap(2)).isEqualTo(0)
        assertThat(65537.wrap(2)).isEqualTo(1)
    }

    @Test
    fun longWrap() {
        assertThat(256L.wrap(1)).isEqualTo(0)
        assertThat(257L.wrap(1)).isEqualTo(1)
        assertThat(65536L.wrap(2)).isEqualTo(0)
        assertThat(65537L.wrap(2)).isEqualTo(1)
        assertThat(0x100000000L.wrap(4)).isEqualTo(0)
        assertThat(0x100000001L.wrap(4)).isEqualTo(1)
    }

    @Test
    fun ulongWrap() {
        assertThat(256.toULong().wrap(1)).isEqualTo(0.toULong())
        assertThat(257.toULong().wrap(1)).isEqualTo(1.toULong())
        assertThat(65536.toULong().wrap(2)).isEqualTo(0.toULong())
        assertThat(65537.toULong().wrap(2)).isEqualTo(1.toULong())
        assertThat(0x100000000uL.wrap(4)).isEqualTo(0.toULong())
        assertThat(0x100000001uL.wrap(4)).isEqualTo(1.toULong())
    }

    @Test
    fun toBigEndianInt() {
        val x = byteArrayOf(
            0xF.toByte(),
            0xFF.toByte(),
            0xF.toByte(),
            0xFF.toByte()
        )

        assertThat(x.toBigEndianInt(1)).isEqualTo(0xF)
        assertThat(x.toBigEndianInt(2)).isEqualTo(0xFF0F)
        assertThat(x.toBigEndianInt(4)).isEqualTo(0xFF0FFF0F.toInt())
    }

    @Test
    fun toBigEndianLong() {
        val x = byteArrayOf(
            0xF.toByte(),
            0xFF.toByte(),
            0xF.toByte(),
            0xFF.toByte(),
            0xF.toByte(),
            0xFF.toByte(),
            0xF.toByte(),
            0x7F.toByte()
        )

        assertThat(x.toBigEndianLong(1)).isEqualTo(0xF)
        assertThat(x.toBigEndianLong(2)).isEqualTo(0xFF0F)
        assertThat(x.toBigEndianLong(4)).isEqualTo(0xFF0FFF0F)
        assertThat(x.toBigEndianLong(8)).isEqualTo(0x7F0FFF0FFF0FFF0F)
    }
}
