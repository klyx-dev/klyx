@file:OptIn(ExperimentalWasmApi::class)

package com.klyx.wasm.type.collections

import com.klyx.wasm.ExperimentalWasmApi
import com.klyx.wasm.WasmMemory
import com.klyx.wasm.internal.InternalExperimentalWasmApi
import com.klyx.wasm.internal.writeInt32LittleEndian
import com.klyx.wasm.type.WasmBool
import com.klyx.wasm.type.WasmByte
import com.klyx.wasm.type.WasmChar
import com.klyx.wasm.type.WasmDouble
import com.klyx.wasm.type.WasmFloat
import com.klyx.wasm.type.WasmInt
import com.klyx.wasm.type.WasmLong
import com.klyx.wasm.type.WasmMemoryReader
import com.klyx.wasm.type.WasmShort
import com.klyx.wasm.type.WasmString
import com.klyx.wasm.type.WasmUByte
import com.klyx.wasm.type.WasmUInt
import com.klyx.wasm.type.WasmULong
import com.klyx.wasm.type.WasmUShort
import com.klyx.wasm.type.WasmType
import kotlin.jvm.JvmName

class WasmList<T : WasmType>(
    private val ptr: Int,
    override val size: Int,
    private val memory: WasmMemory,
    private val reader: WasmMemoryReader<T>
) : List<T>, WasmType {

    override fun isEmpty(): Boolean = size == 0

    override fun contains(element: T): Boolean {
        return (0 until size).any { index ->
            get(index) == element
        }
    }

    override fun get(index: Int): T {
        require(index in 0 until size) { "Index $index out of bounds for list of size $size" }
        val elementOffset = ptr + index * reader.elementSize
        return reader.read(memory, elementOffset)
    }

    override fun indexOf(element: T): Int {
        for (index in 0 until size) {
            if (get(index) == element) {
                return index
            }
        }
        return -1
    }

    override fun lastIndexOf(element: T): Int {
        for (index in size - 1 downTo 0) {
            if (get(index) == element) {
                return index
            }
        }
        return -1
    }

    override fun listIterator(): ListIterator<T> = WasmListIterator(0)

    override fun listIterator(index: Int): ListIterator<T> {
        require(index in 0..size) { "Index $index out of bounds for list of size $size" }
        return WasmListIterator(index)
    }

    override fun subList(fromIndex: Int, toIndex: Int): List<T> {
        require(fromIndex >= 0) { "fromIndex must be non-negative" }
        require(toIndex <= size) { "toIndex must not exceed list size" }
        require(fromIndex <= toIndex) { "fromIndex must not be greater than toIndex" }

        if (fromIndex == toIndex) return emptyList()

        val newSize = toIndex - fromIndex
        val newPtr = ptr + fromIndex * reader.elementSize

        return WasmList(newPtr, newSize, memory, reader)
    }

    override fun iterator(): Iterator<T> = object : Iterator<T> {
        private var currentIndex = 0

        override fun hasNext(): Boolean = currentIndex < size

        override fun next(): T {
            if (!hasNext()) throw NoSuchElementException()
            return get(currentIndex++)
        }
    }

    override fun containsAll(elements: Collection<T>): Boolean {
        return elements.all { contains(it) }
    }

    private inner class WasmListIterator(private var index: Int) : ListIterator<T> {

        override fun hasNext(): Boolean = index < size

        override fun next(): T {
            if (!hasNext()) throw NoSuchElementException()
            return get(index++)
        }

        override fun hasPrevious(): Boolean = index > 0

        override fun previous(): T {
            if (!hasPrevious()) throw NoSuchElementException()
            return get(--index)
        }

        override fun nextIndex(): Int = index

        override fun previousIndex(): Int = index - 1
    }

    override fun toString(): String {
        return buildString {
            append("[")
            for (i in 0 until size) {
                if (i > 0) append(", ")
                append(this@WasmList[i].toString())
            }
            append("]")
        }
    }

    override fun toString(memory: WasmMemory): String {
        return buildString {
            append("[")
            for (i in 0 until size) {
                if (i > 0) append(", ")
                append(this@WasmList[i].toString(memory))
            }
            append("]")
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is List<*>) return false
        if (size != other.size) return false

        return (0 until size).all { index ->
            get(index) == other[index]
        }
    }

    override fun hashCode(): Int {
        var hashCode = 1
        for (element in this) {
            hashCode = 31 * hashCode + element.hashCode()
        }
        return hashCode
    }

    override fun writeToBuffer(buffer: ByteArray, offset: Int) {
        require(offset + 8 <= buffer.size) { "Buffer too small for WasmList" }
        buffer.writeInt32LittleEndian(ptr, offset)
        buffer.writeInt32LittleEndian(size, offset + 4)
    }

    override fun sizeInBytes(): Int = SIZE_BYTES

    override fun createReader() = reader(reader)

    companion object {
        const val SIZE_BYTES = 8 // pointer + size

        fun <T : WasmType> reader(elementReader: WasmMemoryReader<T>) = run {
            object : WasmMemoryReader<WasmList<T>> {
                override val elementSize = SIZE_BYTES

                override fun read(memory: WasmMemory, offset: Int): WasmList<T> {
                    val listPtr = memory.readInt(offset)
                    val listSize = memory.readInt(offset + 4)
                    return WasmList(listPtr, listSize, memory, elementReader)
                }

                override fun read(memory: WasmMemory, ptr: Int, len: Int): WasmList<T> {
                    return WasmList(ptr, len, memory, elementReader)
                }
            }
        }
    }
}

