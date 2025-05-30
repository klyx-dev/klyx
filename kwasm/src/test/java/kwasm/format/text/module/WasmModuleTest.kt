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

package kwasm.format.text.module

import com.google.common.truth.Truth.assertThat
import kwasm.ast.Identifier
import kwasm.ast.IntegerLiteral
import kwasm.ast.astNodeListOf
import kwasm.ast.instruction.ControlInstruction
import kwasm.ast.instruction.Expression
import kwasm.ast.instruction.NumericConstantInstruction
import kwasm.ast.module.DataSegment
import kwasm.ast.module.ElementSegment
import kwasm.ast.module.Export
import kwasm.ast.module.ExportDescriptor
import kwasm.ast.module.Global
import kwasm.ast.module.Import
import kwasm.ast.module.ImportDescriptor
import kwasm.ast.module.Index
import kwasm.ast.module.Local
import kwasm.ast.module.Memory
import kwasm.ast.module.Offset
import kwasm.ast.module.StartFunction
import kwasm.ast.module.Table
import kwasm.ast.module.Type
import kwasm.ast.module.TypeUse
import kwasm.ast.module.WasmFunction
import kwasm.ast.type.ElementType
import kwasm.ast.type.FunctionType
import kwasm.ast.type.GlobalType
import kwasm.ast.type.Limits
import kwasm.ast.type.MemoryType
import kwasm.ast.type.Param
import kwasm.ast.type.Result
import kwasm.ast.type.TableType
import kwasm.ast.type.ValueType
import kwasm.format.ParseContext
import kwasm.format.ParseException
import kwasm.format.text.Tokenizer
import org.assertj.core.api.Assertions.fail
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@Suppress("EXPERIMENTAL_UNSIGNED_LITERALS", "EXPERIMENTAL_API_USAGE", "UNCHECKED_CAST")
@RunWith(JUnit4::class)
class WasmModuleTest {
    private val tokenizer = Tokenizer()
    private val context = ParseContext("WasmModuleTest.wat")

    @Test
    fun parse_returnsNullIf_noOpeningParen() {
        val result = tokenizer.tokenize("module)", context).parseModule(0)
        assertThat(result).isNull()
    }

    @Test
    fun parse_returnsNullIf_moduleKeywordNotFound() {
        val result = tokenizer.tokenize("(nonmodule)", context).parseModule(0)
        assertThat(result).isNull()
    }

    @Test
    fun parse_throwsIf_closingParenNotFound() {
        val e = assertThrows(ParseException::class.java) {
            tokenizer.tokenize("(module", context).parseModule(0)
        }
        assertThat(e).hasMessageThat().contains("Expected ')'")
    }

    @Test
    fun parse_throwsIf_moreThanOneStartFunction_isDefined() {
        val e = assertThrows(ParseException::class.java) {
            tokenizer.tokenize("(module (func) (start 0) (start 0))").parseModule(0)
        }
        assertThat(e).hasMessageThat().contains("Modules may only define one start function")
    }

    @Test
    fun parse_minimal() {
        val result = tokenizer.tokenize("(module)", context).parseModule(0)
            ?: fail("Expected a result")
        assertThat(result.parseLength).isEqualTo(3)
        val module = result.astNode
        assertThat(module.identifier?.stringRepr).isNull()
        assertThat(module.types).isEmpty()
        assertThat(module.imports).isEmpty()
        assertThat(module.functions).isEmpty()
        assertThat(module.tables).isEmpty()
        assertThat(module.memories).isEmpty()
        assertThat(module.globals).isEmpty()
        assertThat(module.exports).isEmpty()
        assertThat(module.start).isNull()
        assertThat(module.elements).isEmpty()
        assertThat(module.data).isEmpty()
    }

