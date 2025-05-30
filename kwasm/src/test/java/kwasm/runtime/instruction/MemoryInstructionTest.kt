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
import kwasm.KWasmRuntimeException
import kwasm.ParseRule
import kwasm.api.ByteBufferMemoryProvider
import kwasm.ast.Identifier
import kwasm.ast.module.Index
import kwasm.ast.module.WasmModule
import kwasm.runtime.ExecutionContext
import kwasm.runtime.ModuleInstance
import kwasm.runtime.Store
import kwasm.runtime.allocate
import kwasm.runtime.stack.Activation
import kwasm.runtime.stack.ActivationStack
import kwasm.runtime.stack.RuntimeStacks
import kwasm.runtime.util.AddressIndex
import kwasm.runtime.util.LocalIndex
import kwasm.runtime.util.TypeIndex
import kwasm.runtime.utils.instructionCases
import kwasm.validation.module.validate
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@Suppress("UNCHECKED_CAST", "EXPERIMENTAL_UNSIGNED_LITERALS", "EXPERIMENTAL_API_USAGE")
@RunWith(JUnit4::class)
class MemoryInstructionTest {
    @get:Rule
    val parser = ParseRule()

    @Test
    fun i32Load() = instructionCases(parser, "i32.load") {
        context = buildMemoryContext(1, 1)
        context.store.memories[0].writeInt(42, 2)

        errorCase(KWasmRuntimeException::class, "Memory loading requires i32", 2L)
        validCase(42, 2)
        (0..3).forEach {
            errorCase(
                KWasmRuntimeException::class,
                "Cannot load at position ${UShort.MAX_VALUE.toInt() + 1 - it}",
                UShort.MAX_VALUE.toInt() + 1 - it
            )
        }
    }

    @Test
    fun i64Load() = instructionCases(parser, "i64.load") {
        context = buildMemoryContext(1, 1)
        context.store.memories[0].writeInt(42, offset = 0)
        context.store.memories[0].writeLong(Long.MAX_VALUE, offset = 8)

        errorCase(KWasmRuntimeException::class, "Memory loading requires i32", 2L)
        validCase(42L, 0)
        validCase(Long.MAX_VALUE, 8)
        validCase(Long.MAX_VALUE ushr 8, 9)
        validCase(Long.MAX_VALUE ushr 16, 10)
        validCase(Long.MAX_VALUE ushr 24, 11)
        validCase(Long.MAX_VALUE ushr 32, 12)
        (0..7).forEach {
            errorCase(
                KWasmRuntimeException::class,
                "Cannot load at position ${UShort.MAX_VALUE.toInt() + 1 - it}",
                UShort.MAX_VALUE.toInt() + 1 - it
            )
        }
    }

    @Test
    fun i32Load8_s() =
        instructionCases(parser, "i32.load8_s") {
            context = buildMemoryContext(1, 1)
            context.store.memories[0].writeInt(42, offset = 0)
            context.store.memories[0].writeUInt(0xFFFFFFFFu, offset = 4)

            errorCase(KWasmRuntimeException::class, "Memory loading requires i32", 2L)
            validCase(42, 0)
            validCase(-1, 4)
            validCase(-1, 5)
            validCase(-1, 6)
            validCase(-1, 7)
            errorCase(
                KWasmRuntimeException::class,
                "Cannot load at position ${UShort.MAX_VALUE.toInt() + 1}",
                UShort.MAX_VALUE.toInt() + 1
            )
        }

    @Test
    fun i32Load8_u() =
        instructionCases(parser, "i32.load8_u") {
            context = buildMemoryContext(1, 1)
            context.store.memories[0].writeInt(42, offset = 0)
            context.store.memories[0].writeUInt(0xFFFFFFFFu, offset = 4)

            errorCase(KWasmRuntimeException::class, "Memory loading requires i32", 2L)
            validCase(42, 0)
            validCase(0xFF, 4)
            validCase(0xFF, 5)
            validCase(0xFF, 6)
            validCase(0xFF, 7)
            errorCase(
                KWasmRuntimeException::class,
                "Cannot load at position ${UShort.MAX_VALUE.toInt() + 1}",
                UShort.MAX_VALUE.toInt() + 1
            )
        }

