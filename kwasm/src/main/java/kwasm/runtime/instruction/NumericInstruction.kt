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

@file:Suppress("EXPERIMENTAL_UNSIGNED_LITERALS", "EXPERIMENTAL_API_USAGE")

package kwasm.runtime.instruction

import kwasm.KWasmRuntimeException
import kwasm.ast.instruction.NumericInstruction
import kwasm.runtime.DoubleValue
import kwasm.runtime.ExecutionContext
import kwasm.runtime.FloatValue
import kwasm.runtime.IntValue
import kwasm.runtime.LongValue
import kwasm.runtime.Value
import kwasm.runtime.toValue
import kotlin.math.absoluteValue
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.round
import kotlin.math.sqrt
import kotlin.math.truncate

private const val EXCEPTION_INTEGER_DIVIDE_BY_ZERO =
    "Cannot divide by zero. (integer divide by zero)"
private const val EXCEPTION_TRUNCATE_NAN =
    "Cannot truncate NaN (invalid conversion to integer)"
private const val EXCEPTION_TRUNCATE_INF =
    "Cannot truncate Infinity (invalid conversion to integer|integer overflow)"

/**
 * See
 * [the docs](https://webassembly.github.io/spec/core/exec/instructions.html#numeric-instructions):
 */
@OptIn(ExperimentalStdlibApi::class)
internal fun NumericInstruction.execute(context: ExecutionContext): ExecutionContext {
    when (this) {
        NumericInstruction.I32Add -> binaryOp<IntValue>(context) { x, y ->
            (x.value + y.value).toValue()
        }
        NumericInstruction.I32CountLeadingZeroes -> unaryOp(context) { x: IntValue ->
            if (x.value == 0) 32.toValue()
            else {
                var xVal = x.unsignedValue
                var total = 0
                var startWidth = 16
                var mask = 0xFFFF0000u
                while (startWidth > 0) {
                    if (xVal and mask == 0u) {
                        xVal = xVal shl startWidth
                        total += startWidth
                    }
                    startWidth = startWidth ushr 1
                    mask = mask shl startWidth
                }
                total.toValue()
            }
        }
        NumericInstruction.I32CountTrailingZeroes -> unaryOp(context) { x: IntValue ->
            if (x.value == 0) 32.toValue()
            else {
                var xVal = x.unsignedValue
                var total = 0
                var endWidth = 16
                var mask = 0xFFFFu
                while (endWidth > 0) {
                    if (xVal and mask == 0u) {
                        xVal = xVal shr endWidth
                        total += endWidth
                    }
                    endWidth = endWidth ushr 1
                    mask = mask shr endWidth
                }
                total.toValue()
            }
        }
        NumericInstruction.I32CountNonZeroBits -> unaryOp(context) { x: IntValue ->
            when (x.value) {
                0 -> 0.toValue()
                -1 -> 32.toValue()
                else -> {
                    var mask = 1
                    var total = 0
                    repeat(31) {
                        if (mask and x.value != 0) total++
                        mask = mask shl 1
                    }
                    total.toValue()
                }
            }
        }
        NumericInstruction.I32Subtract -> binaryOp<IntValue>(context) { x, y ->
            (x.value - y.value).toValue()
        }
        NumericInstruction.I32Multiply -> binaryOp<IntValue>(context) { x, y ->
            (x.value * y.value).toValue()
        }
        NumericInstruction.I32DivideSigned -> binaryOp<IntValue>(context) { x, y ->
            if (y.value == 0) {
                throw KWasmRuntimeException(EXCEPTION_INTEGER_DIVIDE_BY_ZERO)
            } else if (x.value == Int.MIN_VALUE && y.value == -1) {
                throw KWasmRuntimeException(
                    "Quotient unrepresentable as 32bit integer. (integer overflow)"
                )
            }
            (x.value / y.value).toValue()
        }
        NumericInstruction.I32DivideUnsigned -> binaryOp<IntValue>(context) { x, y ->
            if (y.value == 0) throw KWasmRuntimeException(EXCEPTION_INTEGER_DIVIDE_BY_ZERO)
            (x.unsignedValue / y.unsignedValue).toValue()
        }
        NumericInstruction.I32RemainderSigned -> binaryOp<IntValue>(context) { x, y ->
            if (y.value == 0) throw KWasmRuntimeException(EXCEPTION_INTEGER_DIVIDE_BY_ZERO)
            (x.value % y.value).toValue()
        }
        NumericInstruction.I32RemainderUnsigned -> binaryOp<IntValue>(context) { x, y ->
            if (y.value == 0) throw KWasmRuntimeException(EXCEPTION_INTEGER_DIVIDE_BY_ZERO)
            (x.unsignedValue % y.unsignedValue).toValue()
        }
        NumericInstruction.I32BitwiseAnd -> binaryOp<IntValue>(context) { x, y ->
            (x.value and y.value).toValue()
        }
        NumericInstruction.I32BitwiseOr -> binaryOp<IntValue>(context) { x, y ->
            (x.value or y.value).toValue()
        }
        NumericInstruction.I32BitwiseXor -> binaryOp<IntValue>(context) { x, y ->
            (x.value xor y.value).toValue()
        }
        NumericInstruction.I32ShiftLeft -> binaryOp<IntValue>(context) { x, y ->
            val distance = (y.unsignedValue % 32u).toInt()
            (x.value shl distance).toValue()
        }
        NumericInstruction.I32ShiftRightSigned -> binaryOp<IntValue>(context) { x, y ->
            val distance = (y.unsignedValue % 32u).toInt()
            (x.value shr distance).toValue()
        }
        NumericInstruction.I32ShiftRightUnsigned -> binaryOp<IntValue>(context) { x, y ->
            val distance = (y.unsignedValue % 32u).toInt()
            (x.value ushr distance).toValue()
        }
        NumericInstruction.I32RotateLeft -> binaryOp<IntValue>(context) { x, y ->
            val distance = (y.unsignedValue % 32u).toInt()
            ((x.value shl distance) or (x.value ushr -distance)).toValue()
        }
        NumericInstruction.I32RotateRight -> binaryOp<IntValue>(context) { x, y ->
            val distance = (y.unsignedValue % 32u).toInt()
            ((x.value ushr distance) or (x.value shl -distance)).toValue()
        }
        NumericInstruction.I32EqualsZero -> testOp(context) { x: IntValue -> x.value == 0 }
        NumericInstruction.I32Equals -> relOp<IntValue>(context) { x, y -> x.value == y.value }
        NumericInstruction.I32NotEquals -> relOp<IntValue>(context) { x, y -> x.value != y.value }
        NumericInstruction.I32LessThanSigned -> relOp<IntValue>(context) { x, y ->
            x.value < y.value
        }
        NumericInstruction.I32LessThanUnsigned -> relOp<IntValue>(context) { x, y ->
            x.unsignedValue < y.unsignedValue
        }
        NumericInstruction.I32GreaterThanSigned -> relOp<IntValue>(context) { x, y ->
            x.value > y.value
        }
        NumericInstruction.I32GreaterThanUnsigned -> relOp<IntValue>(context) { x, y ->
            x.unsignedValue > y.unsignedValue
        }
        NumericInstruction.I32LessThanEqualToSigned -> relOp<IntValue>(context) { x, y ->
            x.value <= y.value
        }
        NumericInstruction.I32LessThanEqualToUnsigned -> relOp<IntValue>(context) { x, y ->
            x.unsignedValue <= y.unsignedValue
        }
        NumericInstruction.I32GreaterThanEqualToSigned -> relOp<IntValue>(context) { x, y ->
            x.value >= y.value
        }
        NumericInstruction.I32GreaterThanEqualToUnsigned -> relOp<IntValue>(context) { x, y ->
            x.unsignedValue >= y.unsignedValue
        }
        NumericInstruction.I64CountLeadingZeroes -> unaryOp(context) { x: LongValue ->
            if (x.value == 0L) 64L.toValue()
            else {
                var xVal = x.unsignedValue
                var total = 0L
                var mask = 0xFFFFFFFF00000000uL
                var startWidth = 32
                while (startWidth > 0) {
                    if (xVal and mask == 0uL) {
                        xVal = xVal shl startWidth
                        total += startWidth
                    }
                    startWidth = startWidth ushr 1
                    mask = mask shl startWidth
                }
                total.toValue()
            }
        }
        NumericInstruction.I64CountTrailingZeroes -> unaryOp(context) { x: LongValue ->
            if (x.value == 0L) 64L.toValue()
            else {
                var xVal = x.unsignedValue
                var total = 0L
                var endWidth = 32
                var mask = 0xFFFFFFFFuL
                while (endWidth > 0) {
                    if (xVal and mask == 0uL) {
                        xVal = xVal shr endWidth
                        total += endWidth
                    }
                    endWidth = endWidth ushr 1
                    mask = mask shr endWidth
                }
                total.toValue()
            }
        }
        NumericInstruction.I64CountNonZeroBits -> unaryOp(context) { x: LongValue ->
            when (x.value) {
                0L -> 0L.toValue()
                -1L -> 64L.toValue()
                else -> {
                    var mask = 1L
                    var total = 0L
                    repeat(63) {
                        if (mask and x.value != 0L) total++
                        mask = mask shl 1
                    }
                    total.toValue()
                }
            }
        }
        NumericInstruction.I64Add -> binaryOp<LongValue>(context) { x, y ->
            (x.value + y.value).toValue()
        }
        NumericInstruction.I64Subtract -> binaryOp<LongValue>(context) { x, y ->
            (x.value - y.value).toValue()
        }
        NumericInstruction.I64Multiply -> binaryOp<LongValue>(context) { x, y ->
            (x.value * y.value).toValue()
        }
        NumericInstruction.I64DivideSigned -> binaryOp<LongValue>(context) { x, y ->
            if (y.value == 0L) throw KWasmRuntimeException(EXCEPTION_INTEGER_DIVIDE_BY_ZERO)
            else if (x.value == Long.MIN_VALUE && y.value == -1L) {
                throw KWasmRuntimeException(
                    "Quotient unrepresentable as 64bit integer. (integer overflow)"
                )
            }
            (x.value / y.value).toValue()
        }
        NumericInstruction.I64DivideUnsigned -> binaryOp<LongValue>(context) { x, y ->
            if (y.value == 0L) throw KWasmRuntimeException(EXCEPTION_INTEGER_DIVIDE_BY_ZERO)
            (x.unsignedValue / y.unsignedValue).toValue()
        }
        NumericInstruction.I64RemainderSigned -> binaryOp<LongValue>(context) { x, y ->
            if (y.value == 0L) throw KWasmRuntimeException(EXCEPTION_INTEGER_DIVIDE_BY_ZERO)
            (x.value % y.value).toValue()
        }
        NumericInstruction.I64RemainderUnsigned -> binaryOp<LongValue>(context) { x, y ->
            if (y.value == 0L) throw KWasmRuntimeException(EXCEPTION_INTEGER_DIVIDE_BY_ZERO)
            (x.unsignedValue % y.unsignedValue).toValue()
        }
        NumericInstruction.I64BitwiseAnd -> binaryOp<LongValue>(context) { x, y ->
            (x.value and y.value).toValue()
        }
        NumericInstruction.I64BitwiseOr -> binaryOp<LongValue>(context) { x, y ->
            (x.value or y.value).toValue()
        }
        NumericInstruction.I64BitwiseXor -> binaryOp<LongValue>(context) { x, y ->
            (x.value xor y.value).toValue()
        }
        NumericInstruction.I64ShiftLeft -> binaryOp<LongValue>(context) { x, y ->
            (x.value shl (y.unsignedValue % 64uL).toInt()).toValue()
        }
        NumericInstruction.I64ShiftRightSigned -> binaryOp<LongValue>(context) { x, y ->
            (x.value shr (y.unsignedValue % 64uL).toInt()).toValue()
        }
        NumericInstruction.I64ShiftRightUnsigned -> binaryOp<LongValue>(context) { x, y ->
            (x.value ushr (y.unsignedValue % 64uL).toInt()).toValue()
        }
        NumericInstruction.I64RotateLeft -> binaryOp<LongValue>(context) { x, y ->
            val distance = (y.unsignedValue % 64uL).toInt()
            ((x.value shl distance) or (x.value ushr -distance)).toValue()
        }
        NumericInstruction.I64RotateRight -> binaryOp<LongValue>(context) { x, y ->
            val distance = (y.unsignedValue % 64uL).toInt()
            ((x.value ushr distance) or (x.value shl -distance)).toValue()
        }
        NumericInstruction.I64EqualsZero -> testOp(context) { x: LongValue -> x.value == 0L }
        NumericInstruction.I64Equals -> relOp<LongValue>(context) { x, y -> x.value == y.value }
        NumericInstruction.I64NotEquals -> relOp<LongValue>(context) { x, y -> x.value != y.value }
        NumericInstruction.I64LessThanSigned -> relOp<LongValue>(context) { x, y ->
            x.value < y.value
        }
        NumericInstruction.I64LessThanUnsigned -> relOp<LongValue>(context) { x, y ->
            x.unsignedValue < y.unsignedValue
        }
        NumericInstruction.I64GreaterThanSigned -> relOp<LongValue>(context) { x, y ->
            x.value > y.value
        }
        NumericInstruction.I64GreaterThanUnsigned -> relOp<LongValue>(context) { x, y ->
            x.unsignedValue > y.unsignedValue
        }
        NumericInstruction.I64LessThanEqualToSigned -> relOp<LongValue>(context) { x, y ->
            x.value <= y.value
        }
        NumericInstruction.I64LessThanEqualToUnsigned -> relOp<LongValue>(context) { x, y ->
            x.unsignedValue <= y.unsignedValue
        }
        NumericInstruction.I64GreaterThanEqualToSigned -> relOp<LongValue>(context) { x, y ->
            x.value >= y.value
        }
        NumericInstruction.I64GreaterThanEqualToUnsigned -> relOp<LongValue>(context) { x, y ->
            x.unsignedValue >= y.unsignedValue
        }
        NumericInstruction.F32AbsoluteValue -> unaryOp(context) { x: FloatValue ->
            x.value.absoluteValue.toValue()
        }
        NumericInstruction.F32Negative -> unaryOp(context) { x: FloatValue ->
            (-x.value).toValue()
        }
        NumericInstruction.F32Ceiling -> unaryOp(context) { x: FloatValue ->
            ceil(x.value).toValue()
        }
        NumericInstruction.F32Floor -> unaryOp(context) { x: FloatValue ->
            floor(x.value).toValue()
        }
        NumericInstruction.F32Truncate -> unaryOp(context) { x: FloatValue ->
            truncate(x.value).toValue()
        }
        NumericInstruction.F32Nearest -> unaryOp(context) { x: FloatValue ->
            round(x.value).toValue()
        }
        NumericInstruction.F32SquareRoot -> unaryOp(context) { x: FloatValue ->
            sqrt(x.value).toValue()
        }
        NumericInstruction.F32Add -> binaryOp<FloatValue>(context) { x, y ->
            (x.value + y.value).toValue()
        }
        NumericInstruction.F32Subtract -> binaryOp<FloatValue>(context) { x, y ->
            (x.value - y.value).toValue()
        }
        NumericInstruction.F32Multiply -> binaryOp<FloatValue>(context) { x, y ->
            (x.value * y.value).toValue()
        }
        NumericInstruction.F32Divide -> binaryOp<FloatValue>(context) { x, y ->
            (x.value / y.value).toValue()
        }
        NumericInstruction.F32Min -> binaryOp<FloatValue>(context) { x, y ->
            min(x.value, y.value).toValue()
        }
        NumericInstruction.F32Max -> binaryOp<FloatValue>(context) { x, y ->
            max(x.value, y.value).toValue()
        }
        NumericInstruction.F32CopySign -> binaryOp<FloatValue>(context) { x, y ->
            when {
                y.value <= -0f && x.value >= 0f -> (-x.value).toValue()
                y.value >= 0f && x.value <= -0f -> (-x.value).toValue()
                else -> x
            }
        }
        NumericInstruction.F32Equals -> relOp<FloatValue>(context) { x, y ->
            if (
                x.value.isNaN() ||
                y.value.isNaN() ||
                x.value == -Float.NaN ||
                y.value == -Float.NaN
            ) false
            else x == y
        }
        NumericInstruction.F32NotEquals -> relOp<FloatValue>(context) { x, y ->
            if (
                x.value.isNaN() ||
                y.value.isNaN() ||
                x.value == -Float.NaN ||
                y.value == -Float.NaN
            ) true
            else x != y
        }
        NumericInstruction.F32LessThan -> relOp<FloatValue>(context) { x, y ->
            if (
                x.value.isNaN() ||
                y.value.isNaN() ||
                x.value == -Float.NaN ||
                y.value == -Float.NaN
            ) false
            else if (x.value == Float.POSITIVE_INFINITY) false
            else if (x.value == Float.NEGATIVE_INFINITY) true
            else if (y.value == Float.POSITIVE_INFINITY) true
            else if (y.value == Float.NEGATIVE_INFINITY) false
            else x.value < y.value
        }
        NumericInstruction.F32GreaterThan -> relOp<FloatValue>(context) { x, y ->
            if (
                x.value.isNaN() ||
                y.value.isNaN() ||
                x.value == -Float.NaN ||
                y.value == -Float.NaN
            ) false
            else if (x.value == Float.POSITIVE_INFINITY) true
            else if (x.value == Float.NEGATIVE_INFINITY) false
            else if (y.value == Float.POSITIVE_INFINITY) false
            else if (y.value == Float.NEGATIVE_INFINITY) true
            else x.value > y.value
        }
        NumericInstruction.F32LessThanEqualTo -> relOp<FloatValue>(context) { x, y ->
            if (
                x.value.isNaN() ||
                y.value.isNaN() ||
                x.value == -Float.NaN ||
                y.value == -Float.NaN
            ) false
            else if (x.value == y.value) true
            else if (x.value == Float.POSITIVE_INFINITY) false
            else if (x.value == Float.NEGATIVE_INFINITY) true
            else if (y.value == Float.POSITIVE_INFINITY) true
            else if (y.value == Float.NEGATIVE_INFINITY) false
            else x.value <= y.value
        }
        NumericInstruction.F32GreaterThanEqualTo -> relOp<FloatValue>(context) { x, y ->
            if (
                x.value.isNaN() ||
                y.value.isNaN() ||
                x.value == -Float.NaN ||
                y.value == -Float.NaN
            ) false
            else if (x.value == y.value) true
            else if (x.value == Float.POSITIVE_INFINITY) true
            else if (x.value == Float.NEGATIVE_INFINITY) false
            else if (y.value == Float.POSITIVE_INFINITY) false
            else if (y.value == Float.NEGATIVE_INFINITY) true
            else x.value >= y.value
        }
        NumericInstruction.F64AbsoluteValue -> unaryOp(context) { x: DoubleValue ->
            x.value.absoluteValue.toValue()
        }
        NumericInstruction.F64Negative -> unaryOp(context) { x: DoubleValue ->
            (-x.value).toValue()
        }
        NumericInstruction.F64Ceiling -> unaryOp(context) { x: DoubleValue ->
            ceil(x.value).toValue()
        }
        NumericInstruction.F64Floor -> unaryOp(context) { x: DoubleValue ->
            floor(x.value).toValue()
        }
        NumericInstruction.F64Truncate -> unaryOp(context) { x: DoubleValue ->
            truncate(x.value).toValue()
        }
        NumericInstruction.F64Nearest -> unaryOp(context) { x: DoubleValue ->
            round(x.value).toValue()
        }
        NumericInstruction.F64SquareRoot -> unaryOp(context) { x: DoubleValue ->
            sqrt(x.value).toValue()
        }
        NumericInstruction.F64Add -> binaryOp<DoubleValue>(context) { x, y ->
            (x.value + y.value).toValue()
        }
        NumericInstruction.F64Subtract -> binaryOp<DoubleValue>(context) { x, y ->
            (x.value - y.value).toValue()
        }
        NumericInstruction.F64Multiply -> binaryOp<DoubleValue>(context) { x, y ->
            (x.value * y.value).toValue()
        }
        NumericInstruction.F64Divide -> binaryOp<DoubleValue>(context) { x, y ->
            (x.value / y.value).toValue()
        }
        NumericInstruction.F64Min -> binaryOp<DoubleValue>(context) { x, y ->
            min(x.value, y.value).toValue()
        }
        NumericInstruction.F64Max -> binaryOp<DoubleValue>(context) { x, y ->
            max(x.value, y.value).toValue()
        }
        NumericInstruction.F64CopySign -> binaryOp<DoubleValue>(context) { x, y ->
            when {
                y.value <= -0.0 && x.value >= 0.0 -> (-x.value).toValue()
                y.value >= 0.0 && x.value <= -0.0 -> (-x.value).toValue()
                else -> x
            }
        }
        NumericInstruction.F64Equals -> relOp<DoubleValue>(context) { x, y ->
            if (
                x.value.isNaN() ||
                y.value.isNaN() ||
                x.value == -Double.NaN ||
                y.value == -Double.NaN
            ) false
            else x == y
        }
        NumericInstruction.F64NotEquals -> relOp<DoubleValue>(context) { x, y ->
            if (
                x.value.isNaN() ||
                y.value.isNaN() ||
                x.value == -Double.NaN ||
                y.value == -Double.NaN
            ) true
            else x != y
        }
        NumericInstruction.F64LessThan -> relOp<DoubleValue>(context) { x, y ->
            if (
                x.value.isNaN() ||
                y.value.isNaN() ||
                x.value == -Double.NaN ||
                y.value == -Double.NaN
            ) false
            else if (x.value == Double.POSITIVE_INFINITY) false
            else if (x.value == Double.NEGATIVE_INFINITY) true
            else if (y.value == Double.POSITIVE_INFINITY) true
            else if (y.value == Double.NEGATIVE_INFINITY) false
            else x.value < y.value
        }
        NumericInstruction.F64GreaterThan -> relOp<DoubleValue>(context) { x, y ->
            if (
                x.value.isNaN() ||
                y.value.isNaN() ||
                x.value == -Double.NaN ||
                y.value == -Double.NaN
            ) false
            else if (x.value == Double.POSITIVE_INFINITY) true
            else if (x.value == Double.NEGATIVE_INFINITY) false
            else if (y.value == Double.POSITIVE_INFINITY) false
            else if (y.value == Double.NEGATIVE_INFINITY) true
            else x.value > y.value
        }
        NumericInstruction.F64LessThanEqualTo -> relOp<DoubleValue>(context) { x, y ->
            if (
                x.value.isNaN() ||
                y.value.isNaN() ||
                x.value == -Double.NaN ||
                y.value == -Double.NaN
            ) false
            else if (x.value == y.value) true
            else if (x.value == Double.POSITIVE_INFINITY) false
            else if (x.value == Double.NEGATIVE_INFINITY) true
            else if (y.value == Double.POSITIVE_INFINITY) true
            else if (y.value == Double.NEGATIVE_INFINITY) false
            else x.value <= y.value
        }
        NumericInstruction.F64GreaterThanEqualTo -> relOp<DoubleValue>(context) { x, y ->
            if (
                x.value.isNaN() ||
                y.value.isNaN() ||
                x.value == -Double.NaN ||
                y.value == -Double.NaN
            ) false
            else if (x.value == y.value) true
            else if (x.value == Double.POSITIVE_INFINITY) true
            else if (x.value == Double.NEGATIVE_INFINITY) false
            else if (y.value == Double.POSITIVE_INFINITY) false
            else if (y.value == Double.NEGATIVE_INFINITY) true
            else x.value >= y.value
        }
        NumericInstruction.I32WrapI64 -> unaryOp<LongValue, IntValue>(context) { x ->
            (x.unsignedValue % 0x100000000uL).toUInt().toValue()
        }
        NumericInstruction.I32TruncateF32Signed -> unaryOp<FloatValue, IntValue>(context) { x ->
            if (x.value.isNaN()) throw KWasmRuntimeException(EXCEPTION_TRUNCATE_NAN + " ${x.value.toRawBits().toString(2)}")
            if (x.value.isInfinite())
                throw KWasmRuntimeException(EXCEPTION_TRUNCATE_INF)
            val truncated = truncate(x.value)
            if (truncated.toLong() > Int.MAX_VALUE || truncated.toLong() < Int.MIN_VALUE) {
                throw KWasmRuntimeException(
                    "Cannot truncate, magnitude too large for i32 (integer overflow)"
                )
            }
            truncated.toInt().toValue()
        }
        NumericInstruction.I32TruncateSaturatedF32Signed -> unaryOp(context) { x: FloatValue ->
            return@unaryOp truncate(x.value).toInt().toValue()
        }
        NumericInstruction.I32TruncateF32Unsigned -> unaryOp<FloatValue, IntValue>(context) { x ->
            if (x.value.isNaN())
                throw KWasmRuntimeException(EXCEPTION_TRUNCATE_NAN)
            if (x.value.isInfinite())
                throw KWasmRuntimeException(EXCEPTION_TRUNCATE_INF)
            val truncated = truncate(x.value)
            if (truncated < 0) {
                throw KWasmRuntimeException(
                    "Cannot truncate negative f32 to unsigned i32 (integer overflow)"
                )
            }
            if (truncated.toLong() > UInt.MAX_VALUE.toLong()) {
                throw KWasmRuntimeException(
                    "Cannot truncate, magnitude too large for i32 (integer overflow)"
                )
            }
            truncated.toUInt().toValue()
        }
        NumericInstruction.I32TruncateSaturatedF32Unsigned -> unaryOp(context) { x: FloatValue ->
            return@unaryOp truncate(x.value).toUInt().toValue()
        }
        NumericInstruction.I32TruncateF64Signed -> unaryOp<DoubleValue, IntValue>(context) { x ->
            if (x.value.isNaN())
                throw KWasmRuntimeException(EXCEPTION_TRUNCATE_NAN)
            if (x.value.isInfinite())
                throw KWasmRuntimeException(EXCEPTION_TRUNCATE_INF)
            val truncated = truncate(x.value)
            if (truncated.toLong() > Int.MAX_VALUE || truncated.toLong() < Int.MIN_VALUE) {
                throw KWasmRuntimeException(
                    "Cannot truncate, magnitude too large for i32 (integer overflow)"
                )
            }
            truncated.toInt().toValue()
        }
        NumericInstruction.I32TruncateSaturatedF64Signed -> unaryOp(context) { x: DoubleValue ->
            return@unaryOp truncate(x.value).toInt().toValue()
        }
        NumericInstruction.I32TruncateF64Unsigned -> unaryOp<DoubleValue, IntValue>(context) { x ->
            if (x.value.isNaN())
                throw KWasmRuntimeException(EXCEPTION_TRUNCATE_NAN)
            if (x.value.isInfinite())
                throw KWasmRuntimeException(EXCEPTION_TRUNCATE_INF)
            val truncated = truncate(x.value)
            if (truncated < 0) {
                throw KWasmRuntimeException(
                    "Cannot truncate negative f64 to unsigned i32 (integer overflow)"
                )
            }
            if (truncated.toLong() > UInt.MAX_VALUE.toLong()) {
                throw KWasmRuntimeException(
                    "Cannot truncate, magnitude too large for i32 (integer overflow)"
                )
            }
            truncated.toUInt().toValue()
        }
        NumericInstruction.I32TruncateSaturatedF64Unsigned -> unaryOp(context) { x: DoubleValue ->
            return@unaryOp truncate(x.value).toUInt().toValue()
        }
        NumericInstruction.I32ReinterpretF32 -> unaryOp(context) { x: FloatValue ->
            x.value.toRawBits().toValue()
        }
        NumericInstruction.I64ExtendI32Signed -> unaryOp(context) { x: IntValue ->
            x.value.toLong().toValue()
        }
        NumericInstruction.I64ExtendI32Unsigned -> unaryOp(context) { x: IntValue ->
            x.unsignedValue.toULong().toValue()
        }
        NumericInstruction.I64TruncateF32Signed -> unaryOp<FloatValue, LongValue>(context) { x ->
            if (x.value.isNaN())
                throw KWasmRuntimeException(EXCEPTION_TRUNCATE_NAN)
            if (x.value.isInfinite())
                throw KWasmRuntimeException(EXCEPTION_TRUNCATE_INF)
            val truncated = truncate(x.value)
            if (truncated.toULong() > Long.MAX_VALUE.toULong() || truncated < Long.MIN_VALUE) {
                throw KWasmRuntimeException(
                    "Cannot truncate, magnitude too large for i64 (integer overflow)"
                )
            }
            truncated.toLong().toValue()
        }
        NumericInstruction.I64TruncateSaturatedF32Signed -> unaryOp(context) { x: FloatValue ->
            return@unaryOp truncate(x.value).toLong().toValue()
        }
        NumericInstruction.I64TruncateF32Unsigned -> unaryOp<FloatValue, LongValue>(context) { x ->
            if (x.value.isNaN())
                throw KWasmRuntimeException(EXCEPTION_TRUNCATE_NAN)
            if (x.value.isInfinite())
                throw KWasmRuntimeException(EXCEPTION_TRUNCATE_INF)
            val truncated = truncate(x.value.toDouble())
            if (truncated < 0) {
                throw KWasmRuntimeException(
                    "Cannot truncate negative f32 to unsigned i64 (integer overflow)"
                )
            }
            if (truncated >= ULong.MAX_VALUE.toDouble()) {
                throw KWasmRuntimeException(
                    "Cannot truncate, magnitude too large for i64 (integer overflow)"
                )
            }
            truncated.toULong().toValue()
        }
        NumericInstruction.I64TruncateSaturatedF32Unsigned -> unaryOp(context) { x: FloatValue ->
            return@unaryOp truncate(x.value).toULong().toValue()
        }
        NumericInstruction.I64TruncateF64Signed -> unaryOp<DoubleValue, LongValue>(context) { x ->
            if (x.value.isNaN())
                throw KWasmRuntimeException(EXCEPTION_TRUNCATE_NAN)
            if (x.value.isInfinite())
                throw KWasmRuntimeException(EXCEPTION_TRUNCATE_INF)
            val truncated = truncate(x.value)
            if (truncated.toULong() > Long.MAX_VALUE.toULong() || truncated < Long.MIN_VALUE) {
                throw KWasmRuntimeException(
                    "Cannot truncate, magnitude too large for i64 (integer overflow)"
                )
            }
            truncated.toLong().toValue()
        }
        NumericInstruction.I64TruncateSaturatedF64Signed -> unaryOp(context) { x: DoubleValue ->
            return@unaryOp truncate(x.value).toLong().toValue()
        }
        NumericInstruction.I64TruncateF64Unsigned -> unaryOp<DoubleValue, LongValue>(context) { x ->
            if (x.value.isNaN())
                throw KWasmRuntimeException(EXCEPTION_TRUNCATE_NAN)
            if (x.value.isInfinite())
                throw KWasmRuntimeException(EXCEPTION_TRUNCATE_INF)
            val truncated = truncate(x.value)
            if (truncated < 0) {
                throw KWasmRuntimeException(
                    "Cannot truncate negative f64 to unsigned i64 (integer overflow)"
                )
            }
            if (truncated >= ULong.MAX_VALUE.toDouble()) {
                throw KWasmRuntimeException(
                    "Cannot truncate, magnitude too large for i64 (integer overflow)"
                )
            }
            truncated.toULong().toValue()
        }
        NumericInstruction.I64TruncateSaturatedF64Unsigned -> unaryOp(context) { x: DoubleValue ->
            return@unaryOp truncate(x.value).toULong().toValue()
        }
        NumericInstruction.I64ReinterpretF64 -> unaryOp(context) { x: DoubleValue ->
            (x.value).toRawBits().toValue()
        }
        NumericInstruction.F32ConvertI32Signed -> unaryOp(context) { x: IntValue ->
            x.value.toFloat().toValue()
        }
        NumericInstruction.F32ConvertI32Unsigned -> unaryOp(context) { x: IntValue ->
            x.unsignedValue.toFloat().toValue()
        }
        NumericInstruction.F32ConvertI64Signed -> unaryOp(context) { x: LongValue ->
            // See https://github.com/WebAssembly/spec/pull/1021
            if (x.value.absoluteValue < 0x10_0000_0000_0000L) {
                x.value.toFloat().toValue()
            } else {
                val r = if (x.value and 0xfffL == 0L) 0L else 1L
                val result = (x.value shr 12) or r
                (result.toFloat() * 2.0f.pow(12)).toValue()
            }
        }
        NumericInstruction.F32ConvertI64Unsigned -> unaryOp(context) { x: LongValue ->
            // See https://github.com/WebAssembly/spec/pull/1021
            if (x.unsignedValue < 0x10_0000_0000_0000uL) {
                x.unsignedValue.toFloat().toValue()
            } else {
                val r = if (x.unsignedValue and 0xfffuL == 0uL) 0uL else 1uL
                val result = (x.unsignedValue shr 12) or r
                (result.toFloat() * 2.0f.pow(12)).toValue()
            }
        }
        NumericInstruction.F32DemoteF64 -> unaryOp(context) { x: DoubleValue ->
            x.value.toFloat().toValue()
        }
        NumericInstruction.F32ReinterpretI32 -> unaryOp(context) { x: IntValue ->
            Float.fromBits(x.value).toValue()
        }
        NumericInstruction.F64ConvertI32Signed -> unaryOp(context) { x: IntValue ->
            x.value.toDouble().toValue()
        }
        NumericInstruction.F64ConvertI32Unsigned -> unaryOp(context) { x: IntValue ->
            x.unsignedValue.toDouble().toValue()
        }
        NumericInstruction.F64ConvertI64Signed -> unaryOp(context) { x: LongValue ->
            x.value.toDouble().toValue()
        }
        NumericInstruction.F64ConvertI64Unsigned -> unaryOp(context) { x: LongValue ->
            x.unsignedValue.toDouble().toValue()
        }
        NumericInstruction.F64PromoteF32 -> unaryOp(context) { x: FloatValue ->
            x.value.toDouble().toValue()
        }
        NumericInstruction.F64ReinterpretI64 -> unaryOp(context) { x: LongValue ->
            Double.fromBits(x.value).toValue()
        }
        NumericInstruction.I32Extend8Signed -> unaryOp(context) { x: IntValue ->
            x.value.toByte().toInt().toValue()
        }
        NumericInstruction.I32Extend16Signed -> unaryOp(context) { x: IntValue ->
            x.value.toShort().toInt().toValue()
        }
        NumericInstruction.I64Extend8Signed -> unaryOp(context) { x: LongValue ->
            x.value.toByte().toLong().toValue()
        }
        NumericInstruction.I64Extend16Signed -> unaryOp(context) { x: LongValue ->
            x.value.toShort().toLong().toValue()
        }
        NumericInstruction.I64Extend32Signed -> unaryOp(context) { x: LongValue ->
            x.value.toInt().toLong().toValue()
        }
    }
    context.instructionIndex++
    return context
}

