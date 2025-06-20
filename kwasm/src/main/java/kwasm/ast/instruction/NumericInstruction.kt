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

package kwasm.ast.instruction

/**
 * Defines the various numeric [Instruction] variants.
 *
 * See more in [the docs](https://webassembly.github.io/spec/core/exec/numerics.html).
 */
sealed class NumericInstruction : Instruction {
    /** i32.clz */
    data object I32CountLeadingZeroes : NumericInstruction()
    /** i32.ctz */
    data object I32CountTrailingZeroes : NumericInstruction()
    /** i32.popcnt */
    data object I32CountNonZeroBits : NumericInstruction()
    /** i32.add */
    data object I32Add : NumericInstruction()
    /** i32.sub */
    data object I32Subtract : NumericInstruction()
    /** i32.mul */
    data object I32Multiply : NumericInstruction()
    /** i32.div_s */
    data object I32DivideSigned : NumericInstruction()
    /** i32.div_u */
    data object I32DivideUnsigned : NumericInstruction()
    /** i32.rem_s */
    data object I32RemainderSigned : NumericInstruction()
    /** i32.rem_u */
    data object I32RemainderUnsigned : NumericInstruction()
    /** i32.and */
    data object I32BitwiseAnd : NumericInstruction()
    /** i32.or */
    data object I32BitwiseOr : NumericInstruction()
    /** i32.xor */
    data object I32BitwiseXor : NumericInstruction()
    /** i32.shl */
    data object I32ShiftLeft : NumericInstruction()
    /** i32.shr_s */
    data object I32ShiftRightSigned : NumericInstruction()
    /** i32.shr_u */
    data object I32ShiftRightUnsigned : NumericInstruction()
    /** i32.rotl */
    data object I32RotateLeft : NumericInstruction()
    /** i32.rotr */
    data object I32RotateRight : NumericInstruction()
    /** i32.eqz */
    data object I32EqualsZero : NumericInstruction()
    /** i32.eq */
    data object I32Equals : NumericInstruction()
    /** i32.ne */
    data object I32NotEquals : NumericInstruction()
    /** i32.lt_s */
    data object I32LessThanSigned : NumericInstruction()
    /** i32.lt_u */
    data object I32LessThanUnsigned : NumericInstruction()
    /** i32.gt_s */
    data object I32GreaterThanSigned : NumericInstruction()
    /** i32.gt_u */
    data object I32GreaterThanUnsigned : NumericInstruction()
    /** i32.le_s */
    data object I32LessThanEqualToSigned : NumericInstruction()
    /** i32.le_u */
    data object I32LessThanEqualToUnsigned : NumericInstruction()
    /** i32.ge_s */
    data object I32GreaterThanEqualToSigned : NumericInstruction()
    /** i32.ge_u */
    data object I32GreaterThanEqualToUnsigned : NumericInstruction()

    /** i64.clz */
    data object I64CountLeadingZeroes : NumericInstruction()
    /** i64.ctz */
    data object I64CountTrailingZeroes : NumericInstruction()
    /** i64.popcnt */
    data object I64CountNonZeroBits : NumericInstruction()
    /** i64.add */
    data object I64Add : NumericInstruction()
    /** i64.sub */
    data object I64Subtract : NumericInstruction()
    /** i64.mul */
    data object I64Multiply : NumericInstruction()
    /** i64.div_s */
    data object I64DivideSigned : NumericInstruction()
    /** i64.div_u */
    data object I64DivideUnsigned : NumericInstruction()
    /** i64.rem_s */
    data object I64RemainderSigned : NumericInstruction()
    /** i64.rem_u */
    data object I64RemainderUnsigned : NumericInstruction()
    /** i64.and */
    data object I64BitwiseAnd : NumericInstruction()
    /** i64.or */
    data object I64BitwiseOr : NumericInstruction()
    /** i64.xor */
    data object I64BitwiseXor : NumericInstruction()
    /** i64.shl */
    data object I64ShiftLeft : NumericInstruction()
    /** i64.shr_s */
    data object I64ShiftRightSigned : NumericInstruction()
    /** i64.shr_u */
    data object I64ShiftRightUnsigned : NumericInstruction()
    /** i64.rotl */
    data object I64RotateLeft : NumericInstruction()
    /** i64.rotr */
    data object I64RotateRight : NumericInstruction()
    /** i64.eqz */
    data object I64EqualsZero : NumericInstruction()
    /** i64.eq */
    data object I64Equals : NumericInstruction()
    /** i64.eq */
    data object I64NotEquals : NumericInstruction()
    /** i64.lt_s */
    data object I64LessThanSigned : NumericInstruction()
    /** i64.lt_u */
    data object I64LessThanUnsigned : NumericInstruction()
    /** i64.gt_s */
    data object I64GreaterThanSigned : NumericInstruction()
    /** i64.gt_u */
    data object I64GreaterThanUnsigned : NumericInstruction()
    /** i64.le_s */
    data object I64LessThanEqualToSigned : NumericInstruction()
    /** i64.le_u */
    data object I64LessThanEqualToUnsigned : NumericInstruction()
    /** i64.ge_s */
    data object I64GreaterThanEqualToSigned : NumericInstruction()
    /** i64.ge_u */
    data object I64GreaterThanEqualToUnsigned : NumericInstruction()