    @Test
    fun i32Load16_s() =
        instructionCases(parser, "i32.load16_s") {
            context = buildMemoryContext(1, 1)
            context.store.memories[0].writeInt(42, offset = 0)
            context.store.memories[0].writeUInt(0xFFFFFFFFu, offset = 4)

            errorCase(KWasmRuntimeException::class, "Memory loading requires i32", 2L)
            validCase(42, 0)
            validCase(-1, 4)
            validCase(-1, 5)
            validCase(-1, 6)
            validCase(0xFF, 7)
            (0..1).forEach {
                errorCase(
                    KWasmRuntimeException::class,
                    "Cannot load at position ${UShort.MAX_VALUE.toInt() + 1 - it}",
                    UShort.MAX_VALUE.toInt() + 1 - it
                )
            }
        }

    @Test
    fun i32Load16_u() =
        instructionCases(parser, "i32.load16_u") {
            context = buildMemoryContext(1, 1)
            context.store.memories[0].writeInt(42, offset = 0)
            context.store.memories[0].writeUInt(0xFFFFFFFFu, offset = 4)

            errorCase(KWasmRuntimeException::class, "Memory loading requires i32", 2L)
            validCase(42, 0)
            validCase(0xFFFF, 4)
            validCase(0xFFFF, 5)
            validCase(0xFFFF, 6)
            validCase(0xFF, 7)
            (0..1).forEach {
                errorCase(
                    KWasmRuntimeException::class,
                    "Cannot load at position ${UShort.MAX_VALUE.toInt() + 1 - it}",
                    UShort.MAX_VALUE.toInt() + 1 - it
                )
            }
        }

    @Test
    fun i64Load8_s() =
        instructionCases(parser, "i64.load8_s") {
            context = buildMemoryContext(1, 1)
            context.store.memories[0].writeInt(42, offset = 0)
            context.store.memories[0].writeULong(0xFFFFFFFFFFFFFFFFuL, offset = 4)

            errorCase(KWasmRuntimeException::class, "Memory loading requires i32", 2L)
            validCase(42L, 0)
            validCase(-1L, 4)
            validCase(-1L, 5)
            validCase(-1L, 6)
            validCase(-1L, 7)
            validCase(-1L, 8)
            validCase(-1L, 9)
            validCase(-1L, 10)
            validCase(-1L, 11)
            errorCase(
                KWasmRuntimeException::class,
                "Cannot load at position ${UShort.MAX_VALUE.toInt() + 1}",
                UShort.MAX_VALUE.toInt() + 1
            )
        }

    @Test
    fun i64Load8_u() =
        instructionCases(parser, "i64.load8_u") {
            context = buildMemoryContext(1, 1)
            context.store.memories[0].writeInt(42, offset = 0)
            context.store.memories[0].writeULong(0xFFFFFFFFFFFFFFFFuL, offset = 4)

            errorCase(KWasmRuntimeException::class, "Memory loading requires i32", 2L)
            validCase(42L, 0)
            validCase(0xFFL, 4)
            validCase(0xFFL, 5)
            validCase(0xFFL, 6)
            validCase(0xFFL, 7)
            validCase(0xFFL, 8)
            validCase(0xFFL, 9)
            validCase(0xFFL, 10)
            validCase(0xFFL, 11)
            errorCase(
                KWasmRuntimeException::class,
                "Cannot load at position ${UShort.MAX_VALUE.toInt() + 1}",
                UShort.MAX_VALUE.toInt() + 1
            )
        }

    @Test
    fun i64Load16_s() =
        instructionCases(parser, "i64.load16_s") {
            context = buildMemoryContext(1, 1)
            context.store.memories[0].writeInt(42, offset = 0)
            context.store.memories[0].writeULong(0xFFFFFFFFFFFFFFFFuL, offset = 4)

            errorCase(KWasmRuntimeException::class, "Memory loading requires i32", 2L)
            validCase(42L, 0)
            validCase(-1L, 4)
            validCase(-1L, 5)
            validCase(-1L, 6)
            validCase(-1L, 7)
            validCase(-1L, 8)
            validCase(-1L, 9)
            validCase(-1L, 10)
            validCase(0xFFL, 11)
            (0..1).forEach {
                errorCase(
                    KWasmRuntimeException::class,
                    "Cannot load at position ${UShort.MAX_VALUE.toInt() + 1 - it}",
                    UShort.MAX_VALUE.toInt() + 1 - it
                )
            }
        }

