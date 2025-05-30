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

import com.google.common.truth.Truth.assertThat
import kwasm.ast.Identifier
import kwasm.ast.instruction.Expression
import kwasm.ast.module.DataSegment
import kwasm.ast.module.ElementSegment
import kwasm.ast.module.Export
import kwasm.ast.module.ExportDescriptor
import kwasm.ast.module.Global
import kwasm.ast.module.Import
import kwasm.ast.module.Index
import kwasm.ast.module.Memory
import kwasm.ast.module.Offset
import kwasm.ast.module.StartFunction
import kwasm.ast.module.Table
import kwasm.ast.module.Type
import kwasm.ast.module.TypeUse
import kwasm.ast.module.WasmFunction
import kwasm.ast.module.WasmModule
import kwasm.ast.type.ElementType
import kwasm.ast.type.FunctionType
import kwasm.ast.type.GlobalType
import kwasm.ast.type.Limits
import kwasm.ast.type.MemoryType
import kwasm.ast.type.Param
import kwasm.ast.type.Result
import kwasm.ast.type.TableType
import kwasm.ast.type.ValueType
import kwasm.format.ParseException
import kwasm.format.binary.BinaryParser
import kwasm.format.binary.module.ImportSectionTest.Companion.createFunctionImportDescriptor
import kwasm.format.binary.module.ImportSectionTest.Companion.createGlobalImportDescriptor
import kwasm.format.binary.module.ImportSectionTest.Companion.createMemoryImportDescriptor
import kwasm.format.binary.module.ImportSectionTest.Companion.createTableImportDescriptor
import kwasm.format.binary.toByteArray
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.io.ByteArrayInputStream

@Suppress("UNCHECKED_CAST")
@RunWith(JUnit4::class)
class WasmModuleTest {
    @Test
    fun onlyTypes() {
        val parser = (MAGIC + VERSION + CUSTOM + TYPE_SECTION_BYTES + CUSTOM + CUSTOM).toParser()
        val module = parser.readModule()
        assertThat(module).isEqualTo(
            WasmModule(
                identifier = null,
                types = TYPE_SECTION.functionTypes.map { Type(null, it) },
                imports = emptyList(),
                functions = emptyList(),
                tables = emptyList(),
                memories = emptyList(),
                globals = emptyList(),
                exports = emptyList(),
                start = null,
                elements = emptyList(),
                data = emptyList()
            )
        )
    }

    @Test
    fun importsAndTypes() {
        val parser = (MAGIC + VERSION + TYPE_SECTION_BYTES + IMPORT_SECTION_BYTES).toParser()
        val module = parser.readModule()
        assertThat(module).isEqualTo(
            WasmModule(
                identifier = null,
                types = TYPE_SECTION.functionTypes.map { Type(null, it) },
                imports = IMPORT_SECTION.imports,
                functions = emptyList(),
                tables = emptyList(),
                memories = emptyList(),
                globals = emptyList(),
                exports = emptyList(),
                start = null,
                elements = emptyList(),
                data = emptyList()
            )
        )
    }

    @Test
    fun onlyTypesFunctionsCodes() {
        val parser = (
            MAGIC + VERSION + CUSTOM +
                TYPE_SECTION_BYTES + CUSTOM +
                FUNCTION_SECTION_BYTES + CUSTOM +
                CODE_SECTION_BYTES + CUSTOM
            ).toParser()
        val module = parser.readModule()
        assertThat(module).isEqualTo(
            WasmModule(
                identifier = null,
                types = TYPE_SECTION.functionTypes.map { Type(null, it) },
                imports = emptyList(),
                functions = listOf(
                    WasmFunction(
                        null,
                        TypeUse(
                            Index.ByInt(0) as Index<Identifier.Type>,
                            TYPE_SECTION.functionTypes[0].parameters,
                            TYPE_SECTION.functionTypes[0].returnValueEnums
                        ),
                        emptyList(),
                        emptyList()
                    )
                ),
                tables = emptyList(),
                memories = emptyList(),
                globals = emptyList(),
                exports = emptyList(),
                start = null,
                elements = emptyList(),
                data = emptyList()
            )
        )
    }

