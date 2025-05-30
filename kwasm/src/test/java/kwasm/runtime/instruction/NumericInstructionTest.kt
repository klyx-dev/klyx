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

package kwasm.runtime.instruction

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import kwasm.KWasmRuntimeException
import kwasm.ParseRule
import kwasm.ast.instruction.Instruction
import kwasm.runtime.EmptyExecutionContext
import kwasm.runtime.ExecutionContext
import kwasm.runtime.FloatValue
import kwasm.runtime.IntValue
import kwasm.runtime.Value
import kwasm.runtime.toValue
import kwasm.runtime.utils.thatContext
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import kotlin.math.sqrt
import kotlin.random.Random

@ExperimentalStdlibApi
@Suppress(
    "EXPERIMENTAL_API_USAGE",
    "EXPERIMENTAL_UNSIGNED_LITERALS",
    "CanBeVal",
    "FLOAT_LITERAL_CONFORMS_ZERO"
)
@RunWith(JUnit4::class)
class NumericInstructionTest {
    @get:Rule
    val parser = ParseRule()

    private val executionContext: ExecutionContext
        get() = EmptyExecutionContext()

    @Test
    fun i32Add() = listOf(
        TestCase(0, 0, 0),
        TestCase(1, 0, 1),
        TestCase(1, 1, 0),
        TestCase(-1, 0, -1),
        TestCase(-1, -1, 0)
    ).forEach { it.check("i32.add") }

    @Test
    fun i32CountLeadingZeroes() {
        TestCase(32, 0).check("i32.clz")

        var stackValue = 1
        (1 until 32).forEach {
            TestCase(32 - it, stackValue).check("i32.clz")
            stackValue = stackValue shl 1
        }
    }

    @Test
    fun i32CountTrailingZeroes() {
        TestCase(32, 0).check("i32.ctz")

        var stackValue = 1 shl 31
        (1 until 32).forEach {
            TestCase(32 - it, stackValue).check("i32.ctz")
            stackValue = stackValue ushr 1
        }
    }

    @Test
    fun i32CountNonZeroBits() {
        TestCase(0, 0).check("i32.popcnt")
        TestCase(32, -1).check("i32.popcnt")

        var stackStartValue = 1
        (1 until 32).forEach { expected ->
            var stackValue = stackStartValue
            // shift the stack value across the space
            repeat(32 - expected) {
                TestCase(expected, stackValue).check("i32.popcnt")
                stackValue = stackValue shl 1
            }
            // add a 1-bit to the stack start value
            stackStartValue = (stackStartValue shl 1) or 1
        }
    }

    @Test
    fun i32EqualsZero() = listOf(
        TestCase(1, 0),
        TestCase(0, 1),
        TestCase(0, -1)
    ).forEach { it.check("i32.eqz") }

    @Test
    fun i32Equals() = listOf(
        TestCase(1, 42, 42),
        TestCase(0, 42, 41),
        TestCase(0, 41, 42)
    ).forEach { it.check("i32.eq") }

    @Test
    fun i32NotEquals() = listOf(
        TestCase(0, 42, 42),
        TestCase(1, 42, 41),
        TestCase(1, 41, 42)
    ).forEach { it.check("i32.ne") }

    @Test
    fun i32LessThanSigned() = listOf(
        TestCase(0, 42, 42),
        TestCase(0, 42, 41),
        TestCase(1, 41, 42),
        TestCase(1, -41, 42),
        TestCase(1, -42, -41)
    ).forEach { it.check("i32.lt_s") }

    @Test
    fun i32LessThanUnsigned() = listOf(
        TestCase(0, 42, 42),
        TestCase(0, 42, 41),
        TestCase(1, 41, 42),
        TestCase(0, -41, 42),
        TestCase(1, -42, -41),
        TestCase(1, 0, -1)
    ).forEach { it.check("i32.lt_u") }

    @Test
    fun i32GreaterThanSigned() = listOf(
        TestCase(0, 42, 42),
        TestCase(1, 42, 41),
        TestCase(0, 41, 42),
        TestCase(0, -41, 42),
        TestCase(0, -42, -41)
    ).forEach { it.check("i32.gt_s") }

    @Test
    fun i32GreaterThanUnsigned() = listOf(
        TestCase(0, 42, 42),
        TestCase(1, 42, 41),
        TestCase(0, 41, 42),
        TestCase(1, -41, 42),
        TestCase(0, -42, -41),
        TestCase(0, 0, -1)
    ).forEach { it.check("i32.gt_u") }

    @Test
    fun i32LessThanEqualToSigned() = listOf(
        TestCase(1, 42, 42),
        TestCase(0, 42, 41),
        TestCase(1, 41, 42),
        TestCase(1, -41, 42),
        TestCase(1, -42, -41)
    ).forEach { it.check("i32.le_s") }

    @Test
    fun i32LessThanEqualToUnsigned() = listOf(
        TestCase(1, 42, 42),
        TestCase(0, 42, 41),
        TestCase(1, 41, 42),
        TestCase(0, -41, 42),
        TestCase(1, -42, -41),
        TestCase(1, 0, -1)
    ).forEach { it.check("i32.le_u") }

    @Test
    fun i32GreaterThanEqualToSigned() = listOf(
        TestCase(1, 42, 42),
        TestCase(1, 42, 41),
        TestCase(0, 41, 42),
        TestCase(0, -41, 42),
        TestCase(0, -42, -41)
    ).forEach { it.check("i32.ge_s") }

    @Test
    fun i32GreaterThanEqualToUnsigned() = listOf(
        TestCase(1, 42, 42),
        TestCase(1, 42, 41),
        TestCase(0, 41, 42),
        TestCase(1, -41, 42),
        TestCase(0, -42, -41),
        TestCase(0, 0, -1)
    ).forEach { it.check("i32.ge_u") }

    @Test
    fun i32Subtract() = listOf(
        TestCase(0, 0, 0),
        TestCase(-1, 0, 1),
        TestCase(1, 1, 0),
        TestCase(1, 0, -1),
        TestCase(-1, -1, 0)
    ).forEach { it.check("i32.sub") }

    @Test
    fun i32Multiply() = listOf(
        TestCase(0, 0, 0),
        TestCase(0, 0, 1),
        TestCase(0, 1, 0),
        TestCase(-2, 2, -1),
        TestCase(4, 2, 2)
    ).forEach { it.check("i32.mul") }

    @Test
    fun i32DivideSigned() {
        listOf(
            KWasmErrorTestCase("Cannot divide by zero", 1, 0),
            KWasmErrorTestCase("Quotient unrepresentable as 32bit integer", Int.MIN_VALUE, -1)
        ).forEach { it.check("i32.div_s") }

        listOf(
            TestCase(0, 0, 1),
            TestCase(-2, 2, -1),
            TestCase(1, 2, 2),
            TestCase(0, 2, 4)
        ).forEach { it.check("i32.div_s") }
    }

    @Test
    fun i32DivideUnsigned() {
        listOf(
            KWasmErrorTestCase("Cannot divide by zero", 1, 0)
        ).forEach { it.check("i32.div_u") }

        listOf(
            TestCase(0, 0, 1),
            TestCase(0, 2, -1),
            TestCase(UInt.MAX_VALUE.toInt(), UInt.MAX_VALUE.toInt(), 1),
            TestCase(1, 2, 2),
            TestCase(0, 2, 4)
        ).forEach { it.check("i32.div_u") }
    }

    @Test
    fun i32RemainderSigned() {
        KWasmErrorTestCase("Cannot divide by zero", 1, 0).check("i32.rem_s")

        listOf(
            TestCase(0, 0, 1),
            TestCase(1, 1, -2),
            TestCase(-1, -1, 2),
            TestCase(-1, -1, -2),
            TestCase(0, 2, 2),
            TestCase(2, 2, 4)
        ).forEach { it.check("i32.rem_s") }
    }

    @Test
    fun i32RemainderUnsigned() {
        KWasmErrorTestCase("Cannot divide by zero", 1, 0).check("i32.rem_u")

        listOf(
            TestCase(0, 0, 1),
            TestCase(0, 2, 2),
            TestCase(2, 2, 4)
        ).forEach { it.check("i32.rem_u") }
    }

    @Test
    fun i32BitwiseAnd() {
        val random = Random(System.currentTimeMillis())

        repeat(500) {
            val randomLhs = random.nextInt()
            val randomRhs = random.nextInt()
            val expected = randomLhs and randomRhs
            TestCase(expected, randomLhs, randomRhs).check("i32.and")
        }
    }

    @Test
    fun i32BitwiseOr() {
        val random = Random(System.currentTimeMillis())

        repeat(500) {
            val randomLhs = random.nextInt()
            val randomRhs = random.nextInt()
            val expected = randomLhs or randomRhs
            TestCase(expected, randomLhs, randomRhs).check("i32.or")
        }
    }

    @Test
    fun i32BitwiseXor() {
        val random = Random(System.currentTimeMillis())

        repeat(500) {
            val randomLhs = random.nextInt()
            val randomRhs = random.nextInt()
            val expected = randomLhs xor randomRhs
            TestCase(expected, randomLhs, randomRhs).check("i32.xor")
        }
    }

    @Test
    fun i32ShiftLeft() {
        val random = Random(System.currentTimeMillis())

        repeat(500) {
            val randomLhs = random.nextInt()
            val randomRhs = random.nextInt()
            val expected = randomLhs shl randomRhs
            TestCase(expected, randomLhs, randomRhs).check("i32.shl")
        }
    }

    @Test
    fun i32ShiftRightSigned() {
        val random = Random(System.currentTimeMillis())

        repeat(500) {
            val randomLhs = random.nextInt()
            val randomRhs = random.nextInt()
            val expected = randomLhs shr randomRhs
            TestCase(expected, randomLhs, randomRhs).check("i32.shr_s")
        }
    }

    @Test
    fun i32ShiftRightUnsigned() {
        val random = Random(System.currentTimeMillis())

        repeat(500) {
            val randomLhs = random.nextInt()
            val randomRhs = random.nextInt()
            val expected = randomLhs ushr randomRhs
            TestCase(expected, randomLhs, randomRhs).check("i32.shr_u")
        }
    }

    @Test
    fun i32RotateLeft() {
        val random = Random(System.currentTimeMillis())

        repeat(500) {
            val randomLhs = random.nextInt()
            val randomRhs = random.nextInt()
            val expected = randomLhs.rotateLeft(randomRhs)
            TestCase(expected, randomLhs, randomRhs).check("i32.rotl")
        }
    }

    @Test
    fun i32RotateRight() {
        val random = Random(System.currentTimeMillis())

        repeat(500) {
            val randomLhs = random.nextInt()
            val randomRhs = random.nextInt()
            val expected = randomLhs.rotateRight(randomRhs)
            TestCase(expected, randomLhs, randomRhs).check("i32.rotr")
        }
    }

    @Test
    fun i64CountLeadingZeroes() {
        TestCase(64L, 0L).check("i64.clz")

        var stackValue = 1L
        (1 until 64).forEach {
            TestCase((64 - it).toLong(), stackValue).check("i64.clz")
            stackValue = stackValue shl 1
        }
    }

    @Test
    fun i64CountTrailingZeroes() {
        TestCase(64L, 0L).check("i64.ctz")

        var stackValue = 1L shl 63
        (1 until 64).forEach {
            TestCase((64 - it).toLong(), stackValue).check("i64.ctz")
            stackValue = stackValue ushr 1
        }
    }

    @Test
    fun i64CountNonZeroBits() {
        TestCase(0L, 0L).check("i64.popcnt")
        TestCase(64L, -1L).check("i64.popcnt")

        var stackStartValue = 1L
        (1 until 64).forEach { expected ->
            var stackValue = stackStartValue
            // shift the stack value across the space
            repeat(64 - expected) {
                TestCase(expected.toLong(), stackValue).check("i64.popcnt")
                stackValue = stackValue shl 1
            }
            // add a 1-bit to the stack start value
            stackStartValue = (stackStartValue shl 1) or 1
        }
    }

    @Test
    fun i64Add() = listOf(
        TestCase(0L, 0L, 0L),
        TestCase(1L, 0L, 1L),
        TestCase(1L, 1L, 0L),
        TestCase(-1L, 0L, -1L),
        TestCase(-1L, -1L, 0L)
    ).forEach { it.check("i64.add") }

    @Test
    fun i64Subtract() = listOf(
        TestCase(0L, 0L, 0L),
        TestCase(-1L, 0L, 1L),
        TestCase(1L, 1L, 0L),
        TestCase(1L, 0L, -1L),
        TestCase(-1L, -1L, 0L)
    ).forEach { it.check("i64.sub") }

    @Test
    fun i64Multiply() = listOf(
        TestCase(0L, 0L, 0L),
        TestCase(0L, 0L, 1L),
        TestCase(0L, 1L, 0L),
        TestCase(-2L, 2L, -1L),
        TestCase(4L, 2L, 2L)
    ).forEach { it.check("i64.mul") }

    @Test
    fun i64DivideSigned() {
        listOf(
            KWasmErrorTestCase("Cannot divide by zero", 1L, 0L),
            KWasmErrorTestCase("Quotient unrepresentable as 64bit integer", Long.MIN_VALUE, -1L)
        ).forEach { it.check("i64.div_s") }

        listOf(
            TestCase(0L, 0L, 1L),
            TestCase(-2L, 2L, -1L),
            TestCase(1L, 2L, 2L),
            TestCase(0L, 2L, 4L)
        ).forEach { it.check("i64.div_s") }
    }

    @Test
    fun i64DivideUnsigned() {
        KWasmErrorTestCase("Cannot divide by zero", 1L, 0L).check("i64.div_u")

        listOf(
            TestCase(0L, 0L, 1L),
            TestCase(0L, 2L, -1L),
            TestCase(ULong.MAX_VALUE.toLong(), ULong.MAX_VALUE.toLong(), 1L),
            TestCase(1L, 2L, 2L),
            TestCase(0L, 2L, 4L)
        ).forEach { it.check("i64.div_u") }
    }

    @Test
    fun i64RemainderSigned() {
        KWasmErrorTestCase("Cannot divide by zero", 1L, 0L).check("i64.rem_s")

        listOf(
            TestCase(0L, 0L, 1L),
            TestCase(1L, 1L, -2L),
            TestCase(-1L, -1L, 2L),
            TestCase(-1L, -1L, -2L),
            TestCase(0L, 2L, 2L),
            TestCase(2L, 2L, 4L)
        ).forEach { it.check("i64.rem_s") }
    }

    @Test
    fun i64RemainderUnsigned() {
        KWasmErrorTestCase("Cannot divide by zero", 1L, 0L).check("i64.rem_u")

        listOf(
            TestCase(0L, 0L, 1L),
            TestCase(0L, 2L, 2L),
            TestCase(2L, 2L, 4L)
        ).forEach { it.check("i64.rem_u") }
    }

