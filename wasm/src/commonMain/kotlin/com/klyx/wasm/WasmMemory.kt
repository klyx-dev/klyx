package com.klyx.wasm

import com.klyx.wasm.internal.InternalExperimentalWasmApi
import com.klyx.wasm.internal.toLengthPrefixedUtf8ByteArray
import io.github.charlietap.chasm.embedding.error.ChasmError
import io.github.charlietap.chasm.embedding.memory.readByte
import io.github.charlietap.chasm.embedding.memory.readBytes
import io.github.charlietap.chasm.embedding.memory.readDouble
import io.github.charlietap.chasm.embedding.memory.readFloat
import io.github.charlietap.chasm.embedding.memory.readInt
import io.github.charlietap.chasm.embedding.memory.readLong
import io.github.charlietap.chasm.embedding.memory.readNullTerminatedUtf8String
import io.github.charlietap.chasm.embedding.memory.readUtf8String
import io.github.charlietap.chasm.embedding.memory.writeByte
import io.github.charlietap.chasm.embedding.memory.writeBytes
import io.github.charlietap.chasm.embedding.memory.writeDouble
import io.github.charlietap.chasm.embedding.memory.writeFloat
import io.github.charlietap.chasm.embedding.memory.writeInt
import io.github.charlietap.chasm.embedding.memory.writeLong
import io.github.charlietap.chasm.embedding.memory.writeUtf8String
import io.github.charlietap.chasm.embedding.shapes.ChasmResult
import io.github.charlietap.chasm.embedding.shapes.Memory
import io.github.charlietap.chasm.embedding.shapes.Store
import io.itsvks.anyhow.getOrThrow
import kotlin.jvm.JvmSynthetic

@ExperimentalWasmApi
class WasmMemory internal constructor(
    private val instance: WasmInstance,
    private val memory: Memory,
    private val store: Store
) {
    fun writeBytes(pointer: Int, bytes: ByteArray) = writeBytes(store, memory, pointer, bytes).value()
    fun writeByte(pointer: Int, byte: Byte) = writeByte(store, memory, pointer, byte).value()
    fun writeInt(pointer: Int, int: Int) = writeInt(store, memory, pointer, int).value()
    fun writeFloat(pointer: Int, float: Float) = writeFloat(store, memory, pointer, float).value()
    fun writeDouble(pointer: Int, double: Double) = writeDouble(store, memory, pointer, double).value()
    fun writeLong(pointer: Int, long: Long) = writeLong(store, memory, pointer, long).value()
    fun writeUtf8String(pointer: Int, string: String) = writeUtf8String(store, memory, pointer, string).value()

    fun write(pointer: Int, bytes: ByteArray) = writeBytes(pointer, bytes)
    fun write(pointer: Int, int: Int) = writeInt(pointer, int)
    fun write(pointer: Int, float: Float) = writeFloat(pointer, float)
    fun write(pointer: Int, double: Double) = writeDouble(pointer, double)
    fun write(pointer: Int, byte: Byte) = writeByte(pointer, byte)

    fun allocateAndWrite(bytes: ByteArray) = run {
        val ptr = instance.alloc(bytes.size)
        writeBytes(ptr, bytes)
        ptr to bytes.size
    }

    fun allocateAndWriteUtf8String(string: String) = allocateAndWrite(string.toLengthPrefixedUtf8ByteArray())

    fun readBytes(pointer: Int, bytesToRead: Int): ByteArray {
        return readBytes(store, memory, ByteArray(bytesToRead), pointer, bytesToRead).value()
    }

    fun readByte(pointer: Int) = readByte(store, memory, pointer).value()
    fun readInt(pointer: Int) = readInt(store, memory, pointer).value()
    fun readFloat(pointer: Int) = readFloat(store, memory, pointer).value()
    fun readDouble(pointer: Int) = readDouble(store, memory, pointer).value()
    fun readLong(pointer: Int) = readLong(store, memory, pointer).value()

    fun readNullTerminatedUtf8String(pointer: Int) = readNullTerminatedUtf8String(store, memory, pointer).value()

    fun readLengthPrefixedUtf8String(pointer: Int) = run {
        val ptr = readInt(pointer)
        val length = readInt(pointer + 4)
        readUtf8String(ptr, length)
    }

    fun readUtf8String(pointer: Int, stringLengthInBytes: Int): String {
        return readUtf8String(store, memory, pointer, stringLengthInBytes).value()
    }

    /**
     * @see WasmInstance.alloc
     */
    @InternalExperimentalWasmApi
    fun allocate(size: Int, align: Int = 1) = instance.alloc(size, align)
}

@JvmSynthetic
private fun <V, E : ChasmError> ChasmResult<V, E>.value() = asResult().getOrThrow(::WasmRuntimeException)
