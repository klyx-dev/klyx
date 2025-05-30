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

import kwasm.ast.instruction.MemoryInstruction
import kwasm.format.text.ParseResult
import kwasm.format.text.token.Keyword
import kwasm.format.text.token.Token

/**
 * Attempts to parse a [MemoryInstruction] from the receiving [List] of [Token]s. If none is found
 * at the given start index ([fromIndex]), `null` is returned.
 *
 * From [the docs]():
 *
 * ```
 *   plaininstrI    ::= ‘i32.load’ m:memarg4        => i32.load m
 *                      ‘i64.load’ m:memarg8        => i64.load m
 *                      ‘f32.load’ m:memarg4        => f32.load m
 *                      ‘f64.load’ m:memarg8        => f64.load m
 *                      ‘i32.load8_s’ m:memarg1     => i32.load8_s m
 *                      ‘i32.load8_u’ m:memarg1     => i32.load8_u m
 *                      ‘i32.load16_s’ m:memarg2    => i32.load16_s m
 *                      ‘i32.load16_u’ m:memarg2    => i32.load16_u m
 *                      ‘i64.load8_s’ m:memarg1     => i64.load8_s m
 *                      ‘i64.load8_u’ m:memarg1     => i64.load8_u m
 *                      ‘i64.load16_s’ m:memarg2    => i64.load16_s m
 *                      ‘i64.load16_u’ m:memarg2    => i64.load16_u m
 *                      ‘i64.load32_s’ m:memarg4    => i64.load32_s m
 *                      ‘i64.load32_u’ m:memarg4    => i64.load32_u m
 *                      ‘i32.store’ m:memarg4       => i32.store m
 *                      ‘i64.store’ m:memarg8       => i64.store m
 *                      ‘f32.store’ m:memarg4       => f32.store m
 *                      ‘f64.store’ m:memarg8       => f64.store m
 *                      ‘i32.store8’ m:memarg1      => i32.store8 m
 *                      ‘i32.store16’ m:memarg2     => i32.store16 m
 *                      ‘i64.store8’ m:memarg1      => i64.store8 m
 *                      ‘i64.store16’ m:memarg2     => i64.store16 m
 *                      ‘i64.store32’ m:memarg4     => i64.store32 m
 *                      ‘memory.size’               => memory.size
 *                      ‘memory.grow’               => memory.grow
 * ```
 */
fun List<Token>.parseMemoryInstruction(fromIndex: Int): ParseResult<out MemoryInstruction>? {
    var currentIndex = fromIndex
    val keyword = getOrNull(currentIndex) as? Keyword ?: return null
    currentIndex++

    val (instruction, offset) = when (keyword.value) {
        "i32.load" -> {
            val arg = parseMemarg(currentIndex, 4)
            MemoryInstruction.LoadInt(32, 32, false, arg.astNode) to arg.parseLength
        }
        "i64.load" -> {
            val arg = parseMemarg(currentIndex, 8)
            MemoryInstruction.LoadInt(64, 64, false, arg.astNode) to arg.parseLength
        }
        "f32.load" -> {
            val arg = parseMemarg(currentIndex, 4)
            MemoryInstruction.LoadFloat(32, arg.astNode) to arg.parseLength
        }
        "f64.load" -> {
            val arg = parseMemarg(currentIndex, 8)
            MemoryInstruction.LoadFloat(64, arg.astNode) to arg.parseLength
        }
        "i32.load8_s" -> {
            val arg = parseMemarg(currentIndex, 1)
            MemoryInstruction.LoadInt(32, 8, true, arg.astNode) to arg.parseLength
        }
        "i32.load8_u" -> {
            val arg = parseMemarg(currentIndex, 1)
            MemoryInstruction.LoadInt(32, 8, false, arg.astNode) to arg.parseLength
        }
        "i32.load16_s" -> {
            val arg = parseMemarg(currentIndex, 2)
            MemoryInstruction.LoadInt(32, 16, true, arg.astNode) to arg.parseLength
        }
        "i32.load16_u" -> {
            val arg = parseMemarg(currentIndex, 2)
            MemoryInstruction.LoadInt(32, 16, false, arg.astNode) to arg.parseLength
        }
        "i64.load8_s" -> {
            val arg = parseMemarg(currentIndex, 1)
            MemoryInstruction.LoadInt(64, 8, true, arg.astNode) to arg.parseLength
        }
        "i64.load8_u" -> {
            val arg = parseMemarg(currentIndex, 1)
            MemoryInstruction.LoadInt(64, 8, false, arg.astNode) to arg.parseLength
        }
        "i64.load16_s" -> {
            val arg = parseMemarg(currentIndex, 2)
            MemoryInstruction.LoadInt(64, 16, true, arg.astNode) to arg.parseLength
        }
        "i64.load16_u" -> {
            val arg = parseMemarg(currentIndex, 2)
            MemoryInstruction.LoadInt(64, 16, false, arg.astNode) to arg.parseLength
        }
        "i64.load32_s" -> {
            val arg = parseMemarg(currentIndex, 4)
            MemoryInstruction.LoadInt(64, 32, true, arg.astNode) to arg.parseLength
        }
        "i64.load32_u" -> {
            val arg = parseMemarg(currentIndex, 4)
            MemoryInstruction.LoadInt(64, 32, false, arg.astNode) to arg.parseLength
        }
        "i32.store" -> {
            val arg = parseMemarg(currentIndex, 4)
            MemoryInstruction.StoreInt(32, 32, arg.astNode) to arg.parseLength
        }
        "i64.store" -> {
            val arg = parseMemarg(currentIndex, 8)
            MemoryInstruction.StoreInt(64, 64, arg.astNode) to arg.parseLength
        }
        "i32.store8" -> {
            val arg = parseMemarg(currentIndex, 1)
            MemoryInstruction.StoreInt(32, 8, arg.astNode) to arg.parseLength
        }
        "i32.store16" -> {
            val arg = parseMemarg(currentIndex, 2)
            MemoryInstruction.StoreInt(32, 16, arg.astNode) to arg.parseLength
        }
        "i64.store8" -> {
            val arg = parseMemarg(currentIndex, 1)
            MemoryInstruction.StoreInt(64, 8, arg.astNode) to arg.parseLength
        }
        "i64.store16" -> {
            val arg = parseMemarg(currentIndex, 2)
            MemoryInstruction.StoreInt(64, 16, arg.astNode) to arg.parseLength
        }
        "i64.store32" -> {
            val arg = parseMemarg(currentIndex, 4)
            MemoryInstruction.StoreInt(64, 32, arg.astNode) to arg.parseLength
        }
        "f32.store" -> {
            val arg = parseMemarg(currentIndex, 4)
            MemoryInstruction.StoreFloat(32, arg.astNode) to arg.parseLength
        }
        "f64.store" -> {
            val arg = parseMemarg(currentIndex, 8)
            MemoryInstruction.StoreFloat(64, arg.astNode) to arg.parseLength
        }
        "memory.size" -> MemoryInstruction.Size to 0
        "memory.grow" -> MemoryInstruction.Grow to 0
        else -> null to 0
    }
    currentIndex += offset

    return instruction?.let {
        ParseResult(
            instruction.deDupe(),
            currentIndex - fromIndex
        )
    }
}
