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

import com.google.common.truth.Truth.assertThat
import kwasm.format.binary.BinaryParser
import kwasm.format.binary.toByteArray
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.io.ByteArrayInputStream

@RunWith(JUnit4::class)
class NameValueTest {
    @Test
    fun empty() {
        val nameBytes = listOf(0x00)
        val parser = BinaryParser(ByteArrayInputStream(nameBytes.toByteArray()))
        assertThat(parser.readName()).isEqualTo("")
    }

    @Test
    fun nonEmpty() {
        val name = "This is a test!"
        val bytes = listOf(name.length) + name.toByteArray().map { it.toInt() }
        val parser = BinaryParser(ByteArrayInputStream(bytes.toByteArray()))
        assertThat(parser.readName()).isEqualTo(name)
    }
}
