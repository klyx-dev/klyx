@file:OptIn(ExperimentalWasmApi::class)

package com.klyx.wasm.type.collections

import com.klyx.wasm.ExperimentalWasmApi
import com.klyx.wasm.WasmMemory
import com.klyx.wasm.type.WasmMemoryReader
import com.klyx.wasm.type.WasmValue

@JvmInline
value class Tuple2<A : WasmValue, B : WasmValue>(
    private val value: Pair<A, B>
) : WasmValue {
    val first: A get() = value.first
    val second: B get() = value.second

    override fun writeToBuffer(buffer: ByteArray, offset: Int) {
        require(offset + sizeInBytes() <= buffer.size) { "Buffer too small for Tuple2" }
        first.writeToBuffer(buffer, offset)
        second.writeToBuffer(buffer, offset + first.sizeInBytes())
    }

    override fun sizeInBytes(): Int = first.sizeInBytes() + second.sizeInBytes()

    override fun createReader() = reader(first.createReader(), second.createReader())

    override fun toString(): String = "($first, $second)"

    override fun toString(memory: WasmMemory): String {
        return buildString {
            append("(")
            append(first.toString(memory))
            append(", ")
            append(second.toString(memory))
            append(")")
        }
    }

    operator fun component1(): A = first
    operator fun component2(): B = second

    companion object {
        fun <A : WasmValue, B : WasmValue> reader(
            firstReader: WasmMemoryReader<A>,
            secondReader: WasmMemoryReader<B>
        ) = Tuple2Reader(firstReader, secondReader)
    }
}

@JvmInline
value class Tuple3<A : WasmValue, B : WasmValue, C : WasmValue>(
    private val value: Triple<A, B, C>
) : WasmValue {
    val first: A get() = value.first
    val second: B get() = value.second
    val third: C get() = value.third

    override fun writeToBuffer(buffer: ByteArray, offset: Int) {
        require(offset + sizeInBytes() <= buffer.size) { "Buffer too small for Tuple3" }
        var currentOffset = offset
        first.writeToBuffer(buffer, currentOffset)
        currentOffset += first.sizeInBytes()
        second.writeToBuffer(buffer, currentOffset)
        currentOffset += second.sizeInBytes()
        third.writeToBuffer(buffer, currentOffset)
    }

    override fun sizeInBytes(): Int =
        first.sizeInBytes() + second.sizeInBytes() + third.sizeInBytes()

    override fun createReader() =
        reader(first.createReader(), second.createReader(), third.createReader())

    override fun toString(): String = "($first, $second, $third)"

    override fun toString(memory: WasmMemory): String {
        return buildString {
            append("(")
            append(first.toString(memory))
            append(", ")
            append(second.toString(memory))
            append(", ")
            append(third.toString(memory))
            append(")")
        }
    }

    operator fun component1(): A = first
    operator fun component2(): B = second
    operator fun component3(): C = third

    companion object {
        fun <A : WasmValue, B : WasmValue, C : WasmValue> reader(
            firstReader: WasmMemoryReader<A>,
            secondReader: WasmMemoryReader<B>,
            thirdReader: WasmMemoryReader<C>
        ) = Tuple3Reader(firstReader, secondReader, thirdReader)
    }
}

@OptIn(ExperimentalWasmApi::class)
class Tuple2Reader<A : WasmValue, B : WasmValue>(
    private val firstReader: WasmMemoryReader<A>,
    private val secondReader: WasmMemoryReader<B>
) : WasmMemoryReader<Tuple2<A, B>> {

    override val elementSize = firstReader.elementSize + secondReader.elementSize

    override fun read(memory: WasmMemory, offset: Int): Tuple2<A, B> {
        val firstValue = firstReader.read(memory, offset)
        val secondValue = secondReader.read(memory, offset + firstReader.elementSize)
        return Tuple2(firstValue, secondValue)
    }
}

@OptIn(ExperimentalWasmApi::class)
class Tuple3Reader<A : WasmValue, B : WasmValue, C : WasmValue>(
    private val firstReader: WasmMemoryReader<A>,
    private val secondReader: WasmMemoryReader<B>,
    private val thirdReader: WasmMemoryReader<C>
) : WasmMemoryReader<Tuple3<A, B, C>> {

    override val elementSize =
        firstReader.elementSize + secondReader.elementSize + thirdReader.elementSize

    override fun read(memory: WasmMemory, offset: Int): Tuple3<A, B, C> {
        var currentOffset = offset
        val firstValue = firstReader.read(memory, currentOffset)
        currentOffset += firstReader.elementSize
        val secondValue = secondReader.read(memory, currentOffset)
        currentOffset += secondReader.elementSize
        val thirdValue = thirdReader.read(memory, currentOffset)
        return Tuple3(firstValue, secondValue, thirdValue)
    }
}
