@file:OptIn(ExperimentalWasmApi::class)

package com.klyx.wasm.type

import com.klyx.wasm.ExperimentalWasmApi
import com.klyx.wasm.WasmMemory

sealed class Result<out T : WasmValue, out E : WasmValue> : WasmValue {
    data class Ok<T : WasmValue>(val value: T) : Result<T, Nothing>() {
        override fun writeToBuffer(buffer: ByteArray, offset: Int) {
            require(offset + sizeInBytes() <= buffer.size) { "Buffer too small for Result.Ok" }
            buffer[offset] = 0 // discriminant for Ok
            for (i in 1 until 4) {
                buffer[offset + i] = 0
            }
            value.writeToBuffer(buffer, offset + 4)
        }

        override fun sizeInBytes(): Int = 4 + value.sizeInBytes()

        override fun createReader() = reader(value.createReader())

        override fun toString() = "Ok($value)"
        override fun toString(memory: WasmMemory) = "Ok(${value.toString(memory)})"

        companion object {
            fun <T : WasmValue> reader(okReader: WasmMemoryReader<T>) = run {
                object : WasmMemoryReader<Ok<T>> {
                    override val elementSize: Int = 4 + okReader.elementSize

                    override fun read(memory: WasmMemory, offset: Int): Ok<T> {
                        val discriminant = memory.readU8(offset)
                        require(discriminant.toInt() == 0) { "Expected Ok discriminant (0), got $discriminant" }
                        return Ok(okReader.read(memory, offset + 4))
                    }
                }
            }
        }
    }

    data class Err<E : WasmValue>(val error: E) : Result<Nothing, E>() {
        override fun writeToBuffer(buffer: ByteArray, offset: Int) {
            require(offset + sizeInBytes() <= buffer.size) { "Buffer too small for Result.Err" }
            buffer[offset] = 1 // discriminant for Err
            for (i in 1 until 4) {
                buffer[offset + i] = 0
            }
            error.writeToBuffer(buffer, offset + 4)
        }

        override fun sizeInBytes(): Int = 4 + error.sizeInBytes()

        override fun createReader() = reader(error.createReader())

        override fun toString() = "Err($error)"
        override fun toString(memory: WasmMemory) = "Err(${error.toString(memory)})"

        companion object {
            fun <E : WasmValue> reader(errReader: WasmMemoryReader<E>) = run {
                object : WasmMemoryReader<Err<E>> {
                    override val elementSize: Int = 4 + errReader.elementSize

                    override fun read(memory: WasmMemory, offset: Int): Err<E> {
                        val discriminant = memory.readU8(offset)
                        require(discriminant.toInt() == 1) { "Expected Err discriminant (1), got $discriminant" }
                        return Err(errReader.read(memory, offset + 4))
                    }
                }
            }
        }
    }

    inline val isOk get() = this is Ok
    inline val isErr get() = this is Err

    inline fun onOk(block: (T) -> Unit): Result<T, E> {
        if (this is Ok) block(value)
        return this
    }

    inline fun onErr(block: (E) -> Unit): Result<T, E> {
        if (this is Err) block(error)
        return this
    }

    inline fun <R : WasmValue> map(transform: (T) -> R) = when (this) {
        is Ok -> Ok(transform(value))
        is Err -> this
    }

    inline fun <F : WasmValue> mapErr(transform: (E) -> F) = when (this) {
        is Ok -> this
        is Err -> Err(transform(error))
    }

    companion object {
        fun <T : WasmValue, E : WasmValue> reader(
            okReader: WasmMemoryReader<T>,
            errReader: WasmMemoryReader<E>
        ): WasmMemoryReader<Result<T, E>> {
            return object : WasmMemoryReader<Result<T, E>> {
                override val elementSize = 4 + maxOf(okReader.elementSize, errReader.elementSize)

                override fun read(memory: WasmMemory, offset: Int): Result<T, E> {
                    val discriminant = memory.readU8(offset).toInt()
                    return when (discriminant) {
                        0 -> Ok(okReader.read(memory, offset + 4))
                        1 -> Err(errReader.read(memory, offset + 4))
                        else -> error("Invalid discriminant for Result: $discriminant. Expected 0 (Ok) or 1 (Err).")
                    }
                }
            }
        }
    }
}

/**
 * [tag]: 0 = Ok, 1 = Err
 */
@OptIn(ExperimentalWasmApi::class)
context(memory: WasmMemory)
fun <T : WasmValue> Result(
    tag: Int,
    okValue: T?,
    err: String? = null
): Result<T, WasmString> = when (tag) {
    0 -> Result.Ok(requireNotNull(okValue) { "Ok value cannot be null" })
    1 -> Result.Err(WasmString(err ?: "Unknown error"))
    else -> throw IllegalArgumentException("Invalid result code: $tag")
}

context(memory: WasmMemory)
fun <T : WasmValue> Result(
    tag: Byte, okValue: T?, err: String? = null
) = Result(tag.toInt(), okValue, err)

context(memory: WasmMemory)
fun <T : WasmValue> Result(
    tag: UByte, okValue: T?, err: String? = null
) = Result(tag.toInt(), okValue, err)

fun <T : WasmValue> Ok(value: T) = Result.Ok(value)
fun <E : WasmValue> Err(error: E) = Result.Err(error)