    /** f32.abs */
    data object F32AbsoluteValue : NumericInstruction()
    /** f32.neg */
    data object F32Negative : NumericInstruction()
    /** f32.ceil */
    data object F32Ceiling : NumericInstruction()
    /** f32.floor */
    data object F32Floor : NumericInstruction()
    /** f32.trunc */
    data object F32Truncate : NumericInstruction()
    /** f32.nearest */
    data object F32Nearest : NumericInstruction()
    /** f32.sqrt */
    data object F32SquareRoot : NumericInstruction()
    /** f32.add */
    data object F32Add : NumericInstruction()
    /** f32.sub */
    data object F32Subtract : NumericInstruction()
    /** f32.mul */
    data object F32Multiply : NumericInstruction()
    /** f32.div */
    data object F32Divide : NumericInstruction()
    /** f32.min */
    data object F32Min : NumericInstruction()
    /** f32.max */
    data object F32Max : NumericInstruction()
    /** f32.copysign */
    data object F32CopySign : NumericInstruction()
    /** f32.eq */
    data object F32Equals : NumericInstruction()
    /** f32.ne */
    data object F32NotEquals : NumericInstruction()
    /** f32.lt */
    data object F32LessThan : NumericInstruction()
    /** f32.gt */
    data object F32GreaterThan : NumericInstruction()
    /** f32.le */
    data object F32LessThanEqualTo : NumericInstruction()
    /** f32.ge */
    data object F32GreaterThanEqualTo : NumericInstruction()

    /** f64.abs */
    data object F64AbsoluteValue : NumericInstruction()
    /** f64.neg */
    data object F64Negative : NumericInstruction()
    /** f64.ceil */
    data object F64Ceiling : NumericInstruction()
    /** f64.floor */
    data object F64Floor : NumericInstruction()
    /** f64.trunc */
    data object F64Truncate : NumericInstruction()
    /** f64.nearest */
    data object F64Nearest : NumericInstruction()
    /** f64.sqrt */
    data object F64SquareRoot : NumericInstruction()
    /** f64.add */
    data object F64Add : NumericInstruction()
    /** f64.sub */
    data object F64Subtract : NumericInstruction()
    /** f64.mul */
    data object F64Multiply : NumericInstruction()
    /** f64.div */
    data object F64Divide : NumericInstruction()
    /** f64.min */
    data object F64Min : NumericInstruction()
    /** f64.max */
    data object F64Max : NumericInstruction()
    /** f64.copysign */
    data object F64CopySign : NumericInstruction()
    /** f64.eq */
    data object F64Equals : NumericInstruction()
    /** f64.ne */
    data object F64NotEquals : NumericInstruction()
    /** f64.lt */
    data object F64LessThan : NumericInstruction()
    /** f64.gt */
    data object F64GreaterThan : NumericInstruction()
    /** f64.le */
    data object F64LessThanEqualTo : NumericInstruction()
    /** f64.ge */
    data object F64GreaterThanEqualTo : NumericInstruction()

