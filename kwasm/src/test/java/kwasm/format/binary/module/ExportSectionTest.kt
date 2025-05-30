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
import kwasm.ast.module.Export
import kwasm.ast.module.ExportDescriptor
import kwasm.ast.module.Index
import kwasm.format.ParseException
import kwasm.format.binary.BinaryParser
import kwasm.format.binary.value.toNameBytes
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.io.ByteArrayInputStream

@Suppress("UNCHECKED_CAST")
@RunWith(JUnit4::class)
class ExportSectionTest {
    @Test
    fun empty() {
        val section = ExportSection(emptyList())
        val bytes = sequenceOf<Byte>(0x07) + section.toBytesNoHeader()
        val parser = BinaryParser(ByteArrayInputStream(bytes.toList().toByteArray()))
        assertThat(parser.readSection()).isEqualTo(section)
    }

    @Test
    fun nonEmpty() {
        val section = ExportSection(
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
        val bytes = sequenceOf<Byte>(0x07) + section.toBytesNoHeader()
        val parser = BinaryParser(ByteArrayInputStream(bytes.toList().toByteArray()))
        assertThat(parser.readSection()).isEqualTo(section)
    }

    @Test
    fun readExport_function() {
        val export = Export(
            "myFunction",
            ExportDescriptor.Function(Index.ByInt(123) as Index<Identifier.Function>)
        )
        val bytes = export.toBytes().toList().toByteArray()
        val parser = BinaryParser(ByteArrayInputStream(bytes))
        assertThat(parser.readExport()).isEqualTo(export)
    }

    @Test
    fun readExport_table() {
        val export = Export(
            "myTable",
            ExportDescriptor.Table(Index.ByInt(123) as Index<Identifier.Table>)
        )
        val bytes = export.toBytes().toList().toByteArray()
        val parser = BinaryParser(ByteArrayInputStream(bytes))
        assertThat(parser.readExport()).isEqualTo(export)
    }

    @Test
    fun readExport_memory() {
        val export = Export(
            "myMemory",
            ExportDescriptor.Memory(Index.ByInt(123) as Index<Identifier.Memory>)
        )
        val bytes = export.toBytes().toList().toByteArray()
        val parser = BinaryParser(ByteArrayInputStream(bytes))
        assertThat(parser.readExport()).isEqualTo(export)
    }

    @Test
    fun readExport_global() {
        val export = Export(
            "myGlobal",
            ExportDescriptor.Global(Index.ByInt(123) as Index<Identifier.Global>)
        )
        val bytes = export.toBytes().toList().toByteArray()
        val parser = BinaryParser(ByteArrayInputStream(bytes))
        assertThat(parser.readExport()).isEqualTo(export)
    }

    @Test
    fun readExport_withInvalidType_fails() {
        val bytes = "myExport".toNameBytes() + sequenceOf(0xFF.toByte()) + Index.ByInt(5).toBytes()
        val parser = BinaryParser(ByteArrayInputStream(bytes.toList().toByteArray()))
        val e = assertThrows(ParseException::class.java) { parser.readExport() }

        assertThat(e).hasMessageThat().contains("Invalid export descriptor")
    }
}
