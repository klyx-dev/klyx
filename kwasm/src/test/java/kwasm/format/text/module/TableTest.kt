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
import kwasm.ast.instruction.Expression
import kwasm.ast.instruction.NumericConstantInstruction
import kwasm.ast.module.ElementSegment
import kwasm.ast.module.Index
import kwasm.ast.module.Offset
import kwasm.ast.module.Table
import kwasm.ast.type.ElementType
import kwasm.ast.type.Limits
import kwasm.ast.type.TableType
import kwasm.format.ParseContext
import kwasm.format.ParseException
import kwasm.format.text.TextModuleCounts
import kwasm.format.text.Tokenizer
import org.assertj.core.api.Assertions.fail
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@Suppress("EXPERIMENTAL_UNSIGNED_LITERALS", "EXPERIMENTAL_API_USAGE", "UNCHECKED_CAST")
@RunWith(JUnit4::class)
class TableTest {
    private val counts = TextModuleCounts(0, 0, 0, 0, 0)
    private val tokenizer = Tokenizer()
    private val context = ParseContext("TableTest.wast")

    @Test
    fun parseTableBasic_returnsNull_ifOpenParenNotFound() {
        val result = tokenizer.tokenize("table $0 0 1 funcref)", context)
            .parseTableBasic(0, counts)
        assertThat(result).isNull()
    }

    @Test
    fun parseTableBasic_returnsNull_ifTableKeywordNotFound() {
        val result = tokenizer.tokenize("(non-table $0 0 1 funcref)", context)
            .parseTableBasic(0, counts)
        assertThat(result).isNull()
    }

    @Test
    fun parseTableBasic_returnsNull_ifTableTypeCouldntBeFound() {
        val result = tokenizer.tokenize("(table $0)", context)
            .parseTableBasic(0, counts)
        assertThat(result).isNull()
    }

    @Test
    fun parseTableBasic_returnsNull_ifTableTypeInvalid() {
        val result = tokenizer.tokenize("(table $0 0 1 nonfuncref)", context)
            .parseTableBasic(0, counts)
        assertThat(result).isNull()
    }

    @Test
    fun parseTableBasic_throws_ifNoClosingParenFound() {
        val e = assertThrows(ParseException::class.java) {
            tokenizer.tokenize("(table $0 0 1 funcref", context).parseTableBasic(0, counts)
        }
        assertThat(e).hasMessageThat().contains("Expected ')'")
    }

    @Test
    fun parseTableBasic() {
        val (result, newCounts) = tokenizer.tokenize("(table $0 0 1 funcref)", context)
            .parseTableBasic(0, counts) ?: fail("Expected a result")
        assertThat(result.parseLength).isEqualTo(7)
        assertThat(result.astNode.id).isEqualTo(Identifier.Table("$0"))
        assertThat(result.astNode.tableType).isEqualTo(
            TableType(
                Limits(0, 1),
                ElementType.FunctionReference
            )
        )
        assertThat(newCounts.tables).isEqualTo(counts.tables + 1)
    }

    @Test
    fun parseTableAndElementSegment_returnsNullIf_openingParen_notFound() {
        val result = tokenizer.tokenize("table $0 funcref (elem $1 $2))", context)
            .parseTableAndElementSegment(0, counts)
        assertThat(result).isNull()
    }

    @Test
    fun parseTableAndElementSegment_returnsNullIf_tableKeyword_notFound() {
        val result = tokenizer.tokenize("(non-table $0 funcref (elem $1 $2))", context)
            .parseTableAndElementSegment(0, counts)
        assertThat(result).isNull()
    }

    @Test
    fun parseTableAndElementSegment_returnsNullIf_funcrefKeyword_notFound() {
        val result = tokenizer.tokenize("(table $0 nonfuncref (elem $1 $2))", context)
            .parseTableAndElementSegment(0, counts)
        assertThat(result).isNull()
    }

    @Test
    fun parseTableAndElementSegment_throwsIf_elemOpeningParen_notFound() {
        val e = assertThrows(ParseException::class.java) {
            tokenizer.tokenize("(table $0 funcref elem $1 $2))", context)
                .parseTableAndElementSegment(0, counts)
        }
        assertThat(e).hasMessageThat().contains("Expected inline element segment")
    }

    @Test
    fun parseTableAndElementSegment_throwsIf_elemKeyword_notFound() {
        val e = assertThrows(ParseException::class.java) {
            tokenizer.tokenize("(table $0 funcref (non-elem $1 $2))", context)
                .parseTableAndElementSegment(0, counts)
        }
        assertThat(e).hasMessageThat().contains("Expected inline element segment")
    }