    @Test
    fun i64Load16_u() =
        instructionCases(parser, "i64.load16_u") {
            context = buildMemoryContext(1, 1)
            context.store.memories[0].writeInt(42, offset = 0)
            context.store.memories[0].writeULong(0xFFFFFFFFFFFFFFFFuL, offset = 4)

            errorCase(KWasmRuntimeException::class, "Memory loading requires i32", 2L)
            validCase(42L, 0)
            validCase(0xFFFFL, 4)
            validCase(0xFFFFL, 5)
            validCase(0xFFFFL, 6)
            validCase(0xFFFFL, 7)
            validCase(0xFFFFL, 8)
            validCase(0xFFFFL, 9)
            validCase(0xFFFFL, 10)
            validCase(0xFFL, 11)
            (0..1).forEach {
                errorCase(
                    KWasmRuntimeException::class,
                    "Cannot load at position ${UShort.MAX_VALUE.toInt() + 1 - it}",
                    UShort.MAX_VALUE.toInt() + 1 - it
                )
            }
        }

    @Test
    fun i64Load32_s() =
        instructionCases(parser, "i64.load32_s") {
            context = buildMemoryContext(1, 1)
            context.store.memories[0].writeInt(42, offset = 0)
            context.store.memories[0].writeULong(0xFFFFFFFFFFFFFFFFuL, offset = 4)

            errorCase(KWasmRuntimeException::class, "Memory loading requires i32", 2L)
            validCase(42L, 0)
            validCase(-1L, 4)
            validCase(-1L, 5)
            validCase(-1L, 6)
            validCase(-1L, 7)
            validCase(-1L, 8)
            validCase(0xFFFFFFL, 9)
            validCase(0xFFFFL, 10)
            validCase(0xFFL, 11)
            (0..3).forEach {
                errorCase(
                    KWasmRuntimeException::class,
                    "Cannot load at position ${UShort.MAX_VALUE.toInt() + 1 - it}",
                    UShort.MAX_VALUE.toInt() + 1 - it
                )
            }
        }

    @Test
    fun i64Load32_u() =
        instructionCases(parser, "i64.load32_u") {
            context = buildMemoryContext(1, 1)
            context.store.memories[0].writeInt(42, offset = 0)
            context.store.memories[0].writeULong(0xFFFFFFFFFFFFFFFFuL, offset = 4)

            errorCase(KWasmRuntimeException::class, "Memory loading requires i32", 2L)
            validCase(42L, 0)
            validCase(0xFFFFFFFFL, 4)
            validCase(0xFFFFFFFFL, 5)
            validCase(0xFFFFFFFFL, 6)
            validCase(0xFFFFFFFFL, 7)
            validCase(0xFFFFFFFFL, 8)
            validCase(0xFFFFFFL, 9)
            validCase(0xFFFFL, 10)
            validCase(0xFFL, 11)
            (0..3).forEach {
                errorCase(
                    KWasmRuntimeException::class,
                    "Cannot load at position ${UShort.MAX_VALUE.toInt() + 1 - it}",
                    UShort.MAX_VALUE.toInt() + 1 - it
                )
            }
        }

    @Test
    fun f32Load() = instructionCases(parser, "f32.load") {
        context = buildMemoryContext(1, 1)
        context.store.memories[0].writeFloat(42f, offset = 0)
        context.store.memories[0].writeFloat(Float.NaN, offset = 4)
        context.store.memories[0].writeFloat(-Float.NaN, offset = 8)
        context.store.memories[0].writeFloat(Float.POSITIVE_INFINITY, offset = 12)
        context.store.memories[0].writeFloat(Float.NEGATIVE_INFINITY, offset = 16)
        context.store.memories[0].writeFloat(0f, offset = 20)
        context.store.memories[0].writeFloat(-0f, offset = 24)

        errorCase(KWasmRuntimeException::class, "Memory loading requires i32", 2L)
        validCase(42f, 0)
        validCase(Float.NaN, 4)
        validCase(-Float.NaN, 8)
        validCase(Float.POSITIVE_INFINITY, 12)
        validCase(Float.NEGATIVE_INFINITY, 16)
        validCase(0f, 20)
        validCase(-0f, 24)
        (0..3).forEach {
            errorCase(
                KWasmRuntimeException::class,
                "Cannot load at position ${UShort.MAX_VALUE.toInt() + 1 - it}",
                UShort.MAX_VALUE.toInt() + 1 - it
            )
        }
    }

