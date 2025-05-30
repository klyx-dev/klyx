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

import kwasm.ast.DeDupeableAstNode

/**
 * Base class for all [Instruction]s dealing with memory.
 *
 * From
 * [the docs](https://webassembly.github.io/spec/core/syntax/instructions.html#memory-instructions):
 *
 * Instructions in this group are concerned with linear memory.
 *
 * Memory is accessed with `load` and `store` instructions for the different value types. They all
 * take a memory immediate [MemArg] that contains an address offset and the expected alignment
 * (expressed as the exponent of a power of 2). Integer loads and stores can optionally specify a
 * storage size that is smaller than the bit width of the respective value type. In the case of
 * loads, a sign extension mode `sx` is then required to select appropriate behavior.
 *
 * The static address offset is added to the dynamic address operand, yielding a 33 bit effective
 * address that is the zero-based index at which the memory is accessed. All values are read and
 * written in little endian byte order. A trap results if any of the accessed memory bytes lies
 * outside the address range implied by the memory’s current size.
 *
 * ```
 *   instr  ::= inn.load memarg
 *              fnn.load memarg
 *              inn.store memarg
 *              fnn.store memarg
 *              inn.load8_sx memarg
 *              inn.load16_sx memarg
 *              i64.load32_sx memarg
 *              inn.store8 memarg
 *              inn.store16 memarg
 *              i64.store32 memarg
 *              memory.size
 *              memory.grow
 * ```
 */
sealed class MemoryInstruction :
    Instruction,
    DeDupeableAstNode<MemoryInstruction> {
    data class LoadInt(
        val bitWidth: Int,
        val storageBits: Int,
        val signed: Boolean,
        val arg: MemArg
    ) : MemoryInstruction() {
        val byteWidth: Int = bitWidth / 8
        val storageBytes: Int = storageBits / 8

        override fun deDupe(): LoadInt = when (this) {
            I32_LOAD -> I32_LOAD
            I64_LOAD -> I64_LOAD
            I32_LOAD8_S -> I32_LOAD8_S
            I32_LOAD8_U -> I32_LOAD8_U
            I32_LOAD16_S -> I32_LOAD16_S
            I32_LOAD16_U -> I32_LOAD16_U
            I64_LOAD8_S -> I64_LOAD8_S
            I64_LOAD8_U -> I64_LOAD8_U
            I64_LOAD16_S -> I64_LOAD16_S
            I64_LOAD16_U -> I64_LOAD16_U
            I64_LOAD32_S -> I64_LOAD32_S
            I64_LOAD32_U -> I64_LOAD32_U
            else -> this
        }

        companion object {
            internal val I32_LOAD =
                LoadInt(
                    32,
                    32,
                    false,
                    MemArg.FOUR
                )
            internal val I64_LOAD =
                LoadInt(
                    64,
                    64,
                    false,
                    MemArg.EIGHT
                )
            internal val I32_LOAD8_S =
                LoadInt(
                    32,
                    8,
                    true,
                    MemArg.ONE
                )
            internal val I32_LOAD8_U =
                LoadInt(
                    32,
                    8,
                    false,
                    MemArg.ONE
                )
            internal val I32_LOAD16_S =
                LoadInt(
                    32,
                    16,
                    true,
                    MemArg.TWO
                )
            internal val I32_LOAD16_U =
                LoadInt(
                    32,
                    16,
                    false,
                    MemArg.TWO
                )
            internal val I64_LOAD8_S =
                LoadInt(
                    64,
                    8,
                    true,
                    MemArg.ONE
                )
            internal val I64_LOAD8_U =
                LoadInt(
                    64,
                    8,
                    false,
                    MemArg.ONE
                )
            internal val I64_LOAD16_S =
                LoadInt(
                    64,
                    16,
                    true,
                    MemArg.TWO
                )
            internal val I64_LOAD16_U =
                LoadInt(
                    64,
                    16,
                    false,
                    MemArg.TWO
                )
            internal val I64_LOAD32_S =
                LoadInt(
                    64,
                    32,
                    true,
                    MemArg.FOUR
                )
            internal val I64_LOAD32_U =
                LoadInt(
                    64,
                    32,
                    false,
                    MemArg.FOUR
                )
        }
    }

    data class StoreInt(
        val bitWidth: Int,
        val storageBits: Int,
        val arg: MemArg
    ) : MemoryInstruction() {
        val byteWidth: Int = bitWidth / 8
        val storageBytes: Int = storageBits / 8

        override fun deDupe(): StoreInt = when (this) {
            I32_STORE -> I32_STORE
            I64_STORE -> I64_STORE
            I32_STORE8 -> I32_STORE8
            I32_STORE16 -> I32_STORE16
            I64_STORE8 -> I64_STORE8
            I64_STORE16 -> I64_STORE16
            I64_STORE32 -> I64_STORE32
            else -> this
        }

        companion object {
            internal val I32_STORE =
                StoreInt(
                    32,
                    32,
                    MemArg.FOUR
                )
            internal val I64_STORE =
                StoreInt(
                    64,
                    64,
                    MemArg.EIGHT
                )
            internal val I32_STORE8 =
                StoreInt(
                    32,
                    8,
                    MemArg.ONE
                )
            internal val I32_STORE16 =
                StoreInt(
                    32,
                    16,
                    MemArg.TWO
                )
            internal val I64_STORE8 =
                StoreInt(
                    64,
                    8,
                    MemArg.ONE
                )
            internal val I64_STORE16 =
                StoreInt(
                    64,
                    16,
                    MemArg.TWO
                )
            internal val I64_STORE32 =
                StoreInt(
                    64,
                    32,
                    MemArg.FOUR
                )
        }
    }

    data class LoadFloat(val bitWidth: Int, val arg: MemArg) : MemoryInstruction() {
        val byteWidth: Int = bitWidth / 8

        override fun deDupe(): LoadFloat = when (this) {
            F32_LOAD -> F32_LOAD
            F64_LOAD -> F64_LOAD
            else -> this
        }

        companion object {
            internal val F32_LOAD =
                LoadFloat(
                    32,
                    MemArg.FOUR
                )
            internal val F64_LOAD =
                LoadFloat(
                    32,
                    MemArg.EIGHT
                )
        }
    }

    data class StoreFloat(val bitWidth: Int, val arg: MemArg) : MemoryInstruction() {
        val byteWidth: Int = bitWidth / 8

        override fun deDupe(): StoreFloat = when (this) {
            F32_STORE -> F32_STORE
            F64_STORE -> F64_STORE
            else -> this
        }

        companion object {
            internal val F32_STORE =
                StoreFloat(
                    32,
                    MemArg.FOUR
                )
            internal val F64_STORE =
                StoreFloat(
                    64,
                    MemArg.EIGHT
                )
        }
    }

    object Size : MemoryInstruction() {
        override fun deDupe(): Size = this
    }

    object Grow : MemoryInstruction() {
        override fun deDupe(): Grow = this
    }
}
