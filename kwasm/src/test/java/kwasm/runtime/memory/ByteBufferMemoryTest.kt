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
import com.google.common.truth.Truth.assertWithMessage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runBlockingTest
import kwasm.runtime.Memory.Companion.GROW_FAILURE
import kwasm.runtime.Memory.Companion.PAGE_SIZE
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import kotlin.random.Random

@Suppress("EXPERIMENTAL_UNSIGNED_LITERALS", "EXPERIMENTAL_API_USAGE")
@RunWith(JUnit4::class)
class ByteBufferMemoryTest {
    @Test
    fun constructor_validation_illegalMaximumPages_tooSmall() {
        assertThrows(IllegalArgumentException::class.java) {
            ByteBufferMemory(maximumPages = 0)
        }
        assertThrows(IllegalArgumentException::class.java) {
            ByteBufferMemory(maximumPages = -1)
        }
    }

    @Test
    fun constructor_validation_illegalMaximumPages_lessThan_initialPages() {
        assertThrows(IllegalArgumentException::class.java) {
            ByteBufferMemory(maximumPages = 1, initialPages = 2)
        }
    }

    @Test
    fun constructor_validation_illegalMaximumPages_notEnough_forInitialData() {
        assertThrows(IllegalArgumentException::class.java) {
            ByteBufferMemory(
                maximumPages = 1,
                initialPages = 1,
                initialData = ByteArray(PAGE_SIZE * 2)
            )
        }
    }

    @Test
    fun constructor_buildsPagesToFitData_emptyData() {
        val memory = ByteBufferMemory(initialData = ByteArray(0))
        assertThat(memory.sizeBytes).isEqualTo(PAGE_SIZE)
        assertThat(memory.sizePages).isEqualTo(1)
    }

    @Test
    fun constructor_buildsPagesToFitData_singleByte() {
        val memory = ByteBufferMemory(initialData = ByteArray(1) { 1 })
        assertThat(memory.sizeBytes).isEqualTo(PAGE_SIZE)
        assertThat(memory.sizePages).isEqualTo(1)
    }

    @Test
    fun constructor_buildsPagesToFitData_oneFullPage() {
        val memory = ByteBufferMemory(initialData = ByteArray(PAGE_SIZE) { 1 })
        assertThat(memory.sizeBytes).isEqualTo(PAGE_SIZE)
        assertThat(memory.sizePages).isEqualTo(1)
    }

    @Test
    fun constructor_buildsPagesToFitData_barelyTwoPages() {
        val memory = ByteBufferMemory(initialData = ByteArray(PAGE_SIZE + 1) { 1 })
        assertThat(memory.sizeBytes).isEqualTo(PAGE_SIZE * 2)
        assertThat(memory.sizePages).isEqualTo(2)
    }

    @Test
    fun readWrite_int_simple() {
        val memory = ByteBufferMemory()

        memory.writeInt(10, 0)
        assertThat(memory.readInt(0)).isEqualTo(10)

        memory.writeInt(10, 1)
        assertThat(memory.readInt(1)).isEqualTo(10)

        memory.writeInt(1337, PAGE_SIZE / 2)
        assertThat(memory.readInt(PAGE_SIZE / 2)).isEqualTo(1337)

        val twoPages = ByteBufferMemory(initialPages = 2)

        twoPages.writeInt(1337, PAGE_SIZE)
        assertThat(twoPages.readInt(PAGE_SIZE)).isEqualTo(1337)
    }

    @Test
    fun readWrite_int_withNonStandardByteWidth() {
        val memory = ByteBufferMemory()

        memory.writeInt(Int.MAX_VALUE, 0, 1)
        assertThat(memory.readInt(0, 1))
            .isEqualTo(Int.MAX_VALUE.wrap(1).toByte())

        memory.writeInt(Int.MAX_VALUE, 0, 2)
        assertThat(memory.readInt(0, 2))
            .isEqualTo(Int.MAX_VALUE.wrap(2).toShort())
    }

