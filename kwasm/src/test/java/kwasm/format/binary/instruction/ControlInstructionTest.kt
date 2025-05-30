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

package kwasm.format.binary.instruction

import com.google.common.truth.Truth.assertThat
import kwasm.ast.Identifier
import kwasm.ast.instruction.ControlInstruction
import kwasm.ast.instruction.NumericInstruction
import kwasm.ast.module.Index
import kwasm.ast.module.TypeUse
import kwasm.ast.type.Result
import kwasm.ast.type.ResultType
import kwasm.ast.type.ValueType
import kwasm.format.ParseException
import kwasm.format.binary.BinaryParser
import kwasm.format.binary.toByteArray
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.ByteArrayInputStream

@Suppress("UNCHECKED_CAST")
@RunWith(Parameterized::class)
class ControlInstructionTest(val params: Params) {
    @Test
    fun runTest() {
        val parser = BinaryParser(ByteArrayInputStream(params.bytes.toByteArray()))
        if (params.expected != null) {
            assertThat(parser.readInstruction()).isEqualTo(params.expected)
        } else {
            assertThrows(params.expectedError!!.javaClass) { parser.readInstruction() }
        }
    }

    data class Params(
        val name: String,
        val bytes: List<Int>,
        val expected: ControlInstruction?,
        val expectedError: Throwable? = null
    ) {
        override fun toString(): String = name
    }

