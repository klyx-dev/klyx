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

import kwasm.ast.module.Export
import kwasm.ast.module.ExportDescriptor
import kwasm.format.binary.BinaryParser
import kwasm.format.binary.value.readName
import kwasm.format.binary.value.readVector
import kwasm.format.binary.value.toBytesAsVector
import kwasm.format.binary.value.toNameBytes
import kwasm.util.Leb128

/**
 * From [the docs](https://webassembly.github.io/spec/core/binary/modules.html#export-section):
 *
 * The export section has the id 7. It decodes into a vector of exports that represent the `exports`
 * component of a module.
 *
 * ```
 *      exportsec   ::= ex*:section_7(vec(export))  => ex*
 *      export      ::= nm:name d:exportdesc        => {name nm, desc d}
 *      exportdesc  ::= 0x00 x:funcidx              => func x
 *                      0x01 x:tableidx             => table x
 *                      0x02 x:memidx               => mem x
 *                      0x03 x:globalidx            => global x
 * ```
 */
fun BinaryParser.readExportSection(): ExportSection = ExportSection(readVector { readExport() })

/** Encodes an [ExportSection] to a sequence of Bytes. */
internal fun ExportSection.toBytesNoHeader(): Sequence<Byte> {
    val vecBytes = exports.toBytesAsVector { it.toBytes() }.toList()
    return Leb128.encodeUnsigned(vecBytes.size) + vecBytes.asSequence()
}

/** Reads an [Export] as encoded within an [ExportSection] in a binary-encoded WebAssembly Module */
internal fun BinaryParser.readExport(): Export {
    val name = readName()
    val descriptor = when (val type = readByte().toInt()) {
        0 -> ExportDescriptor.Function(readIndex())
        1 -> ExportDescriptor.Table(readIndex())
        2 -> ExportDescriptor.Memory(readIndex())
        3 -> ExportDescriptor.Global(readIndex())
        else -> throwException("Invalid export descriptor type: 0x${type.toString(16)}", -1)
    }
    return Export(name, descriptor)
}

/** Encodes an [Export] to a sequence of bytes. */
internal fun Export.toBytes(): Sequence<Byte> {
    val exportByte: Byte = when (descriptor) {
        is ExportDescriptor.Function -> 0x00
        is ExportDescriptor.Table -> 0x01
        is ExportDescriptor.Memory -> 0x02
        is ExportDescriptor.Global -> 0x03
    }
    return name.toNameBytes() + sequenceOf(exportByte) + this.descriptor.index.toBytes()
}

/** Represents an export section from a binary-encoded WebAssembly program. */
data class ExportSection(val exports: List<Export>) : Section