    @Test
    fun readWrite_int_boundaryCross() {
        val memory = ByteBufferMemory(initialPages = 2)

        memory.writeInt(1337, PAGE_SIZE - 3)
        assertThat(memory.readInt(PAGE_SIZE - 3)).isEqualTo(1337)

        memory.writeInt(1337, PAGE_SIZE - 2)
        assertThat(memory.readInt(PAGE_SIZE - 2)).isEqualTo(1337)

        memory.writeInt(1337, PAGE_SIZE - 1)
        assertThat(memory.readInt(PAGE_SIZE - 1)).isEqualTo(1337)
    }

    @Test
    fun readWrite_uint_simple() {
        val memory = ByteBufferMemory()

        memory.writeUInt(10u, 0)
        assertThat(memory.readUInt(0)).isEqualTo(10u)

        memory.writeUInt(10u, 1)
        assertThat(memory.readUInt(1)).isEqualTo(10u)

        memory.writeUInt(1337u, PAGE_SIZE / 2)
        assertThat(memory.readUInt(PAGE_SIZE / 2)).isEqualTo(1337u)

        val twoPages = ByteBufferMemory(initialPages = 2)

        twoPages.writeUInt(1337u, PAGE_SIZE)
        assertThat(twoPages.readUInt(PAGE_SIZE)).isEqualTo(1337u)
    }

    @Test
    fun readWrite_uint_withNonStandardByteWidth() {
        val memory = ByteBufferMemory()

        memory.writeUInt(UInt.MAX_VALUE, 0, 1)
        assertThat(memory.readUInt(0, 1))
            .isEqualTo(UInt.MAX_VALUE.toInt().wrap(1).toUByte().toUInt())

        memory.writeUInt(UInt.MAX_VALUE, 0, 2)
        assertThat(memory.readUInt(0, 2))
            .isEqualTo(UInt.MAX_VALUE.toInt().wrap(2).toUShort().toUInt())
    }

    @Test
    fun readWrite_uint_boundaryCross() {
        val memory = ByteBufferMemory(initialPages = 2)

        memory.writeUInt(1337u, PAGE_SIZE - 3)
        assertThat(memory.readUInt(PAGE_SIZE - 3)).isEqualTo(1337u)

        memory.writeUInt(1337u, PAGE_SIZE - 2)
        assertThat(memory.readUInt(PAGE_SIZE - 2)).isEqualTo(1337u)

        memory.writeUInt(1337u, PAGE_SIZE - 1)
        assertThat(memory.readUInt(PAGE_SIZE - 1)).isEqualTo(1337u)
    }

    @Test
    fun readWrite_long_simple() {
        val memory = ByteBufferMemory()

        memory.writeLong(10, 0)
        assertThat(memory.readLong(0)).isEqualTo(10)

        memory.writeLong(10, 1)
        assertThat(memory.readLong(1)).isEqualTo(10)

        memory.writeLong(1337, PAGE_SIZE / 2)
        assertThat(memory.readLong(PAGE_SIZE / 2)).isEqualTo(1337)

        val twoPages = ByteBufferMemory(initialPages = 2)

        twoPages.writeLong(1337, PAGE_SIZE)
        assertThat(twoPages.readLong(PAGE_SIZE)).isEqualTo(1337)
    }

    @Test
    fun readWrite_long_withNonStandardByteWidth() {
        val memory = ByteBufferMemory()

        memory.writeLong(Long.MAX_VALUE, 0, 1)
        assertThat(memory.readLong(0, 1))
            .isEqualTo(Long.MAX_VALUE.wrap(1).toByte())

        memory.writeLong(Long.MAX_VALUE, 0, 2)
        assertThat(memory.readLong(0, 2))
            .isEqualTo(Long.MAX_VALUE.wrap(2).toShort())

        memory.writeLong(Long.MAX_VALUE, 0, 4)
        assertThat(memory.readLong(0, 4))
            .isEqualTo(Long.MAX_VALUE.wrap(4).toInt())
    }

