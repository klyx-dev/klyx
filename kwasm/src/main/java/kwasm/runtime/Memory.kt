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

package kwasm.runtime

import kwasm.api.MemoryProvider
import kwasm.ast.type.MemoryType
import kotlin.math.ceil

/**
 * Defines runtime memory for use by a wasm program, along with facilities for accessing and
 * mutating memory by the host JVM application.
 *
 * From [the docs](https://webassembly.github.io/spec/core/exec/runtime.html#memory-instances):
 *
 * A memory instance is the runtime representation of a linear memory. It holds a vector of bytes
 * and an optional maximum size, if one was specified at the definition site of the memory.
 *
 * ```
 *   meminst ::= {data vec(byte), max u32?}
 * ```
 *
 * The length of the vector always is a multiple of the WebAssembly page size, which is defined to
 * be the constant `65536` – abbreviated `64Ki`. Like in a memory type, the maximum size in a memory
 * instance is given in units of this page size.
 *
 * The bytes can be mutated through memory instructions, the execution of a data segment, or by
 * external means provided by the embedder.
 *
 * It is an invariant of the semantics that the length of the byte vector, divided by page size,
 * never exceeds the maximum size, if present.
 */
@Suppress("EXPERIMENTAL_API_USAGE")
interface Memory {
    /**
     * The size of the [Memory] as a number of pages, where each page is [PAGE_SIZE] bytes.
     */
    val sizePages: Int

    /**
     * Returns the size of the [Memory] as the total number of bytes available.
     *
     * **Note:** this is equivalent to [sizePages] * [PAGE_SIZE].
     */
    val sizeBytes: Int
        get() = sizePages * PAGE_SIZE

    /** Runs the [block] within a thread-safe mutual exclusion. */
    suspend fun <T : Any?> lock(block: suspend Memory.() -> T): T

    /** Reads an [Int] from the [Memory] at the given [offset]. */
    fun readInt(offset: Int, byteWidth: Int = 4, alignment: Int = 0): Int

    /** Reads a [UInt] from the [Memory] at the given [offset]. */
    fun readUInt(offset: Int, byteWidth: Int = 4, alignment: Int = 0): UInt

    /** Reads a [Long] from the [Memory] at the given [offset]. */
    fun readLong(offset: Int, byteWidth: Int = 8, alignment: Int = 0): Long

    /** Reads a [ULong] from the [Memory] at the given [offset]. */
    fun readULong(offset: Int, byteWidth: Int = 8, alignment: Int = 0): ULong

    /** Reads a [Float] from the [Memory] at the given [offset]. */
    fun readFloat(offset: Int, alignment: Int = 0): Float

    /** Reads a [Double] from the [Memory] at the given [offset]. */
    fun readDouble(offset: Int, alignment: Int = 0): Double

    /** Writes an [Int] to the [Memory] at the given [offset]. */
    fun writeInt(value: Int, offset: Int, byteWidth: Int = 4, alignment: Int = 0)

    /** Writes a [UInt] to the [Memory] at the given [offset]. */
    fun writeUInt(value: UInt, offset: Int, byteWidth: Int = 4, alignment: Int = 0)

    /** Writes a [Long] to the [Memory] at the given [offset]. */
    fun writeLong(value: Long, offset: Int, byteWidth: Int = 8, alignment: Int = 0)

    /** Writes a [ULong] to the [Memory] at the given [offset]. */
    fun writeULong(value: ULong, offset: Int, byteWidth: Int = 8, alignment: Int = 0)

    /** Writes a [Float] to the [Memory] at the given [offset]. */
    fun writeFloat(value: Float, offset: Int, alignment: Int = 0)

    /** Writes a [Double] to the [Memory] at the given [offset]. */
    fun writeDouble(value: Double, offset: Int, alignment: Int = 0)

    /**
     * Reads [length] bytes starting at the given [memoryOffset] from the [Memory] into the supplied
     * [ByteArray], starting at [outOffset] and returns the number of bytes which were able to be
     * read.
     */
    fun readBytes(
        out: ByteArray,
        memoryOffset: Int,
        outOffset: Int = 0,
        length: Int = out.size
    ): Int

    /**
     * Writes a [ByteArray] into the [Memory] at the given [offset].
     *
     * Optional arguments [valueOffset] and [valueLength] allow the caller to specify a subsequence
     * of bytes from the [value] to write. Defaults for these arguments result in the entirety of
     * the [value] being written.
     */
    fun writeBytes(
        value: ByteArray,
        offset: Int,
        valueOffset: Int = 0,
        valueLength: Int = value.size
    )

    /**
     * Grows the [Memory] by the specified number of [newPages] (each page's size = [PAGE_SIZE]).
     *
     * @return the size of the [Memory] before the grow operation if the grow was successful, else
     *  [GROW_FAILURE].
     */
    fun growBy(newPages: Int): Int

    companion object {
        const val PAGE_SIZE = 65536
        const val GROW_FAILURE = -1

        /** Returns the minimum number of pages required to hold the specified number of [bytes]. */
        fun pagesForBytes(bytes: Long): Int = ceil(bytes / PAGE_SIZE.toDouble()).toInt()
    }
}

/**
 * From [the docs](https://webassembly.github.io/spec/core/exec/modules.html#alloc-mem):
 *
 * 1. Let `memtype` be the memory type to allocate.
 * 1. Let `{min n, max m?}` be the structure of memory type `memtype`.
 * 1. Let `a` be the first free memory address in `S`.
 * 1. Let `meminst` be the memory instance `{data (0x00)^(n⋅64Ki), max m?}` that contains
 *    `n` pages of zeroed bytes.
 * 1. Append `meminst` to the `mems` of `S`.
 * 1. Return `a`.
 */
fun Store.allocate(
    memoryProvider: MemoryProvider,
    memoryType: MemoryType
): Store.Allocation<Address.Memory> {
    val memory = memoryProvider.buildMemory(
        memoryType.limits.min.toInt(),
        memoryType.limits.max?.toInt()
    )
    return allocateMemory(memory)
}
