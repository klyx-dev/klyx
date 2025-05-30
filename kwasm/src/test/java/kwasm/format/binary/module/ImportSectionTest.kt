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
import kwasm.ast.module.Import
import kwasm.ast.module.ImportDescriptor
import kwasm.ast.module.Index
import kwasm.ast.module.TypeUse
import kwasm.ast.type.ElementType
import kwasm.ast.type.GlobalType
import kwasm.ast.type.Limits
import kwasm.ast.type.MemoryType
import kwasm.ast.type.TableType
import kwasm.ast.type.ValueType
import kwasm.format.ParseException
import kwasm.format.binary.BinaryParser
import kwasm.format.binary.toByteArray
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.io.ByteArrayInputStream

@Suppress("UNCHECKED_CAST", "SameParameterValue")
@RunWith(JUnit4::class)
class ImportSectionTest {
    @Test
    fun readImportSection_empty() {
        val importSection = ImportSection(emptyList())
        val bytes = (listOf<Byte>(0x02) + importSection.toBytesNoHeader().toList()).toByteArray()
        val parser = BinaryParser(ByteArrayInputStream(bytes))
        assertThat(parser.readSection()).isEqualTo(ImportSection(emptyList()))
    }

    @Test
    fun readImportSection_oneOfEach() {
        val importSection = ImportSection(
            listOf(
                Import("functions", "myFunc", createFunctionImportDescriptor(1234)),
                Import("tables", "myTable", createTableImportDescriptor(0, 25)),
                Import("memories", "myMemory", createMemoryImportDescriptor(1, 42)),
                Import("globals", "myGlobal", createGlobalImportDescriptor(ValueType.I32, true)),
            )
        )
        val bytes = (listOf<Byte>(0x02) + importSection.toBytesNoHeader().toList()).toByteArray()
        val parser = BinaryParser(ByteArrayInputStream(bytes))
        assertThat(parser.readSection()).isEqualTo(importSection)
    }

    @Test
    fun readImportDescriptor_invalidType_throws() {
        val bytes = listOf(0x04, 0x01).toByteArray()
        val parser = BinaryParser(ByteArrayInputStream(bytes))
        assertThrows(ParseException::class.java) { parser.readImportDescriptor() }
    }

    @Test
    fun readImportDescriptor_function() {
        val descriptor = createFunctionImportDescriptor(128)
        val bytes = descriptor.toBytes().toList().toByteArray()
        val parser = BinaryParser(ByteArrayInputStream(bytes))
        assertThat(parser.readImportDescriptor()).isEqualTo(descriptor)
    }

    @Test
    fun readImportDescriptor_table() {
        val descriptor = createTableImportDescriptor(1, 256)
        val bytes = descriptor.toBytes().toList().toByteArray()
        val parser = BinaryParser(ByteArrayInputStream(bytes))
        assertThat(parser.readImportDescriptor()).isEqualTo(descriptor)
    }

    @Test
    fun readImportDescriptor_memory() {
        val descriptor = createMemoryImportDescriptor(1, 256)
        val bytes = descriptor.toBytes().toList().toByteArray()
        val parser = BinaryParser(ByteArrayInputStream(bytes))
        assertThat(parser.readImportDescriptor()).isEqualTo(descriptor)
    }

    @Test
    fun readImportDescriptor_global_immutable() {
        val descriptor = createGlobalImportDescriptor(ValueType.F64, false)
        val bytes = descriptor.toBytes().toList().toByteArray()
        val parser = BinaryParser(ByteArrayInputStream(bytes))
        assertThat(parser.readImportDescriptor()).isEqualTo(descriptor)
    }

    @Test
    fun readImportDescriptor_global_mutable() {
        val descriptor = createGlobalImportDescriptor(ValueType.I64, true)
        val bytes = descriptor.toBytes().toList().toByteArray()
        val parser = BinaryParser(ByteArrayInputStream(bytes))
        assertThat(parser.readImportDescriptor()).isEqualTo(descriptor)
    }

    @Test
    fun readImport() {
        val import = Import("My Module", "My Import", createMemoryImportDescriptor(1337))
        val bytes = import.toBytes().toList().toByteArray()
        val parser = BinaryParser(ByteArrayInputStream(bytes))
        assertThat(parser.readImport()).isEqualTo(import)
    }

    companion object {
        internal fun createFunctionImportDescriptor(indexVal: Int) =
            ImportDescriptor.Function(
                Identifier.Function(null, null),
                TypeUse(Index.ByInt(indexVal) as Index<Identifier.Type>, emptyList(), emptyList())
            )

        internal fun createTableImportDescriptor(min: Int, max: Int? = null) =
            ImportDescriptor.Table(
                Identifier.Table(null, null),
                TableType(Limits(min.toLong(), max?.toLong()), ElementType.FunctionReference)
            )

        internal fun createMemoryImportDescriptor(min: Int, max: Int? = null) =
            ImportDescriptor.Memory(
                Identifier.Memory(null, null),
                MemoryType(Limits(min.toLong(), max?.toLong()))
            )

        internal fun createGlobalImportDescriptor(valueType: ValueType, mutable: Boolean) =
            ImportDescriptor.Global(Identifier.Global(null, null), GlobalType(valueType, mutable))
    }
}
