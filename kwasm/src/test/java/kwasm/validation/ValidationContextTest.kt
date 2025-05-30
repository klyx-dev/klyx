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

package kwasm.validation

import com.google.common.truth.Truth.assertThat
import kwasm.ast.AstNode
import kwasm.ast.Identifier
import kwasm.ast.astNodeListOf
import kwasm.ast.module.Index
import kwasm.ast.module.Type
import kwasm.ast.module.TypeUse
import kwasm.ast.type.ElementType
import kwasm.ast.type.FunctionType
import kwasm.ast.type.GlobalType
import kwasm.ast.type.Limits
import kwasm.ast.type.MemoryType
import kwasm.ast.type.Param
import kwasm.ast.type.Result
import kwasm.ast.type.TableType
import kwasm.ast.type.ValueType
import kwasm.ast.util.AstNodeIndex
import kwasm.format.text.Tokenizer
import kwasm.format.text.module.parseModule
import org.assertj.core.api.Assertions.fail
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@Suppress("EXPERIMENTAL_UNSIGNED_LITERALS")
@RunWith(JUnit4::class)
class ValidationContextTest {
    @Test
    fun conversion_throws_whenFuncReferences_nonExistentType() {
        val module = Tokenizer().tokenize(
            """
            (module
                (func $1 (type $0) (return))
            )
            """.trimIndent()
        ).parseModule(0)?.astNode ?: fail("Expected module")

        val e = assertThrows(ValidationException::class.java) { ValidationContext(module) }
        assertThat(e).hasMessageThat().contains(
            "Function uses type $0, but it's not declared by the module"
        )
    }

    @Test
    fun conversion_throws_whenFuncImportReferences_nonExistentType() {
        val module = Tokenizer().tokenize(
            """
            (module
                (import "a" "b" (func $1 (type $0)))
            )
            """.trimIndent()
        ).parseModule(0)?.astNode ?: fail("Expected module")

        val e = assertThrows(ValidationException::class.java) { ValidationContext(module) }
        assertThat(e).hasMessageThat().contains(
            "Function uses type $0, but it's not declared by the module"
        )
    }

    @Test
    fun conversion() {
        val module = Tokenizer().tokenize(
            """
            (module
                (type $0 (func (param i32) (result f32)))
                (func $1 (param i64) (result f64) (local i32))
                (table 0 1 funcref) 
                (memory 0 1)
                (global $2 (mut i32) (i32.const 0))
                (import "a" "b" (func $2 (type $0)))
                (import "a" "b" (global $1 f32))
            )
            """.trimIndent()
        ).parseModule(0) ?: fail("Expected module")

        val context = ValidationContext(module.astNode)

        assertThat(context.functions.size).isEqualTo(2)
        assertThat(context.functions["$1"]).isEqualTo(
            TypeUse(
                null,
                astNodeListOf(
                    Param(
                        Identifier.Local(null, null),
                        ValueType.I64
                    )
                ),
                astNodeListOf(
                    Result(ValueType.F64)
                )
            )
        )
        assertThat(context.functions["$2"]).isEqualTo(
            TypeUse(
                Index.ByIdentifier(Identifier.Type("$0")),
                astNodeListOf(),
                astNodeListOf()
            )
        )
        assertThat(context.types.size).isEqualTo(2)
        assertThat(context.types["$0"]).isEqualTo(
            Type(
                Identifier.Type("$0"),
                FunctionType(
                    astNodeListOf(
                        Param(
                            Identifier.Local(null, null),
                            ValueType.I32
                        )
                    ),
                    astNodeListOf(
                        Result(ValueType.F32)
                    )
                )
            )
        )
        assertThat(context.types[1]).isEqualTo(
            Type(
                null,
                FunctionType(
                    astNodeListOf(
                        Param(
                            Identifier.Local(null, null),
                            ValueType.I64
                        )
                    ),
                    astNodeListOf(
                        Result(ValueType.F64)
                    )
                )
            )
        )
        assertThat(context.tables.size).isEqualTo(1)
        assertThat(context.tables[0]).isEqualTo(
            TableType(
                Limits(0, 1),
                ElementType.FunctionReference
            )
        )
        assertThat(context.memories.size).isEqualTo(1)
        assertThat(context.memories[0]).isEqualTo(
            MemoryType(
                Limits(0, 1)
            )
        )
        assertThat(context.globals.size).isEqualTo(2)
        assertThat(context.globals["$2"]).isEqualTo(
            GlobalType(ValueType.I32, true)
        )
        assertThat(context.globals["$1"]).isEqualTo(
            GlobalType(ValueType.F32, false)
        )
    }

    operator fun <T : AstNode> AstNodeIndex<T>.get(idString: String): T? =
        get(Identifier.Local(idString))
}
