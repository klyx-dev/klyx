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

package kwasm.format.binary

import kwasm.format.ParseException
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.io.ByteArrayInputStream

@RunWith(JUnit4::class)
class BinaryParserTest {
    @Test
    fun readByte_throwsWhenNoBytesLeft() {
        val bytes = listOf(0x00).toByteArray()
        val parser = BinaryParser(ByteArrayInputStream(bytes))
        parser.readByte()

        assertThrows(ParseException::class.java) { parser.readByte() }
    }

    @Test
    fun readFourBytes_throwsWhenNoBytesLeft() {
        val bytes = listOf(0x00, 0x00, 0x00, 0x00).toByteArray()
        val parser = BinaryParser(ByteArrayInputStream(bytes))
        parser.readFourBytes()

        assertThrows(ParseException::class.java) { parser.readFourBytes() }
    }

    @Test
    fun readFourBytes_throwsWhenNotEnoughBytesLeft() {
        val bytes = listOf(0x00, 0x00, 0x00, 0x00).toByteArray()
        val parser = BinaryParser(ByteArrayInputStream(bytes))
        parser.readByte()

        assertThrows(ParseException::class.java) { parser.readFourBytes() }
    }

    @Test
    fun readEightBytes_throwsWhenNoBytesLeft() {
        val bytes = listOf(0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00).toByteArray()
        val parser = BinaryParser(ByteArrayInputStream(bytes))
        parser.readEightBytes()

        assertThrows(ParseException::class.java) { parser.readEightBytes() }
    }

    @Test
    fun readEightBytes_throwsWhenNotEnoughBytesLeft() {
        val bytes = listOf(0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00).toByteArray()
        val parser = BinaryParser(ByteArrayInputStream(bytes))
        parser.readByte()

        assertThrows(ParseException::class.java) { parser.readEightBytes() }
    }
}