    @Test
    fun i64BitwiseAnd() {
        val random = Random(System.currentTimeMillis())

        repeat(500) {
            val randomLhs = random.nextLong()
            val randomRhs = random.nextLong()
            val expected = randomLhs and randomRhs
            TestCase(expected, randomLhs, randomRhs).check("i64.and")
        }
    }

    @Test
    fun i64BitwiseOr() {
        val random = Random(System.currentTimeMillis())

        repeat(500) {
            val randomLhs = random.nextLong()
            val randomRhs = random.nextLong()
            val expected = randomLhs or randomRhs
            TestCase(expected, randomLhs, randomRhs).check("i64.or")
        }
    }

    @Test
    fun i64BitwiseXor() {
        val random = Random(System.currentTimeMillis())

        repeat(500) {
            val randomLhs = random.nextLong()
            val randomRhs = random.nextLong()
            val expected = randomLhs xor randomRhs
            TestCase(expected, randomLhs, randomRhs).check("i64.xor")
        }
    }

    @Test
    fun i64ShiftLeft() {
        val random = Random(System.currentTimeMillis())

        repeat(500) {
            val randomLhs = random.nextLong()
            val randomRhs = random.nextLong().toULong()
            val expected = randomLhs shl (randomRhs % 64uL).toInt()
            TestCase(expected, randomLhs, randomRhs.toLong()).check("i64.shl")
        }
    }

    @Test
    fun i64ShiftRightSigned() {
        val random = Random(System.currentTimeMillis())

        repeat(500) {
            val randomLhs = random.nextLong()
            val randomRhs = random.nextLong().toULong()
            val expected = randomLhs shr (randomRhs % 64uL).toInt()
            TestCase(expected, randomLhs, randomRhs.toLong()).check("i64.shr_s")
        }
    }

    @Test
    fun i64ShiftRightUnsigned() {
        val random = Random(System.currentTimeMillis())

        repeat(500) {
            val randomLhs = random.nextLong()
            val randomRhs = random.nextLong().toULong()
            val expected = randomLhs ushr (randomRhs % 64uL).toInt()
            TestCase(expected, randomLhs, randomRhs.toLong()).check("i64.shr_u")
        }
    }

    @Test
    fun i64RotateLeft() {
        val random = Random(System.currentTimeMillis())

        repeat(500) {
            val randomLhs = random.nextLong()
            val randomRhs = random.nextLong().toULong()
            val expected = randomLhs.rotateLeft((randomRhs % 64uL).toInt())
            TestCase(expected, randomLhs, randomRhs.toLong()).check("i64.rotl")
        }
    }

    @Test
    fun i64RotateRight() {
        val random = Random(System.currentTimeMillis())

        repeat(500) {
            val randomLhs = random.nextLong()
            val randomRhs = random.nextLong().toULong()
            val expected = randomLhs.rotateRight((randomRhs % 64uL).toInt())
            TestCase(expected, randomLhs, randomRhs.toLong()).check("i64.rotr")
        }
    }

    @Test
    fun i64EqualsZero() = listOf(
        TestCase(1, 0L),
        TestCase(0, 1L),
        TestCase(0, -1L)
    ).forEach { it.check("i64.eqz") }

    @Test
    fun i64Equals() = listOf(
        TestCase(1, 42L, 42L),
        TestCase(0, 42L, 41L),
        TestCase(0, 41L, 42L)
    ).forEach { it.check("i64.eq") }

    @Test
    fun i64NotEquals() = listOf(
        TestCase(0, 42L, 42L),
        TestCase(1, 42L, 41L),
        TestCase(1, 41L, 42L)
    ).forEach { it.check("i64.ne") }

    @Test
    fun i64LessThanSigned() = listOf(
        TestCase(0, 42L, 42L),
        TestCase(0, 42L, 41L),
        TestCase(1, 41L, 42L),
        TestCase(1, -41L, 42L),
        TestCase(1, -42L, -41L)
    ).forEach { it.check("i64.lt_s") }

    @Test
    fun i64LessThanUnsigned() = listOf(
        TestCase(0, 42L, 42L),
        TestCase(0, 42L, 41L),
        TestCase(1, 41L, 42L),
        TestCase(0, -41L, 42L),
        TestCase(1, -42L, -41L),
        TestCase(1, 0L, -1L)
    ).forEach { it.check("i64.lt_u") }

    @Test
    fun i64GreaterThanSigned() = listOf(
        TestCase(0, 42L, 42L),
        TestCase(1, 42L, 41L),
        TestCase(0, 41L, 42L),
        TestCase(0, -41L, 42L),
        TestCase(0, -42L, -41L)
    ).forEach { it.check("i64.gt_s") }

    @Test
    fun i64GreaterThanUnsigned() = listOf(
        TestCase(0, 42L, 42L),
        TestCase(1, 42L, 41L),
        TestCase(0, 41L, 42L),
        TestCase(1, -41L, 42L),
        TestCase(0, -42L, -41L),
        TestCase(0, 0L, -1L)
    ).forEach { it.check("i64.gt_u") }

    @Test
    fun i64LessThanEqualToSigned() = listOf(
        TestCase(1, 42L, 42L),
        TestCase(0, 42L, 41L),
        TestCase(1, 41L, 42L),
        TestCase(1, -41L, 42L),
        TestCase(1, -42L, -41L)
    ).forEach { it.check("i64.le_s") }

    @Test
    fun i64LessThanEqualToUnsigned() = listOf(
        TestCase(1, 42L, 42L),
        TestCase(0, 42L, 41L),
        TestCase(1, 41L, 42L),
        TestCase(0, -41L, 42L),
        TestCase(1, -42L, -41L),
        TestCase(1, 0L, -1L)
    ).forEach { it.check("i64.le_u") }

    @Test
    fun i64GreaterThanEqualToSigned() = listOf(
        TestCase(1, 42L, 42L),
        TestCase(1, 42L, 41L),
        TestCase(0, 41L, 42L),
        TestCase(0, -41L, 42L),
        TestCase(0, -42L, -41L)
    ).forEach { it.check("i64.ge_s") }

    @Test
    fun i64GreaterThanEqualToUnsigned() = listOf(
        TestCase(1, 42L, 42L),
        TestCase(1, 42L, 41L),
        TestCase(0, 41L, 42L),
        TestCase(1, -41L, 42L),
        TestCase(0, -42L, -41L),
        TestCase(0, 0L, -1L)
    ).forEach { it.check("i64.ge_u") }

    @Test
    fun f32AbsoluteValue() = parser.with {
        val instruction = "f32.abs".parseInstruction()

        var resultContext = instruction.execute(
            executionContextWithOpStack(Float.NEGATIVE_INFINITY.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Float.POSITIVE_INFINITY.toValue())

        resultContext = instruction.execute(executionContextWithOpStack(0f.toValue()))
        assertThat(resultContext).hasOpStackContaining(0f.toValue())

        resultContext = instruction.execute(executionContextWithOpStack((-0f).toValue()))
        assertThat(resultContext).hasOpStackContaining(0f.toValue())

        resultContext = instruction.execute(executionContextWithOpStack((-1f).toValue()))
        assertThat(resultContext).hasOpStackContaining(1f.toValue())

        resultContext = instruction.execute(executionContextWithOpStack(1f.toValue()))
        assertThat(resultContext).hasOpStackContaining(1f.toValue())
    }

    @Test
    fun f32Negative() = parser.with {
        val instruction = "f32.neg".parseInstruction()

        var resultContext = instruction.execute(
            executionContextWithOpStack(Float.NEGATIVE_INFINITY.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Float.POSITIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Float.POSITIVE_INFINITY.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Float.NEGATIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Float.NaN.toValue())
        )
        assertThat(resultContext).hasOpStackContaining((-Float.NaN).toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-Float.NaN).toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Float.NaN.toValue())

        resultContext = instruction.execute(executionContextWithOpStack(0f.toValue()))
        assertThat(resultContext).hasOpStackContaining((-0f).toValue())

        resultContext = instruction.execute(executionContextWithOpStack((-0f).toValue()))
        assertThat(resultContext).hasOpStackContaining(0f.toValue())

        resultContext = instruction.execute(executionContextWithOpStack((-1f).toValue()))
        assertThat(resultContext).hasOpStackContaining(1f.toValue())

        resultContext = instruction.execute(executionContextWithOpStack((-1f).toValue()))
        assertThat(resultContext).hasOpStackContaining(1f.toValue())
    }

    @Test
    fun f32Ceil() = parser.with {
        val instruction = "f32.ceil".parseInstruction()

        var resultContext = instruction.execute(
            executionContextWithOpStack(Float.NEGATIVE_INFINITY.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Float.NEGATIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Float.POSITIVE_INFINITY.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Float.POSITIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Float.NaN.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Float.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-Float.NaN).toValue())
        )
        assertThat(resultContext).hasOpStackContaining((-Float.NaN).toValue())

        resultContext = instruction.execute(executionContextWithOpStack(0.0f.toValue()))
        assertThat(resultContext).hasOpStackContaining(0.0f.toValue())

        resultContext = instruction.execute(executionContextWithOpStack((-0.0f).toValue()))
        assertThat(resultContext).hasOpStackContaining((-0.0f).toValue())

        resultContext = instruction.execute(executionContextWithOpStack((-0.1f).toValue()))
        assertThat(resultContext).hasOpStackContaining((-0.0f).toValue())

