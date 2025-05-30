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
import kwasm.ast.module.Index
import kwasm.ast.module.Memory
import kwasm.ast.module.StartFunction
import kwasm.ast.module.Table
import kwasm.ast.module.Type
import kwasm.ast.module.TypeUse
import kwasm.ast.module.WasmFunction
import kwasm.ast.module.WasmModule
import kwasm.format.binary.BinaryParser

/**
 * From [the docs](https://webassembly.github.io/spec/core/binary/modules.html#binary-module):
 *
 * The encoding of a module starts with a preamble containing a 4-byte magic number (the string
 * `‘∖0asm’`) and a version field. The current version of the WebAssembly binary format is 1.
 *
 * The preamble is followed by a sequence of sections. Custom sections may be inserted at any place
 * in this sequence, while other sections must occur at most once and in the prescribed order. All
 * sections can be empty.
 *
 * The lengths of vectors produced by the (possibly empty) function and code section must match up.
 *
 * ```
 *      magic   ::= 0x00 0x61 0x73 0x6D
 *      version ::= 0x01 0x00 0x00 0x00
 *      module  ::= magic
 *                  version
 *                  customsec*
 *                  functype*:typesec
 *                  customsec*
 *                  import*:importsec
 *                  customsec*
 *                  typeidx^n:funcsec
 *                  customsec*
 *                  table*:tablesec
 *                  customsec*
 *                  mem*:memsec
 *                  customsec*
 *                  global*:globalsec
 *                  customsec*
 *                  export*:exportsec
 *                  customsec*
 *                  start?:startsec
 *                  customsec*
 *                  elem*:elemsec
 *                  customsec*
 *                  code^n:codesec
 *                  customsec*
 *                  data*:datasec
 *                  customsec*          => {types functype*,
 *                                          funcs func^n,
 *                                          tables table*,
 *                                          mems mem*,
 *                                          globals global*,
 *                                          elem elem*,
 *                                          data data*,
 *                                          start start?,
 *                                          imports import*,
 *                                          exports export*}
 * ```
 */
fun BinaryParser.readModule(): WasmModule {
    // Read the magic.
    val magicBytes = readBytes(4).withIndex()
    if (magicBytes.any { (i, byte) -> MAGIC[i] != byte }) {
        throwException(
            "File appears to not be a valid binary-formatted WebAssembly module, missing header." +
                " (magic header not detected)"
        )
    }
    // Read the version.
    val versionBytes = readBytes(4).withIndex()
    if (versionBytes.any { (i, byte) -> VERSION[i] != byte }) {
        val versionString =
            versionBytes.joinToString(" ") { (_, b) -> "0x" + b.toString(16).padStart(2, '0') }
        val expectedString = VERSION.joinToString(" ") { "0x" + it.toString(16).padStart(2, '0') }
        throwException(
            "File appears to be of an unsupported version: $versionString, " +
                "expected: $expectedString (unknown binary version)"
        )
    }

    var typeSection = TypeSection(emptyList())
    var importSection = ImportSection(emptyList())
    var funcSection = FunctionSection(emptyList())
    var tableSection = TableSection(emptyList())
    var memorySection = MemorySection(emptyList())
    var globalSection = GlobalSection(emptyList())
    var exportSection = ExportSection(emptyList())
    var startSection: StartSection? = null
    var elementSection = ElementSection(emptyList())
    var codeSection = CodeSection(emptyList())
    var dataSection = DataSection(emptyList())

    do {
        val section = readSection()
        when (section) {
            is TypeSection -> typeSection = section
            is ImportSection -> importSection = section
            is FunctionSection -> funcSection = section
            is TableSection -> tableSection = section
            is MemorySection -> memorySection = section
            is GlobalSection -> globalSection = section
            is ExportSection -> exportSection = section
            is StartSection -> {
                if (startSection != null) {
                    throwException("Multiple start sections not allowed (junk after last section)")
                }
                startSection = section
            }
            is ElementSection -> elementSection = section
            is CodeSection -> codeSection = section
            is DataSection -> dataSection = section
            null -> continue
        }
    } while (section != null)

    if (funcSection.functionTypes.size != codeSection.code.size) {
        throwException("Section mismatch: function and code section have inconsistent lengths")
    }

    return WasmModule(
        identifier = null,
        types = typeSection.functionTypes.map { Type(null, it) },
        imports = importSection.imports.map {
            val descriptor = it.descriptor
            if (descriptor !is ImportDescriptor.Function) return@map it

            val typeIndex = descriptor.typeUse.index ?: return@map it
            val type = typeSection.functionTypes.getOrNull((typeIndex as Index.ByInt).indexVal)
                ?: return@map it

            Import(
                it.moduleName,
                it.name,
                ImportDescriptor.Function(
                    Identifier.Function(null, null),
                    TypeUse(typeIndex, type.parameters, type.returnValueEnums)
                )
            )
        },
        functions = funcSection.functionTypes.zip(codeSection.code) { typeIndex, func ->
            val i = typeIndex as Index.ByInt
            val type = typeSection.functionTypes[i.indexVal]
            WasmFunction(
                id = null,
                typeUse = TypeUse(typeIndex, type.parameters, type.returnValueEnums),
                locals = func.locals,
                instructions = func.expr.instructions
            )
        },
        tables = tableSection.tables.map { Table(Identifier.Table(null, null), it) },
        memories = memorySection.memories.map { Memory(Identifier.Memory(null, null), it) },
        globals = globalSection.globals,
        exports = exportSection.exports,
        start = startSection?.funcIndex?.let { StartFunction(it) },
        elements = elementSection.segments,
        data = dataSection.data
    )
}

private val MAGIC = byteArrayOf(0x00, 0x61, 0x73, 0x6D)
private val VERSION = byteArrayOf(0x01, 0x00, 0x00, 0x00)