    @Test
    fun f64Load() = instructionCases(parser, "f64.load") {
        context = buildMemoryContext(1, 1)
        context.store.memories[0].writeDouble(42.0, offset = 0)
        context.store.memories[0].writeDouble(Double.NaN, offset = 8)
        context.store.memories[0].writeDouble(-Double.NaN, offset = 16)
        context.store.memories[0].writeDouble(Double.POSITIVE_INFINITY, offset = 24)
        context.store.memories[0].writeDouble(Double.NEGATIVE_INFINITY, offset = 32)
        context.store.memories[0].writeDouble(0.0, offset = 40)
        context.store.memories[0].writeDouble(-0.0, offset = 48)

        errorCase(KWasmRuntimeException::class, "Memory loading requires i32", 2L)
        validCase(42.0, 0)
        validCase(Double.NaN, 8)
        validCase(-Double.NaN, 16)
        validCase(Double.POSITIVE_INFINITY, 24)
        validCase(Double.NEGATIVE_INFINITY, 32)
        validCase(0.0, 40)
        validCase(-0.0, 48)
        (0..7).forEach {
            errorCase(
                KWasmRuntimeException::class,
                "Cannot load at position ${UShort.MAX_VALUE.toInt() + 1 - it}",
                UShort.MAX_VALUE.toInt() + 1 - it
            )
        }
    }

    @Test
    fun i32Store() = instructionCases(parser, "i32.store") {
        context = buildMemoryContext(minPages = 1, maxPages = 1)

        errorCase(KWasmRuntimeException::class, "Memory storing requires an i32", 1f, 0)
        errorCase(KWasmRuntimeException::class, "Illegal type for i32.store", 0, 0.0)
        errorCase(KWasmRuntimeException::class, "Illegal type for i32.store", 0, 0f)
        errorCase(KWasmRuntimeException::class, "Illegal type for i32.store", 0, 0L)

        validVoidCase(0, 42)
        assertThat(context.getMemory().readInt(0)).isEqualTo(42)
        validVoidCase(1, -42)
        assertThat(context.getMemory().readInt(1)).isEqualTo(-42)
        validVoidCase(2, -1)
        assertThat(context.getMemory().readUInt(2)).isEqualTo(0xFFFFFFFFu)
        (0..3).forEach {
            errorCase(
                KWasmRuntimeException::class,
                "Cannot store at position ${UShort.MAX_VALUE.toInt() + 1 - it}",
                UShort.MAX_VALUE.toInt() + 1 - it,
                42
            )
        }
    }

    @Test
    fun i64Store() = instructionCases(parser, "i64.store") {
        context = buildMemoryContext(minPages = 1, maxPages = 1)

        errorCase(KWasmRuntimeException::class, "Memory storing requires an i32", 1f, 0)
        errorCase(KWasmRuntimeException::class, "Illegal type for i64.store", 0, 0.0)
        errorCase(KWasmRuntimeException::class, "Illegal type for i64.store", 0, 0f)
        errorCase(KWasmRuntimeException::class, "Illegal type for i64.store", 0, 0)

        validVoidCase(0, 42L)
        assertThat(context.getMemory().readLong(0)).isEqualTo(42L)
        validVoidCase(1, -42L)
        assertThat(context.getMemory().readLong(1)).isEqualTo(-42L)
        validVoidCase(2, -1L)
        assertThat(context.getMemory().readULong(2)).isEqualTo(0xFFFFFFFFFFFFFFFFuL)
        (0..7).forEach {
            errorCase(
                KWasmRuntimeException::class,
                "Cannot store at position ${UShort.MAX_VALUE.toInt() + 1 - it}",
                UShort.MAX_VALUE.toInt() + 1 - it,
                42L
            )
        }
    }

