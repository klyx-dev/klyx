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
import kwasm.ast.module.Import
import kwasm.ast.module.ImportDescriptor
import kwasm.ast.module.TypeUse
import kwasm.format.binary.BinaryParser
import kwasm.format.binary.type.readGlobalType
import kwasm.format.binary.type.readMemoryType
import kwasm.format.binary.type.readTableType
import kwasm.format.binary.type.toBytes
import kwasm.format.binary.value.readName
import kwasm.format.binary.value.readVector
import kwasm.format.binary.value.toBytesAsVector
import kwasm.format.binary.value.toNameBytes
import kwasm.util.Leb128

/**
 * From [the docs](https://webassembly.github.io/spec/core/binary/modules.html#import-section):
 *
 * The import section has the id 2. It decodes into a vector of imports that represent the
 * `imports` component of a module.
 *
 * ```
 *      importsec   ::= im*:section_2(vec(import))      => im*
 *      import      ::= mod:name nm:name d:importdesc   => {module mod, name nm, desc d}
 *      importdesc  ::= 0x00 x:typeidx                  => func x
 *                      0x01 tt:tabletype               => table tt
 *                      0x02 mt:memtype                 => mem mt
 *                      0x03 gt:globaltype              => global gt
 * ```
 */
fun BinaryParser.readImportSection(): ImportSection = ImportSection(readVector { readImport() })

/** Encodes the Receiving [ImportSection] as a sequence of bytes. */
internal fun ImportSection.toBytesNoHeader(): Sequence<Byte> {
    val bodyBytes = imports.toBytesAsVector { it.toBytes() }.toList()
    return Leb128.encodeUnsigned(bodyBytes.size) + bodyBytes.asSequence()
}

/** Parses a single [Import]. */
internal fun BinaryParser.readImport(): Import =
    Import(readName(), readName(), readImportDescriptor())

/** Encodes the [Import] as a sequence of bytes. */
internal fun Import.toBytes(): Sequence<Byte> =
    moduleName.toNameBytes() + name.toNameBytes() + descriptor.toBytes()

/** Parses a binary-encoded [ImportDescriptor]. */
internal fun BinaryParser.readImportDescriptor(): ImportDescriptor =
    when (val type = readByte().toInt()) {
        0x00 -> {
            val typeIndex = readIndex<Identifier.Type>()
            ImportDescriptor.Function(
                Identifier.Function(null, null),
                TypeUse(typeIndex, emptyList(), emptyList())
            )
        }
        0x01 -> {
            val tableType = readTableType()
            ImportDescriptor.Table(Identifier.Table(null, null), tableType)
        }
        0x02 -> {
            val memType = readMemoryType()
            ImportDescriptor.Memory(Identifier.Memory(null, null), memType)
        }
        0x03 -> {
            val globalType = readGlobalType()
            ImportDescriptor.Global(Identifier.Global(null, null), globalType)
        }
        else ->
            throwException(
                "Invalid import type: 0x${type.toString(16)} (malformed import kind)",
                -1
            )
    }

/** Creates bytes for the receiving [ImportDescriptor]. */
internal fun ImportDescriptor.toBytes(): Sequence<Byte> = when (this) {
    is ImportDescriptor.Function -> sequenceOf<Byte>(0x00) + requireNotNull(typeUse.index).toBytes()
    is ImportDescriptor.Table -> sequenceOf<Byte>(0x01) + tableType.toBytes()
    is ImportDescriptor.Memory -> sequenceOf<Byte>(0x02) + memoryType.toBytes()
    is ImportDescriptor.Global -> sequenceOf<Byte>(0x03) + globalType.toBytes()
}

/** Contains the data from a binary-encoded WebAssembly program's Import Section. */
data class ImportSection(val imports: List<Import>) : Section
