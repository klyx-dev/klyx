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
import kwasm.ast.module.Index
import kwasm.format.binary.BinaryParser
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.io.ByteArrayInputStream

@Suppress("UNCHECKED_CAST")
@RunWith(JUnit4::class)
class FunctionSectionTest {
    @Test
    fun empty() {
        val section = FunctionSection(emptyList())
        val bytes = (sequenceOf<Byte>(0x03) + section.toBytesNoHeader()).toList().toByteArray()
        val parser = BinaryParser(ByteArrayInputStream(bytes))
        assertThat(parser.readSection()).isEqualTo(section)
    }

    @Test
    fun nonEmpty() {
        val section = FunctionSection(
            listOf(
                Index.ByInt(1) as Index<Identifier.Type>,
                Index.ByInt(128) as Index<Identifier.Type>,
                Index.ByInt(1000) as Index<Identifier.Type>,
                Index.ByInt(-1) as Index<Identifier.Type>,
                Index.ByInt(2) as Index<Identifier.Type>,
            )
        )
        val bytes = (sequenceOf<Byte>(0x03) + section.toBytesNoHeader()).toList().toByteArray()
        val parser = BinaryParser(ByteArrayInputStream(bytes))
        assertThat(parser.readSection()).isEqualTo(section)
    }
}