/**
 * From [the docs](https://webassembly.github.io/spec/core/exec/instructions.html#exec-unop):
 *
 * ```
 *   t.unop
 * ```
 *
 * 1. Assert: due to validation, a value of value type `t` is on the top of the stack.
 * 1. Pop the value `t.const c_1` from the stack.
 * 1. If `unopt(c_1)` is defined, then:
 *    * Let `c` be a possible result of computing `unopt(c_1)`.
 *    * Push the value `t.const c` to the stack.
 * 1. Else:
 *    * Trap.
 *
 * Also functions as the conversion operator util.
 *
 * From [the docs](https://webassembly.github.io/spec/core/exec/instructions.html#exec-cvtop):
 *
 * ```
 *   t2.cvtop_t1_sx?
 * ```
 *
 * 1. Assert: due to validation, a value of value type `t1` is on the top of the stack.
 * 1. Pop the value `t1.const c1` from the stack.
 * 1. If `cvtop^sx?_t1,_t2(c1)` is defined:
 *    * Let `c_2` be a possible result of computing `cvtop^sx?_t1,_t2(c_1)`.
 *    * Push the value `t_2.const c_2` to the stack.
 * 1. Else:
 *    * Trap.
 */
internal inline fun <reified In : Value<*>, reified Out : Value<*>> unaryOp(
    executionContext: ExecutionContext,
    crossinline op: (In) -> Out
): ExecutionContext {
    val stackTop = executionContext.stacks.operands.pop()
    if (stackTop !is In) throw KWasmRuntimeException("Top of stack is invalid type")

    val newTop = op(stackTop)
    executionContext.stacks.operands.push(newTop)

    return executionContext
}