    @Test
    fun parseTableAndElementSegment_throwsIf_elemClosingParen_notFound() {
        val e = assertThrows(ParseException::class.java) {
            tokenizer.tokenize("(table $0 funcref (elem $1 $2)", context)
                .parseTableAndElementSegment(0, counts)
        }
        assertThat(e).hasMessageThat().contains("Expected ')'")
    }

    @Test
    fun parseTableAndElementSegment_throwsIf_closingParen_notFound() {
        val e = assertThrows(ParseException::class.java) {
            tokenizer.tokenize("(table $0 funcref (elem $1 $2", context)
                .parseTableAndElementSegment(0, counts)
        }
        assertThat(e).hasMessageThat().contains("Expected ')'")
    }

    @Test
    fun parseTableAndElementSegment_minimal() {
        val (result, newCounts) = tokenizer.tokenize("(table funcref (elem))", context)
            .parseTableAndElementSegment(0, counts) ?: fail("Expected a result")
        assertThat(result.parseLength).isEqualTo(7)
        val table = result.astNode[0] as Table
        assertThat(table.tableType).isEqualTo(
            TableType(
                Limits(0, 0),
                ElementType.FunctionReference
            )
        )
        val elementSegment = result.astNode[1] as ElementSegment
        assertThat(elementSegment.tableIndex).isEqualTo(
            Index.ByInt(0) as Index<Identifier.Table>,
        )
        assertThat(elementSegment.offset).isEqualTo(
            Offset(
                Expression(
                    astNodeListOf(NumericConstantInstruction.I32(IntegerLiteral.S32(0)))
                )
            )
        )
        assertThat(elementSegment.init).isEmpty()
        assertThat(newCounts.tables).isEqualTo(counts.tables + 1)
    }

    @Test
    fun parseTableAndElementSegment() {
        val (result, newCounts) = tokenizer.tokenize("(table $0 funcref (elem $1 $2))", context)
            .parseTableAndElementSegment(0, counts) ?: fail("Expected a result")
        assertThat(result.parseLength).isEqualTo(10)
        assertThat(result.astNode).containsExactly(
            Table(
                Identifier.Table("$0"),
                TableType(
                    Limits(2, 2),
                    ElementType.FunctionReference
                )
            ),
            ElementSegment(
                Index.ByInt(0) as Index<Identifier.Table>,
                Offset(
                    Expression(
                        astNodeListOf(NumericConstantInstruction.I32(IntegerLiteral.S32(0)))
                    )
                ),
                astNodeListOf(
                    Index.ByIdentifier(Identifier.Function("$1")),
                    Index.ByIdentifier(Identifier.Function("$2"))
                )
            )
        ).inOrder()
        assertThat(newCounts.tables).isEqualTo(counts.tables + 1)
    }

    @Test
    fun parseTable_parsesPlain() {
        val (result, newCounts) = tokenizer.tokenize("(table $0 0 1 funcref)", context)
            .parseTable(0, counts) ?: fail("Expected a result")
        assertThat(result.parseLength).isEqualTo(7)
        assertThat(result.astNode.size).isEqualTo(1)
        assertThat((result.astNode.first() as Table).id).isEqualTo(Identifier.Table("$0"))
        assertThat((result.astNode.first() as Table).tableType).isEqualTo(
            TableType(
                Limits(0, 1),
                ElementType.FunctionReference
            )
        )
        assertThat(newCounts.tables).isEqualTo(counts.tables + 1)
    }

    @Test
    fun parseTable_parsesWithElementSegment() {
        val (result, newCounts) = tokenizer.tokenize("(table $0 funcref (elem $1 $2))", context)
            .parseTable(0, counts) ?: fail("Expected a result")
        assertThat(result.parseLength).isEqualTo(10)
        assertThat(result.astNode).containsExactly(
            Table(
                Identifier.Table("$0"),
                TableType(
                    Limits(2, 2),
                    ElementType.FunctionReference
                )
            ),
            ElementSegment(
                Index.ByInt(0) as Index<Identifier.Table>,
                Offset(
                    Expression(
                        astNodeListOf(NumericConstantInstruction.I32(IntegerLiteral.S32(0)))
                    )
                ),
                astNodeListOf(
                    Index.ByIdentifier(Identifier.Function("$1")),
                    Index.ByIdentifier(Identifier.Function("$2"))
                )
            )
        ).inOrder()
        assertThat(newCounts.tables).isEqualTo(counts.tables + 1)
    }
}
