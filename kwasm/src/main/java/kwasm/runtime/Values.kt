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

@file:Suppress("EXPERIMENTAL_API_USAGE")

package kwasm.runtime

import kwasm.KWasmRuntimeException
import kwasm.ast.AstNode
import kwasm.ast.type.ValueType
import kwasm.util.Impossible
import kotlin.reflect.KClass

/**
 * Defines a value for use in WebAssembly at runtime.
 *
 * From [the docs](https://webassembly.github.io/spec/core/exec/runtime.html#values):
 *
 * WebAssembly computations manipulate values of the four basic value types: integers and
 * floating-point data of 32 or 64 bit width each, respectively.
 *
 * In most places of the semantics, values of different types can occur. In order to avoid
 * ambiguities, values are therefore represented with an abstract syntax that makes their type
 * explicit. It is convenient to reuse the same notation as for the `const` instructions producing
 * them:
 *
 * ```
 *   val    ::= i32.const i32
 *              i64.const i64
 *              f32.const f32
 *              f64.const f64
 * ```
 */
interface Value<T : Number> : StackElement, AstNode {
    val value: T
}

/**
 * Represents an empty value. Should only ever be used as a return-type for a void/Unit
 * [kwasm.api.HostFunction].
 */
object EmptyValue : Value<Byte> {
    override val value: Byte = 1
}

/** Holds a 32-bit integer [Value]. */
inline class IntValue(override val value: Int) : Value<Int> {
    val unsignedValue: UInt
        get() = value.toUInt()

    companion object {
        val ZERO = IntValue(0)
    }
}

/** Holds a 64-bit integer [Value]. */
inline class LongValue(override val value: Long) : Value<Long> {
    val unsignedValue: ULong
        get() = value.toULong()

    companion object {
        val ZERO = LongValue(0L)
    }
}

/** Holds a 32-bit floating-point [Value]. */
inline class FloatValue(override val value: Float) : Value<Float> {
    companion object {
        val ZERO = FloatValue(0f)
    }
}

/** Holds a 64-bit floating-point [Value]. */
inline class DoubleValue(override val value: Double) : Value<Double> {
    companion object {
        val ZERO = DoubleValue(0.0)
    }
}

/** Wraps an [Int] in an [IntValue]. */
fun Int.toValue(): IntValue = IntValue(this)

/** Wraps a [UInt] in an [IntValue]. */
fun UInt.toValue(): IntValue = IntValue(this.toInt())

/** Wraps a [Long] in a [LongValue]. */
fun Long.toValue(): LongValue = LongValue(this)

/** Wraps a [ULong] in a [LongValue]. */
fun ULong.toValue(): LongValue = LongValue(this.toLong())

/** Wraps a [Float] in a [FloatValue]. */
fun Float.toValue(): FloatValue = FloatValue(this)

/** Wraps a [Double] in a [DoubleValue]. */
fun Double.toValue(): DoubleValue = DoubleValue(this)

/** Wraps the receiving value as a [Value] instance. */
@Suppress("UNCHECKED_CAST")
inline fun <reified T : Number> T.toValue(): Value<*> = when (this) {
    is Int -> IntValue(this)
    is Long -> LongValue(this)
    is Float -> FloatValue(this)
    is Double -> DoubleValue(this)
    else -> throw KWasmRuntimeException("Unsupported type: ${T::class} for Value conversion")
}

/** Converts a [KClass] for a [Value] class to a [ValueType]. */
fun KClass<out Value<*>>.toValueType(): ValueType? = when (this) {
    IntValue::class -> ValueType.I32
    LongValue::class -> ValueType.I64
    FloatValue::class -> ValueType.F32
    DoubleValue::class -> ValueType.F64
    EmptyValue::class -> null
    else -> Impossible("Unsupported Value: $this")
}

/** Gets the default zero [Value] for the receiving [ValueType]. */
val ValueType.zeroValue: Value<*>
    get() = when (this) {
        ValueType.I32 -> IntValue.ZERO
        ValueType.I64 -> LongValue.ZERO
        ValueType.F32 -> FloatValue.ZERO
        ValueType.F64 -> DoubleValue.ZERO
    }

/** Checks the receiving [Value] against an expected [ValueType] */
fun Value<*>.checkType(expected: ValueType) {
    if (!isType(expected)) throw KWasmRuntimeException(
        "Expected type: $expected, but found ${this::class.toValueType()}"
    )
}

/** Checks the receiving [Value] against an expected [ValueType] */
fun Value<*>.isType(expected: ValueType): Boolean {
    return when (expected) {
        ValueType.I32 -> this is IntValue
        ValueType.I64 -> this is LongValue
        ValueType.F32 -> this is FloatValue
        ValueType.F64 -> this is DoubleValue
    }
}
