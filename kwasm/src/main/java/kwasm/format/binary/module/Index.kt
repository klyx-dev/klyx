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

import kwasm.ast.Identifier
import kwasm.ast.module.Index
import kwasm.format.binary.BinaryParser
import kwasm.format.binary.value.readUInt
import kwasm.util.Leb128

/**
 * From [the docs](https://webassembly.github.io/spec/core/binary/modules.html#indices):
 *
 * All indices are encoded with their respective value.
 *
 * ```
 *  typeidx     ::= x:u32 => x
 *  funcidx     ::= x:u32 => x
 *  tableidx    ::= x:u32 => x
 *  memidx      ::= x:u32 => x
 *  globalidx   ::= x:u32 => x
 *  localidx    ::= x:u32 => x
 *  labelidx    ::= l:u32 => l
 * ```
 */
@Suppress("UNCHECKED_CAST")
inline fun <reified T : Identifier> BinaryParser.readIndex(): Index<T> =
    Index.ByInt(readUInt()) as Index<T>

/** Encodes the index (assuming it's an [Index.ByInt]) into a sequence of LEB-128-encoded bytes. */
internal fun Index<*>.toBytes(): Sequence<Byte> =
    Leb128.encodeUnsigned(requireNotNull(this as? Index.ByInt).indexVal)
