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
    object I32CountLeadingZeroes : NumericInstruction()
    /** i32.ctz */
    object I32CountTrailingZeroes : NumericInstruction()
    /** i32.popcnt */
    object I32CountNonZeroBits : NumericInstruction()
    /** i32.add */
    object I32Add : NumericInstruction()
    /** i32.sub */
    object I32Subtract : NumericInstruction()
    /** i32.mul */
    object I32Multiply : NumericInstruction()
    /** i32.div_s */
    object I32DivideSigned : NumericInstruction()
    /** i32.div_u */
    object I32DivideUnsigned : NumericInstruction()
    /** i32.rem_s */
    object I32RemainderSigned : NumericInstruction()
    /** i32.rem_u */
    object I32RemainderUnsigned : NumericInstruction()
    /** i32.and */
    object I32BitwiseAnd : NumericInstruction()
    /** i32.or */
    object I32BitwiseOr : NumericInstruction()
    /** i32.xor */
    object I32BitwiseXor : NumericInstruction()
    /** i32.shl */
    object I32ShiftLeft : NumericInstruction()
    /** i32.shr_s */
    object I32ShiftRightSigned : NumericInstruction()
    /** i32.shr_u */
    object I32ShiftRightUnsigned : NumericInstruction()
    /** i32.rotl */
    object I32RotateLeft : NumericInstruction()
    /** i32.rotr */
    object I32RotateRight : NumericInstruction()
    /** i32.eqz */
    object I32EqualsZero : NumericInstruction()
    /** i32.eq */
    object I32Equals : NumericInstruction()
    /** i32.ne */
    object I32NotEquals : NumericInstruction()
    /** i32.lt_s */
    object I32LessThanSigned : NumericInstruction()
    /** i32.lt_u */
    object I32LessThanUnsigned : NumericInstruction()
    /** i32.gt_s */
    object I32GreaterThanSigned : NumericInstruction()
    /** i32.gt_u */
    object I32GreaterThanUnsigned : NumericInstruction()
    /** i32.le_s */
    object I32LessThanEqualToSigned : NumericInstruction()
    /** i32.le_u */
    object I32LessThanEqualToUnsigned : NumericInstruction()
    /** i32.ge_s */
    object I32GreaterThanEqualToSigned : NumericInstruction()
    /** i32.ge_u */
    object I32GreaterThanEqualToUnsigned : NumericInstruction()

    /** i64.clz */
    object I64CountLeadingZeroes : NumericInstruction()
    /** i64.ctz */
    object I64CountTrailingZeroes : NumericInstruction()
    /** i64.popcnt */
    object I64CountNonZeroBits : NumericInstruction()
    /** i64.add */
    object I64Add : NumericInstruction()
    /** i64.sub */
    object I64Subtract : NumericInstruction()
    /** i64.mul */
    object I64Multiply : NumericInstruction()
    /** i64.div_s */
    object I64DivideSigned : NumericInstruction()
    /** i64.div_u */
    object I64DivideUnsigned : NumericInstruction()
    /** i64.rem_s */
    object I64RemainderSigned : NumericInstruction()
    /** i64.rem_u */
    object I64RemainderUnsigned : NumericInstruction()
    /** i64.and */
    object I64BitwiseAnd : NumericInstruction()
    /** i64.or */
    object I64BitwiseOr : NumericInstruction()
    /** i64.xor */
    object I64BitwiseXor : NumericInstruction()
    /** i64.shl */
    object I64ShiftLeft : NumericInstruction()
    /** i64.shr_s */
    object I64ShiftRightSigned : NumericInstruction()
    /** i64.shr_u */
    object I64ShiftRightUnsigned : NumericInstruction()
    /** i64.rotl */
    object I64RotateLeft : NumericInstruction()
    /** i64.rotr */
    object I64RotateRight : NumericInstruction()
    /** i64.eqz */
    object I64EqualsZero : NumericInstruction()
    /** i64.eq */
    object I64Equals : NumericInstruction()
    /** i64.eq */
    object I64NotEquals : NumericInstruction()
    /** i64.lt_s */
    object I64LessThanSigned : NumericInstruction()
    /** i64.lt_u */
    object I64LessThanUnsigned : NumericInstruction()
    /** i64.gt_s */
    object I64GreaterThanSigned : NumericInstruction()
    /** i64.gt_u */
    object I64GreaterThanUnsigned : NumericInstruction()
    /** i64.le_s */
    object I64LessThanEqualToSigned : NumericInstruction()
    /** i64.le_u */
    object I64LessThanEqualToUnsigned : NumericInstruction()
    /** i64.ge_s */
    object I64GreaterThanEqualToSigned : NumericInstruction()
    /** i64.ge_u */
    object I64GreaterThanEqualToUnsigned : NumericInstruction()

    /** f32.abs */
    object F32AbsoluteValue : NumericInstruction()
    /** f32.neg */
    object F32Negative : NumericInstruction()
    /** f32.ceil */
    object F32Ceiling : NumericInstruction()
    /** f32.floor */
    object F32Floor : NumericInstruction()
    /** f32.trunc */
    object F32Truncate : NumericInstruction()
    /** f32.nearest */
    object F32Nearest : NumericInstruction()
    /** f32.sqrt */
    object F32SquareRoot : NumericInstruction()
    /** f32.add */
    object F32Add : NumericInstruction()
    /** f32.sub */
    object F32Subtract : NumericInstruction()
    /** f32.mul */
    object F32Multiply : NumericInstruction()
    /** f32.div */
    object F32Divide : NumericInstruction()
    /** f32.min */
    object F32Min : NumericInstruction()
    /** f32.max */
    object F32Max : NumericInstruction()
    /** f32.copysign */
    object F32CopySign : NumericInstruction()
    /** f32.eq */
    object F32Equals : NumericInstruction()
    /** f32.ne */
    object F32NotEquals : NumericInstruction()
    /** f32.lt */
    object F32LessThan : NumericInstruction()
    /** f32.gt */
    object F32GreaterThan : NumericInstruction()
    /** f32.le */
    object F32LessThanEqualTo : NumericInstruction()
    /** f32.ge */
    object F32GreaterThanEqualTo : NumericInstruction()

    /** f64.abs */
    object F64AbsoluteValue : NumericInstruction()
    /** f64.neg */
    object F64Negative : NumericInstruction()
    /** f64.ceil */
    object F64Ceiling : NumericInstruction()
    /** f64.floor */
    object F64Floor : NumericInstruction()
    /** f64.trunc */
    object F64Truncate : NumericInstruction()
    /** f64.nearest */
    object F64Nearest : NumericInstruction()
    /** f64.sqrt */
    object F64SquareRoot : NumericInstruction()
    /** f64.add */
    object F64Add : NumericInstruction()
    /** f64.sub */
    object F64Subtract : NumericInstruction()
    /** f64.mul */
    object F64Multiply : NumericInstruction()
    /** f64.div */
    object F64Divide : NumericInstruction()
    /** f64.min */
    object F64Min : NumericInstruction()
    /** f64.max */
    object F64Max : NumericInstruction()
    /** f64.copysign */
    object F64CopySign : NumericInstruction()
    /** f64.eq */
    object F64Equals : NumericInstruction()
    /** f64.ne */
    object F64NotEquals : NumericInstruction()
    /** f64.lt */
    object F64LessThan : NumericInstruction()
    /** f64.gt */
    object F64GreaterThan : NumericInstruction()
    /** f64.le */
    object F64LessThanEqualTo : NumericInstruction()
    /** f64.ge */
    object F64GreaterThanEqualTo : NumericInstruction()

    /*
     * Conversions
     */

    /** i32.wrap_i64 */
    object I32WrapI64 : NumericInstruction()
    /** i32.trunc_f32_s */
    object I32TruncateF32Signed : NumericInstruction()
    /** i32.trunc_f32_u */
    object I32TruncateF32Unsigned : NumericInstruction()
    /** i32.trunc_f64_s */
    object I32TruncateF64Signed : NumericInstruction()
    /** i32.trunc_f64_u */
    object I32TruncateF64Unsigned : NumericInstruction()
    /** i32.reinterpret_f32 */
    object I32ReinterpretF32 : NumericInstruction()

    /** i64.extend_i32_s */
    object I64ExtendI32Signed : NumericInstruction()
    /** i64.extend_i32_u */
    object I64ExtendI32Unsigned : NumericInstruction()
    /** i64.trunc_f32_s */
    object I64TruncateF32Signed : NumericInstruction()
    /** i64.trunc_f32_u */
    object I64TruncateF32Unsigned : NumericInstruction()
    /** i64.trunc_f64_s */
    object I64TruncateF64Signed : NumericInstruction()
    /** i64.trunc_f64_u */
    object I64TruncateF64Unsigned : NumericInstruction()
    /** i64.reinterpret_f64 */
    object I64ReinterpretF64 : NumericInstruction()

    /** f32.convert_i32_s */
    object F32ConvertI32Signed : NumericInstruction()
    /** f32.convert_i32_u */
    object F32ConvertI32Unsigned : NumericInstruction()
    /** f32.convert_i64_s */
    object F32ConvertI64Signed : NumericInstruction()
    /** f32.convert_i64_u */
    object F32ConvertI64Unsigned : NumericInstruction()
    /** f32.demote_f64 */
    object F32DemoteF64 : NumericInstruction()
    /** f32.reinterpret_i32 */
    object F32ReinterpretI32 : NumericInstruction()

    /** f64.convert_i32_s */
    object F64ConvertI32Signed : NumericInstruction()
    /** f64.convert_i32_u */
    object F64ConvertI32Unsigned : NumericInstruction()
    /** f64.convert_i64_s */
    object F64ConvertI64Signed : NumericInstruction()
    /** f64.convert_i64_u */
    object F64ConvertI64Unsigned : NumericInstruction()
    /** f64.promote_f32 */
    object F64PromoteF32 : NumericInstruction()
    /** f64.reinterpret_i64 */
    object F64ReinterpretI64 : NumericInstruction()

    /** i32.extend8_s */
    object I32Extend8Signed : NumericInstruction()
    /** i32.extend16_s */
    object I32Extend16Signed : NumericInstruction()
    /** i64.extend8_s */
    object I64Extend8Signed : NumericInstruction()
    /** i64.extend16_s */
    object I64Extend16Signed : NumericInstruction()
    /** i64.extend32_s */
    object I64Extend32Signed : NumericInstruction()

    /** i32.trunc_sat_f32_s */
    object I32TruncateSaturatedF32Signed : NumericInstruction()
    /** i32.trunc_sat_f32_u */
    object I32TruncateSaturatedF32Unsigned : NumericInstruction()
    /** i32.trunc_sat_f64_s */
    object I32TruncateSaturatedF64Signed : NumericInstruction()
    /** i32.trunc_sat_f64_u */
    object I32TruncateSaturatedF64Unsigned : NumericInstruction()
    /** i64.trunc_sat_f32_s */
    object I64TruncateSaturatedF32Signed : NumericInstruction()
    /** i64.trunc_sat_f32_u */
    object I64TruncateSaturatedF32Unsigned : NumericInstruction()
    /** i64.trunc_sat_f64_s */
    object I64TruncateSaturatedF64Signed : NumericInstruction()
    /** i64.trunc_sat_f64_u */
    object I64TruncateSaturatedF64Unsigned : NumericInstruction()
}