    @Test
    fun i32Store8() =
        instructionCases(parser, "i32.store8") {
            context = buildMemoryContext(minPages = 1, maxPages = 1)

            errorCase(KWasmRuntimeException::class, "Memory storing requires an i32", 1f, 0)
            errorCase(KWasmRuntimeException::class, "Illegal type for i32.store", 0, 0.0)
            errorCase(KWasmRuntimeException::class, "Illegal type for i32.store", 0, 0f)
            errorCase(KWasmRuntimeException::class, "Illegal type for i32.store", 0, 0L)

            validVoidCase(0, 42)
            assertThat(context.getMemory().readInt(0)).isEqualTo(42)
            validVoidCase(1, 0xABCD)
            assertThat(context.getMemory().readInt(1)).isEqualTo(0xCD)
            errorCase(
                KWasmRuntimeException::class,
                "Cannot store at position ${UShort.MAX_VALUE.toInt() + 1}",
                UShort.MAX_VALUE.toInt() + 1,
                42
            )
        }

    @Test
    fun i32Store16() =
        instructionCases(parser, "i32.store16") {
            context = buildMemoryContext(minPages = 1, maxPages = 1)

            errorCase(KWasmRuntimeException::class, "Memory storing requires an i32", 1f, 0)
            errorCase(KWasmRuntimeException::class, "Illegal type for i32.store", 0, 0.0)
            errorCase(KWasmRuntimeException::class, "Illegal type for i32.store", 0, 0f)
            errorCase(KWasmRuntimeException::class, "Illegal type for i32.store", 0, 0L)

            validVoidCase(0, 42)
            assertThat(context.getMemory().readInt(0)).isEqualTo(42)
            validVoidCase(1, 0xABCD)
            assertThat(context.getMemory().readInt(1)).isEqualTo(0xABCD)
            validVoidCase(2, 0xABCDEF)
            assertThat(context.getMemory().readInt(2)).isEqualTo(0xCDEF)
            (0..1).forEach {
                errorCase(
                    KWasmRuntimeException::class,
                    "Cannot store at position ${UShort.MAX_VALUE.toInt() + 1 - it}",
                    UShort.MAX_VALUE.toInt() + 1 - it,
                    42
                )
            }
        }

    @Test
    fun i64Store8() =
        instructionCases(parser, "i64.store8") {
            context = buildMemoryContext(minPages = 1, maxPages = 1)

            errorCase(KWasmRuntimeException::class, "Memory storing requires an i32", 1f, 0)
            errorCase(KWasmRuntimeException::class, "Illegal type for i64.store", 0, 0.0)
            errorCase(KWasmRuntimeException::class, "Illegal type for i64.store", 0, 0f)
            errorCase(KWasmRuntimeException::class, "Illegal type for i64.store", 0, 0)

            validVoidCase(0, 42L)
            assertThat(context.getMemory().readLong(0)).isEqualTo(42L)
            validVoidCase(1, 0xABCDL)
            assertThat(context.getMemory().readLong(1)).isEqualTo(0xCDL)
            errorCase(
                KWasmRuntimeException::class,
                "Cannot store at position ${UShort.MAX_VALUE.toInt() + 1}",
                UShort.MAX_VALUE.toInt() + 1,
                42L
            )
        }

    @Test
    fun i64Store16() =
        instructionCases(parser, "i64.store16") {
            context = buildMemoryContext(minPages = 1, maxPages = 1)

            errorCase(KWasmRuntimeException::class, "Memory storing requires an i32", 1f, 0)
            errorCase(KWasmRuntimeException::class, "Illegal type for i64.store", 0, 0.0)
            errorCase(KWasmRuntimeException::class, "Illegal type for i64.store", 0, 0f)
            errorCase(KWasmRuntimeException::class, "Illegal type for i64.store", 0, 0)

            validVoidCase(0, 42L)
            assertThat(context.getMemory().readLong(0)).isEqualTo(42L)
            validVoidCase(1, 0xABCDL)
            assertThat(context.getMemory().readLong(1)).isEqualTo(0xABCDL)
            validVoidCase(2, 0xABCDEFL)
            assertThat(context.getMemory().readLong(2)).isEqualTo(0xCDEFL)
            (0..1).forEach {
                errorCase(
                    KWasmRuntimeException::class,
                    "Cannot store at position ${UShort.MAX_VALUE.toInt() + 1 - it}",
                    UShort.MAX_VALUE.toInt() + 1 - it,
                    42L
                )
            }
        }

