package com.klyx.wasm.utils

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

val Long.i32 get() = toInt()
val Long.i64 get() = this
val Long.f32 get() = toFloat()
val Long.f64 get() = toDouble()
