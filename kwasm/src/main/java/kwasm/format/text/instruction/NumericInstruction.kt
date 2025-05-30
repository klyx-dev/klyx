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

package kwasm.format.text.instruction

import kwasm.ast.instruction.NumericInstruction
import kwasm.format.text.ParseResult
import kwasm.format.text.token.Keyword
import kwasm.format.text.token.Token

/**
 * Parses a [NumericInstruction] from the receiving [List] of [Token]s.
 *
 * See
 * [the docs](https://webassembly.github.io/spec/core/text/instructions.html#numeric-instructions)
 * for details.
 */
fun List<Token>.parseNumericInstruction(fromIndex: Int): ParseResult<NumericInstruction>? {
    val keyword = getOrNull(fromIndex) as? Keyword ?: return null
    return NUMERIC_LOOKUP[keyword.value]?.let {
        ParseResult(
            it,
            1
        )
    }
}

private val NUMERIC_LOOKUP = mapOf<String, NumericInstruction>(
    "i32.clz" to NumericInstruction.I32CountLeadingZeroes,
    "i32.ctz" to NumericInstruction.I32CountTrailingZeroes,
    "i32.popcnt" to NumericInstruction.I32CountNonZeroBits,
    "i32.add" to NumericInstruction.I32Add,
    "i32.sub" to NumericInstruction.I32Subtract,
    "i32.mul" to NumericInstruction.I32Multiply,
    "i32.div_s" to NumericInstruction.I32DivideSigned,
    "i32.div_u" to NumericInstruction.I32DivideUnsigned,
    "i32.rem_s" to NumericInstruction.I32RemainderSigned,
    "i32.rem_u" to NumericInstruction.I32RemainderUnsigned,
    "i32.and" to NumericInstruction.I32BitwiseAnd,
    "i32.or" to NumericInstruction.I32BitwiseOr,
    "i32.xor" to NumericInstruction.I32BitwiseXor,
    "i32.shl" to NumericInstruction.I32ShiftLeft,
    "i32.shr_s" to NumericInstruction.I32ShiftRightSigned,
    "i32.shr_u" to NumericInstruction.I32ShiftRightUnsigned,
    "i32.rotl" to NumericInstruction.I32RotateLeft,
    "i32.rotr" to NumericInstruction.I32RotateRight,
    "i32.eqz" to NumericInstruction.I32EqualsZero,
    "i32.eq" to NumericInstruction.I32Equals,
    "i32.ne" to NumericInstruction.I32NotEquals,
    "i32.lt_s" to NumericInstruction.I32LessThanSigned,
    "i32.lt_u" to NumericInstruction.I32LessThanUnsigned,
    "i32.gt_s" to NumericInstruction.I32GreaterThanSigned,
    "i32.gt_u" to NumericInstruction.I32GreaterThanUnsigned,
    "i32.le_s" to NumericInstruction.I32LessThanEqualToSigned,
    "i32.le_u" to NumericInstruction.I32LessThanEqualToUnsigned,
    "i32.ge_s" to NumericInstruction.I32GreaterThanEqualToSigned,
    "i32.ge_u" to NumericInstruction.I32GreaterThanEqualToUnsigned,

    "i64.clz" to NumericInstruction.I64CountLeadingZeroes,
    "i64.ctz" to NumericInstruction.I64CountTrailingZeroes,
    "i64.popcnt" to NumericInstruction.I64CountNonZeroBits,
    "i64.add" to NumericInstruction.I64Add,
    "i64.sub" to NumericInstruction.I64Subtract,
    "i64.mul" to NumericInstruction.I64Multiply,
    "i64.div_s" to NumericInstruction.I64DivideSigned,
    "i64.div_u" to NumericInstruction.I64DivideUnsigned,
    "i64.rem_s" to NumericInstruction.I64RemainderSigned,
    "i64.rem_u" to NumericInstruction.I64RemainderUnsigned,
    "i64.and" to NumericInstruction.I64BitwiseAnd,
    "i64.or" to NumericInstruction.I64BitwiseOr,
    "i64.xor" to NumericInstruction.I64BitwiseXor,
    "i64.shl" to NumericInstruction.I64ShiftLeft,
    "i64.shr_s" to NumericInstruction.I64ShiftRightSigned,
    "i64.shr_u" to NumericInstruction.I64ShiftRightUnsigned,
    "i64.rotl" to NumericInstruction.I64RotateLeft,
    "i64.rotr" to NumericInstruction.I64RotateRight,
    "i64.eqz" to NumericInstruction.I64EqualsZero,
    "i64.eq" to NumericInstruction.I64Equals,
    "i64.ne" to NumericInstruction.I64NotEquals,
    "i64.lt_s" to NumericInstruction.I64LessThanSigned,
    "i64.lt_u" to NumericInstruction.I64LessThanUnsigned,
    "i64.gt_s" to NumericInstruction.I64GreaterThanSigned,
    "i64.gt_u" to NumericInstruction.I64GreaterThanUnsigned,
    "i64.le_s" to NumericInstruction.I64LessThanEqualToSigned,
    "i64.le_u" to NumericInstruction.I64LessThanEqualToUnsigned,
    "i64.ge_s" to NumericInstruction.I64GreaterThanEqualToSigned,
    "i64.ge_u" to NumericInstruction.I64GreaterThanEqualToUnsigned,

    "f32.abs" to NumericInstruction.F32AbsoluteValue,
    "f32.neg" to NumericInstruction.F32Negative,
    "f32.ceil" to NumericInstruction.F32Ceiling,
    "f32.floor" to NumericInstruction.F32Floor,
    "f32.trunc" to NumericInstruction.F32Truncate,
    "f32.nearest" to NumericInstruction.F32Nearest,
    "f32.sqrt" to NumericInstruction.F32SquareRoot,
    "f32.add" to NumericInstruction.F32Add,
    "f32.sub" to NumericInstruction.F32Subtract,
    "f32.mul" to NumericInstruction.F32Multiply,
    "f32.div" to NumericInstruction.F32Divide,
    "f32.min" to NumericInstruction.F32Min,
    "f32.max" to NumericInstruction.F32Max,
    "f32.copysign" to NumericInstruction.F32CopySign,
    "f32.eq" to NumericInstruction.F32Equals,
    "f32.ne" to NumericInstruction.F32NotEquals,
    "f32.lt" to NumericInstruction.F32LessThan,
    "f32.gt" to NumericInstruction.F32GreaterThan,
    "f32.le" to NumericInstruction.F32LessThanEqualTo,
    "f32.ge" to NumericInstruction.F32GreaterThanEqualTo,

    "f64.abs" to NumericInstruction.F64AbsoluteValue,
    "f64.neg" to NumericInstruction.F64Negative,
    "f64.ceil" to NumericInstruction.F64Ceiling,
    "f64.floor" to NumericInstruction.F64Floor,
    "f64.trunc" to NumericInstruction.F64Truncate,
    "f64.nearest" to NumericInstruction.F64Nearest,
    "f64.sqrt" to NumericInstruction.F64SquareRoot,
    "f64.add" to NumericInstruction.F64Add,
    "f64.sub" to NumericInstruction.F64Subtract,
    "f64.mul" to NumericInstruction.F64Multiply,
    "f64.div" to NumericInstruction.F64Divide,
    "f64.min" to NumericInstruction.F64Min,
    "f64.max" to NumericInstruction.F64Max,
    "f64.copysign" to NumericInstruction.F64CopySign,
    "f64.eq" to NumericInstruction.F64Equals,
    "f64.ne" to NumericInstruction.F64NotEquals,
    "f64.lt" to NumericInstruction.F64LessThan,
    "f64.gt" to NumericInstruction.F64GreaterThan,
    "f64.le" to NumericInstruction.F64LessThanEqualTo,
    "f64.ge" to NumericInstruction.F64GreaterThanEqualTo,

    "i32.wrap_i64" to NumericInstruction.I32WrapI64,
    "i32.trunc_f32_s" to NumericInstruction.I32TruncateF32Signed,
    "i32.trunc_f32_u" to NumericInstruction.I32TruncateF32Unsigned,
    "i32.trunc_f64_s" to NumericInstruction.I32TruncateF64Signed,
    "i32.trunc_f64_u" to NumericInstruction.I32TruncateF64Unsigned,
    "i32.reinterpret_f32" to NumericInstruction.I32ReinterpretF32,

    "i64.extend_i32_s" to NumericInstruction.I64ExtendI32Signed,
    "i64.extend_i32_u" to NumericInstruction.I64ExtendI32Unsigned,
    "i64.trunc_f32_s" to NumericInstruction.I64TruncateF32Signed,
    "i64.trunc_f32_u" to NumericInstruction.I64TruncateF32Unsigned,
    "i64.trunc_f64_s" to NumericInstruction.I64TruncateF64Signed,
    "i64.trunc_f64_u" to NumericInstruction.I64TruncateF64Unsigned,
    "i64.reinterpret_f64" to NumericInstruction.I64ReinterpretF64,

    "f32.convert_i32_s" to NumericInstruction.F32ConvertI32Signed,
    "f32.convert_i32_u" to NumericInstruction.F32ConvertI32Unsigned,
    "f32.convert_i64_s" to NumericInstruction.F32ConvertI64Signed,
    "f32.convert_i64_u" to NumericInstruction.F32ConvertI64Unsigned,
    "f32.demote_f64" to NumericInstruction.F32DemoteF64,
    "f32.reinterpret_i32" to NumericInstruction.F32ReinterpretI32,

    "f64.convert_i32_s" to NumericInstruction.F64ConvertI32Signed,
    "f64.convert_i32_u" to NumericInstruction.F64ConvertI32Unsigned,
    "f64.convert_i64_s" to NumericInstruction.F64ConvertI64Signed,
    "f64.convert_i64_u" to NumericInstruction.F64ConvertI64Unsigned,
    "f64.promote_f32" to NumericInstruction.F64PromoteF32,
    "f64.reinterpret_i64" to NumericInstruction.F64ReinterpretI64,

    "i32.extend8_s" to NumericInstruction.I32Extend8Signed,
    "i32.extend16_s" to NumericInstruction.I32Extend16Signed,
    "i64.extend8_s" to NumericInstruction.I64Extend8Signed,
    "i64.extend16_s" to NumericInstruction.I64Extend16Signed,
    "i64.extend32_s" to NumericInstruction.I64Extend32Signed,

    "i32.trunc_sat_f32_s" to NumericInstruction.I32TruncateSaturatedF32Signed,
    "i32.trunc_sat_f32_u" to NumericInstruction.I32TruncateSaturatedF32Unsigned,
    "i32.trunc_sat_f64_s" to NumericInstruction.I32TruncateSaturatedF64Signed,
    "i32.trunc_sat_f64_u" to NumericInstruction.I32TruncateSaturatedF64Unsigned,
    "i64.trunc_sat_f32_s" to NumericInstruction.I64TruncateSaturatedF32Signed,
    "i64.trunc_sat_f32_u" to NumericInstruction.I64TruncateSaturatedF32Unsigned,
    "i64.trunc_sat_f64_s" to NumericInstruction.I64TruncateSaturatedF64Signed,
    "i64.trunc_sat_f64_u" to NumericInstruction.I64TruncateSaturatedF64Unsigned,
)
