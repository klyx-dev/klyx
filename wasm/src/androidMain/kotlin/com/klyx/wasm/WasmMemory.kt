package com.klyx.wasm

import com.dylibso.chicory.runtime.Memory
import com.klyx.asIntPair
import com.klyx.wasm.utils.toBytesLE
import java.nio.charset.Charset

@ExperimentalWasmApi
class WasmMemory internal constructor(
    val instance: WasmInstance,
    private val memory: Memory
) {
    fun readString(addr: Int, len: Int, charset: Charset = Charsets.UTF_8): String = run {
        memory.readString(addr, len, charset)
    }

    fun writeString(offset: Int, data: String, charset: Charset = Charsets.UTF_8) {
        memory.writeString(offset, data, charset)
    }

    fun write(addr: Int, data: ByteArray) = memory.write(addr, data)

    fun read(addr: Int) = memory.read(addr)
    fun byte(addr: Int) = read(addr)

    fun readShort(addr: Int) = memory.readShort(addr)
    fun readInt(addr: Int) = memory.readInt(addr)
    fun readLong(addr: Int) = memory.readLong(addr)
    fun readFloat(addr: Int) = memory.readFloat(addr)
    fun readDouble(addr: Int) = memory.readDouble(addr)

    fun readU8(addr: Int) = read(addr).toUByte()
    fun readU16(addr: Int) = readShort(addr).toUShort()
    fun readU32(addr: Int) = readInt(addr).toUInt()

    fun readI8(addr: Int) = read(addr)
    fun readI16(addr: Int) = readShort(addr)
    fun readI32(addr: Int) = readInt(addr)
    fun readI64(addr: Int) = readLong(addr)

    fun readF32(addr: Int) = memory.readF32(addr).toInt()
    fun readF64(addr: Int) = memory.readF64(addr)

    fun ubyte(addr: Int) = readU8(addr)
    fun uint8(addr: Int) = readU8(addr)
    fun uint16(addr: Int) = readU16(addr)
    fun uint32(addr: Int) = readU32(addr)

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
fun WasmMemory.writeString(data: String) = write(data.toBytesLE())