    @Test
    fun parse_someOfEverything() {
        val result = tokenizer.tokenize(
            """
            (module $10
                (import "other" "func" (func $0))
                (type $1 (func (param i32) (result i64)))
                (func $2 (local i32) return)
                (table $3 0 1 funcref)
                (memory $4 0 1)
                (global $5 (mut i32) (i32.const 1))
                (export "num2" (func $2))
                (start $2)
                (elem $3 (offset (i32.const 0)) $0)
                (data $4 (offset (i32.const 0)) "test")
                ;; inline abbreviations
                (func $6 (import "a" "b")) ;; 9 tokens
                (func $7 (export "a") return) ;; 9 tokens
                (table $8 (import "a" "b") 1 funcref) ;; 9 tokens
                (table $9 (export "a") 1 funcref) ;; 10 tokens
                (memory $10 (import "a" "b") 1) ;; 10 tokens
                (memory $11 (export "a") 1) ;; 9 tokens
                (global $12 (import "a" "b") i32) ;; 10 tokens
                (global $13 (export "a") i32 (i32.const 0)) ;; 13 tokens
            )
            """.trimIndent(),
            context
        ).parseModule(0) ?: fail("Expected a result")

        assertThat(result.parseLength).isEqualTo(179)
        val module = result.astNode
        assertThat(module.identifier).isEqualTo(Identifier.Label("$10"))
        assertThat(module.imports)
            .containsExactly(
                Import(
                    "other",
                    "func",
                    ImportDescriptor.Function(
                        Identifier.Function("$0"),
                        TypeUse(
                            null,
                            astNodeListOf(),
                            astNodeListOf()
                        )
                    )
                ),
                Import(
                    "a",
                    "b",
                    ImportDescriptor.Function(
                        Identifier.Function("$6"),
                        TypeUse(
                            null,
                            astNodeListOf(),
                            astNodeListOf()
                        )
                    )
                ),
                Import(
                    "a",
                    "b",
                    ImportDescriptor.Table(
                        Identifier.Table("$8"),
                        TableType(
                            Limits(1),
                            ElementType.FunctionReference
                        )
                    )
                ),
                Import(
                    "a",
                    "b",
                    ImportDescriptor.Memory(
                        Identifier.Memory("$10"),
                        MemoryType(Limits(1))
                    )
                ),
                Import(
                    "a",
                    "b",
                    ImportDescriptor.Global(
                        Identifier.Global("$12"),
                        GlobalType(ValueType.I32, false)
                    )
                )
            ).inOrder()
        assertThat(module.types)
            .containsExactly(
                Type(
                    Identifier.Type("$1"),
                    FunctionType(
                        astNodeListOf(
                            Param(
                                Identifier.Local(
                                    null,
                                    null
                                ),
                                ValueType.I32
                            )
                        ),
                        astNodeListOf(Result(ValueType.I64))
                    )
                )
            ).inOrder()
        assertThat(module.functions)
            .containsExactly(
                WasmFunction(
                    Identifier.Function("$2"),
                    TypeUse(
                        null,
                        astNodeListOf(),
                        astNodeListOf()
                    ),
                    astNodeListOf(
                        Local(null, ValueType.I32)
                    ),
                    astNodeListOf(ControlInstruction.Return)
                ),
                WasmFunction(
                    Identifier.Function("$7"),
                    TypeUse(
                        null,
                        astNodeListOf(),
                        astNodeListOf()
                    ),
                    astNodeListOf(),
                    astNodeListOf(ControlInstruction.Return)
                )
            ).inOrder()
        assertThat(module.tables)
            .containsExactly(
                Table(
                    Identifier.Table("$3"),
                    TableType(
                        Limits(0, 1),
                        ElementType.FunctionReference
                    )
                ),
                Table(
                    Identifier.Table("$9"),
                    TableType(
                        Limits(1),
                        ElementType.FunctionReference
                    )
                )
            ).inOrder()
        assertThat(module.memories)
            .containsExactly(
                Memory(
                    Identifier.Memory("$4"),
                    MemoryType(Limits(0, 1))
                ),
                Memory(
                    Identifier.Memory("$11"),
                    MemoryType(Limits(1))
                )
            ).inOrder()
        assertThat(module.globals)
            .containsExactly(
                Global(
                    Identifier.Global("$5"),
                    GlobalType(ValueType.I32, true),
                    Expression(
                        astNodeListOf(
                            NumericConstantInstruction.I32(IntegerLiteral.S32(1))
                        )
                    )
                ),
                Global(
                    Identifier.Global("$13"),
                    GlobalType(ValueType.I32, false),
                    Expression(
                        astNodeListOf(
                            NumericConstantInstruction.I32(IntegerLiteral.S32(0))
                        )
                    )
                )
            ).inOrder()
        assertThat(module.exports)
            .containsExactly(
                Export(
                    "num2",
                    ExportDescriptor.Function(
                        Index.ByIdentifier(Identifier.Function("$2"))
                    )
                ),
                Export(
                    "a",
                    ExportDescriptor.Function(
                        Index.ByInt(3) as Index<Identifier.Function>
                    )
                ),
                Export(
                    "a",
                    ExportDescriptor.Table(
                        Index.ByInt(2) as Index<Identifier.Table>
                    )
                ),
                Export(
                    "a",
                    ExportDescriptor.Memory(
                        Index.ByInt(2) as Index<Identifier.Memory>
                    )
                ),
                Export(
                    "a",
                    ExportDescriptor.Global(
                        Index.ByInt(2) as Index<Identifier.Global>
                    )
                )
            ).inOrder()
        assertThat(module.start)
            .isEqualTo(
                StartFunction(
                    Index.ByIdentifier(
                        Identifier.Function(
                            "$2"
                        )
                    )
                )
            )
        assertThat(module.elements)
            .containsExactly(
                ElementSegment(
                    Index.ByIdentifier(Identifier.Table("$3")),
                    Offset(
                        Expression(
                            astNodeListOf(
                                NumericConstantInstruction.I32(IntegerLiteral.S32(0))
                            )
                        )
                    ),
                    astNodeListOf(
                        Index.ByIdentifier(Identifier.Function("$0"))
                    )
                )
            ).inOrder()
        assertThat(module.data)
            .containsExactly(
                DataSegment(
                    Index.ByIdentifier(Identifier.Memory("$4")),
                    Offset(
                        Expression(
                            astNodeListOf(
                                NumericConstantInstruction.I32(IntegerLiteral.S32(0))
                            )
                        )
                    ),
                    "test".toByteArray(Charsets.UTF_8)
                )
            ).inOrder()
    }
}
