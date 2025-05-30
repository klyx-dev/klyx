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

import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kwasm.runtime.Memory
import kwasm.runtime.Memory.Companion.GROW_FAILURE
import kwasm.runtime.Memory.Companion.PAGE_SIZE
import kwasm.util.Impossible
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.ceil

@Suppress("EXPERIMENTAL_API_USAGE", "MemberVisibilityCanBePrivate")
internal class ByteBufferMemory(
    internal val maximumPages: Int = 10, // 64 Megabytes
    initialData: ByteArray = ByteArray(0),
    initialPages: Int = maxOf(1, ceil(initialData.size / PAGE_SIZE.toDouble()).toInt())
) : Memory {
    private val semaphore = Semaphore(1)
    private val pages = mutableListOf<ByteBuffer>()
    private val tempBytes = ByteArray(8)
    private val tempBuffer = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN)

    init {
        require(maximumPages >= initialPages) {
            "Maximum size specified as $maximumPages page(s), but initialPages = $initialPages"
        }
        require(maximumPages * PAGE_SIZE >= initialData.size) {
            "Maximum size specified as $maximumPages page(s) (${maximumPages * PAGE_SIZE} " +
                "bytes), but initialData has length: ${initialData.size}"
        }

        var initializedPages = 0
        while (initializedPages < initialPages) {
            pages.add(
                ByteBuffer.allocate(PAGE_SIZE)
                    .order(ByteOrder.LITTLE_ENDIAN)
            )
            initializedPages++
        }

        writeBytes(initialData, offset = 0)
    }

    override val sizePages: Int
        get() = pages.size

    override suspend fun <T> lock(block: suspend Memory.() -> T): T =
        semaphore.withPermit { this.block() }

    override fun readInt(offset: Int, byteWidth: Int, alignment: Int): Int {
        byteWidth.assertValidByteWidth<Int>()
        val currentPage = offset / PAGE_SIZE

        return if (byteWidth == 4 && offset % PAGE_SIZE < PAGE_SIZE - 4) {
            // Simple case... use ByteBuffer directly.
            pages[currentPage].getInt(offset % PAGE_SIZE)
        } else {
            pages.readInt(offset, byteWidth)
        }
    }

    override fun readUInt(offset: Int, byteWidth: Int, alignment: Int): UInt {
        byteWidth.assertValidByteWidth<Int>()
        val currentPage = offset / PAGE_SIZE

        return if (byteWidth == 4 && offset % PAGE_SIZE < PAGE_SIZE - 4) {
            // Simple case... use ByteBuffer directly.
            pages[currentPage].getInt(offset % PAGE_SIZE).toUInt()
        } else {
            pages.readUInt(offset, byteWidth)
        }
    }

    override fun readLong(offset: Int, byteWidth: Int, alignment: Int): Long {
        byteWidth.assertValidByteWidth<Long>()
        val currentPage = offset / PAGE_SIZE

        return if (byteWidth == 8 && offset % PAGE_SIZE < PAGE_SIZE - 8) {
            // Simple case... use ByteBuffer directly.
            pages[currentPage].getLong(offset % PAGE_SIZE)
        } else {
            pages.readLong(offset, byteWidth)
        }
    }

    override fun readULong(offset: Int, byteWidth: Int, alignment: Int): ULong {
        byteWidth.assertValidByteWidth<Long>()
        val currentPage = offset / PAGE_SIZE

        return if (byteWidth == 8 && offset % PAGE_SIZE < PAGE_SIZE - 8) {
            // Simple case... use ByteBuffer directly.
            pages[currentPage].getLong(offset % PAGE_SIZE).toULong()
        } else {
            pages.readULong(offset, byteWidth)
        }
    }

    override fun readFloat(offset: Int, alignment: Int): Float {
        val currentPage = offset / PAGE_SIZE

        return if (offset % PAGE_SIZE < PAGE_SIZE - 4 - 1) {
            // Simple case... use ByteBuffer directly.
            pages[currentPage].getFloat(offset % PAGE_SIZE)
        } else {
            pages.readFloat(offset)
        }
    }

    override fun readDouble(offset: Int, alignment: Int): Double {
        val currentPage = offset / PAGE_SIZE

        return if (offset % PAGE_SIZE < PAGE_SIZE - 8) {
            // Simple case... use ByteBuffer directly.
            pages[currentPage].getDouble(offset % PAGE_SIZE)
        } else {
            pages.readDouble(offset)
        }
    }

    override fun writeInt(value: Int, offset: Int, byteWidth: Int, alignment: Int) {
        byteWidth.assertValidByteWidth<Int>()
        val currentPage = offset / PAGE_SIZE

        if (byteWidth == 4 && offset % PAGE_SIZE < PAGE_SIZE - 4) {
            // Simple case... use ByteBuffer directly.
            pages[currentPage].putInt(offset % PAGE_SIZE, value)
        } else if ((offset + byteWidth) / PAGE_SIZE < pages.size) {
            pages.writeInt(value, offset, byteWidth)
        } else throw IndexOutOfBoundsException(
            "Data at $offset with width $byteWidth would overflow into " +
                "a page that does not exist yet."
        )
    }

    override fun writeUInt(value: UInt, offset: Int, byteWidth: Int, alignment: Int) {
        byteWidth.assertValidByteWidth<Int>()
        val currentPage = offset / PAGE_SIZE

        if (byteWidth == 4 && offset % PAGE_SIZE < PAGE_SIZE - 4) {
            // Simple case... use ByteBuffer directly.
            pages[currentPage].putInt(offset % PAGE_SIZE, value.toInt())
        } else if ((offset + byteWidth) / PAGE_SIZE < pages.size) {
            pages.writeUInt(value, offset, byteWidth)
        } else throw IndexOutOfBoundsException(
            "Data at $offset with width $byteWidth would overflow into " +
                "a page that does not exist yet."
        )
    }

    override fun writeLong(value: Long, offset: Int, byteWidth: Int, alignment: Int) {
        byteWidth.assertValidByteWidth<Long>()
        val currentPage = offset / PAGE_SIZE

        if (byteWidth == 8 && offset % PAGE_SIZE < PAGE_SIZE - 8) {
            // Simple case... use ByteBuffer directly.
            pages[currentPage].putLong(offset % PAGE_SIZE, value)
        } else if ((offset + byteWidth) / PAGE_SIZE < pages.size) {
            pages.writeLong(value, offset, byteWidth)
        } else throw IndexOutOfBoundsException(
            "Data at $offset with width $byteWidth would overflow into " +
                "a page that does not exist yet."
        )
    }

    override fun writeULong(value: ULong, offset: Int, byteWidth: Int, alignment: Int) {
        byteWidth.assertValidByteWidth<Long>()
        val currentPage = offset / PAGE_SIZE

        if (byteWidth == 8 && offset % PAGE_SIZE < PAGE_SIZE - 8) {
            // Simple case... use ByteBuffer directly.
            pages[currentPage].putLong(offset % PAGE_SIZE, value.toLong())
        } else if ((offset + byteWidth) / PAGE_SIZE < pages.size) {
            pages.writeULong(value, offset, byteWidth)
        } else throw IndexOutOfBoundsException(
            "Data at $offset with width $byteWidth would overflow into " +
                "a page that does not exist yet."
        )
    }

    override fun writeFloat(value: Float, offset: Int, alignment: Int) {
        val currentPage = offset / PAGE_SIZE

        if (offset % PAGE_SIZE < PAGE_SIZE - 4) {
            // Simple case... use ByteBuffer directly.
            pages[currentPage].putFloat(offset % PAGE_SIZE, value)
        } else if ((offset + 4) / PAGE_SIZE < pages.size) {
            pages.writeFloat(value, offset)
        } else throw IndexOutOfBoundsException(
            "Data at $offset with width 4 would overflow into " +
                "a page that does not exist yet."
        )
    }

    override fun writeDouble(value: Double, offset: Int, alignment: Int) {
        val currentPage = offset / PAGE_SIZE

        if (offset % PAGE_SIZE < PAGE_SIZE - 8) {
            // Simple case... use ByteBuffer directly.
            pages[currentPage].putDouble(offset % PAGE_SIZE, value)
        } else if ((offset + 8) / PAGE_SIZE < pages.size) {
            pages.writeDouble(value, offset)
        } else throw IndexOutOfBoundsException(
            "Data at $offset with width 8 would overflow into " +
                "a page that does not exist yet."
        )
    }

    override fun readBytes(
        out: ByteArray,
        memoryOffset: Int,
        outOffset: Int,
        length: Int
    ): Int {
        require(outOffset + length <= out.size) {
            "Invalid read length ($length) for output array (size: ${out.size}) starting at " +
                "output offset: $outOffset"
        }

        val readableLength = minOf(length, sizeBytes - memoryOffset)
        pages.readBytes(out, memoryOffset, length, outOffset)
        return readableLength
    }

    override fun writeBytes(value: ByteArray, offset: Int, valueOffset: Int, valueLength: Int) {
        require(valueOffset + valueLength <= value.size) {
            "Illegal offset/length for value with size: ${value.size}"
        }
        require(offset + valueLength <= sizeBytes) {
            "Value with size ${value.size} bytes cannot fit into memory with size: $sizeBytes " +
                "bytes ($sizePages page(s))"
        }
        var valuePosition = valueOffset
        var currentPage = offset / PAGE_SIZE
        var currentPositionInPage = offset % PAGE_SIZE
        while (valuePosition < valueOffset + valueLength) {
            val leftInPage = PAGE_SIZE - currentPositionInPage
            val leftInValue = (valueLength + valueOffset) - valuePosition
            valuePosition += if (leftInPage >= leftInValue) {
                // We're at the last page, so write all the remaining bytes from the value.
                (pages[currentPage].position(currentPositionInPage) as ByteBuffer)
                    .put(value, valuePosition, leftInValue)
                    .rewind()
                leftInValue
            } else {
                // We're still working through the pages, so write as much as we can in this page.
                (pages[currentPage].position(currentPositionInPage) as ByteBuffer)
                    .put(value, valuePosition, leftInPage)
                    .rewind()
                leftInPage
            }
            currentPage++
            currentPositionInPage = 0
        }
    }

    override fun growBy(newPages: Int): Int {
        if (sizePages + newPages > maximumPages) return GROW_FAILURE
        val sizeBefore = sizePages
        repeat(newPages) {
            pages.add(
                ByteBuffer.allocate(PAGE_SIZE)
                    .order(ByteOrder.LITTLE_ENDIAN)
            )
        }
        return sizeBefore
    }

    private fun List<ByteBuffer>.readInt(offset: Int, byteWidth: Int): Int {
        val result = this.readBytes(tempBytes, offset, byteWidth).toBigEndianInt(byteWidth)
        return when (byteWidth) {
            1 -> result.toByte().toInt()
            2 -> result.toShort().toInt()
            4 -> result
            else -> Impossible()
        }
    }

    private fun List<ByteBuffer>.readUInt(offset: Int, byteWidth: Int): UInt {
        val result = this.readBytes(tempBytes, offset, byteWidth).toBigEndianInt(byteWidth)
        return when (byteWidth) {
            1 -> result.toUByte().toUInt()
            2 -> result.toUShort().toUInt()
            4 -> result.toUInt()
            else -> Impossible()
        }
    }

    private fun List<ByteBuffer>.writeInt(value: Int, offset: Int, byteWidth: Int) {
        val wrappedValue = value.wrap(byteWidth)
        var currentPage = offset / PAGE_SIZE
        var currentPositionInPage = offset % PAGE_SIZE
        repeat(byteWidth) {
            val byte = (wrappedValue ushr (8 * it)) and 0xFF
            this[currentPage].put(currentPositionInPage, byte.toByte())
            currentPositionInPage++
            if (currentPositionInPage == PAGE_SIZE) {
                currentPositionInPage = 0
                currentPage++
            }
        }
    }

    private fun List<ByteBuffer>.writeUInt(value: UInt, offset: Int, byteWidth: Int) {
        val wrappedValue = value.toLong().wrap(byteWidth)
        var currentPage = offset / PAGE_SIZE
        var currentPositionInPage = offset % PAGE_SIZE
        repeat(byteWidth) {
            val byte = (wrappedValue ushr (8 * it)) and 0xFF
            this[currentPage].put(currentPositionInPage, byte.toByte())
            currentPositionInPage++
            if (currentPositionInPage == PAGE_SIZE) {
                currentPositionInPage = 0
                currentPage++
            }
        }
    }

    private fun List<ByteBuffer>.readLong(offset: Int, byteWidth: Int): Long {
        val result = this.readBytes(tempBytes, offset, byteWidth).toBigEndianLong(byteWidth)
        return when (byteWidth) {
            1 -> result.toByte().toLong()
            2 -> result.toShort().toLong()
            4 -> result.toInt().toLong()
            8 -> result
            else -> Impossible()
        }
    }

    private fun List<ByteBuffer>.readULong(offset: Int, byteWidth: Int): ULong {
        val result = this.readBytes(tempBytes, offset, byteWidth).toBigEndianLong(byteWidth)
        return when (byteWidth) {
            1 -> result.toUByte().toULong()
            2 -> result.toUShort().toULong()
            4 -> result.toUInt().toULong()
            8 -> result.toULong()
            else -> Impossible()
        }
    }

    private fun List<ByteBuffer>.writeLong(value: Long, offset: Int, byteWidth: Int) {
        val wrappedValue = value.wrap(byteWidth)
        var currentPage = offset / PAGE_SIZE
        var currentPositionInPage = offset % PAGE_SIZE
        repeat(byteWidth) {
            val byte = (wrappedValue ushr (8 * it)) and 0xFF
            this[currentPage].put(currentPositionInPage, byte.toByte())
            currentPositionInPage++
            if (currentPositionInPage == PAGE_SIZE) {
                currentPositionInPage = 0
                currentPage++
            }
        }
    }

    private fun List<ByteBuffer>.writeULong(value: ULong, offset: Int, byteWidth: Int) {
        val wrappedValue = value.wrap(byteWidth)
        var currentPage = offset / PAGE_SIZE
        var currentPositionInPage = offset % PAGE_SIZE
        repeat(byteWidth) {
            val byte = (wrappedValue shr (8 * it)) and 0xFF.toULong()
            this[currentPage].put(currentPositionInPage, byte.toByte())
            currentPositionInPage++
            if (currentPositionInPage == PAGE_SIZE) {
                currentPositionInPage = 0
                currentPage++
            }
        }
    }

    private fun List<ByteBuffer>.readFloat(offset: Int): Float {
        this.readBytes(tempBuffer.array(), offset, 4)
        return tempBuffer.getFloat(0)
    }

    @Suppress("unused")
    private fun List<ByteBuffer>.writeFloat(value: Float, offset: Int) {
        tempBuffer.putFloat(0, value)
        writeBytes(tempBuffer.array(), offset, 0, 4)
    }

    private fun List<ByteBuffer>.readDouble(offset: Int): Double {
        this.readBytes(tempBuffer.array(), offset, 8)
        return tempBuffer.getDouble(0)
    }

    @Suppress("unused")
    private fun List<ByteBuffer>.writeDouble(value: Double, offset: Int) {
        tempBuffer.putDouble(0, value)
        writeBytes(tempBuffer.array(), offset, 0, 8)
    }

    private fun List<ByteBuffer>.readBytes(
        out: ByteArray,
        offset: Int,
        length: Int,
        outOffset: Int = 0
    ): ByteArray {
        var currentPage = offset / PAGE_SIZE
        var currentPageOffset = offset % PAGE_SIZE
        var currentOutOffset = outOffset
        var readBytes = 0
        while (readBytes < length) {
            val leftInPage = PAGE_SIZE - currentPageOffset
            val leftInLength = length - readBytes
            readBytes += if (leftInPage >= leftInLength) {
                // Last page needed for reading. Read the remaining length.
                (this[currentPage].position(currentPageOffset) as ByteBuffer)
                    .get(out, currentOutOffset, leftInLength)
                    .rewind()
                leftInLength
            } else {
                (this[currentPage].position(currentPageOffset) as ByteBuffer)
                    .get(out, currentOutOffset, leftInPage)
                    .rewind()
                leftInPage
            }
            currentOutOffset = outOffset + readBytes
            currentPage++
            currentPageOffset = 0
        }
        return out
    }
}