/**
 * From [the docs](https://webassembly.github.io/spec/core/exec/instructions.html#exec-binop):
 *
 * ```
 *   t.binop
 * ```
 *
 * 1. Assert: due to validation, two values of value type `t` are on the top of the stack.
 * 1. Pop the value `t.const c_2` from the stack.
 * 1. Pop the value `t.const c_1` from the stack.
 * 1. If `binopt(c_1, c_2)` is defined, then:
 *    * Let `c` be a possible result of computing `binopt(c_1, c_2)`.
 *    * Push the value `t.const c` to the stack.
 * 1. Else:
 *    * Trap.
 */
internal inline fun <reified Type : Value<*>> binaryOp(
    executionContext: ExecutionContext,
    crossinline op: (Type, Type) -> Type
): ExecutionContext {
    val arg2 = executionContext.stacks.operands.pop()
    if (arg2 !is Type) throw KWasmRuntimeException("RHS is invalid type")

    val arg1 = executionContext.stacks.operands.pop()
    if (arg1 !is Type) throw KWasmRuntimeException("LHS is invalid type")

    val result = op(arg1, arg2)
    executionContext.stacks.operands.push(result)

    return executionContext
}

/**
 * From [the docs](https://webassembly.github.io/spec/core/exec/instructions.html#exec-testop):
 *
 * ```
 *   t.testop
 * ```
 *
 * 1. Assert: due to validation, a value of value type `t` is on the top of the stack.
 * 1. Pop the value `t.const c_1` from the stack.
 * 1. Let `c` be the result of computing `testopt(c_1)`.
 * 1. Push the value `i32.const c` to the stack.
 */