context(memory: WasmMemory)
fun <T : WasmType> wasmListOf(vararg values: T): WasmList<T> {
    return wasmListOf(values.toList())
}

context(memory: WasmMemory)
fun <T : WasmType> wasmEmptyList(): WasmList<T> = wasmListOf()

context(memory: WasmMemory)
fun <T : WasmType> wasmListOf() = emptyList<T>().toWasmList()

@OptIn(InternalExperimentalWasmApi::class)
context(memory: WasmMemory)
@Suppress("UNCHECKED_CAST")
fun <T : WasmType> wasmListOf(values: List<T>): WasmList<T> {
    if (values.isEmpty()) {
        // Return an empty list
        return WasmList(0, 0, memory, WasmString.reader as WasmMemoryReader<T>)
    }

    val firstElement = values.first()
    val reader = firstElement.createReader() as WasmMemoryReader<T>

    val count = values.size
    val totalSize = reader.elementSize * count
    val bytes = ByteArray(totalSize)
    var offset = 0

    for (element in values) {
        require(element.sizeInBytes() == reader.elementSize) {
            "All elements must have the same size. Expected ${reader.elementSize}, got ${element.sizeInBytes()}"
        }
        element.writeToBuffer(bytes, offset)
        offset += reader.elementSize
    }

    val listPtr = memory.allocate(bytes.size)
    memory.write(listPtr, bytes)

    return WasmList(listPtr, count, memory, reader)
}

context(memory: WasmMemory)
fun wasmListOf(vararg values: String) = wasmListOf(values.map { WasmString(it) })

context(memory: WasmMemory)
fun wasmListOf(vararg values: Char) = wasmListOf(values.map { WasmChar(it) })

@OptIn(ExperimentalUnsignedTypes::class)
context(memory: WasmMemory)
fun wasmListOf(vararg values: UByte) = wasmListOf(values.map { WasmUByte(it) })

context(memory: WasmMemory)
fun wasmListOf(vararg values: Byte) = wasmListOf(values.map { WasmByte(it) })

@OptIn(ExperimentalUnsignedTypes::class)
context(memory: WasmMemory)
fun wasmListOf(vararg values: UShort) = wasmListOf(values.map { WasmUShort(it) })

context(memory: WasmMemory)
fun wasmListOf(vararg values: Short) = wasmListOf(values.map { WasmShort(it) })

@OptIn(ExperimentalUnsignedTypes::class)
context(memory: WasmMemory)
fun wasmListOf(vararg values: UInt) = wasmListOf(values.map { WasmUInt(it) })

context(memory: WasmMemory)
fun wasmListOf(vararg values: Int) = wasmListOf(values.map { WasmInt(it) })

@OptIn(ExperimentalUnsignedTypes::class)
context(memory: WasmMemory)
fun wasmListOf(vararg values: ULong) = wasmListOf(values.map { WasmULong(it) })

context(memory: WasmMemory)
fun wasmListOf(vararg values: Long) = wasmListOf(values.map { WasmLong(it) })

context(memory: WasmMemory)
fun wasmListOf(vararg values: Boolean) = wasmListOf(values.map { WasmBool(it) })

context(memory: WasmMemory)
fun wasmListOf(vararg values: Float) = wasmListOf(values.map { WasmFloat(it) })

context(memory: WasmMemory)
fun wasmListOf(vararg values: Double) = wasmListOf(values.map { WasmDouble(it) })

context(memory: WasmMemory)
fun <T : WasmType> List<T>.toWasmList(): WasmList<T> = wasmListOf(this)

@JvmName("toWasmListPair")
context(memory: WasmMemory)
fun <A, B> List<Pair<A, B>>.toWasmList() = map { it.toWasmTuple() }.toWasmList()

@JvmName("toWasmListTriple")
context(memory: WasmMemory)
fun <A, B, C> List<Triple<A, B, C>>.toWasmList() = map { it.toWasmTuple() }.toWasmList()

