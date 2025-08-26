@file:OptIn(ExperimentalWasmApi::class)

package com.klyx.wasm.type

import com.klyx.wasm.ExperimentalWasmApi
import com.klyx.wasm.WasmMemory
import com.klyx.wasm.memory.uint8

sealed class Option<out T : WasmType> : WasmType {
    data object None : Option<Nothing>(), HasWasmReader<None> {
        override fun writeToBuffer(buffer: ByteArray, offset: Int) {
            buffer[offset] = 0 // discriminant for None
        }

        override fun sizeInBytes(): Int = 4 + 4 // discriminant + padding to align to 4 bytes

        override fun createReader() = reader

        override fun toString() = "None"
        override fun toString(memory: WasmMemory) = "None"

        override val reader
            get() = object : WasmMemoryReader<None> {
                override val elementSize = 4
                override fun read(memory: WasmMemory, offset: Int): None {
                    val discriminant = memory.uint8(offset)
                    require(discriminant.toInt() == 0) { "Expected None discriminant (0), got $discriminant" }
                    return None
                }
            }
    }

    data class Some<T : WasmType>(val value: T) : Option<T>() {
        override fun writeToBuffer(buffer: ByteArray, offset: Int) {
            require(offset + sizeInBytes() <= buffer.size) { "Buffer too small for Option.Some" }
            buffer[offset] = 1 // discriminant for Some
            value.writeToBuffer(buffer, offset + 4)
        }

        override fun sizeInBytes(): Int {
            return 4 + value.sizeInBytes()
        }

        override fun createReader() = reader(value.createReader())

        override fun toString() = "Some($value)"
        override fun toString(memory: WasmMemory) = "Some(${value.toString(memory)})"

        companion object {
            fun <T : WasmType> reader(valueReader: WasmMemoryReader<T>) = run {
                object : WasmMemoryReader<Some<T>> {
                    override val elementSize = 4 + valueReader.elementSize
                    override fun read(memory: WasmMemory, offset: Int): Some<T> {
                        val discriminant = memory.uint8(offset)
                        require(discriminant.toInt() == 1) { "Expected Some discriminant (1), got $discriminant" }

                        val readValue = valueReader.read(memory, offset + 4)
                        return Some(readValue)
                    }
                }
            }
        }
    }

    inline val isSome: Boolean get() = this is Some<T>
    inline val isNone: Boolean get() = this is None

    companion object {
        fun <T : WasmType> reader(valueReader: WasmMemoryReader<T>): WasmMemoryReader<Option<T>> {
            return object : WasmMemoryReader<Option<T>> {
                override val elementSize = 4 + valueReader.elementSize

                override fun read(memory: WasmMemory, offset: Int): Option<T> {
                    val discriminant = memory.uint8(offset).toInt()
                    return when (discriminant) {
                        0 -> None
                        1 -> Some(valueReader.read(memory, offset + 4))
                        else -> error("Invalid discriminant for Option: $discriminant. Expected 0 (None) or 1 (Some).")
                    }
                }
            }
        }
    }
}

inline fun <T : WasmType> Option<T>.onSome(block: (T) -> Unit): Option<T> {
    if (this is Option.Some) block(value)
    return this
}

inline fun <T : WasmType> Option<T>.onNone(block: () -> Unit): Option<T> {
    if (this is Option.None) block()
    return this
}

inline fun <T : WasmType, R> Option<T>.map(transform: (T) -> R): R? {
    return when (this) {
        is Option.Some -> transform(value)
        is Option.None -> null
    }
}

fun <T : WasmType> Option<T>.getOrNull(): T? {
    return when (this) {
        is Option.Some -> value
        is Option.None -> null
    }
}

fun <T : WasmType> Option<T>.getOrDefault(default: T): T {
    return when (this) {
        is Option.Some -> value
        is Option.None -> default
    }
}

/**
 * [tag]: 0 = None, 1 = Some
 */
fun <T : WasmType> Option(tag: Int, value: T? = null): Option<T> = when (tag) {
    0 -> Option.None
    1 -> Option.Some(requireNotNull(value) { "Some(...) value cannot be null" })
    else -> throw IllegalArgumentException("Invalid Option tag: $tag. Expected 0 (None) or 1 (Some).")
}

fun <T : WasmType> Option(tag: Byte, value: T? = null) = Option(tag.toInt(), value)
fun <T : WasmType> Option(tag: UByte, value: T? = null) = Option(tag.toInt(), value)

typealias None = Option.None

fun <T : WasmType> Some(value: T) = Option.Some(value)
