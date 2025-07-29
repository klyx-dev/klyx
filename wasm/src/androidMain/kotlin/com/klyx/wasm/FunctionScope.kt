package com.klyx.wasm

@ExperimentalWasm
class FunctionScope internal constructor(
    val instance: WasmInstance
) {
    /**
     * Reads a UTF-8 string from WASM memory
     */
    val LongArray.string get() = instance.memory.readString(this)
    fun LongArray.string(offset: Int = 0) = instance.memory.readString(this, offset)

    /**
     * Returns the first [Int] of this array
     */
    val LongArray.i32 get() = this.first().i32
    fun LongArray.i32(index: Int = 0) = this[index].i32

    /**
     * Returns the first [Float] of this array
     */
    val LongArray.f32 get() = this.first().f32
    fun LongArray.f32(index: Int = 0) = this[index].f32

    /**
     * Returns the first [Long] of this array
     */
    val LongArray.i64 get() = this.first().i64
    fun LongArray.i64(index: Int = 0) = this[index].i64

    /**
     * Returns the first [Double] of this array
     */
    val LongArray.f64 get() = this.first().f64
    fun LongArray.f64(index: Int = 0) = this[index].f64
}
