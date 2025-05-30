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
import kwasm.ast.module.Local
import kwasm.ast.type.ValueType
import kwasm.format.binary.BinaryParser
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.io.ByteArrayInputStream

@RunWith(JUnit4::class)
class LocalsTest {
    @Test
    fun readFuncLocals_empty() {
        val locals = emptyList<Local>()
        val bytes = locals.toBytes()
        val parser = BinaryParser(ByteArrayInputStream(bytes.toList().toByteArray()))
        assertThat(parser.readFuncLocals()).isEqualTo(locals)
    }

    @Test
    fun readFuncLocals_singles() {
        val locals = listOf(
            Local(null, ValueType.I32),
            Local(null, ValueType.I64),
            Local(null, ValueType.F32),
            Local(null, ValueType.F64),
            Local(null, ValueType.I64)
        )
        val bytes = locals.toBytes().toList()
        assertThat(bytes.size).isEqualTo(11)
        val parser = BinaryParser(ByteArrayInputStream(bytes.toByteArray()))
        assertThat(parser.readFuncLocals()).isEqualTo(locals)
    }

    @Test
    fun readFuncLocals_allTheSame() {
        val locals = listOf(
            Local(null, ValueType.I32),
            Local(null, ValueType.I32),
            Local(null, ValueType.I32),
            Local(null, ValueType.I32),
            Local(null, ValueType.I32),
            Local(null, ValueType.I32),
        )
        val bytes = locals.toBytes().toList()
        assertThat(bytes.size).isEqualTo(3)
        val parser = BinaryParser(ByteArrayInputStream(bytes.toByteArray()))
        assertThat(parser.readFuncLocals()).isEqualTo(locals)
    }

    @Test
    fun readFuncLocals_halfAndHalf() {
        val locals = listOf(
            Local(null, ValueType.I32),
            Local(null, ValueType.I32),
            Local(null, ValueType.I32),
            Local(null, ValueType.F32),
            Local(null, ValueType.F32),
            Local(null, ValueType.F32),
        )
        val bytes = locals.toBytes().toList()
        assertThat(bytes.size).isEqualTo(5)
        val parser = BinaryParser(ByteArrayInputStream(bytes.toByteArray()))
        assertThat(parser.readFuncLocals()).isEqualTo(locals)
    }
}