    @Test
    fun onlyTables() {
        val parser = (MAGIC + VERSION + CUSTOM + TABLE_SECTION_BYTES + CUSTOM).toParser()
        val module = parser.readModule()
        assertThat(module).isEqualTo(
            WasmModule(
                identifier = null,
                types = emptyList(),
                imports = emptyList(),
                functions = emptyList(),
                tables = TABLE_SECTION.tables.map { Table(Identifier.Table(null, null), it) },
                memories = emptyList(),
                globals = emptyList(),
                exports = emptyList(),
                start = null,
                elements = emptyList(),
                data = emptyList()
            )
        )
    }

    @Test
    fun onlyMemories() {
        val parser = (MAGIC + VERSION + CUSTOM + MEMORY_SECTION_BYTES + CUSTOM).toParser()
        val module = parser.readModule()
        assertThat(module).isEqualTo(
            WasmModule(
                identifier = null,
                types = emptyList(),
                imports = emptyList(),
                functions = emptyList(),
                tables = emptyList(),
                memories = MEMORY_SECTION.memories.map {
                    Memory(Identifier.Memory(null, null), it)
                },
                globals = emptyList(),
                exports = emptyList(),
                start = null,
                elements = emptyList(),
                data = emptyList()
            )
        )
    }

    @Test
    fun onlyGlobals() {
        val parser = (MAGIC + VERSION + CUSTOM + GLOBAL_SECTION_BYTES + CUSTOM).toParser()
        val module = parser.readModule()
        assertThat(module).isEqualTo(
            WasmModule(
                identifier = null,
                types = emptyList(),
                imports = emptyList(),
                functions = emptyList(),
                tables = emptyList(),
                memories = emptyList(),
                globals = GLOBAL_SECTION.globals,
                exports = emptyList(),
                start = null,
                elements = emptyList(),
                data = emptyList()
            )
        )
    }

    @Test
    fun onlyExports() {
        val parser = (MAGIC + VERSION + CUSTOM + EXPORT_SECTION_BYTES + CUSTOM).toParser()
        val module = parser.readModule()
        assertThat(module).isEqualTo(
            WasmModule(
                identifier = null,
                types = emptyList(),
                imports = emptyList(),
                functions = emptyList(),
                tables = emptyList(),
                memories = emptyList(),
                globals = emptyList(),
                exports = EXPORT_SECTION.exports,
                start = null,
                elements = emptyList(),
                data = emptyList()
            )
        )
    }

    @Test
    fun onlyStart() {
        val parser = (MAGIC + VERSION + CUSTOM + START_SECTION_BYTES + CUSTOM).toParser()
        val module = parser.readModule()
        assertThat(module).isEqualTo(
            WasmModule(
                identifier = null,
                types = emptyList(),
                imports = emptyList(),
                functions = emptyList(),
                tables = emptyList(),
                memories = emptyList(),
                globals = emptyList(),
                exports = emptyList(),
                start = StartFunction(Index.ByInt(0) as Index<Identifier.Function>),
                elements = emptyList(),
                data = emptyList()
            )
        )
    }

    @Test
    fun onlyElements() {
        val parser = (MAGIC + VERSION + CUSTOM + ELEMENT_SECTION_BYTES + CUSTOM).toParser()
        val module = parser.readModule()
        assertThat(module).isEqualTo(
            WasmModule(
                identifier = null,
                types = emptyList(),
                imports = emptyList(),
                functions = emptyList(),
                tables = emptyList(),
                memories = emptyList(),
                globals = emptyList(),
                exports = emptyList(),
                start = null,
                elements = ELEMENT_SECTION.segments,
                data = emptyList()
            )
        )
    }

    @Test
    fun onlyData() {
        val parser = (MAGIC + VERSION + CUSTOM + DATA_SECTION_BYTES + CUSTOM).toParser()
        val module = parser.readModule()
        assertThat(module).isEqualTo(
            WasmModule(
                identifier = null,
                types = emptyList(),
                imports = emptyList(),
                functions = emptyList(),
                tables = emptyList(),
                memories = emptyList(),
                globals = emptyList(),
                exports = emptyList(),
                start = null,
                elements = emptyList(),
                data = DATA_SECTION.data
            )
        )
    }

