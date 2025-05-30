/*
 * Copyright 2019 Google LLC
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

package kwasm.ast

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class IdentifierTest {
    @Test
    fun type_createsValidUniqueValue() {
        val type = Identifier.Type("test")
        assertThat(type.unique).isEqualTo("test".hashCode())
    }

    @Test
    fun function_createsValidUniqueValue() {
        val type = Identifier.Function("test")
        assertThat(type.unique).isEqualTo("test".hashCode())
    }

    @Test
    fun table_createsValidUniqueValue() {
        val type = Identifier.Table("test")
        assertThat(type.unique).isEqualTo("test".hashCode())
    }

    @Test
    fun memory_createsValidUniqueValue() {
        val type = Identifier.Memory("test")
        assertThat(type.unique).isEqualTo("test".hashCode())
    }

    @Test
    fun global_createsValidUniqueValue() {
        val type = Identifier.Global("test")
        assertThat(type.unique).isEqualTo("test".hashCode())
    }

    @Test
    fun local_createsValidUniqueValue() {
        val type = Identifier.Local("test")
        assertThat(type.unique).isEqualTo("test".hashCode())
    }

    @Test
    fun label_createsValidUniqueValue() {
        val type = Identifier.Label("test")
        assertThat(type.unique).isEqualTo("test".hashCode())
    }

    // TODO: add test(s) for TypeDef when ready.
}
