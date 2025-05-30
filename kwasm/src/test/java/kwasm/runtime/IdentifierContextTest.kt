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

package kwasm.runtime

import com.google.common.truth.Truth.assertThat
import kwasm.KWasmRuntimeException
import kwasm.ast.Identifier
import kwasm.ast.astNodeListOf
import kwasm.ast.type.FunctionType
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class IdentifierContextTest {
    private lateinit var context: IdentifierContext

    @Before
    fun setUp() {
        context = IdentifierContext()
    }

    @Test
    fun register_registersIntoCorrectMap() {
        Identifier.Type("myType").also {
            assertThat(context.get<Identifier.Type>(it.unique)).isNull()
            val registeredType: Identifier.Type = context.register(it)
            assertThat(context.get<Identifier.Type>(registeredType.unique)).isEqualTo(it)
        }

        Identifier.Function("myFunction").also {
            assertThat(context.get<Identifier.Function>(it.unique)).isNull()
            val registeredType: Identifier.Function = context.register(it)
            assertThat(context.get<Identifier.Function>(registeredType.unique)).isEqualTo(it)
        }

        Identifier.Table("myTable").also {
            assertThat(context.get<Identifier.Table>(it.unique)).isNull()
            val registeredType: Identifier.Table = context.register(it)
            assertThat(context.get<Identifier.Table>(registeredType.unique)).isEqualTo(it)
        }

        Identifier.Memory("myMemory").also {
            assertThat(context.get<Identifier.Memory>(it.unique)).isNull()
            val registeredType: Identifier.Memory = context.register(it)
            assertThat(context.get<Identifier.Memory>(registeredType.unique)).isEqualTo(it)
        }

        Identifier.Global("myGlobal").also {
            assertThat(context.get<Identifier.Global>(it.unique)).isNull()
            val registeredType: Identifier.Global = context.register(it)
            assertThat(context.get<Identifier.Global>(registeredType.unique)).isEqualTo(it)
        }

        Identifier.Local("myLocal").also {
            assertThat(context.get<Identifier.Local>(it.unique)).isNull()
            val registeredType: Identifier.Local = context.register(it)
            assertThat(context.get<Identifier.Local>(registeredType.unique)).isEqualTo(it)
        }

        Identifier.Label("myLabel").also {
            assertThat(context.get<Identifier.Label>(it.unique)).isNull()
            val registeredType: Identifier.Label = context.register(it)
            assertThat(context.get<Identifier.Label>(registeredType.unique)).isEqualTo(it)
        }

        Identifier.TypeDef(
            FunctionType(
                astNodeListOf(),
                astNodeListOf()
            )
        ).also {
            assertThat(context.get(it.funcType)).isNull()
            val registeredType: Identifier.TypeDef = context.register(it)
            assertThat(context.get(registeredType.funcType)).isEqualTo(it)
        }
    }

    @Test
    fun getOfIdThrows_ifAskingFor_TypeDef() {
        assertThrows(KWasmRuntimeException::class.java) { context.get<Identifier.TypeDef>(10) }
    }
}
