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

package kwasm.format.binary.value

import kwasm.format.binary.BinaryParser

/**
 * From [the docs](https://webassembly.github.io/spec/core/binary/values.html#names):
 *
 * Names are encoded as a vector of bytes containing the Unicode (Section 3.9) UTF-8 encoding of
 * the name’s character sequence.
 *
 * ```
 *      name    ::= b∗:vec(byte)    =>  name (if utf8(name)=b∗)
 * ```
 */
fun BinaryParser.readName(): String = readVector().toByteArray().toString(Charsets.UTF_8)

/** Encodes the receiving [String] as a WebAssembly name into a sequence of Bytes. */
internal fun String.toNameBytes(): Sequence<Byte> =
    toByteArray(Charsets.UTF_8).toBytesAsVector()
