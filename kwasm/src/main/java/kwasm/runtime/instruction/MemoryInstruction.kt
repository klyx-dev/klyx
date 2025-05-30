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

package kwasm.runtime.instruction

import kwasm.KWasmRuntimeException
import kwasm.ast.instruction.MemoryInstruction
import kwasm.runtime.DoubleValue
import kwasm.runtime.ExecutionContext
import kwasm.runtime.FloatValue
import kwasm.runtime.IntValue
import kwasm.runtime.LongValue
import kwasm.runtime.Memory
import kwasm.runtime.toValue
import kotlin.IndexOutOfBoundsException

/**
 * From
 * [the docs](https://webassembly.github.io/spec/core/syntax/instructions.html#memory-instructions):
 *
 * Memory is accessed with `load` and `store` instructions for the different value types. They all
 * take a memory immediate `memarg` that contains an address offset and the expected alignment
 * (expressed as the exponent of a power of 2). Integer loads and stores can optionally specify a
 * storage size that is smaller than the bit width of the respective value type. In the case of
 * loads, a sign extension mode `sx` is then required to select appropriate behavior.
 *
 * The static address offset is added to the dynamic address operand, yielding a 33 bit effective
 * address that is the zero-based index at which the memory is accessed. All values are read and
 * written in little endian byte order. A trap results if any of the accessed memory bytes lies
 * outside the address range implied by the memory’s current size.
 *
 * The `memory.size` instruction returns the current size of a memory. The `memory.grow` instruction
 * grows memory by a given delta and returns the previous size, or `−1` if enough memory cannot be
 * allocated. Both instructions operate in units of page size.
 */
internal fun MemoryInstruction.execute(context: ExecutionContext): ExecutionContext = when (this) {
    is MemoryInstruction.LoadInt -> this.execute(context)
    is MemoryInstruction.StoreInt -> this.execute(context)
    is MemoryInstruction.LoadFloat -> this.execute(context)
    is MemoryInstruction.StoreFloat -> this.execute(context)
    is MemoryInstruction.Size -> this.execute(context)
    is MemoryInstruction.Grow -> this.execute(context)
}.also { it.instructionIndex++ }

/**
 * From [the docs](https://webassembly.github.io/spec/core/exec/instructions.html#and):
 *
 * 1. See [ExecutionContext.getMemory]
 * 1. Assert: due to validation, a value of value type `i32` is on the top of the stack.
 * 1. Pop the value `i32.const i` from the stack.
 * 1. Let `ea` be the integer `i + memarg.offset`.
 * 1. If `N` is not part of the instruction, then:
 *    * Let `N` be the bit width `|t|` of value type `t`.
 * 1. If `ea + N/8` is larger than the length of `mem.data`, then:
 *    * Trap.
 * 1. Let `b*` be the byte sequence `mem.data[ea: N/8]`.
 * 1. If `N` and `sx` are part of the instruction, then:
 *    * Let `n` be the integer for which `bytes_iN(n) = b*`.
 *    * Let `c` be the result of computing `extend_sx_N,_|t|(n)`.
 * 1. Else:
 *    * Let `c` be the constant for which `bytes_t(c) = b*`.
 * 1. Push the value `t.const c` to the stack.
 */
internal fun MemoryInstruction.LoadInt.execute(context: ExecutionContext): ExecutionContext {
    val memory = context.getMemory()
    val memAddress = (context.stacks.operands.pop() as? IntValue)
        ?: throw KWasmRuntimeException("Memory loading requires i32 on top of the stack")
    val eaLong = memAddress.unsignedValue.toLong() + arg.offset.toLong()
    val ea = (memAddress.unsignedValue + arg.offset).toInt()
    if (ea < eaLong) {
        throw KWasmRuntimeException("Cannot load at position $eaLong (out of bounds memory access)")
    }
    val resultValue = try {
        when (byteWidth) {
            // i32 requested.
            4 -> when {
                signed -> memory.readInt(ea, storageBytes).toValue()
                else -> memory.readUInt(ea, storageBytes).toValue()
            }
            // i64 requested.
            8 -> when {
                signed -> memory.readLong(ea, storageBytes).toValue()
                else -> memory.readULong(ea, storageBytes).toValue()
            }
            else ->
                throw KWasmRuntimeException("Illegal byte width: $byteWidth for load instruction")
        }
    } catch (e: IndexOutOfBoundsException) {
        throw KWasmRuntimeException("Cannot load at position $ea (out of bounds memory access)", e)
    }
    context.stacks.operands.push(resultValue)
    return context
}