    @Test
    fun readWrite_long_boundaryCross() {
        val memory = ByteBufferMemory(initialPages = 2)

        memory.writeLong(1337, PAGE_SIZE - 7)
        assertThat(memory.readLong(PAGE_SIZE - 7)).isEqualTo(1337)

        memory.writeLong(1337, PAGE_SIZE - 6)
        assertThat(memory.readLong(PAGE_SIZE - 6)).isEqualTo(1337)

        memory.writeLong(1337, PAGE_SIZE - 5)
        assertThat(memory.readLong(PAGE_SIZE - 5)).isEqualTo(1337)

        memory.writeLong(1337, PAGE_SIZE - 4)
        assertThat(memory.readLong(PAGE_SIZE - 4)).isEqualTo(1337)

        memory.writeLong(1337, PAGE_SIZE - 3)
        assertThat(memory.readLong(PAGE_SIZE - 3)).isEqualTo(1337)

        memory.writeLong(1337, PAGE_SIZE - 2)
        assertThat(memory.readLong(PAGE_SIZE - 2)).isEqualTo(1337)

        memory.writeLong(1337, PAGE_SIZE - 1)
        assertThat(memory.readLong(PAGE_SIZE - 1)).isEqualTo(1337)
    }

    @Test
    fun readWrite_ulong_simple() {
        val memory = ByteBufferMemory()

        memory.writeULong(10u, 0)
        assertThat(memory.readULong(0)).isEqualTo(10.toULong())

        memory.writeULong(10u, 1)
        assertThat(memory.readULong(1)).isEqualTo(10.toULong())

        memory.writeULong(1337u, PAGE_SIZE / 2)
        assertThat(memory.readULong(PAGE_SIZE / 2)).isEqualTo(1337.toULong())

        val twoPages = ByteBufferMemory(initialPages = 2)

        twoPages.writeULong(1337u, PAGE_SIZE)
        assertThat(twoPages.readULong(PAGE_SIZE)).isEqualTo(1337.toULong())
    }

    @Test
    fun readWrite_ulong_withNonStandardByteWidth() {
        val memory = ByteBufferMemory()

        memory.writeULong(ULong.MAX_VALUE, 0, 1)
        assertThat(memory.readULong(0, 1))
            .isEqualTo(ULong.MAX_VALUE.wrap(1).toUByte().toULong())

        memory.writeULong(ULong.MAX_VALUE, 0, 2)
        assertThat(memory.readULong(0, 2))
            .isEqualTo(ULong.MAX_VALUE.wrap(2).toUShort().toULong())

        memory.writeULong(ULong.MAX_VALUE, 0, 4)
        assertThat(memory.readULong(0, 4))
            .isEqualTo(ULong.MAX_VALUE.wrap(4).toUInt().toULong())
    }

    @Test
    fun readWrite_ulong_boundaryCross() {
        val memory = ByteBufferMemory(initialPages = 2)

        memory.writeULong(1337u, PAGE_SIZE - 7)
        assertThat(memory.readULong(PAGE_SIZE - 7)).isEqualTo(1337.toULong())

        memory.writeULong(1337u, PAGE_SIZE - 6)
        assertThat(memory.readULong(PAGE_SIZE - 6)).isEqualTo(1337.toULong())

        memory.writeULong(1337u, PAGE_SIZE - 5)
        assertThat(memory.readULong(PAGE_SIZE - 5)).isEqualTo(1337.toULong())

        memory.writeULong(1337u, PAGE_SIZE - 4)
        assertThat(memory.readULong(PAGE_SIZE - 4)).isEqualTo(1337.toULong())

        memory.writeULong(1337u, PAGE_SIZE - 3)
        assertThat(memory.readULong(PAGE_SIZE - 3)).isEqualTo(1337.toULong())

        memory.writeULong(1337u, PAGE_SIZE - 2)
        assertThat(memory.readULong(PAGE_SIZE - 2)).isEqualTo(1337.toULong())

        memory.writeULong(1337u, PAGE_SIZE - 1)
        assertThat(memory.readULong(PAGE_SIZE - 1)).isEqualTo(1337.toULong())
    }