    companion object {
        private val INDEX = 0x2C
        private val INDEX_LABEL = Index.ByInt(0x2C) as Index<Identifier.Label>
        private val INDEX_FUNCTION = Index.ByInt(0x2C) as Index<Identifier.Function>
        private val INDEX_TYPE_USE = Index.ByInt(0x2C) as Index<Identifier.Type>
        private val EMPTY_LABEL: Identifier.Label? = null
        private val EMPTY_RESULTTYPE = ResultType(null, null)
        private val VALUETYPE_RESULTTYPE = ResultType(Result(ValueType.I32))
        private val INDEX_RESULTTYPE =
            ResultType(null, Index.ByInt(INDEX) as Index<Identifier.Type>)

        @get:Parameterized.Parameters(name = "{0}")
        @get:JvmStatic
        val parameters = arrayOf(
            Params(
                name = "Unreachable",
                bytes = listOf(0x00),
                expected = ControlInstruction.Unreachable
            ),
            Params(
                name = "No-Op",
                bytes = listOf(0x01),
                expected = ControlInstruction.NoOp
            ),
            Params(
                name = "Block (empty blocktype, empty)",
                bytes = listOf(0x02, 0x40, 0x0B),
                expected = ControlInstruction.Block(
                    EMPTY_LABEL,
                    EMPTY_RESULTTYPE,
                    emptyList()
                )
            ),
            Params(
                name = "Block (empty blocktype, non-empty)",
                bytes = listOf(0x02, 0x40, 0x45, 0x0B),
                expected = ControlInstruction.Block(
                    EMPTY_LABEL,
                    EMPTY_RESULTTYPE,
                    listOf(NumericInstruction.I32EqualsZero)
                )
            ),
            Params(
                name = "Block (empty blocktype, nested)",
                bytes = listOf(0x02, 0x40, 0x02, 0x40, 0x45, 0x0B, 0x0B),
                expected = ControlInstruction.Block(
                    EMPTY_LABEL,
                    EMPTY_RESULTTYPE,
                    listOf(
                        ControlInstruction.Block(
                            EMPTY_LABEL,
                            EMPTY_RESULTTYPE,
                            listOf(
                                NumericInstruction.I32EqualsZero
                            )
                        )
                    )
                )
            ),
            Params(
                name = "Block (valuetype blocktype, empty)",
                bytes = listOf(0x02, 0x7F, 0x0B),
                expected = ControlInstruction.Block(
                    EMPTY_LABEL,
                    VALUETYPE_RESULTTYPE,
                    emptyList()
                )
            ),
            Params(
                name = "Block (valuetype blocktype, non-empty)",
                bytes = listOf(0x02, 0x7F, 0x45, 0x0B),
                expected = ControlInstruction.Block(
                    EMPTY_LABEL,
                    VALUETYPE_RESULTTYPE,
                    listOf(NumericInstruction.I32EqualsZero)
                )
            ),
            Params(
                name = "Block (valuetype blocktype, nested)",
                bytes = listOf(0x02, 0x7F, 0x02, 0x7F, 0x45, 0x0B, 0x0B),
                expected = ControlInstruction.Block(
                    EMPTY_LABEL,
                    VALUETYPE_RESULTTYPE,
                    listOf(
                        ControlInstruction.Block(
                            EMPTY_LABEL,
                            VALUETYPE_RESULTTYPE,
                            listOf(NumericInstruction.I32EqualsZero)
                        )
                    )
                )
            ),
            Params(
                name = "Block (index blocktype, empty)",
                bytes = listOf(0x02, INDEX, 0x0B),
                expected = ControlInstruction.Block(
                    EMPTY_LABEL,
                    INDEX_RESULTTYPE,
                    emptyList()
                )
            ),
            Params(
                name = "Block (index blocktype, non-empty)",
                bytes = listOf(0x02, INDEX, 0x45, 0x0B),
                expected = ControlInstruction.Block(
                    EMPTY_LABEL,
                    INDEX_RESULTTYPE,
                    listOf(NumericInstruction.I32EqualsZero)
                )
            ),
            Params(
                name = "Block (index blocktype, nested)",
                bytes = listOf(0x02, INDEX, 0x02, INDEX, 0x45, 0x0B, 0x0B),
                expected = ControlInstruction.Block(
                    EMPTY_LABEL,
                    INDEX_RESULTTYPE,
                    listOf(
                        ControlInstruction.Block(
                            EMPTY_LABEL,
                            INDEX_RESULTTYPE,
                            listOf(NumericInstruction.I32EqualsZero)
                        )
                    )
                )
            ),
            Params(
                name = "Loop (empty blocktype, empty)",
                bytes = listOf(0x03, 0x40, 0x0B),
                expected = ControlInstruction.Loop(
                    EMPTY_LABEL,
                    EMPTY_RESULTTYPE,
                    emptyList()
                )
            ),
            Params(
                name = "Loop (empty blocktype, non-empty)",
                bytes = listOf(0x03, 0x40, 0x45, 0x0B),
                expected = ControlInstruction.Loop(
                    EMPTY_LABEL,
                    EMPTY_RESULTTYPE,
                    listOf(NumericInstruction.I32EqualsZero)
                )
            ),
            Params(
                name = "Loop (empty blocktype, nested)",
                bytes = listOf(0x03, 0x40, 0x03, 0x40, 0x45, 0x0B, 0x0B),
                expected = ControlInstruction.Loop(
                    EMPTY_LABEL,
                    EMPTY_RESULTTYPE,
                    listOf(
                        ControlInstruction.Loop(
                            EMPTY_LABEL,
                            EMPTY_RESULTTYPE,
                            listOf(
                                NumericInstruction.I32EqualsZero
                            )
                        )
                    )
                )
            ),
            Params(
                name = "Loop (valuetype blocktype, empty)",
                bytes = listOf(0x03, 0x7F, 0x0B),
                expected = ControlInstruction.Loop(
                    EMPTY_LABEL,
                    VALUETYPE_RESULTTYPE,
                    emptyList()
                )
            ),
            Params(
                name = "Loop (valuetype blocktype, non-empty)",
                bytes = listOf(0x03, 0x7F, 0x45, 0x0B),
                expected = ControlInstruction.Loop(
                    EMPTY_LABEL,
                    VALUETYPE_RESULTTYPE,
                    listOf(NumericInstruction.I32EqualsZero)
                )
            ),
            Params(
                name = "Loop (valuetype blocktype, nested)",
                bytes = listOf(0x03, 0x7F, 0x03, 0x7F, 0x45, 0x0B, 0x0B),
                expected = ControlInstruction.Loop(
                    EMPTY_LABEL,
                    VALUETYPE_RESULTTYPE,
                    listOf(
                        ControlInstruction.Loop(
                            EMPTY_LABEL,
                            VALUETYPE_RESULTTYPE,
                            listOf(NumericInstruction.I32EqualsZero)
                        )
                    )
                )
            ),
            Params(
                name = "Loop (index blocktype, empty)",
                bytes = listOf(0x03, INDEX, 0x0B),
                expected = ControlInstruction.Loop(
                    EMPTY_LABEL,
                    INDEX_RESULTTYPE,
                    emptyList()
                )
            ),
            Params(
                name = "Loop (index blocktype, non-empty)",
                bytes = listOf(0x03, INDEX, 0x45, 0x0B),
                expected = ControlInstruction.Loop(
                    EMPTY_LABEL,
                    INDEX_RESULTTYPE,
                    listOf(NumericInstruction.I32EqualsZero)
                )
            ),
            Params(
                name = "Loop (index blocktype, nested)",
                bytes = listOf(0x03, INDEX, 0x03, INDEX, 0x45, 0x0B, 0x0B),
                expected = ControlInstruction.Loop(
                    EMPTY_LABEL,
                    INDEX_RESULTTYPE,
                    listOf(
                        ControlInstruction.Loop(
                            EMPTY_LABEL,
                            INDEX_RESULTTYPE,
                            listOf(NumericInstruction.I32EqualsZero)
                        )
                    )
                )
            ),
            Params(
                name = "If - no else (empty blocktype, empty)",
                bytes = listOf(0x04, 0x40, 0x0B),
                expected = ControlInstruction.If(
                    EMPTY_LABEL,
                    EMPTY_RESULTTYPE,
                    emptyList(),
                    emptyList()
                )
            ),
            Params(
                name = "If - no else (empty blocktype, non-empty)",
                bytes = listOf(0x04, 0x40, 0x45, 0x0B),
                expected = ControlInstruction.If(
                    EMPTY_LABEL,
                    EMPTY_RESULTTYPE,
                    listOf(NumericInstruction.I32EqualsZero),
                    emptyList()
                )
            ),
            Params(
                name = "If - no else (empty blocktype, nested)",
                bytes = listOf(0x04, 0x40, 0x04, 0x40, 0x45, 0x0B, 0x0B),
                expected = ControlInstruction.If(
                    EMPTY_LABEL,
                    EMPTY_RESULTTYPE,
                    listOf(
                        ControlInstruction.If(
                            EMPTY_LABEL,
                            EMPTY_RESULTTYPE,
                            listOf(
                                NumericInstruction.I32EqualsZero
                            ),
                            emptyList()
                        )
                    ),
                    emptyList()
                )
            ),
            Params(
                name = "If - no else (valuetype blocktype, empty)",
                bytes = listOf(0x04, 0x7F, 0x0B),
                expected = ControlInstruction.If(
                    EMPTY_LABEL,
                    VALUETYPE_RESULTTYPE,
                    emptyList(),
                    emptyList()
                )
            ),
            Params(
                name = "If - no else (valuetype blocktype, non-empty)",
                bytes = listOf(0x04, 0x7F, 0x45, 0x0B),
                expected = ControlInstruction.If(
                    EMPTY_LABEL,
                    VALUETYPE_RESULTTYPE,
                    listOf(NumericInstruction.I32EqualsZero),
                    emptyList()
                )
            ),
            Params(
                name = "If - no else (valuetype blocktype, nested)",
                bytes = listOf(0x04, 0x7F, 0x04, 0x7F, 0x45, 0x0B, 0x0B),
                expected = ControlInstruction.If(
                    EMPTY_LABEL,
                    VALUETYPE_RESULTTYPE,
                    listOf(
                        ControlInstruction.If(
                            EMPTY_LABEL,
                            VALUETYPE_RESULTTYPE,
                            listOf(NumericInstruction.I32EqualsZero),
                            emptyList()
                        )
                    ),
                    emptyList()
                )
            ),
            Params(
                name = "If - no else (index blocktype, empty)",
                bytes = listOf(0x04, INDEX, 0x0B),
                expected = ControlInstruction.If(
                    EMPTY_LABEL,
                    INDEX_RESULTTYPE,
                    emptyList(),
                    emptyList()
                )
            ),
            Params(
                name = "If - no else (index blocktype, non-empty)",
                bytes = listOf(0x04, INDEX, 0x45, 0x0B),
                expected = ControlInstruction.If(
                    EMPTY_LABEL,
                    INDEX_RESULTTYPE,
                    listOf(NumericInstruction.I32EqualsZero),
                    emptyList()
                )
            ),
            Params(
                name = "If - no else (index blocktype, nested)",
                bytes = listOf(0x04, INDEX, 0x04, INDEX, 0x45, 0x0B, 0x0B),
                expected = ControlInstruction.If(
                    EMPTY_LABEL,
                    INDEX_RESULTTYPE,
                    listOf(
                        ControlInstruction.If(
                            EMPTY_LABEL,
                            INDEX_RESULTTYPE,
                            listOf(NumericInstruction.I32EqualsZero),
                            emptyList()
                        )
                    ),
                    emptyList()
                )
            ),
            Params(
                name = "If - else (empty blocktype, empty)",
                bytes = listOf(0x04, 0x40, 0x05, 0x0B),
                expected = ControlInstruction.If(
                    EMPTY_LABEL,
                    EMPTY_RESULTTYPE,
                    emptyList(),
                    emptyList()
                )
            ),
            Params(
                name = "If - else (empty blocktype, non-empty)",
                bytes = listOf(0x04, 0x40, 0x45, 0x05, 0x45, 0x0B),
                expected = ControlInstruction.If(
                    EMPTY_LABEL,
                    EMPTY_RESULTTYPE,
                    listOf(NumericInstruction.I32EqualsZero),
                    listOf(NumericInstruction.I32EqualsZero),
                )
            ),
            Params(
                name = "If - else (empty blocktype, nested)",
                bytes = listOf(0x04, 0x40, 0x04, 0x40, 0x45, 0x0B, 0x05, 0x04, 0x40, 0x45, 0x0B, 0x0B),
                expected = ControlInstruction.If(
                    EMPTY_LABEL,
                    EMPTY_RESULTTYPE,
                    listOf(
                        ControlInstruction.If(
                            EMPTY_LABEL,
                            EMPTY_RESULTTYPE,
                            listOf(
                                NumericInstruction.I32EqualsZero
                            ),
                            emptyList()
                        )
                    ),
                    listOf(
                        ControlInstruction.If(
                            EMPTY_LABEL,
                            EMPTY_RESULTTYPE,
                            listOf(
                                NumericInstruction.I32EqualsZero
                            ),
                            emptyList()
                        )
                    ),
                )
            ),
            Params(
                name = "If - else (valuetype blocktype, empty)",
                bytes = listOf(0x04, 0x7F, 0x05, 0x0B),
                expected = ControlInstruction.If(
                    EMPTY_LABEL,
                    VALUETYPE_RESULTTYPE,
                    emptyList(),
                    emptyList()
                )
            ),
            Params(
                name = "If - else (valuetype blocktype, non-empty)",
                bytes = listOf(0x04, 0x7F, 0x45, 0x05, 0x45, 0x0B),
                expected = ControlInstruction.If(
                    EMPTY_LABEL,
                    VALUETYPE_RESULTTYPE,
                    listOf(NumericInstruction.I32EqualsZero),
                    listOf(NumericInstruction.I32EqualsZero),
                )
            ),
            Params(
                name = "If - else (valuetype blocktype, nested)",
                bytes = listOf(0x04, 0x7F, 0x04, 0x7F, 0x45, 0x0B, 0x05, 0x04, 0x7F, 0x45, 0x0B, 0x0B),
                expected = ControlInstruction.If(
                    EMPTY_LABEL,
                    VALUETYPE_RESULTTYPE,
                    listOf(
                        ControlInstruction.If(
                            EMPTY_LABEL,
                            VALUETYPE_RESULTTYPE,
                            listOf(NumericInstruction.I32EqualsZero),
                            emptyList()
                        )
                    ),
                    listOf(
                        ControlInstruction.If(
                            EMPTY_LABEL,
                            VALUETYPE_RESULTTYPE,
                            listOf(NumericInstruction.I32EqualsZero),
                            emptyList()
                        )
                    ),
                )
            ),
            Params(
                name = "If - else (index blocktype, empty)",
                bytes = listOf(0x04, INDEX, 0x05, 0x0B),
                expected = ControlInstruction.If(
                    EMPTY_LABEL,
                    INDEX_RESULTTYPE,
                    emptyList(),
                    emptyList()
                )
            ),
            Params(
                name = "If - else (index blocktype, non-empty)",
                bytes = listOf(0x04, INDEX, 0x45, 0x05, 0x45, 0x0B),
                expected = ControlInstruction.If(
                    EMPTY_LABEL,
                    INDEX_RESULTTYPE,
                    listOf(NumericInstruction.I32EqualsZero),
                    listOf(NumericInstruction.I32EqualsZero),
                )
            ),
            Params(
                name = "If - else (index blocktype, nested)",
                bytes = listOf(0x04, INDEX, 0x04, INDEX, 0x45, 0x0B, 0x05, 0x04, INDEX, 0x45, 0x0B, 0x0B),
                expected = ControlInstruction.If(
                    EMPTY_LABEL,
                    INDEX_RESULTTYPE,
                    listOf(
                        ControlInstruction.If(
                            EMPTY_LABEL,
                            INDEX_RESULTTYPE,
                            listOf(NumericInstruction.I32EqualsZero),
                            emptyList()
                        )
                    ),
                    listOf(
                        ControlInstruction.If(
                            EMPTY_LABEL,
                            INDEX_RESULTTYPE,
                            listOf(NumericInstruction.I32EqualsZero),
                            emptyList()
                        )
                    ),
                )
            ),
            Params(
                name = "Break",
                bytes = listOf(0x0C, INDEX),
                expected = ControlInstruction.Break(INDEX_LABEL)
            ),
            Params(
                name = "Break If",
                bytes = listOf(0x0D, INDEX),
                expected = ControlInstruction.BreakIf(INDEX_LABEL)
            ),
            Params(
                name = "Break Table",
                bytes = listOf(0x0E, 0x03, INDEX, INDEX, INDEX, INDEX),
                expected = ControlInstruction.BreakTable(
                    listOf(INDEX_LABEL, INDEX_LABEL, INDEX_LABEL),
                    INDEX_LABEL
                )
            ),
            Params(
                name = "Return",
                bytes = listOf(0x0F),
                expected = ControlInstruction.Return
            ),
            Params(
                name = "Call",
                bytes = listOf(0x10, INDEX),
                expected = ControlInstruction.Call(INDEX_FUNCTION)
            ),
            Params(
                name = "Call Indirect",
                bytes = listOf(0x11, INDEX, 0x00),
                expected = ControlInstruction.CallIndirect(
                    TypeUse(INDEX_TYPE_USE, emptyList(), emptyList())
                )
            ),
            Params(
                name = "Call Indirect - Missing Table Index",
                bytes = listOf(0x11, INDEX),
                expected = null,
                expectedError = ParseException("Invalid table index")
            ),
            Params(
                name = "Call Indirect - Bad Table Index",
                bytes = listOf(0x11, INDEX, 0x01),
                expected = null,
                expectedError = ParseException("Invalid table index")
            ),
        )
    }
}