/**
 * See [MemoryInstruction.LoadInt.execute] for details.
 */
internal fun MemoryInstruction.LoadFloat.execute(context: ExecutionContext): ExecutionContext {
    val memory = context.getMemory()
    val memAddress = (context.stacks.operands.pop() as? IntValue)
        ?: throw KWasmRuntimeException("Memory loading requires i32 on top of the stack")
    val eaLong = memAddress.unsignedValue.toLong() + arg.offset.toLong()
    val ea = (memAddress.unsignedValue + arg.offset).toInt()
    if (ea < eaLong) {
        throw KWasmRuntimeException("Cannot load at position $eaLong (out of bounds memory access)")
    }
    val resultValue = try {
        when (byteWidth) {
            // f32 requested.
            4 -> memory.readFloat(ea).toValue()
            // f64 requested.
            8 -> memory.readDouble(ea).toValue()
            else ->
                throw KWasmRuntimeException("Illegal byte width: $byteWidth for load instruction")
        }
    } catch (e: IndexOutOfBoundsException) {
        throw KWasmRuntimeException("Cannot load at position $ea (out of bounds memory access)", e)
    }
    context.stacks.operands.push(resultValue)
    return context
}

/**
 * From [the docs](https://webassembly.github.io/spec/core/exec/instructions.html#exec-storen):
 *
 * 1. See [ExecutionContext.getMemory].
 * 1. Assert: due to validation, a value of value type `t` is on the top of the stack.
 * 1. Pop the value `t.const c` from the stack.
 * 1. Assert: due to validation, a value of value type `i32` is on the top of the stack.
 * 1. Pop the value `i32.const i` from the stack.
 * 1. Let `ea` be the integer `i + memarg.offset`.
 * 1. If `N` is not part of the instruction, then:
 *    * Let `N` be the bit width `|t|` of value type `t`.
 * 1. If `ea + N/8` is larger than the length of `mem.data`, then:
 *    * Trap.
 * 1. If `N` is part of the instruction, then:
 *    * Let `n` be the result of computing `wrap_|t|,_N(c)`.
 *    * Let `b*` be the byte sequence `bytes_iN(n)`.
 * 1. Else:
 *    * Let `b*` be the byte sequence `bytes_t(c)`.
 * 1. Replace the bytes `mem.data[ea: N/8]` with `b*`.
 */
internal fun MemoryInstruction.StoreInt.execute(context: ExecutionContext): ExecutionContext {
    val memory = context.getMemory()

    val valueToStore = context.stacks.operands.pop()
    val memoryAddress = context.stacks.operands.pop() as? IntValue
        ?: throw KWasmRuntimeException(
            "Memory storing requires an i32 at the second position in the stack"
        )

    val ea = (memoryAddress.unsignedValue + arg.offset).toInt()
    when (byteWidth) {
        // storing an i32.
        4 -> {
            val intValue = valueToStore as? IntValue
                ?: throw KWasmRuntimeException("Illegal type for i32.store")
            try {
                memory.writeUInt(intValue.unsignedValue, ea, storageBytes)
            } catch (e: IndexOutOfBoundsException) {
                throw KWasmRuntimeException("Cannot store at position $ea", e)
            } catch (e: IllegalArgumentException) {
                throw KWasmRuntimeException("Cannot store at position $ea", e)
            }
        }
        // storing an i64.
        8 -> {
            val longValue = valueToStore as? LongValue
                ?: throw KWasmRuntimeException("Illegal type for i64.store")
            try {
                memory.writeULong(longValue.unsignedValue, ea, storageBytes)
            } catch (e: IndexOutOfBoundsException) {
                throw KWasmRuntimeException(
                    "Cannot store at position $ea (out of bounds memory access)",
                    e
                )
            } catch (e: IllegalArgumentException) {
                throw KWasmRuntimeException("Cannot store at position $ea", e)
            }
        }
        else -> throw KWasmRuntimeException("Illegal byte width: $byteWidth for store instruction")
    }

    return context
}