    @Test
    fun readWrite_float_simple() {
        val memory = ByteBufferMemory()

        memory.writeFloat(10f, 0)
        assertThat(memory.readFloat(0)).isEqualTo(10f)

        memory.writeFloat(10f, 1)
        assertThat(memory.readFloat(1)).isEqualTo(10f)

        memory.writeFloat(1337f, PAGE_SIZE / 2)
        assertThat(memory.readFloat(PAGE_SIZE / 2)).isEqualTo(1337f)

        val twoPages = ByteBufferMemory(initialPages = 2)

        twoPages.writeFloat(1337f, PAGE_SIZE)
        assertThat(twoPages.readFloat(PAGE_SIZE)).isEqualTo(1337f)
    }

    @Test
    fun readWrite_float_boundaryCross() {
        val memory = ByteBufferMemory(initialPages = 2)

        memory.writeFloat(1337f, PAGE_SIZE - 3)
        assertThat(memory.readFloat(PAGE_SIZE - 3)).isEqualTo(1337f)

        memory.writeFloat(1337f, PAGE_SIZE - 2)
        assertThat(memory.readFloat(PAGE_SIZE - 2)).isEqualTo(1337f)

        memory.writeFloat(1337f, PAGE_SIZE - 1)
        assertThat(memory.readFloat(PAGE_SIZE - 1)).isEqualTo(1337f)
    }

    @Test
    fun readWrite_double_simple() {
        val memory = ByteBufferMemory()

        memory.writeDouble(10.0, 0)
        assertThat(memory.readDouble(0)).isEqualTo(10.0)

        memory.writeDouble(10.0, 1)
        assertThat(memory.readDouble(1)).isEqualTo(10.0)

        memory.writeDouble(1337.0, PAGE_SIZE / 2)
        assertThat(memory.readDouble(PAGE_SIZE / 2)).isEqualTo(1337.0)

        val twoPages = ByteBufferMemory(initialPages = 2)

        twoPages.writeDouble(1337.0, PAGE_SIZE)
        assertThat(twoPages.readDouble(PAGE_SIZE)).isEqualTo(1337.0)
    }

    @Test
    fun readWrite_double_boundaryCross() {
        val memory = ByteBufferMemory(initialPages = 2)

        memory.writeDouble(1337.0, PAGE_SIZE - 7)
        assertThat(memory.readDouble(PAGE_SIZE - 7)).isEqualTo(1337.0)

        memory.writeDouble(1337.0, PAGE_SIZE - 6)
        assertThat(memory.readDouble(PAGE_SIZE - 6)).isEqualTo(1337.0)

        memory.writeDouble(1337.0, PAGE_SIZE - 5)
        assertThat(memory.readDouble(PAGE_SIZE - 5)).isEqualTo(1337.0)

        memory.writeDouble(1337.0, PAGE_SIZE - 4)
        assertThat(memory.readDouble(PAGE_SIZE - 4)).isEqualTo(1337.0)

        memory.writeDouble(1337.0, PAGE_SIZE - 3)
        assertThat(memory.readDouble(PAGE_SIZE - 3)).isEqualTo(1337.0)

        memory.writeDouble(1337.0, PAGE_SIZE - 2)
        assertThat(memory.readDouble(PAGE_SIZE - 2)).isEqualTo(1337.0)

        memory.writeDouble(1337.0, PAGE_SIZE - 1)
        assertThat(memory.readDouble(PAGE_SIZE - 1)).isEqualTo(1337.0)
    }

