/*
 * Copyright 2021 Google LLC
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

package kwasm.format.binary.module

import kwasm.ast.instruction.Expression
import kwasm.ast.module.Local
import kwasm.format.binary.BinaryParser
import kwasm.format.binary.instruction.readExpression
import kwasm.format.binary.value.readUInt
import kwasm.format.binary.value.readVector

/**
 * From [the docs](https://webassembly.github.io/spec/core/binary/modules.html#code-section):
 *
 * The code section has the id 10. It decodes into a vector of code entries that are pairs of value
 * type vectors and expressions. They represent the `locals` and `body` field of the functions in
 * the `funcs` component of a module. The `type` fields of the respective functions are encoded
 * separately in the function section.
 *
 * The encoding of each code entry consists of
 * * the `u32` size of the function code in bytes,
 * * the actual function code, which in turn consists of
 *   * the declaration of locals,
 *   * the function body as an expression.
 *
 * Local declarations are compressed into a vector whose entries consist of
 * * a `u32` count,
 * * a value type,
 * denoting count locals of the same value type.
 *
 * ```
 *      codesec ::= code*:section_10(vec(code)) => code*
 *      code    ::= size:u32 code:func          => code (if size = ||func||)
 *      func    ::= (t*)*:vec(locals) e:expr    => concat((t*)*), e* (if |concat((t*)*)| < 2^32)
 *      locals  ::= n:u32 t:valtype             => t^n
 * ```
 *
 * Here, `code` ranges over pairs `(valtype*, expr)`. The meta function `concat((t*)*)`
 * concatenates all sequences `t*_i` in `(t*)*`. Any code for which the length of the resulting
 * sequence is out of bounds of the maximum size of a vector is malformed.
 *
 * **Note**
 *
 * Like with sections, the code `size` is not needed for decoding, but can be used to skip functions
 * when navigating through a binary. The module is malformed if a size does not match the length of
 * the respective function code.
 */
fun BinaryParser.readCodeSection(): CodeSection = CodeSection(readVector { readCode() })

/** Reads a `code`/`func` element from a binary-enoded WebAssembly module's [CodeSection]. */
internal fun BinaryParser.readCode(): Func {
    val expectedSize = readUInt()
    val startPosition = position
    val locals = readFuncLocals()
    val expr = readExpression()

    if (position - startPosition != expectedSize) {
        throwException("Code section func body does not match declared length: $expectedSize")
    }
    return Func(locals, expr)
}

/** Represents a code section from a binary-encoded WebAssembly module. */
data class CodeSection(val code: List<Func>) : Section

/**
 * Represents a func from a binary-encoded WebAssembly module's [CodeSection]. Will be used to build
 * [kwasm.ast.module.WasmFunction] instances.
 */
data class Func(val locals: List<Local>, val expr: Expression)