/**
 * See [MemoryInstruction.StoreInt.execute] for details.
 */
internal fun MemoryInstruction.StoreFloat.execute(context: ExecutionContext): ExecutionContext {
    val memory = context.getMemory()

    val valueToStore = context.stacks.operands.pop()
    val memoryAddress = context.stacks.operands.pop() as? IntValue
        ?: throw KWasmRuntimeException(
            "Memory storing requires an i32 at the second position in the stack"
        )

    val ea = (memoryAddress.unsignedValue + arg.offset).toInt()
    try {
        when (byteWidth) {
            // storing an f32.
            4 -> {
                val floatValue = valueToStore as? FloatValue
                    ?: throw KWasmRuntimeException("Illegal type for f32.store")
                memory.writeFloat(floatValue.value, ea)
            }
            // storing an f64.
            8 -> {
                val doubleValue = valueToStore as? DoubleValue
                    ?: throw KWasmRuntimeException("Illegal type for f64.store")
                memory.writeDouble(doubleValue.value, ea)
            }
            else ->
                throw KWasmRuntimeException("Illegal byte width: $byteWidth for store instruction")
        }
    } catch (e: IndexOutOfBoundsException) {
        throw KWasmRuntimeException("Cannot store at position $ea", e)
    } catch (e: IllegalArgumentException) {
        throw KWasmRuntimeException("Cannot store at position $ea", e)
    }

    return context
}

/**
 * From [the docs](https://webassembly.github.io/spec/core/exec/instructions.html#exec-memory-size):
 *
 * 1. See [ExecutionContext.getMemory]
 * 1. Let `sz` be the length of `mem.data` divided by the page size.
 * 1. Push the value `i32.const sz` to the stack.
 */
internal fun MemoryInstruction.Size.execute(context: ExecutionContext): ExecutionContext {
    val memory = context.getMemory()
    context.stacks.operands.push(memory.sizePages.toValue())
    return context
}

/**
 * From [the docs](https://webassembly.github.io/spec/core/exec/instructions.html#exec-memory-grow):
 *
 * 1. See [ExecutionContext.getMemory]
 * 1. Let `sz` be the length of `S.mems\[a]` divided by the page size.
 * 1. Assert: due to validation, a value of value type `i32` is on the top of the stack.
 * 1. Pop the value `i32.const n` from the stack.
 * 1. Let `err` be the `i32` value `2^32 −1`, for which `signed_32(err)` is `−1`.
 * 1. Either, try growing `mem` by `n` pages:
 *    * If it succeeds, push the value `i32.const sz` to the stack.
 *    * Else, push the value `i32.const err` to the stack.
 * 1. Or, push the value `i32.const err` to the stack.
 */
internal fun MemoryInstruction.Grow.execute(context: ExecutionContext): ExecutionContext {
    val memory = context.getMemory()
    val newPages = context.stacks.operands.pop() as? IntValue
        ?: throw KWasmRuntimeException("memory.grow requires i32 on top of the stack")
    val result = memory.growBy(newPages.value)
    context.stacks.operands.push(result.toValue())
    return context
}

/**
 * Performs validation on an [ExecutionContext], as the first step for any [MemoryInstruction].
 *
 * 1. Let `F` be the current frame.
 * 1. Assert: due to validation, `F.module.memaddrs\[0]` exists.
 * 1. Let `a` be the memory address `F.module.memaddrs\[0]`.
 * 1. Assert: due to validation, `S.mems\[a]` exists.
 * 1. Let `mem` be the memory instance `S.mems\[a]`.
 */
internal fun ExecutionContext.getMemory(): Memory {
    val currentFrame = stacks.activations.peek()
        ?: throw KWasmRuntimeException("Cannot access memory from outside a call frame")
    val memoryAddress = currentFrame.module.memoryAddresses.getOrNull(0)
        ?: throw KWasmRuntimeException("No memory address available for module")
    return store.memories.getOrNull(memoryAddress.value)
        ?: throw KWasmRuntimeException("No memory available for store at address: $memoryAddress")
}
