package com.klyx.wasm

import com.dylibso.chicory.runtime.Memory
import com.klyx.asIntPair
import com.klyx.pointer.Pointer
import com.klyx.wasm.utils.i32
import java.nio.charset.Charset

@ExperimentalWasmApi
class WasmMemory internal constructor(
    internal val instance: WasmInstance,
    private val memory: Memory
) {
    fun readString(addr: Int, len: Int, charset: Charset = Charsets.UTF_8): String = run {
        memory.readString(addr, len, charset)
    }

    fun writeString(offset: Int, data: String, charset: Charset = Charsets.UTF_8) {
        memory.writeString(offset, data, charset)
    }

    fun write(addr: Int, data: ByteArray, offset: Int = 0, size: Int = data.size) {
        memory.write(addr, data, offset, size)
    }

    fun writeByte(addr: Int, data: Byte) = memory.writeByte(addr, data)
    fun writeShort(addr: Int, data: Short) = memory.writeShort(addr, data)
    fun writeInt(addr: Int, data: Int) = memory.writeI32(addr, data)
    fun writeLong(addr: Int, data: Long) = memory.writeLong(addr, data)
    fun writeFloat(addr: Int, data: Float) = memory.writeF32(addr, data)
    fun writeDouble(addr: Int, data: Double) = memory.writeF64(addr, data)

    fun writeU8(addr: Int, data: UByte) = writeByte(addr, data.toByte())
    fun writeU16(addr: Int, data: UShort) = writeShort(addr, data.toShort())
    fun writeU32(addr: Int, data: UInt) = writeInt(addr, data.toInt())
    fun writeU64(addr: Int, data: ULong) = writeLong(addr, data.toLong())

    fun writeI8(addr: Int, data: Byte) = writeByte(addr, data)
    fun writeI16(addr: Int, data: Short) = writeShort(addr, data)
    fun writeI32(addr: Int, data: Int) = writeInt(addr, data)
    fun writeI64(addr: Int, data: Long) = writeLong(addr, data)

    fun writeF32(addr: Int, data: Float) = writeFloat(addr, data)
    fun writeF64(addr: Int, data: Double) = writeDouble(addr, data)

    fun read(addr: Int) = memory.read(addr)

    fun readShort(addr: Int) = memory.readShort(addr)
    fun readInt(addr: Int) = memory.readInt(addr)
    fun readLong(addr: Int) = memory.readLong(addr)

    fun readU8(addr: Int) = read(addr).toUByte()
    fun readU16(addr: Int) = readShort(addr).toUShort()
    fun readU32(addr: Int) = readInt(addr).toUInt()
    fun readU64(addr: Int) = readInt(addr).toULong()

    fun readI8(addr: Int) = read(addr)
    fun readI16(addr: Int) = readShort(addr)
    fun readI32(addr: Int) = readInt(addr)
    fun readI64(addr: Int) = readLong(addr)

    fun readF32(addr: Int) = Float.fromBits(memory.readF32(addr).toInt())
    fun readF64(addr: Int) = Double.fromBits(memory.readF64(addr))

    fun uint8(addr: Int) = readU8(addr)
    fun uint16(addr: Int) = readU16(addr)
    fun uint32(addr: Int) = readU32(addr)
    fun uint64(addr: Int) = readU64(addr)

    fun int8(addr: Int) = readI8(addr)
    fun int16(addr: Int) = readI16(addr)
    fun int32(addr: Int) = readI32(addr)
    fun int64(addr: Int) = readI64(addr)

    fun float32(addr: Int) = readF32(addr)
    fun float64(addr: Int) = readF64(addr)

    fun string(addr: Int, len: Int) = readString(addr, len)
    fun string(addr: UInt, len: UInt) = readString(addr.toInt(), len.toInt())
    fun readBytes(addr: Int, len: Int): ByteArray = memory.readBytes(addr, len)
}

@ExperimentalWasmApi
fun WasmMemory.readString(args: LongArray, offset: Int = 0) = run {
    val (addr, len) = args.takePair(offset).asIntPair()
    readString(addr, len)
}

@ExperimentalWasmApi
fun WasmMemory.write(data: ByteArray) = run {
    val ptr = instance.alloc(data.size)
    write(ptr, data)
    ptr to data.size
}

@ExperimentalWasmApi
fun WasmMemory.readLoweredString(ptr: Int): String {
    val strPtr = uint32(ptr)
    val strLen = uint32(ptr + 4)
    return string(strPtr, strLen)
}

@ExperimentalWasmApi
fun WasmMemory.readLoweredString(ptr: UInt) = readLoweredString(ptr.toInt())

@ExperimentalWasmApi
fun WasmMemory.readLoweredString(ptr: Pointer) = readLoweredString(ptr.raw.i32)

@ExperimentalWasmApi
fun WasmMemory.readStringList(addr: Int, len: Int? = null) = run {
    val ptr = uint32(addr)
    val ulen = len?.toUInt() ?: uint32(addr + 4)
    (0u until ulen).map { i -> readLoweredString(ptr + i * 8u) }
}

/**
 * @see WasmInstance.alloc
 */
@ExperimentalWasmApi
fun WasmMemory.allocate(size: Int, align: Int = 1) = instance.alloc(size, align)