    @Test
    fun i64Store32() =
        instructionCases(parser, "i64.store32") {
            context = buildMemoryContext(minPages = 1, maxPages = 1)

            errorCase(KWasmRuntimeException::class, "Memory storing requires an i32", 1f, 0)
            errorCase(KWasmRuntimeException::class, "Illegal type for i64.store", 0, 0.0)
            errorCase(KWasmRuntimeException::class, "Illegal type for i64.store", 0, 0f)
            errorCase(KWasmRuntimeException::class, "Illegal type for i64.store", 0, 0)

            validVoidCase(0, 42L)
            assertThat(context.getMemory().readLong(0)).isEqualTo(42L)
            validVoidCase(1, 0xABCDEF01L)
            assertThat(context.getMemory().readLong(1)).isEqualTo(0xABCDEF01L)
            validVoidCase(1, 0x1FFFFFFFFL)
            assertThat(context.getMemory().readLong(1)).isEqualTo(0xFFFFFFFFL)
            (0..3).forEach {
                errorCase(
                    KWasmRuntimeException::class,
                    "Cannot store at position ${UShort.MAX_VALUE.toInt() + 1 - it}",
                    UShort.MAX_VALUE.toInt() + 1 - it,
                    42L
                )
            }
        }

    @Test
    fun f32Store() = instructionCases(parser, "f32.store") {
        context = buildMemoryContext(minPages = 1, maxPages = 1)

        errorCase(KWasmRuntimeException::class, "Memory storing requires an i32", 1f, 0f)
        errorCase(KWasmRuntimeException::class, "Illegal type for f32.store", 0, 0.0)
        errorCase(KWasmRuntimeException::class, "Illegal type for f32.store", 0, 0)
        errorCase(KWasmRuntimeException::class, "Illegal type for f32.store", 0, 0L)

        validVoidCase(0, 42f)
        assertThat(context.getMemory().readFloat(0)).isEqualTo(42f)
        validVoidCase(0, Float.NaN)
        assertThat(context.getMemory().readFloat(0)).isEqualTo(Float.NaN)
        validVoidCase(0, -Float.NaN)
        assertThat(context.getMemory().readFloat(0)).isEqualTo(-Float.NaN)
        validVoidCase(0, Float.POSITIVE_INFINITY)
        assertThat(context.getMemory().readFloat(0)).isEqualTo(Float.POSITIVE_INFINITY)
        validVoidCase(0, Float.NEGATIVE_INFINITY)
        assertThat(context.getMemory().readFloat(0)).isEqualTo(Float.NEGATIVE_INFINITY)
        validVoidCase(0, 0f)
        assertThat(context.getMemory().readFloat(0)).isEqualTo(0f)
        validVoidCase(0, -0f)
        assertThat(context.getMemory().readFloat(0)).isEqualTo(-0f)
        (0..3).forEach {
            errorCase(
                KWasmRuntimeException::class,
                "Cannot store at position ${UShort.MAX_VALUE.toInt() + 1 - it}",
                UShort.MAX_VALUE.toInt() + 1 - it,
                42f
            )
        }
    }