    @Test
    fun everything() {
        val bytes = MAGIC + VERSION +
            TYPE_SECTION_BYTES +
            IMPORT_SECTION_BYTES +
            FUNCTION_SECTION_BYTES +
            TABLE_SECTION_BYTES +
            MEMORY_SECTION_BYTES +
            GLOBAL_SECTION_BYTES +
            EXPORT_SECTION_BYTES +
            START_SECTION_BYTES +
            ELEMENT_SECTION_BYTES +
            CODE_SECTION_BYTES +
            DATA_SECTION_BYTES
        val parser = bytes.toParser()
        assertThat(parser.readModule()).isEqualTo(
            WasmModule(
                identifier = null,
                types = TYPE_SECTION.functionTypes.map { Type(null, it) },
                imports = IMPORT_SECTION.imports,
                functions = listOf(
                    WasmFunction(
                        null,
                        TypeUse(
                            Index.ByInt(0) as Index<Identifier.Type>,
                            TYPE_SECTION.functionTypes[0].parameters,
                            TYPE_SECTION.functionTypes[0].returnValueEnums
                        ),
                        emptyList(),
                        emptyList()
                    )
                ),
                tables = TABLE_SECTION.tables.map { Table(Identifier.Table(null, null), it) },
                memories = MEMORY_SECTION.memories.map {
                    Memory(Identifier.Memory(null, null), it)
                },
                globals = GLOBAL_SECTION.globals,
                exports = EXPORT_SECTION.exports,
                start = StartFunction(Index.ByInt(0) as Index<Identifier.Function>),
                elements = ELEMENT_SECTION.segments,
                data = DATA_SECTION.data
            )
        )
    }

    @Test
    fun invalidMagic_throws() {
        val parser = listOf(0x00, 0x61, 0x73, 0x6E).toParser()
        val e = assertThrows(ParseException::class.java) { parser.readModule() }
        assertThat(e).hasMessageThat()
            .contains(
                "File appears to not be a valid binary-formatted WebAssembly module, missing " +
                    "header."
            )
    }

    @Test
    fun invalidVersion_throws() {
        var parser = listOf(
            0x00,
            0x61,
            0x73,
            0x6D,
            0x02,
            0x00,
            0x00,
            0x00
        ).toParser()
        var e = assertThrows(ParseException::class.java) { parser.readModule() }
        assertThat(e).hasMessageThat()
            .contains(
                "File appears to be of an unsupported version: 0x02 0x00 0x00 0x00, " +
                    "expected: 0x01 0x00 0x00 0x00"
            )

        parser = listOf(
            0x00,
            0x61,
            0x73,
            0x6D,
            0x01,
            0x01,
            0x00,
            0x00
        ).toParser()
        e = assertThrows(ParseException::class.java) { parser.readModule() }
        assertThat(e).hasMessageThat()
            .contains(
                "File appears to be of an unsupported version: 0x01 0x01 0x00 0x00, " +
                    "expected: 0x01 0x00 0x00 0x00"
            )

        parser = listOf(
            0x00,
            0x61,
            0x73,
            0x6D,
            0x01,
            0x00,
            0x01,
            0x00
        ).toParser()
        e = assertThrows(ParseException::class.java) { parser.readModule() }
        assertThat(e).hasMessageThat()
            .contains(
                "File appears to be of an unsupported version: 0x01 0x00 0x01 0x00, " +
                    "expected: 0x01 0x00 0x00 0x00"
            )

        parser = listOf(
            0x00,
            0x61,
            0x73,
            0x6D,
            0x01,
            0x00,
            0x00,
            0x01
        ).toParser()
        e = assertThrows(ParseException::class.java) { parser.readModule() }
        assertThat(e).hasMessageThat()
            .contains(
                "File appears to be of an unsupported version: 0x01 0x00 0x00 0x01, " +
                    "expected: 0x01 0x00 0x00 0x00"
            )
    }

    private fun List<Int>.toParser(): BinaryParser =
        BinaryParser(ByteArrayInputStream(toByteArray()))