    @Test
    fun readWrite_bytes_simple() {
        val memory = ByteBufferMemory()

        fun buildTest(str: String) =
            str.toByteArray(Charsets.UTF_8).let {
                it to ByteArray(it.size)
            }

        buildTest("Hello world!").also { (testString, readBuffer) ->
            memory.writeBytes(testString, 0)
            assertThat(memory.readBytes(readBuffer, 0)).isEqualTo(readBuffer.size)
            assertThat(readBuffer.toString(Charsets.UTF_8)).isEqualTo("Hello world!")
        }

        buildTest("What about offset=1?").also { (testString, readBuffer) ->
            memory.writeBytes(testString, 1)
            assertThat(memory.readBytes(readBuffer, 1)).isEqualTo(readBuffer.size)
            assertThat(readBuffer.toString(Charsets.UTF_8)).isEqualTo("What about offset=1?")
        }

        buildTest("Middle of the page").also { (testString, readBuffer) ->
            memory.writeBytes(testString, PAGE_SIZE / 2)
            assertThat(memory.readBytes(readBuffer, PAGE_SIZE / 2)).isEqualTo(readBuffer.size)
            assertThat(readBuffer.toString(Charsets.UTF_8)).isEqualTo("Middle of the page")
        }
    }

    @Test
    fun readWrite_bytes_simple_acrossBoundary() {
        val memory = ByteBufferMemory(initialPages = 2)

        fun buildTest(str: String) =
            str.toByteArray(Charsets.UTF_8).let {
                it to ByteArray(it.size)
            }

        buildTest("Hello world!").also { (testString, readBuffer) ->
            memory.writeBytes(testString, PAGE_SIZE - 5)
            assertThat(memory.readBytes(readBuffer, PAGE_SIZE - 5)).isEqualTo(readBuffer.size)
            assertThat(readBuffer.toString(Charsets.UTF_8)).isEqualTo("Hello world!")
        }
    }

    @Test
    fun read_bytes_with_outOffset_andLength() {
        val memory = ByteBufferMemory()

        class TestCase(str: String, outOffset: Int, length: Int) {
            val testString = str.toByteArray(Charsets.UTF_8)
            val readBuffer = ByteArray(testString.size)
            val expectedContents = ByteArray(testString.size) {
                if (it >= outOffset && it < outOffset + length) testString[it - outOffset] else 0
            }.toTypedArray()
        }

        TestCase("Hello world!", outOffset = 1, length = 5).also {
            memory.writeBytes(it.testString, 0)
            assertThat(memory.readBytes(it.readBuffer, 0, 1, 5))
                .isEqualTo(5)
            assertThat(it.readBuffer.toList()).containsExactly(*it.expectedContents)
        }

        TestCase("Hello world!", outOffset = 5, length = 5).also {
            memory.writeBytes(it.testString, 0)
            assertThat(memory.readBytes(it.readBuffer, 0, 5, 5))
                .isEqualTo(5)
            assertThat(it.readBuffer.toList()).containsExactly(*it.expectedContents)
        }
    }

    @Test
    fun write_bytes_with_outOffset_andLength() {
        val memory = ByteBufferMemory()

        class TestCase(str: String, outOffset: Int, length: Int) {
            val testString = str.toByteArray(Charsets.UTF_8)
            val readBuffer = ByteArray(testString.size)
            val expectedContents = ByteArray(testString.size) {
                if (it >= outOffset && it < outOffset + length) testString[it] else 0
            }.toTypedArray()
        }

        TestCase("Hello world!", outOffset = 1, length = 5).also {
            memory.writeBytes(it.testString, 0, 1, 5)
            assertThat(memory.readBytes(it.readBuffer, 0))
                .isEqualTo(it.readBuffer.size)
            assertThat(it.readBuffer.toList()).containsExactly(*it.expectedContents)
        }

        TestCase("Hello world!", outOffset = 5, length = 5).also {
            memory.writeBytes(it.testString, 0, 5, 5)
            assertThat(memory.readBytes(it.readBuffer, 0))
                .isEqualTo(it.readBuffer.size)
            assertThat(it.readBuffer.toList()).containsExactly(*it.expectedContents)
        }
    }