internal inline fun <reified In : Value<*>> testOp(
    executionContext: ExecutionContext,
    crossinline op: (In) -> Boolean
): ExecutionContext {
    val stackTop = executionContext.stacks.operands.pop()
    if (stackTop !is In) throw KWasmRuntimeException("Top of stack is invalid type")

    val testResult = op(stackTop)
    executionContext.stacks.operands.push(
        if (testResult) 1.toValue() else 0.toValue()
    )

    return executionContext
}

/**
 * From [the docs](https://webassembly.github.io/spec/core/exec/instructions.html#exec-relop):
 *
 * ```
 *   t.relop
 * ```
 *
 * 1. Assert: due to validation, two values of value type `t` are on the top of the stack.
 * 1. Pop the value `t.const c_2` from the stack.
 * 1. Pop the value `t.const c_1` from the stack.
 * 1. Let `c` be the result of computing `relopt(c_1, c_2)`.
 * 1. Push the value `i32.const c` to the stack.
 */
internal inline fun <reified In : Value<*>> relOp(
    executionContext: ExecutionContext,
    crossinline op: (In, In) -> Boolean
): ExecutionContext {
    val arg2 = executionContext.stacks.operands.pop()
    if (arg2 !is In) throw KWasmRuntimeException("RHS is invalid type")

    val arg1 = executionContext.stacks.operands.pop()
    if (arg1 !is In) throw KWasmRuntimeException("LHS is invalid type")

    val result = op(arg1, arg2)
    executionContext.stacks.operands.push(
        if (result) 1.toValue() else 0.toValue()
    )

    return executionContext
}