        resultContext = instruction.execute(executionContextWithOpStack(0.1f.toValue()))
        assertThat(resultContext).hasOpStackContaining(1.0f.toValue())
    }

    @Test
    fun f32Floor() = parser.with {
        val instruction = "f32.floor".parseInstruction()

        var resultContext = instruction.execute(
            executionContextWithOpStack(Float.NEGATIVE_INFINITY.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Float.NEGATIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Float.POSITIVE_INFINITY.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Float.POSITIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Float.NaN.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Float.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-Float.NaN).toValue())
        )
        assertThat(resultContext).hasOpStackContaining((-Float.NaN).toValue())

        resultContext = instruction.execute(executionContextWithOpStack(0.0f.toValue()))
        assertThat(resultContext).hasOpStackContaining(0.0f.toValue())

        resultContext = instruction.execute(executionContextWithOpStack((-0.0f).toValue()))
        assertThat(resultContext).hasOpStackContaining((-0.0f).toValue())

        resultContext = instruction.execute(executionContextWithOpStack((-0.1f).toValue()))
        assertThat(resultContext).hasOpStackContaining((-1.0f).toValue())

        resultContext = instruction.execute(executionContextWithOpStack(0.1f.toValue()))
        assertThat(resultContext).hasOpStackContaining(0.0f.toValue())
    }

    @Test
    fun f32Truncate() = parser.with {
        val instruction = "f32.trunc".parseInstruction()

        var resultContext = instruction.execute(
            executionContextWithOpStack(Float.NEGATIVE_INFINITY.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Float.NEGATIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Float.POSITIVE_INFINITY.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Float.POSITIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Float.NaN.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Float.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-Float.NaN).toValue())
        )
        assertThat(resultContext).hasOpStackContaining((-Float.NaN).toValue())

        resultContext = instruction.execute(executionContextWithOpStack(0.0f.toValue()))
        assertThat(resultContext).hasOpStackContaining(0.0f.toValue())

        resultContext = instruction.execute(executionContextWithOpStack((-0.0f).toValue()))
        assertThat(resultContext).hasOpStackContaining((-0.0f).toValue())

        resultContext = instruction.execute(executionContextWithOpStack((-0.1f).toValue()))
        assertThat(resultContext).hasOpStackContaining((-0.0f).toValue())

        resultContext = instruction.execute(executionContextWithOpStack(0.1f.toValue()))
        assertThat(resultContext).hasOpStackContaining(0.0f.toValue())

        resultContext = instruction.execute(executionContextWithOpStack(1.1f.toValue()))
        assertThat(resultContext).hasOpStackContaining(1.0f.toValue())

        resultContext = instruction.execute(executionContextWithOpStack((-1.1f).toValue()))
        assertThat(resultContext).hasOpStackContaining((-1.0f).toValue())
    }

    @Test
    fun f32Nearest() = parser.with {
        val instruction = "f32.nearest".parseInstruction()

        var resultContext = instruction.execute(
            executionContextWithOpStack(Float.NEGATIVE_INFINITY.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Float.NEGATIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Float.POSITIVE_INFINITY.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Float.POSITIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Float.NaN.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Float.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-Float.NaN).toValue())
        )
        assertThat(resultContext).hasOpStackContaining((-Float.NaN).toValue())

        resultContext = instruction.execute(executionContextWithOpStack(0.0f.toValue()))
        assertThat(resultContext).hasOpStackContaining(0.0f.toValue())

        resultContext = instruction.execute(executionContextWithOpStack((-0.0f).toValue()))
        assertThat(resultContext).hasOpStackContaining((-0.0f).toValue())

        resultContext = instruction.execute(executionContextWithOpStack((-0.1f).toValue()))
        assertThat(resultContext).hasOpStackContaining((-0.0f).toValue())

        resultContext = instruction.execute(executionContextWithOpStack(0.1f.toValue()))
        assertThat(resultContext).hasOpStackContaining(0.0f.toValue())

        resultContext = instruction.execute(executionContextWithOpStack(1.5f.toValue()))
        assertThat(resultContext).hasOpStackContaining(2.0f.toValue())

        resultContext = instruction.execute(executionContextWithOpStack((-1.5f).toValue()))
        assertThat(resultContext).hasOpStackContaining((-2.0f).toValue())
    }

    @Test
    fun f32SquareRoot() = parser.with {
        val instruction = "f32.sqrt".parseInstruction()

        var resultContext = instruction.execute(
            executionContextWithOpStack(Float.NEGATIVE_INFINITY.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Float.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Float.POSITIVE_INFINITY.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Float.POSITIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Float.NaN.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Float.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-Float.NaN).toValue())
        )
        assertThat(resultContext).hasOpStackContaining((-Float.NaN).toValue())

        resultContext = instruction.execute(executionContextWithOpStack(0.0f.toValue()))
        assertThat(resultContext).hasOpStackContaining(0.0f.toValue())

        resultContext = instruction.execute(executionContextWithOpStack((-0.0f).toValue()))
        assertThat(resultContext).hasOpStackContaining((-0.0f).toValue())

        resultContext = instruction.execute(executionContextWithOpStack((-0.1f).toValue()))
        assertThat(resultContext).hasOpStackContaining((-Float.NaN).toValue())

        resultContext = instruction.execute(executionContextWithOpStack(0.1f.toValue()))
        assertThat(resultContext).hasOpStackContaining(sqrt(0.1f).toValue())
    }

    @Test
    fun f32Add() = parser.with {
        val instruction = "f32.add".parseInstruction()
        var resultContext: ExecutionContext

        // NaNs.

        resultContext = instruction.execute(
            executionContextWithOpStack(Float.NaN.toValue(), 1f.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Float.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(1f.toValue(), Float.NaN.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Float.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Float.NaN.toValue(), Float.NaN.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Float.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-Float.NaN).toValue(), 1f.toValue())
        )
        assertThat(resultContext).hasOpStackContaining((-Float.NaN).toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(1f.toValue(), (-Float.NaN).toValue())
        )
        assertThat(resultContext).hasOpStackContaining((-Float.NaN).toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-Float.NaN).toValue(), (-Float.NaN).toValue())
        )
        assertThat(resultContext).hasOpStackContaining((-Float.NaN).toValue())

        // Infinites

        resultContext = instruction.execute(
            executionContextWithOpStack(
                Float.POSITIVE_INFINITY.toValue(),
                Float.NEGATIVE_INFINITY.toValue()
            )
        )
        assertThat(resultContext).hasOpStackContaining(Float.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(
                Float.NEGATIVE_INFINITY.toValue(),
                Float.POSITIVE_INFINITY.toValue()
            )
        )
        assertThat(resultContext).hasOpStackContaining(Float.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(
                Float.POSITIVE_INFINITY.toValue(),
                Float.POSITIVE_INFINITY.toValue()
            )
        )
        assertThat(resultContext).hasOpStackContaining(Float.POSITIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(
                Float.NEGATIVE_INFINITY.toValue(),
                Float.NEGATIVE_INFINITY.toValue()
            )
        )
        assertThat(resultContext).hasOpStackContaining(Float.NEGATIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(
                Float.POSITIVE_INFINITY.toValue(),
                1f.toValue()
            )
        )
        assertThat(resultContext).hasOpStackContaining(Float.POSITIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(
                1f.toValue(),
                Float.POSITIVE_INFINITY.toValue()
            )
        )
        assertThat(resultContext).hasOpStackContaining(Float.POSITIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(
                Float.NEGATIVE_INFINITY.toValue(),
                1f.toValue()
            )
        )
        assertThat(resultContext).hasOpStackContaining(Float.NEGATIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(
                1f.toValue(),
                Float.NEGATIVE_INFINITY.toValue()
            )
        )
        assertThat(resultContext).hasOpStackContaining(Float.NEGATIVE_INFINITY.toValue())

        // Zeroes

        listOf(
            TestCase(0f, 0f, -0f),
            TestCase(0f, -0f, 0f),
            TestCase(0f, 0f, 0f),
            TestCase(-0f, -0f, -0f),
            TestCase(42f, 0f, 42f),
            TestCase(42f, -0f, 42f),
            TestCase(42f, 42f, 0f),
            TestCase(42f, 42f, -0f)
        ).forEach { it.check("f32.add") }

        // Regular

        listOf(
            TestCase(0f, 42f, -42f),
            TestCase(42f + 42f, 42f, 42f)
        ).forEach { it.check("f32.add") }
    }

    @Test
    fun f32Sub() = parser.with {
        val instruction = "f32.sub".parseInstruction()
        var resultContext: ExecutionContext

        // NaNs.

        resultContext = instruction.execute(
            executionContextWithOpStack(Float.NaN.toValue(), 1f.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Float.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(1f.toValue(), Float.NaN.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Float.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Float.NaN.toValue(), Float.NaN.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Float.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-Float.NaN).toValue(), 1f.toValue())
        )
        assertThat(resultContext).hasOpStackContaining((-Float.NaN).toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(1f.toValue(), (-Float.NaN).toValue())
        )
        assertThat(resultContext).hasOpStackContaining((-Float.NaN).toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-Float.NaN).toValue(), (-Float.NaN).toValue())
        )
        assertThat(resultContext).hasOpStackContaining((-Float.NaN).toValue())

        // Infinites

        resultContext = instruction.execute(
            executionContextWithOpStack(
                Float.POSITIVE_INFINITY.toValue(),
                Float.NEGATIVE_INFINITY.toValue()
            )
        )
        assertThat(resultContext).hasOpStackContaining(Float.POSITIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(
                Float.NEGATIVE_INFINITY.toValue(),
                Float.POSITIVE_INFINITY.toValue()
            )
        )
        assertThat(resultContext).hasOpStackContaining(Float.NEGATIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(
                Float.POSITIVE_INFINITY.toValue(),
                Float.POSITIVE_INFINITY.toValue()
            )
        )
        assertThat(resultContext).hasOpStackContaining(Float.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(
                Float.NEGATIVE_INFINITY.toValue(),
                Float.NEGATIVE_INFINITY.toValue()
            )
        )
        assertThat(resultContext).hasOpStackContaining(Float.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(
                Float.POSITIVE_INFINITY.toValue(),
                1f.toValue()
            )
        )
        assertThat(resultContext).hasOpStackContaining(Float.POSITIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(
                1f.toValue(),
                Float.POSITIVE_INFINITY.toValue()
            )
        )
        assertThat(resultContext).hasOpStackContaining(Float.NEGATIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(
                Float.NEGATIVE_INFINITY.toValue(),
                1f.toValue()
            )
        )
        assertThat(resultContext).hasOpStackContaining(Float.NEGATIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(
                1f.toValue(),
                Float.NEGATIVE_INFINITY.toValue()
            )
        )
        assertThat(resultContext).hasOpStackContaining(Float.POSITIVE_INFINITY.toValue())

        // Zeroes

        listOf(
            TestCase(0f, 0f, (-0f)),
            TestCase((-0f), (-0f), 0f),
            TestCase(0f, 0f, 0f),
            TestCase(0f, (-0f), (-0f)),
            TestCase((-42f), 0f, 42f),
            TestCase((-42f), (-0f), 42f),
            TestCase(42f, 42f, 0f),
            TestCase(42f, 42f, (-0f))
        ).forEach { it.check("f32.sub") }

        // Regular

        listOf(
            TestCase(0f, 42f, 42f),
            TestCase(0f, (-42f), (-42f)),
            TestCase(42f - 12f, 42f, 12f)
        ).forEach { it.check("f32.sub") }
    }

    @Test
    fun f32Mul() = parser.with {
        val instruction = "f32.mul".parseInstruction()
        var resultContext: ExecutionContext

        // NaNs.

        resultContext = instruction.execute(
            executionContextWithOpStack(Float.NaN.toValue(), 1f.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Float.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(1f.toValue(), Float.NaN.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Float.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Float.NaN.toValue(), Float.NaN.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Float.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-Float.NaN).toValue(), 1f.toValue())
        )
        assertThat(resultContext).hasOpStackContaining((-Float.NaN).toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(1f.toValue(), (-Float.NaN).toValue())
        )
        assertThat(resultContext).hasOpStackContaining((-Float.NaN).toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-Float.NaN).toValue(), (-Float.NaN).toValue())
        )
        assertThat(resultContext).hasOpStackContaining((-Float.NaN).toValue())

        // Infinites

        resultContext = instruction.execute(
            executionContextWithOpStack(0f.toValue(), Float.POSITIVE_INFINITY.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Float.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(0f.toValue(), Float.NEGATIVE_INFINITY.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Float.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-0f).toValue(), Float.POSITIVE_INFINITY.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Float.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-0f).toValue(), Float.NEGATIVE_INFINITY.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Float.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Float.POSITIVE_INFINITY.toValue(), 0f.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Float.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Float.NEGATIVE_INFINITY.toValue(), 0f.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Float.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Float.POSITIVE_INFINITY.toValue(), (-0f).toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Float.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Float.NEGATIVE_INFINITY.toValue(), (-0f).toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Float.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(
                Float.NEGATIVE_INFINITY.toValue(),
                Float.NEGATIVE_INFINITY.toValue()
            )
        )
        assertThat(resultContext).hasOpStackContaining(Float.POSITIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(
                Float.POSITIVE_INFINITY.toValue(),
                Float.POSITIVE_INFINITY.toValue()
            )
        )
        assertThat(resultContext).hasOpStackContaining(Float.POSITIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(
                Float.NEGATIVE_INFINITY.toValue(),
                Float.POSITIVE_INFINITY.toValue()
            )
        )
        assertThat(resultContext).hasOpStackContaining(Float.NEGATIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(
                Float.POSITIVE_INFINITY.toValue(),
                Float.NEGATIVE_INFINITY.toValue()
            )
        )
        assertThat(resultContext).hasOpStackContaining(Float.NEGATIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(
                Float.POSITIVE_INFINITY.toValue(),
                Float.NEGATIVE_INFINITY.toValue()
            )
        )
        assertThat(resultContext).hasOpStackContaining(Float.NEGATIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(
                Float.NEGATIVE_INFINITY.toValue(),
                Float.POSITIVE_INFINITY.toValue()
            )
        )
        assertThat(resultContext).hasOpStackContaining(Float.NEGATIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Float.NEGATIVE_INFINITY.toValue(), (-2f).toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Float.POSITIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Float.POSITIVE_INFINITY.toValue(), 2f.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Float.POSITIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-2f).toValue(), Float.NEGATIVE_INFINITY.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Float.POSITIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(2f.toValue(), Float.POSITIVE_INFINITY.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Float.POSITIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Float.NEGATIVE_INFINITY.toValue(), 2f.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Float.NEGATIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Float.POSITIVE_INFINITY.toValue(), (-2f).toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Float.NEGATIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(2f.toValue(), Float.NEGATIVE_INFINITY.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Float.NEGATIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-2f).toValue(), Float.POSITIVE_INFINITY.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Float.NEGATIVE_INFINITY.toValue())

        // Zeroes

        listOf(
            TestCase(0f, 0f, 0f),
            TestCase(0f, -0f, -0f),
            TestCase(-0f, 0f, -0f),
            TestCase(-0f, -0f, 0f)
        ).forEach { it.check("f32.mul") }

        // Regular

        TestCase(2f * 3f, 2f, 3f).check("f32.mul")
    }

    @Test
    fun f32Div() = parser.with {
        val instruction = "f32.div".parseInstruction()
        var resultContext: ExecutionContext

        // NaNs

        resultContext = instruction.execute(
            executionContextWithOpStack(Float.NaN.toValue(), 1f.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Float.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(1f.toValue(), Float.NaN.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Float.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Float.NaN.toValue(), Float.NaN.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Float.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-Float.NaN).toValue(), 1f.toValue())
        )
        assertThat(resultContext).hasOpStackContaining((-Float.NaN).toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(1f.toValue(), (-Float.NaN).toValue())
        )
        assertThat(resultContext).hasOpStackContaining((-Float.NaN).toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-Float.NaN).toValue(), (-Float.NaN).toValue())
        )
        assertThat(resultContext).hasOpStackContaining((-Float.NaN).toValue())

        // Infinites

        resultContext = instruction.execute(
            executionContextWithOpStack(
                Float.NEGATIVE_INFINITY.toValue(),
                Float.NEGATIVE_INFINITY.toValue()
            )
        )
        assertThat(resultContext).hasOpStackContaining(Float.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(
                Float.POSITIVE_INFINITY.toValue(),
                Float.POSITIVE_INFINITY.toValue()
            )
        )
        assertThat(resultContext).hasOpStackContaining(Float.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(
                Float.NEGATIVE_INFINITY.toValue(),
                Float.POSITIVE_INFINITY.toValue()
            )
        )
        assertThat(resultContext).hasOpStackContaining(Float.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(
                Float.POSITIVE_INFINITY.toValue(),
                Float.NEGATIVE_INFINITY.toValue()
            )
        )
        assertThat(resultContext).hasOpStackContaining(Float.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(
                Float.POSITIVE_INFINITY.toValue(),
                Float.NEGATIVE_INFINITY.toValue()
            )
        )
        assertThat(resultContext).hasOpStackContaining(Float.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(
                Float.NEGATIVE_INFINITY.toValue(),
                Float.POSITIVE_INFINITY.toValue()
            )
        )
        assertThat(resultContext).hasOpStackContaining(Float.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Float.POSITIVE_INFINITY.toValue(), 42f.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Float.POSITIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Float.NEGATIVE_INFINITY.toValue(), (-42f).toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Float.POSITIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Float.POSITIVE_INFINITY.toValue(), (-42f).toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Float.NEGATIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Float.NEGATIVE_INFINITY.toValue(), 42f.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Float.NEGATIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(42f.toValue(), Float.POSITIVE_INFINITY.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0f.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-42f).toValue(), Float.NEGATIVE_INFINITY.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0f.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-42f).toValue(), Float.POSITIVE_INFINITY.toValue())
        )
        assertThat(resultContext).hasOpStackContaining((-0f).toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(42f.toValue(), Float.NEGATIVE_INFINITY.toValue())
        )
        assertThat(resultContext).hasOpStackContaining((-0f).toValue())

        // Zeroes

        resultContext = instruction.execute(
            executionContextWithOpStack(0f.toValue(), 0f.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Float.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-0f).toValue(), (-0f).toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Float.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(0f.toValue(), (-0f).toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Float.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-0f).toValue(), 0f.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Float.NaN.toValue())

        listOf(
            TestCase(0f, 0f, 42f),
            TestCase(0f, -0f, -42f),
            TestCase(-0f, 0f, -42f),
            TestCase(-0f, -0f, 42f)
        ).forEach { it.check("f32.div") }

        resultContext = instruction.execute(
            executionContextWithOpStack(42f.toValue(), 0f.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Float.POSITIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-42f).toValue(), (-0f).toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Float.POSITIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-42f).toValue(), 0f.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Float.NEGATIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(42f.toValue(), (-0f).toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Float.NEGATIVE_INFINITY.toValue())

        // Regular

        resultContext = instruction.execute(
            executionContextWithOpStack(42f.toValue(), 42f.toValue())
        )
        assertThat(resultContext).hasOpStackContaining((42f / 42f).toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(42f.toValue(), 12f.toValue())
        )
        assertThat(resultContext).hasOpStackContaining((42f / 12f).toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(12f.toValue(), 42f.toValue())
        )
        assertThat(resultContext).hasOpStackContaining((12f / 42f).toValue())
    }

    @Test
    fun f32Min() = parser.with {
        val instruction = "f32.min".parseInstruction()
        var resultContext: ExecutionContext

        // NaNs

        resultContext = instruction.execute(
            executionContextWithOpStack(Float.NaN.toValue(), 1f.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Float.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(1f.toValue(), Float.NaN.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Float.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Float.NaN.toValue(), Float.NaN.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Float.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-Float.NaN).toValue(), 1f.toValue())
        )
        assertThat(resultContext).hasOpStackContaining((-Float.NaN).toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(1f.toValue(), (-Float.NaN).toValue())
        )
        assertThat(resultContext).hasOpStackContaining((-Float.NaN).toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-Float.NaN).toValue(), (-Float.NaN).toValue())
        )
        assertThat(resultContext).hasOpStackContaining((-Float.NaN).toValue())

        // Infinites

        resultContext = instruction.execute(
            executionContextWithOpStack(
                Float.NEGATIVE_INFINITY.toValue(),
                Float.NEGATIVE_INFINITY.toValue()
            )
        )
        assertThat(resultContext).hasOpStackContaining(Float.NEGATIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Float.NEGATIVE_INFINITY.toValue(), 42f.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Float.NEGATIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(42f.toValue(), Float.NEGATIVE_INFINITY.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Float.NEGATIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(
                Float.POSITIVE_INFINITY.toValue(),
                Float.POSITIVE_INFINITY.toValue()
            )
        )
        assertThat(resultContext).hasOpStackContaining(Float.POSITIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Float.POSITIVE_INFINITY.toValue(), 42f.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(42f.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(42f.toValue(), Float.POSITIVE_INFINITY.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(42f.toValue())

        // Zeroes

        listOf(
            TestCase(-0f, 0f, -0f),
            TestCase(-0f, -0f, 0f)
        ).forEach { it.check("f32.min") }

        // Regular

        listOf(
            TestCase(-42f, -42f, 42f),
            TestCase(-42f, 42f, -42f)
        ).forEach { it.check("f32.min") }
    }

    @Test
    fun f32Max() = parser.with {
        val instruction = "f32.max".parseInstruction()
        var resultContext: ExecutionContext

        // NaNs

        resultContext = instruction.execute(
            executionContextWithOpStack(Float.NaN.toValue(), 1f.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Float.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(1f.toValue(), Float.NaN.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Float.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Float.NaN.toValue(), Float.NaN.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Float.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-Float.NaN).toValue(), 1f.toValue())
        )
        assertThat(resultContext).hasOpStackContaining((-Float.NaN).toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(1f.toValue(), (-Float.NaN).toValue())
        )
        assertThat(resultContext).hasOpStackContaining((-Float.NaN).toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-Float.NaN).toValue(), (-Float.NaN).toValue())
        )
        assertThat(resultContext).hasOpStackContaining((-Float.NaN).toValue())

        // Infinites

        resultContext = instruction.execute(
            executionContextWithOpStack(
                Float.POSITIVE_INFINITY.toValue(),
                Float.POSITIVE_INFINITY.toValue()
            )
        )
        assertThat(resultContext).hasOpStackContaining(Float.POSITIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Float.POSITIVE_INFINITY.toValue(), 42f.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Float.POSITIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(42f.toValue(), Float.POSITIVE_INFINITY.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Float.POSITIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(
                Float.NEGATIVE_INFINITY.toValue(),
                Float.NEGATIVE_INFINITY.toValue()
            )
        )
        assertThat(resultContext).hasOpStackContaining(Float.NEGATIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Float.NEGATIVE_INFINITY.toValue(), 42f.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(42f.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(42f.toValue(), Float.NEGATIVE_INFINITY.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(42f.toValue())

        // Zeroes

        listOf(
            TestCase(0f, 0f, -0f),
            TestCase(0f, -0f, 0f)
        ).forEach { it.check("f32.max") }

        // Regular

        listOf(
            TestCase(42f, -42f, 42f),
            TestCase(42f, 42f, -42f)
        ).forEach { it.check("f32.max") }
    }

    @Test
    fun f32CopySign() = listOf(
        TestCase(42f, 42f, 1f),
        TestCase(-42f, -42f, -1f),
        TestCase(-42f, 42f, -1f),
        TestCase(42f, -42f, 1f)
    ).forEach { it.check("f32.copysign") }

    @Test
    fun f32Equals() = listOf(
        TestCase(1, 42f, 42f),
        TestCase(0, 42f, 41f),
        TestCase(0, 41f, 42f),
        TestCase(0, Float.NaN, 42f),
        TestCase(0, 42f, Float.NaN),
        TestCase(0, Float.NaN, Float.NaN)
    ).forEach { it.check("f32.eq") }

    @Test
    fun f32NotEquals() = listOf(
        TestCase(0, 42f, 42f),
        TestCase(1, 42f, 41f),
        TestCase(1, 41f, 42f),
        TestCase(1, Float.NaN, 42f),
        TestCase(1, 42f, Float.NaN),
        TestCase(1, Float.NaN, Float.NaN)
    ).forEach { it.check("f32.ne") }

    @Test
    fun f32LessThan() = parser.with {
        val instruction = "f32.lt".parseInstruction()

        var resultContext = instruction.execute(
            executionContextWithOpStack(42f.toValue(), 42f.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(42f.toValue(), 41f.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(41f.toValue(), 42f.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(1.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-41f).toValue(), 42f.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(1.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-42f).toValue(), (-41f).toValue())
        )
        assertThat(resultContext).hasOpStackContaining(1.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Float.NaN.toValue(), 42f.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(42f.toValue(), Float.NaN.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Float.NaN.toValue(), Float.NaN.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        //  Check infinity cases
        resultContext = instruction.execute(
            executionContextWithOpStack(Float.POSITIVE_INFINITY.toValue(), 42f.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Float.NEGATIVE_INFINITY.toValue(), 42f.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(1.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(42f.toValue(), Float.POSITIVE_INFINITY.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(1.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(42f.toValue(), Float.NEGATIVE_INFINITY.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())
    }

    @Test
    fun f32GreaterThan() = parser.with {
        val instruction = "f32.gt".parseInstruction()

        var resultContext = instruction.execute(
            executionContextWithOpStack(42f.toValue(), 42f.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(42f.toValue(), 41f.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(1.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(41f.toValue(), 42f.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-41f).toValue(), 42f.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-42f).toValue(), (-41f).toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Float.NaN.toValue(), 42f.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(42f.toValue(), Float.NaN.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Float.NaN.toValue(), Float.NaN.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        //  Check infinity cases
        resultContext = instruction.execute(
            executionContextWithOpStack(Float.POSITIVE_INFINITY.toValue(), 42f.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(1.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Float.NEGATIVE_INFINITY.toValue(), 42f.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(42f.toValue(), Float.POSITIVE_INFINITY.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(42f.toValue(), Float.NEGATIVE_INFINITY.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(1.toValue())
    }

    @Test
    fun f32LessThanEqualTo() = parser.with {
        val instruction = "f32.le".parseInstruction()

        var resultContext = instruction.execute(
            executionContextWithOpStack(42f.toValue(), 42f.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(1.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(42f.toValue(), 41f.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(41f.toValue(), 42f.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(1.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-41f).toValue(), 42f.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(1.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-42f).toValue(), (-41f).toValue())
        )
        assertThat(resultContext).hasOpStackContaining(1.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Float.NaN.toValue(), 42f.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(42f.toValue(), Float.NaN.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Float.NaN.toValue(), Float.NaN.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        //  Check infinity cases
        resultContext = instruction.execute(
            executionContextWithOpStack(
                Float.POSITIVE_INFINITY.toValue(),
                Float.POSITIVE_INFINITY.toValue()
            )
        )
        assertThat(resultContext).hasOpStackContaining(1.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(
                Float.NEGATIVE_INFINITY.toValue(),
                Float.NEGATIVE_INFINITY.toValue()
            )
        )
        assertThat(resultContext).hasOpStackContaining(1.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Float.POSITIVE_INFINITY.toValue(), 42f.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Float.NEGATIVE_INFINITY.toValue(), 42f.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(1.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(42f.toValue(), Float.POSITIVE_INFINITY.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(1.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(42f.toValue(), Float.NEGATIVE_INFINITY.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())
    }

    @Test
    fun f32GreaterThanEqualTo() = parser.with {
        val instruction = "f32.ge".parseInstruction()

        var resultContext = instruction.execute(
            executionContextWithOpStack(42f.toValue(), 42f.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(1.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(42f.toValue(), 41f.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(1.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(41f.toValue(), 42f.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-41f).toValue(), 42f.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-42f).toValue(), (-41f).toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Float.NaN.toValue(), 42f.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(42f.toValue(), Float.NaN.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Float.NaN.toValue(), Float.NaN.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        //  Check infinity cases
        resultContext = instruction.execute(
            executionContextWithOpStack(
                Float.POSITIVE_INFINITY.toValue(),
                Float.POSITIVE_INFINITY.toValue()
            )
        )
        assertThat(resultContext).hasOpStackContaining(1.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(
                Float.NEGATIVE_INFINITY.toValue(),
                Float.NEGATIVE_INFINITY.toValue()
            )
        )
        assertThat(resultContext).hasOpStackContaining(1.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Float.POSITIVE_INFINITY.toValue(), 42f.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(1.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Float.NEGATIVE_INFINITY.toValue(), 42f.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(42f.toValue(), Float.POSITIVE_INFINITY.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(42f.toValue(), Float.NEGATIVE_INFINITY.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(1.toValue())
    }

    @Test
    fun f64AbsoluteValue() = parser.with {
        val instruction = "f64.abs".parseInstruction()

        var resultContext = instruction.execute(
            executionContextWithOpStack(Double.NEGATIVE_INFINITY.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Double.POSITIVE_INFINITY.toValue())

        resultContext = instruction.execute(executionContextWithOpStack(0.0.toValue()))
        assertThat(resultContext).hasOpStackContaining(0.0.toValue())

        resultContext = instruction.execute(executionContextWithOpStack((-0.0).toValue()))
        assertThat(resultContext).hasOpStackContaining(0.0.toValue())

        resultContext = instruction.execute(executionContextWithOpStack((-1.0).toValue()))
        assertThat(resultContext).hasOpStackContaining(1.0.toValue())

        resultContext = instruction.execute(executionContextWithOpStack(1.0.toValue()))
        assertThat(resultContext).hasOpStackContaining(1.0.toValue())
    }

    @Test
    fun f64Negative() = parser.with {
        val instruction = "f64.neg".parseInstruction()

        var resultContext = instruction.execute(
            executionContextWithOpStack(Double.NEGATIVE_INFINITY.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Double.POSITIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Double.POSITIVE_INFINITY.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Double.NEGATIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Double.NaN.toValue())
        )
        assertThat(resultContext).hasOpStackContaining((-Double.NaN).toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-Double.NaN).toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Double.NaN.toValue())

        resultContext = instruction.execute(executionContextWithOpStack(0.0.toValue()))
        assertThat(resultContext).hasOpStackContaining((-0.0).toValue())

        resultContext = instruction.execute(executionContextWithOpStack((-0.0).toValue()))
        assertThat(resultContext).hasOpStackContaining(0.0.toValue())

        resultContext = instruction.execute(executionContextWithOpStack((-1.0).toValue()))
        assertThat(resultContext).hasOpStackContaining(1.0.toValue())

        resultContext = instruction.execute(executionContextWithOpStack((-1.0).toValue()))
        assertThat(resultContext).hasOpStackContaining(1.0.toValue())
    }

    @Test
    fun f64Ceil() = parser.with {
        val instruction = "f64.ceil".parseInstruction()

        var resultContext = instruction.execute(
            executionContextWithOpStack(Double.NEGATIVE_INFINITY.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Double.NEGATIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Double.POSITIVE_INFINITY.toValue())
        )
        assertThat(resultContext.stacks.operands.height).isEqualTo(1)
        assertThat(resultContext.stacks.operands.pop())
            .isEqualTo(Double.POSITIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Double.NaN.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Double.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-Double.NaN).toValue())
        )
        assertThat(resultContext).hasOpStackContaining((-Double.NaN).toValue())

        resultContext = instruction.execute(executionContextWithOpStack(0.0.toValue()))
        assertThat(resultContext).hasOpStackContaining(0.0.toValue())

        resultContext = instruction.execute(executionContextWithOpStack((-0.0).toValue()))
        assertThat(resultContext).hasOpStackContaining((-0.0).toValue())

        resultContext = instruction.execute(executionContextWithOpStack((-0.1).toValue()))
        assertThat(resultContext).hasOpStackContaining((-0.0).toValue())

        resultContext = instruction.execute(executionContextWithOpStack(0.1.toValue()))
        assertThat(resultContext).hasOpStackContaining(1.0.toValue())
    }

    @Test
    fun f64Floor() = parser.with {
        val instruction = "f64.floor".parseInstruction()

        var resultContext = instruction.execute(
            executionContextWithOpStack(Double.NEGATIVE_INFINITY.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Double.NEGATIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Double.POSITIVE_INFINITY.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Double.POSITIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Double.NaN.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Double.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-Double.NaN).toValue())
        )
        assertThat(resultContext).hasOpStackContaining((-Double.NaN).toValue())

        resultContext = instruction.execute(executionContextWithOpStack(0.0.toValue()))
        assertThat(resultContext).hasOpStackContaining(0.0.toValue())

        resultContext = instruction.execute(executionContextWithOpStack((-0.0).toValue()))
        assertThat(resultContext).hasOpStackContaining((-0.0).toValue())

        resultContext = instruction.execute(executionContextWithOpStack((-0.1).toValue()))
        assertThat(resultContext).hasOpStackContaining((-1.0).toValue())

        resultContext = instruction.execute(executionContextWithOpStack(0.1.toValue()))
        assertThat(resultContext).hasOpStackContaining(0.0.toValue())
    }

    @Test
    fun f64Truncate() = parser.with {
        val instruction = "f64.trunc".parseInstruction()

        var resultContext = instruction.execute(
            executionContextWithOpStack(Double.NEGATIVE_INFINITY.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Double.NEGATIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Double.POSITIVE_INFINITY.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Double.POSITIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Double.NaN.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Double.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-Double.NaN).toValue())
        )
        assertThat(resultContext).hasOpStackContaining((-Double.NaN).toValue())

        resultContext = instruction.execute(executionContextWithOpStack(0.0.toValue()))
        assertThat(resultContext).hasOpStackContaining(0.0.toValue())

        resultContext = instruction.execute(executionContextWithOpStack((-0.0).toValue()))
        assertThat(resultContext).hasOpStackContaining((-0.0).toValue())

        resultContext = instruction.execute(executionContextWithOpStack((-0.1).toValue()))
        assertThat(resultContext).hasOpStackContaining((-0.0).toValue())

        resultContext = instruction.execute(executionContextWithOpStack(0.1.toValue()))
        assertThat(resultContext).hasOpStackContaining(0.0.toValue())

        resultContext = instruction.execute(executionContextWithOpStack(1.1.toValue()))
        assertThat(resultContext).hasOpStackContaining(1.0.toValue())

        resultContext = instruction.execute(executionContextWithOpStack((-1.1).toValue()))
        assertThat(resultContext).hasOpStackContaining((-1.0).toValue())
    }

    @Test
    fun f64Nearest() = parser.with {
        val instruction = "f64.nearest".parseInstruction()

        var resultContext = instruction.execute(
            executionContextWithOpStack(Double.NEGATIVE_INFINITY.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Double.NEGATIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Double.POSITIVE_INFINITY.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Double.POSITIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Double.NaN.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Double.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-Double.NaN).toValue())
        )
        assertThat(resultContext).hasOpStackContaining((-Double.NaN).toValue())

        resultContext = instruction.execute(executionContextWithOpStack(0.0.toValue()))
        assertThat(resultContext).hasOpStackContaining(0.0.toValue())

        resultContext = instruction.execute(executionContextWithOpStack((-0.0).toValue()))
        assertThat(resultContext).hasOpStackContaining((-0.0).toValue())

        resultContext = instruction.execute(executionContextWithOpStack((-0.1).toValue()))
        assertThat(resultContext).hasOpStackContaining((-0.0).toValue())

        resultContext = instruction.execute(executionContextWithOpStack(0.1.toValue()))
        assertThat(resultContext).hasOpStackContaining(0.0.toValue())

        resultContext = instruction.execute(executionContextWithOpStack(1.5.toValue()))
        assertThat(resultContext).hasOpStackContaining(2.0.toValue())

        resultContext = instruction.execute(executionContextWithOpStack((-1.5).toValue()))
        assertThat(resultContext).hasOpStackContaining((-2.0).toValue())
    }

    @Test
    fun f64SquareRoot() = parser.with {
        val instruction = "f64.sqrt".parseInstruction()

        var resultContext = instruction.execute(
            executionContextWithOpStack(Double.NEGATIVE_INFINITY.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Double.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Double.POSITIVE_INFINITY.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Double.POSITIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Double.NaN.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Double.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-Double.NaN).toValue())
        )
        assertThat(resultContext).hasOpStackContaining((-Double.NaN).toValue())

        resultContext = instruction.execute(executionContextWithOpStack(0.0.toValue()))
        assertThat(resultContext).hasOpStackContaining(0.0.toValue())

        resultContext = instruction.execute(executionContextWithOpStack((-0.0).toValue()))
        assertThat(resultContext).hasOpStackContaining((-0.0).toValue())

        resultContext = instruction.execute(executionContextWithOpStack((-0.1).toValue()))
        assertThat(resultContext).hasOpStackContaining((-Double.NaN).toValue())

        resultContext = instruction.execute(executionContextWithOpStack(0.1.toValue()))
        assertThat(resultContext).hasOpStackContaining(sqrt(0.1).toValue())
    }

    @Test
    fun f64Add() = parser.with {
        val instruction = "f64.add".parseInstruction()
        var resultContext: ExecutionContext

        // NaNs.

        resultContext = instruction.execute(
            executionContextWithOpStack(Double.NaN.toValue(), 1.0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Double.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(1.0.toValue(), Double.NaN.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Double.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Double.NaN.toValue(), Double.NaN.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Double.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-Double.NaN).toValue(), 1.0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining((-Double.NaN).toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(1.0.toValue(), (-Double.NaN).toValue())
        )
        assertThat(resultContext).hasOpStackContaining((-Double.NaN).toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-Double.NaN).toValue(), (-Double.NaN).toValue())
        )
        assertThat(resultContext).hasOpStackContaining((-Double.NaN).toValue())

        // Infinites

        resultContext = instruction.execute(
            executionContextWithOpStack(
                Double.POSITIVE_INFINITY.toValue(),
                Double.NEGATIVE_INFINITY.toValue()
            )
        )
        assertThat(resultContext).hasOpStackContaining(Double.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(
                Double.NEGATIVE_INFINITY.toValue(),
                Double.POSITIVE_INFINITY.toValue()
            )
        )
        assertThat(resultContext).hasOpStackContaining(Double.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(
                Double.POSITIVE_INFINITY.toValue(),
                Double.POSITIVE_INFINITY.toValue()
            )
        )
        assertThat(resultContext).hasOpStackContaining(Double.POSITIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(
                Double.NEGATIVE_INFINITY.toValue(),
                Double.NEGATIVE_INFINITY.toValue()
            )
        )
        assertThat(resultContext).hasOpStackContaining(Double.NEGATIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(
                Double.POSITIVE_INFINITY.toValue(),
                1.0.toValue()
            )
        )
        assertThat(resultContext).hasOpStackContaining(Double.POSITIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(
                1.0.toValue(),
                Double.POSITIVE_INFINITY.toValue()
            )
        )
        assertThat(resultContext).hasOpStackContaining(Double.POSITIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(
                Double.NEGATIVE_INFINITY.toValue(),
                1.0.toValue()
            )
        )
        assertThat(resultContext).hasOpStackContaining(Double.NEGATIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(
                1.0.toValue(),
                Double.NEGATIVE_INFINITY.toValue()
            )
        )
        assertThat(resultContext).hasOpStackContaining(Double.NEGATIVE_INFINITY.toValue())

        // Zeroes

        resultContext = instruction.execute(
            executionContextWithOpStack(0.0.toValue(), (-0.0).toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-0.0).toValue(), 0.0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(0.0.toValue(), 0.0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-0.0).toValue(), (-0.0).toValue())
        )
        assertThat(resultContext).hasOpStackContaining((-0.0).toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(0.0.toValue(), 42.0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(42.0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-0.0).toValue(), 42.0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(42.0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(42.0.toValue(), 0.0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(42.0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(42.0.toValue(), (-0.0).toValue())
        )
        assertThat(resultContext).hasOpStackContaining(42.0.toValue())

        // Regular

        resultContext = instruction.execute(
            executionContextWithOpStack(42.0.toValue(), (-42.0).toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(42.0.toValue(), 42.0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining((42.0 + 42.0).toValue())
    }

    @Test
    fun f64Sub() = parser.with {
        val instruction = "f64.sub".parseInstruction()
        var resultContext: ExecutionContext

        // NaNs.

        resultContext = instruction.execute(
            executionContextWithOpStack(Double.NaN.toValue(), 1.0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Double.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(1.0.toValue(), Double.NaN.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Double.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Double.NaN.toValue(), Double.NaN.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Double.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-Double.NaN).toValue(), 1.0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining((-Double.NaN).toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(1.0.toValue(), (-Double.NaN).toValue())
        )
        assertThat(resultContext).hasOpStackContaining((-Double.NaN).toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-Double.NaN).toValue(), (-Double.NaN).toValue())
        )
        assertThat(resultContext).hasOpStackContaining((-Double.NaN).toValue())

        // Infinites

        resultContext = instruction.execute(
            executionContextWithOpStack(
                Double.POSITIVE_INFINITY.toValue(),
                Double.NEGATIVE_INFINITY.toValue()
            )
        )
        assertThat(resultContext).hasOpStackContaining(Double.POSITIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(
                Double.NEGATIVE_INFINITY.toValue(),
                Double.POSITIVE_INFINITY.toValue()
            )
        )
        assertThat(resultContext).hasOpStackContaining(Double.NEGATIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(
                Double.POSITIVE_INFINITY.toValue(),
                Double.POSITIVE_INFINITY.toValue()
            )
        )
        assertThat(resultContext).hasOpStackContaining(Double.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(
                Double.NEGATIVE_INFINITY.toValue(),
                Double.NEGATIVE_INFINITY.toValue()
            )
        )
        assertThat(resultContext).hasOpStackContaining(Double.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(
                Double.POSITIVE_INFINITY.toValue(),
                1.0.toValue()
            )
        )
        assertThat(resultContext).hasOpStackContaining(Double.POSITIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(
                1.0.toValue(),
                Double.POSITIVE_INFINITY.toValue()
            )
        )
        assertThat(resultContext).hasOpStackContaining(Double.NEGATIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(
                Double.NEGATIVE_INFINITY.toValue(),
                1.0.toValue()
            )
        )
        assertThat(resultContext).hasOpStackContaining(Double.NEGATIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(
                1.0.toValue(),
                Double.NEGATIVE_INFINITY.toValue()
            )
        )
        assertThat(resultContext).hasOpStackContaining(Double.POSITIVE_INFINITY.toValue())

        // Zeroes

        resultContext = instruction.execute(
            executionContextWithOpStack(0.0.toValue(), (-0.0).toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-0.0).toValue(), 0.0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining((-0.0).toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(0.0.toValue(), 0.0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-0.0).toValue(), (-0.0).toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(0.0.toValue(), 42.0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining((-42.0).toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-0.0).toValue(), 42.0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining((-42.0).toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(42.0.toValue(), 0.0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(42.0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(42.0.toValue(), (-0.0).toValue())
        )
        assertThat(resultContext).hasOpStackContaining(42.0.toValue())

        // Regular

        resultContext = instruction.execute(
            executionContextWithOpStack(42.0.toValue(), 42.0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-42.0).toValue(), (-42.0).toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(42.0.toValue(), 12.0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining((42.0 - 12.0).toValue())
    }

    @Test
    fun f64Mul() = parser.with {
        val instruction = "f64.mul".parseInstruction()
        var resultContext: ExecutionContext

        // NaNs.

        resultContext = instruction.execute(
            executionContextWithOpStack(Double.NaN.toValue(), 1.0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Double.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(1.0.toValue(), Double.NaN.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Double.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Double.NaN.toValue(), Double.NaN.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Double.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-Double.NaN).toValue(), 1.0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining((-Double.NaN).toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(1.0.toValue(), (-Double.NaN).toValue())
        )
        assertThat(resultContext).hasOpStackContaining((-Double.NaN).toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-Double.NaN).toValue(), (-Double.NaN).toValue())
        )
        assertThat(resultContext).hasOpStackContaining((-Double.NaN).toValue())

        // Infinites

        resultContext = instruction.execute(
            executionContextWithOpStack(0.0.toValue(), Double.POSITIVE_INFINITY.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Double.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(0.0.toValue(), Double.NEGATIVE_INFINITY.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Double.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-0.0).toValue(), Double.POSITIVE_INFINITY.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Double.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-0.0).toValue(), Double.NEGATIVE_INFINITY.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Double.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Double.POSITIVE_INFINITY.toValue(), 0.0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Double.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Double.NEGATIVE_INFINITY.toValue(), 0.0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Double.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Double.POSITIVE_INFINITY.toValue(), (-0.0).toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Double.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Double.NEGATIVE_INFINITY.toValue(), (-0.0).toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Double.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(
                Double.NEGATIVE_INFINITY.toValue(),
                Double.NEGATIVE_INFINITY.toValue()
            )
        )
        assertThat(resultContext).hasOpStackContaining(Double.POSITIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(
                Double.POSITIVE_INFINITY.toValue(),
                Double.POSITIVE_INFINITY.toValue()
            )
        )
        assertThat(resultContext).hasOpStackContaining(Double.POSITIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(
                Double.NEGATIVE_INFINITY.toValue(),
                Double.POSITIVE_INFINITY.toValue()
            )
        )
        assertThat(resultContext).hasOpStackContaining(Double.NEGATIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(
                Double.POSITIVE_INFINITY.toValue(),
                Double.NEGATIVE_INFINITY.toValue()
            )
        )
        assertThat(resultContext).hasOpStackContaining(Double.NEGATIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(
                Double.POSITIVE_INFINITY.toValue(),
                Double.NEGATIVE_INFINITY.toValue()
            )
        )
        assertThat(resultContext).hasOpStackContaining(Double.NEGATIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(
                Double.NEGATIVE_INFINITY.toValue(),
                Double.POSITIVE_INFINITY.toValue()
            )
        )
        assertThat(resultContext).hasOpStackContaining(Double.NEGATIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Double.NEGATIVE_INFINITY.toValue(), (-2.0).toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Double.POSITIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Double.POSITIVE_INFINITY.toValue(), 2.0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Double.POSITIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-2.0).toValue(), Double.NEGATIVE_INFINITY.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Double.POSITIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(2.0.toValue(), Double.POSITIVE_INFINITY.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Double.POSITIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Double.NEGATIVE_INFINITY.toValue(), 2.0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Double.NEGATIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Double.POSITIVE_INFINITY.toValue(), (-2.0).toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Double.NEGATIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(2.0.toValue(), Double.NEGATIVE_INFINITY.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Double.NEGATIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-2.0).toValue(), Double.POSITIVE_INFINITY.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Double.NEGATIVE_INFINITY.toValue())

        // Zeroes

        resultContext = instruction.execute(
            executionContextWithOpStack(0.0.toValue(), 0.0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-0.0).toValue(), (-0.0).toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(0.0.toValue(), (-0.0).toValue())
        )
        assertThat(resultContext).hasOpStackContaining((-0.0).toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-0.0).toValue(), 0.0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining((-0.0).toValue())

        // Regular

        resultContext = instruction.execute(
            executionContextWithOpStack(2.0.toValue(), 3.0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining((2.0 * 3.0).toValue())
    }

    @Test
    fun f64Div() = parser.with {
        val instruction = "f64.div".parseInstruction()
        var resultContext: ExecutionContext

        // NaNs

        resultContext = instruction.execute(
            executionContextWithOpStack(Double.NaN.toValue(), 1.0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Double.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(1.0.toValue(), Double.NaN.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Double.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Double.NaN.toValue(), Double.NaN.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Double.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-Double.NaN).toValue(), 1.0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining((-Double.NaN).toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(1.0.toValue(), (-Double.NaN).toValue())
        )
        assertThat(resultContext).hasOpStackContaining((-Double.NaN).toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-Double.NaN).toValue(), (-Double.NaN).toValue())
        )
        assertThat(resultContext).hasOpStackContaining((-Double.NaN).toValue())

        // Infinites

        resultContext = instruction.execute(
            executionContextWithOpStack(
                Double.NEGATIVE_INFINITY.toValue(),
                Double.NEGATIVE_INFINITY.toValue()
            )
        )
        assertThat(resultContext).hasOpStackContaining(Double.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(
                Double.POSITIVE_INFINITY.toValue(),
                Double.POSITIVE_INFINITY.toValue()
            )
        )
        assertThat(resultContext).hasOpStackContaining(Double.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(
                Double.NEGATIVE_INFINITY.toValue(),
                Double.POSITIVE_INFINITY.toValue()
            )
        )
        assertThat(resultContext).hasOpStackContaining(Double.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(
                Double.POSITIVE_INFINITY.toValue(),
                Double.NEGATIVE_INFINITY.toValue()
            )
        )
        assertThat(resultContext).hasOpStackContaining(Double.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(
                Double.POSITIVE_INFINITY.toValue(),
                Double.NEGATIVE_INFINITY.toValue()
            )
        )
        assertThat(resultContext).hasOpStackContaining(Double.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(
                Double.NEGATIVE_INFINITY.toValue(),
                Double.POSITIVE_INFINITY.toValue()
            )
        )
        assertThat(resultContext).hasOpStackContaining(Double.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Double.POSITIVE_INFINITY.toValue(), 42.0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Double.POSITIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Double.NEGATIVE_INFINITY.toValue(), (-42.0).toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Double.POSITIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Double.POSITIVE_INFINITY.toValue(), (-42.0).toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Double.NEGATIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Double.NEGATIVE_INFINITY.toValue(), 42.0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Double.NEGATIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(42.0.toValue(), Double.POSITIVE_INFINITY.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-42.0).toValue(), Double.NEGATIVE_INFINITY.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-42.0).toValue(), Double.POSITIVE_INFINITY.toValue())
        )
        assertThat(resultContext).hasOpStackContaining((-0.0).toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(42.0.toValue(), Double.NEGATIVE_INFINITY.toValue())
        )
        assertThat(resultContext).hasOpStackContaining((-0.0).toValue())

        // Zeroes

        resultContext = instruction.execute(
            executionContextWithOpStack(0.0.toValue(), 0.0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Double.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-0.0).toValue(), (-0.0).toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Double.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(0.0.toValue(), (-0.0).toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Double.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-0.0).toValue(), 0.0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Double.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(0.0.toValue(), 42.0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-0.0).toValue(), (-42.0).toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(0.0.toValue(), (-42.0).toValue())
        )
        assertThat(resultContext).hasOpStackContaining((-0.0).toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-0.0).toValue(), 42.0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining((-0.0).toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(42.0.toValue(), 0.0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Double.POSITIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-42.0).toValue(), (-0.0).toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Double.POSITIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-42.0).toValue(), 0.0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Double.NEGATIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(42.0.toValue(), (-0.0).toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Double.NEGATIVE_INFINITY.toValue())

        // Regular

        resultContext = instruction.execute(
            executionContextWithOpStack(42.0.toValue(), 42.0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining((42.0 / 42.0).toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(42.0.toValue(), 12.0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining((42.0 / 12.0).toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(12.0.toValue(), 42.0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining((12.0 / 42.0).toValue())
    }

    @Test
    fun f64Min() = parser.with {
        val instruction = "f64.min".parseInstruction()
        var resultContext: ExecutionContext

        // NaNs

        resultContext = instruction.execute(
            executionContextWithOpStack(Double.NaN.toValue(), 1.0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Double.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(1.0.toValue(), Double.NaN.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Double.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Double.NaN.toValue(), Double.NaN.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Double.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-Double.NaN).toValue(), 1.0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining((-Double.NaN).toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(1.0.toValue(), (-Double.NaN).toValue())
        )
        assertThat(resultContext).hasOpStackContaining((-Double.NaN).toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-Double.NaN).toValue(), (-Double.NaN).toValue())
        )
        assertThat(resultContext).hasOpStackContaining((-Double.NaN).toValue())

        // Infinites

        resultContext = instruction.execute(
            executionContextWithOpStack(
                Double.NEGATIVE_INFINITY.toValue(),
                Double.NEGATIVE_INFINITY.toValue()
            )
        )
        assertThat(resultContext).hasOpStackContaining(Double.NEGATIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Double.NEGATIVE_INFINITY.toValue(), 42.0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Double.NEGATIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(42.0.toValue(), Double.NEGATIVE_INFINITY.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Double.NEGATIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(
                Double.POSITIVE_INFINITY.toValue(),
                Double.POSITIVE_INFINITY.toValue()
            )
        )
        assertThat(resultContext).hasOpStackContaining(Double.POSITIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Double.POSITIVE_INFINITY.toValue(), 42.0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(42.0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(42.0.toValue(), Double.POSITIVE_INFINITY.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(42.0.toValue())

        // Zeroes

        resultContext = instruction.execute(
            executionContextWithOpStack(0.0.toValue(), (-0.0).toValue())
        )
        assertThat(resultContext).hasOpStackContaining((-0.0).toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-0.0).toValue(), 0.0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining((-0.0).toValue())

        // Regular

        resultContext = instruction.execute(
            executionContextWithOpStack((-42.0).toValue(), 42.0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining((-42.0).toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(42.0.toValue(), (-42.0).toValue())
        )
        assertThat(resultContext).hasOpStackContaining((-42.0).toValue())
    }

    @Test
    fun f64Max() = parser.with {
        val instruction = "f64.max".parseInstruction()
        var resultContext: ExecutionContext

        // NaNs

        resultContext = instruction.execute(
            executionContextWithOpStack(Double.NaN.toValue(), 1.0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Double.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(1.0.toValue(), Double.NaN.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Double.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Double.NaN.toValue(), Double.NaN.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Double.NaN.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-Double.NaN).toValue(), 1.0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining((-Double.NaN).toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(1.0.toValue(), (-Double.NaN).toValue())
        )
        assertThat(resultContext).hasOpStackContaining((-Double.NaN).toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-Double.NaN).toValue(), (-Double.NaN).toValue())
        )
        assertThat(resultContext).hasOpStackContaining((-Double.NaN).toValue())

        // Infinites

        resultContext = instruction.execute(
            executionContextWithOpStack(
                Double.POSITIVE_INFINITY.toValue(),
                Double.POSITIVE_INFINITY.toValue()
            )
        )
        assertThat(resultContext).hasOpStackContaining(Double.POSITIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Double.POSITIVE_INFINITY.toValue(), 42.0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Double.POSITIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(42.0.toValue(), Double.POSITIVE_INFINITY.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(Double.POSITIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(
                Double.NEGATIVE_INFINITY.toValue(),
                Double.NEGATIVE_INFINITY.toValue()
            )
        )
        assertThat(resultContext).hasOpStackContaining(Double.NEGATIVE_INFINITY.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Double.NEGATIVE_INFINITY.toValue(), 42.0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(42.0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(42.0.toValue(), Double.NEGATIVE_INFINITY.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(42.0.toValue())

        // Zeroes

        resultContext = instruction.execute(
            executionContextWithOpStack(0.0.toValue(), (-0.0).toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-0.0).toValue(), 0.0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.0.toValue())

        // Regular

        resultContext = instruction.execute(
            executionContextWithOpStack((-42.0).toValue(), 42.0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(42.0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(42.0.toValue(), (-42.0).toValue())
        )
        assertThat(resultContext).hasOpStackContaining(42.0.toValue())
    }

    @Test
    fun f64CopySign() = listOf(
        TestCase(42.0, 42.0, 1.0),
        TestCase(-42.0, -42.0, -1.0),
        TestCase(-42.0, 42.0, -1.0),
        TestCase(42.0, -42.0, 1.0)
    ).forEach { it.check("f64.copysign") }

    @Test
    fun f64Equals() = listOf(
        TestCase(1, 42.0, 42.0),
        TestCase(0, 42.0, 41.0),
        TestCase(0, 41.0, 42.0),
        TestCase(0, Double.NaN, 42.0),
        TestCase(0, 42.0, Double.NaN),
        TestCase(0, Double.NaN, Double.NaN)
    ).forEach { it.check("f64.eq") }

    @Test
    fun f64NotEquals() = listOf(
        TestCase(0, 42.0, 42.0),
        TestCase(1, 42.0, 41.0),
        TestCase(1, 41.0, 42.0),
        TestCase(1, Double.NaN, 42.0),
        TestCase(1, 42.0, Double.NaN),
        TestCase(1, Double.NaN, Double.NaN)
    ).forEach { it.check("f64.ne") }

    @Test
    fun f64LessThan() = parser.with {
        val instruction = "f64.lt".parseInstruction()

        var resultContext = instruction.execute(
            executionContextWithOpStack(42.0.toValue(), 42.0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(42.0.toValue(), 41.0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(41.0.toValue(), 42.0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(1.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-41.0).toValue(), 42.0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(1.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-42.0).toValue(), (-41.0).toValue())
        )
        assertThat(resultContext).hasOpStackContaining(1.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Double.NaN.toValue(), 42.0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(42.0.toValue(), Double.NaN.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Double.NaN.toValue(), Double.NaN.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        //  Check infinity cases
        resultContext = instruction.execute(
            executionContextWithOpStack(Double.POSITIVE_INFINITY.toValue(), 42.0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Double.NEGATIVE_INFINITY.toValue(), 42.0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(1.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(42.0.toValue(), Double.POSITIVE_INFINITY.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(1.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(42.0.toValue(), Double.NEGATIVE_INFINITY.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())
    }

    @Test
    fun f64GreaterThan() = parser.with {
        val instruction = "f64.gt".parseInstruction()

        var resultContext = instruction.execute(
            executionContextWithOpStack(42.0.toValue(), 42.0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(42.0.toValue(), 41.0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(1.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(41.0.toValue(), 42.0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-41.0).toValue(), 42.0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-42.0).toValue(), (-41.0).toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Double.NaN.toValue(), 42.0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(42.0.toValue(), Double.NaN.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Double.NaN.toValue(), Double.NaN.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        //  Check infinity cases
        resultContext = instruction.execute(
            executionContextWithOpStack(Double.POSITIVE_INFINITY.toValue(), 42.0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(1.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Double.NEGATIVE_INFINITY.toValue(), 42.0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(42.0.toValue(), Double.POSITIVE_INFINITY.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(42.0.toValue(), Double.NEGATIVE_INFINITY.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(1.toValue())
    }

    @Test
    fun f64LessThanEqualTo() = parser.with {
        val instruction = "f64.le".parseInstruction()

        var resultContext = instruction.execute(
            executionContextWithOpStack(42.0.toValue(), 42.0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(1.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(42.0.toValue(), 41.0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(41.0.toValue(), 42.0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(1.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-41.0).toValue(), 42.0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(1.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-42.0).toValue(), (-41.0).toValue())
        )
        assertThat(resultContext).hasOpStackContaining(1.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Double.NaN.toValue(), 42.0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(42.0.toValue(), Double.NaN.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Double.NaN.toValue(), Double.NaN.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        //  Check infinity cases
        resultContext = instruction.execute(
            executionContextWithOpStack(
                Double.POSITIVE_INFINITY.toValue(),
                Double.POSITIVE_INFINITY.toValue()
            )
        )
        assertThat(resultContext).hasOpStackContaining(1.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(
                Double.NEGATIVE_INFINITY.toValue(),
                Double.NEGATIVE_INFINITY.toValue()
            )
        )
        assertThat(resultContext).hasOpStackContaining(1.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Double.POSITIVE_INFINITY.toValue(), 42.0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Double.NEGATIVE_INFINITY.toValue(), 42.0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(1.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(42.0.toValue(), Double.POSITIVE_INFINITY.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(1.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(42.0.toValue(), Double.NEGATIVE_INFINITY.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())
    }

    @Test
    fun f64GreaterThanEqualTo() = parser.with {
        val instruction = "f64.ge".parseInstruction()

        var resultContext = instruction.execute(
            executionContextWithOpStack(42.0.toValue(), 42.0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(1.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(42.0.toValue(), 41.0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(1.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(41.0.toValue(), 42.0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-41.0).toValue(), 42.0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((-42.0).toValue(), (-41.0).toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Double.NaN.toValue(), 42.0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(42.0.toValue(), Double.NaN.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Double.NaN.toValue(), Double.NaN.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        //  Check infinity cases
        resultContext = instruction.execute(
            executionContextWithOpStack(
                Double.POSITIVE_INFINITY.toValue(),
                Double.POSITIVE_INFINITY.toValue()
            )
        )
        assertThat(resultContext).hasOpStackContaining(1.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(
                Double.NEGATIVE_INFINITY.toValue(),
                Double.NEGATIVE_INFINITY.toValue()
            )
        )
        assertThat(resultContext).hasOpStackContaining(1.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Double.POSITIVE_INFINITY.toValue(), 42.0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(1.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(Double.NEGATIVE_INFINITY.toValue(), 42.0.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(42.0.toValue(), Double.POSITIVE_INFINITY.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack(42.0.toValue(), Double.NEGATIVE_INFINITY.toValue())
        )
        assertThat(resultContext).hasOpStackContaining(1.toValue())
    }

    @Test
    fun i32WrapI64() = parser.with {
        val instruction = "i32.wrap_i64".parseInstruction()
        var resultContext: ExecutionContext

        resultContext = instruction.execute(
            executionContextWithOpStack(0xFFFFFFFFFFFFFFFFuL.toValue())
        )
        assertThat(resultContext)
            .hasOpStackContaining((-1).toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((0xFFFFFFFFuL + 1uL).toValue())
        )
        assertThat(resultContext)
            .hasOpStackContaining(0.toValue())

        resultContext = instruction.execute(
            executionContextWithOpStack((0xFFFFFFFFuL).toValue())
        )
        assertThat(resultContext)
            .hasOpStackContaining((-1).toValue())
    }

    @Test
    fun i32TruncateF32Signed() = parser.with {
        val instruction = "i32.trunc_f32_s".parseInstruction()
        var resultContext: ExecutionContext

        // NaNs

        assertThrows(KWasmRuntimeException::class.java) {
            instruction.execute(executionContextWithOpStack(Float.NaN.toValue()))
        }.also { assertThat(it).hasMessageThat().contains("Cannot truncate NaN") }

        assertThrows(KWasmRuntimeException::class.java) {
            instruction.execute(executionContextWithOpStack((-Float.NaN).toValue()))
        }.also { assertThat(it).hasMessageThat().contains("Cannot truncate NaN") }

        // Infinites

        assertThrows(KWasmRuntimeException::class.java) {
            instruction.execute(executionContextWithOpStack(Float.POSITIVE_INFINITY.toValue()))
        }.also { assertThat(it).hasMessageThat().contains("Cannot truncate Infinity") }

        assertThrows(KWasmRuntimeException::class.java) {
            instruction.execute(executionContextWithOpStack(Float.NEGATIVE_INFINITY.toValue()))
        }.also { assertThat(it).hasMessageThat().contains("Cannot truncate Infinity") }

        // Out of bounds

        assertThrows(KWasmRuntimeException::class.java) {
            instruction.execute(executionContextWithOpStack((-1e10f).toValue()))
        }.also {
            assertThat(it).hasMessageThat().contains("Cannot truncate, magnitude too large for i32")
        }

        assertThrows(KWasmRuntimeException::class.java) {
            instruction.execute(executionContextWithOpStack(1e10f.toValue()))
        }.also {
            assertThat(it).hasMessageThat().contains("Cannot truncate, magnitude too large for i32")
        }

        assertThrows(KWasmRuntimeException::class.java) {
            instruction.execute(executionContextWithOpStack((Int.MAX_VALUE.toFloat() + 1.0f).toValue()))
        }.also {
            assertThat(it).hasMessageThat().contains("Cannot truncate, magnitude too large for i32")
        }

        // Okay

        resultContext = instruction.execute(executionContextWithOpStack(42.2f.toValue()))
        assertThat(resultContext).hasOpStackContaining(42.toValue())
    }

    @Test
    fun i32TruncateF32Unsigned() = parser.with {
        val instruction = "i32.trunc_f32_u".parseInstruction()
        var resultContext: ExecutionContext

        // NaNs

        assertThrows(KWasmRuntimeException::class.java) {
            instruction.execute(executionContextWithOpStack(Float.NaN.toValue()))
        }.also { assertThat(it).hasMessageThat().contains("Cannot truncate NaN") }

        assertThrows(KWasmRuntimeException::class.java) {
            instruction.execute(executionContextWithOpStack((-Float.NaN).toValue()))
        }.also { assertThat(it).hasMessageThat().contains("Cannot truncate NaN") }

        // Infinites

        assertThrows(KWasmRuntimeException::class.java) {
            instruction.execute(executionContextWithOpStack(Float.POSITIVE_INFINITY.toValue()))
        }.also { assertThat(it).hasMessageThat().contains("Cannot truncate Infinity") }

        assertThrows(KWasmRuntimeException::class.java) {
            instruction.execute(executionContextWithOpStack(Float.NEGATIVE_INFINITY.toValue()))
        }.also { assertThat(it).hasMessageThat().contains("Cannot truncate Infinity") }

        // Out of bounds

        assertThrows(KWasmRuntimeException::class.java) {
            instruction.execute(executionContextWithOpStack((-1f).toValue()))
        }.also {
            assertThat(it).hasMessageThat().contains("Cannot truncate negative f32 to unsigned i32")
        }

        assertThrows(KWasmRuntimeException::class.java) {
            instruction.execute(executionContextWithOpStack((0x1000000000uL.toFloat()).toValue()))
        }.also {
            assertThat(it).hasMessageThat().contains("Cannot truncate, magnitude too large for i32")
        }

        // Okay

        resultContext = instruction.execute(executionContextWithOpStack(42.2f.toValue()))
        assertThat(resultContext).hasOpStackContaining(42.toValue())
    }

    @Test
    fun i32TruncateF64Signed() = parser.with {
        val instruction = "i32.trunc_f64_s".parseInstruction()
        var resultContext: ExecutionContext

        // NaNs

        assertThrows(KWasmRuntimeException::class.java) {
            instruction.execute(executionContextWithOpStack(Double.NaN.toValue()))
        }.also { assertThat(it).hasMessageThat().contains("Cannot truncate NaN") }

        assertThrows(KWasmRuntimeException::class.java) {
            instruction.execute(executionContextWithOpStack((-Double.NaN).toValue()))
        }.also { assertThat(it).hasMessageThat().contains("Cannot truncate NaN") }

        // Infinites

        assertThrows(KWasmRuntimeException::class.java) {
            instruction.execute(executionContextWithOpStack(Double.POSITIVE_INFINITY.toValue()))
        }.also { assertThat(it).hasMessageThat().contains("Cannot truncate Infinity") }

        assertThrows(KWasmRuntimeException::class.java) {
            instruction.execute(executionContextWithOpStack(Double.NEGATIVE_INFINITY.toValue()))
        }.also { assertThat(it).hasMessageThat().contains("Cannot truncate Infinity") }

        // Out of bounds

        assertThrows(KWasmRuntimeException::class.java) {
            instruction.execute(executionContextWithOpStack((-1e10).toValue()))
        }.also {
            assertThat(it).hasMessageThat().contains("Cannot truncate, magnitude too large for i32")
        }

        assertThrows(KWasmRuntimeException::class.java) {
            instruction.execute(executionContextWithOpStack(1e10.toValue()))
        }.also {
            assertThat(it).hasMessageThat().contains("Cannot truncate, magnitude too large for i32")
        }

        // Okay

        resultContext = instruction.execute(executionContextWithOpStack(42.2.toValue()))
        assertThat(resultContext).hasOpStackContaining(42.toValue())
    }

    @Test
    fun i32TruncateF64Unsigned() = parser.with {
        val instruction = "i32.trunc_f64_u".parseInstruction()
        var resultContext: ExecutionContext

        // NaNs

        assertThrows(KWasmRuntimeException::class.java) {
            instruction.execute(executionContextWithOpStack(Double.NaN.toValue()))
        }.also { assertThat(it).hasMessageThat().contains("Cannot truncate NaN") }

        assertThrows(KWasmRuntimeException::class.java) {
            instruction.execute(executionContextWithOpStack((-Double.NaN).toValue()))
        }.also { assertThat(it).hasMessageThat().contains("Cannot truncate NaN") }

        // Infinites

        assertThrows(KWasmRuntimeException::class.java) {
            instruction.execute(executionContextWithOpStack(Double.POSITIVE_INFINITY.toValue()))
        }.also { assertThat(it).hasMessageThat().contains("Cannot truncate Infinity") }

        assertThrows(KWasmRuntimeException::class.java) {
            instruction.execute(executionContextWithOpStack(Double.NEGATIVE_INFINITY.toValue()))
        }.also { assertThat(it).hasMessageThat().contains("Cannot truncate Infinity") }

        // Out of bounds

        assertThrows(KWasmRuntimeException::class.java) {
            instruction.execute(executionContextWithOpStack((-1.0).toValue()))
        }.also {
            assertThat(it).hasMessageThat().contains("Cannot truncate negative f64 to unsigned i32")
        }

        assertThrows(KWasmRuntimeException::class.java) {
            instruction.execute(executionContextWithOpStack((0x1000000000uL.toDouble()).toValue()))
        }.also {
            assertThat(it).hasMessageThat().contains("Cannot truncate, magnitude too large for i32")
        }

        // Okay

        resultContext = instruction.execute(executionContextWithOpStack(42.2.toValue()))
        assertThat(resultContext).hasOpStackContaining(42.toValue())
    }

    @Test
    fun i32ReinterpretF32() = listOf(
        TestCase(0x7fc00000u.toInt(), Float.NaN),
        TestCase(0x7f800000u.toInt(), Float.POSITIVE_INFINITY),
        TestCase(0xff800000u.toInt(), Float.NEGATIVE_INFINITY),
        TestCase(1.0f.toRawBits(), 1.0f),
        TestCase((-1.0f).toRawBits(), -1.0f)
    ).forEach { it.check("i32.reinterpret_f32") }

    @Test
    fun i64ExtendI32Signed() = listOf(
        TestCase(-1L, -1),
        TestCase(0L, 0),
        TestCase(10000L, 10000),
        TestCase(-10000L, -10000),
        TestCase(0x7fffffffL, 0x7fffffff),
        TestCase(0xffffffff80000000uL.toLong(), 0x80000000u.toInt())
    ).forEach { it.check("i64.extend_i32_s") }

    @Test
    fun i64ExtendI32Unsigned() = listOf(
        TestCase(0L, 0),
        TestCase(10000L, 10000),
        TestCase(0xffffd8f0L, -10000),
        TestCase(0xffffffffL, -1),
        TestCase(0x7fffffffL, 0x7fffffff),
        TestCase(0x80000000L, 0x80000000u.toInt())
    ).forEach { it.check("i64.extend_i32_u") }

    @Test
    fun i64TruncateF32Signed() = parser.with {
        listOf(
            // NaNs
            KWasmErrorTestCase("Cannot truncate NaN", Float.NaN),
            KWasmErrorTestCase("Cannot truncate NaN", -Float.NaN),
            // Infinites
            KWasmErrorTestCase("Cannot truncate Infinity", Float.POSITIVE_INFINITY),
            KWasmErrorTestCase("Cannot truncate Infinity", Float.NEGATIVE_INFINITY),
            // Out of bounds
            KWasmErrorTestCase("Cannot truncate, magnitude too large for i64", -1e20f),
            KWasmErrorTestCase("Cannot truncate, magnitude too large for i64", 1e20f)
        ).forEach { it.check("i64.trunc_f32_s") }

        // Okay
        TestCase(42L, 42.2f).check("i64.trunc_f32_s")
    }

    @Test
    fun i64TruncateF32Unsigned() {
        listOf(
            // NaNs
            KWasmErrorTestCase("Cannot truncate NaN", Float.NaN),
            KWasmErrorTestCase("Cannot truncate NaN", -Float.NaN),
            // Infinites
            KWasmErrorTestCase("Cannot truncate Infinity", Float.POSITIVE_INFINITY),
            KWasmErrorTestCase("Cannot truncate Infinity", Float.NEGATIVE_INFINITY),
            // Out of bounds
            KWasmErrorTestCase("Cannot truncate negative f32 to unsigned i64", -1f),
            KWasmErrorTestCase("Cannot truncate, magnitude too large for i64", 1e20f)
        ).forEach { it.check("i64.trunc_f32_u") }

        // Okay
        TestCase(42L, 42.2f).check("i64.trunc_f32_u")
    }

    @Test
    fun i64TruncateF64Signed() {
        listOf(
            // NaNs
            KWasmErrorTestCase("Cannot truncate NaN", Double.NaN),
            KWasmErrorTestCase("Cannot truncate NaN", -Double.NaN),
            // Infinites
            KWasmErrorTestCase("Cannot truncate Infinity", Double.POSITIVE_INFINITY),
            KWasmErrorTestCase("Cannot truncate Infinity", Double.NEGATIVE_INFINITY),
            // Out of bounds
            KWasmErrorTestCase("Cannot truncate, magnitude too large for i64", -1e20),
            KWasmErrorTestCase("Cannot truncate, magnitude too large for i64", 1e20)
        ).forEach { it.check("i64.trunc_f64_s") }

        // Okay
        TestCase(42L, 42.2).check("i64.trunc_f64_s")
    }

    @Test
    fun i64TruncateF64Unsigned() {
        listOf(
            // NaNs
            KWasmErrorTestCase("Cannot truncate NaN", Double.NaN),
            KWasmErrorTestCase("Cannot truncate NaN", -Double.NaN),
            // Infinites
            KWasmErrorTestCase("Cannot truncate Infinity", Double.POSITIVE_INFINITY),
            KWasmErrorTestCase("Cannot truncate Infinity", Double.NEGATIVE_INFINITY),
            // Out of bounds
            KWasmErrorTestCase("Cannot truncate negative f64 to unsigned i64", -1.0),
            KWasmErrorTestCase("Cannot truncate, magnitude too large for i64", 1e21)
        ).forEach { it.check("i64.trunc_f64_u") }

        // Okay
        TestCase(42L, 42.2).check("i64.trunc_f64_u")
    }

    @Test
    fun i64ReinterpretF64() = listOf(
        TestCase(0x7ff8000000000000L, Double.NaN),
        TestCase(0x7ff0000000000000L, Double.POSITIVE_INFINITY),
        TestCase(0xfff0000000000000uL.toLong(), Double.NEGATIVE_INFINITY),
        TestCase(1.0.toRawBits(), 1.0),
        TestCase((-1.0).toRawBits(), -1.0)
    ).forEach { it.check("i64.reinterpret_f64") }

    @Test
    fun f32ConvertI32Signed() = listOf(
        TestCase(-1f, -1),
        TestCase(1f, 1),
        TestCase(0f, 0),
        TestCase(Int.MAX_VALUE.toFloat(), Int.MAX_VALUE),
        TestCase(Int.MIN_VALUE.toFloat(), Int.MIN_VALUE),
        TestCase(1234567890f, 1234567890)
    ).forEach { it.check("f32.convert_i32_s") }

    @Test
    fun f32ConvertI32Unsigned() = listOf(
        TestCase(UInt.MAX_VALUE.toFloat(), -1),
        TestCase(1f, 1),
        TestCase(0f, 0),
        TestCase(Int.MAX_VALUE.toFloat(), Int.MAX_VALUE),
        TestCase(Int.MIN_VALUE.toUInt().toFloat(), Int.MIN_VALUE),
        TestCase(1234567890f, 1234567890)
    ).forEach { it.check("f32.convert_i32_u") }

    @Test
    fun f32ConvertI64Signed() = listOf(
        TestCase(-1f, -1L),
        TestCase(1f, 1L),
        TestCase(0f, 0L),
        TestCase(Long.MAX_VALUE.toFloat(), Long.MAX_VALUE),
        TestCase(Long.MIN_VALUE.toFloat(), Long.MIN_VALUE),
        TestCase(12345678901234f, 12345678901234L)
    ).forEach { it.check("f32.convert_i64_s") }

    @Test
    fun f32ConvertI64Unsigned() = listOf(
        TestCase(ULong.MAX_VALUE.toFloat(), -1L),
        TestCase(1f, 1L),
        TestCase(0f, 0L),
        TestCase(Long.MAX_VALUE.toFloat(), Long.MAX_VALUE),
        TestCase(Long.MIN_VALUE.toULong().toFloat(), Long.MIN_VALUE),
        TestCase(12345678901234f, 12345678901234L)
    ).forEach { it.check("f32.convert_i64_u") }

    @Test
    fun f32DemoteF64() = listOf(
        TestCase(Float.NaN, Double.NaN),
        TestCase(-Float.NaN, -Double.NaN),
        TestCase(Float.POSITIVE_INFINITY, Double.POSITIVE_INFINITY),
        TestCase(Float.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY),
        TestCase(-0f, -0.0),
        TestCase(0f, 0.0),
        TestCase(42f, 42.0)
    ).forEach { it.check("f32.demote_f64") }

    @Test
    fun f32ReinterpretI32() = listOf(
        TestCase(0f, 0),
        TestCase(-0f, 0x80000000u.toInt()),
        TestCase(1.4e-45f, 1),
        TestCase(Float.NaN, -1),
        TestCase(1.6535997e-34f, 123456789),
        TestCase(-0f, Int.MIN_VALUE),
        TestCase(Float.POSITIVE_INFINITY, 0x7f800000),
        TestCase(Float.NEGATIVE_INFINITY, 0xff800000u.toInt()),
        TestCase(Float.NaN, 0x7fc00000),
        TestCase(-Float.NaN, 0xffc00000u.toInt()),
        TestCase(Float.NaN, 0x7fa00000),
        TestCase(-Float.NaN, 0xffa00000u.toInt())
    ).forEach { it.check("f32.reinterpret_i32") }

    @Test
    fun f64ConvertI32Signed() = listOf(
        TestCase(-1.0, -1),
        TestCase(1.0, 1),
        TestCase(0.0, 0),
        TestCase(Int.MAX_VALUE.toDouble(), Int.MAX_VALUE),
        TestCase(Int.MIN_VALUE.toDouble(), Int.MIN_VALUE),
        TestCase(1234567890.0, 1234567890)
    ).forEach { it.check("f64.convert_i32_s") }

    @Test
    fun f64ConvertI32Unsigned() = listOf(
        TestCase(UInt.MAX_VALUE.toDouble(), -1),
        TestCase(1.0, 1),
        TestCase(0.0, 0),
        TestCase(Int.MAX_VALUE.toDouble(), Int.MAX_VALUE),
        TestCase(Int.MIN_VALUE.toUInt().toDouble(), Int.MIN_VALUE),
        TestCase(1234567890.0, 1234567890)
    ).forEach { it.check("f64.convert_i32_u") }

    @Test
    fun f64ConvertI64Signed() = listOf(
        TestCase(-1.0, -1L),
        TestCase(1.0, 1L),
        TestCase(0.0, 0L),
        TestCase(Long.MAX_VALUE.toDouble(), Long.MAX_VALUE),
        TestCase(Long.MIN_VALUE.toDouble(), Long.MIN_VALUE),
        TestCase(12345678901234.0, 12345678901234L)
    ).forEach { it.check("f64.convert_i64_s") }

    @Test
    fun f64ConvertI64Unsigned() = listOf(
        TestCase(ULong.MAX_VALUE.toDouble(), -1L),
        TestCase(1.0, 1L),
        TestCase(0.0, 0L),
        TestCase(Long.MAX_VALUE.toDouble(), Long.MAX_VALUE),
        TestCase(Long.MIN_VALUE.toULong().toDouble(), Long.MIN_VALUE),
        TestCase(12345678901234.0, 12345678901234L)
    ).forEach { it.check("f64.convert_i64_u") }

    @Test
    fun f64PromoteF32() = listOf(
        TestCase(Double.POSITIVE_INFINITY, Float.POSITIVE_INFINITY),
        TestCase(Double.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY),
        TestCase(Double.NaN, Float.NaN),
        TestCase(-Double.NaN, -Float.NaN),
        TestCase(-0.0, -0f),
        TestCase(0.0, 0f),
        TestCase(42.0, 42f)
    ).forEach { it.check("f64.promote_f32") }

    @Test
    fun f64ReinterpretI64() = listOf(
        TestCase(0.0, 0L),
        TestCase(Double.MIN_VALUE, 1L),
        TestCase(-Double.NaN, -1L),
        TestCase(-0.0, 0x8000000000000000uL.toLong()),
        TestCase(6.09957582e-315, 1234567890L),
        TestCase(Double.POSITIVE_INFINITY, 0x7ff0000000000000L),
        TestCase(Double.NEGATIVE_INFINITY, 0xfff0000000000000uL.toLong()),
        TestCase(Double.NaN, 0x7ff8000000000000L),
        TestCase(-Double.NaN, 0xfff8000000000000uL.toLong()),
        TestCase(Double.NaN, 0x7ff4000000000000L),
        TestCase(-Double.NaN, 0xfff4000000000000uL.toLong())
    ).forEach { it.check("f64.reinterpret_i64") }

    @Test
    fun i32Extend8Signed() = listOf(
        TestCase(0, 0),
        TestCase(1, 1),
        TestCase(-1, 0xFF),
        TestCase(-2, 0xFE),
        TestCase(Byte.MIN_VALUE.toInt(), 0x80)
    ).forEach { it.check("i32.extend8_s") }

    @Test
    fun i32Extend16Signed() = listOf(
        TestCase(0, 0),
        TestCase(1, 1),
        TestCase(0xFF, 0xFF),
        TestCase(-1, 0xFFFF),
        TestCase(-2, 0xFFFE),
        TestCase(Short.MIN_VALUE.toInt(), 0x8000)
    ).forEach { it.check("i32.extend16_s") }

    @Test
    fun i64Extend8Signed() = listOf(
        TestCase(0L, 0L),
        TestCase(1L, 1L),
        TestCase(-1L, 0xFFL),
        TestCase(-2L, 0xFEL),
        TestCase(Byte.MIN_VALUE.toLong(), 0x80L)
    ).forEach { it.check("i64.extend8_s") }

    @Test
    fun i64Extend16Signed() = listOf(
        TestCase(0L, 0L),
        TestCase(1L, 1L),
        TestCase(0xFFL, 0xFFL),
        TestCase(-1L, 0xFFFFL),
        TestCase(-2L, 0xFFFEL),
        TestCase(Short.MIN_VALUE.toLong(), 0x8000L)
    ).forEach { it.check("i64.extend16_s") }

    @Test
    fun i64Extend32Signed() = listOf(
        TestCase(0L, 0L),
        TestCase(1L, 1L),
        TestCase(0xFFL, 0xFFL),
        TestCase(0xFFFFL, 0xFFFFL),
        TestCase(0xFFFFFFL, 0xFFFFFFL),
        TestCase(-1L, 0xFFFFFFFFL),
        TestCase(-2L, 0xFFFFFFFEL),
        TestCase(Int.MIN_VALUE.toLong(), 0x80000000L)
    ).forEach { it.check("i64.extend32_s") }

    @Test
    fun i32TruncateSaturatedF32Signed() = listOf(
        TestCase(0, Float.NaN),
        TestCase(0, -Float.NaN),
        TestCase(Int.MIN_VALUE, Float.NEGATIVE_INFINITY),
        TestCase(Int.MAX_VALUE, Float.POSITIVE_INFINITY),
        TestCase(Int.MIN_VALUE, -5e9f),
        TestCase(Int.MAX_VALUE, 5e9f),
        TestCase(-1, -1.0f),
        TestCase(1, 1.0f),
        TestCase(0, 0.0f),
        TestCase(0, -0.0f),
        TestCase(1337, 1337.424333f),
        TestCase(-1337, -1337.424333f),
    ).forEach { it.check("i32.trunc_sat_f32_s") }

    @Test
    fun i32TruncateSaturatedF32Unsigned() = listOf(
        TestCase(0, Float.NaN),
        TestCase(0, -Float.NaN),
        TestCase(0, Float.NEGATIVE_INFINITY),
        TestCase(UInt.MAX_VALUE.toInt(), Float.POSITIVE_INFINITY),
        TestCase(0, -5e9f),
        TestCase(UInt.MAX_VALUE.toInt(), 5e9f),
        TestCase(0, -1.0f),
        TestCase(1, 1.0f),
        TestCase(0, 0.0f),
        TestCase(0, -0.0f),
        TestCase(1337, 1337.424333f),
        TestCase(0, -1337.424333f),
    ).forEach { it.check("i32.trunc_sat_f32_u") }

    @Test
    fun i32TruncateSaturatedF64Signed() = listOf(
        TestCase(0, Double.NaN),
        TestCase(0, -Double.NaN),
        TestCase(Int.MIN_VALUE, Double.NEGATIVE_INFINITY),
        TestCase(Int.MAX_VALUE, Double.POSITIVE_INFINITY),
        TestCase(Int.MIN_VALUE, -5e9),
        TestCase(Int.MAX_VALUE, 5e9),
        TestCase(-1, -1.0),
        TestCase(1, 1.0),
        TestCase(0, 0.0),
        TestCase(0, -0.0),
        TestCase(1337, 1337.424333),
        TestCase(-1337, -1337.424333),
    ).forEach { it.check("i32.trunc_sat_f64_s") }

    @Test
    fun i32TruncateSaturatedF64Unsigned() = listOf(
        TestCase(0, Double.NaN),
        TestCase(0, -Double.NaN),
        TestCase(0, Double.NEGATIVE_INFINITY),
        TestCase(UInt.MAX_VALUE.toInt(), Double.POSITIVE_INFINITY),
        TestCase(0, -5e9),
        TestCase(UInt.MAX_VALUE.toInt(), 5e9),
        TestCase(0, -1.0),
        TestCase(1, 1.0),
        TestCase(0, 0.0),
        TestCase(0, -0.0),
        TestCase(1337, 1337.424333),
        TestCase(0, -1337.424333),
    ).forEach { it.check("i32.trunc_sat_f64_u") }

    @Test
    fun i64TruncateSaturatedF32Signed() = listOf(
        TestCase(0L, Float.NaN),
        TestCase(0L, -Float.NaN),
        TestCase(Long.MIN_VALUE, Float.NEGATIVE_INFINITY),
        TestCase(Long.MAX_VALUE, Float.POSITIVE_INFINITY),
        TestCase(Long.MIN_VALUE, -5e20f),
        TestCase(Long.MAX_VALUE, 5e20f),
        TestCase(-1L, -1.0f),
        TestCase(1L, 1.0f),
        TestCase(0L, 0.0f),
        TestCase(0L, -0.0f),
        TestCase(1337L, 1337.424333f),
        TestCase(-1337L, -1337.424333f),
    ).forEach { it.check("i64.trunc_sat_f32_s") }

    @Test
    fun i64TruncateSaturatedF32Unsigned() = listOf(
        TestCase(0L, Float.NaN),
        TestCase(0L, -Float.NaN),
        TestCase(0L, Float.NEGATIVE_INFINITY),
        TestCase(ULong.MAX_VALUE.toLong(), Float.POSITIVE_INFINITY),
        TestCase(0L, -5e20f),
        TestCase(ULong.MAX_VALUE.toLong(), 5e20f),
        TestCase(0L, -1.0f),
        TestCase(1L, 1.0f),
        TestCase(0L, 0.0f),
        TestCase(0L, -0.0f),
        TestCase(1337L, 1337.424333f),
        TestCase(0L, -1337.424333f),
    ).forEach { it.check("i64.trunc_sat_f32_u") }

    @Test
    fun i64TruncateSaturatedF64Signed() = listOf(
        TestCase(0L, Double.NaN),
        TestCase(0L, -Double.NaN),
        TestCase(Long.MIN_VALUE, Double.NEGATIVE_INFINITY),
        TestCase(Long.MAX_VALUE, Double.POSITIVE_INFINITY),
        TestCase(Long.MIN_VALUE, -5e90),
        TestCase(Long.MAX_VALUE, 5e90),
        TestCase(-1L, -1.0),
        TestCase(1L, 1.0),
        TestCase(0L, 0.0),
        TestCase(0L, -0.0),
        TestCase(1337L, 1337.424333),
        TestCase(-1337L, -1337.424333),
    ).forEach { it.check("i64.trunc_sat_f64_s") }

    @Test
    fun i64TruncateSaturatedF64Unsigned() = listOf(
        TestCase(0L, Double.NaN),
        TestCase(0L, -Double.NaN),
        TestCase(0L, Double.NEGATIVE_INFINITY),
        TestCase(ULong.MAX_VALUE.toLong(), Double.POSITIVE_INFINITY),
        TestCase(0L, -5e90),
        TestCase(ULong.MAX_VALUE.toLong(), 5e90),
        TestCase(0L, -1.0),
        TestCase(1L, 1.0),
        TestCase(0L, 0.0),
        TestCase(0L, -0.0),
        TestCase(1337L, 1337.424333),
        TestCase(0L, -1337.424333),
    ).forEach { it.check("i64.trunc_sat_f64_u") }

    @Test
    fun unaryOp_throws_ifStackEmpty() {
        assertThrows(IllegalStateException::class.java) {
            unaryOp<IntValue, IntValue>(executionContext) {
                1.toValue()
            }
        }
    }

    @Test
    fun unaryOp_throws_ifStackTopInvalidType() {
        assertThrows(KWasmRuntimeException::class.java) {
            unaryOp<IntValue, IntValue>(executionContextWithOpStack(42L.toValue())) {
                1.toValue()
            }
        }.also {
            assertThat(it).hasMessageThat().contains("Top of stack is invalid type")
        }
    }

    @Test
    fun unaryOp_valid() {
        val result = unaryOp<IntValue, IntValue>(executionContextWithOpStack(13.toValue())) {
            42.toValue()
        }
        assertThat(result).hasOpStackContaining(42.toValue())
    }

    @Test
    fun testOp_throws_ifStackEmpty() {
        assertThrows(IllegalStateException::class.java) {
            testOp<IntValue>(executionContext) {
                false
            }
        }
    }

    @Test
    fun testOp_throws_ifStackTopInvalidType() {
        assertThrows(KWasmRuntimeException::class.java) {
            testOp<FloatValue>(executionContextWithOpStack(42.toValue())) {
                false
            }
        }.also {
            assertThat(it).hasMessageThat().contains("Top of stack is invalid type")
        }
    }

    @Test
    fun testOp_valid() {
        val result = testOp<IntValue>(executionContextWithOpStack(42.toValue())) {
            true
        }
        assertThat(result).hasOpStackContaining(1.toValue())
    }

    @Test
    fun relOp_throws_ifStackDoesntHaveEnoughOperands() {
        assertThrows(IllegalStateException::class.java) {
            relOp<IntValue>(executionContext) { _, _ -> false }
        }
        assertThrows(IllegalStateException::class.java) {
            relOp<IntValue>(executionContextWithOpStack(1.toValue())) { _, _ -> false }
        }
    }

    @Test
    fun relOp_throws_ifStackHasWrongTypes() {
        assertThrows(KWasmRuntimeException::class.java) {
            relOp<IntValue>(executionContextWithOpStack(1.toValue(), 1L.toValue())) { _, _ ->
                false
            }
        }.also {
            assertThat(it).hasMessageThat().contains("RHS is invalid type")
        }
        assertThrows(KWasmRuntimeException::class.java) {
            relOp<IntValue>(executionContextWithOpStack(1L.toValue(), 1.toValue())) { _, _ ->
                false
            }
        }.also {
            assertThat(it).hasMessageThat().contains("LHS is invalid type")
        }
    }

    @Test
    fun relOp_valid() {
        var result = relOp<IntValue>(
            executionContextWithOpStack(42.toValue(), (-42).toValue())
        ) { _, _ -> true }
        assertThat(result).hasOpStackContaining(1.toValue())

        result = relOp<IntValue>(
            executionContextWithOpStack(42.toValue(), (-42).toValue())
        ) { _, _ -> false }
        assertThat(result).hasOpStackContaining(0.toValue())
    }

    @Test
    fun binaryOp_throws_ifStackDoesntHaveEnoughOperands() {
        assertThrows(IllegalStateException::class.java) {
            binaryOp<IntValue>(executionContext) { _, _ -> 1.toValue() }
        }
        assertThrows(IllegalStateException::class.java) {
            binaryOp<IntValue>(executionContextWithOpStack(1.toValue())) { _, _ -> 1.toValue() }
        }
    }

    @Test
    fun binaryOp_throws_ifStackHasWrongTypes() {
        assertThrows(KWasmRuntimeException::class.java) {
            binaryOp<IntValue>(executionContextWithOpStack(1.toValue(), 1L.toValue())) { _, _ ->
                1.toValue()
            }
        }.also {
            assertThat(it).hasMessageThat().contains("RHS is invalid type")
        }
        assertThrows(KWasmRuntimeException::class.java) {
            binaryOp<IntValue>(executionContextWithOpStack(1L.toValue(), 1.toValue())) { _, _ ->
                1.toValue()
            }
        }.also {
            assertThat(it).hasMessageThat().contains("LHS is invalid type")
        }
    }

    @Test
    fun binaryOp_valid() {
        val result = binaryOp<IntValue>(
            executionContextWithOpStack(42.toValue(), (-42).toValue())
        ) { x, y -> (x.value + y.value).toValue() }
        assertThat(result).hasOpStackContaining(0.toValue())
    }

    private fun assertThat(context: ExecutionContext) = object {
        fun hasOpStackContaining(vararg vals: Value<*>) {
            val stack = mutableListOf<Value<*>>()
            while (context.stacks.operands.peek() != null) {
                stack += context.stacks.operands.pop()
            }
            stack.reverse()
            assertThat(stack).containsExactly(*vals).inOrder()
        }
    }

    private inner class TestCase(val expected: Number, vararg val opStack: Number) {
        fun check(source: String) {
            var instruction: Instruction? = null
            parser.with { instruction = source.parseInstruction() }

            val resultContext = instruction!!.execute(
                executionContextWithOpStack(*(opStack.map { it.toValue() }.toTypedArray()))
            )

            val inputStr = opStack.joinToString(prefix = "[", postfix = "]")
            assertWithMessage("$source with input $inputStr should output $expected")
                .thatContext(resultContext)
                .hasOpStackContaining(expected.toValue())
        }
    }

    private inner class KWasmErrorTestCase(
        val expectedMessage: String,
        vararg val opStack: Number
    ) {
        fun check(source: String) {
            var instruction: Instruction? = null
            parser.with { instruction = source.parseInstruction() }

            assertThrows(KWasmRuntimeException::class.java) {
                instruction!!.execute(
                    executionContextWithOpStack(*(opStack.map { it.toValue() }.toTypedArray()))
                )
            }.also { assertThat(it).hasMessageThat().contains(expectedMessage) }
        }
    }

    private fun executionContextWithOpStack(vararg stackVals: Value<*>) =
        executionContext.also {
            stackVals.forEach { stackVal ->
                it.stacks.operands.push(stackVal)
            }
        }
}
