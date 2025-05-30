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

import kwasm.ast.module.Local
import kwasm.ast.type.ValueType
import kwasm.format.binary.BinaryParser
import kwasm.format.binary.type.readValueType
import kwasm.format.binary.type.toByte
import kwasm.format.binary.value.readUInt
import kwasm.format.binary.value.readVector
import kwasm.format.binary.value.toBytesAsVector
import kwasm.util.Leb128

/**
 * From [the code section docs](https://webassembly.github.io/spec/core/binary/modules.html#code-section):
 *
 * Local declarations are compressed into a vector whose entries consist of
 * * a `u32` count,
 * * a value type,
 * denoting count locals of the same value type.
 *
 * ```
 *      func    ::= (t*)*:vec(locals) e:expr    => concat((t*)*), e* (if |concat((t*)*)| < 2^32)
 *      locals  ::= n:u32 t:valtype             => tn
 * ```
 *
 * The meta function `concat((t*)*)` concatenates all sequences `t*_i` in `(t*)*`.
 */
@Suppress("EXPERIMENTAL_API_USAGE")
fun BinaryParser.readFuncLocals(): List<Local> {
    return readVector {
        val count = readUInt()
        val type = readValueType()
        mutableListOf<ValueType>().apply {
            if (count.toUInt() > Int.MAX_VALUE.toUInt()) {
                throwException("Cannot parse func locals (too many locals)", -2)
            }
            repeat(count) { add(type) }
        }
    }.flatten().map { Local(null, it) }
}

/** Encodes a list of [Local]s to a sequence of bytes. */
fun List<Local>.toBytes(): Sequence<Byte> {
    val localsPairs = mutableListOf<Pair<Int, ValueType>>()

    var count = 0
    var type: ValueType = ValueType.I32
    forEach {
        if (it.valueType != type && count > 0) {
            localsPairs.add(count to type)
            count = 0
        }
        count++
        type = it.valueType
    }
    if (count > 0) localsPairs.add(count to type)

    return localsPairs.toBytesAsVector { (count, type) ->
        Leb128.encodeUnsigned(count) + type.toByte()
    }
}
