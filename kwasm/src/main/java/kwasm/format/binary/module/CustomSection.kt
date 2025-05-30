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

import kwasm.format.binary.BinaryParser
import kwasm.format.binary.value.readName

/**
 * From [the docs](https://webassembly.github.io/spec/core/binary/modules.html#custom-section):
 *
 * Custom sections have the id 0. They are intended to be used for debugging information or
 * third-party extensions, and are ignored by the WebAssembly semantics. Their contents consist of
 * a name further identifying the custom section, followed by an uninterpreted sequence of bytes
 * for custom use.
 *
 * ```
 *      customsec   ::= section_0(custom)
 *      custom      ::= name byte*
 * ```
 * **Note**
 * If an implementation interprets the data of a custom section, then errors in that data, or the
 * placement of the section, must not invalidate the module.
 */
fun BinaryParser.readCustomSection(totalSize: Int): CustomSection {
    val beforePos = position
    val name = readName()
    val nameLen = position - beforePos
    val expectedBytesSize = totalSize - nameLen
    if (expectedBytesSize < 0) {
        throwException("Custom section has invalid size, can't contain name (unexpected end)")
    }
    return CustomSection(name, readBytes(expectedBytesSize))
}

/** Represents a custom section in a binary-encoded WebAssembly module. */
data class CustomSection(val name: String, val data: ByteArray) : Section {
    override fun equals(other: Any?): Boolean {
        if (other !is CustomSection) return false
        if (name != other.name) return false
        return true
    }
    override fun hashCode(): Int = name.hashCode()
    override fun toString(): String = "CustomSection(name=\"$name\", data=${data.size} bytes)"
}