    /*
     * Conversions
     */

    /** i32.wrap_i64 */
    data object I32WrapI64 : NumericInstruction()
    /** i32.trunc_f32_s */
    data object I32TruncateF32Signed : NumericInstruction()
    /** i32.trunc_f32_u */
    data object I32TruncateF32Unsigned : NumericInstruction()
    /** i32.trunc_f64_s */
    data object I32TruncateF64Signed : NumericInstruction()
    /** i32.trunc_f64_u */
    data object I32TruncateF64Unsigned : NumericInstruction()
    /** i32.reinterpret_f32 */
    data object I32ReinterpretF32 : NumericInstruction()

    /** i64.extend_i32_s */
    data object I64ExtendI32Signed : NumericInstruction()
    /** i64.extend_i32_u */
    data object I64ExtendI32Unsigned : NumericInstruction()
    /** i64.trunc_f32_s */
    data object I64TruncateF32Signed : NumericInstruction()
    /** i64.trunc_f32_u */
    data object I64TruncateF32Unsigned : NumericInstruction()
    /** i64.trunc_f64_s */
    data object I64TruncateF64Signed : NumericInstruction()
    /** i64.trunc_f64_u */
    data object I64TruncateF64Unsigned : NumericInstruction()
    /** i64.reinterpret_f64 */
    data object I64ReinterpretF64 : NumericInstruction()

    /** f32.convert_i32_s */
    data object F32ConvertI32Signed : NumericInstruction()
    /** f32.convert_i32_u */
    data object F32ConvertI32Unsigned : NumericInstruction()
    /** f32.convert_i64_s */
    data object F32ConvertI64Signed : NumericInstruction()
    /** f32.convert_i64_u */
    data object F32ConvertI64Unsigned : NumericInstruction()
    /** f32.demote_f64 */
    data object F32DemoteF64 : NumericInstruction()
    /** f32.reinterpret_i32 */
    data object F32ReinterpretI32 : NumericInstruction()

    /** f64.convert_i32_s */
    data object F64ConvertI32Signed : NumericInstruction()
    /** f64.convert_i32_u */
    data object F64ConvertI32Unsigned : NumericInstruction()
    /** f64.convert_i64_s */
    data object F64ConvertI64Signed : NumericInstruction()
    /** f64.convert_i64_u */
    data object F64ConvertI64Unsigned : NumericInstruction()
    /** f64.promote_f32 */
    data object F64PromoteF32 : NumericInstruction()
    /** f64.reinterpret_i64 */
    data object F64ReinterpretI64 : NumericInstruction()

    /** i32.extend8_s */
    data object I32Extend8Signed : NumericInstruction()
    /** i32.extend16_s */
    data object I32Extend16Signed : NumericInstruction()
    /** i64.extend8_s */
    data object I64Extend8Signed : NumericInstruction()
    /** i64.extend16_s */
    data object I64Extend16Signed : NumericInstruction()
    /** i64.extend32_s */
    data object I64Extend32Signed : NumericInstruction()

    /** i32.trunc_sat_f32_s */
    data object I32TruncateSaturatedF32Signed : NumericInstruction()
    /** i32.trunc_sat_f32_u */
    data object I32TruncateSaturatedF32Unsigned : NumericInstruction()
    /** i32.trunc_sat_f64_s */
    data object I32TruncateSaturatedF64Signed : NumericInstruction()
    /** i32.trunc_sat_f64_u */
    data object I32TruncateSaturatedF64Unsigned : NumericInstruction()
    /** i64.trunc_sat_f32_s */
    data object I64TruncateSaturatedF32Signed : NumericInstruction()
    /** i64.trunc_sat_f32_u */
    data object I64TruncateSaturatedF32Unsigned : NumericInstruction()
    /** i64.trunc_sat_f64_s */
    data object I64TruncateSaturatedF64Signed : NumericInstruction()
    /** i64.trunc_sat_f64_u */
    data object I64TruncateSaturatedF64Unsigned : NumericInstruction()
}