    @Test
    fun readBytes_throws_ifLengthDoesNotFit_givenOutSize_andOffset() {
        val memory = ByteBufferMemory()
        var e = assertThrows(IllegalArgumentException::class.java) {
            memory.readBytes(ByteArray(0), 0, 0, 1)
        }
        assertThat(e).hasMessageThat().contains("Invalid read length")

        e = assertThrows(IllegalArgumentException::class.java) {
            memory.readBytes(ByteArray(0), 0, 1, 0)
        }
        assertThat(e).hasMessageThat().contains("Invalid read length")

        e = assertThrows(IllegalArgumentException::class.java) {
            memory.readBytes(ByteArray(10), 0, 5, 6)
        }
        assertThat(e).hasMessageThat().contains("Invalid read length")
    }

    @Test
    fun writeBytes_throws_ifValueArgs_areIllegal() {
        val memory = ByteBufferMemory()
        var e = assertThrows(IllegalArgumentException::class.java) {
            memory.writeBytes(ByteArray(0), 0, 0, 1)
        }
        assertThat(e).hasMessageThat().contains("Illegal offset/length")

        e = assertThrows(IllegalArgumentException::class.java) {
            memory.writeBytes(ByteArray(0), 0, 1, 0)
        }
        assertThat(e).hasMessageThat().contains("Illegal offset/length")

        e = assertThrows(IllegalArgumentException::class.java) {
            memory.writeBytes(ByteArray(10), 0, 5, 6)
        }
        assertThat(e).hasMessageThat().contains("Illegal offset/length")
    }

    @Test
    fun writeBytes_throws_ifOffsetAndLength_dontFit() {
        val memory = ByteBufferMemory()
        var e = assertThrows(IllegalArgumentException::class.java) {
            memory.writeBytes(ByteArray(0), PAGE_SIZE + 1)
        }
        assertThat(e).hasMessageThat().contains("Value with size")

        e = assertThrows(IllegalArgumentException::class.java) {
            memory.writeBytes(ByteArray(0), PAGE_SIZE + 1)
        }
        assertThat(e).hasMessageThat().contains("Value with size")

        e = assertThrows(IllegalArgumentException::class.java) {
            memory.writeBytes(ByteArray(1), PAGE_SIZE)
        }
        assertThat(e).hasMessageThat().contains("Value with size")

        e = assertThrows(IllegalArgumentException::class.java) {
            memory.writeBytes(ByteArray(10), PAGE_SIZE - 5)
        }
        assertThat(e).hasMessageThat().contains("Value with size")
    }

    @Test
    fun growBy_validInput() {
        val memory = ByteBufferMemory(maximumPages = 3)
        assertThat(memory.growBy(1)).isEqualTo(1)
        assertThat(memory.growBy(1)).isEqualTo(2)
    }

    @Test
    fun growBy_whenNewSizeWouldBeTooLarge_returnsGrowFailure() {
        val memory = ByteBufferMemory(maximumPages = 3)
        assertThat(memory.growBy(3)).isEqualTo(GROW_FAILURE)
    }

    @Test
    fun lock_enforcesFiFo() = runBlockingTest {
        val memory = ByteBufferMemory()

        val values = arrayOf(1, 2, 3)
        val beforeLockDelays =
            LongArray(1000) { it.toLong() }
                .toList()
                .shuffled(Random(System.nanoTime()))
                .take(3)
        val expectedFinalValue =
            values[beforeLockDelays.withIndex().maxByOrNull { it.value }!!.index]

        val jobOne = launch {
            delay(beforeLockDelays[0])
            memory.lock {
                writeInt(1, 0)
                delay(1000)
                assertWithMessage("Job 1").that(readInt(0)).isEqualTo(1)
            }
        }
        val jobTwo = launch {
            delay(beforeLockDelays[1])
            memory.lock {
                writeInt(2, 0)
                delay(1000)
                assertWithMessage("Job 2").that(readInt(0)).isEqualTo(2)
            }
        }
        val jobThree = launch {
            delay(beforeLockDelays[2])
            memory.lock {
                writeInt(3, 0)
                delay(1000)
                assertWithMessage("Job 3").that(readInt(0)).isEqualTo(3)
            }
        }

        jobOne.join()
        jobTwo.join()
        jobThree.join()

        assertThat(memory.readInt(0)).isEqualTo(expectedFinalValue)
    }
}