    companion object {
        private val MAGIC = listOf(0x00, 0x61, 0x73, 0x6D)
        private val VERSION = listOf(0x01, 0x00, 0x00, 0x00)
        private val CUSTOM = listOf(0x00, 0x03, 0x01, 0x02, 0x03)

        private val TYPE_SECTION = TypeSection(
            listOf(
                FunctionType(
                    listOf(
                        Param(Identifier.Local(null, null), ValueType.I32)
                    ),
                    listOf(
                        Result(ValueType.I64)
                    )
                ),
                FunctionType(emptyList(), emptyList())
            )
        )
        private val TYPE_SECTION_BYTES =
            listOf(0x01, 0x09, 0x02, 0x60, 0x01, 0x7F, 0x01, 0x7E, 0x60, 0x00, 0x00)

        private val IMPORT_SECTION = ImportSection(
            listOf(
                Import(
                    "functions",
                    "myFunc",
                    createFunctionImportDescriptor(0).let {
                        it.copy(
                            typeUse = it.typeUse.copy(
                                params = listOf(Param(Identifier.Local(null, null), ValueType.I32)),
                                results = listOf(Result(ValueType.I64))
                            )
                        )
                    }
                ),
                Import("tables", "myTable", createTableImportDescriptor(0, 25)),
                Import("memories", "myMemory", createMemoryImportDescriptor(1, 42)),
                Import("globals", "myGlobal", createGlobalImportDescriptor(ValueType.I32, true)),
            )
        )
        private val IMPORT_SECTION_BYTES =
            (listOf<Byte>(0x02) + IMPORT_SECTION.toBytesNoHeader().toList()).map(Byte::toInt)

        private val FUNCTION_SECTION =
            FunctionSection(listOf(Index.ByInt(0) as Index<Identifier.Type>))
        private val FUNCTION_SECTION_BYTES =
            (listOf<Byte>(0x03) + FUNCTION_SECTION.toBytesNoHeader().toList()).map(Byte::toInt)

        private val TABLE_SECTION =
            TableSection(listOf(TableType(Limits(0, 1337), ElementType.FunctionReference)))
        private val TABLE_SECTION_BYTES =
            (listOf<Byte>(0x04) + TABLE_SECTION.toBytesNoHeader().toList()).map(Byte::toInt)

        private val MEMORY_SECTION =
            MemorySection(listOf(MemoryType(Limits(0, 1337))))
        private val MEMORY_SECTION_BYTES =
            (listOf<Byte>(0x05) + MEMORY_SECTION.toBytesNoHeader().toList()).map(Byte::toInt)

        private val GLOBAL_SECTION =
            GlobalSection(
                listOf(
                    Global(
                        Identifier.Global(null, null),
                        GlobalType(ValueType.I32, true),
                        Expression(emptyList())
                    )
                )
            )
        private val GLOBAL_SECTION_BYTES =
            listOf(0x06, 0x04, 0x01, 0x7F, 0x01, 0x0B)

        private val EXPORT_SECTION = ExportSection(
            listOf(
                Export(
                    "myFunction",
                    ExportDescriptor.Function(Index.ByInt(1) as Index<Identifier.Function>)
                ),
                Export(
                    "myTable",
                    ExportDescriptor.Table(Index.ByInt(2) as Index<Identifier.Table>)
                ),
                Export(
                    "myMemory",
                    ExportDescriptor.Memory(Index.ByInt(3) as Index<Identifier.Memory>)
                ),
                Export(
                    "myGlobal",
                    ExportDescriptor.Global(Index.ByInt(4) as Index<Identifier.Global>)
                ),
            )
        )
        private val EXPORT_SECTION_BYTES =
            (listOf<Byte>(0x07) + EXPORT_SECTION.toBytesNoHeader().toList()).map(Byte::toInt)

        private val START_SECTION_BYTES = listOf(0x08, 0x01, 0x00)

        private val ELEMENT_SECTION =
            ElementSection(
                listOf(
                    ElementSegment(
                        Index.ByInt(0) as Index<Identifier.Table>,
                        Offset(Expression(emptyList())),
                        emptyList()
                    )
                )
            )
        private val ELEMENT_SECTION_BYTES =
            listOf(0x09, 0x04, 0x01, 0x00, 0x0B, 0x00)

        private val CODE_SECTION_BYTES =
            listOf(0x0A, 0x04, 0x01, 0x02, 0x00, 0x0B)

        private val DATA_SECTION = DataSection(
            listOf(
                DataSegment(
                    Index.ByInt(0) as Index<Identifier.Memory>,
                    Offset(Expression(emptyList())),
                    byteArrayOf(0x01, 0x02, 0x03)
                )
            )
        )
        private val DATA_SECTION_BYTES =
            listOf(0x0B, 0x07, 0x01, 0x00, 0x0B, 0x03, 0x01, 0x02, 0x03)
    }
}