    @Test
    fun f64Store() = instructionCases(parser, "f64.store") {
        context = buildMemoryContext(minPages = 1, maxPages = 1)

        errorCase(KWasmRuntimeException::class, "Memory storing requires an i32", 1f, 0.0)
        errorCase(KWasmRuntimeException::class, "Illegal type for f64.store", 0, 0f)
        errorCase(KWasmRuntimeException::class, "Illegal type for f64.store", 0, 0)
        errorCase(KWasmRuntimeException::class, "Illegal type for f64.store", 0, 0L)

        validVoidCase(0, 42.0)
        assertThat(context.getMemory().readDouble(0)).isEqualTo(42.0)
        validVoidCase(0, Double.NaN)
        assertThat(context.getMemory().readDouble(0)).isEqualTo(Double.NaN)
        validVoidCase(0, -Double.NaN)
        assertThat(context.getMemory().readDouble(0)).isEqualTo(-Double.NaN)
        validVoidCase(0, Double.POSITIVE_INFINITY)
        assertThat(context.getMemory().readDouble(0)).isEqualTo(Double.POSITIVE_INFINITY)
        validVoidCase(0, Double.NEGATIVE_INFINITY)
        assertThat(context.getMemory().readDouble(0)).isEqualTo(Double.NEGATIVE_INFINITY)
        validVoidCase(0, 0.0)
        assertThat(context.getMemory().readDouble(0)).isEqualTo(0.0)
        validVoidCase(0, -0.0)
        assertThat(context.getMemory().readDouble(0)).isEqualTo(-0.0)
        (0..7).forEach {
            errorCase(
                KWasmRuntimeException::class,
                "Cannot store at position ${UShort.MAX_VALUE.toInt() + 1 - it}",
                UShort.MAX_VALUE.toInt() + 1 - it,
                42.0
            )
        }
    }

    @Test
    fun size_returnsSize() =
        instructionCases(parser, "memory.size") {
            context = buildMemoryContext(minPages = 0)
            validCase(0)
            context = buildMemoryContext(minPages = 1)
            validCase(1)
            context = buildMemoryContext(minPages = 20, maxPages = 20)
            validCase(20)
        }

    @Test
    fun grow_returnsError_ifCouldntGrow() =
        instructionCases(parser, "memory.grow") {
            context = buildMemoryContext(maxPages = 1)
            validCase(-1, 2)
            context = buildMemoryContext(maxPages = 1)
            validCase(-1, 20)
        }

    @Test
    fun grow_returnsOldSize_onSuccess() =
        instructionCases(parser, "memory.grow") {
            context = buildMemoryContext()
            validCase(0, 1)
            validCase(1, 1)
            validCase(2, 1)
            validCase(3, 3)
            validCase(6, 0)
        }

    @Test
    fun getMemory_throws_ifNoCallFrame() {
        val context = buildMemoryContext()
        context.stacks.activations.pop()
        assertThrows(KWasmRuntimeException::class.java) {
            context.getMemory()
        }.also {
            assertThat(it).hasMessageThat()
                .contains("Cannot access memory from outside a call frame")
        }
    }

    @Test
    fun getMemory_throws_ifNoMemoryAddressAvailable() {
        val emptyModule = ModuleInstance(
            TypeIndex(),
            AddressIndex(),
            AddressIndex(),
            AddressIndex(),
            AddressIndex(),
            emptyList()
        )
        val context = buildMemoryContext().copy(
            moduleInstance = emptyModule,
            stacks = RuntimeStacks(
                activations = ActivationStack().also {
                    it.push(
                        Activation(
                            Index.ByInt(0) as Index<Identifier.Function>,
                            LocalIndex(),
                            emptyModule
                        )
                    )
                }
            )
        )

        assertThrows(KWasmRuntimeException::class.java) {
            context.getMemory()
        }.also {
            assertThat(it).hasMessageThat().contains("No memory address available for module")
        }
    }

    @Test
    fun getMemory_throws_ifNoMemoryInStore() {
        val context = buildMemoryContext().copy(store = Store())
        assertThrows(KWasmRuntimeException::class.java) {
            context.getMemory()
        }.also {
            assertThat(it).hasMessageThat().contains("No memory available for store at address:")
        }
    }

    private fun buildMemoryContext(maxPages: Int = 10, minPages: Int = 0): ExecutionContext {
        var module: WasmModule? = null

        parser.with {
            module =
                """
                (module
                    (memory $minPages $maxPages)
                )
                """.parseModule()
        }

        val validationContext = module!!.validate()
        val moduleAlloc = module!!.allocate(
            validationContext,
            ByteBufferMemoryProvider(4 * 1024 * 1024)
        )

        return ExecutionContext(
            moduleAlloc.store,
            moduleAlloc.moduleInstance,
            RuntimeStacks(
                activations = ActivationStack().apply {
                    push(
                        Activation(
                            Index.ByInt(0) as Index<Identifier.Function>,
                            LocalIndex(),
                            moduleAlloc.moduleInstance
                        )
                    )
                }
            )
        )
    }
}
