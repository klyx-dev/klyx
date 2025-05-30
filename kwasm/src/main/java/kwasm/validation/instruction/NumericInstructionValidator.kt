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

package kwasm.validation.instruction

import kwasm.ast.instruction.NumericInstruction
/* ktlint-disable no-wildcard-imports */
import kwasm.ast.instruction.NumericInstruction.*
/* ktlint-enable no-wildcard-imports */
import kwasm.ast.type.ValueType
import kwasm.ast.type.ValueType.F32
import kwasm.ast.type.ValueType.F64
import kwasm.ast.type.ValueType.I32
import kwasm.ast.type.ValueType.I64
import kwasm.validation.FunctionBodyValidationVisitor
import kwasm.validation.ValidationContext
import kwasm.validation.validate

/**
 * Validates [NumericInstruction] nodes.
 *
 * From
 * [the docs](https://webassembly.github.io/spec/core/valid/instructions.html#numeric-instructions):
 *
 * ```
 *   t.unop
 * ```
 * * The [NumericInstruction] is valid with type `\[t] => \[t]`.
 *
 * ```
 *   t.binop
 * ```
 * * The [NumericInstruction] is valid with type `[t t] => \[t]`.
 *
 * ```
 *   t.testop
 * ```
 * * The [NumericInstruction] is valid with type `\[t] => \[i32]`.
 *
 * ```
 *   t.relop
 * ```
 * * The [NumericInstruction] is valid with type `[t t] => \[i32]`.
 *
 * ```
 *   t2.cvtop_t1_sx?
 * ```
 * * The [NumericInstruction] is valid with type `\[t1] => \[t2]`.
 */
object NumericInstructionValidator : FunctionBodyValidationVisitor<NumericInstruction> {
    override fun visit(
        node: NumericInstruction,
        context: ValidationContext.FunctionBody
    ): ValidationContext.FunctionBody {
        val (inputs, output) = node.getExpectedInsAndOuts()

        val (stackArgs, poppedContext) = context.popStack(inputs.size)

        validate(inputs == stackArgs, parseContext = null) {
            "Instruction requires top of stack to have args: $inputs, but at this point the " +
                "stack has $stackArgs (type mismatch)"
        }

        return poppedContext.pushStack(output)
    }
}

internal fun NumericInstruction.getExpectedInsAndOuts(): Pair<List<ValueType>, ValueType> =
    when (this) {
        I32CountLeadingZeroes,
        I32CountTrailingZeroes,
        I32EqualsZero,
        I32CountNonZeroBits -> listOf(I32) to I32
        I32Add,
        I32Subtract,
        I32Multiply,
        I32DivideSigned,
        I32DivideUnsigned,
        I32RemainderSigned,
        I32RemainderUnsigned,
        I32BitwiseAnd,
        I32BitwiseOr,
        I32BitwiseXor,
        I32ShiftLeft,
        I32ShiftRightSigned,
        I32ShiftRightUnsigned,
        I32RotateLeft,
        I32RotateRight,
        I32Equals,
        I32NotEquals,
        I32LessThanSigned,
        I32LessThanUnsigned,
        I32GreaterThanSigned,
        I32GreaterThanUnsigned,
        I32LessThanEqualToSigned,
        I32LessThanEqualToUnsigned,
        I32GreaterThanEqualToSigned,
        I32GreaterThanEqualToUnsigned -> listOf(I32, I32) to I32

        I64CountLeadingZeroes,
        I64CountTrailingZeroes,
        I64EqualsZero,
        I64CountNonZeroBits -> listOf(I64) to I64
        I64Add,
        I64Subtract,
        I64Multiply,
        I64DivideSigned,
        I64DivideUnsigned,
        I64RemainderSigned,
        I64RemainderUnsigned,
        I64BitwiseAnd,
        I64BitwiseOr,
        I64BitwiseXor,
        I64ShiftLeft,
        I64ShiftRightSigned,
        I64ShiftRightUnsigned,
        I64RotateLeft,
        I64RotateRight -> listOf(I64, I64) to I64
        I64Equals,
        I64NotEquals,
        I64LessThanSigned,
        I64LessThanUnsigned,
        I64GreaterThanSigned,
        I64GreaterThanUnsigned,
        I64LessThanEqualToSigned,
        I64LessThanEqualToUnsigned,
        I64GreaterThanEqualToSigned,
        I64GreaterThanEqualToUnsigned -> listOf(I64, I64) to I32

        F32AbsoluteValue,
        F32Negative,
        F32Ceiling,
        F32Floor,
        F32Truncate,
        F32Nearest,
        F32SquareRoot -> listOf(F32) to F32
        F32Add,
        F32Subtract,
        F32Multiply,
        F32Divide,
        F32Min,
        F32Max,
        F32CopySign -> listOf(F32, F32) to F32
        F32Equals,
        F32NotEquals,
        F32LessThan,
        F32GreaterThan,
        F32LessThanEqualTo,
        F32GreaterThanEqualTo -> listOf(F32, F32) to I32

        F64AbsoluteValue,
        F64Negative,
        F64Ceiling,
        F64Floor,
        F64Truncate,
        F64Nearest,
        F64SquareRoot -> listOf(F64) to F64
        F64Add,
        F64Subtract,
        F64Multiply,
        F64Divide,
        F64Min,
        F64Max,
        F64CopySign -> listOf(F64, F64) to F64
        F64Equals,
        F64NotEquals,
        F64LessThan,
        F64GreaterThan,
        F64LessThanEqualTo,
        F64GreaterThanEqualTo -> listOf(F64, F64) to I32

        I32WrapI64 -> listOf(I64) to I32
        I32TruncateF32Signed,
        I32ReinterpretF32,
        I32TruncateF32Unsigned -> listOf(F32) to I32
        I32TruncateF64Signed,
        I32TruncateF64Unsigned -> listOf(F64) to I32

        I64ExtendI32Signed,
        I64ExtendI32Unsigned -> listOf(I32) to I64
        I64TruncateF32Signed,
        I64TruncateF32Unsigned -> listOf(F32) to I64
        I64TruncateF64Signed,
        I64TruncateF64Unsigned,
        I64ReinterpretF64 -> listOf(F64) to I64

        F32ConvertI32Signed,
        F32ConvertI32Unsigned,
        F32ReinterpretI32 -> listOf(I32) to F32
        F32ConvertI64Signed,
        F32ConvertI64Unsigned -> listOf(I64) to F32
        F32DemoteF64 -> listOf(F64) to F32

        F64ConvertI32Signed,
        F64ConvertI32Unsigned -> listOf(I32) to F64
        F64ConvertI64Signed,
        F64ConvertI64Unsigned,
        F64ReinterpretI64 -> listOf(I64) to F64
        F64PromoteF32 -> listOf(F32) to F64

        I32Extend8Signed,
        I32Extend16Signed -> listOf(I32) to I32

        I64Extend8Signed,
        I64Extend16Signed,
        I64Extend32Signed -> listOf(I64) to I64

        I32TruncateSaturatedF32Signed,
        I32TruncateSaturatedF32Unsigned -> listOf(F32) to I32
        I32TruncateSaturatedF64Signed,
        I32TruncateSaturatedF64Unsigned -> listOf(F64) to I32
        I64TruncateSaturatedF32Signed,
        I64TruncateSaturatedF32Unsigned -> listOf(F32) to I64
        I64TruncateSaturatedF64Signed,
        I64TruncateSaturatedF64Unsigned -> listOf(F64) to I64
    }
